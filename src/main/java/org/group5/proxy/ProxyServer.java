package org.group5.proxy;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.util.concurrent.*;

import org.group5.common.LoggerConfig;
import org.group5.server.ServerInterface;

public class ProxyServer implements ProxyServerInterface
{
    private static final Logger logger = LoggerConfig.getSimpleLogger(ProxyServer.class);
    private static final int OVERLOAD_THRESHOLD = 18;
    private static final int UPDATE_INTERVAL = 18;
    private final AtomicInteger nextZoneId;
    
    // Data structure for existing servers - thread-safe collections
    private final ConcurrentHashMap<String, ServerInfo> registeredServers;
    private final ConcurrentHashMap<Integer, CopyOnWriteArrayList<ServerInfo>> serversByZone;
    
    // Load balancing structures
    private final ConcurrentHashMap<String, ServerInterface> serverInterfaces;
    private final ConcurrentHashMap<String, Integer> serverQueueSizes;
    private final ConcurrentHashMap<String, AtomicInteger> serverAssignmentCounters;
    private final ScheduledExecutorService loadUpdateScheduler;
    private final Registry registry;
    
    public ProxyServer() throws RemoteException
    {
        this.registeredServers = new ConcurrentHashMap<>();
        this.serversByZone = new ConcurrentHashMap<>();
        this.nextZoneId = new AtomicInteger(1);
        
        // Initialize load balancing structures
        this.serverInterfaces = new ConcurrentHashMap<>();
        this.serverQueueSizes = new ConcurrentHashMap<>();
        this.serverAssignmentCounters = new ConcurrentHashMap<>();
        this.loadUpdateScheduler = Executors.newScheduledThreadPool(2);
        
        // Get registry reference for looking up server interfaces
        try
        {
            this.registry = LocateRegistry.getRegistry("localhost", 1099);
        }
        catch (RemoteException e)
        {
            throw new RemoteException("Failed to get registry", e);
        }
    }
    
    @Override
    public ServerInfo requestProcessingServer(int zone) throws RemoteException
    {
        // First try to find a server in the exact zone
        ServerInfo server = findServerInZone(zone);
        if (server != null && !isServerOverloaded(server.getServerId()))
        {
//            logger.info("Found non-overloaded server in exact zone " + zone + ": " + server.getServerId());
            incrementAndCheckUpdateNeeded(server.getServerId());
            return server;
        }
        
        // If server in exact zone is overloaded, find the server with least queue size
        ServerInfo leastLoadedServer = findLeastLoadedServer(zone);
        if (leastLoadedServer != null)
        {
//            logger.info("Found least loaded server for zone " + zone + " in zone " +
//                    leastLoadedServer.getZone() + ": " + leastLoadedServer.getServerId() +
//                    " with queue size: " + serverQueueSizes.get(leastLoadedServer.getServerId()));
            incrementAndCheckUpdateNeeded(leastLoadedServer.getServerId());
            return leastLoadedServer;
        }
        
        // If all servers are overloaded, return the server in the same zone if it exists
        if (server != null)
        {
//            logger.info("All servers overloaded, returning server in same zone: " + server.getServerId());
            incrementAndCheckUpdateNeeded(server.getServerId());
            return server;
        }
        
        // No servers available
//        logger.warning("No servers available for zone " + zone);
        throw new RemoteException("No processing servers available");
    }
    
    @Override
    public void registerServer(ServerInfo serverInfo) throws RemoteException
    {
        logger.info("Registering server: " + serverInfo);
        
        // Add to main registry
        registeredServers.put(serverInfo.getServerId(), serverInfo);
        
        // Add to zone-specific registry
        serversByZone.computeIfAbsent(serverInfo.getZone(), k -> new CopyOnWriteArrayList<>()).add(serverInfo);
        
        // Initialize assignment counter for this server
        serverAssignmentCounters.put(serverInfo.getServerId(), new AtomicInteger(0));
        
        // Initialize queue size to 0
        serverQueueSizes.put(serverInfo.getServerId(), 0);
        
        // Get server interface and perform initial load update - ASYNCHRONOUSLY
        loadUpdateScheduler.execute(() -> {
            try
            {
                ServerInterface serverInterface = (ServerInterface) registry.lookup(serverInfo.getServerId());
                serverInterfaces.put(serverInfo.getServerId(), serverInterface);
                updateServerLoad(serverInfo.getServerId());
            }
            catch (Exception e)
            {
                logger.warning("Failed to get initial server interface for " + serverInfo.getServerId() + ": " + e.getMessage());
            }
        });
        
        logger.info("Server registered successfully. Total servers: " + registeredServers.size());
    }
    
    @Override
    public void unregisterServer(String serverId) throws RemoteException
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
            
