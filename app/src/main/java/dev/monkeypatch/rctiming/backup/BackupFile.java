package dev.monkeypatch.rctiming.backup;

import java.time.Instant;

/** One backup in the backup folder. */
public record BackupFile(String name, long sizeBytes, Instant createdAt, String reason) {
}
