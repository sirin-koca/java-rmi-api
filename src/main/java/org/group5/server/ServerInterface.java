package org.group5.server;
import java.rmi.Remote;
import java.rmi.RemoteException;
public interface ServerInterface extends Remote {
    long getPopulationofCountry(String countryName) throws RemoteException;
    int getNumberofCities(String countryName, long threshold) throws RemoteException;
    int getNumberofCountries(int citycount, long threshold) throws RemoteException;
    int getNumberofCountriesMM(int citycount, long minpopulation, long maxpopulation) throws RemoteException;
    int queueSize() throws RemoteException;
}

