package dev.monkeypatch.rctiming.forwarder.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Plain-Java configuration loader for the forwarder process.
 *
 * <p>Reads {@code forwarder.properties} from a file-system path ({@link #load(Path)}) or
 * from the classpath ({@link #loadDefault()}). No Spring dependencies.
 *
 * <h3>Required keys</h3>
 * <ul>
 *   <li>{@code forwarder.api-token} — pre-shared API token for gRPC authentication (D-08)</li>
 * </ul>
 *
 * <h3>Optional keys (with defaults)</h3>
 * <ul>
 *   <li>{@code forwarder.decoder.host} — default {@code localhost}</li>
 *   <li>{@code forwarder.decoder.port} — default {@code 5100}</li>
 *   <li>{@code forwarder.grpc.host} — default {@code localhost}</li>
 *   <li>{@code forwarder.grpc.port} — default {@code 9090}</li>
 *   <li>{@code forwarder.grpc.plaintext} — default {@code true}</li>
 * </ul>
 */
public class ForwarderConfig {

    private static final Logger log = LoggerFactory.getLogger(ForwarderConfig.class);

    private final String  apiToken;
    private final String  decoderHost;
    private final int     decoderPort;
    private final String  grpcHost;
    private final int     grpcPort;
    private final boolean grpcPlaintext;

    private ForwarderConfig(Properties p) {
        this.apiToken      = p.getProperty("forwarder.api-token", "");
        this.decoderHost   = p.getProperty("forwarder.decoder.host", "localhost");
        this.decoderPort   = Integer.parseInt(p.getProperty("forwarder.decoder.port", "5100"));
        this.grpcHost      = p.getProperty("forwarder.grpc.host", "localhost");
        this.grpcPort      = Integer.parseInt(p.getProperty("forwarder.grpc.port", "9090"));
        this.grpcPlaintext = Boolean.parseBoolean(p.getProperty("forwarder.grpc.plaintext", "true"));
    }

    /** Load configuration from a file-system path. */
    public static ForwarderConfig load(Path file) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        }
        log.info("Loaded forwarder config from {}", file);
        return new ForwarderConfig(p);
    }

    /** Load configuration from {@code forwarder.properties} on the classpath. */
    public static ForwarderConfig loadDefault() throws IOException {
        InputStream in = ForwarderConfig.class.getClassLoader()
                                              .getResourceAsStream("forwarder.properties");
        if (in == null) {
            log.warn("forwarder.properties not found on classpath — using all defaults");
            return new ForwarderConfig(new Properties());
        }
        Properties p = new Properties();
        try (in) { p.load(in); }
        log.info("Loaded default forwarder.properties from classpath");
        return new ForwarderConfig(p);
    }

    public String  apiToken()      { return apiToken; }
    public String  decoderHost()   { return decoderHost; }
    public int     decoderPort()   { return decoderPort; }
    public String  grpcHost()      { return grpcHost; }
    public int     grpcPort()      { return grpcPort; }
    public boolean grpcPlaintext() { return grpcPlaintext; }
}
