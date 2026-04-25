plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("nu.studer.jooq")
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Separate configuration for jOOQ codegen classpath (Flyway + PG driver + TC)
val jooqCodegen: Configuration by configurations.creating

// Pin all jOOQ artifacts in jooqGenerator to the same version as version.set() below (3.19.24).
// The jooqGenerator config does not inherit Spring Boot BOM, so without this constraint
// jooq-meta and jooq resolve to 3.19.24 while jooq-codegen stays at 3.19.11, causing
// AbstractMethodError at codegen time. We use 3.19.24 for codegen; runtime stays at
// whatever Spring Boot BOM provides (also 3.19.x — compatible generated code).
configurations.named("jooqGenerator") {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jooq") {
            useVersion("3.19.24")
        }
    }
}

// Docker Engine 29.x raised the minimum client API version to 1.40.
// Testcontainers 1.20.6 (shaded docker-java) negotiates at v1.32 — rejected by Docker 29.x.
// Testcontainers 1.21.0+ fixes this (shaded docker-java 3.4.2+ uses 1.40 as minimum).
// Override the Spring Boot BOM managed testcontainers version.
ext["testcontainers.version"] = "1.21.3"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.jooq:jooq")

    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.11")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:minio")

    // D-22: MinIO-backed object storage via AWS S3 SDK v2 (also works against AWS S3)
    implementation(platform("software.amazon.awssdk:bom:2.25.60"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:auth")

    jooqGenerator("org.postgresql:postgresql")
    jooqGenerator("org.flywaydb:flyway-core")
    jooqGenerator("org.flywaydb:flyway-database-postgresql")
    jooqGenerator("org.slf4j:slf4j-simple:2.0.13")

    // Separate config used by the flywayMigrateForCodegen JavaExec task
    jooqCodegen("org.postgresql:postgresql")
    jooqCodegen("org.flywaydb:flyway-core")
    jooqCodegen("org.flywaydb:flyway-database-postgresql")
    jooqCodegen("org.slf4j:slf4j-simple:2.0.13")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Docker Engine 29.x requires API version >= 1.40.
    // Testcontainers hardcodes VERSION_1_32 as the default when no api.version is configured.
    // The docker-java shaded code in testcontainers reads "api.version" from JVM system properties
    // via overrideDockerPropertiesWithSystemProperties — setting it here bypasses the 1.32 fallback.
    jvmArgs("-Dapi.version=1.47")
}

// ---------------------------------------------------------------------------
// jOOQ codegen pipeline:
//   startJooqDb → waitForJooqDb → flywayMigrateForCodegen → generateJooq → stopJooqDb
//
// Override via env vars JOOQ_JDBC_URL / JOOQ_JDBC_USER / JOOQ_JDBC_PASSWORD
// to point at a pre-existing migrated database (skips Docker tasks).
// ---------------------------------------------------------------------------

val jooqContainerName = "rctiming-jooq-codegen"
val jooqDbPort = 54320
val jooqJdbcUser = System.getenv("JOOQ_JDBC_USER") ?: "jooq"
val jooqJdbcPassword = System.getenv("JOOQ_JDBC_PASSWORD") ?: "jooq"
val jooqJdbcUrl = System.getenv("JOOQ_JDBC_URL")
    ?: "jdbc:postgresql://localhost:$jooqDbPort/jooq"
val useExternalDb = System.getenv("JOOQ_JDBC_URL") != null

val startJooqDb by tasks.registering(Exec::class) {
    group = "jooq"
    description = "Start a temporary Postgres container for jOOQ codegen"
    onlyIf { !useExternalDb }
    commandLine(
        "bash", "-c",
        "docker rm -f $jooqContainerName 2>/dev/null || true; " +
        "docker run --rm -d " +
        "--name $jooqContainerName " +
        "-e POSTGRES_USER=$jooqJdbcUser " +
        "-e POSTGRES_PASSWORD=$jooqJdbcPassword " +
        "-e POSTGRES_DB=jooq " +
        "-p $jooqDbPort:5432 " +
        "postgres:16-alpine"
    )
    isIgnoreExitValue = false
}

val waitForJooqDb by tasks.registering(Exec::class) {
    group = "jooq"
    description = "Wait until the jOOQ codegen Postgres container is ready"
    dependsOn(startJooqDb)
    onlyIf { !useExternalDb }
    commandLine(
        "bash", "-c",
        "for i in \$(seq 1 30); do docker exec $jooqContainerName pg_isready -U $jooqJdbcUser -d jooq -q && exit 0; sleep 1; done; echo 'Postgres not ready' >&2; exit 1"
    )
}

// Run Flyway migrations against the codegen DB using buildSrc FlywayMigrator
val flywayMigrateForCodegen by tasks.registering {
    group = "jooq"
    description = "Run Flyway migrations against the jOOQ codegen database"
    dependsOn(waitForJooqDb)
    inputs.dir("src/main/resources/db/migration")
    doFirst {
        dev.monkeypatch.build.FlywayMigrator.migrate(
            jooqJdbcUrl,
            jooqJdbcUser,
            jooqJdbcPassword,
            "filesystem:${project.projectDir}/src/main/resources/db/migration"
        )
    }
}

val stopJooqDb by tasks.registering(Exec::class) {
    group = "jooq"
    description = "Stop the temporary jOOQ codegen Postgres container"
    onlyIf { !useExternalDb }
    commandLine("docker", "stop", jooqContainerName)
    isIgnoreExitValue = true
}

jooq {
    version.set("3.19.24")
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = jooqJdbcUrl
                    user = jooqJdbcUser
                    password = jooqJdbcPassword
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        excludes = "flyway_schema_history"
                    }
                    generate.apply {
                        isRecords = true
                        isImmutablePojos = false
                        isFluentSetters = false
                    }
                    target.apply {
                        packageName = "dev.monkeypatch.rctiming.jooq.generated"
                        directory = "build/generated-sources/jooq"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

tasks.withType<nu.studer.gradle.jooq.JooqGenerate>().configureEach {
    dependsOn(flywayMigrateForCodegen)
    finalizedBy(stopJooqDb)
    inputs.dir("src/main/resources/db/migration")
}
