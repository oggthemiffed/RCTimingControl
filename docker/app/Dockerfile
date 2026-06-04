# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts VERSION ./
COPY buildSrc/ buildSrc/
COPY gradle/ gradle/
COPY app/ app/
COPY forwarder/ forwarder/
RUN chmod +x gradlew && \
    ./gradlew :app:bootJar -x test -x generateJooq -x generateProto --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /build/app/build/libs/app.jar app.jar
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
