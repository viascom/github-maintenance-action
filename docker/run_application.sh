#!/bin/bash

JAVA_OPTS="${JAVA_OPTS:="-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Duser.timezone=UTC -XX:NativeMemoryTracking=summary -XX:+HeapDumpOnOutOfMemoryError"}"
PORT="${PORT:="8080"}"
pid=0

term_handler() {
  if [ $pid -ne 0 ]; then
    # Check if the process is still running before attempting to kill it
    if kill -0 "$pid" 2>/dev/null; then
      kill -SIGTERM "$pid"
      wait "$pid"
    else
      echo "Process $pid is not running"
    fi
  fi
  exit 143 # 128 + 15 -- SIGTERM
}

# Set up a trap to handle SIGTERM signal (e.g., when Docker stops the container)
# This ensures proper cleanup by killing the current background process and calling term_handler
trap 'kill ${!}; term_handler' SIGTERM

cd /srv/"$APP" || exit

/opt/java/bin/java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher --bind 0.0.0.0:$PORT &
# $! contains the PID of the most recently started background process
pid="$!"

while true; do
  # The kill -0 command checks if the process exists without sending a signal
  # If the process doesn't exist, the command returns non-zero
  kill -0 "$pid" 2>/dev/null
  if [ $? -ne 0 ]; then
    echo "Process $pid is not running, shutting down the container."
    term_handler
  fi

  sleep 5
done
