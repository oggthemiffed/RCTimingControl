package dev.monkeypatch.rctiming.localday.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * A single injectable {@link Clock} bean so time-sensitive logic (currently
 * {@code SnapshotPushService}'s backoff timing, U11) can be driven by a fixed/controllable clock
 * in tests instead of calling {@code Instant.now()} directly.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
