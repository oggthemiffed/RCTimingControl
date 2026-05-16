package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.forwarder.config.ForwarderConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ForwarderApplication#resolveConfig(String[])} arg parsing.
 *
 * <p>Plan 10-01: proves both the --config-file path branch and the classpath-default fallback.
 */
class ForwarderApplicationConfigArgTest {

    @TempDir
    Path tempDir;

    @Test
    void configFileArg_loadsFromSpecifiedPath() throws IOException {
        // Arrange: write a temp properties file with a custom decoder host
        Path propsFile = tempDir.resolve("test-forwarder.properties");
        Files.writeString(propsFile,
                "forwarder.decoder.host=testhost\n" +
                "forwarder.decoder.port=5100\n" +
                "forwarder.grpc.host=localhost\n" +
                "forwarder.grpc.port=9090\n" +
                "forwarder.grpc.plaintext=true\n" +
                "forwarder.api-token=test-token\n");

        // Act
        ForwarderConfig cfg = ForwarderApplication.resolveConfig(
                new String[]{"--config-file=" + propsFile.toAbsolutePath()});

        // Assert
        assertThat(cfg.decoderHost()).isEqualTo("testhost");
    }

    @Test
    void noArgs_fallsBackToClasspathDefault() throws IOException {
        // Act
        ForwarderConfig cfg = ForwarderApplication.resolveConfig(new String[0]);

        // Assert: classpath forwarder.properties has decoderHost=localhost
        assertThat(cfg.decoderHost()).isEqualTo("localhost");
    }

    @Test
    void unrecognisedArg_fallsBackToClasspathDefault() throws IOException {
        // Act
        ForwarderConfig cfg = ForwarderApplication.resolveConfig(new String[]{"--something"});

        // Assert: unknown arg triggers classpath default
        assertThat(cfg.decoderHost()).isEqualTo("localhost");
    }
}
