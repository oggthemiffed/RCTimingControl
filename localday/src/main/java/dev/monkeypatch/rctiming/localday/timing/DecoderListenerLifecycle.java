package dev.monkeypatch.rctiming.localday.timing;

import dev.monkeypatch.rctiming.decoderprotocol.timing.AmbRc4TimingSource;
import dev.monkeypatch.rctiming.decoderprotocol.timing.EpochCorrectedPassing;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.LapPassing;
import dev.monkeypatch.rctiming.localday.domain.LapPassingRepository;
import dev.monkeypatch.rctiming.localday.race.RaceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Wraps {@code :decoder-protocol}'s {@link AmbRc4TimingSource} as a Spring {@link SmartLifecycle}
 * bean — the local equivalent of the cloud's forwarder-side decoder wiring
 * (see {@code forwarder/.../ForwarderApplication.java}), but driven by Spring's lifecycle
 * rather than a manual shutdown hook, since this module IS the Spring process.
 *
 * <p>{@link AmbRc4TimingSource} owns its own single-threaded {@code NioEventLoopGroup}, so the
 * TCP receiver already runs on a dedicated background thread, completely isolated from the
 * Tomcat thread pool, per root CLAUDE.md's architecture rule — no additional thread wrapping is
 * needed here.
 *
 * <p><strong>Durability ordering (R10):</strong> {@link #onPassing} persists a raw
 * {@link LapPassing} row synchronously, on the Netty event-loop thread, BEFORE publishing the
 * local {@link LapPassingEvent} that drives the in-memory live-timing update. This ordering is
 * deliberate: a passing that has reached this process must survive a crash even if the
 * downstream async live-timing path never runs. A direct synchronous JPA save on the Netty
 * thread is an accepted simplification at this application's scale (a handful of passings per
 * second, not a high-frequency trading system) — not an oversight.
 *
 * <p><strong>SmartLifecycle contract (KTD7):</strong> implements the async
 * {@link #stop(Runnable)} overload rather than relying on the synchronous no-arg {@link #stop()}.
 * {@link AmbRc4TimingSource#stop()} is itself fire-and-forget — it calls
 * {@code group.shutdownGracefully(0, 100, TimeUnit.MILLISECONDS)} without awaiting the returned
 * future — so there is no real completion signal to block on. {@link #stop(Runnable)} therefore
 * calls {@code source.stop()} and invokes the callback promptly afterward; this is
 * correct-for-now behavior given what {@link AmbRc4TimingSource} actually supports today, not an
 * unfinished implementation. Because this returns near-instantly, it adds negligible time to the
 * existing {@code spring.lifecycle.timeout-per-shutdown-phase: 30s} budget in
 * {@code application.yml}, which was already sized for embedded-Postgres shutdown before this
 * unit existed — that value is intentionally left unchanged.
 *
 * <p>{@link #getPhase()} is independent of Tomcat's own graceful-shutdown bean, which has a
 * hard-coded phase of {@code Integer.MAX_VALUE} and always stops first regardless of the phase
 * value here — this is fine since the two are functionally independent (KTD7).
 */
@Component
public class DecoderListenerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(DecoderListenerLifecycle.class);

    /** Arbitrary — see class javadoc on why the exact value doesn't matter here. */
    private static final int PHASE = 100;

    private final String host;
    private final int port;
    private final LapPassingRepository lapPassingRepository;
    private final CachedScheduleEntryRepository cachedScheduleEntryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LiveTimingHub liveTimingHub;

    private volatile AmbRc4TimingSource source;
    private volatile boolean running = false;

    public DecoderListenerLifecycle(@Value("${localday.decoder.host}") String host,
                                     @Value("${localday.decoder.port}") int port,
                                     LapPassingRepository lapPassingRepository,
                                     CachedScheduleEntryRepository cachedScheduleEntryRepository,
                                     ApplicationEventPublisher eventPublisher,
                                     LiveTimingHub liveTimingHub) {
        this.host = host;
        this.port = port;
        this.lapPassingRepository = lapPassingRepository;
        this.cachedScheduleEntryRepository = cachedScheduleEntryRepository;
        this.eventPublisher = eventPublisher;
        this.liveTimingHub = liveTimingHub;
    }

    @Override
    public void start() {
        log.info("DecoderListenerLifecycle starting — decoder={}:{}", host, port);
        source = new AmbRc4TimingSource(host, port, this::onPassing, this::onStatus);
        source.start();
        running = true;
    }

    /**
     * Async stop overload — see class javadoc's "SmartLifecycle contract" section for why this
     * (not the synchronous no-arg {@link #stop()}) is the intended override point.
     */
    @Override
    public void stop(Runnable callback) {
        if (source != null) {
            source.stop();
        }
        running = false;
        callback.run();
    }

    /** Synchronous fallback for direct/manual callers — the Spring container prefers {@link #stop(Runnable)}. */
    @Override
    public void stop() {
        stop(() -> { });
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    /** Broadcasts decoder connection state changes via STOMP. Runs on the Netty event-loop thread. */
    void onStatus(AmbRc4TimingSource.ConnectionState state) {
        liveTimingHub.broadcastDecoderStatus(state);
    }

    /**
     * Handles one decoded passing. Runs on the Netty event-loop thread — kept package-private so
     * it can be exercised directly in tests without spinning up real Netty.
     *
     * <p>Step (a) durably persists the raw passing synchronously before anything else (R10).
     * Step (b) publishes the local {@link LapPassingEvent} that {@link LapTimingService} picks up
     * asynchronously for the in-memory live-timing update. This ordering must not change —
     * persistence must not be skipped or reordered behind the async/live-timing path.
     */
    void onPassing(EpochCorrectedPassing passing) {
        // (a) Durable persist first, synchronously — independent of any downstream step succeeding.
        Long scheduleId = cachedScheduleEntryRepository.findFirstByStatus(RaceState.RUNNING)
                .map(CachedScheduleEntry::getId)
                .orElse(null);

        LapPassing row = new LapPassing();
        row.setCachedScheduleId(scheduleId);
        row.setTransponderNumber(passing.transponderNumber());
        // rtcTimeMicros is already epoch-anchored absolute UTC microseconds; millisecond
        // precision is sufficient here (lapTimeMs/lapNumber are computed by LiveRaceState, not
        // this raw-capture row), so truncating to Instant.ofEpochMilli is a deliberate choice.
        row.setPassingAt(Instant.ofEpochMilli(passing.rtcTimeMicros() / 1000L));
        row.setRawDecoderLine(String.valueOf(passing));
        lapPassingRepository.save(row);

        // (b) Publish for in-memory live-timing update — does not affect durability above.
        eventPublisher.publishEvent(
                new LapPassingEvent(scheduleId, passing.transponderNumber(), passing.rtcTimeMicros()));
    }
}
