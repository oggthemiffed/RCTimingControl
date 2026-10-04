package dev.monkeypatch.rctiming.persistence;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Where data lives by default: per user when run by hand, per machine when installed (#23). */
class DataDirectoriesTest {

    private static final Map<String, String> WINDOWS_ENV =
            Map.of("LOCALAPPDATA", "C:\\Users\\dave\\AppData\\Local", "ProgramData", "D:\\ProgramData");

    @Test
    void runByHandUsesThePerUserFolder() {
        assertThat(DataDirectories.defaultDirectory("Windows 11", "C:\\Users\\dave", WINDOWS_ENV, false))
                .isEqualTo(Path.of("C:\\Users\\dave\\AppData\\Local", "RCTimingControl"));
        assertThat(DataDirectories.defaultDirectory("Mac OS X", "/Users/dave", Map.of(), false))
                .isEqualTo(Path.of("/Users/dave/Library/Application Support/RCTimingControl"));
        assertThat(DataDirectories.defaultDirectory("Linux", "/home/dave", Map.of(), false))
                .isEqualTo(Path.of("/home/dave/.local/share/rctimingcontrol"));
    }

    @Test
    void theInstalledServiceUsesThePerMachineFolder() {
        assertThat(DataDirectories.defaultDirectory("Windows 11", "C:\\Windows\\system32", WINDOWS_ENV, true))
                .isEqualTo(Path.of("D:\\ProgramData", "RCTimingControl"));
        assertThat(DataDirectories.defaultDirectory("Windows 11", "C:\\Windows\\system32", Map.of(), true))
                .isEqualTo(Path.of("C:\\ProgramData", "RCTimingControl"));
        assertThat(DataDirectories.defaultDirectory("Mac OS X", "/var/root", Map.of(), true))
                .isEqualTo(Path.of("/Library/Application Support/RCTimingControl"));
        assertThat(DataDirectories.defaultDirectory("Linux", "/root", Map.of("XDG_DATA_HOME", "/root/xdg"), true))
                .isEqualTo(Path.of("/var/lib/rctimingcontrol"));
    }

    @Test
    void theScopeComesFromTheLauncherProperty() {
        String before = System.getProperty(DataDirectories.SCOPE_PROPERTY);
        try {
            System.setProperty(DataDirectories.SCOPE_PROPERTY, "machine");
            assertThat(DataDirectories.isMachineScope()).isTrue();
            System.setProperty(DataDirectories.SCOPE_PROPERTY, "user");
            assertThat(DataDirectories.isMachineScope()).isFalse();
        } finally {
            if (before == null) {
                System.clearProperty(DataDirectories.SCOPE_PROPERTY);
            } else {
                System.setProperty(DataDirectories.SCOPE_PROPERTY, before);
            }
        }
    }
}
