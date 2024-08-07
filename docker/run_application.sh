#!/bin/bash

JAVA_OPTS="${JAVA_OPTS:="-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Duser.timezone=UTC -XX:NativeMemoryTracking=summary -XX:+HeapDumpOnOutOfMemoryError"}"
PORT="${PORT:="8080"}"

term_handler() {
  if [ $pid -ne 0 ]; then
    kill -SIGTERM "$pid"
    wait "$pid"
  fi
  exit 143 # 128 + 15 -- SIGTERM
}

trap 'kill ${!}; term_handler' SIGTERM

cd /srv/"$APP" || exit

/opt/java/bin/java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher --bind 0.0.0.0:$PORT &
pid="$!"

while true; do
  kill -0 "$pid" 2>/dev/null
  if [ $? -ne 0 ]; then
    echo "Process $pid is not running, shutting down the container."
    term_handler
  fi

  sleep 5
done
