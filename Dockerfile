FROM eclipse-temurin:21-jdk-alpine AS BUILD_IMAGE
ENV APP_HOME /tmp/action
WORKDIR $APP_HOME
COPY . $APP_HOME
RUN chmod +x ./gradlew
RUN ./gradlew clean build --no-daemon --warn --stacktrace

FROM eclipse-temurin:21-jre-alpine

RUN apk update && apk add dumb-init

WORKDIR /opt/action
COPY --from=BUILD_IMAGE /tmp/action/build/libs/action.jar .
COPY entrypoint.sh .
RUN chmod +x /opt/action/entrypoint.sh

RUN addgroup -S party && adduser -S exie -G party
USER exie

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/opt/action/entrypoint.sh"]