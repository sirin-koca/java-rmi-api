# Java RMI – International Statistics Service

_🚧 Work in progress –This README will be updated as the project grows._

## 1. Overview

This project is the first mandatory assignment in IN5020.
We will implement a distributed system using Java RMI, measure and compare performance (latency, execution time,
waiting time) across different setups.

## 2. Objectives
- Distributed system with Java RMI
- Proxy (load balancer) + Servers + Clients
- Large dataset (~140k cities)
- Caching mechanisms
- Dockerized servers

## 3. System Architecture
- **Proxy Server**: distributes client requests by zone with load balancing
- **Servers**: provide statistics queries on countries/cities
- **Clients**: parse input file, invoke remote methods, collect metrics
- **Zones**: simulate geography and latency
- **Data source**: [Geonames – Cities >1000 population](https://public.opendatasoft.com/explore/dataset/geonames-all-cities-with-a-population-1000/)
- **Format**: CSV (~140,574 entries)

### 3.1 Queries Supported
```
getPopulationofCountry(countryName)
getNumberofCities(countryName, threshold, comp)
getNumberofCountries(citycount, threshold, comp)
getNumberofCountriesMM(citycount, minPopulation, maxPopulation)
```

## 4. Caching Strategies
- Naïve (no cache).
- Server-side cache (FIFO, LRU).
- Client-side cache.

## 5. USER MANUAL
#### Prerequisites
- Install [Docker](https://docs.docker.com/get-docker/).
- Ensure dataset `geonames-all-cities-with-a-population-1000.csv` is available.
- Clone the project repository:
  ```
  git clone https://github.com/sirin-koca/java-rmi-api
  cd java-rmi-api
  ```
#### Build Docker Images
Each server runs in its own container. Build images with Maven + Docker:
 ```
mvn clean package
docker build -t rmi-server ./server
docker build -t rmi-proxy ./proxy
docker build -t rmi-client ./client
 ```
#### Start containers:
- *Proxy*:
 ```
docker run -d --name proxy -p 1099:1099 rmi-proxy
```
- *Server*:
```
docker run -d --name server1 -e ZONE=1 -p 2001:2001 rmi-server
docker run -d --name server2 -e ZONE=2 -p 2002:2002 rmi-server
docker run -d --name server3 -e ZONE=3 -p 2003:2003 rmi-server
```
- *Client*: _uses the provided input file_
```
docker run --rm -v $(pwd)/exercise_1_input.txt:/app/input.txt \
  -v $(pwd)/output:/app/output \
  rmi-client /app/input.txt
```

## 6. Output
- Client logs:
  ``` 
  <result> <query> (turnaround: XXms, execution: YYms, waiting: ZZms, Server #)
  ``` 
- Summary with avg/min/max per query type
- Server logs queue length over time
- Output files: naive_server.txt, server_cache.txt, client_cache.txt.
- Metrics: turnaround, execution, waiting time.
- Graphs for turnaround time and queue length.

## 7. Workload Distribution
- Marta: Server
- Mariam: Client + Proxy
- Stål: Caching
- Sirin: Docker

## 8. Deliverables
- Source code as a zip file
- Docker images with dependencies
- Output logs & graphs
- Final report as PDF


IFI @ UIO | Group Project | 2025

