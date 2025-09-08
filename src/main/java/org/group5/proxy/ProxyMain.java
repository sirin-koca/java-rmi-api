package org.group5.proxy;

import org.group5.api.ProxyService;
import org.group5.api.ServerEndpoint;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyMain implements ProxyService {

    private final Map<Integer, ServerEndpoint> zoneMap = new ConcurrentHashMap<>();
    private final AtomicInteger nextZone = new AtomicInteger(1);

    @Override
    public synchronized ServerEndpoint registerServer(String host, int registryPort, String suggestedBindingName)
            throws RemoteException {
        int assignedZone = nextZone.getAndIncrement(); // ascending order
        String bind = suggestedBindingName != null ? suggestedBindingName : ("stats-server-" + assignedZone);
        ServerEndpoint ep = new ServerEndpoint(host, registryPort, bind, assignedZone);
        zoneMap.put(assignedZone, ep);
        System.out.printf("Registered server %s at %s:%d as Zone %d%n", bind, host, registryPort, assignedZone);
        return ep;
    }

    @Override
    public ServerEndpoint getServerForZone(int zone) throws RemoteException {
        // Minimal behavior: return exact zone if present; (LB/clockwise comes later)
        return zoneMap.get(zone);
    }

    public static void main(String[] args) throws Exception {
        int registryPort = 1099; // default
        try { LocateRegistry.createRegistry(registryPort); } catch (Exception ignore) { }
        ProxyMain proxy = new ProxyMain();
        ProxyService stub = (ProxyService) UnicastRemoteObject.exportObject(proxy, 0);
        Registry reg = LocateRegistry.getRegistry(registryPort);
        reg.rebind("proxy", stub);
        System.out.println("Proxy started on port " + registryPort + " (binding 'proxy').");
    }
}
