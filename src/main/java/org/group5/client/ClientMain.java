package org.group5.client;

import org.group5.api.Hello;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        Hello stub = (Hello) registry.lookup("HelloService");
        System.out.println(stub.sayHello());
    }
}
