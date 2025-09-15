package org.group5.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote
{
    long getPopulationofCountry(String countryName, int clientZone) throws RemoteException;
    
    int getNumberofCities(String countryName, long threshold, int clientZone) throws RemoteException;
    
    int getNumberofCountries(int citycount, long threshold, int clientZone) throws RemoteException;
    
    int getNumberofCountriesMM(int citycount, long minpopulation, long maxpopulation, int clientZone) throws RemoteException;
    
    int queueSize() throws RemoteException;
}

