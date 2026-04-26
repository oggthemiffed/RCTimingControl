package dev.monkeypatch.rctiming.forwarder.dto;

/**
 * Phase 5: broadcast on /topic/system/forwarder-status when forwarder/decoder state changes.
 * Both fields use string values: "CONNECTED", "RECONNECTING", "DISCONNECTED".
 */
public record ForwarderStatusDto(String decoderState, String forwarderState) {}
