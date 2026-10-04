plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("nu.studer.jooq")
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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.flywaydb:flyway-core")

    // #26: the app keeps its data in one SQLite file; only persistence/ may use these directly
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("org.hibernate.orm:hibernate-community-dialects")

    implementation("org.jooq:jooq")

    // L1: the app reads the decoder directly (DecoderListener) using the shared RC-4 parser
    implementation(project(":decoder-protocol"))

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    jooqGenerator("org.xerial:sqlite-jdbc:3.50.3.0")
    jooqGenerator("org.slf4j:slf4j-simple:2.0.13")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ---------------------------------------------------------------------------
// jOOQ codegen: Flyway builds a throwaway SQLite file from the migrations, then jOOQ reads it.
//   flywayMigrateForCodegen → generateJooq
// ---------------------------------------------------------------------------

val jooqSchemaFile = layout.buildDirectory.file("jooq-codegen/schema.db").get().asFile
val jooqJdbcUrl = "jdbc:sqlite:${jooqSchemaFile.absolutePath}"
val migrationsDir = "src/main/resources/db/migration/sqlite"

val flywayMigrateForCodegen by tasks.registering {
    group = "jooq"
    description = "Build a SQLite schema from the migrations for jOOQ codegen"
    inputs.dir(migrationsDir)
    doFirst {
        jooqSchemaFile.parentFile.mkdirs()
        jooqSchemaFile.delete()
        dev.monkeypatch.build.FlywayMigrator.migrate(jooqJdbcUrl, "filesystem:${project.projectDir}/$migrationsDir")
    }
}

jooq {
    version.set("3.19.24")
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.sqlite.JDBC"
                    url = jooqJdbcUrl
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.sqlite.SQLiteDatabase"
                        excludes = "flyway_schema_history"
                        forcedTypes.addAll(listOf(
                            // Primary keys are INTEGER (SQLite's rowid alias); read them as Long like
                            // every other id column
                            org.jooq.meta.jaxb.ForcedType()
                                .withName("BIGINT")
                                .withIncludeExpression(".*\\.ID")
                                .withIncludeTypes("INTEGER"),
                            // Timestamps are BIGINT UTC microseconds (see V1); read them as Instant
                            org.jooq.meta.jaxb.ForcedType()
                                .withUserType("java.time.Instant")
                                .withConverter("dev.monkeypatch.rctiming.persistence.convert.InstantMicrosConverter")
                                .withIncludeExpression(".*\\..*_AT|PRACTICE_LAPS\\.CROSSING_TIME")
                                .withIncludeTypes("BIGINT")
                        ))
                    }
                    generate.apply {
                        isRecords = true
                        isImmutablePojos = false
                        isFluentSetters = false
                    }
                    target.apply {
                        packageName = "dev.monkeypatch.rctiming.jooq.generated"
                        directory = "src/generated/jooq"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

tasks.withType<nu.studer.gradle.jooq.JooqGenerate>().configureEach {
    dependsOn(flywayMigrateForCodegen)
    inputs.dir(migrationsDir)
    val settingsFile = file("src/jooq/jooq-codegen-settings.xml")
    inputs.file(settingsFile)
    javaExecSpec = Action { systemProperty("org.jooq.settings", settingsFile.absolutePath) }
}

// jOOQ generated sources are committed at app/src/generated/jooq (Docker builds skip codegen with -x generateJooq)
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

sourceSets["main"].java.srcDir("src/generated/jooq")
