import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
public class Server implements ServerInterface{
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
