# Build-time container
FROM eclipse-temurin:21-jdk-alpine as builder
USER root
ENV APP_HOME /tmp/action
WORKDIR $APP_HOME
COPY . $APP_HOME
RUN chmod +x ./gradlew
RUN ./gradlew clean build --no-daemon --warn --stacktrace

FROM eclipse-temurin:21-jre-alpine
# Run-time container
FROM viascom/jre:17.0.8

ARG ACTION_NAME=github-maintenance-action

ENV ACTION=$ACTION_NAME \
    JAVA_HOME=/opt/java \
    PATH="${JAVA_HOME}/bin:${PATH}"

USER root
WORKDIR /opt/$ACTION

COPY run_application.sh /etc
RUN chmod +x /etc/run_application.sh

## Spring Boot Layers
COPY --from=builder /tmp/application/spring-boot-loader/ ./
COPY --from=builder /tmp/application/dependencies/ ./
COPY --from=builder /tmp/application/snapshot-dependencies/ ./
COPY --from=builder /tmp/application/application/ ./

USER $USER

CMD ["/etc/run_application.sh"]