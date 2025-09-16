package org.group5.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.rmi.Naming;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import org.group5.common.ComputationCache;
import org.group5.common.LoggerConfig;
import org.group5.proxy.ProxyServerInterface;
import org.group5.proxy.ServerInfo;
import org.group5.server.ServerInterface;

public class Client {
    private static final Logger logger = LoggerConfig.getSimpleLogger(Client.class);
    private static final int CACHE_SIZE = 45;
    private static int delay = 50;
    private static String mode = "naive";
    
    // Configuration flags
    private static boolean cacheEnabled = false;
    private static boolean useLRU = false;
    
    // Client-side cache
    private static ComputationCache clientCache;
    
    // Concurrent request handling
    private static final ExecutorService requestExecutor = Executors.newFixedThreadPool(10);
    
    private static void initializeCache() {
        if (cacheEnabled) {
            clientCache = new ComputationCache(CACHE_SIZE, useLRU, "Client", logger);
        } else {
            clientCache = null;
        }
    }
    
    // Helper method to generate cache key from query
    private static String generateCacheKey(String method, String[] parts) {
        StringBuilder keyBuilder = new StringBuilder(method);
        for (int i = 1; i < parts.length; i++) {
            keyBuilder.append("_").append(parts[i]);
        }
        return keyBuilder.toString();
    }
    
    // Helper method to execute server call with caching
    private static String executeWithCache(String cacheKey, ServerInterface server,
                                           String method, String[] parts, int zone) throws Exception {
        // Check cache if enabled
        if (cacheEnabled && clientCache != null) {
            String cachedResult = clientCache.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }
        }
        
