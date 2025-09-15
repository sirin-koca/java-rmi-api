package org.group5.server;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.group5.proxy.ProxyServerInterface;

public class Server extends UnicastRemoteObject implements ServerInterface{
    private final BlockingQueue<Request> requestQueue; //needs zone number somewhere in here
    private final Thread requestHandlerThread;
    private static final String csv_path = "/Users/Maryam/Desktop/java-rmi-api/src/main/resources/dataset/exercise_1_dataset.csv";
    private int zone; //will be assigned by proxy server
    private String name; //Assigned on instantiation
    private int port; //Assigned on instantiation
   //Registry registry;

    //Constructor that gets zone number from proxy and creates queue for requests
    protected Server(String name, int port) throws RemoteException {
       super();
       this.name = name;
       this.port = port;
       //this.registry = registry;
       //this.zone = zone;
        this.requestQueue = new LinkedBlockingQueue<>();

        //connect to proxy and get zone number
        // connect to proxy and get zone number
try {
    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
    ProxyServerInterface proxy = (ProxyServerInterface) registry.lookup("proxy");

    // Assign a zone from proxy
    this.zone = proxy.assignZoneNumber(name);

    // Bind server object in registry under its name
    registry.rebind(name, this);

    // Register server info with proxy so it appears in status
    org.group5.proxy.ServerInfo serverInfo =
        new org.group5.proxy.ServerInfo(name, name, this.zone, "localhost", port);
    proxy.registerServer(serverInfo);

    System.out.println("Assigned zone number: " + zone + " for server " + name);
} catch (Exception e) {
    throw new RemoteException("Failed to register with proxy", e);
}

        //Start thread to handle execution of requests from queue
        this.requestHandlerThread = new Thread(new RequestHandler());
        this.requestHandlerThread.start();
    }
    //Add request to queue
    public synchronized void addRequest(Request request){
        try {
            requestQueue.put(request);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            System.err.println("Failed to add request to queue: " + e.getMessage());
        }
    }
    //Thread that takes request from queue and executes
    private class RequestHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    //Take from queue and process
                    Request request = requestQueue.take(); //add latency simulation
                    processRequest(request);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; //Exit loop if interrupted
                }
            }
        }
    }
    //method that gets method name and arguments from client request
    //method name is taken out first, so first argument refers to argument after method name
    private void processRequest(Request request) {
        try {
            switch (request.getMethodName()) {
                case "getPopulationofCountry":
                    String countryName = (String) request.getArgs()[0];
                    long population = getPopulationofCountry(countryName);
                    System.out.println("Population of " + countryName +": " + population);
                    break;
                case "getNumberofCities":
                    String countryNameCities = (String) request.getArgs()[0];
                    long threshold = (Long) request.getArgs()[1];
                    int cityCount = getNumberofCities(countryNameCities, threshold);
                    System.out.println("Number of cities in " + countryNameCities + " with population >= " + threshold + ": " + cityCount);
                    break;
                case "getNumberofCountries":
                    int reqCityCount = (Integer) request.getArgs()[0];
                    long populationThreshold = (Long) request.getArgs()[1];
                    int countryCount = getNumberofCountries(reqCityCount, populationThreshold);
                    System.out.println("Number of countries with at least " + reqCityCount + " cities with population >= " + populationThreshold + ": " + countryCount);
                    break;
                case "getNumberofCountriesMM":
                    int cityCountThreshold = (Integer) request.getArgs()[0];
                    long minPopulation = (Long) request.getArgs()[1];
                    long maxPopulation = (Long) request.getArgs()[2];
                    int countriesMMCount = getNumberofCountriesMM(cityCountThreshold, minPopulation, maxPopulation);
                    System.out.println("Number of countries with at least " + cityCountThreshold + " cities with population between " + minPopulation + " and " + maxPopulation + ": " + countriesMMCount);
                    break;
                default:
                    System.out.println("Method must be listed in Server Interface");
                }
            } catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public long getPopulationofCountry(String countryName) throws RemoteException {
        //cache check here
        long population = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(csv_path))){
                String line;
                while ((line = br.readLine()) != null) {
                    String[] fields = line.split(",");
                    //field 4 = country name, field 5 = population
                    if (fields.length > 5 && fields[4].equalsIgnoreCase(countryName)) {
                        population += Long.parseLong(fields[5]);
                    }
                }
            } catch (Exception e) {
            e.printStackTrace();
        } return population;
    }
    @Override //minimum threshold
    public int getNumberofCities(String countryName, long threshold) throws RemoteException {
        int cityCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(csv_path))){
                String line;
                while ((line = br.readLine()) != null) {
                    String[] fields = line.split(",");
                    if(fields.length > 5 && fields[4].equalsIgnoreCase(countryName) && Long.parseLong(fields[5]) >= threshold){
                        cityCount++;
                    }
                }
            }catch (Exception e){
            e.printStackTrace();
        } return cityCount;
    }

    @Override //minimum threshold
    public int getNumberofCountries(int citycount, long threshold) throws RemoteException {
        //Save cities above threshold per country in hashmap
        Map<String, Integer> citiesPerCountry = new HashMap<>();
        //count cites above threshold for each country
        try (BufferedReader br = new BufferedReader(new FileReader(csv_path))){
                String line;
                while ((line = br.readLine()) != null){
                    String[] fields = line.split(",");
                    if (fields.length > 5 && Long.parseLong(fields[5]) >= threshold){
                        String countryName = fields[4];
                        citiesPerCountry.put(countryName, citiesPerCountry.getOrDefault(countryName,0) + 1);
                    }
                }
        }catch (Exception e) {
            e.printStackTrace();
        }
        //Count number of countries above threshold from dictionary/hashmap
        int qualifyingCountries = 0;
        for (int count: citiesPerCountry.values()){
            if (count >= citycount){
                qualifyingCountries++;
            }
        }
        return qualifyingCountries;
    }
    @Override
    public int getNumberofCountriesMM(int citycount, long minpopulation, long maxpopulation) throws RemoteException {
        Map<String, Integer> countryCities = new HashMap<>();
        //count cities within population interval per country
        try (BufferedReader br = new BufferedReader(new FileReader(csv_path))){
                String line;
                while ((line = br.readLine()) != null){
                    String[] fields = line.split(",");
                    long population = Long.parseLong(fields[5]);
                    String countryName = fields[4];
                    if (fields.length > 5 && population >= minpopulation && population <= maxpopulation){
                        countryCities.put(countryName, countryCities.getOrDefault(countryName, 0) + 1);
                    }
                }
        }catch (Exception e){
            e.printStackTrace();
        }
        //Count qualifying countries based on city count threshold
        int qualifyingCountries = 0;
        for (int count : countryCities.values()){
            if (count >= citycount){
                qualifyingCountries++;
            }
        }
        return qualifyingCountries;
    }
    public int queueSize() throws RemoteException{
        return requestQueue.size();
    }

}