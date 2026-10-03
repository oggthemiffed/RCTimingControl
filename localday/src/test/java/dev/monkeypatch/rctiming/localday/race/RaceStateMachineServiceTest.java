package dev.monkeypatch.rctiming.localday.race;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.sync.SnapshotTriggerEvent;
import dev.monkeypatch.rctiming.localday.timing.LapTimingService;
import dev.monkeypatch.rctiming.localday.timing.LiveTimingHub;
import dev.monkeypatch.rctiming.localday.timing.dto.MarshalAdjustmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for {@link RaceStateMachineService}'s transition logic and marshal
 * adjustment recording. Mirrors the style of app's RaceStateMachineServiceTest — plain JUnit 5
 * + AssertJ, no Spring context. {@link MarshalAdjustmentRepository}, {@link LiveTimingHub}, and
 * {@link LapTimingService} are all mocked; transition()/recordAdjustment() tests verify
 * interaction with the mocks directly.
 */
class RaceStateMachineServiceTest {

    private MarshalAdjustmentRepository marshalAdjustmentRepository;
    private LiveTimingHub liveTimingHub;
    private LapTimingService lapTimingService;
    private ApplicationEventPublisher eventPublisher;
    private RaceStateMachineService service;

    @BeforeEach
    void setUp() {
        marshalAdjustmentRepository = Mockito.mock(MarshalAdjustmentRepository.class);
        liveTimingHub = Mockito.mock(LiveTimingHub.class);
        lapTimingService = Mockito.mock(LapTimingService.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        service = new RaceStateMachineService(marshalAdjustmentRepository, liveTimingHub, lapTimingService, eventPublisher);
    }

    private CachedScheduleEntry raceWithStatus(RaceState status) {
        CachedScheduleEntry race = new CachedScheduleEntry();
        race.setId(1L);
        race.setStatus(status);
        return race;
    }

    // --- Happy path: every valid transition ---

    @Test
    void validTransition_pendingToGrid_updatesStatus() {
        CachedScheduleEntry race = raceWithStatus(RaceState.PENDING);
        service.transition(race, RaceState.GRID);
        assertThat(race.getStatus()).isEqualTo(RaceState.GRID);
    }

    @Test
    void validTransition_gridToRunning_updatesStatus() {
        CachedScheduleEntry race = raceWithStatus(RaceState.GRID);
        service.transition(race, RaceState.RUNNING);
        assertThat(race.getStatus()).isEqualTo(RaceState.RUNNING);
    }

    @Test
    void validTransition_gridToPending_updatesStatus() {
        CachedScheduleEntry race = raceWithStatus(RaceState.GRID);
        service.transition(race, RaceState.PENDING);
        assertThat(race.getStatus()).isEqualTo(RaceState.PENDING);
    }

    @Test
    void validTransition_runningToStopped_updatesStatus() {
        CachedScheduleEntry race = raceWithStatus(RaceState.RUNNING);
        service.transition(race, RaceState.STOPPED);
        assertThat(race.getStatus()).isEqualTo(RaceState.STOPPED);
    }

    @Test
    void validTransition_runningToFinished_updatesStatus() {
        CachedScheduleEntry race = raceWithStatus(RaceState.RUNNING);
        service.transition(race, RaceState.FINISHED);
        assertThat(race.getStatus()).isEqualTo(RaceState.FINISHED);
    }

    @Test
    void validTransition_stoppedToRunning_updatesStatus() {
        CachedScheduleEntry race = raceWithStatus(RaceState.STOPPED);
        service.transition(race, RaceState.RUNNING);
        assertThat(race.getStatus()).isEqualTo(RaceState.RUNNING);
    }

    @Test
    void validTransition_stoppedToFinished_updatesStatus() {
        CachedScheduleEntry race = raceWithStatus(RaceState.STOPPED);
        service.transition(race, RaceState.FINISHED);
        assertThat(race.getStatus()).isEqualTo(RaceState.FINISHED);
    }

    // --- Live-timing wiring: broadcasts and state release ---

    @Test
    void validTransition_broadcastsStateChangeViaLiveTimingHub() {
        CachedScheduleEntry race = raceWithStatus(RaceState.PENDING);
        race.setId(5L);

        service.transition(race, RaceState.GRID);

        Mockito.verify(liveTimingHub).broadcastStateChange(5L, RaceState.GRID);
    }

    @Test
    void transitionToFinished_releasesLapTimingState() {
        CachedScheduleEntry race = raceWithStatus(RaceState.RUNNING);
        race.setId(6L);

        service.transition(race, RaceState.FINISHED);

        Mockito.verify(lapTimingService).releaseState(6L);
    }

    @Test
    void transitionToNonFinishedState_doesNotReleaseLapTimingState() {
        CachedScheduleEntry race = raceWithStatus(RaceState.PENDING);
        race.setId(7L);

        service.transition(race, RaceState.GRID);

        Mockito.verify(lapTimingService, Mockito.never()).releaseState(Mockito.anyLong());
    }

    @Test
    void invalidTransition_doesNotBroadcastOrTouchLapTimingService() {
        CachedScheduleEntry race = raceWithStatus(RaceState.PENDING);
        race.setId(8L);

        assertThatThrownBy(() -> service.transition(race, RaceState.FINISHED))
                .isInstanceOf(IllegalStateTransitionException.class);

        Mockito.verifyNoInteractions(liveTimingHub);
        Mockito.verifyNoInteractions(lapTimingService);
        Mockito.verifyNoInteractions(eventPublisher);
    }

    @Test
    void validTransition_publishesSnapshotTriggerEvent() {
        CachedScheduleEntry race = raceWithStatus(RaceState.PENDING);

        service.transition(race, RaceState.GRID);

        ArgumentCaptor<SnapshotTriggerEvent> captor = ArgumentCaptor.forClass(SnapshotTriggerEvent.class);
        Mockito.verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().reason()).isEqualTo("race-state-transition");
    }

