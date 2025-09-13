import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Server extends UnicastRemoteObject implements ServerInterface{
    private final BlockingQueue<Request> requestQueue; //needs zone number somewhere in here
    private final Thread requestHandlerThread;
    private static final String csv_path = "pathToCsvFile";
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
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 4040);//need correct port
            ProxyServerInterface proxy = (ProxyServerInterface) registry.lookup("ProxyServer");
            this.zone = proxy.assignZoneNumber(name);
            System.out.println("Assigned zone number: " +zone + " for server " + name);
            //Should I create new registry and bind server to it?

        } catch (Exception e) {
            throw new RemoteException("Failed to get zone number from proxy server");
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
    private void processRequest(Request request) {
        switch (request.getMethodName()) {
            case "getPopulationofCountry":
                //Call method and use request.Args()
                break;
            case "getNumberofCities":
                //Call method and use request.Args()
                break;
            case "getNumberofCountries":
                //Call method and use request.Args()
                break;
            case "getNumberofCountriesMM":
                //Call method and use request.Args()
                break;
            default:
                System.out.println("Method must be listed in Server Interface");
        }
    }

    @Override
    public long getPopulationofCountry(String countryName) throws RemoteException {
        //cache check here
        long population = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(csv_path)){
                String line = br.readLine();
                while (line != null) {
                    String[] fields = line.split(",");
                    //field 4 = country name, field 5 = population
                    if (fields.length > 5 && fields[4].equalsIgnoreCase(countryName)) {
                        population += Long.parseLong(fields[5]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } return population;
    }
    @Override //minimum threshold
    public int getNumberofCities(String countryName, long threshold) throws RemoteException {
        int cityCount = 0;

        try {
            BufferedReader br = new BufferedReader(new FileReader(csv_path)){
                String line = br.readLine();
                while (line != null) {
                    String[] fields = line.split(",");
                    if(fields.length > 5 && fields[4].equalsIgnoreCase(countryName) && Long.parseLong(fields[5]) >= threshold){
                        cityCount++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } return cityCount;
        }
    @Override //minimum threshold
    public int getNumberofCountries(int citycount, long threshold) throws RemoteException {
        //Save cities above threshold per country in hashmap
        Map<String, Integer> citiesPerCountry = new HashMap<>();
        //count cites above threshold for each country
        try {
            BufferedReader br = new BufferedReader(new FileReader(csv_path)){
                String line = br.readLine();
                while (line != null){
                    String[] fields = line.split(",");
                    if (fields.length > 5 && Long.parseLong(fields[5]) >= threshold){
                        String countryName = fields[4];
                        citiesPerCountry.put(countryName, citiesPerCountry.getOrDefault(countryName,0) + 1);
                    }
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
        try {
            BufferedReader br = new BufferedReader(new FileReader(csv_path)){
                String line = br.readLine();
                while (line != null){
                    String[] fields = line.split(",");
                    long population = Long.parseLong(fields[5]);
                    String countryName = fields[4];
                    if (fields.length > 5 && population >= minpopulation && population <= maxpopulation){
                        countryCities.put(countryName, countryCities.getOrDefault(countryName, 0) + 1);
                    }
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
