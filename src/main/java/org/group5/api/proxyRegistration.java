package org.group5.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

//Interface for server proxy communication
//Servers call this to register with the proxy, register a server with its zone and RMI url
public interface proxyRegistration extends Remote {
    void registerServer(int zone, String serverUrl) throws RemoteException;
}
