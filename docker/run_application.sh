#!/bin/bash -e

DEFAULT_JAVA_OPTS="-Dfile.encoding=UTF-8 \
-Duser.timezone=UTC \
-Djava.net.preferIPv4Stack=true \
-Djava.security.egd=file:/dev/urandom \
-Dsun.net.inetaddr.ttl=60 \
-Dsun.net.inetaddr.negative.ttl=5 \
-Djava.net.useSystemProxies=false \
-XX:+ExitOnOutOfMemoryError"

if [ -n "$JAVA_OPTS" ]; then
  JAVA_OPTS="$DEFAULT_JAVA_OPTS $JAVA_OPTS"
else
  JAVA_OPTS="$DEFAULT_JAVA_OPTS"
fi

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
  # Check if a process is still running
  kill -0 "$pid" 2>/dev/null
  if [ $? -ne 0 ]; then
    echo "Process $pid is not running, shutting down the container."
    term_handler
  fi

  sleep 5
done
