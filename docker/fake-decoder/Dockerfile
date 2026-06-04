# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts VERSION ./
COPY buildSrc/ buildSrc/
COPY gradle/ gradle/
COPY app/ app/
COPY forwarder/ forwarder/
RUN --mount=type=cache,target=/root/.gradle,id=gradle-fake-decoder \
    chmod +x gradlew && \
    ./gradlew :forwarder:generateProto --no-daemon 2>/dev/null || true && \
    find /root/.gradle -name "protoc-gen-grpc-java*.exe" -exec chmod +x {} \; && \
    ./gradlew :forwarder:installDist -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/forwarder/build/install/forwarder/ .
COPY docker/fake-decoder/simulate.sh /simulate.sh
RUN chmod +x /simulate.sh
EXPOSE 5100
ENTRYPOINT ["/simulate.sh"]
