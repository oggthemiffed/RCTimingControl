package dev.monkeypatch.rctiming.persistence;

import java.nio.file.Path;
import java.util.Locale;

/** Where the app keeps its data when no folder is configured: the usual per-user app-data folder. */
public final class DataDirectories {

    private static final String APP_FOLDER = "RCTimingControl";

    private DataDirectories() {
    }

    public static Path defaultDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            return Path.of(localAppData != null ? localAppData : home, APP_FOLDER);
        }
        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", APP_FOLDER);
        }
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        return xdgDataHome != null && !xdgDataHome.isBlank()
                ? Path.of(xdgDataHome, APP_FOLDER.toLowerCase(Locale.ROOT))
                : Path.of(home, ".local", "share", APP_FOLDER.toLowerCase(Locale.ROOT));
    }
}
