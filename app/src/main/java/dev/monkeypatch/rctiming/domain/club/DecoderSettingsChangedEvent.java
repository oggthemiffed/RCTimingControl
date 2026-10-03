package dev.monkeypatch.rctiming.domain.club;

/**
 * Published after the club's decoder settings are committed, so the direct decoder listener can
 * reconnect to the new address without a restart.
 */
public record DecoderSettingsChangedEvent(DecoderSettings settings) {}
