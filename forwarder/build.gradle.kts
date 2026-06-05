plugins {
    java
    application
    id("com.google.protobuf") version "0.9.4"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:4.1.121.Final")
    implementation("io.grpc:grpc-stub:1.73.0")
    implementation("io.grpc:grpc-protobuf:1.73.0")
    implementation("io.grpc:grpc-netty-shaded:1.73.0")
    implementation("com.google.protobuf:protobuf-java:3.25.8")
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.8" }
    plugins {
        create("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.73.0" }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins { create("grpc") }
        }
    }
    generatedFilesBaseDir = "src/generated/proto"
}

// Docker builds skip generateProto (-x generateProto) so the plugin never registers its output dirs.
// Pass -PcommittedProto to put the committed generated sources on the source path in that case.
if (project.hasProperty("committedProto")) {
    sourceSets["main"].java.srcDir("src/generated/proto/main/java")
    sourceSets["main"].java.srcDir("src/generated/proto/main/grpc")
}

application {
    mainClass.set("dev.monkeypatch.rctiming.forwarder.ForwarderApplication")
}

tasks.register<JavaExec>("runSimulator") {
    group = "application"
    description = "Run the AMB RC-4 TCP decoder simulator"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain")
    // Pass-through args: ./gradlew :forwarder:runSimulator --args="--mode=generative --port=5100"
}

tasks.test {
    useJUnitPlatform()
}
