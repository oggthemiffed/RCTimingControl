package dev.monkeypatch.rctiming.timing;

import dev.monkeypatch.rctiming.decoderprotocol.timing.AmbRc4TimingSource;
import dev.monkeypatch.rctiming.decoderprotocol.timing.EpochCorrectedPassing;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import dev.monkeypatch.rctiming.domain.club.DecoderSettings;
import dev.monkeypatch.rctiming.domain.club.DecoderSettingsChangedEvent;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DecoderListener} that never open a socket. The source factory is
 * replaced with one that records each source's callbacks, so tests can drive them directly.
 * The socket path is covered by {@code DecoderListenerIT}.
 */
class DecoderListenerTest {

    private ClubProfileService clubProfileService;
    private RaceRepository raceRepository;
    private ApplicationEventPublisher eventPublisher;
    private DecoderStatusPublisher statusPublisher;
    private DecoderListener listener;

    /** Callbacks for each source the listener creates, in creation order. */
    private final List<Consumer<EpochCorrectedPassing>> passingCallbacks = new ArrayList<>();
    private final List<Consumer<AmbRc4TimingSource.ConnectionState>> statusCallbacks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clubProfileService = mock(ClubProfileService.class);
        raceRepository = mock(RaceRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        statusPublisher = mock(DecoderStatusPublisher.class);
        when(clubProfileService.getDecoderSettings()).thenReturn(new DecoderSettings(null, null, null));
        listener = new DecoderListener(clubProfileService, raceRepository, eventPublisher, statusPublisher,
                (host, port, onPassing, onStatus) -> {
                    passingCallbacks.add(onPassing);
                    statusCallbacks.add(onStatus);
                    return mock(AmbRc4TimingSource.class);
                });
    }

    @Test
    void onPassing_runningRace_publishesEventWithRaceId() {
        Race race = mock(Race.class);
        when(race.getId()).thenReturn(77L);
        when(raceRepository.findFirstByStatus(RaceStatus.RUNNING)).thenReturn(Optional.of(race));

        listener.onPassing(new EpochCorrectedPassing("1234567", 1_000_000L, 1, 1, 63, 1));

        ArgumentCaptor<LapPassingEvent> captor = ArgumentCaptor.forClass(LapPassingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new LapPassingEvent(77L, "1234567", 1_000_000L));
    }

    @Test
    void onPassing_noRunningRace_publishesEventWithSentinelRaceIdZero() {
        when(raceRepository.findFirstByStatus(RaceStatus.RUNNING)).thenReturn(Optional.empty());

        assertThatCode(() -> listener.onPassing(new EpochCorrectedPassing("7654321", 2_000_000L, 1, 1, 63, 1)))
                .doesNotThrowAnyException();

        ArgumentCaptor<LapPassingEvent> captor = ArgumentCaptor.forClass(LapPassingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().raceId()).isZero();
    }

    @Test
    void currentSourceStatus_isPublishedToStatusPublisher() {
        listener.start();
        configure("localhost", 5100, "RC4");

        statusCallbacks.get(0).accept(AmbRc4TimingSource.ConnectionState.RECONNECTING);

        verify(statusPublisher).onDecoderStatus("RECONNECTING");
    }

    @Test
    void currentSourcePassing_isPublishedAsLapPassingEvent() {
        when(raceRepository.findFirstByStatus(RaceStatus.RUNNING)).thenReturn(Optional.empty());
        listener.start();
        configure("localhost", 5100, "RC4");

        passingCallbacks.get(0).accept(new EpochCorrectedPassing("1234567", 3_000_000L, 1, 1, 63, 1));

        verify(eventPublisher).publishEvent(new LapPassingEvent(0L, "1234567", 3_000_000L));
    }

    @Test
    void start_unconfiguredDecoder_reportsDisconnectedAndStaysIdle() {
        when(clubProfileService.getDecoderSettings()).thenReturn(new DecoderSettings(null, null, null));

        listener.start();

        verify(statusPublisher).onDecoderStatus("DISCONNECTED");
        assertThat(listener.isRunning()).isTrue();
        assertThat(statusCallbacks).isEmpty();
    }

