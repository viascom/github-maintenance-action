#!/bin/bash
set -e

# --- Default Configuration ---
DEFAULT_JAVA_OPTS="-Dfile.encoding=UTF-8 \
-Duser.timezone=UTC \
-Djava.net.preferIPv4Stack=true \
-Djava.security.egd=file:/dev/urandom \
-Dsun.net.inetaddr.ttl=60 \
-Dsun.net.inetaddr.negative.ttl=5 \
-Djava.net.useSystemProxies=false \
-XX:+ExitOnOutOfMemoryError"

# Merge provided JAVA_OPTS with defaults
JAVA_OPTS="${DEFAULT_JAVA_OPTS} ${JAVA_OPTS}"

# Set default port if not provided
PORT="${PORT:-"8080"}"

# Ensure APP is set
if [ -z "$APP" ]; then
  echo "Error: APP environment variable is not set."
  exit 1
fi

# --- Signal Handling ---
pid=0

term_handler() {
  echo "Received SIGTERM, shutting down gracefully..."
  if [ "$pid" -ne 0 ]; then
    kill -SIGTERM "$pid"
    wait "$pid"
  fi
  exit 0
}

# Trap SIGTERM and SIGINT for graceful shutdown
trap term_handler SIGTERM SIGINT

# --- Application Startup ---
cd "/srv/${APP}" || { echo "Error: Could not change directory to /srv/${APP}"; exit 1; }

echo "Starting application ${APP}..."
# shellcheck disable=SC2086
/opt/java/bin/java ${JAVA_OPTS} org.springframework.boot.loader.launch.JarLauncher --bind 0.0.0.0:"${PORT}" &
pid="$!"

# Wait for the process to finish and capture its exit code
# In bash, if the wait is interrupted by a signal for which a trap has been set,
# then the wait returns immediately with an exit status greater than 128.
wait "$pid"
exit_code=$?

echo "Application process (PID: $pid) exited with code $exit_code"
exit "$exit_code"
