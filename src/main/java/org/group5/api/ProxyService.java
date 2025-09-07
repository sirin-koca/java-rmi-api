package org.group5.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ProxyService extends Remote {
    // Server registers itself; proxy assigns zone in ascending order and returns endpoint
    ServerEndpoint registerServer(String host, int registryPort, String suggestedBindingName)
            throws RemoteException;

    // Client asks for server for a given zone (minimal for now: just return same-zone if registered)
    ServerEndpoint getServerForZone(int zone) throws RemoteException;
}
