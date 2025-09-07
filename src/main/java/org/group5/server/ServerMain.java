package org.group5.server;

import org.group5.api.ProxyService;
import org.group5.api.ServerEndpoint;
import org.group5.api.StatsServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ServerMain {

    public static void main(String[] args) throws Exception {
        String dataset = "/dataset/exercise_1_dataset.csv"; // put CSV under src/main/resources/dataset/
        int registryPort = 1099;

        // Start RMI registry if not running
        try { LocateRegistry.createRegistry(registryPort); } catch (Exception ignore) { }

        // Export server
        StatsServerImpl impl = new StatsServerImpl(dataset);
        StatsServer stub = (StatsServer) UnicastRemoteObject.exportObject(impl, 0);

        // Bind with a temporary name until proxy assigns zone
        Registry reg = LocateRegistry.getRegistry(registryPort);
        String tempName = "stats-server-pending";
        reg.rebind(tempName, stub);

        // Register at proxy (assumes proxy on localhost:1099; adjust later if needed)
        ProxyService proxy = (ProxyService) reg.lookup("proxy");
        ServerEndpoint ep = proxy.registerServer("localhost", registryPort, null);

        // Rebind under the zone-specific name returned by proxy
        reg.unbind(tempName);
        reg.rebind(ep.bindingName, stub);

        System.out.printf("Stats server ready as %s on zone %d%n", ep.bindingName, ep.zone);
    }
}
