# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts VERSION ./
COPY buildSrc/ buildSrc/
COPY gradle/ gradle/
COPY app/ app/
COPY decoder-simulator/ decoder-simulator/
COPY decoder-protocol/ decoder-protocol/
RUN chmod +x gradlew && \
    ./gradlew :decoder-simulator:installDist -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/decoder-simulator/build/install/decoder-simulator/ .
COPY docker/fake-decoder/simulate.sh /simulate.sh
RUN chmod +x /simulate.sh
EXPOSE 5100
ENTRYPOINT ["/simulate.sh"]
