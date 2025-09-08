package org.group5.server;

import org.group5.api.Hello;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class HelloImpl extends UnicastRemoteObject implements Hello {
    public HelloImpl() throws RemoteException {
        super();
    }

    @Override
    public String sayHello() {
        return "Hello from RMI server!";
    }
}
