package com.ass1.proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ProxyServerInterface extends Remote
{
    /**
     * Request a processing server for a specific geographical zone
     *
     * @param zone The geographical zone number for the client
     * @return ServerInfo containing the registry name and zone of the assigned server
     * @throws RemoteException if no servers are available or RMI error occurs
     */
    ServerInfo requestProcessingServer(int zone) throws RemoteException;
    
    /**
     * Register a processing server with the proxy
     *
     * @param serverInfo Information about the server to register
     * @throws RemoteException if registration fails
     */
    void registerServer(ServerInfo serverInfo) throws RemoteException;
    
    /**
     * Unregister a processing server from the proxy
     *
     * @param serverId The unique identifier of the server to unregister
     * @throws RemoteException if unregistration fails
     */
    void unregisterServer(String serverId) throws RemoteException;
}
