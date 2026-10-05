package dev.monkeypatch.rctiming.backup;

import dev.monkeypatch.rctiming.persistence.DatabaseProperties;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * {@code java -jar app.jar restore <backup-file>} (#22): puts a backup in place as the database,
 * on a fresh install or one whose app is stopped. The data directory is found the way the app
 * finds it ({@code RCTIMING_DATA_DIR}, {@code --rctiming.database.data-directory=...}, a profile,
 * or the default app-data folder). The files it replaces are kept beside it. The next start of the
 * app migrates the restored database if it came from an older version.
 */
public final class RestoreCommand {

    private RestoreCommand() {
    }

    /** Binds only the database settings; nothing else of the app starts. */
    @EnableConfigurationProperties(DatabaseProperties.class)
    static class Settings {
    }

    /** Returns the process exit code. */
    public static int run(String[] args) {
        if (args.length == 0 || args[0].startsWith("--")) {
            System.err.println("Usage: java -jar app.jar restore <backup-file> [--rctiming.database.data-directory=<folder>]");
            return 2;
        }
        Path backup = Path.of(args[0]).toAbsolutePath();
        if (!Files.isRegularFile(backup)) {
            System.err.println("No backup file at " + backup);
            return 2;
        }
        String[] settings = Arrays.copyOfRange(args, 1, args.length);
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Settings.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .run(settings)) {
            DatabaseProperties database = context.getBean(DatabaseProperties.class);
            Path dataDirectory = database.effectiveDataDirectory().toAbsolutePath();
            database.vendor().restore(backup, dataDirectory);
            System.out.println("Restored " + backup + " into " + dataDirectory);
            return 0;
        } catch (Exception e) {
            System.err.println("Restore failed: " + e.getMessage());
            return 1;
        }
    }
}
