FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the server jar - replace it with the real jar after merging
COPY server.jar /app/server.jar
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# RMI registry / server port (Change and use the correct port number)
EXPOSE 1099

# Configurable via -e RMI_HOST=... -e RMI_PORT=...
ENV RMI_PORT=1099
CMD ["/app/entrypoint.sh"]
