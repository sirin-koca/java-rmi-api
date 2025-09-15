package org.group5.server;

import java.io.Serializable;

//Own Request class to allow encapsulation of client requests into queue
public class Request implements Serializable
{
    private String methodName;
    private Object[] args;
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
}
