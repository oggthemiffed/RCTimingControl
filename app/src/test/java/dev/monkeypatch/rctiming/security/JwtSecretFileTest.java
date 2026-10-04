package dev.monkeypatch.rctiming.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Without JWT_SECRET, each install signs tokens with its own key, kept across restarts (#23). */
class JwtSecretFileTest {

    @TempDir Path dataDirectory;

    @Test
    void createsARandomKeyOnceAndReusesIt() throws Exception {
        String first = JwtSecretFile.loadOrCreate(dataDirectory.resolve("new-folder"));
        assertThat(Base64.getDecoder().decode(first)).hasSize(32);
        assertThat(JwtSecretFile.loadOrCreate(dataDirectory.resolve("new-folder"))).isEqualTo(first);
        assertThat(JwtSecretFile.loadOrCreate(dataDirectory)).isNotEqualTo(first);

        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Path file = dataDirectory.resolve("new-folder").resolve(JwtSecretFile.FILE_NAME);
            assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(file))).isEqualTo("rw-------");
        }
    }

    @Test
    void anEmptyKeyFileIsAnError() throws Exception {
        Files.writeString(dataDirectory.resolve(JwtSecretFile.FILE_NAME), "\n");
        assertThatThrownBy(() -> JwtSecretFile.loadOrCreate(dataDirectory))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is empty");
    }
}
