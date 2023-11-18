#!/bin/bash

JAVA_OPTS="${JAVA_OPTS:="-Dfile.encoding=UTF-8 -Duser.timezone=UTC -XX:NativeMemoryTracking=summary -XX:+HeapDumpOnOutOfMemoryError"}"

cd /srv/"$APP" || exit

/opt/java/bin/java $JAVA_OPTS org.springframework.boot.loader.JarLauncher
