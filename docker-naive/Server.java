import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Server extends UnicastRemoteObject implements ServerInterface {
    protected Server() throws RemoteException { super(); }

    @Override
    public String sayHello() throws RemoteException {
        return "Hello from Naive RMI Server (inside Docker)!";
    }
}
