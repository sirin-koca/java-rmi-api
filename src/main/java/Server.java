import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
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
    public int getPopulationofCountry(String countryName) throws RemoteException {
        return 0;
    }
    @Override
    public int getNumberofCities(String countryName, int threshold, String comp) throws RemoteException {
        return 0;
    }
    @Override
    public int getNumberofCountries(int citycount, int threshold, String comp) throws RemoteException {
        return 0;
    }
    @Override
    public int getNumberofCountriesMM(int citycount, int minpopulation, int maxpopulation) throws RemoteException {
        return 0;
    }
    public int queueSize() throws RemoteException{
        return requestQueue.size();
    }

}
