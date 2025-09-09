package org.group5.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.rmi.Naming;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.group5.api.ProxyInterface;
import org.group5.server.ServerInterface;

public class Client {
    public static void main(String[] args) throws Exception {
        String inputFile = "src/main/resources/dataset/exercise_1_input.txt";
        String outputFile = ""; //Results

        //Client connects to proxy via RMI 
        ProxyInterface proxy = (ProxyInterface) Naming.lookup("rmi://localhost:1099/ProxyService");
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            int delay = 50; //change to 20 in second run 

            while ((line = br.readLine()) != null) {
                final String query = line;
                scheduler.schedule(() -> {
                    try {
                        //Parse zone from query
                        int zone = Integer.parseInt(query.split("Zone:")[1].trim());
                        String serverURL = proxy.getServerForZone(zone);

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
                                    Integer.parseInt(parts[2]),
                                    parts[3]
                            );
                        } else if (method.equals("getNumberofCountries")) {
                            result = "Countries=" + server.getNumberofCountries(
                                    Integer.parseInt(parts[1]),
                                    Integer.parseInt(parts[2]),
                                    parts[3]
                            );
                        } else if (method.equals("getNumberofCountriesMM")) {
                            result = "Countries=" + server.getNumberofCountriesMM(
                                    Integer.parseInt(parts[1]),
                                    Integer.parseInt(parts[2]),
                                    Integer.parseInt(parts[3])
                            );
                        }

                        writer.write(query + " -> " + result + " (handled by " + serverURL + ")\n");
                        writer.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }
        }
    }
}
