package dev.monkeypatch.rctiming.backup;

/** A backup could not be written, for example because the backup folder is missing or full. */
public class BackupFailedException extends RuntimeException {

    public BackupFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
