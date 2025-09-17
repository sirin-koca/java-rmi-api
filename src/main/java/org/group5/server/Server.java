package org.group5.server;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

import org.group5.common.ComputationCache;
import org.group5.common.LoggerConfig;
import org.group5.proxy.ProxyServerInterface;

import static java.lang.Math.abs;

public class Server extends UnicastRemoteObject implements ServerInterface
{
    private static final Logger logger = LoggerConfig.getSimpleLogger(Server.class);
    private static final int CACHE_SIZE = 150;
    private final BlockingQueue<Request> requestQueue;
    private static final String CSV_PATH =
            System.getProperty("csv.path", "src/main/resources/dataset/exercise_1_dataset.csv");
    private final int zone;

    //Logging thread for writing queue size at intervals to server log file
    private ScheduledExecutorService scheduler;
    
    // Configuration flags
    private static boolean cacheEnabled = false;
    //    private static boolean useLRU = false;
    
    // Server-side cache
    private final ComputationCache serverCache;
    
    // Map to store futures for request results
    private final Map<String, CompletableFuture<Object>> resultFutures = new ConcurrentHashMap<>();
    
    protected Server(String name, int port, boolean cache, boolean lru) throws RemoteException
    {
        super(port); // export RMI object on the fixed port we pass (works through Docker port mapping)

        if (cache)
        {
            cacheEnabled = true;
            serverCache = new ComputationCache(CACHE_SIZE, lru, name, logger);
        }
        else
        {
            serverCache = null;
        }
        
        this.requestQueue = new LinkedBlockingQueue<>();
        //Start the logging thread
        startQueueSizeLogger();
        
        // Connect to proxy and get zone number
        try {
            String proxyHost = System.getProperty("proxy.host", "localhost");
            int    proxyPort = Integer.parseInt(System.getProperty("proxy.port", "1099"));

            // This value was set in ServerBootstrap, we read it here for registration
            String advertisedHost = System.getProperty("java.rmi.server.hostname", "localhost");

            Registry registry = LocateRegistry.getRegistry(proxyHost, proxyPort);
            ProxyServerInterface proxy = (ProxyServerInterface) registry.lookup("proxy");

            // Ask proxy for a zone
            zone = proxy.assignZoneNumber(name);

            // Bind this server object into the proxy's registry
            registry.rebind(name, this);

            // Register server info (host + fixed RMI port)
            org.group5.proxy.ServerInfo serverInfo =
                    new org.group5.proxy.ServerInfo(name, name, zone, advertisedHost, port);
            proxy.registerServer(serverInfo);

            System.out.println("Assigned zone number: " + zone + " for server " + name);
        }
        catch (Exception e)
        {
            throw new RemoteException("Failed to register with proxy", e);
        }
        
        // Start thread to handle execution of requests from queue
        Thread requestHandlerThread = new Thread(new RequestHandler());
        requestHandlerThread.start();
    }
    
    public static void main(String[] args)
    {
        parseCommandLineArgs(args);
    }
    
    // Simulating latency before adding request to queue
    private CompletableFuture<Object> addRequest(Request request)
    {
        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);
        
        CompletableFuture<Object> future = new CompletableFuture<>();
        resultFutures.put(requestId, future);
        
        try
        {
            int clientZone = request.getClientZone();
            if (zone != clientZone)
            {
                // when server and client on different zones, use the distance between server zone and client zone
                int diff = abs(zone - clientZone);
                long latency = 80 + 30L * diff;
                //                logger.info("Request from different zone, sleeping for " + latency + "ms");
                Thread.sleep(latency);
            }
            else
            {
                // standard latency 80ms when client and server in the same zone
                //                logger.info("Request from same zone, sleeping for 80ms");
                Thread.sleep(80);
            }
            requestQueue.put(request);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            future.completeExceptionally(new RemoteException("Failed to add request to queue", e));
        }
        
