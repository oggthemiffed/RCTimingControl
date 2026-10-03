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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DecoderListener} that never open a socket. The socket path is covered by
 * {@code DecoderListenerIT}.
 */
class DecoderListenerTest {

    private ClubProfileService clubProfileService;
    private RaceRepository raceRepository;
    private ApplicationEventPublisher eventPublisher;
    private ForwarderStatusPublisher statusPublisher;
    private DecoderListener listener;

    @BeforeEach
    void setUp() {
        clubProfileService = mock(ClubProfileService.class);
        raceRepository = mock(RaceRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        statusPublisher = mock(ForwarderStatusPublisher.class);
        listener = new DecoderListener(clubProfileService, raceRepository, eventPublisher, statusPublisher);
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
    void onStatus_publishesDecoderStateToStatusPublisher() {
        listener.onStatus(AmbRc4TimingSource.ConnectionState.RECONNECTING);

        verify(statusPublisher).onDecoderStatus("RECONNECTING");
    }

    @Test
    void start_unconfiguredDecoder_reportsDisconnectedAndStaysIdle() {
        when(clubProfileService.getDecoderSettings()).thenReturn(new DecoderSettings(null, null, null));

        listener.start();

        verify(statusPublisher).onDecoderStatus("DISCONNECTED");
        assertThat(listener.isRunning()).isTrue();
    }

    @Test
    void start_p3Protocol_isNotYetSupported_reportsDisconnectedAndStaysIdle() {
        when(clubProfileService.getDecoderSettings()).thenReturn(new DecoderSettings("192.168.1.10", 5403, "P3"));

        listener.start();

        verify(statusPublisher).onDecoderStatus("DISCONNECTED");
    }

    @Test
    void settingsChanged_whenNotRunning_doesNothing() {
        listener.onSettingsChanged(new DecoderSettingsChangedEvent(new DecoderSettings("localhost", 5100, "RC4")));

        verify(statusPublisher, never()).onDecoderStatus(any());
    }

    @Test
    void stopWithCallback_invokesCallback_evenWhenNeverStarted() {
        AtomicBoolean invoked = new AtomicBoolean(false);

        listener.stop(() -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(listener.isRunning()).isFalse();
    }
}
