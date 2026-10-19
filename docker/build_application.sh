#!/bin/bash
#
# Runs inside the builder stage and produces what the runtime stage copies:
#   /opt/java-runtime  a minimal JRE built with jlink from the modules the jar needs
#   ./extracted        the Spring Boot layers, one directory each
#
# jdeps cannot see modules a Spring Boot fat jar loads reflectively, so a small safety net
# (bean introspection via java.desktop, TLS providers, JMX, agents) is appended. A runtime
# failure of the form "module ... not found" means a module is missing from that list.
set -euo pipefail

JAR_FILE=application.jar
if [ ! -f "$JAR_FILE" ]; then
  echo "build.error: application jar is missing!" >&2
  exit 1
fi

jar xf "$JAR_FILE"

DETECTED_MODULES="$(jdeps \
  --print-module-deps \
  --ignore-missing-deps \
  --recursive \
  --multi-release 25 \
  --class-path="./BOOT-INF/lib/*" \
  --module-path="./BOOT-INF/lib/*" \
  "./$JAR_FILE")"
REQUIRED_MODULES="${DETECTED_MODULES},java.desktop,java.instrument,java.management,java.naming,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.unsupported,jdk.zipfs"
echo "Required java modules: $REQUIRED_MODULES"

jlink \
  --no-header-files \
  --no-man-pages \
  --compress zip-6 \
  --add-modules "$REQUIRED_MODULES" \
  --output /opt/java-runtime

java -Djarmode=tools -jar "$JAR_FILE" extract --layers --launcher --destination ./extracted

for layer in spring-boot-loader dependencies snapshot-dependencies application; do
  if [ ! -d "./extracted/$layer" ]; then
    echo "build.error: layer '$layer' was not extracted!" >&2
    exit 1
  fi
done
