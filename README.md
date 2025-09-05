# Java RMI – International Statistics Service

_🚧 Work in progress –This README will be updated as the project grows._

## Overview

This project is the first mandatory assignment in IN5020.
We will implement a distributed system using Java RMI, with following components:
- Proxy (load balancer) server
- Multiple processing servers across zones
- Client issuing statistical queries on a global cities dataset
- Naïve parsing, server-side caching, and client-side caching strategies
- Docker containerization for deployment

Objective: Measure and compare performance (latency, execution time, waiting time) across different setups.

## Features

- Proxy/Load Balancer: Distributes requests by zone, with fallback and load balancing.
- Servers: Handle statistical queries, queues, and simulate network latency.
- Client: Parses query files, executes remote calls, and collects performance metrics.
- Caching:
  - Naïve (no cache)
  - Server-side cache (FIFO / LRU)
  - Client-side cache
- Dockerized Deployment: Servers containerized for reproducibility.
- Graphs & Logs: Visualize turnaround time, execution time, waiting time, and server queue length.

## Queries Supported

```
getPopulationofCountry(countryName)
getNumberofCities(countryName, threshold, comp)
getNumberofCountries(citycount, threshold, comp)
getNumberofCountriesMM(citycount, minPopulation, maxPopulation)
```

### What is Java RMI

Java Remote Method Invocation (RMI) allows distributed Java applications to call methods on remote objects running in other 
JVMs, possibly on different hosts. It uses object serialization to transfer data and fully supports polymorphism across the network.

#### Key Concept: Distributed Systems: 
A Distributed System consists of multiple computers (nodes) that communicate and coordinate over a network. 
The goal is to appear as a single, unified system, even though its components run on different machines.

_Source: [Java SE Remote Method Invocation APIs and Developer Guides](https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/index.html)_

