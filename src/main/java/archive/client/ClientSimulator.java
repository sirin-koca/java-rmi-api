package archive.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

import org.group5.common.ComputationCache;
import org.group5.common.LoggerConfig;
import org.group5.proxy.ProxyServerInterface;
import org.group5.proxy.ServerInfo;
import archive.server.ProcessingServerInterface;

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
    
    private static int remoteAdd(ProcessingServerInterface server, int num1, int num2) throws RemoteException
    {
        return server.add(num1, num2);
    }
    
    /**
     * Generates a cache key for the given operation
     */
    private static String generateCacheKey(int num1, int num2)
    {
        return num1 + "+" + num2;
    }
    
    private static void handleClientError(Exception e)
    {
        if (e instanceof RemoteException)
        {
            logger.log(Level.SEVERE, "Failed to connect to RMI server", e);
        }
        else if (e instanceof NotBoundException)
        {
            logger.log(Level.SEVERE, "Server 'server' not found in registry", e);
        }
        else
        {
            logger.log(Level.SEVERE, "Unexpected error during client operation", e);
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
     * Simulates a single independent client with a singular request to be fulfilled, it gets a dedicated connection
     * that is only used for this request
     *
     * @param clientZone The geographical zone of the simulated client
     * @return ServerInterface for the assigned server
     * @throws RemoteException, NotBoundException if connection fails
     */
    private static ProcessingServerInterface connectToServerForZone(int clientZone) throws RemoteException,
            NotBoundException
    {
        Registry registry = LocateRegistry.getRegistry();
        
        // First, contact the proxy to get server information
        ProxyServerInterface proxy = (ProxyServerInterface) registry.lookup("proxy");
        ServerInfo serverInfo = proxy.requestProcessingServer(clientZone);
        
        logger.info("Client from zone " + clientZone + " - Proxy assigned server: " + serverInfo);
        
        // Now connect to the assigned server
        ProcessingServerInterface server = (ProcessingServerInterface) registry.lookup(serverInfo.getRegistryName());
        logger.info("Client from zone " + clientZone +
                " - Successfully connected to processing server in zone " + serverInfo.getZone());
        
        return server;
    }
    
    /**
     * Simulates a client request from a specific zone First checks cache (if enabled), then connects to proxy/server if
     * needed
     */
    private static void simulateClientRequest(int clientZone, int num1, int num2)
    {
        try
        {
            logger.info("\n--- Simulating client from zone " + clientZone + " ---");
            
            int result;
            
            // Cache enabled and working
            if (cacheEnabled && clientCache != null)
            {
                // Generate cache key
                String cacheKey = generateCacheKey(num1, num2);
                
                // Check cache
                Integer cachedResult = clientCache.get(cacheKey);
                
                if (cachedResult != null)
                {
                    // Cache hit
                    logger.info("Cache: hit for \"" + cacheKey + "\" - no connection needed");
                    logger.info("Zone " + clientZone + " client: " + num1 + " + " + num2 +
                            " = " + cachedResult + " (from cache, no connection made)");
                    return;
                }
                
                // Cache miss - need to connect and compute
                logger.info("Cache: miss for \"" + cacheKey + "\" - connection required");
                logger.info("Zone " + clientZone + " client: Establishing connection for computation");
                
                // Connect to server through proxy for this specific zone
                ProcessingServerInterface server = connectToServerForZone(clientZone);
                
                // Perform the Add operation
                result = remoteAdd(server, num1, num2);
                
                // Store result in cache for future use
                clientCache.put(cacheKey, result);
                logger.info("Cache: stored result for \"" + cacheKey + "\"");
                
                logger.info("Zone " + clientZone + " client: " + num1 + " + " + num2 +
                        " = " + result + " (computed via RMI, cached)");
            }
            else
            {
                if (cacheEnabled)
                {
                    throw new NullPointerException("Client cache is null");
                }
                // Cache disabled - always connect and compute
                logger.info("Zone " + clientZone + " client: Establishing connection for computation (cache disabled)");
                
                // Connect to server through proxy for this specific zone
                ProcessingServerInterface server = connectToServerForZone(clientZone);
                
                // Perform the Add operation
                result = remoteAdd(server, num1, num2);
                
                logger.info("Zone " + clientZone + " client: " + num1 + " + " + num2 +
                        " = " + result + " (computed via RMI)");
            }
            
            // Connection is automatically disconnected when we exit this method
            // Next request will go through the proxy again (unless cached)
            
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
