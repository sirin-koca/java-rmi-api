FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY ServerInterface.java Server.java ServerMain.java ./
RUN javac *.java
EXPOSE 1099
CMD ["java", "ServerMain"]
