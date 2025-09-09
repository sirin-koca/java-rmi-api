package com.ass1.client;

import com.ass1.common.LoggerConfig;
import com.ass1.common.ComputationCache;

import com.ass1.server.ServerInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ClientSimulator
{
    private static final Logger logger = LoggerConfig.getSimpleLogger(ClientSimulator.class);
    private static final int CACHE_SIZE = 3;
    
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
    
    // Cached wrapper for Add method
    private static int RemoteAdd(ServerInterface server, int geo_zone, int num1, int num2) throws RemoteException
    {
        int result = -1;
        
        // Cache enabled and working
        if (cacheEnabled && clientCache != null)
        {
            String key = num1 + "+" + num2;
            
            // Check cache
            Integer cachedResult = clientCache.get(key);
            if (cachedResult != null)
            {
                return cachedResult;
            }
            
            // Cache miss - make RMI call
            logger.info("Cache: miss for \"" + key + "\" - making RMI call");
            result = server.Add(num1, num2);
            
            // Store result in cache
            clientCache.put(key, result);
        }
        else
        {
            // Cache broken
            if (clientCache == null)
            {
                throw new NullPointerException("Client cache is null");
            }
            
            // Cache disabled - make RMI call and return result
            return server.Add(num1, num2);
        }
        
        // Return cached result (whether it was a hit or miss)
        return result;
    }
    
    
    private static void handleClientError(Exception e)
    {
        if (e instanceof RemoteException)
        {
            logger.log(Level.SEVERE, "Failed to connect to RMI server", e);
            System.err.println("Error: Unable to connect to server. Please " + "check if the server is running and " + "accessible.");
        }
        else if (e instanceof NotBoundException)
        {
            logger.log(Level.SEVERE, "Server 'server' not found in registry", e);
            System.err.println("Error: Server not found. Please ensure the " + "server is started and properly " +
                    "registered.");
        }
        else
        {
            logger.log(Level.SEVERE, "Unexpected error during client " + "operation", e);
            System.err.println("Error: Unexpected error occurred while " + "connecting to server.");
        }
    }
    
    private static void parseCommandLineArgs(String[] args)
    {
        // It's reasonable to expect the Client and Server to potentially have different command line arguments,
        // so making a common method doesn't necessarily make sense.
        //noinspection DuplicatedCode
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
        System.out.println("Usage: java Client [OPTIONS]");
        System.out.println("Options:");
        System.out.println("  --enable-cache    Enable client-side caching (default: false)");
        System.out.println("  --use-lru         Use LRU eviction policy instead of FIFO (default: false)");
        System.out.println("  --help            Show this help message");
        System.out.println();
        System.out.println("Note: --use-lru only takes effect when --enable-cache is also specified");
    }
    
    public static void main(String[] args)
    {
        parseCommandLineArgs(args);
        initializeCache();
        
        // Log configuration
        logger.info("Client configuration: cache=" + cacheEnabled + ", useLRU=" + useLRU);
        
        try
        {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface server = (ServerInterface) registry.lookup("server");
            
            System.out.println(RemoteAdd(server, 1, 10, 20));
            System.out.println(RemoteAdd(server, 1, 11, 20));
            System.out.println(RemoteAdd(server, 1, 10, 20));
            System.out.println(RemoteAdd(server, 1, 12, 20));
            System.out.println(RemoteAdd(server, 1, 12, 20));
            System.out.println(RemoteAdd(server, 1, 5, 5));
            System.out.println(RemoteAdd(server, 1, 10, 20));
            System.out.println(RemoteAdd(server, 1, 5, 5));
        } catch (RemoteException | NotBoundException e)
        {
            handleClientError(e);
        }
    }
}