    @Test
    void start_p3Protocol_isNotYetSupported_reportsDisconnectedAndStaysIdle() {
        when(clubProfileService.getDecoderSettings()).thenReturn(new DecoderSettings("192.168.1.10", 5403, "P3"));

        listener.start();

        verify(statusPublisher).onDecoderStatus("DISCONNECTED");
        assertThat(statusCallbacks).isEmpty();
    }

    @Test
    void settingsChanged_whenNotRunning_doesNothing() {
        listener.onSettingsChanged(new DecoderSettingsChangedEvent(new DecoderSettings("localhost", 5100, "RC4")));

        verify(statusPublisher, never()).onDecoderStatus(any());
        assertThat(statusCallbacks).isEmpty();
    }

    @Test
    void lateStatusFromRetiredSource_afterAddressChange_isIgnored() {
        listener.start();
        configure("localhost", 5100, "RC4");
        configure("localhost", 5200, "RC4");

        // The first source (index 0) was still in flight when it was replaced by index 1.
        statusCallbacks.get(0).accept(AmbRc4TimingSource.ConnectionState.CONNECTED);
        statusCallbacks.get(0).accept(AmbRc4TimingSource.ConnectionState.RECONNECTING);
        verify(statusPublisher, never()).onDecoderStatus("CONNECTED");
        verify(statusPublisher, never()).onDecoderStatus("RECONNECTING");

        // The replacement's status is still applied.
        statusCallbacks.get(1).accept(AmbRc4TimingSource.ConnectionState.CONNECTED);
        verify(statusPublisher).onDecoderStatus("CONNECTED");
    }

    @Test
    void lateReconnectingFromRetiredSource_afterSettingsCleared_doesNotOverwriteDisconnected() {
        listener.start();
        configure("localhost", 5100, "RC4");

        configure(null, null, null);
        statusCallbacks.get(0).accept(AmbRc4TimingSource.ConnectionState.RECONNECTING);

        ArgumentCaptor<String> states = ArgumentCaptor.forClass(String.class);
        verify(statusPublisher, atLeastOnce()).onDecoderStatus(states.capture());
        assertThat(states.getAllValues()).doesNotContain("RECONNECTING");
        assertThat(states.getValue()).isEqualTo("DISCONNECTED");
    }

    @Test
    void latePassingFromRetiredSource_isNotPublished() {
        listener.start();
        configure("localhost", 5100, "RC4");
        configure("localhost", 5200, "RC4");

        passingCallbacks.get(0).accept(new EpochCorrectedPassing("1234567", 1_000_000L, 1, 1, 63, 1));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void reconcile_storedSettingsChangedWithoutEvent_reconnectsToThem() {
        listener.start();
        assertThat(statusCallbacks).isEmpty();

        // Settings written directly to the database (for example by the demo data), with no event.
        when(clubProfileService.getDecoderSettings()).thenReturn(new DecoderSettings("fake-decoder", 5100, "RC4"));
        listener.reconcile();

        assertThat(statusCallbacks).hasSize(1);
    }

    @Test
    void reconcile_settingsUnchanged_doesNotRebuildSource() {
        when(clubProfileService.getDecoderSettings()).thenReturn(new DecoderSettings("localhost", 5100, "RC4"));
        listener.start();
        assertThat(statusCallbacks).hasSize(1);

        listener.reconcile();

        assertThat(statusCallbacks).hasSize(1);
    }

    @Test
    void stopWithCallback_invokesCallback_evenWhenNeverStarted() {
        AtomicBoolean invoked = new AtomicBoolean(false);

        listener.stop(() -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(listener.isRunning()).isFalse();
    }

    /** Simulates the club profile being saved, as {@code ClubProfileService} does after commit. */
    private void configure(String host, Integer port, String protocol) {
        listener.onSettingsChanged(new DecoderSettingsChangedEvent(new DecoderSettings(host, port, protocol)));
    }
}
