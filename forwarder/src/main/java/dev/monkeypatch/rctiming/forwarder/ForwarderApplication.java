package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.forwarder.config.ForwarderConfig;
import dev.monkeypatch.rctiming.forwarder.timing.AmbRc4TimingSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the forwarder process.
 *
 * <p>Plan 02 wires: {@link ForwarderConfig} → {@link AmbRc4TimingSource} → log passings.
 * Plan 04 will add the gRPC client and replace the log callback with a gRPC stream emitter.
 *
 * <p>T-05-05: API token presence is logged as {@code <set>} or {@code <unset>} — never the value.
 */
public class ForwarderApplication {

    private static final Logger log = LoggerFactory.getLogger(ForwarderApplication.class);

    public static void main(String[] args) throws Exception {
        ForwarderConfig cfg = ForwarderConfig.loadDefault();
        log.info("Forwarder starting — decoder={}:{}, grpc={}:{}",
                 cfg.decoderHost(), cfg.decoderPort(), cfg.grpcHost(), cfg.grpcPort());
        // T-05-05: log token presence only — never the value
        log.info("apiToken={}", cfg.apiToken() != null && !cfg.apiToken().isBlank() ? "<set>" : "<unset>");

        AmbRc4TimingSource source = new AmbRc4TimingSource(
            cfg.decoderHost(), cfg.decoderPort(),
            p -> log.info("PASSING: t={} micros={} seq={}", p.transponderNumber(), p.rtcTimeMicros(), p.seqNum()),
            state -> log.info("DECODER {}", state)
        );

        source.start();
        Runtime.getRuntime().addShutdownHook(new Thread(source::stop));
        Thread.currentThread().join(); // block forever — Plan 04 adds gRPC stream here
    }
}
