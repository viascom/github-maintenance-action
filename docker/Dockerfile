# Build-time container
FROM viascom/jdk:21.0.7 as builder
ARG JAR_FILE
USER root
WORKDIR /tmp/application
COPY $JAR_FILE application.jar
COPY build_application.sh ./
RUN bash build_application.sh

# Run-time container
FROM viascom/ubuntu:24.04.02

ARG APP_NAME=github-maintenance-action
ARG LOG_FOLDER=/srv/github-maintenance-action/logs

ENV APP=$APP_NAME \
    JAVA_HOME=/opt/java \
    PATH="${JAVA_HOME}/bin:${PATH}"

USER root

## Adding programs for operation
COPY packages.list /tmp

RUN apt-get update && apt-get upgrade --yes && apt-get autoremove --yes; \
    xargs -r apt-get install --yes --no-install-recommends < /tmp/packages.list; \
    apt-get clean && rm -rf /var/lib/apt/lists /var/cache/apt/archives /tmp/* /var/tmp/*

WORKDIR /srv/$APP

COPY run_application.sh /etc

RUN mkdir -p $LOG_FOLDER; \
    chgrp $GROUP $LOG_FOLDER; \
    chmod g+rwx $LOG_FOLDER; \
    chmod +x /etc/run_application.sh

## Application-specif created JRE
COPY --from=builder /opt/java-runtime $JAVA_HOME

## Spring Boot Layers
COPY --from=builder /tmp/application/spring-boot-loader/ ./
COPY --from=builder /tmp/application/dependencies/ ./
COPY --from=builder /tmp/application/snapshot-dependencies/ ./
COPY --from=builder /tmp/application/application/ ./

USER $USER

CMD ["/etc/run_application.sh"]
