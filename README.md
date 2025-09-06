# Java RMI – International Statistics Service

_🚧 Work in progress –This README will be updated as the project grows._

### 1. Overview

This project is the first mandatory assignment in IN5020.
We will implement a distributed system using Java RMI, with following components:
- Proxy (load balancer) server
- Multiple processing servers across zones
- Client issuing statistical queries on a global cities dataset
- Naïve parsing, server-side caching, and client-side caching strategies
- Docker containerization for deployment

Objective: Measure and compare performance (latency, execution time, waiting time) across different setups.

### 2. Features

- Proxy/Load Balancer: Distributes requests by zone, with fallback and load balancing.
- Servers: Handle statistical queries, queues, and simulate network latency.
- Client: Parses query files, executes remote calls, and collects performance metrics.
- Caching:
  - Naïve (no cache)
  - Server-side cache (FIFO / LRU)
  - Client-side cache
- Dockerized Deployment: Servers containerized for reproducibility.
- Graphs & Logs: Visualize turnaround time, execution time, waiting time, and server queue length.

### 3. System Architecture
_Short description of Proxy, Server, Client roles, dataset and storage (* visualize the SA - diagram etc.)_
#### Queries Supported
```
getPopulationofCountry(countryName)
getNumberofCities(countryName, threshold, comp)
getNumberofCountries(citycount, threshold, comp)
getNumberofCountriesMM(citycount, minPopulation, maxPopulation)
```
### 4. Caching Strategies
- Naïve (no cache).
- Server-side cache (FIFO, LRU).
- Client-side cache.

### 5. Dataset
Source: Geonames all cities with population >1000
~140,000 cities worldwide.

### 6. Build & Run
_(How to build (Maven), how to run Proxy, Servers, Client etc.) - mention Dockerizing here (sirin)_

### 7. Output & Measurements
- Output files: naive_server.txt, server_cache.txt, client_cache.txt.
- Metrics: turnaround, execution, waiting time.
- Graphs for turnaround time and queue length.

### 8. Workload Distribution
_How the group collaborates, who is responsible for what etc._

### 9. Deliverables
_What will be submitted: code, Docker images, report, graphs, output files etc._

## About Java RMI

Java Remote Method Invocation (RMI) allows distributed Java applications to call methods on remote objects running in other 
JVMs, possibly on different hosts. It uses object serialization to transfer data and fully supports polymorphism across the network.

#### Key Concept: Distributed Systems: 
A Distributed System consists of multiple computers (nodes) that communicate and coordinate over a network. 
The goal is to appear as a single, unified system, even though its components run on different machines.

_Source: [Java SE Remote Method Invocation APIs and Developer Guides](https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/index.html)_

