package org.group5.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.logging.Logger;

import org.group5.common.ComputationCache;
import org.group5.common.LoggerConfig;
import org.group5.proxy.ProxyServerInterface;
import org.group5.proxy.ServerInfo;

import java.util.logging.Level;

public class ProcessingServer implements ProcessingServerInterface
{
    private static final int GEOGRAPHICAL_ZONE = 1;
    private static final Logger logger = LoggerConfig.getSimpleLogger(ProcessingServer.class);
    private static final int CACHE_SIZE = 3;
    
    // Configuration flags
    private static boolean cacheEnabled = false;
    private static boolean useLRU = false;
    
    // Server identification
    private static int serverZone = GEOGRAPHICAL_ZONE;
    private static String serverId;
    
    // Server-side cache using
    private final ComputationCache cache;
    
    public ProcessingServer()
    {
        if (cacheEnabled)
        {
            cache = new ComputationCache(CACHE_SIZE, useLRU, "Server", logger);
        }
        else
        {
            cache = null;
        }
    }
    
    @Override
    public int add(int num1, int num2)
    {
        int result;
        
        // Cache enabled and working
        if (cacheEnabled && cache != null)
        {
            String key = num1 + "+" + num2;
            
            // Check cache
            Integer cachedResult = cache.get(key);
            if (cachedResult != null)
            {
                return cachedResult;
            }
            
            // Cache miss - perform computation
            logger.info("Cache: miss for \"" + key + "\" - performing computation");
            result = num1 + num2;
            
            // Store in cache
            cache.put(key, result);
        }
        else
        {
            // Cache broken
            if (cacheEnabled)
            {
                throw new NullPointerException("Server cache is null");
            }
            
            // Cache disabled - return computation result directly
            result = num1 + num2;
        }
        
        // Return cached result (whether it was a hit or miss)
        return result;
    }
    
    private static void handleServerStartupError(Exception e)
    {
        if (e instanceof RemoteException)
        {
            logger.log(Level.SEVERE, "Failed to start RMI server due to remote communication error", e);
            System.err.println("Error: Unable to start server. Please check if RMI registry is running.");
        }
        else if (e instanceof AlreadyBoundException)
        {
            logger.log(Level.SEVERE, "Server name 'server' is already bound in registry", e);
            System.err.println("Error: Server is already running or name is already in use.");
        }
        else
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
                case "--zone":
                    if (i + 1 < args.length)
                    {
                        try
                        {
                            serverZone = Integer.parseInt(args[++i]);
                            logger.info("Server zone set to: " + serverZone);
                        }
                        catch (NumberFormatException e)
                        {
                            System.err.println("Invalid zone number: " + args[i]);
                            printUsage();
                            System.exit(1);
                        }
                    }
                    else
                    {
                        System.err.println("--zone requires a zone number");
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
    
    private static void printUsage()
    {
        System.out.println("Usage: java Server [OPTIONS]");
        System.out.println("Options:");
        System.out.println("  --enable-cache    Enable server-side caching (default: false)");
        System.out.println("  --use-lru         Use LRU eviction policy instead of FIFO (default: false)");
        System.out.println("  --zone <number>   Set geographical zone for this server (default: 1)");
        System.out.println("  --help            Show this help message");
        System.out.println();
        System.out.println("Note: --use-lru only takes effect when --enable-cache is also specified");
    }
    
    private static void registerWithProxy(String serverRegistryName) throws RemoteException
    {
        try
        {
            Registry registry = LocateRegistry.getRegistry();
            ProxyServerInterface proxy = (ProxyServerInterface) registry.lookup("proxy");
            
            ServerInfo serverInfo = new ServerInfo(
                    serverId,
                    serverRegistryName,
                    serverZone,
                    "localhost", // Configurable, works for our purposes
                    1099              // Potentially configurable as well
            );
            
            proxy.registerServer(serverInfo);
            logger.info("Successfully registered with proxy server");
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Failed to register with proxy server", e);
            System.err.println("Warning: Could not register with proxy server. Continuing without proxy registration.");
        }
    }
    
    public static void main(String[] args)
    {
        parseCommandLineArgs(args);
        
        // Generate unique server ID
        serverId = "zone" + serverZone + "-" + System.currentTimeMillis();
        
        // Log configuration
        logger.info("Server configuration: cache=" + cacheEnabled + ", useLRU=" + useLRU + ", zone=" + serverZone);
        
        try
        {
            // Get RMI registry
            Registry registry;
            try
            {
                registry = LocateRegistry.getRegistry();
            }
            catch (RemoteException e)
            {
                throw new RuntimeException("Failed to get RMI registry", e);
            }
            
            ProcessingServer processingServer = new ProcessingServer();
            ProcessingServerInterface serverStub =
                    (ProcessingServerInterface) UnicastRemoteObject.exportObject(processingServer, 0);
            
            // Use unique registry name based on server ID
            String registryName = "server-" + serverId;
            registry.bind(registryName, serverStub);
            
            logger.info("Server started successfully and bound to registry as: " + registryName);
            
            // Register with proxy server
            registerWithProxy(registryName);
            
            logger.info("Server is ready and waiting for clients...");
            
            // Keep the server running
            Thread.currentThread().join();
        }
        catch (RemoteException | AlreadyBoundException | InterruptedException e)
        {
            handleServerStartupError(e);
        }
    }
}