        // Execute the actual server call
        String result;
        switch (method) {
            case "getPopulationofCountry" -> {
                String countryName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                result = "Population=" + server.getPopulationofCountry(countryName, zone);
            }
            case "getNumberofCities" -> {
                long threshold = Long.parseLong(parts[parts.length - 2]);
                String countryName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1));
                result = "Cities=" + server.getNumberofCities(countryName, threshold, zone);
            }
            case "getNumberofCountries" -> {
                int cityCount = Integer.parseInt(parts[1]);
                long threshold = Long.parseLong(parts[2]);
                result = "Countries=" + server.getNumberofCountries(cityCount, threshold, zone);
            }
            case "getNumberofCountriesMM" -> {
                int cityCount = Integer.parseInt(parts[1]);
                long minPopulation = Long.parseLong(parts[2]);
                long maxPopulation = Long.parseLong(parts[3]);
                result = "Countries=" + server.getNumberofCountriesMM(cityCount, minPopulation, maxPopulation, zone);
            }
            default -> {
                throw new IllegalArgumentException("Unknown method: " + method);
            }
        }
        
        // Store in cache if enabled
        if (cacheEnabled && clientCache != null) {
            clientCache.put(cacheKey, result);
        }
        
        return result;
    }
    
    public static void main(String[] args) throws Exception {
        parseCommandLineArgs(args);
        initializeCache();
        
        class Stats {
            long totalTurnaround = 0;
            long totalExec = 0;
            long totalWait = 0;
            long minTurnaround = Long.MAX_VALUE;
            long maxTurnaround = Long.MIN_VALUE;
            int count = 0;
        }
        
        Map<String, Stats> methodStats = new ConcurrentHashMap<>();
        
        String inputFile = "src/main/resources/input/exercise_1_input.txt";
        String outputFile;
        switch (mode) {
            case "naive" -> outputFile = "src/main/resources/output/naive_server.txt";
            case "server-cache" -> outputFile = "src/main/resources/output/server_cache.txt";
            case "client-cache" -> outputFile = "src/main/resources/output/client_cache.txt";
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        }
        
        ProxyServerInterface proxy = (ProxyServerInterface) Naming.lookup("rmi://localhost:1099/proxy");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
        
        // Use ConcurrentLinkedQueue to collect results in order
        ConcurrentLinkedQueue<CompletableFuture<String>> resultFutures = new ConcurrentLinkedQueue<>();
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            int index = 0;
            
            while ((line = br.readLine()) != null) {
                final String query = line.trim();
                final int currentIndex = index++;
                
                // Schedule request submission
                scheduler.schedule(() -> {
                    // Create a future for this request
                    CompletableFuture<String> resultFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            // Validate and split line
                            if (!query.contains("Zone:")) {
                                return "ERROR: Skipping malformed query: " + query;
                            }
                            
                            // Extract method and args first
                            String head = query.split("Zone:")[0].trim();
                            String[] parts = head.split("\\s+");
                            if (parts.length < 2) {
                                return "ERROR: Skipping malformed query: " + query;
                            }
                            
                            String method = parts[0];
                            
                            // Start timing for total turnaround time
                            long startTurnaround = System.currentTimeMillis();
                            
                            // Generate cache key (zone-independent)
                            String cacheKey = generateCacheKey(method, parts);
                            
                            // Check cache first if enabled
                            String result = null;
                            String serverURL = null;
                            long execTime = 0, turnaround = 0, waitTime = 0;
                            
                            if (cacheEnabled && clientCache != null) {
                                result = clientCache.get(cacheKey);
                                if (result != null) {
                                    serverURL = "CLIENT-CACHE";
                                    turnaround = System.currentTimeMillis() - startTurnaround;
                                    
                                    // Update stats for cache hit
                                    synchronized (methodStats) {
                                        Stats stats = methodStats.computeIfAbsent(method, k -> new Stats());
                                        stats.totalTurnaround += turnaround;
                                        stats.totalExec += execTime;
                                        stats.totalWait += waitTime;
                                        stats.count++;
                                        stats.minTurnaround = Math.min(stats.minTurnaround, turnaround);
                                        stats.maxTurnaround = Math.max(stats.maxTurnaround, turnaround);
                                    }
                                    
                                    return result + " " + query +
                                            " (turnaround time: " + turnaround +
                                            " ms, execution time: " + execTime +
                                            " ms, waiting time: " + waitTime +
                                            " ms, processed by CLIENT-CACHE)";
                                }
                            }
                            
                            // If not in cache, proceed with server lookup
                            int zone = Integer.parseInt(query.split("Zone:")[1].trim());
                            ServerInfo serverInfo = proxy.requestProcessingServer(zone);
                            serverURL = "rmi://localhost:1099/" + serverInfo.getRegistryName();
                            ServerInterface server = (ServerInterface) Naming.lookup(serverURL);
                            
                            long startExecution = System.currentTimeMillis();
                            result = executeWithCache(cacheKey, server, method, parts, zone);
                            long endExecution = System.currentTimeMillis();
                            long endTurnaround = System.currentTimeMillis();
                            
                            execTime = endExecution - startExecution;
                            turnaround = endTurnaround - startTurnaround;
                            waitTime = turnaround - execTime;
                            
                            // Update stats
                            synchronized (methodStats) {
                                Stats stats = methodStats.computeIfAbsent(method, k -> new Stats());
                                stats.totalTurnaround += turnaround;
                                stats.totalExec += execTime;
                                stats.totalWait += waitTime;
                                stats.count++;
                                stats.minTurnaround = Math.min(stats.minTurnaround, turnaround);
                                stats.maxTurnaround = Math.max(stats.maxTurnaround, turnaround);
                            }
                            
                            return result + " " + query +
                                    " (turnaround time: " + turnaround +
                                    " ms, execution time: " + execTime +
                                    " ms, waiting time: " + waitTime +
                                    " ms, processed by Server " + serverInfo.getZone() + ")";
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                            return "ERROR: " + e.getMessage() + " for query: " + query;
                        }
                    }, requestExecutor);
                    
                    // Add future to queue without waiting for completion
                    resultFutures.add(resultFuture);
                    
                }, (long) delay * currentIndex, TimeUnit.MILLISECONDS);
            }
        }
        
        // Shutdown the scheduler - no more requests will be submitted
        scheduler.shutdown();
        
        try {
            // Wait for all requests to be scheduled
            if (!scheduler.awaitTermination(10, TimeUnit.MINUTES)) {
                System.err.println("Some requests were not scheduled within timeout");
                scheduler.shutdownNow();
            }
            
            // Now process all results in order
            System.out.println("All requests submitted. Waiting for " + resultFutures.size() + " responses...");
            
            for (CompletableFuture<String> future : resultFutures) {
                try {
                    // Wait for this specific result
                    String result = future.get(5, TimeUnit.MINUTES);
                    if (!result.startsWith("ERROR:")) {
                        writer.write(result + "\n");
                    } else {
                        System.err.println(result);
                    }
                } catch (TimeoutException e) {
                    System.err.println("Request timed out");
                } catch (Exception e) {
                    System.err.println("Error processing request: " + e.getMessage());
                }
            }
            
            // Write summary statistics
            for (Map.Entry<String, Stats> entry : methodStats.entrySet()) {
                String m = entry.getKey();
                Stats s = entry.getValue();
                long avgTurnaround = s.count > 0 ? s.totalTurnaround / s.count : 0;
                long avgExec = s.count > 0 ? s.totalExec / s.count : 0;
                long avgWait = s.count > 0 ? s.totalWait / s.count : 0;
                
                writer.write(m + " avg turn-around time: " + avgTurnaround +
                        " ms, avg execution time: " + avgExec +
                        " ms, avg waiting time: " + avgWait +
                        " ms, min turn-around time: " + s.minTurnaround +
                        " ms, max turn-around time: " + s.maxTurnaround + " ms\n");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Cleanup
            writer.close();
            requestExecutor.shutdown();
            try {
                if (!requestExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    requestExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                requestExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java Client [OPTIONS]");
        System.out.println("Options:");
        System.out.println("  --mode <naive|server-cache|client-cache>  Select run mode (default: naive)");
        System.out.println("  --enable-cache    Enable client-side caching (default: false)");
        System.out.println("  --use-lru         Use LRU eviction policy instead of FIFO (default: false)");
        System.out.println("  --delay <N>       The delay between each request in milliseconds (default: 50)");
        System.out.println("  --help            Show this help message");
        System.out.println();
        System.out.println("Note: --use-lru only takes effect when --enable-cache is also specified");
    }
    
    private static void parseCommandLineArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--enable-cache":
                    cacheEnabled = true;
                    logger.info("Cache enabled");
                    break;
                case "--use-lru":
                    useLRU = true;
                    logger.info("LRU cache policy selected");
                    break;
                case "--mode":
                    if (i + 1 < args.length) {
                        mode = args[++i].toLowerCase();
                        logger.info("Run mode set to " + mode);
                        // Configure cache based on mode
                        switch (mode) {
                            case "naive":
                                cacheEnabled = false;
                                break;
                            case "server-cache":
                                cacheEnabled = false;
                                break;
                            case "client-cache":
                                cacheEnabled = true;
                                break;
                            default:
                                System.err.println("Unknown mode: " + mode);
                                printUsage();
                                System.exit(1);
                        }
                    } else {
                        System.err.println("--mode requires a value");
                        printUsage();
                        System.exit(1);
                    }
                    break;
                case "--delay":
                    if (i + 1 < args.length) {
                        try {
                            delay = Integer.parseInt(args[i + 1]);
                            logger.info("Delay set to " + delay + "ms");
                            i++;
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid delay value: " + args[i + 1]);
                            printUsage();
                            System.exit(1);
                        }
                    } else {
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
                    if (arg.startsWith("--")) {
                        System.err.println("Unknown flag: " + arg);
                        printUsage();
                        System.exit(1);
                    }
                    break;
            }
        }
    }
}
