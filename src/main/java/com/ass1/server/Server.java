package com.ass1.server;

import com.ass1.common.LoggerConfig;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.LinkedHashMap;
import java.util.Map;

public class Server implements ServerInterface
{
    private static final Logger logger = LoggerConfig.getSimpleLogger(Server.class);
    private static final int CACHE_SIZE = 3;
    
    // FIFO cache using LinkedHashMap with access order
    private final Map<String, Integer> cache = new LinkedHashMap<>(16, 0.75f, false)
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest)
        {
            boolean shouldRemove = size() > CACHE_SIZE;
            if (shouldRemove)
            {
                logger.info("Cache eviction: Removing eldest entry '" + eldest.getKey() + "' = " + eldest.getValue() + " (Policy: FIFO, Cache size exceeded: " + size() + " > " + CACHE_SIZE + ")");
            }
            return shouldRemove;
        }
    };
    
    public int Add(int num1, int num2)
    {
        // Create a unique key for the computation
        String key = num1 + "+" + num2;
        
        // Check if result is already in cache
        synchronized (cache)
        {
            if (cache.containsKey(key))
            {
                logger.info("Cache hit for computation: " + key);
                return cache.get(key);
            }
            
            // Perform computation
            int result = num1 + num2;
            
            // Store in cache
            cache.put(key, result);
            logger.info("Computed and cached: " + key + " = " + result + " (Cache size: " + cache.size() + ")");
            
            return result;
        }
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
    
    public static void main(String[] args)
    {
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
        
        try
        {
            Registry registry = LocateRegistry.getRegistry();
            Server server = new Server();
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            registry.bind("server", serverStub);
            logger.info("Server started successfully and bound to registry");
        } catch (RemoteException | AlreadyBoundException e)
        {
            handleServerStartupError(e);
        }
        
    }
}
