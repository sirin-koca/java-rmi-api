package org.group5.proxy;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data class containing information about a registered processing server
 */
public class ServerInfo implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;
    
    private final String serverId;
    private final String registryName;
    private final int zone;
    private final String host;
    private final int port;
    private final long registrationTime;
    
    public ServerInfo(String serverId, String registryName, int zone, String host, int port)
    {
        this.serverId = serverId;
        this.registryName = registryName;
        this.zone = zone;
        this.host = host;
        this.port = port;
        this.registrationTime = System.currentTimeMillis();
    }
    
    public String getServerId() {return serverId;}
    
    public String getRegistryName() {return registryName;}
    
    public int getZone() {return zone;}
    
    public String getHost() {return host;}
    
    public int getPort() {return port;}
    
    public long getRegistrationTime() {return registrationTime;}
    
    @Override
    public String toString()
    {
        return String.format("ServerInfo{id='%s', registry='%s', zone=%d, host='%s', port=%d}", serverId,
                registryName, zone, host, port);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ServerInfo that = (ServerInfo) obj;
        return serverId.equals(that.serverId);
    }
    
    @Override
    public int hashCode()
    {
        return serverId.hashCode();
    }
}
