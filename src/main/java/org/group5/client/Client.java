package org.group5.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.rmi.Naming;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.group5.common.ComputationCache;
import org.group5.common.LoggerConfig;
import org.group5.proxy.ProxyServerInterface;
import org.group5.proxy.ServerInfo;
import org.group5.server.ServerInterface;

public class Client
{
    private static final Logger logger = LoggerConfig.getSimpleLogger(Client.class);
    private static final int CACHE_SIZE = 45;
    private static int delay = 50;
    
    // Configuration flags
    private static boolean cacheEnabled = false;
    private static boolean useLRU = false;
    
    // Client-side cache
    private static ComputationCache clientCache;
    
    private static void initializeCache()
    {
        if (cacheEnabled)
        {
            clientCache = new ComputationCache(CACHE_SIZE, useLRU, "Client", logger);
        }
        else
        {
            clientCache = null;
        }
    }
    
    // Helper method to generate cache key from query
    private static String generateCacheKey(String method, String[] parts)
    {
        // Create a consistent cache key based on method and parameters
        StringBuilder keyBuilder = new StringBuilder(method);
        for (int i = 1; i < parts.length; i++)
        {
            keyBuilder.append("_").append(parts[i]);
        }
        return keyBuilder.toString();
    }
    
    // Helper method to execute server call with caching
    private static String executeWithCache(String cacheKey, ServerInterface server,
                                           String method, String[] parts, int zone) throws Exception
    {
        // Check cache if enabled
        if (cacheEnabled && clientCache != null)
        {
            String cachedResult = clientCache.get(cacheKey);
            if (cachedResult != null)
            {
                return cachedResult;
            }
        }
        
        // Execute the actual server call
        String result;
        // Extract zone (final argument)
        switch (method)
        {
            case "getPopulationofCountry" ->
            {
                String countryName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                result = "Population=" + server.getPopulationofCountry(countryName, zone);
            }
            case "getNumberofCities" ->
            {
                long threshold = Long.parseLong(parts[parts.length - 1]);
                String countryName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1));
                result = "Cities=" + server.getNumberofCities(countryName, threshold, zone);
            }
            case "getNumberofCountries" ->
            {
                int cityCount = Integer.parseInt(parts[1]);
                long threshold = Long.parseLong(parts[2]);
                result = "Countries=" + server.getNumberofCountries(cityCount, threshold, zone);
            }
            case "getNumberofCountriesMM" ->
            {
                int cityCount = Integer.parseInt(parts[1]);
                long minPopulation = Long.parseLong(parts[2]);
                long maxPopulation = Long.parseLong(parts[3]);
                result = "Countries=" + server.getNumberofCountriesMM(cityCount, minPopulation, maxPopulation, zone);
            }
            default ->
            {
                throw new IllegalArgumentException("Unknown method: " + method);
            }
        }
        
        // Store in cache if enabled
        if (cacheEnabled && clientCache != null)
        {
            clientCache.put(cacheKey, result);
        }
        
        return result;
    }
    
    public static void main(String[] args) throws Exception
    {
        parseCommandLineArgs(args);
        initializeCache();
        
        String inputFile = "src/main/resources/dataset/exercise_1_input.txt";
        String outputFile = "src/main/resources/dataset/exercise_1_output.txt";
        
        ProxyServerInterface proxy = (ProxyServerInterface) Naming.lookup("rmi://localhost:1099/proxy");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile)))
        {
            String line;
            int index = 0;
            
            while ((line = br.readLine()) != null)
            {
                final String query = line.trim();
                final int currentIndex = index++;
                
                scheduler.schedule(() -> {
                    try
                    {
                        // Validate and split line
                        if (!query.contains("Zone:"))
                        {
                            System.err.println("Skipping malformed query: " + query);
                            return;
                        }
                        
                        // Extract method and args first (needed for cache key)
                        String head = query.split("Zone:")[0].trim();
                        String[] parts = head.split("\\s+");
                        if (parts.length < 2)
                        {
                            System.err.println("Skipping malformed query: " + query);
                            return;
                        }
                        
                        String method = parts[0];
                        
                        String result = null;
                        String serverURL = null;
                        
                        // Generate cache key (zone-independent)
                        String cacheKey = generateCacheKey(method, parts);
                        
                        // Check cache first if enabled
                        if (cacheEnabled && clientCache != null)
                        {
                            result = clientCache.get(cacheKey);
                            if (result != null)
                            {
                                serverURL = "CLIENT-CACHE";
                            }
                        }
                        
                        // If not in cache, proceed with server lookup
                        if (result == null)
                        {
                            int zone = Integer.parseInt(query.split("Zone:")[1].trim());
                            ServerInfo serverInfo = proxy.requestProcessingServer(zone);
                            serverURL = "rmi://localhost:1099/" + serverInfo.getRegistryName();
                            ServerInterface server = (ServerInterface) Naming.lookup(serverURL);
                            
                            // Execute with potential caching
                            result = executeWithCache(cacheKey, server, method, parts, zone);
                        }
                        
                        synchronized (writer)
                        {
                            writer.write(query + " -> " + result + " (handled by " + serverURL + ")\n");
                            writer.flush();
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }, (long) delay * currentIndex, TimeUnit.MILLISECONDS);
            }
        }
        
        // Shutdown the scheduler and wait for all tasks to complete
        scheduler.shutdown();
        try
        {
            // Wait for all tasks to complete before closing the writer
            if (!scheduler.awaitTermination(10, TimeUnit.MINUTES))
            {
                System.err.println("Some tasks did not complete within the timeout");
                scheduler.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        finally
        {
            // Close the writer only after all tasks have completed
            writer.close();
        }
    }
    
    private static void printUsage()
    {
        System.out.println("Usage: java Server [OPTIONS]");
        System.out.println("Options:");
        System.out.println("  --enable-cache    Enable server-side caching (default: false)");
        System.out.println("  --use-lru         Use LRU eviction policy instead of FIFO (default: false)");
        System.out.println("  --delay <N>       The delay between each request in milliseconds (default: 50");
        System.out.println("  --help            Show this help message");
        System.out.println();
        System.out.println("Note: --use-lru only takes effect when --enable-cache is also specified");
    }
    
    private static void parseCommandLineArgs(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            switch (arg)
            {
                case "--enable-cache":
                    cacheEnabled = true;
                    logger.info("Cache enabled");
                    break;
                case "--use-lru":
                    useLRU = true;
                    logger.info("LRU cache policy selected");
                    break;
                case "--delay":
                    if (i + 1 < args.length)
                    {
                        try
                        {
                            delay = Integer.parseInt(args[i + 1]);
                            logger.info("Delay set to " + delay + "ms");
                            i++; // Skip the next argument since we've consumed it
                        }
                        catch (NumberFormatException e)
                        {
                            System.err.println("Invalid delay value: " + args[i + 1]);
                            printUsage();
                            System.exit(1);
                        }
                    }
                    else
                    {
                        System.err.println("--delay requires a value");
                        printUsage();
                        System.exit(1);
                    }
                    break;
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
