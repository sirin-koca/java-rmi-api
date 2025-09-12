package com.ass1.client;

import com.ass1.common.LoggerConfig;
import com.ass1.common.ComputationCache;
import com.ass1.proxy.ProxyServerInterface;
import com.ass1.proxy.ServerInfo;
import com.ass1.server.ProcessingServerInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ClientSimulator
{
    private static final Logger logger = LoggerConfig.getSimpleLogger(ClientSimulator.class);
    private static final int CACHE_SIZE = 3;
    
    // Configuration flags
    private static boolean cacheEnabled = false;
    private static boolean useLRU = false;
    
    // Client-side cache (thread-safe)
    private static ComputationCache clientCache;
    
    // Thread pool for concurrent clients
    private static ExecutorService clientExecutor;
    
    // Inner class to represent a client request
    private static class ClientRequest
    {
        final int clientZone;
        final int num1;
        final int num2;
        final String requestId;
        
        ClientRequest(int clientZone, int num1, int num2)
        {
            this.clientZone = clientZone;
            this.num1 = num1;
            this.num2 = num2;
            this.requestId = "Zone" + clientZone + "_" + num1 + "+" + num2 + "_" + System.nanoTime();
        }
    }
    
    // Inner class to represent a result
    private static class ClientResult
    {
        final ClientRequest request;
        final int result;
        final long responseTime;
        final boolean fromCache;
        final String threadName;
        
        ClientResult(ClientRequest request, int result, long responseTime, boolean fromCache, String threadName)
        {
            this.request = request;
            this.result = result;
            this.responseTime = responseTime;
            this.fromCache = fromCache;
            this.threadName = threadName;
        }
    }
    
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
    
    private static ProcessingServerInterface connectToServerForZone(int clientZone)
            throws RemoteException, NotBoundException
    {
        Registry registry = LocateRegistry.getRegistry();
        
        // Contact proxy to get server information
        ProxyServerInterface proxy = (ProxyServerInterface) registry.lookup("proxy");
        ServerInfo serverInfo = proxy.requestProcessingServer(clientZone);
        
        logger.info("Client from zone " + clientZone + " - Proxy assigned server: " + serverInfo);
        
        // Connect to the assigned server
        ProcessingServerInterface server = (ProcessingServerInterface) registry.lookup(serverInfo.getRegistryName());
        logger.info("Client from zone " + clientZone +
                " - Connected to server in zone " + serverInfo.getZone());
        
        return server;
    }
    
    /**
     * Processes a single client request (can be called concurrently)
     */
    private static ClientResult processClientRequest(ClientRequest request)
    {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        
        try
        {
            logger.info("[" + threadName + "] Processing request: " + request.requestId);
            
            int result;
            boolean fromCache = false;
            
            // Check cache if enabled
            if (cacheEnabled && clientCache != null)
            {
                String cacheKey = request.num1 + "+" + request.num2;
                Integer cachedResult = clientCache.get(cacheKey);
                
                if (cachedResult != null)
                {
                    // Cache hit
                    logger.info("[" + threadName + "] Cache HIT for " + cacheKey);
                    result = cachedResult;
                    fromCache = true;
                }
                else
                {
                    // Cache miss - connect and compute
                    logger.info("[" + threadName + "] Cache MISS for " + cacheKey + " - connecting to server");
                    
                    ProcessingServerInterface server = connectToServerForZone(request.clientZone);
                    result = server.add(request.num1, request.num2);
                    
                    // Store in cache
                    clientCache.put(cacheKey, result);
                    logger.info("[" + threadName + "] Cached result for " + cacheKey);
                }
            }
            else
            {
                // Cache disabled - always connect
                ProcessingServerInterface server = connectToServerForZone(request.clientZone);
                result = server.add(request.num1, request.num2);
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            return new ClientResult(request, result, responseTime, fromCache, threadName);
            
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "[" + threadName + "] Error processing request " + request.requestId, e);
            return null;
        }
    }
    
    /**
     * Simulates concurrent client requests
     */
    private static void simulateConcurrentRequests(List<ClientRequest> requests)
    {
        logger.info("\n=== Starting concurrent simulation with " + requests.size() + " requests ===\n");
        
        // Create futures for all requests
        List<Future<ClientResult>> futures = new ArrayList<>();
        
        // Submit all requests concurrently
        long startTime = System.currentTimeMillis();
        for (ClientRequest request : requests)
        {
            Future<ClientResult> future = clientExecutor.submit(() -> processClientRequest(request));
            futures.add(future);
        }
        
        // Collect results
        List<ClientResult> results = new ArrayList<>();
        for (Future<ClientResult> future : futures)
        {
            try
            {
                ClientResult result = future.get(60, TimeUnit.SECONDS);
                if (result != null)
                {
                    results.add(result);
                }
            }
            catch (Exception e)
            {
                logger.log(Level.SEVERE, "Failed to get result", e);
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Print summary
        printResults(results, totalTime);
    }
    
    private static void printResults(List<ClientResult> results, long totalTime)
    {
        logger.info("\n=== RESULTS SUMMARY ===");
        logger.info("Total execution time: " + totalTime + " ms");
        logger.info("Total requests: " + results.size());
        
        long totalResponseTime = 0;
        int cacheHits = 0;
        
        for (ClientResult result : results)
        {
            logger.info(String.format(
                    "Zone %d: %d + %d = %d | Time: %dms | Cache: %s | Thread: %s",
                    result.request.clientZone,
                    result.request.num1,
                    result.request.num2,
                    result.result,
                    result.responseTime,
                    result.fromCache ? "HIT" : "MISS",
                    result.threadName
            ));
            
            totalResponseTime += result.responseTime;
            if (result.fromCache) cacheHits++;
        }
        
        double avgResponseTime = results.isEmpty() ? 0 : (double) totalResponseTime / results.size();
        logger.info("\nAverage response time: " + String.format("%.2f", avgResponseTime) + " ms");
        
        if (cacheEnabled)
        {
            double cacheHitRate = results.isEmpty() ? 0 : (double) cacheHits / results.size() * 100;
            logger.info("Cache hit rate: " + String.format("%.1f%%", cacheHitRate));
        }
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
                case "--use-lru":
                    useLRU = true;
                    logger.info("LRU cache policy selected");
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
    
    private static void printUsage()
    {
        System.out.println("Usage: java ClientSimulator [OPTIONS]");
        System.out.println("Options:");
        System.out.println("  --enable-cache    Enable client-side caching (default: false)");
        System.out.println("  --use-lru         Use LRU eviction policy instead of FIFO (default: false)");
        System.out.println("  --help            Show this help message");
    }
    
    public static void main(String[] args)
    {
        parseCommandLineArgs(args);
        initializeCache();
        
        // Configure thread pool size (adjust based on your needs)
        int numThreads = 10;
        clientExecutor = Executors.newFixedThreadPool(numThreads, r -> {
            Thread t = new Thread(r);
            t.setName("Client-" + t.getId());
            return t;
        });
        
        logger.info("Configuration: cache=" + cacheEnabled + ", useLRU=" + useLRU + ", threads=" + numThreads);
        
        // Create a list of requests to simulate
        List<ClientRequest> requests = new ArrayList<>();
        
        // Add requests that will be processed concurrently
        requests.add(new ClientRequest(1, 10, 20));
        requests.add(new ClientRequest(3, 15, 25));
        requests.add(new ClientRequest(1, 10, 20));
        requests.add(new ClientRequest(5, 30, 40));
        requests.add(new ClientRequest(2, 12, 18));
        requests.add(new ClientRequest(1, 5, 5));
        requests.add(new ClientRequest(4, 20, 30));
        requests.add(new ClientRequest(2, 10, 20));
        requests.add(new ClientRequest(3, 15, 25));
        requests.add(new ClientRequest(1, 100, 200));
        
        simulateConcurrentRequests(requests);
        
        // Shutdown executor
        clientExecutor.shutdown();
        try
        {
            if (!clientExecutor.awaitTermination(60, TimeUnit.SECONDS))
            {
                clientExecutor.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            clientExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("\nClientSimulator finished successfully");
    }
}
