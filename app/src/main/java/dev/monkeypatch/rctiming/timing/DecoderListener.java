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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * L1: reads the AMB decoder directly over TCP, replacing the gRPC hop through the forwarder.
 *
 * <p>Connection settings come from the club profile ({@code decoder_host}, {@code decoder_port},
 * {@code decoder_protocol}). {@link AmbRc4TimingSource} owns the socket and reconnects with
 * backoff when it drops. When the settings change, the listener stops the current source and
 * starts a new one against the new address.
 *
 * <p>Each passing is published as the same {@link LapPassingEvent} the gRPC path publishes, with
 * the same active-race lookup, so {@link LapTimingService} and the practice timing service
 * need no changes. Disable with {@code app.decoder.listener.enabled=false}.
 *
 * <p>Only the RC-4 text protocol is supported. A P3 binary configuration leaves the listener
 * idle and reports the decoder as disconnected (see O5 in the local-timing plan).
 */
@Component
@ConditionalOnProperty(prefix = "app.decoder.listener", name = "enabled", matchIfMissing = true)
public class DecoderListener implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(DecoderListener.class);

    private final ClubProfileService clubProfileService;
    private final RaceRepository raceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ForwarderStatusPublisher statusPublisher;

    // Guarded by this. Settings changes and lifecycle calls both take the lock.
    private AmbRc4TimingSource source;
    private boolean running = false;

    public DecoderListener(ClubProfileService clubProfileService,
                           RaceRepository raceRepository,
                           ApplicationEventPublisher eventPublisher,
                           ForwarderStatusPublisher statusPublisher) {
        this.clubProfileService = clubProfileService;
        this.raceRepository = raceRepository;
        this.eventPublisher = eventPublisher;
        this.statusPublisher = statusPublisher;
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
            statusPublisher.onDecoderStatus(AmbRc4TimingSource.ConnectionState.DISCONNECTED.name());
            return;
        }
        if (!"RC4".equals(settings.protocol())) {
            log.warn("Decoder protocol {} is not supported by the direct listener yet (RC4 only) — listener idle",
                     settings.protocol());
            statusPublisher.onDecoderStatus(AmbRc4TimingSource.ConnectionState.DISCONNECTED.name());
            return;
        }

        log.info("Direct decoder listener starting — decoder={}:{}", settings.host(), settings.port());
        source = new AmbRc4TimingSource(settings.host(), settings.port(), this::onPassing, this::onStatus);
        source.start();
    }

    private void stopSource() {
        if (source != null) {
            // stop() reports DISCONNECTED through onStatus before the replacement reports anything.
            source.stop();
            source = null;
        }
    }

    /** Runs on the Netty event-loop thread. */
    void onStatus(AmbRc4TimingSource.ConnectionState state) {
        statusPublisher.onDecoderStatus(state.name());
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
