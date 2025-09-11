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

}
