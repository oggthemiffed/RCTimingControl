# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts VERSION ./
COPY buildSrc/ buildSrc/
COPY gradle/ gradle/
COPY app/ app/
COPY forwarder/ forwarder/
RUN chmod +x gradlew && \
    ./gradlew :forwarder:installDist -x test -x generateProto --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/forwarder/build/install/forwarder/ .
COPY docker/forwarder/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
