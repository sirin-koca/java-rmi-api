package org.group5.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

//Interface for client and proxy communication
//The client asks the proxy which erver to use for a given zone
//It returns the RMI URL of a server for a given zone
public interface proxyInterface extends Remote {
    String getServerForZone(int clientZone) throws RemoteException;
}
