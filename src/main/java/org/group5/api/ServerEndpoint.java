package org.group5.api;

import java.io.Serializable;

public class ServerEndpoint implements Serializable {
    public final String host;
    public final int    port;          // RMI registry port (1099 default)
    public final String bindingName;   // e.g., "stats-server-1"
    public final int    zone;

    public ServerEndpoint(String host, int port, String bindingName, int zone) {
        this.host = host;
        this.port = port;
        this.bindingName = bindingName;
        this.zone = zone;
    }
}
