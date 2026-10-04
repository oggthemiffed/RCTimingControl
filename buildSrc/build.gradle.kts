plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.flywaydb:flyway-core:10.20.1")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
}
