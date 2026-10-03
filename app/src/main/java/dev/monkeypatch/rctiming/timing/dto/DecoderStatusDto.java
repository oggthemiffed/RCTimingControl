package dev.monkeypatch.rctiming.timing.dto;

/**
 * Broadcast on /topic/system/decoder-status when the decoder connection state changes.
 * Values are "CONNECTED", "RECONNECTING" or "DISCONNECTED".
 */
public record DecoderStatusDto(String decoderState) {}
