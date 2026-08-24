package dev.monkeypatch.rctiming.localday.sync;

import dev.monkeypatch.rctiming.localday.daylifecycle.CloudUnreachableException;
import dev.monkeypatch.rctiming.localday.daylifecycle.DayLifecycleState;
import dev.monkeypatch.rctiming.localday.daylifecycle.DayLifecycleStateRepository;
import dev.monkeypatch.rctiming.localday.daylifecycle.DayLifecycleStatus;
import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.LapPassing;
import dev.monkeypatch.rctiming.localday.domain.LapPassingRepository;
import dev.monkeypatch.rctiming.localday.race.RaceResultEntry;
import dev.monkeypatch.rctiming.localday.race.RaceResultEntryRepository;
import dev.monkeypatch.rctiming.localday.race.RaceState;
import dev.monkeypatch.rctiming.localday.sync.dto.SnapshotRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Pure unit tests for {@link SnapshotPushService} — plain JUnit 5 + Mockito, no Spring context,
 * mirroring the style of {@code RaceStateMachineServiceTest}. {@link SnapshotSyncClient} and all
 * repositories are mocked; a {@link MutableClock} test double drives backoff timing
 * deterministically instead of sleeping real seconds.
 */
class SnapshotPushServiceTest {

    /** A {@link Clock} whose {@link #instant()} can be advanced by tests. */
    private static final class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void advance(long seconds) { now = now.plusSeconds(seconds); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { throw new UnsupportedOperationException(); }
        @Override public Instant instant() { return now; }
    }

    private SnapshotSyncClient client;
    private DayLifecycleStateRepository dayLifecycleStateRepository;
    private SnapshotQueueRepository snapshotQueueRepository;
    private LapPassingRepository lapPassingRepository;
    private CachedScheduleEntryRepository cachedScheduleEntryRepository;
    private CachedEntryRepository cachedEntryRepository;
    private RaceResultEntryRepository raceResultEntryRepository;
    private MutableClock clock;
    private SnapshotPushService service;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(SnapshotSyncClient.class);
        dayLifecycleStateRepository = Mockito.mock(DayLifecycleStateRepository.class);
        snapshotQueueRepository = Mockito.mock(SnapshotQueueRepository.class);
        lapPassingRepository = Mockito.mock(LapPassingRepository.class);
        cachedScheduleEntryRepository = Mockito.mock(CachedScheduleEntryRepository.class);
        cachedEntryRepository = Mockito.mock(CachedEntryRepository.class);
        raceResultEntryRepository = Mockito.mock(RaceResultEntryRepository.class);
        clock = new MutableClock(Instant.parse("2026-08-24T12:00:00Z"));

        service = new SnapshotPushService(client, dayLifecycleStateRepository, snapshotQueueRepository,
                lapPassingRepository, cachedScheduleEntryRepository, cachedEntryRepository,
                raceResultEntryRepository, clock, 20L, 300L);

        // Defaults shared by most tests: no schedule/results state, empty repos.
        Mockito.when(cachedScheduleEntryRepository.findAll()).thenReturn(List.of());
        Mockito.when(cachedEntryRepository.findAll()).thenReturn(List.of());
        Mockito.when(cachedScheduleEntryRepository.findFirstByStatus(any())).thenReturn(Optional.empty());
        Mockito.when(cachedScheduleEntryRepository.findFirstByStatusInOrderBySequenceAsc(any()))
                .thenReturn(Optional.empty());
        Mockito.when(cachedScheduleEntryRepository.findFirstByStatusOrderByFinishedAtDesc(any()))
                .thenReturn(Optional.empty());
        Mockito.when(raceResultEntryRepository.findAll()).thenReturn(List.of());
        Mockito.when(snapshotQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(dayLifecycleStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private DayLifecycleState openState() {
        DayLifecycleState state = new DayLifecycleState();
        state.setInstanceId("inst-1");
        state.setStatus(DayLifecycleStatus.OPEN);
        state.setCloudEventId(7L);
        state.setInstanceSecret("top-secret");
        state.setGeneration(3L);
        return state;
    }

    private LapPassing lap(long id, String transponder) {
        LapPassing l = new LapPassing();
        l.setId(id);
        l.setTransponderNumber(transponder);
        l.setPassingAt(Instant.parse("2026-08-24T11:59:00Z"));
        l.setLapTimeMs(30_000L);
        l.setLapNumber(1);
        return l;
    }

    // --- Guard conditions: no-op, never calls the client ---

    @Test
    void pushNow_noDayLifecycleState_doesNotCallClient() {
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.empty());

        service.pushNow();

        Mockito.verifyNoInteractions(client);
    }

    @Test
    void pushNow_dayNotOpen_doesNotCallClient() {
        DayLifecycleState state = openState();
        state.setStatus(DayLifecycleStatus.PRE_CACHED);
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.of(state));

        service.pushNow();

        Mockito.verifyNoInteractions(client);
    }

