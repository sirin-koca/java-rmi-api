import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
public class CreateServers {
    private int numServers;
    private int startPort;
    private int zone;

    public void createServers(int numServers, int startPort, int zone) throws RemoteException {
        this.numServers = 5;
        this.startPort = 5000;
        System.out.println("Starting " + numServers + " servers");
        //Registry registry = LocateRegistry.getRegistry("localhost", 4000); //proxyport
        //ProxyServerInterface proxy = (ProxyServerInterface) registry.lookup("ProxyServer"); //name of proxy port for lookup
        //this.zone = proxy.assignZoneNumber("server-url"); //Function from proxy interface
        //System.out.println("Assigned zone number: " + zone);
        for (int i = 1; i <= numServers; i++){
            Server server = new Server("server"+i, startPort + i);
    }

    }
}
