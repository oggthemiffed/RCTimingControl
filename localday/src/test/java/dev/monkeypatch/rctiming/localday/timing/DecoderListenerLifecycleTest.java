package dev.monkeypatch.rctiming.localday.timing;

import dev.monkeypatch.rctiming.decoderprotocol.timing.AmbRc4TimingSource;
import dev.monkeypatch.rctiming.decoderprotocol.timing.EpochCorrectedPassing;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.LapPassing;
import dev.monkeypatch.rctiming.localday.domain.LapPassingRepository;
import dev.monkeypatch.rctiming.localday.race.RaceState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link DecoderListenerLifecycle}. No real Netty is spun up: {@code onPassing}
 * and {@code onStatus} are exercised directly as package-private test hooks (per the plan's
 * "refactor the callback body into a small package-private method" guidance), and the
 * {@code SmartLifecycle} contract is verified by calling {@code stop(Runnable)} before
 * {@code start()} was ever invoked — {@code source} stays null, so no Netty is touched.
 */
class DecoderListenerLifecycleTest {

    private LapPassingRepository lapPassingRepository;
    private CachedScheduleEntryRepository cachedScheduleEntryRepository;
    private ApplicationEventPublisher eventPublisher;
    private LiveTimingHub liveTimingHub;
    private DecoderListenerLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        lapPassingRepository = Mockito.mock(LapPassingRepository.class);
        cachedScheduleEntryRepository = Mockito.mock(CachedScheduleEntryRepository.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        liveTimingHub = Mockito.mock(LiveTimingHub.class);
        lifecycle = new DecoderListenerLifecycle("localhost", 5100,
                lapPassingRepository, cachedScheduleEntryRepository, eventPublisher, liveTimingHub);
    }

    @Test
    void onPassing_activeRaceRunning_persistsRowWithScheduleIdAndPublishesEvent() {
        CachedScheduleEntry schedule = new CachedScheduleEntry();
        schedule.setId(77L);
        Mockito.when(cachedScheduleEntryRepository.findFirstByStatus(RaceState.RUNNING))
                .thenReturn(Optional.of(schedule));

        EpochCorrectedPassing passing = new EpochCorrectedPassing("1234567", 1_000_000L, 1, 1, 63, 1);
        lifecycle.onPassing(passing);

        ArgumentCaptor<LapPassing> rowCaptor = ArgumentCaptor.forClass(LapPassing.class);
        Mockito.verify(lapPassingRepository).save(rowCaptor.capture());
        LapPassing saved = rowCaptor.getValue();
        assertThat(saved.getCachedScheduleId()).isEqualTo(77L);
        assertThat(saved.getTransponderNumber()).isEqualTo("1234567");
        assertThat(saved.getPassingAt()).isNotNull();
        assertThat(saved.getLapTimeMs()).isNull();
        assertThat(saved.getLapNumber()).isNull();

        ArgumentCaptor<LapPassingEvent> eventCaptor = ArgumentCaptor.forClass(LapPassingEvent.class);
        Mockito.verify(eventPublisher).publishEvent(eventCaptor.capture());
        LapPassingEvent published = eventCaptor.getValue();
        assertThat(published.cachedScheduleId()).isEqualTo(77L);
        assertThat(published.transponderNumber()).isEqualTo("1234567");
        assertThat(published.rtcTimeMicros()).isEqualTo(1_000_000L);
    }

    // --- Failure / gap-handling path: no active race ---

    @Test
    void onPassing_noActiveRace_doesNotThrowAndPersistsRowWithNullScheduleId() {
        Mockito.when(cachedScheduleEntryRepository.findFirstByStatus(RaceState.RUNNING))
                .thenReturn(Optional.empty());

        EpochCorrectedPassing passing = new EpochCorrectedPassing("7654321", 2_000_000L, 1, 1, 63, 1);

        assertThatCode(() -> lifecycle.onPassing(passing)).doesNotThrowAnyException();

        ArgumentCaptor<LapPassing> rowCaptor = ArgumentCaptor.forClass(LapPassing.class);
        Mockito.verify(lapPassingRepository).save(rowCaptor.capture());
        assertThat(rowCaptor.getValue().getCachedScheduleId()).isNull();

        ArgumentCaptor<LapPassingEvent> eventCaptor = ArgumentCaptor.forClass(LapPassingEvent.class);
        Mockito.verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().cachedScheduleId()).isNull();
    }

    @Test
    void onStatus_broadcastsDecoderStatusViaLiveTimingHub() {
        lifecycle.onStatus(AmbRc4TimingSource.ConnectionState.CONNECTED);

        Mockito.verify(liveTimingHub).broadcastDecoderStatus(AmbRc4TimingSource.ConnectionState.CONNECTED);
    }

    // --- SmartLifecycle contract ---

    @Test
    void isRunning_falseBeforeStart() {
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void isAutoStartup_isTrue() {
        assertThat(lifecycle.isAutoStartup()).isTrue();
    }

    @Test
    void stopWithCallback_invokesCallback_evenWhenNeverStarted() {
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        lifecycle.stop(() -> callbackInvoked.set(true));

        assertThat(callbackInvoked).isTrue();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void plainStop_doesNotThrowWhenNeverStarted() {
        assertThatCode(() -> lifecycle.stop()).doesNotThrowAnyException();
        assertThat(lifecycle.isRunning()).isFalse();
    }
}