    @Test
    void pushNow_offlineOpen_missingInstanceSecretAndGeneration_doesNotCallClient() {
        DayLifecycleState state = openState();
        state.setInstanceSecret(null);
        state.setGeneration(null);
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.of(state));

        service.pushNow();

        Mockito.verifyNoInteractions(client);
    }

    // --- Happy path ---

    @Test
    void pushNow_happyPath_sendsIncrementalLapsAndAdvancesWatermark() {
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.of(openState()));
        Mockito.when(snapshotQueueRepository.findById(SnapshotQueue.SINGLETON_ID)).thenReturn(Optional.empty());
        Mockito.when(lapPassingRepository.findAllByIdGreaterThanOrderByIdAsc(0L))
                .thenReturn(List.of(lap(5L, "1111111"), lap(6L, "2222222")));

        service.pushNow();

        ArgumentCaptor<SnapshotRequest> requestCaptor = ArgumentCaptor.forClass(SnapshotRequest.class);
        Mockito.verify(client).pushSnapshot(eq(7L), eq("top-secret"), requestCaptor.capture());
        SnapshotRequest sent = requestCaptor.getValue();
        assertThat(sent.instanceId()).isEqualTo("inst-1");
        assertThat(sent.generation()).isEqualTo(3L);
        assertThat(sent.snapshotId()).isNotBlank();
        assertThat(sent.payload().laps()).hasSize(2);
        assertThat(sent.payload().laps().get(0).transponderNumber()).isEqualTo("1111111");

        ArgumentCaptor<SnapshotQueue> queueCaptor = ArgumentCaptor.forClass(SnapshotQueue.class);
        Mockito.verify(snapshotQueueRepository).save(queueCaptor.capture());
        assertThat(queueCaptor.getValue().getLastSyncedLapId()).isEqualTo(6L);
        assertThat(queueCaptor.getValue().getPendingSnapshotId()).isNull();

        ArgumentCaptor<DayLifecycleState> stateCaptor = ArgumentCaptor.forClass(DayLifecycleState.class);
        Mockito.verify(dayLifecycleStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getPendingSyncCount()).isZero();
    }

    @Test
    void pushNow_noNewLaps_stillPushesFullResultsAndStateOnEveryTick() {
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.of(openState()));
        Mockito.when(snapshotQueueRepository.findById(SnapshotQueue.SINGLETON_ID)).thenReturn(Optional.empty());
        Mockito.when(lapPassingRepository.findAllByIdGreaterThanOrderByIdAsc(0L)).thenReturn(List.of());

        service.pushNow();

        ArgumentCaptor<SnapshotRequest> requestCaptor = ArgumentCaptor.forClass(SnapshotRequest.class);
        Mockito.verify(client).pushSnapshot(eq(7L), eq("top-secret"), requestCaptor.capture());
        assertThat(requestCaptor.getValue().payload().laps()).isEmpty();
    }

    @Test
    void pushNow_resultsTranslatedToCloudIdsAndGroupedByRace() {
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.of(openState()));
        Mockito.when(snapshotQueueRepository.findById(SnapshotQueue.SINGLETON_ID)).thenReturn(Optional.empty());
        Mockito.when(lapPassingRepository.findAllByIdGreaterThanOrderByIdAsc(0L)).thenReturn(List.of());

        CachedScheduleEntry race = new CachedScheduleEntry();
        race.setId(100L);
        race.setCloudRaceId(900L);
        race.setClassName("Touring Stock");
        race.setFinalLetter("A");
        race.setStatus(RaceState.FINISHED);
        Mockito.when(cachedScheduleEntryRepository.findAll()).thenReturn(List.of(race));

        CachedEntry entry = new CachedEntry();
        entry.setId(200L);
        entry.setCloudEntryId(800L);
        entry.setRacerName("Jane Doe");
        entry.setTransponderNumber("1234567");
        Mockito.when(cachedEntryRepository.findAll()).thenReturn(List.of(entry));

        RaceResultEntry result = new RaceResultEntry();
        result.setRaceId(100L);
        result.setEntryId(200L);
        result.setPosition(1);
        result.setLapsCompleted(10);
        result.setBestLapMs(31_000L);
        result.setRecordedAt(Instant.parse("2026-08-24T11:00:00Z"));
        Mockito.when(raceResultEntryRepository.findAll()).thenReturn(List.of(result));

        service.pushNow();

        ArgumentCaptor<SnapshotRequest> requestCaptor = ArgumentCaptor.forClass(SnapshotRequest.class);
        Mockito.verify(client).pushSnapshot(eq(7L), eq("top-secret"), requestCaptor.capture());
        var payload = requestCaptor.getValue().payload();

        assertThat(payload.results()).hasSize(1);
        assertThat(payload.results().get(0).cloudRaceId()).isEqualTo(900L);
        assertThat(payload.results().get(0).rows()).hasSize(1);
        assertThat(payload.results().get(0).rows().get(0).cloudEntryId()).isEqualTo(800L);
        assertThat(payload.results().get(0).rows().get(0).racerName()).isEqualTo("Jane Doe");

        assertThat(payload.standings()).hasSize(1);
        assertThat(payload.standings().get(0).className()).isEqualTo("Touring Stock");
        assertThat(payload.standings().get(0).rows().get(0).bestPosition()).isEqualTo(1);
    }

    // --- Idempotent retry: same snapshotId reused for identical unacknowledged content ---

    @Test
    void pushNow_retryWithSameLapRange_reusesSnapshotId() {
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.of(openState()));
        Mockito.when(snapshotQueueRepository.findById(SnapshotQueue.SINGLETON_ID)).thenReturn(Optional.empty());
        Mockito.when(lapPassingRepository.findAllByIdGreaterThanOrderByIdAsc(0L))
                .thenReturn(List.of(lap(5L, "1111111")));
        Mockito.doThrow(new CloudUnreachableException("down", new RuntimeException()))
                .when(client).pushSnapshot(anyLong(), any(), any());

        service.pushNow();
        ArgumentCaptor<SnapshotQueue> firstSave = ArgumentCaptor.forClass(SnapshotQueue.class);
        Mockito.verify(snapshotQueueRepository).save(firstSave.capture());
        String firstSnapshotId = firstSave.getValue().getPendingSnapshotId();
        assertThat(firstSnapshotId).isNotBlank();

        // Second attempt sees the same not-yet-acked queue row (still no new laps) and clock past
        // backoff — reuses the same snapshotId rather than minting a new one.
        Mockito.when(snapshotQueueRepository.findById(SnapshotQueue.SINGLETON_ID))
                .thenReturn(Optional.of(firstSave.getValue()));
        clock.advance(60);

        service.pushNow();
        ArgumentCaptor<SnapshotRequest> secondRequest = ArgumentCaptor.forClass(SnapshotRequest.class);
        Mockito.verify(client, Mockito.times(2)).pushSnapshot(anyLong(), any(), secondRequest.capture());
        assertThat(secondRequest.getAllValues().get(1).snapshotId()).isEqualTo(firstSnapshotId);
    }

    // --- Failure path / backoff: repeated failures skip attempts until the backoff elapses ---

    @Test
    void pushNow_failure_setsPendingSyncCountAndDoesNotAdvanceWatermark() {
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.of(openState()));
        Mockito.when(snapshotQueueRepository.findById(SnapshotQueue.SINGLETON_ID)).thenReturn(Optional.empty());
        Mockito.when(lapPassingRepository.findAllByIdGreaterThanOrderByIdAsc(0L))
                .thenReturn(List.of(lap(5L, "1111111"), lap(6L, "2222222")));
        Mockito.doThrow(new CloudUnreachableException("down", new RuntimeException()))
                .when(client).pushSnapshot(anyLong(), any(), any());

        service.pushNow();

        ArgumentCaptor<SnapshotQueue> queueCaptor = ArgumentCaptor.forClass(SnapshotQueue.class);
        Mockito.verify(snapshotQueueRepository).save(queueCaptor.capture());
        assertThat(queueCaptor.getValue().getLastSyncedLapId()).isNull();

        ArgumentCaptor<DayLifecycleState> stateCaptor = ArgumentCaptor.forClass(DayLifecycleState.class);
        Mockito.verify(dayLifecycleStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getPendingSyncCount()).isEqualTo(2);
    }

    @Test
    void pushNow_repeatedFailures_backOffSkipsAttemptsUntilWindowElapses() {
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.of(openState()));
        Mockito.when(snapshotQueueRepository.findById(SnapshotQueue.SINGLETON_ID)).thenReturn(Optional.empty());
        Mockito.when(lapPassingRepository.findAllByIdGreaterThanOrderByIdAsc(0L)).thenReturn(List.of());
        Mockito.doThrow(new CloudUnreachableException("down", new RuntimeException()))
                .when(client).pushSnapshot(anyLong(), any(), any());

        service.pushNow(); // 1st attempt: fails, schedules backoff (20s * 2^0 = 20s)
        Mockito.verify(client, Mockito.times(1)).pushSnapshot(anyLong(), any(), any());

        service.pushNow(); // called again immediately — still inside the 20s backoff window
        Mockito.verify(client, Mockito.times(1)).pushSnapshot(anyLong(), any(), any());

        clock.advance(25);
        service.pushNow(); // backoff window elapsed — attempts again (and fails again)
        Mockito.verify(client, Mockito.times(2)).pushSnapshot(anyLong(), any(), any());
    }

    @Test
    void pushNow_successAfterFailure_resetsBackoff() {
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.of(openState()));
        Mockito.when(snapshotQueueRepository.findById(SnapshotQueue.SINGLETON_ID)).thenReturn(Optional.empty());
        Mockito.when(lapPassingRepository.findAllByIdGreaterThanOrderByIdAsc(0L)).thenReturn(List.of());
        Mockito.doThrow(new CloudUnreachableException("down", new RuntimeException()))
                .when(client).pushSnapshot(anyLong(), any(), any());

        service.pushNow(); // fails, backs off 20s
        Mockito.clearInvocations(client);
        Mockito.doNothing().when(client).pushSnapshot(anyLong(), any(), any());

        clock.advance(25);
        service.pushNow(); // succeeds — resets backoff to immediate
        Mockito.verify(client, Mockito.times(1)).pushSnapshot(anyLong(), any(), any());

        clock.advance(1); // no meaningful wait needed post-reset
        service.pushNow();
        Mockito.verify(client, Mockito.times(2)).pushSnapshot(anyLong(), any(), any());
    }

    // --- Immediate push on trigger event, not waiting for the next scheduled tick ---

    @Test
    void onTrigger_beforeStart_doesNotThrowAndDoesNotCallClient() {
        // The scheduler only exists after start() — a trigger arriving before then (unlikely in
        // practice, since day-open can't happen before the app is up) must not NPE.
        service.onTrigger(new SnapshotTriggerEvent("check-in"));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void onTrigger_afterStart_dispatchesAnImmediatePushAsynchronously() throws InterruptedException {
        Mockito.when(dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID))
                .thenReturn(Optional.of(openState()));
        Mockito.when(snapshotQueueRepository.findById(SnapshotQueue.SINGLETON_ID)).thenReturn(Optional.empty());
        Mockito.when(lapPassingRepository.findAllByIdGreaterThanOrderByIdAsc(0L)).thenReturn(List.of());

        CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(client).pushSnapshot(anyLong(), any(), any());

        service.start();
        try {
            service.onTrigger(new SnapshotTriggerEvent("check-in"));
            assertThat(latch.await(5, TimeUnit.SECONDS)).as("client called within 5s of the trigger").isTrue();
        } finally {
            service.stop();
        }
    }
}
