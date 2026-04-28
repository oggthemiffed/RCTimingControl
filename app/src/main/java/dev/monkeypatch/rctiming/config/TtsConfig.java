package dev.monkeypatch.rctiming.config;

import dev.monkeypatch.rctiming.infrastructure.tts.TtsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TtsProperties.class)
public class TtsConfig {
    // TtsProperties is a @ConfigurationProperties record — just needs registration.
}
