package org.group5.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface StatsServer extends Remote {
    StatsResult getPopulationofCountry(String countryName) throws RemoteException;

    // placeholders for later steps (for now... work-in-progress):
    // StatsResult getNumberofCities(String countryName, long threshold, String comp) throws RemoteException;
    // StatsResult getNumberofCountries(int citycount, long threshold, String comp) throws RemoteException;
    // StatsResult getNumberofCountriesMM(int citycount, long minPopulation, long maxPopulation) throws RemoteException;
}
