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

    public static void main(String[] args) throws Exception {
        ForwarderConfig cfg = ForwarderConfig.loadDefault();
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
