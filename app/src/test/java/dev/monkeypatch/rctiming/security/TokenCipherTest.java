package dev.monkeypatch.rctiming.security;

import dev.monkeypatch.rctiming.persistence.DatabaseProperties;
import dev.monkeypatch.rctiming.persistence.vendor.DatabaseVendor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Entry feed tokens are kept encrypted with a key in the data folder (#42). */
class TokenCipherTest {

    @TempDir Path dataDirectory;

    @Test
    void encryptsSoOnlyThisDataFoldersKeyCanReadTheToken() throws Exception {
        TokenCipher cipher = cipher(dataDirectory);

        String first = cipher.encrypt("feed-token");
        String second = cipher.encrypt("feed-token");

        assertThat(first).doesNotContain("feed-token").isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("feed-token");
        assertThat(cipher(dataDirectory).decrypt(second)).isEqualTo("feed-token");
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Path key = dataDirectory.resolve(TokenCipher.FILE_NAME);
            assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(key))).isEqualTo("rw-------");
        }
    }

    @Test
    void aTokenFromAnotherLaptopCantBeRead() {
        String elsewhere = cipher(dataDirectory.resolve("other-laptop")).encrypt("feed-token");

        assertThatThrownBy(() -> cipher(dataDirectory).decrypt(elsewhere))
                .isInstanceOf(TokenCipher.UnreadableTokenException.class);
        assertThatThrownBy(() -> cipher(dataDirectory).decrypt("not base64!"))
                .isInstanceOf(TokenCipher.UnreadableTokenException.class);
    }

    private static TokenCipher cipher(Path dataDirectory) {
        return new TokenCipher(new DatabaseProperties(DatabaseVendor.SQLITE, dataDirectory, List.of("db/migration"), 4));
    }
}
