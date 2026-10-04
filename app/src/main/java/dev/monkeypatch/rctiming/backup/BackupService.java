package dev.monkeypatch.rctiming.backup;

import dev.monkeypatch.rctiming.persistence.DatabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Copies the live database into the backup folder and keeps the newest few (#22). The copy is
 * made by the database vendor, so it is consistent even while a race is being timed.
 */
@Service
@EnableConfigurationProperties(BackupProperties.class)
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private static final Pattern NAME = Pattern.compile("rctiming-(\\d{8}-\\d{6})(?:-\\d+)?-([a-z-]+)\\.db");

    private final DatabaseProperties database;
    private final BackupProperties properties;

    public BackupService(DatabaseProperties database, BackupProperties properties) {
        this.database = database;
        this.properties = properties;
    }

    /** The backup folder; {@code backups} inside the data directory unless configured. */
    public Path directory() {
        return properties.directory() != null
                ? properties.directory()
                : database.effectiveDataDirectory().resolve("backups");
    }

    /**
     * Takes a backup now and deletes the oldest beyond the number kept.
     *
     * @param reason a short lower-case label stored in the file name, such as {@code manual}
     */
    public synchronized BackupFile backup(String reason) {
        try {
            Path folder = Files.createDirectories(directory());
            Path target = uniqueTarget(folder, Instant.now(), reason);
            Path partial = target.resolveSibling(target.getFileName() + ".partial");
            Files.deleteIfExists(partial);
            database.vendor().backup(database.effectiveDataDirectory(), partial);
            database.vendor().checkBackup(partial);
            Files.move(partial, target);
            log.info("Database backed up to {}", target);
            prune();
            return describe(target);
        } catch (IOException | SQLException e) {
            throw new BackupFailedException("Backup to " + directory() + " failed: " + e.getMessage(), e);
        }
    }

    /** Backups in the folder, newest first. */
    public List<BackupFile> list() {
        Path folder = directory();
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(f -> NAME.matcher(f.getFileName().toString()).matches())
                    .map(this::describe)
                    .sorted(Comparator.comparing(BackupFile::createdAt).thenComparing(BackupFile::name).reversed())
                    .toList();
        } catch (IOException e) {
            throw new BackupFailedException("Can't list backups in " + folder + ": " + e.getMessage(), e);
        }
    }

    private void prune() throws IOException {
        List<BackupFile> backups = list();
        for (BackupFile old : backups.subList(Math.min(properties.keep(), backups.size()), backups.size())) {
            Files.deleteIfExists(directory().resolve(old.name()));
            log.info("Deleted old backup {}", old.name());
        }
    }

    private static Path uniqueTarget(Path folder, Instant now, String reason) {
        String stamp = STAMP.format(now);
        Path target = folder.resolve("rctiming-" + stamp + "-" + reason + ".db");
        for (int n = 2; Files.exists(target); n++) {
            target = folder.resolve("rctiming-" + stamp + "-" + n + "-" + reason + ".db");
        }
        return target;
    }

    private BackupFile describe(Path file) {
        String name = file.getFileName().toString();
        Matcher matcher = NAME.matcher(name);
        try {
            Instant created = Files.getLastModifiedTime(file).toInstant();
            return new BackupFile(name, Files.size(file), created, matcher.matches() ? matcher.group(2) : "");
        } catch (IOException e) {
            throw new BackupFailedException("Can't read backup " + file + ": " + e.getMessage(), e);
        }
    }
}
