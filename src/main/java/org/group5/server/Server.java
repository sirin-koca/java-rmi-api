import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
public class Server implements ServerInterface{
    String name;
    int zone = 0; //will be assigned by proxy server
    int port;
    Registry registry;
    public Server(String name, int port, Registry registry){
        this.name = name;
        this.port = port;
        this.registry = registry;
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

}
