package com.ass1.client;

import com.ass1.server.ServerInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Client
{
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    
    private static void handleClientError(Exception e)
    {
        if (e instanceof RemoteException)
        {
            logger.log(Level.SEVERE, "Failed to connect to RMI server", e);
            System.err.println("Error: Unable to connect to server. Please check if the server is running and " +
                    "accessible.");
        } else if (e instanceof NotBoundException)
        {
            logger.log(Level.SEVERE, "Server 'server' not found in registry", e);
            System.err.println("Error: Server not found. Please ensure the server is started and properly registered.");
        } else
        {
            logger.log(Level.SEVERE, "Unexpected error during client operation", e);
            System.err.println("Error: Unexpected error occurred while connecting to server.");
        }
    }
    
    public static void main(String[] args)
    {
        try
        {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface server = (ServerInterface) registry.lookup("server");
            System.out.println(server.Add(10, 20));
            System.out.println(server.Add(11, 20));
            System.out.println(server.Add(10, 20));
            System.out.println(server.Add(12, 20));
            System.out.println(server.Add(12, 20));
            System.out.println(server.Add(5, 5));
            System.out.println(server.Add(10, 20));
            System.out.println(server.Add(5, 5));
        } catch (RemoteException | NotBoundException e)
        {
            handleClientError(e);
        }
    }
}
