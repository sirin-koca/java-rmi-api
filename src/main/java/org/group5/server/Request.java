package org.group5.server;
//Own Request class to allow encapsulation of client requests into queue
public class Request {
    private String methodName; //name of method to invoke
    private Object[] args; //args to pass to method

    public Request(String methodName, Object... args){
        this.methodName = methodName;
        this.args = args;
    }
    //parse name of method from client request/input
    public String getMethodName(){
        return methodName;
    }
    //parse arguments given in client request
    public Object[] getArgs(){
        return args;
    }
    //Method to get clients zone from request in order to add appropriate latency when adding to queue
    public int getClientZone() {
        //args are type Object, need to cast to int
        int clientZone = 0;
        if (args.length > 0) {
            //zone is the last argument in all client requests
            Object lastArg = args[args.length - 1];
            if (lastArg instanceof Integer) {
                clientZone = (Integer) lastArg;
            } else {
                throw new IllegalArgumentException("Last argument must be an int");
            }
        } else{
            throw new IllegalArgumentException("No arguments provided");
        }
        return clientZone;
    }
}