        return future;
    }
    //Start logging the queue size to a file every 10 seconds
    private void startQueueSizeLogger(){
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try{
                    logQueueSizeToFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
    //Method that logs the current queue size with a timestamp to a file with the server zone in the file name
    private void logQueueSizeToFile() throws IOException {
        String baseLogDir = System.getProperty(
                "log.dir",
                System.getenv().getOrDefault("LOG_DIR", "/app/logs")
        );
        File dir = new File(baseLogDir);
        if (!dir.exists()) dir.mkdirs();

        String logFilePath = baseLogDir + "/" + zone + ".txt";
        int currentQueueSize = queueSize();
        String ts = new java.util.Date().toString();

        try (BufferedWriter w = new BufferedWriter(new FileWriter(logFilePath, true))) {
            w.write("Timestamp: " + ts + ", Queue Size: " + currentQueueSize);
            w.newLine();
        }
    }
    //Get size of queue for server and proxy server use
    @Override
    public int queueSize() throws RemoteException
    {
        return requestQueue.size();
    }
    //Shut down scheduler when server is done
    public void shutdownServerLogger(){
        if (scheduler != null){
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)){
                    scheduler.shutdownNow();
                }
            }catch (InterruptedException e){
                scheduler.shutdownNow();
            }
        }
    }

    @Override
    public long getPopulationofCountry(String countryName, int clientZone) throws RemoteException
    {
        Request request = new Request("getPopulationofCountry", countryName, clientZone);

        try
        {
            CompletableFuture<Object> future = addRequest(request);
            // This blocks until the request handler processes the request
            return (Long) future.get();
        }
        catch (Exception e)
        {
            throw new RemoteException("Failed to process request", e);
        }
    }

    @Override
    public int getNumberofCities(String countryName, long threshold, int clientZone) throws RemoteException
    {
        Request request = new Request("getNumberofCities", countryName, threshold, clientZone);

        try
        {
            CompletableFuture<Object> future = addRequest(request);
            return (Integer) future.get();
        }
        catch (Exception e)
        {
            throw new RemoteException("Failed to process request", e);
        }
    }

    @Override
    public int getNumberofCountries(int citycount, long threshold, int clientZone) throws RemoteException
    {
        Request request = new Request("getNumberofCountries", citycount, threshold, clientZone);

        try
        {
            CompletableFuture<Object> future = addRequest(request);
            return (Integer) future.get();
        }
        catch (Exception e)
        {
            throw new RemoteException("Failed to process request", e);
        }
    }

    @Override
    public int getNumberofCountriesMM(int citycount, long minpopulation, long maxpopulation, int clientZone) throws RemoteException
    {
        Request request = new Request("getNumberofCountriesMM", citycount, minpopulation, maxpopulation, clientZone);
        try
        {
            CompletableFuture<Object> future = addRequest(request);
            return (Integer) future.get();
        }
        catch (Exception e)
        {
            throw new RemoteException("Failed to process request", e);
        }
    }


    // Helper method to generate cache key from request
    private String generateCacheKey(Request request)
    {
        StringBuilder keyBuilder = new StringBuilder(request.getMethodName());
        for (Object arg : request.getArgs())
        {
            keyBuilder.append("_").append(arg.toString());
        }
        return keyBuilder.toString();
    }
    
    // Thread that takes request from queue and executes
    private class RequestHandler implements Runnable
    {
        @Override
        public void run()
        {
            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
//                    Thread.sleep(300);
                    Request request = requestQueue.take();
                    processRequest(request);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break; //Exit loop if interrupted
                }
            }
        }
    }
    
    // Method that gets method name and arguments from client request
    // Method name is taken out first, so first argument refers to argument after method name
    private void processRequest(Request request)
    {
        String requestId = request.getRequestId();
        CompletableFuture<Object> future = resultFutures.remove(requestId);
        
        if (future == null)
        {
            System.err.println("No future found for request: " + requestId);
            return;
        }
        try
        {
            Object result = null;
            
            // Generate cache key for this request
            String cacheKey = generateCacheKey(request);
            
            // Check cache first if enabled
            if (cacheEnabled && serverCache != null)
            {
                String cachedResult = serverCache.get(cacheKey);
                if (cachedResult != null)
                {
                    // Parse cached result back to appropriate type
                    result = parseCachedResult(request.getMethodName(), cachedResult);
                }
            }
            // If not in cache, compute the result
            if (result == null)
            {
                switch (request.getMethodName())
                {
                    case "getPopulationofCountry":
                        String countryName = (String) request.getArgs()[0];
                        result = calculatePopulationofCountry(countryName);
                        System.out.println("Population of " + countryName + ": " + result);
                        break;
                    
                    case "getNumberofCities":
                        String countryNameCities = (String) request.getArgs()[0];
                        long threshold = (Long) request.getArgs()[1];
                        result = calculateNumberofCities(countryNameCities, threshold);
                        System.out.println("Number of cities in " + countryNameCities +
                                " with population >= " + threshold + ": " + result);
                        break;
                    
                    case "getNumberofCountries":
                        int reqCityCount = (Integer) request.getArgs()[0];
                        long populationThreshold = (Long) request.getArgs()[1];
                        result = calculateNumberofCountries(reqCityCount, populationThreshold);
                        System.out.println("Number of countries with at least " + reqCityCount +
                                " cities with population >= " + populationThreshold + ": " + result);
                        break;
                    
                    case "getNumberofCountriesMM":
                        int cityCountThreshold = (Integer) request.getArgs()[0];
                        long minPopulation = (Long) request.getArgs()[1];
                        long maxPopulation = (Long) request.getArgs()[2];
                        result = calculateNumberofCountriesMM(cityCountThreshold, minPopulation, maxPopulation);
                        System.out.println("Number of countries with at least " + cityCountThreshold +
                                " cities with population between " + minPopulation +
                                " and " + maxPopulation + ": " + result);
                        break;
                    
                    default:
                        future.completeExceptionally(
                                new IllegalArgumentException("Unknown method: " + request.getMethodName()));
                        return;
                }
                // Store result in cache if enabled
                if (cacheEnabled && serverCache != null)
                {
                    serverCache.put(cacheKey, result.toString());
                }
            }
            future.complete(result);
        }
        catch (Exception e)
        {
            future.completeExceptionally(e);
            e.printStackTrace();
        }
    }
    
    // Helper method to parse cached string result back to appropriate type
    private Object parseCachedResult(String methodName, String cachedValue)
    {
        switch (methodName)
        {
            case "getPopulationofCountry":
                return Long.parseLong(cachedValue);
            case "getNumberofCities":
            case "getNumberofCountries":
            case "getNumberofCountriesMM":
                return Integer.parseInt(cachedValue);
            default:
                throw new IllegalArgumentException("Unknown method: " + methodName);
        }
    }
    
    // Private methods that do the actual calculations
    private long calculatePopulationofCountry(String countryName)
    {
        long population = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_PATH)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                String[] fields = line.split(";");
                if (fields.length > 4 && fields[3].equalsIgnoreCase(countryName))
                {
                    population += Long.parseLong(fields[4]);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return population;
    }
    
    private int calculateNumberofCities(String countryName, long threshold)
    {
        int cityCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_PATH)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                String[] fields = line.split(";");
                if (fields.length > 4 && fields[3].equalsIgnoreCase(countryName)
                        && Long.parseLong(fields[4]) >= threshold)
                {
                    cityCount++;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return cityCount;
    }
    
    private int calculateNumberofCountries(int citycount, long threshold)
    {
        Map<String, Integer> citiesPerCountry = new HashMap<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_PATH)))
        {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null)
            {
                if (firstLine)
                {
                    firstLine = false;
                    continue;
                }
                String[] fields = line.split(";");
                if (fields.length > 4)
                {
                    try
                    {
                        long population = Long.parseLong(fields[4]);
                        if (population >= threshold)
                        {
                            String countryName = fields[3];
                            citiesPerCountry.put(countryName,
                                    citiesPerCountry.getOrDefault(countryName, 0) + 1);
                        }
                    }
                    catch (NumberFormatException ignored)
                    {
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        int qualifyingCountries = 0;
        for (int count : citiesPerCountry.values())
        {
            if (count >= citycount)
            {
                qualifyingCountries++;
            }
        }
        return qualifyingCountries;
    }
    
    private int calculateNumberofCountriesMM(int citycount, long minpopulation, long maxpopulation)
    {
        Map<String, Integer> countryCities = new HashMap<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_PATH)))
        {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null)
            {
                if (firstLine)
                {
                    firstLine = false;
                    continue;
                }
                String[] fields = line.split(";");
                if (fields.length > 4)
                {
                    try
                    {
                        long population = Long.parseLong(fields[4]);
                        String countryName = fields[3];
                        if (population >= minpopulation && population <= maxpopulation)
                        {
                            countryCities.put(countryName,
                                    countryCities.getOrDefault(countryName, 0) + 1);
                        }
                    }
                    catch (NumberFormatException ignored)
                    {
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        int qualifyingCountries = 0;
        for (int count : countryCities.values())
        {
            if (count >= citycount)
            {
                qualifyingCountries++;
            }
        }
        return qualifyingCountries;
    }
    
    private static void printUsage()
    {
        System.out.println("Usage: java Server [OPTIONS]");
        System.out.println("Options:");
        System.out.println("  --enable-cache    Enable server-side caching (default: false)");
        //      System.out.println("  --use-lru   Use LRU eviction policy instead of FIFO (default: false)");
        System.out.println("  --help            Show this help message");
        System.out.println();
        System.out.println("Note: --use-lru only takes effect when --enable-cache is also specified");
    }
    
    private static void parseCommandLineArgs(String[] args)
    {
        for (String arg : args)
        {
            switch (arg)
            {
                case "--enable-cache":
                    cacheEnabled = true;
                    logger.info("Cache enabled");
                    break;
                //                case "--use-lru":
                //                    useLRU = true;
                //                    logger.info("LRU cache policy selected");
                //                    break;
                case "--help":
                    printUsage();
                    System.exit(0);
                    break;
                default:
                    if (arg.startsWith("--"))
                    {
                        System.err.println("Unknown flag: " + arg);
                        printUsage();
                        System.exit(1);
                    }
                    break;
            }
        }
    }
}
