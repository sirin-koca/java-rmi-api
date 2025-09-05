package com.ass1.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Server implements ServerInterface
{
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    
    public int Add(int num1, int num2)
    {
        return num1 + num2;
    }
    
    private static void handleServerStartupError(Exception e)
    {
        if (e instanceof RemoteException)
        {
            logger.log(Level.SEVERE, "Failed to start RMI server due to remote communication error", e);
            System.err.println("Error: Unable to start server. Please check if RMI registry is running.");
        } else if (e instanceof AlreadyBoundException)
        {
            logger.log(Level.SEVERE, "Server name 'server' is already bound in registry", e);
            System.err.println("Error: Server is already running or name is already in use.");
        } else
        {
            logger.log(Level.SEVERE, "Unexpected error during server startup", e);
            System.err.println("Error: Unexpected error occurred during server startup.");
        } System.exit(1);
    }
    
    public static void main(String[] args)
    {
        try
        {
            // Start RMI registry programmatically
            Registry registry = LocateRegistry.createRegistry(1099);
            
            Server server = new Server();
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            registry.bind("server", serverStub);
            
            logger.info("Server started successfully and bound to registry");
            System.out.println("Server is ready and waiting for clients...");
            
            // Keep the server running
            Thread.currentThread().join();
        } catch (RemoteException | AlreadyBoundException | InterruptedException e)
        {
            handleServerStartupError(e);
        }
        
        try
        {
            Registry registry = LocateRegistry.getRegistry(); Server server = new Server();
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            registry.bind("server", serverStub); logger.info("Server started successfully and bound to registry");
        } catch (RemoteException | AlreadyBoundException e)
        {
            handleServerStartupError(e);
        }
        
    }
}
