#!/usr/bin/env bash
set -euo pipefail

# Default RMI_HOST to the container's IP on its Docker network
RMI_HOST=${RMI_HOST:-$(hostname -i | awk '{print $1}')}
RMI_PORT=${RMI_PORT:-1099}
JAVA_OPTS=${JAVA_OPTS:-"-Xms256m -Xmx512m"}

# NOTE: /app/server.jar is the RMI server JAR
exec java $JAVA_OPTS -Djava.rmi.server.hostname="$RMI_HOST" -jar /app/server.jar "$@"
