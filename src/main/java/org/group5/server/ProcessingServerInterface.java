package org.group5.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ProcessingServerInterface extends Remote
{
    int add(int num1, int num2) throws RemoteException;
}
