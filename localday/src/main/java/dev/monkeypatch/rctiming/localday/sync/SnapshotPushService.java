package dev.monkeypatch.rctiming.localday.sync;

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
import dev.monkeypatch.rctiming.localday.sync.dto.ClassStanding;
import dev.monkeypatch.rctiming.localday.sync.dto.LapPassingSummary;
import dev.monkeypatch.rctiming.localday.sync.dto.RaceResultsSummary;
import dev.monkeypatch.rctiming.localday.sync.dto.ResultRow;
import dev.monkeypatch.rctiming.localday.sync.dto.ScheduleSummary;
import dev.monkeypatch.rctiming.localday.sync.dto.SnapshotPayload;
import dev.monkeypatch.rctiming.localday.sync.dto.SnapshotRequest;
import dev.monkeypatch.rctiming.localday.sync.dto.StandingRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Periodic snapshot push (R11, KTD4, KTD8) plus automatic resend of unsent snapshots on
 * reconnect (R12) — the local side of U11. Every tick (roughly every {@code push-interval-
 * seconds}, default 20s) composes a fresh payload from durable local state and pushes it to the
 * cloud; an immediate push is also triggered on a race-state transition or check-in via
 * {@link SnapshotTriggerEvent} rather than waiting for the next tick.
 *
 * <p>Nothing here is a system of record — laps, results, and schedule are already durably
 * persisted elsewhere (R10) before this service ever sees them. What this service durably tracks
 * ({@link SnapshotQueue}) is purely a sync cursor: how far the cloud has acknowledged, so a
 * connectivity gap of any length only delays a lap's arrival at the cloud, never loses it.
 *
 * <p>Backoff state ({@link #consecutiveFailures}, {@link #nextAttemptNotBefore}) is deliberately
 * kept in memory, not persisted — a process restart resetting backoff to "try immediately" is the
 * desirable behavior, not a bug to guard against.
 */
@Service
public class SnapshotPushService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(SnapshotPushService.class);

    private final SnapshotSyncClient client;
    private final DayLifecycleStateRepository dayLifecycleStateRepository;
    private final SnapshotQueueRepository snapshotQueueRepository;
    private final LapPassingRepository lapPassingRepository;
    private final CachedScheduleEntryRepository cachedScheduleEntryRepository;
    private final CachedEntryRepository cachedEntryRepository;
    private final RaceResultEntryRepository raceResultEntryRepository;
    private final DeviceLossHandler deviceLossHandler;
    private final Clock clock;
    private final long pushIntervalSeconds;
    private final long maxBackoffSeconds;

    private volatile ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private volatile int consecutiveFailures = 0;
    private volatile Instant nextAttemptNotBefore = Instant.MIN;

    public SnapshotPushService(SnapshotSyncClient client,
                                DayLifecycleStateRepository dayLifecycleStateRepository,
                                SnapshotQueueRepository snapshotQueueRepository,
                                LapPassingRepository lapPassingRepository,
                                CachedScheduleEntryRepository cachedScheduleEntryRepository,
                                CachedEntryRepository cachedEntryRepository,
                                RaceResultEntryRepository raceResultEntryRepository,
                                DeviceLossHandler deviceLossHandler,
                                Clock clock,
                                @Value("${localday.sync.push-interval-seconds:20}") long pushIntervalSeconds,
                                @Value("${localday.sync.max-backoff-seconds:300}") long maxBackoffSeconds) {
        this.client = client;
        this.dayLifecycleStateRepository = dayLifecycleStateRepository;
        this.snapshotQueueRepository = snapshotQueueRepository;
        this.lapPassingRepository = lapPassingRepository;
        this.cachedScheduleEntryRepository = cachedScheduleEntryRepository;
        this.cachedEntryRepository = cachedEntryRepository;
        this.raceResultEntryRepository = raceResultEntryRepository;
        this.deviceLossHandler = deviceLossHandler;
        this.clock = clock;
        this.pushIntervalSeconds = pushIntervalSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    // --- SmartLifecycle: periodic tick on a dedicated single-thread scheduler ---

    @Override
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "snapshot-push");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::tick, pushIntervalSeconds, pushIntervalSeconds, TimeUnit.SECONDS);
        running = true;
    }

    @Override
    public void stop(Runnable callback) {
        running = false;
        ScheduledExecutorService s = scheduler;
        if (s != null) {
            s.shutdown();
        }
        callback.run();
    }

    @Override
    public void stop() {
        stop(() -> { });
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * An immediate push, requested outside the regular cadence (KTD8: on a race-state transition
     * or check-in). Dispatched onto the same single-thread scheduler as the periodic tick so an
     * event- and a timer-triggered push can never run concurrently with each other — the calling
     * request thread (e.g. the "start race" HTTP request) is never blocked waiting on the cloud.
     */
    @EventListener(SnapshotTriggerEvent.class)
    public void onTrigger(SnapshotTriggerEvent event) {
        ScheduledExecutorService s = scheduler;
        if (s != null && !s.isShutdown()) {
            s.execute(this::tick);
        }
    }

    private void tick() {
        try {
            pushNow();
        } catch (RuntimeException e) {
            // Should be unreachable — pushNow() already catches the client's own exceptions.
            // This is a last-resort guard so an unexpected bug never silently kills future
            // scheduled ticks (a ScheduledExecutorService stops rescheduling a task that throws).
            log.error("Unexpected error during snapshot push", e);
        }
    }

    /**
     * Attempts one push. Safe to call directly (used by both the scheduler and the event
     * listener, and by tests) — a no-op if the day isn't open yet, if this instance hasn't
     * finished pre-cache (no instance secret/generation), or if a prior failure's backoff window
     * hasn't elapsed yet.
     *
     * <p>Deliberately not {@code @Transactional}: every real call path is self-invocation
     * ({@code this::tick} from within this same bean), which bypasses the Spring AOP proxy the
     * annotation relies on — an {@code @Transactional} here would only look atomic without
     * actually being so. It doesn't need to be: each repository call below is already atomic on
     * its own, and it's the single-thread scheduler (see {@link #start()}), not a database
     * transaction boundary, that rules out a concurrent writer racing the queue/state rows
     * within this one instance.
     */
    public void pushNow() {
        Instant now = clock.instant();
        if (now.isBefore(nextAttemptNotBefore)) {
            return;
        }

        Optional<DayLifecycleState> stateOpt = dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID);
        if (stateOpt.isEmpty()) {
            return;
        }
        DayLifecycleState state = stateOpt.get();
        if (state.getStatus() != DayLifecycleStatus.OPEN) {
            return;
        }
        if (state.isSuperseded()) {
            // A prior push was already rejected as superseded (KTD4/DeviceLossHandler) — this
            // instance's generation can never become current again, so there is nothing to gain
            // from ever attempting another push. Permanent, not backoff-timed.
            return;
        }
        Long eventId = state.getCloudEventId();
        String instanceSecret = state.getInstanceSecret();
        Long generation = state.getGeneration();
        if (eventId == null || instanceSecret == null || generation == null) {
            // Offline day-open (KTD4/DayLifecycleService.openOffline) never obtained an instance
            // secret or a real generation — there is nothing this instance can authenticate a
            // push with yet.
            return;
        }

        SnapshotQueue queue = snapshotQueueRepository.findById(SnapshotQueue.SINGLETON_ID)
                .orElseGet(SnapshotQueue::new);

        long lastSyncedLapId = queue.getLastSyncedLapId() == null ? 0L : queue.getLastSyncedLapId();
        List<LapPassing> unsyncedLaps = lapPassingRepository.findAllByIdGreaterThanOrderByIdAsc(lastSyncedLapId);
        long ceiling = unsyncedLaps.isEmpty() ? lastSyncedLapId
                : unsyncedLaps.get(unsyncedLaps.size() - 1).getId();

        String snapshotId;
        if (queue.getPendingSnapshotId() != null && Objects.equals(queue.getPendingLapIdCeiling(), ceiling)) {
            // Identical content to the last unacknowledged attempt — reuse its snapshotId so a
            // retry after a flaky connection can't double-apply on the cloud side (KTD4).
            snapshotId = queue.getPendingSnapshotId();
        } else {
            snapshotId = UUID.randomUUID().toString();
            queue.setPendingSnapshotId(snapshotId);
            queue.setPendingLapIdCeiling(ceiling);
        }

        SnapshotPayload payload = buildPayload(now, unsyncedLaps);
        SnapshotRequest request = new SnapshotRequest(state.getInstanceId(), generation, snapshotId, payload);

        queue.setLastPushAttemptAt(now);
        try {
            client.pushSnapshot(eventId, instanceSecret, request);

            queue.setLastSyncedLapId(ceiling);
            queue.setPendingSnapshotId(null);
            queue.setPendingLapIdCeiling(null);
            queue.setLastPushSuccessAt(now);
            consecutiveFailures = 0;
            nextAttemptNotBefore = Instant.MIN;
            state.setPendingSyncCount(0);
        } catch (RuntimeException e) {
            if (deviceLossHandler.isSuperseded(e)) {
                // Permanent, not a transient failure — no backoff bookkeeping; pushNow()'s guard
                // above stops every future attempt once state.isSuperseded() is true.
                deviceLossHandler.markSuperseded(state, now);
            } else {
                consecutiveFailures++;
                long backoffMultiplier = 1L << Math.min(consecutiveFailures - 1, 20);
                long backoffSeconds = Math.min(pushIntervalSeconds * backoffMultiplier, maxBackoffSeconds);
                nextAttemptNotBefore = now.plusSeconds(backoffSeconds);
                log.warn("Snapshot push failed (attempt {}), backing off {}s: {}",
                        consecutiveFailures, backoffSeconds, e.getMessage());
            }
            state.setPendingSyncCount(unsyncedLaps.size());
        }

        queue.setUpdatedAt(now);
        snapshotQueueRepository.save(queue);
        dayLifecycleStateRepository.save(state);
    }

    private SnapshotPayload buildPayload(Instant capturedAt, List<LapPassing> unsyncedLaps) {
        // Per KTD8, results/standings are resent in full every push, so allResults naturally
        // grows across the day — that's the intended design, not something to optimize away
        // here. What must NOT scan the whole table every 20s tick is the *lookup* support for
        // it: only fetch the specific schedule/entry rows this tick's results and laps actually
        // reference, by id, rather than every cached_schedule/cached_entries row that exists.
        List<RaceResultEntry> allResults = raceResultEntryRepository.findAll();

        Set<Long> scheduleIds = new LinkedHashSet<>();
        allResults.forEach(r -> scheduleIds.add(r.getRaceId()));
        unsyncedLaps.forEach(l -> {
            if (l.getCachedScheduleId() != null) {
                scheduleIds.add(l.getCachedScheduleId());
            }
        });
        Map<Long, CachedScheduleEntry> scheduleById = cachedScheduleEntryRepository.findAllById(scheduleIds).stream()
                .collect(Collectors.toMap(CachedScheduleEntry::getId, e -> e));

        Set<Long> entryIds = allResults.stream().map(RaceResultEntry::getEntryId).collect(Collectors.toSet());
        Map<Long, CachedEntry> entriesById = cachedEntryRepository.findAllById(entryIds).stream()
                .collect(Collectors.toMap(CachedEntry::getId, e -> e));

        ScheduleSummary current = cachedScheduleEntryRepository.findFirstByStatus(RaceState.RUNNING)
                .or(() -> cachedScheduleEntryRepository.findFirstByStatus(RaceState.STOPPED))
                .map(SnapshotPushService::toSummary).orElse(null);
        ScheduleSummary next = cachedScheduleEntryRepository
                .findFirstByStatusInOrderBySequenceAsc(List.of(RaceState.PENDING, RaceState.GRID))
                .map(SnapshotPushService::toSummary).orElse(null);
        ScheduleSummary lastCompleted = cachedScheduleEntryRepository
                .findFirstByStatusOrderByFinishedAtDesc(RaceState.FINISHED)
                .map(SnapshotPushService::toSummary).orElse(null);

        List<RaceResultsSummary> results = buildResults(allResults, scheduleById, entriesById);
        List<ClassStanding> standings = buildStandings(allResults, scheduleById, entriesById);
        List<LapPassingSummary> laps = buildLaps(unsyncedLaps, scheduleById);

        return new SnapshotPayload(capturedAt, current, next, lastCompleted, results, standings, laps);
    }

    private static ScheduleSummary toSummary(CachedScheduleEntry e) {
        return new ScheduleSummary(e.getCloudRaceId(), e.getRoundNumber(), e.getHeatNumber(),
                e.getClassName(), e.getFinalLetter(), e.getStatus().name());
    }

    private List<RaceResultsSummary> buildResults(List<RaceResultEntry> allResults,
                                                    Map<Long, CachedScheduleEntry> scheduleById,
                                                    Map<Long, CachedEntry> entriesById) {
        Map<Long, List<RaceResultEntry>> byRace = allResults.stream()
                .collect(Collectors.groupingBy(RaceResultEntry::getRaceId, LinkedHashMap::new, Collectors.toList()));

        List<RaceResultsSummary> out = new ArrayList<>();
        for (Map.Entry<Long, List<RaceResultEntry>> raceEntry : byRace.entrySet()) {
            CachedScheduleEntry schedule = scheduleById.get(raceEntry.getKey());
            if (schedule == null || schedule.getCloudRaceId() == null) {
                continue; // no cloud-side race to attach this result to
            }
            List<ResultRow> rows = raceEntry.getValue().stream()
                    .sorted(Comparator.comparingInt(RaceResultEntry::getPosition))
                    .map(r -> toResultRow(r, entriesById.get(r.getEntryId())))
                    .toList();
            out.add(new RaceResultsSummary(schedule.getCloudRaceId(), schedule.getClassName(),
                    schedule.getFinalLetter(), rows));
        }
        return out;
    }

    private static ResultRow toResultRow(RaceResultEntry r, CachedEntry entry) {
        return new ResultRow(
                entry == null ? null : entry.getCloudEntryId(),
                entry == null ? null : entry.getRacerName(),
                entry == null ? null : entry.getTransponderNumber(),
                r.getPosition(), r.getLapsCompleted(), r.getBestLapMs());
    }

    /**
     * See {@link SnapshotPayload}'s javadoc: a simple best-position-per-entry-per-class summary,
     * not a points-scoring engine — the cloud recomputes authoritative standings itself (R11).
     */
    private List<ClassStanding> buildStandings(List<RaceResultEntry> allResults,
                                                 Map<Long, CachedScheduleEntry> scheduleById,
                                                 Map<Long, CachedEntry> entriesById) {
        Map<String, Map<Long, List<Integer>>> positionsByClassAndEntry = new LinkedHashMap<>();
        for (RaceResultEntry r : allResults) {
            CachedScheduleEntry schedule = scheduleById.get(r.getRaceId());
            if (schedule == null || schedule.getClassName() == null) {
                continue;
            }
            positionsByClassAndEntry
                    .computeIfAbsent(schedule.getClassName(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(r.getEntryId(), k -> new ArrayList<>())
                    .add(r.getPosition());
        }

        List<ClassStanding> out = new ArrayList<>();
        for (Map.Entry<String, Map<Long, List<Integer>>> classEntry : positionsByClassAndEntry.entrySet()) {
            List<StandingRow> rows = classEntry.getValue().entrySet().stream()
                    .map(e -> toStandingRow(e.getKey(), e.getValue(), entriesById.get(e.getKey())))
                    .sorted(Comparator.comparingInt(StandingRow::bestPosition))
                    .toList();
            out.add(new ClassStanding(classEntry.getKey(), rows));
        }
        return out;
    }

    private static StandingRow toStandingRow(Long entryId, List<Integer> positions, CachedEntry entry) {
        int best = positions.stream().min(Integer::compareTo).orElse(Integer.MAX_VALUE);
        return new StandingRow(
                entry == null ? null : entry.getCloudEntryId(),
                entry == null ? null : entry.getRacerName(),
                best, positions.size());
    }

    private static List<LapPassingSummary> buildLaps(List<LapPassing> unsyncedLaps,
                                                        Map<Long, CachedScheduleEntry> scheduleById) {
        return unsyncedLaps.stream()
                .map(l -> new LapPassingSummary(
                        l.getTransponderNumber(),
                        resolveCloudRaceId(l.getCachedScheduleId(), scheduleById),
                        l.getPassingAt(), l.getLapTimeMs(), l.getLapNumber()))
                .toList();
    }

    private static Long resolveCloudRaceId(Long cachedScheduleId, Map<Long, CachedScheduleEntry> scheduleById) {
        if (cachedScheduleId == null) {
            return null;
        }
        CachedScheduleEntry schedule = scheduleById.get(cachedScheduleId);
        return schedule == null ? null : schedule.getCloudRaceId();
    }
}
