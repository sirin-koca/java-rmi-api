package com.ass1.proxy;

import com.ass1.common.LoggerConfig;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ProxyServer implements ProxyServerInterface
{
    private static final Logger logger = LoggerConfig.getSimpleLogger(ProxyServer.class);
    
    // Data structure for existing servers - thread-safe collections
    private final ConcurrentHashMap<String, ServerInfo> registeredServers;
    private final ConcurrentHashMap<Integer, CopyOnWriteArrayList<ServerInfo>> serversByZone;
    
    public ProxyServer()
    {
        this.registeredServers = new ConcurrentHashMap<>();
        this.serversByZone = new ConcurrentHashMap<>();
    }
    
    @Override
    public ServerInfo requestProcessingServer(int zone) throws RemoteException
    {
        logger.info("Received request for processing server in zone " + zone);
        
        // First try to find a server in the exact zone
        ServerInfo server = findServerInZone(zone);
        if (server != null)
        {
            logger.info("Found server in exact zone " + zone + ": " + server.getServerId());
            return server;
        }
        
        // If no server in exact zone, find the closest zone
        server = findClosestServer(zone);
        if (server != null)
        {
            logger.info("Found closest server for zone " + zone + " in zone " + server.getZone() + ": " + server.getServerId());
            return server;
        }
        
        // No servers available
        logger.warning("No servers available for zone " + zone);
        throw new RemoteException("No processing servers available");
    }
    
    @Override
    public synchronized void registerServer(ServerInfo serverInfo) throws RemoteException
    {
        logger.info("Registering server: " + serverInfo);
        
        // Add to main registry
        registeredServers.put(serverInfo.getServerId(), serverInfo);
        
        // Add to zone-specific registry
        serversByZone.computeIfAbsent(serverInfo.getZone(), k -> new CopyOnWriteArrayList<>()).add(serverInfo);
        
        logger.info("Server registered successfully. Total servers: " + registeredServers.size());
    }
    
    @Override
    public synchronized void unregisterServer(String serverId) throws RemoteException
    {
        logger.info("Unregistering server: " + serverId);
        
        ServerInfo serverInfo = registeredServers.remove(serverId);
        if (serverInfo != null)
        {
            // Remove from zone-specific registry
            CopyOnWriteArrayList<ServerInfo> zoneServers = serversByZone.get(serverInfo.getZone());
            if (zoneServers != null)
            {
                zoneServers.remove(serverInfo);
                if (zoneServers.isEmpty())
                {
                    serversByZone.remove(serverInfo.getZone());
                }
            }
            logger.info("Server unregistered successfully. Remaining servers: " + registeredServers.size());
        }
        else
        {
            logger.warning("Attempted to unregister unknown server: " + serverId);
        }
    }
    
    private ServerInfo findServerInZone(int zone)
    {
        CopyOnWriteArrayList<ServerInfo> zoneServers = serversByZone.get(zone);
        if (zoneServers != null && !zoneServers.isEmpty())
        {
            // Return first available server in zone
            return zoneServers.getFirst();
        }
        return null;
    }
    
    private ServerInfo findClosestServer(int targetZone)
    {
        ServerInfo closestServer = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (ServerInfo server : registeredServers.values())
        {
            int distance = Math.abs(server.getZone() - targetZone);
            if (distance < minDistance)
            {
                minDistance = distance;
                closestServer = server;
            }
        }
        
        return closestServer;
    }
    
    public void printServerStatus()
    {
        logger.info("=== Proxy Status ===");
        logger.info("Total registered servers: " + registeredServers.size());
        for (ServerInfo server : registeredServers.values())
        {
            logger.info("  " + server);
        }
        logger.info("Zones with servers: " + serversByZone.keySet());
    }
    
    private static void handleProxyStartupError(Exception e)
    {
        if (e instanceof RemoteException)
        {
            logger.log(Level.SEVERE, "Failed to start RMI proxy server", e);
            System.err.println("Error: Unable to start proxy server. Please check if RMI registry is running.");
        }
        else if (e instanceof AlreadyBoundException)
        {
            logger.log(Level.SEVERE, "Proxy server name 'proxy' is already bound in registry", e);
            System.err.println("Error: Proxy server is already running.");
        }
        else
        {
            logger.log(Level.SEVERE, "Unexpected error during proxy startup", e);
            System.err.println("Error: Unexpected error occurred during proxy startup.");
        }
        System.exit(1);
    }
    
    public static void main(String[] args)
    {
        try
        {
            // Start RMI registry programmatically
            Registry registry = LocateRegistry.createRegistry(1099);
            
            ProxyServer proxyServer = new ProxyServer();
            ProxyServerInterface proxyStub = (ProxyServerInterface) UnicastRemoteObject.exportObject(proxyServer, 0);
            registry.bind("proxy", proxyStub);
            
            logger.info("Proxy server started successfully and bound to registry");
            logger.info("Proxy server is ready and waiting for server registrations...");
            
            // Periodically print proxy status
            Thread statusThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted())
                {
                    try
                    {
                        Thread.sleep(10000); // Every 10 seconds
                        proxyServer.printServerStatus();
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            statusThread.setDaemon(true);
            statusThread.start();
            
            // Keep the proxy running
            Thread.currentThread().join();
        }
        catch (RemoteException | AlreadyBoundException | InterruptedException e)
        {
            handleProxyStartupError(e);
        }
    }
}
