# This image builds and runs ONE server.
# We keep it in two parts:
# 1) Build stage: use Maven + JDK to compile the code
# 2) Run stage: use a smaller JRE image to run the server

############################
# 1) Build stage
############################
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only the Maven file first, so Docker can cache dependencies.
COPY pom.xml .
# Download dependencies now (faster rebuilds later).
RUN mvn -q -DskipTests dependency:go-offline

# Now copy the source code and build it.
COPY src ./src
RUN mvn -q -DskipTests package
# After this, compiled classes are in /workspace/target/classes

############################
# 2) Run stage (smaller image)
############################
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy compiled classes and resources into the runtime image.
COPY --from=build /workspace/target/classes /app/classes
COPY src/main/resources /app/resources

# Default env values for local testing on Docker Desktop.
# - PROXY_HOST: where the proxy's RMI registry runs (host machine)
# - SERVER_HOST: what address the server advertises to clients (host machine)
# - SERVER_CACHE / SERVER_LRU: enable cache + policy
ENV PROXY_HOST=host.docker.internal \
    SERVER_HOST=host.docker.internal \
    SERVER_CACHE=false \
    SERVER_LRU=false \
    RMI_REGISTRY_PORT=1099 \
    SERVER_PORT=2000 \
    SERVER_NAME=server

# We only need to open the server's fixed RMI port
EXPOSE 2000

# Start the Java server
# We pass server.host so clients can reach the container via the host's address.
CMD ["bash","-lc","exec java -Dserver.host=$SERVER_HOST -cp /app/classes org.group5.server.ServerRunner"]
