package org.group5.server;

import org.group5.api.Hello;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        Registry registry = LocateRegistry.createRegistry(1099);  // start registry on port 1099
        Hello svc = new HelloImpl();
        registry.rebind("HelloService", svc);
        System.out.println("RMI Server ready on port 1099");
    }
}