    // --- Error path: invalid transitions ---

    @Test
    void invalidTransition_pendingToFinished_throwsIllegalStateTransitionException() {
        CachedScheduleEntry race = raceWithStatus(RaceState.PENDING);
        assertThatThrownBy(() -> service.transition(race, RaceState.FINISHED))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("1")
                .hasMessageContaining("PENDING")
                .hasMessageContaining("FINISHED");
        assertThat(race.getStatus()).isEqualTo(RaceState.PENDING);
    }

    @Test
    void invalidTransition_pendingToRunning_skippingGrid_throwsIllegalStateTransitionException() {
        CachedScheduleEntry race = raceWithStatus(RaceState.PENDING);
        assertThatThrownBy(() -> service.transition(race, RaceState.RUNNING))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThat(race.getStatus()).isEqualTo(RaceState.PENDING);
    }

    @ParameterizedTest
    @EnumSource(RaceState.class)
    void invalidTransition_finishedToAny_throwsIllegalStateTransitionException(RaceState target) {
        CachedScheduleEntry race = raceWithStatus(RaceState.FINISHED);
        assertThatThrownBy(() -> service.transition(race, target))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThat(race.getStatus()).isEqualTo(RaceState.FINISHED);
    }

    @Test
    void invalidTransition_gridToFinished_throwsIllegalStateTransitionException() {
        CachedScheduleEntry race = raceWithStatus(RaceState.GRID);
        assertThatThrownBy(() -> service.transition(race, RaceState.FINISHED))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void invalidTransition_stoppedToPending_throwsIllegalStateTransitionException() {
        CachedScheduleEntry race = raceWithStatus(RaceState.STOPPED);
        assertThatThrownBy(() -> service.transition(race, RaceState.PENDING))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    // --- Marshal adjustments: non-state-transition audit records ---

    @Test
    void recordAdjustment_doesNotAlterRaceStatus() {
        CachedScheduleEntry race = raceWithStatus(RaceState.RUNNING);
        CachedEntry entry = new CachedEntry();
        entry.setId(42L);
        entry.setTransponderNumber("1234567");

        Mockito.when(marshalAdjustmentRepository.save(Mockito.any(MarshalAdjustment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordAdjustment(race, entry, 1, 7L, "Referee Bob");

        assertThat(race.getStatus()).isEqualTo(RaceState.RUNNING);
    }

    @Test
    void recordAdjustment_snapshotsRaceStateAtCallTime() {
        CachedScheduleEntry race = raceWithStatus(RaceState.STOPPED);
        race.setId(9L);
        CachedEntry entry = new CachedEntry();
        entry.setId(42L);
        entry.setTransponderNumber("1234567");

        Mockito.when(marshalAdjustmentRepository.save(Mockito.any(MarshalAdjustment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MarshalAdjustment adjustment = service.recordAdjustment(race, entry, -1, 7L, "Referee Bob");

        assertThat(adjustment.getRaceId()).isEqualTo(9L);
        assertThat(adjustment.getEntryId()).isEqualTo(42L);
        assertThat(adjustment.getTransponderNumber()).isEqualTo("1234567");
        assertThat(adjustment.getLapDelta()).isEqualTo(-1);
        assertThat(adjustment.getRaceStateAtTime()).isEqualTo("STOPPED");
        assertThat(adjustment.getActingUserId()).isEqualTo(7L);
        assertThat(adjustment.getActingUserName()).isEqualTo("Referee Bob");
        assertThat(adjustment.getAdjustedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());

        // Race transitions after the adjustment was recorded must not retroactively change the
        // already-persisted snapshot.
        service.transition(race, RaceState.RUNNING);
        assertThat(adjustment.getRaceStateAtTime()).isEqualTo("STOPPED");
    }

    @Test
    void recordAdjustment_appliesMarshalAdjustmentToLapTimingServiceWithoutAlteringStatus() {
        CachedScheduleEntry race = raceWithStatus(RaceState.RUNNING);
        race.setId(11L);
        CachedEntry entry = new CachedEntry();
        entry.setId(42L);
        entry.setTransponderNumber("1234567");

        Mockito.when(marshalAdjustmentRepository.save(Mockito.any(MarshalAdjustment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordAdjustment(race, entry, 1, 7L, "Referee Bob");

        ArgumentCaptor<MarshalAdjustmentDto> dtoCaptor = ArgumentCaptor.forClass(MarshalAdjustmentDto.class);
        Mockito.verify(lapTimingService).applyMarshalAdjustment(
                Mockito.eq(11L), Mockito.eq(42L), Mockito.eq(1), dtoCaptor.capture());

        MarshalAdjustmentDto dto = dtoCaptor.getValue();
        assertThat(dto.scheduleId()).isEqualTo(11L);
        assertThat(dto.entryId()).isEqualTo(42L);
        assertThat(dto.transponderNumber()).isEqualTo("1234567");
        assertThat(dto.lapDelta()).isEqualTo(1);
        assertThat(dto.actingUserName()).isEqualTo("Referee Bob");

        // Re-verify the existing "status untouched" assertion still holds after this change.
        assertThat(race.getStatus()).isEqualTo(RaceState.RUNNING);
    }
}
