package dev.monkeypatch.rctiming.api.localday.dto;

/** {@code status} is {@code "accepted"} or {@code "superseded"}; {@code :localday}'s current
 * client only inspects the HTTP status code, not this body — kept simple/informational. */
public record SnapshotIngestResponseDto(String status, long generation) {
}
