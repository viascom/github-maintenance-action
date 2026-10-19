# Two-stage build in the Viascom convention: the builder (full JDK) jlinks a minimal,
# application-specific JRE and explodes the Spring Boot jar into its layers; the runtime
# (viascom/ubuntu, non-root, dumb-init as PID 1) ships only that JRE and those layers.
#
# The build context is the repository root:
#   ./gradlew build && docker build -f docker/Dockerfile -t github-maintenance-action .

FROM viascom/jdk:25.0.4.1@sha256:97c68b1948f5a586e8a7a03b54598e77edf67811dc08d51fcea5971e0650cc35 AS builder
ARG JAR_FILE=build/libs/github-maintenance-action.jar
USER root
WORKDIR /tmp/application
COPY $JAR_FILE application.jar
COPY docker/build_application.sh ./
RUN bash build_application.sh

FROM viascom/ubuntu:26.04@sha256:126c749c6bb0e08ab3afc4b58e494b8e20f25ccbe803441c9cdd97696c055341 AS runtime

ARG APP_NAME=github-maintenance-action

ENV APP=$APP_NAME \
    JAVA_HOME=/opt/java \
    PATH="/opt/java/bin:${PATH}"

# A Docker container action runs as root and without a WORKDIR: the runner mounts its
# workspace and the GITHUB_OUTPUT / GITHUB_STEP_SUMMARY files into the container, and only
# root is guaranteed to write them (https://docs.github.com/en/actions/sharing-automations/creating-actions/dockerfile-support-for-github-actions).
USER root

COPY --chmod=755 docker/run_application.sh /etc/run_application.sh
COPY --from=builder /opt/java-runtime $JAVA_HOME

# Most stable layer first, so a rebuild with unchanged dependencies reuses the cache.
COPY --from=builder /tmp/application/extracted/spring-boot-loader/ /srv/$APP_NAME/
COPY --from=builder /tmp/application/extracted/dependencies/ /srv/$APP_NAME/
COPY --from=builder /tmp/application/extracted/snapshot-dependencies/ /srv/$APP_NAME/
COPY --from=builder /tmp/application/extracted/application/ /srv/$APP_NAME/

CMD ["/etc/run_application.sh"]
