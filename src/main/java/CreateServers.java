import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
public class CreateServers {

    public static void main(String[] args) throws RemoteException {
        int startPort = 5000;
        int numServers = 5;
        Registry registry = LocateRegistry.createRegistry(startPort);

        for (int i = 0; i < numServers; i++){
            new Server("server_"+i, startPort+i, registry);
        }

    }
}
