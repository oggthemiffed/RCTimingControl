plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

version = file("../VERSION").readText().trim()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

springBoot {
    buildInfo()
}

// Pin the embedded Postgres *engine* version (not the embedded-postgres library version) so the
// vendored binaries match the cloud app's PostgreSQL 16 — same engine/dialect/JSONB support.
// This BOM only supplies platform-binary artifact versions; it does not affect the
// io.zonky.test:embedded-postgres library version below.
val embeddedPostgresBinariesVersion = "16.14.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")

    // Shared, dependency-free RC-4 decoder protocol module. Not wired up yet in this unit —
    // will be used once the local TCP receiver / lap ingestion lands.
    implementation(project(":decoder-protocol"))

    // Embedded PostgreSQL: the durable local store for a venue laptop running fully offline.
    // Deliberately not H2 (flat-file storage risks corruption on unclean shutdown — the exact
    // failure mode a venue laptop hits) and not SQLite (a second ORM dialect). Embedded Postgres
    // gives the same engine/dialect/JSONB support as the cloud app's real PostgreSQL 16.
    implementation("io.zonky.test:embedded-postgres:2.2.2")

    // Vendor the platform binaries into the build so they ship with the artifact instead of being
    // fetched over the network on first launch — this app must work with zero internet access at
    // the venue. Cover the platforms officials are likely to run the local app on.
    implementation(platform("io.zonky.test.postgres:embedded-postgres-binaries-bom:$embeddedPostgresBinariesVersion"))
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64")
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-darwin-amd64")
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8")
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-windows-amd64")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("localday.jar")
}
