package dev.monkeypatch.rctiming.localday.sync.dto;

/**
 * Outbound body for the periodic snapshot push (KTD4/KTD8) — the cloud's (not-yet-built, U14)
 * {@code SnapshotIngestController} is the eventual consumer of this exact shape. {@code
 * generation} is this instance's day-open generation (KTD4's fencing token, fixed for the whole
 * session); {@code snapshotId} is this attempt's idempotency key, reused across retries of
 * identical content by {@link dev.monkeypatch.rctiming.localday.sync.SnapshotPushService}.
 */
public record SnapshotRequest(String instanceId, long generation, String snapshotId, SnapshotPayload payload) {
}
