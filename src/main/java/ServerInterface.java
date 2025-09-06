import java.rmi.Remote;
import java.rmi.RemoteException;
public interface ServerInterface extends Remote {
    int getPopulationofCountry(String countryName) throws RemoteException;
    int getNumberofCities(String countryName, int threshold, String comp) throws RemoteException;
    int getNumberofCountries(int citycount, int threshold, String comp) throws RemoteException;
    int getNumberofCountriesMM(int citycount, int minpopulation, int maxpopulation) throws RemoteException;
    }


