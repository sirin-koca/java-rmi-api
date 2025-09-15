package org.group5.server;

import java.rmi.RemoteException;

public class CreateServers
{
    public void createServers(int numServers, int startPort) throws RemoteException
    {
        System.out.println("Starting " + numServers + " servers");
        for (int i = 1; i <= numServers; i++)
        {
            new Server("server" + i, startPort + i);
        }
    }
    
    public static void main(String[] args) throws RemoteException
    {
        CreateServers creator = new CreateServers();
        creator.createServers(5, 5000);
    }
}
