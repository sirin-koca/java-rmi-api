package org.group5.server;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.group5.common.ComputationCache;
import org.group5.common.LoggerConfig;
import org.group5.proxy.ProxyServerInterface;

public class Server extends UnicastRemoteObject implements ServerInterface
{
    private static final Logger logger = LoggerConfig.getSimpleLogger(Server.class);
    private static final int CACHE_SIZE = 150;
    private final BlockingQueue<Request> requestQueue;
    private static final String csv_path = "src/main/resources/dataset/exercise_1_dataset.csv";
    
    // Configuration flags
    private static boolean cacheEnabled = false;
    //    private static boolean useLRU = false;
    
    // Server-side cache
    private final ComputationCache serverCache;
    
    // Map to store futures for request results
    private final Map<String, CompletableFuture<Object>> resultFutures = new ConcurrentHashMap<>();
    
    protected Server(String name, int port, boolean cache, boolean lru) throws RemoteException
    {
        super();
        
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
        
        // Connect to proxy and get zone number
        try
        {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            ProxyServerInterface proxy = (ProxyServerInterface) registry.lookup("proxy");
            
            // Assign a zone from proxy
            int zone = proxy.assignZoneNumber(name);
            
            // Bind server object in registry under its name
            registry.rebind(name, this);
            
            // Register server info with proxy
            org.group5.proxy.ServerInfo serverInfo =
                    new org.group5.proxy.ServerInfo(name, name, zone, "localhost", port);
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
    
    private synchronized CompletableFuture<Object> addRequest(Request request)
    {
        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);
        
        CompletableFuture<Object> future = new CompletableFuture<>();
        resultFutures.put(requestId, future);
        
        try
        {
            requestQueue.put(request);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            future.completeExceptionally(new RemoteException("Failed to add request to queue", e));
        }
        
        return future;
    }
    
    @Override
    public long getPopulationofCountry(String countryName) throws RemoteException
    {
        Request request = new Request("getPopulationofCountry", countryName);
        
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
    public int getNumberofCities(String countryName, long threshold) throws RemoteException
    {
        Request request = new Request("getNumberofCities", countryName, threshold);
        
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
    public int getNumberofCountries(int citycount, long threshold) throws RemoteException
    {
        Request request = new Request("getNumberofCountries", citycount, threshold);
        
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
    public int getNumberofCountriesMM(int citycount, long minpopulation, long maxpopulation) throws RemoteException
    {
        Request request = new Request("getNumberofCountriesMM", citycount, minpopulation, maxpopulation);
        
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
    public int queueSize() throws RemoteException
    {
        return requestQueue.size();
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
                    Request request = requestQueue.take(); // TODO: add latency simulation
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
                else
                {
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
        try (BufferedReader br = new BufferedReader(new FileReader(csv_path)))
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
        try (BufferedReader br = new BufferedReader(new FileReader(csv_path)))
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
        
        try (BufferedReader br = new BufferedReader(new FileReader(csv_path)))
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
        
        try (BufferedReader br = new BufferedReader(new FileReader(csv_path)))
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
        //        System.out.println("  --use-lru         Use LRU eviction policy instead of FIFO (default: false)");
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
