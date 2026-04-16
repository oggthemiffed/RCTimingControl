plugins {
    id("org.springframework.boot") version "3.4.7" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    java
}

allprojects {
    group = "dev.monkeypatch"
    version = "0.0.1-SNAPSHOT"
    repositories { mavenCentral() }
}
