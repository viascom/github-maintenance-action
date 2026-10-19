#!/bin/bash
#
# Container entrypoint. dumb-init from the base image is PID 1 and forwards signals, so the
# JVM simply replaces this shell and its exit code becomes the container's.
set -euo pipefail

DEFAULT_JAVA_OPTS="-Dfile.encoding=UTF-8 \
-Duser.timezone=UTC \
-Djava.security.egd=file:/dev/urandom \
-XX:MaxRAMPercentage=70 \
-XX:+ExitOnOutOfMemoryError \
-XX:TieredStopAtLevel=1"

cd "/srv/${APP}"

# shellcheck disable=SC2086
exec /opt/java/bin/java ${DEFAULT_JAVA_OPTS} ${JAVA_OPTS:-} org.springframework.boot.loader.launch.JarLauncher
