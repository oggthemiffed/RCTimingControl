package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.forwarder.config.ForwarderConfig;
import dev.monkeypatch.rctiming.forwarder.grpc.ForwarderGrpcClient;
import dev.monkeypatch.rctiming.forwarder.timing.AmbRc4TimingSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the forwarder process.
 *
 * <p>Plan 04 wires: {@link ForwarderConfig} → {@link AmbRc4TimingSource} → {@link ForwarderGrpcClient} → cloud.
 * Parsed RC-4 passings flow: TCP decoder → parser → epoch correction → gRPC stream → cloud app.
 *
 * <p>T-05-05: API token presence is logged as {@code <set>} or {@code <unset>} — never the value.
 *
 * <p>FORWARDER-03 RESEND: RC-4 protocol has no RESEND mechanism. ForwarderCommand proto is defined
 * for future P3 binary use. In Phase 5, the cloud sends no RESEND commands.
 */
public class ForwarderApplication {

    private static final Logger log = LoggerFactory.getLogger(ForwarderApplication.class);

    /**
     * Resolves the forwarder configuration from command-line arguments.
     *
     * <p>If the first argument starts with {@code --config-file=}, the config is loaded from
     * the filesystem path that follows. Otherwise, the classpath default is used.
     *
     * <p>Package-private for testability — avoids calling {@link #main} which blocks forever.
     */
    static ForwarderConfig resolveConfig(String[] args) throws java.io.IOException {
        if (args.length > 0 && args[0].startsWith("--config-file=")) {
            String path = args[0].substring("--config-file=".length());
            return ForwarderConfig.load(java.nio.file.Path.of(path));
        }
        return ForwarderConfig.loadDefault();
    }

    public static void main(String[] args) throws Exception {
        ForwarderConfig cfg = resolveConfig(args);
        log.info("Forwarder starting — decoder={}:{}, grpc={}:{}",
                 cfg.decoderHost(), cfg.decoderPort(), cfg.grpcHost(), cfg.grpcPort());
        // T-05-05: log token presence only — never the value
        log.info("apiToken={}", cfg.apiToken() != null && !cfg.apiToken().isBlank() ? "<set>" : "<unset>");

        ForwarderGrpcClient grpcClient = ForwarderGrpcClient.fromConfig(cfg);
        grpcClient.connect();

        AmbRc4TimingSource source = new AmbRc4TimingSource(
            cfg.decoderHost(), cfg.decoderPort(),
            grpcClient::sendPassing,           // EpochCorrectedPassing → gRPC stream
            state -> {
                log.info("DECODER {}", state);
                grpcClient.sendDecoderStatus(state.name());
            });

        source.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            source.stop();
            grpcClient.close();
        }));

        Thread.currentThread().join(); // block forever
    }
}
