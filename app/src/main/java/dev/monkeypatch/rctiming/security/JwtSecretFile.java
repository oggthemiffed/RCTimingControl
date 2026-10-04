package dev.monkeypatch.rctiming.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * The key officials' sign-in tokens are signed with, when {@code JWT_SECRET} is not set (#23).
 *
 * <p>An installed copy is reachable from every device on the venue network, so it must not sign
 * tokens with a key anyone can read in the source. The first start writes a random key to
 * {@code jwt-secret} in the data folder and later starts reuse it, so sign-ins survive restarts.
 */
final class JwtSecretFile {

    static final String FILE_NAME = "jwt-secret";

    private JwtSecretFile() {
    }

    /** Returns the base64 key stored in {@code dataDirectory}, creating it on first use. */
    static String loadOrCreate(Path dataDirectory) {
        Path file = dataDirectory.resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(dataDirectory);
                byte[] key = new byte[32];
                new SecureRandom().nextBytes(key);
                try {
                    Files.writeString(file, Base64.getEncoder().encodeToString(key), StandardCharsets.US_ASCII,
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    restrictToOwner(file);
                } catch (FileAlreadyExistsException raced) {
                    // Another process wrote it first; use theirs
                }
            }
            String secret = Files.readString(file, StandardCharsets.US_ASCII).trim();
            if (secret.isEmpty()) {
                throw new IllegalStateException(file + " is empty; delete it to generate a new key");
            }
            return secret;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read or create the sign-in key at " + file, e);
        }
    }

    private static void restrictToOwner(Path file) throws IOException {
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException windows) {
            // Windows: ProgramData files inherit the folder's ACL, which only lets users read
        }
    }
}
