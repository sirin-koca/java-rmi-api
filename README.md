# Java RMI – International Statistics Service

This project implements an object-based distributed system using Java RMI for remote method invocation. 
It follows a simple client/server architecture with a load-balancing proxy, processes a large dataset, and 
simulates remote communication on a single machine. To improve scalability and reproducibility, 
it integrates caching mechanisms and hosts the servers in Docker containers.
- The first mandatory assignment in IN5020, Autumn 2025.

## 1. Objectives
- Stub/Proxy/Load Balancer: Distributes requests by zone, with fallback and load balancing.
- Servers: Multiple processing servers across zones, handle statistical queries, queues, and simulate network latency.
- Client: Parses query files, executes remote calls, and collects performance metrics.
- Caching methods: Naïve parsing, server-side caching, and client-side caching strategies
- Dockerized Deployment: Servers containerized for reproducibility.
- Graphs & Logs: Visualize turnaround time, execution time, waiting time, and server queue length.


## 2. System Architecture
- **Proxy (load balancer)**: Clients ask the proxy for a processing server for their zone.Proxy prefers a server in the same zone; if overloaded (≥18 queued), it chooses the least-loaded neighbor clockwise; if no server in a zone, it reroutes to the closest clockwise zone. After every 18 assignments to a server, the proxy updates its view of that server’s load (non-blocking).RMI registry binds the proxy under the name "proxy" on port 1099 (see ProxyServer main).
- **Servers**: Each server exposes the stats API (below), maintains a request queue, and may enable an internal cache (FIFO or LRU). Default dataset path in code: `src/main/resources/dataset/exercise_1_dataset.csv`
- **Client**: Reads queries and emits timing stats. Default input: `src/main/resources/input/exercise_1_input.txt`. Modes: naive, server-cache, client-cache. Output examples are recorded under `src/main/resources/output/`*(please see the provided samples).
- **Simulated latency**: Base 80 ms within same zone; add 30 ms × zone-distance to neighbor zones (clockwise).
- **Data source**: [Geonames – Cities >1000 population](https://public.opendatasoft.com/explore/dataset/geonames-all-cities-with-a-population-1000/)
- **Queries Supported**:
```
getPopulationofCountry(countryName)
getNumberofCities(countryName, threshold, comp)
getNumberofCountries(citycount, threshold, comp)
getNumberofCountriesMM(citycount, minPopulation, maxPopulation)
```
## 3. Caching Strategies
- Naïve (no cache).
- Server-side cache (FIFO, LRU).
- Client-side cache.

## 4. USER MANUAL
### 4.1 Prerequisites
- Install [Docker](https://docs.docker.com/get-docker/).
- Java JDK (Temurin 21)
- Ensure dataset is available: `src/main/resources/dataset/exercise_1_dataset.csv`
- Clone the project repository from:
  ```
  git clone https://github.com/sirin-koca/java-rmi-api
  cd java-rmi-api
  ```
  
### 4.2 How to deploy Docker:

  #### Docker image url: https://drive.google.com/file/d/1UgLLHQiGkd9nuXi5QaKtMUwIDV5sruLA/view?usp=sharing

  - **Filename**: ds-asn1-server-1.0.zip
  - **Size**: ~109 MB
  - **Expected SHA-256 (ZIP)**: EA3B73CA046CB4A1F0D177D69646196AA30595B572B7B76C53292D7F6D414D71
    - macOS/Linux: `shasum -a 256 ds-asn1-server-1.0.zip`
    - Windows (PowerShell): `Get-FileHash ds-asn1-server-1.0.zip -Algorithm SHA256`
    
1) Download, unzip and place it under the project-root
2) Load image:  `docker load -i ds-asn1-server-1.0.tar`


### 4.3 Compile packages:
```
mvn -q -DskipTests package
```
### 4.4 Start Proxy (Terminal A)
* (creates RMI registry on 1099, binds "proxy")
```
java -cp target/classes org.group5.proxy.ProxyServer
```
### 4.5 Build Docker image (Terminal B):
```
docker build -f docker\server\Dockerfile -t rmi-server:dev .
```
### 4.6 Run servers (Terminal C):
* (each server on a new terminal)
````
# Server #1:
docker run --rm --name rmi-s1 `
  -e PROXY_HOST=host.docker.internal `
  -e SERVER_HOST=host.docker.internal `
  -e RMI_REGISTRY_PORT=1099 `
  -e SERVER_PORT=2000 `
  -e SERVER_NAME=server1 `
  -p 2000:2000 rmi-server:dev

# Server #2:
docker run --rm --name rmi-s2 `
  -e PROXY_HOST=host.docker.internal `
  -e SERVER_HOST=host.docker.internal `
  -e RMI_REGISTRY_PORT=1099 `
  -e SERVER_PORT=2001 `
  -e SERVER_NAME=server2 `
  -p 2001:2001 rmi-server:dev

# Server #3:
docker run --rm --name rmi-s3 `
  -e PROXY_HOST=host.docker.internal `
  -e SERVER_HOST=host.docker.internal `
  -e RMI_REGISTRY_PORT=1099 `
  -e SERVER_PORT=2002 `
  -e SERVER_NAME=server3 `
  -p 2001:2001 rmi-server:dev
````
### 4.6 Run client (Terminal D):
````
java -cp target/classes org.group5.client.Client --mode server-cache
````

## 5. Results & Output
- Client logs 
- Server logs queue length over time
- Output files: naive_server.txt, server_cache.txt, client_cache.txt. 
- Metrics: turnaround, execution, waiting time.
- Graphs for turnaround time and queue length (ref. submitted report as PDF).
- Output examples are recorded under `src/main/resources/output/`*(please see the provided samples).

## 6. Workload Distribution
- Marta: Server
- Mariam: Client + Proxy
- Stål: Caching + graphs
- Sirin: Docker + report

## 7. Deliverables
- Source code (zip file)
- Final report as PDF with screenshots
- Ready to deploy docker image 
- Output files, logs & graphs

---
IFI @ UIO | IN5020 Group5 | H2025

---
