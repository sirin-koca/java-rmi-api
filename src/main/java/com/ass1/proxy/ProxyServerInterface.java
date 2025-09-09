package com.ass1.proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ProxyServerInterface extends Remote
{
    void RequestProcessingServer(int zone) throws RemoteException;
}
