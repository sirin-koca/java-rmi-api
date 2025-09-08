package org.group5.client;

import org.group5.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.regex.*;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        // 1) Read one query line (from CLI args or prompt)
        String line;
        if (args.length >= 1) {
            line = args[0];  // full query passed as one string in quotes
        } else {
            System.out.print("Query> ");
            line = new BufferedReader(new InputStreamReader(System.in)).readLine();
        }

        // 2) Parse exactly: getPopulationofCountry <Country> Zone:<Z>
        Pattern POP = Pattern.compile("^getPopulationofCountry\\s+(.+?)\\s+Zone:(\\d+)\\s*$");
        Matcher m = POP.matcher(line);
        if (!m.matches()) {
            System.err.println("Unsupported input. Expected: getPopulationofCountry <Country> Zone:<Z>");
            return;
        }
        String country = m.group(1);
        int zone = Integer.parseInt(m.group(2));

        long t0 = System.nanoTime();

        var reg = java.rmi.registry.LocateRegistry.getRegistry("localhost", 1099);
        var proxy = (org.group5.api.ProxyService) reg.lookup("proxy");
        var ep = proxy.getServerForZone(zone);
        if (ep == null) { System.err.println("No server registered for zone " + zone); return; }

        var srvReg = java.rmi.registry.LocateRegistry.getRegistry(ep.host, ep.port);
        var server = (org.group5.api.StatsServer) srvReg.lookup(ep.bindingName);
        var res = server.getPopulationofCountry(country);

        long turnaroundMs = (System.nanoTime() - t0)/1_000_000;
        long execMs = res.getExecTimeMs();
        long waitingMs = Math.max(0, turnaroundMs - execMs);

        String out = String.format("%d %s (turnaround time: %d ms, execution time: %d ms, waiting time: %d ms, processed by Server %d)",
                res.getValue(), line, turnaroundMs, execMs, waitingMs, ep.zone);

        System.out.println(out);
        Files.writeString(Paths.get("naive_server.txt"), out + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
