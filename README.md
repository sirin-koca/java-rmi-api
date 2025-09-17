# Java RMI – International Statistics Service

_🚧 Work in progress –This README will be updated as the project grows._

### 1. Overview
Distributed system over Java RMI with:
- Proxy (load balancer)
- Multiple servers across zones
- Client sending queries from an input file
- Three caching modes: naïve, server-side cache, client-side cache
- Docker containerization for deployment
- Goal: measure turnaround / latency/ execution / waiting times per query type.
- Toolchain: JDK 21 (Temurin), Maven 3.x, Docker version 26.1.1

### 2. Features
- Stub/Proxy/Load Balancer: Distributes requests by zone, with fallback and load balancing.
- Servers: Multiple processing servers across zones, handle statistical queries, queues, and simulate network latency.
- Client: Parses query files, executes remote calls, and collects performance metrics.
- Caching methods: Naïve parsing, server-side caching, and client-side caching strategies
- Dockerized Deployment: Servers containerized for reproducibility.
- Graphs & Logs: Visualize turnaround time, execution time, waiting time, and server queue length.

### 3. System Architecture
_Short description of Proxy, Server, Client roles, dataset and storage (* visualize the SA - diagram etc.)_

### 4. Caching Strategies
- Naïve (no cache).
- Server-side cache (FIFO, LRU).
- Client-side cache.

### 5. Dataset
- CSV file with geonames all cities with population >1000
~140,000 cities worldwide.
#### Queries Supported
```
getPopulationofCountry(countryName)
getNumberofCities(countryName, threshold, comp)
getNumberofCountries(citycount, threshold, comp)
getNumberofCountriesMM(citycount, minPopulation, maxPopulation)
```

### 6. Build & Run
_(How to build (Maven), how to run Proxy, Servers, Client etc.) - mention Dockerizing here (sirin)_

🔹 Runtime Flow
- Server starts: creates a remote object → binds it into the RMI Registry under a name.
- Client starts: asks the Registry for "HelloService" (or whatever name).
- RMI Registry returns a stub (proxy reference) to the client.
- Client now calls methods on the stub (like a normal method call).
- Stub (proxy) forwards the call across the network to the remote object on the server.
- Server object executes the method and returns the result.
- Stub delivers the result back to the client.

### 7. Output & Measurements
- Output files: naive_server.txt, server_cache.txt, client_cache.txt.
- Metrics: turnaround, execution, waiting time.
- Graphs for turnaround time and queue length.

### 8. Workload Distribution
_How the group collaborates, who is responsible for what etc._
1) Mariam:  Client & Proxy
2) Martta: Server & Queue
3) Staal: Caching
4) Sirin: Dockerize

### 9. Deliverables
_What will be submitted: code, Docker images, report, graphs, output files etc._

## About Java RMI
Java Remote Method Invocation (RMI) allows distributed Java applications to call methods on remote objects running in other 
JVMs, possibly on different hosts. It uses object serialization to transfer data and fully supports polymorphism across the network.

#### Key Concept: Distributed Systems: 
A Distributed System consists of multiple computers (nodes) that communicate and coordinate over a network. 
The goal is to appear as a single, unified system, even though its components run on different machines.

_Source: [Java SE Remote Method Invocation APIs and Developer Guides](https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/index.html)_

IFI @ UIO | Group Project | 2025

