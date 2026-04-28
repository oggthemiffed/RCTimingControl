package dev.monkeypatch.rctiming.infrastructure.tts;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tts")
public record TtsProperties(String endpoint, String defaultVoice, boolean enabled) {}
