package com.ass1.client;

import com.ass1.common.LoggerConfig;
import com.ass1.server.ServerInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Client
{
    private static final Logger logger = LoggerConfig.getSimpleLogger(Client.class);
    private static final int CACHE_SIZE = 3;
    
    // Client-side FIFO cache using LinkedHashMap
    private static final Map<String, Integer> clientCache = new LinkedHashMap<>(16, 0.75f, false)
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
    
    // Cached wrapper for Add method
    private static int cachedAdd(ServerInterface server, int num1, int num2) throws RemoteException
    {
        String key = num1 + "+" + num2;
        
        synchronized (clientCache)
        {
            // Check client cache first
            if (clientCache.containsKey(key))
            {
                logger.info("Client cache hit for: " + key + " = " + clientCache.get(key));
                return clientCache.get(key);
            }
            
            // Cache miss - make actual RMI call
            logger.info("Client cache miss for: " + key + " - making RMI call");
            int result = server.Add(num1, num2);
            
            // Store result in client cache
            clientCache.put(key, result);
            logger.info("Cached result in client: " + key + " = " + result + " (Client cache size: " + clientCache.size() + ")");
            
            return result;
        }
    }
    
    private static void handleClientError(Exception e)
    {
        if (e instanceof RemoteException)
        {
            logger.log(Level.SEVERE, "Failed to connect to RMI server", e);
            System.err.println("Error: Unable to connect to server. Please check if the server is running and " +
                    "accessible.");
        } else if (e instanceof NotBoundException)
        {
            logger.log(Level.SEVERE, "Server 'server' not found in registry", e);
            System.err.println("Error: Server not found. Please ensure the server is started and properly registered.");
        } else
        {
            logger.log(Level.SEVERE, "Unexpected error during client operation", e);
            System.err.println("Error: Unexpected error occurred while connecting to server.");
        }
    }
    
    public static void main(String[] args)
    {
        try
        {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface server = (ServerInterface) registry.lookup("server");
            
            System.out.println(cachedAdd(server, 10, 20));
            System.out.println(cachedAdd(server, 11, 20));
            System.out.println(cachedAdd(server, 10, 20));
            System.out.println(cachedAdd(server, 12, 20));
            System.out.println(cachedAdd(server, 12, 20));
            System.out.println(cachedAdd(server, 5, 5));
            System.out.println(cachedAdd(server, 10, 20));
            System.out.println(cachedAdd(server, 5, 5));
        } catch (RemoteException | NotBoundException e)
        {
            handleClientError(e);
        }
    }
}
