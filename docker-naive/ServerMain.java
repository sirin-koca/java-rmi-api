import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(1099); // default RMI port
            Server server = new Server();
            registry.rebind("NaiveServer", server);
            System.out.println("Naive RMI Server ready!");
            Object lock = new Object(); synchronized (lock) { lock.wait(); } // keep alive
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
