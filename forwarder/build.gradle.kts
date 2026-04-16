plugins { java }

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Forwarder implementation deferred to Phase 5
