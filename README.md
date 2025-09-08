# Naive RMI Example

This branch contains a minimal Remote Method Invocation (RMI) example in Java.
It demonstrates how a client can call a method on a server object running in a different JVM.

## Project Structure
src/main/java/org/group5/
````
├─ api/
│   └─ Hello.java          # Remote interface
├─ server/
│   ├─ HelloImpl.java      # Implementation of remote interface
│   └─ ServerMain.java     # Starts RMI registry and binds remote object
└─ client/
└─ ClientMain.java     # Looks up remote object and calls method
````

## How It Works
- Server creates a remote object (HelloImpl) and binds it to the RMI registry under the name HelloService.
- Client connects to the registry, looks up HelloService, and retrieves a stub (proxy).
- The client calls sayHello() on the stub.
- RMI forwards the call to the server object, executes it, and returns the result.

## How2 Run
This is the naive RMI skeleton.
- Run ServerMain first → starts registry + binds object.
- Then run ClientMain → should print "Hello from RMI server!".

sirin-koca 