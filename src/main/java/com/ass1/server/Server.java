package com.ass1.server;

import com.ass1.common.LoggerConfig;
import com.ass1.common.ComputationCache;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Server implements ServerInterface
{
    private static final Logger logger = LoggerConfig.getSimpleLogger(Server.class);
    private static final int CACHE_SIZE = 3;
    
    // Configuration flags
    private static boolean cacheEnabled = false;
    private static boolean useLRU = false;
    
    // Server-side cache using
    private final ComputationCache cache;
    
    public Server()
    {
        if (cacheEnabled)
        {
            cache = new ComputationCache(CACHE_SIZE, useLRU, "Server", logger);
        } else
        {
            cache = null;
        }
    }
    
    public int Add(int num1, int num2)
    {
        // If cache is disabled, perform computation directly
        if (!cacheEnabled || cache == null)
        {
            int result = num1 + num2;
            logger.info("Computed (no cache): " + num1 + "+" + num2 + " = " + result);
            return result;
        }
        
        // Create a unique key for the computation
        String key = num1 + "+" + num2;
        
        // Check if result is already in cache
        Integer cachedResult = cache.get(key);
        if (cachedResult != null)
        {
            return cachedResult;
        }
        
        // Cache miss - perform computation
        logger.info("Server cache miss for: " + key + " - performing computation");
        int result = num1 + num2;
        
        // Store in cache
        cache.put(key, result);
        
        return result;
    }
    
    
    private static void handleServerStartupError(Exception e)
    {
        if (e instanceof RemoteException)
        {
            logger.log(Level.SEVERE, "Failed to start RMI server due to remote communication error", e);
            System.err.println("Error: Unable to start server. Please check if RMI registry is running.");
        } else if (e instanceof AlreadyBoundException)
        {
            logger.log(Level.SEVERE, "Server name 'server' is already bound in registry", e);
            System.err.println("Error: Server is already running or name is already in use.");
        } else
        {
            logger.log(Level.SEVERE, "Unexpected error during server startup", e);
            System.err.println("Error: Unexpected error occurred during server startup.");
        }
        System.exit(1);
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
        System.out.println("Usage: java Server [OPTIONS]");
        System.out.println("Options:");
        System.out.println("  --enable-cache    Enable server-side caching (default: false)");
        System.out.println("  --use-lru         Use LRU eviction policy instead of FIFO (default: false)");
        System.out.println("  --help            Show this help message");
        System.out.println();
        System.out.println("Note: --use-lru only takes effect when --enable-cache is also specified");
    }
    
    public static void main(String[] args)
    {
        parseCommandLineArgs(args);
        
        // Log configuration
        logger.info("Server configuration: cache=" + cacheEnabled + ", useLRU=" + useLRU);
        
        try
        {
            // Start RMI registry programmatically
            Registry registry = LocateRegistry.createRegistry(1099);
            
            Server server = new Server();
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            registry.bind("server", serverStub);
            
            logger.info("Server started successfully and bound to registry");
            logger.info("Server is ready and waiting for clients...");
            
            // Keep the server running
            Thread.currentThread().join();
        } catch (RemoteException | AlreadyBoundException | InterruptedException e)
        {
            handleServerStartupError(e);
        }
    }
}
