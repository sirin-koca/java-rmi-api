package org.group5.server;

import java.rmi.RemoteException;

public class CreateServers
{
    public static void createServers(int numServers, int startPort, boolean cache, boolean lru) throws RemoteException
    {
        System.out.println("Starting " + numServers + " servers");
        for (int i = 1; i <= numServers; i++)
        {
            new Server("server" + i, startPort + i, cache, lru);
        }
    }
    
    public static void main(String[] args) throws RemoteException
    {
        createServers(5, 5000, false, false);
    }
}
