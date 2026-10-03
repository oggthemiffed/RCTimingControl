package dev.monkeypatch.rctiming.timing;

import dev.monkeypatch.rctiming.decoderprotocol.timing.AmbRc4TimingSource;
import dev.monkeypatch.rctiming.decoderprotocol.timing.EpochCorrectedPassing;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import dev.monkeypatch.rctiming.domain.club.DecoderSettings;
import dev.monkeypatch.rctiming.domain.club.DecoderSettingsChangedEvent;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.forwarder.ForwarderStatusPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.function.Consumer;

/**
 * L1: reads the AMB decoder directly over TCP, replacing the gRPC hop through the forwarder.
 *
 * <p>Connection settings come from the club profile ({@code decoder_host}, {@code decoder_port},
 * {@code decoder_protocol}). {@link AmbRc4TimingSource} owns the socket and reconnects with
 * backoff when it drops. When the settings change, the listener retires the current source and
 * starts a new one against the new address.
 *
 * <p>Each passing is published as the same {@link LapPassingEvent} the gRPC path publishes, with
 * the same active-race lookup, so {@link LapTimingService} and the practice timing service
 * need no changes. Disable with {@code app.decoder.listener.enabled=false}.
 *
 * <p>Only the RC-4 text protocol is supported. A P3 binary configuration leaves the listener
 * idle and reports the decoder as disconnected (see O5 in the local-timing plan).
 *
 * <p><strong>Retired sources:</strong> {@link AmbRc4TimingSource#stop()} is asynchronous, so a
 * connection attempt in flight can still call back after the source has been replaced. Each
 * source is tagged with a generation number. Retiring a source bumps the generation first, so
 * late passings and status changes from it are ignored.
 */
@Component
@ConditionalOnProperty(prefix = "app.decoder.listener", name = "enabled", matchIfMissing = true)
public class DecoderListener implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(DecoderListener.class);

    /** Creates the TCP source. Replaceable in tests so callbacks can be driven directly. */
    @FunctionalInterface
    interface SourceFactory {
        AmbRc4TimingSource create(String host, int port,
                                  Consumer<EpochCorrectedPassing> onPassing,
                                  Consumer<AmbRc4TimingSource.ConnectionState> onStatus);
    }

    private final ClubProfileService clubProfileService;
    private final RaceRepository raceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ForwarderStatusPublisher statusPublisher;
    private final SourceFactory sourceFactory;

    // Guarded by this. Settings changes and lifecycle calls both take the lock.
    private AmbRc4TimingSource source;
    private boolean running = false;
    // Written only while holding this lock. Read without it by the Netty callbacks.
    private volatile int generation = 0;

    @Autowired
    public DecoderListener(ClubProfileService clubProfileService,
                           RaceRepository raceRepository,
                           ApplicationEventPublisher eventPublisher,
                           ForwarderStatusPublisher statusPublisher) {
        this(clubProfileService, raceRepository, eventPublisher, statusPublisher, AmbRc4TimingSource::new);
    }

    DecoderListener(ClubProfileService clubProfileService,
                    RaceRepository raceRepository,
                    ApplicationEventPublisher eventPublisher,
                    ForwarderStatusPublisher statusPublisher,
                    SourceFactory sourceFactory) {
        this.clubProfileService = clubProfileService;
        this.raceRepository = raceRepository;
        this.eventPublisher = eventPublisher;
        this.statusPublisher = statusPublisher;
        this.sourceFactory = sourceFactory;
    }

    @Override
    public synchronized void start() {
        running = true;
        applySettings(clubProfileService.getDecoderSettings());
    }

    /** Async stop overload: {@link AmbRc4TimingSource#stop()} is fire-and-forget, so the callback runs straight after. */
    @Override
    public synchronized void stop(Runnable callback) {
        running = false;
        stopSource();
        callback.run();
    }

    /** Synchronous fallback. Spring prefers {@link #stop(Runnable)}. */
    @Override
    public void stop() {
        stop(() -> { });
    }

    @Override
    public synchronized boolean isRunning() {
        return running;
    }

    /**
     * Applies new decoder settings after the club profile update commits. Runs after commit so
     * the listener never reconnects to an address that was rolled back.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public synchronized void onSettingsChanged(DecoderSettingsChangedEvent event) {
        if (!running) {
            return;
        }
        log.info("Decoder settings changed — restarting direct decoder listener");
        applySettings(event.settings());
    }

    private void applySettings(DecoderSettings settings) {
        stopSource();

        if (!settings.isConfigured()) {
            log.info("Decoder not configured — direct decoder listener idle");
            statusPublisher.onDirectDecoderStatus(AmbRc4TimingSource.ConnectionState.DISCONNECTED.name());
            return;
        }
        if (!"RC4".equals(settings.protocol())) {
            log.warn("Decoder protocol {} is not supported by the direct listener yet (RC4 only) — listener idle",
                     settings.protocol());
            statusPublisher.onDirectDecoderStatus(AmbRc4TimingSource.ConnectionState.DISCONNECTED.name());
            return;
        }

        log.info("Direct decoder listener starting — decoder={}:{}", settings.host(), settings.port());
        int current = generation;
        source = sourceFactory.create(settings.host(), settings.port(),
                passing -> {
                    if (current == generation) {
                        onPassing(passing);
                    }
                },
                state -> onStatus(current, state));
        source.start();
    }

    /** Retires the current source. Must be called while holding the lock. */
    private void stopSource() {
        // Bump the generation before stopping, so the retiring source's own DISCONNECTED
        // callback (and anything still in flight from it) is ignored.
        generation++;
        if (source != null) {
            source.stop();
            source = null;
        }
    }

    /** Runs on the Netty event-loop thread. Ignored unless it comes from the current source. */
    void onStatus(int sourceGeneration, AmbRc4TimingSource.ConnectionState state) {
        synchronized (this) {
            if (sourceGeneration != generation) {
                return;
            }
            statusPublisher.onDirectDecoderStatus(state.name());
        }
    }

    /**
     * Publishes one decoded passing. Runs on the Netty event-loop thread. Package-private so it
     * can be exercised without a socket.
     */
    void onPassing(EpochCorrectedPassing passing) {
        long raceId = raceRepository.findFirstByStatus(RaceStatus.RUNNING)
                .map(Race::getId)
                .orElse(0L);
        eventPublisher.publishEvent(
                new LapPassingEvent(raceId, passing.transponderNumber(), passing.rtcTimeMicros()));
    }
}
