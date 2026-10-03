package dev.monkeypatch.rctiming.localday.sync;

/**
 * Published to request an immediate snapshot push outside {@link SnapshotPushService}'s regular
 * 20 s cadence (KTD8) — fired on a race-state transition and on a fresh check-in confirmation.
 * A plain POJO event: Spring's {@code ApplicationEventPublisher} supports arbitrary event
 * objects since 4.2, no {@code ApplicationEvent} base class required.
 */
public record SnapshotTriggerEvent(String reason) {
}
