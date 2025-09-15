package org.group5.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.rmi.Naming;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.group5.proxy.ProxyServerInterface;
import org.group5.proxy.ServerInfo;
import org.group5.server.ServerInterface;

public class Client {
    public static void main(String[] args) throws Exception {
        String inputFile = "src/main/resources/dataset/exercise_1_input.txt";
        String outputFile = "src/main/resources/dataset/exercise_1_output.txt"; //Results

        //Client connects to proxy via RMI 
        ProxyServerInterface proxy = (ProxyServerInterface) Naming.lookup("rmi://localhost:1099/proxy");
        
        //Adds delay so not all requests get sent at once
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        //Writes results to output file
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            //Small delay added between each query 
            int delay = 50; //change to 20 in second run 
            int index = 0;

            while ((line = br.readLine()) != null) {
                final String query = line;
                final int currentIndex = index++;

                scheduler.schedule(() -> {
                    try {
                        //Parse zone from query
                        int zone = Integer.parseInt(query.split("Zone:")[1].trim());
                        ServerInfo serverInfo = proxy.requestProcessingServer(zone);
                        String serverURL = "rmi://localhost:1099/" + serverInfo.getRegistryName();

                        ServerInterface server = (ServerInterface) Naming.lookup(serverURL);
                        //Will call methods defined by group inn here 

                        String head = query.split("Zone:")[0].trim();
                        String[] parts = head.split("\\s+");
                        String method = parts[0];
                        String result = "";

                        if (method.equals("getPopulationofCountry")) {
                            result = "Population=" + server.getPopulationofCountry(parts[1]);
                        } else if (method.equals("getNumberofCities")) {
                            result = "Cities=" + server.getNumberofCities(
                                    parts[1],
                                    Long.parseLong(parts[2])
                            );
                        } else if (method.equals("getNumberofCountries")) {
                            result = "Countries=" + server.getNumberofCountries(
                                    Integer.parseInt(parts[1]),
                                    Long.parseLong(parts[2])
                            );
                        } else if (method.equals("getNumberofCountriesMM")) {
                            result = "Countries=" + server.getNumberofCountriesMM(
                                    Integer.parseInt(parts[1]),
                                    Long.parseLong(parts[2]),
                                    Long.parseLong(parts[3])
                            );
                        }

                        writer.write(query + " -> " + result + " (handled by " + serverURL + ")\n");
                        writer.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, delay * currentIndex, TimeUnit.MILLISECONDS);
            }
        }
    }
}
