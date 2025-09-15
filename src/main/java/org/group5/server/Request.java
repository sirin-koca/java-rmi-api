package org.group5.server;

import java.io.Serializable;

//Own Request class to allow encapsulation of client requests into queue
public class Request implements Serializable
{
    private final String methodName;
    private final Object[] args;
    private String requestId;
    
    // Constructor with variable arguments
    public Request(String methodName, Object... args)
    {
        this.methodName = methodName;
        this.args = args;
    }
    
    public String getMethodName()
    {
        return methodName;
    }
    
    public Object[] getArgs()
    {
        return args;
    }
    
    public String getRequestId()
    {
        return requestId;
    }
    
    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }
    
    // Method to get clients zone from request in order to add appropriate latency when adding to queue
    public int getClientZone()
    {
        // args are type Object, need to cast to int
        int clientZone = 0;
        if (args.length > 0)
        {
            // zone is the last argument in all client requests
            Object lastArg = args[args.length - 1];
            if (lastArg instanceof Integer)
            {
                clientZone = (Integer) lastArg;
            }
            else
            {
                throw new IllegalArgumentException("Last argument must be an int");
            }
        }
        else
        {
            throw new IllegalArgumentException("No arguments provided");
        }
        return clientZone;
    }
}