            // Clean up load balancing structures
            serverInterfaces.remove(serverId);
            serverQueueSizes.remove(serverId);
            serverAssignmentCounters.remove(serverId);
            
            logger.info("Server unregistered successfully. Remaining servers: " + registeredServers.size());
        }
        else
        {
            logger.warning("Attempted to unregister unknown server: " + serverId);
        }
    }
    
    @Override
    public int assignZoneNumber(String serverId) throws RemoteException
    {
        logger.info("Assigning zone number to server: " + serverId);
        
        int assignedZone = nextZoneId.getAndIncrement();
        
        logger.info("Assigned zone " + assignedZone + " to server: " + serverId);
        return assignedZone;
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
    
    private boolean isServerOverloaded(String serverId)
    {
        Integer queueSize = serverQueueSizes.get(serverId);
        return queueSize != null && queueSize >= OVERLOAD_THRESHOLD;
    }
    
    private ServerInfo findLeastLoadedServer(int targetZone)
    {
        ServerInfo bestServer = null;
        int minQueueSize = Integer.MAX_VALUE;
        int minDistance = Integer.MAX_VALUE;
        
        // Calculate number of zones (equal to number of servers)
        int numZones = nextZoneId.get() - 1;
        if (numZones <= 0) return null; // No servers registered
        
        // Find server with minimum queue size
        for (ServerInfo server : registeredServers.values())
        {
            Integer queueSize = serverQueueSizes.get(server.getServerId());
            if (queueSize == null)
            {
                queueSize = Integer.MAX_VALUE;
            }
            
            // Skip if this server is more loaded than current best
            if (queueSize > minQueueSize)
            {
                continue;
            }
            
            int serverZone = server.getZone();
            
            // Calculate clockwise distance
            int clockwiseDistance = ((serverZone - targetZone) + numZones) % numZones;
            
            // Update best server if:
            // 1. This server has lower queue size, OR
            // 2. Same queue size but closer (clockwise priority)
            if (queueSize < minQueueSize ||
                    (queueSize == minQueueSize && clockwiseDistance < minDistance))
            {
                minQueueSize = queueSize;
                minDistance = clockwiseDistance;
                bestServer = server;
            }
        }
        
        // Only return if the best server found is not overloaded
        if (bestServer != null && minQueueSize < OVERLOAD_THRESHOLD)
        {
            return bestServer;
        }
        
        return null;
    }
    
    private void incrementAndCheckUpdateNeeded(String serverId)
    {
        AtomicInteger counter = serverAssignmentCounters.get(serverId);
        if (counter != null)
        {
            int count = counter.incrementAndGet();
            if (count >= UPDATE_INTERVAL)
            {
                counter.set(0);
                // Schedule asynchronous update to not block the request
                loadUpdateScheduler.execute(() -> updateServerLoad(serverId));
            }
        }
    }
    
    private void updateServerLoad(String serverId)
    {
        try
        {
            ServerInterface serverInterface = serverInterfaces.get(serverId);
            if (serverInterface == null)
            {
                // Try to get it if we don't have it yet
                serverInterface = (ServerInterface) registry.lookup(serverId);
                if (serverInterface != null)
                {
                    serverInterfaces.put(serverId, serverInterface);
                }
            }
            
            if (serverInterface != null)
            {
                int queueSize = serverInterface.queueSize();
                serverQueueSizes.put(serverId, queueSize);
                logger.info("Updated queue size for server " + serverId + ": " + queueSize);
            }
        }
        catch (Exception e)
        {
            logger.warning("Failed to update load for server " + serverId + ": " + e.getMessage());
            // Set a high queue size to avoid routing to unreachable server
            serverQueueSizes.put(serverId, Integer.MAX_VALUE);
        }
    }
    
    public void printServerStatus()
    {
        logger.info("=== Proxy Status ===");
        logger.info("Total registered servers: " + registeredServers.size());
        for (ServerInfo server : registeredServers.values())
        {
            Integer queueSize = serverQueueSizes.get(server.getServerId());
            String queueInfo = (queueSize != null) ? ", Queue Size: " + queueSize : ", Queue Size: Unknown";
            logger.info("  " + server + queueInfo);
        }
        logger.info("Zones with servers: " + serversByZone.keySet());
    }
    
    private void shutdown()
    {
        loadUpdateScheduler.shutdown();
        try
        {
            if (!loadUpdateScheduler.awaitTermination(5, TimeUnit.SECONDS))
            {
                loadUpdateScheduler.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            loadUpdateScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
            
            // Add shutdown hook to clean up resources
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down proxy server...");
                proxyServer.shutdown();
            }));
            
            // Keep the proxy running
            Thread.currentThread().join();
        }
        catch (RemoteException | AlreadyBoundException | InterruptedException e)
        {
            handleProxyStartupError(e);
        }
    }
}
