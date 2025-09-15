package org.group5.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.rmi.Naming;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.group5.proxy.ProxyServerInterface;
import org.group5.proxy.ServerInfo;
import org.group5.server.ServerInterface;

public class Client
{
    public static void main(String[] args) throws Exception
    {
        String inputFile = "src/main/resources/dataset/exercise_1_input.txt";
        String outputFile = "src/main/resources/dataset/exercise_1_output.txt";
        
        ProxyServerInterface proxy = (ProxyServerInterface) Naming.lookup("rmi://localhost:1099/proxy");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile)))
        {
            String line;
            int delay = 50;
            int index = 0;
            
            while ((line = br.readLine()) != null)
            {
                final String query = line.trim();
                final int currentIndex = index++;
                
                scheduler.schedule(() -> {
                    try
                    {
                        // Validate and split line
                        if (!query.contains("Zone:"))
                        {
                            System.err.println("Skipping malformed query: " + query);
                            return;
                        }
                        
                        int zone = Integer.parseInt(query.split("Zone:")[1].trim());
                        ServerInfo serverInfo = proxy.requestProcessingServer(zone);
                        String serverURL = "rmi://localhost:1099/" + serverInfo.getRegistryName();
                        ServerInterface server = (ServerInterface) Naming.lookup(serverURL);
                        
                        // Extract method and args
                        String head = query.split("Zone:")[0].trim();
                        String[] parts = head.split("\\s+");
                        if (parts.length < 2)
                        {
                            System.err.println("Skipping malformed query: " + query);
                            return;
                        }
                        
                        String method = parts[0];
                        String result;
                        
                        switch (method)
                        {
                            case "getPopulationofCountry" ->
                            {
                                // Join all parts after method as country name
                                String countryName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                                result = "Population=" + server.getPopulationofCountry(countryName);
                                
                            }
                            case "getNumberofCities" ->
                            {
                                long threshold = Long.parseLong(parts[parts.length - 1]);
                                String countryName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1));
                                result = "Cities=" + server.getNumberofCities(countryName, threshold);
                                
                            }
                            case "getNumberofCountries" ->
                            {
                                int cityCount = Integer.parseInt(parts[1]);
                                long threshold = Long.parseLong(parts[2]);
                                result = "Countries=" + server.getNumberofCountries(cityCount, threshold);
                                
                            }
                            case "getNumberofCountriesMM" ->
                            {
                                int cityCount = Integer.parseInt(parts[1]);
                                long minPopulation = Long.parseLong(parts[2]);
                                long maxPopulation = Long.parseLong(parts[3]);
                                result = "Countries=" + server.getNumberofCountriesMM(cityCount, minPopulation,
                                        maxPopulation);
                                
                            }
                            default ->
                            {
                                System.err.println("Unknown method: " + method);
                                return;
                            }
                        }
                        
                        synchronized (writer)
                        {
                            writer.write(query + " -> " + result + " (handled by " + serverURL + ")\n");
                            writer.flush();
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }, (long) delay * currentIndex, TimeUnit.MILLISECONDS);
            }
        }
        
        // Shutdown the scheduler and wait for all tasks to complete
        scheduler.shutdown();
        try
        {
            // Wait for all tasks to complete before closing the writer
            if (!scheduler.awaitTermination(10, TimeUnit.MINUTES))
            {
                System.err.println("Some tasks did not complete within the timeout");
                scheduler.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        finally
        {
            // Close the writer only after all tasks have completed
            writer.close();
        }
    }
}
