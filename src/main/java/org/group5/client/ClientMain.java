package org.group5.client;

import org.group5.api.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String country = (args.length > 0) ? args[0] : "Sweden";
        int zone = (args.length > 1) ? Integer.parseInt(args[1]) : 1;

        long t0 = System.nanoTime();

        Registry reg = LocateRegistry.getRegistry("localhost", 1099);
        ProxyService proxy = (ProxyService) reg.lookup("proxy");
        ServerEndpoint ep = proxy.getServerForZone(zone);
        if (ep == null) {
            System.err.println("No server registered for zone " + zone);
            return;
        }

        Registry srvReg = LocateRegistry.getRegistry(ep.host, ep.port);
        StatsServer server = (StatsServer) srvReg.lookup(ep.bindingName);

        StatsResult res = server.getPopulationofCountry(country);

        long t1 = System.nanoTime();
        long turnaroundMs = (t1 - t0) / 1_000_000;
        long execMs = res.getExecTimeMs();
        long waitingMs = Math.max(0, turnaroundMs - execMs); // no queue yet → near zero

        String inputQuery = "getPopulationofCountry " + country + " Zone:" + zone;
        String line = String.format("%d %s (turnaround time: %d ms, execution time: %d ms, waiting time: %d ms, processed by Server %d)",
                res.getValue(), inputQuery, turnaroundMs, execMs, waitingMs, ep.zone);

        System.out.println(line);

        // For the naive mode, write to naive_server.txt (as required by the spec)
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("naive_server.txt", true))) {
            bw.write(line);
            bw.newLine();
        }
    }
}
