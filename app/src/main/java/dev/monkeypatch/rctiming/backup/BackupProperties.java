package dev.monkeypatch.rctiming.backup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;

/**
 * {@code rctiming.backup.*}: where database backups go and how many are kept (#22).
 *
 * @param directory   folder for backups, which may be a USB stick or network share; defaults to
 *                    {@code backups} inside the data directory
 * @param keep        how many backups to keep; the oldest beyond this are deleted
 * @param nightlyCron when the nightly backup runs (Spring cron, server time); "-" turns it off
 */
@ConfigurationProperties(prefix = "rctiming.backup")
public record BackupProperties(
        Path directory,
        @DefaultValue("14") int keep,
        @DefaultValue("0 0 2 * * *") String nightlyCron) {
}
