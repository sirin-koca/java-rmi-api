# Distributed Systems (DS) + Java RMI API

## Java RMI TEST
Java Remote Method Invocation (Java RMI) enables the programmer to create distributed Java technology-based applications, in which the methods of remote Java objects can be invoked from other Java virtual machines, possibly on different hosts. RMI uses object serialization to marshal and unmarshal parameters and does not truncate types, supporting true object-oriented polymorphism.

•	Distributed Systems (DS): 
A system where multiple computers (nodes) communicate and coordinate their actions by passing messages over a network. 
Goal: behave like one system when parts are on different machines.
  
•	RMI = Remote Method Invocation
A Java API that lets you call methods on objects that live on another JVM (remote computer) as if they were local.

## How it works (simplified)
1.	Interface (defines remote methods, must extend java.rmi.Remote).
2.	Server (class that implements the interface, registered in RMI registry).
3.	Client (looks up the remote object in the registry and calls its methods).
4.	RMI runtime handles communication, object serialization, network sockets, etc.

## Why it matters in DS
•	It’s a tool to learn object-based distributed systems: we can interact with remote objects, not just raw sockets.
•	It demonstrates core DS principles: transparency (we will be able to call a method as if it’s local), stubs/skeletons, marshalling, and communication.

### Java RMI API = the standard Java library to build object-based distributed applications.

_Source: [Java SE Remote Method Invocation APIs and Developer Guides](https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/index.html)_

