package com.ass1.client;

import com.ass1.common.LoggerConfig;
import com.ass1.common.ComputationCache;

import com.ass1.proxy.ProxyServerInterface;
import com.ass1.proxy.ServerInfo;
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
    private static int RemoteAdd(ServerInterface server, int num1, int num2) throws RemoteException
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
            if (cacheEnabled)
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
    
    /**
     * Connects to a processing server through the proxy for a specific zone
     *
     * @param clientZone The geographical zone of the simulated client
     * @return ServerInterface for the assigned server
     * @throws RemoteException, NotBoundException if connection fails
     */
    private static ServerInterface connectToServerForZone(int clientZone) throws RemoteException, NotBoundException
    {
        Registry registry = LocateRegistry.getRegistry();
        
        // First, contact the proxy to get server information
        ProxyServerInterface proxy = (ProxyServerInterface) registry.lookup("proxy");
        ServerInfo serverInfo = proxy.requestProcessingServer(clientZone);
        
        logger.info("Client from zone " + clientZone + " - Proxy assigned server: " + serverInfo);
        
        // Now connect to the assigned server
        ServerInterface server = (ServerInterface) registry.lookup(serverInfo.getRegistryName());
        logger.info("Client from zone " + clientZone + " - Successfully connected to processing server in zone " + serverInfo.getZone());
        
        return server;
    }
    
    /**
     * Simulates a client request from a specific zone Connects to proxy, gets assigned server, performs one Add
     * operation, then disconnects
     */
    private static void simulateClientRequest(int clientZone, int num1, int num2)
    {
        try
        {
            logger.info("\n--- Simulating client from zone " + clientZone + " ---");
            
            // Connect to server through proxy for this specific zone
            ServerInterface server = connectToServerForZone(clientZone);
            
            // Perform one Add operation as specified in requirements
            int result = RemoteAdd(server, num1, num2);
            logger.info("Zone " + clientZone + " client: " + num1 + " + " + num2 + " = " + result);
            
            // Connection is automatically disconnected when we exit this method
            // Next request will go through the proxy again
            
        }
        catch (RemoteException | NotBoundException e)
        {
            logger.warning("Error for client in zone " + clientZone + ":");
            handleClientError(e);
        }
    }
    
    
    public static void main(String[] args)
    {
        parseCommandLineArgs(args);
        initializeCache();
        
        // Log configuration
        logger.info("ClientSimulator configuration: cache=" + cacheEnabled + ", useLRU=" + useLRU);
        
        // Simulate multiple clients from different geographical zones
        simulateClientRequest(1, 10, 20);
        simulateClientRequest(3, 15, 25);
        simulateClientRequest(1, 10, 20);
        simulateClientRequest(5, 30, 40);
        simulateClientRequest(2, 12, 18);
        simulateClientRequest(1, 5, 5);
        simulateClientRequest(4, 20, 30);
        simulateClientRequest(2, 10, 20);
        
        logger.info("ClientSimulator finished successfully");
    }
    
}
