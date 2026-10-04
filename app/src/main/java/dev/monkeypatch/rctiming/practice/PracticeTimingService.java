package dev.monkeypatch.rctiming.practice;

import dev.monkeypatch.rctiming.domain.practice.PracticeLap;
import dev.monkeypatch.rctiming.domain.practice.PracticeLapRepository;
import dev.monkeypatch.rctiming.domain.practice.PracticeSession;
import dev.monkeypatch.rctiming.domain.practice.PracticeSessionRepository;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorRepository;
import dev.monkeypatch.rctiming.domain.competitor.Competitor;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.practice.dto.PracticeTimingRowDto;
import dev.monkeypatch.rctiming.timing.LapPassingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Practice session lap processing service.
 *
 * Listens to the same LapPassingEvent as LapTimingService, but only acts
 * when a practice session is RUNNING. Does NOT conflict with LapTimingService
 * because practice sessions are created outside of race context (raceId = 0
 * in LapTimingService has no RUNNING race, so no positions are broadcast for that).
 *
 * Lap time computed from rtcTimeMicros delta (same technique as LiveRaceState).
 * CrossingTime uses Instant.now() (server receipt time) since rtcTimeMicros is
 * a raw hardware counter, not epoch-based.
 */
@Service
public class PracticeTimingService {

    private static final Logger log = LoggerFactory.getLogger(PracticeTimingService.class);

    private final PracticeSessionRepository sessionRepository;
    private final PracticeLapRepository lapRepository;
    private final EntryRepository entryRepository;
    private final CompetitorRepository competitorRepository;
    private final PracticeTimingHub timingHub;

    /** Active practice session states keyed by sessionId. */
    private final Map<Long, LivePracticeState> activeStates = new ConcurrentHashMap<>();

    /**
     * Per-transponder last RTC time (micros) tracked per active session.
     * Used to compute lap duration from consecutive passings.
     * Key: sessionId + "|" + transponderNumber
     */
    private final Map<String, Long> lastRtcMicros = new ConcurrentHashMap<>();

    public PracticeTimingService(PracticeSessionRepository sessionRepository,
                                 PracticeLapRepository lapRepository,
                                 EntryRepository entryRepository,
                                 CompetitorRepository competitorRepository,
                                 PracticeTimingHub timingHub) {
        this.sessionRepository = sessionRepository;
        this.lapRepository = lapRepository;
        this.entryRepository = entryRepository;
        this.competitorRepository = competitorRepository;
        this.timingHub = timingHub;
    }

    // ---------------------------------------------------------------------------
    // Session lifecycle
    // ---------------------------------------------------------------------------

    /**
     * Begin tracking a newly started practice session.
     * Called from PracticeSessionService.start() after the session is persisted.
     */
    public void startSession(PracticeSession session) {
        LivePracticeState state = new LivePracticeState(session.getId(), session.getBestLapN());
        activeStates.put(session.getId(), state);
        log.info("Practice session {} ({}) started timing", session.getId(), session.getName());
    }

    /**
     * Stop tracking a practice session.
     * Called from PracticeSessionService.stop() after the session is persisted.
     */
    public void stopSession(Long sessionId) {
        activeStates.remove(sessionId);
        // Remove RTC tracking entries for this session
        lastRtcMicros.keySet().removeIf(k -> k.startsWith(sessionId + "|"));
        log.info("Practice session {} stopped timing", sessionId);
    }

    // ---------------------------------------------------------------------------
    // Lap event processing
    // ---------------------------------------------------------------------------

    /**
     * Handle LapPassingEvent. Processes only when a practice session is RUNNING.
     * Fires alongside LapTimingService — both can coexist without interference.
     */
    @EventListener
    @Transactional
    public void onLapPassing(LapPassingEvent event) {
        // Check for a running practice session
        PracticeSession session = sessionRepository.findRunningSession().orElse(null);
        if (session == null) {
            return;
        }

        LivePracticeState state = activeStates.get(session.getId());
        if (state == null) {
            // Race condition: session found as RUNNING in DB but not yet tracked locally
            // (e.g. server restart). Re-initialise gracefully.
            startSession(session);
            state = activeStates.get(session.getId());
        }

        String transponderNumber = event.transponderNumber();
        Instant crossingTime = Instant.now();

        // Compute lap time from RTC delta
        String rtcKey = session.getId() + "|" + transponderNumber;
        Long prevRtcMicros = lastRtcMicros.put(rtcKey, event.rtcTimeMicros());
        Long lapTimeMs = null;
        if (prevRtcMicros != null) {
            long deltaMs = (event.rtcTimeMicros() - prevRtcMicros) / 1000L;
            if (deltaMs > 0) {
                lapTimeMs = deltaMs;
            }
        }

        // Resolve transponder → competitor through the session's event entries (L10, #18).
        // A session with no event, or a transponder no entry uses, stays unknown.
        String racerName = resolveCompetitorName(session, transponderNumber);

        // Record in in-memory state
        state.recordLap(transponderNumber, null, racerName, lapTimeMs, crossingTime);

        // Persist lap record (only when we have a real lap time)
        if (lapTimeMs != null) {
            int lapNumber = lapRepository
                    .findByPracticeSessionIdAndTransponderNumberOrderByLapNumberAsc(
                            session.getId(), transponderNumber)
                    .size() + 1;

            PracticeLap lap = new PracticeLap();
            lap.setPracticeSession(session);
            lap.setTransponderNumber(transponderNumber);
            lap.setLapNumber(lapNumber);
            lap.setLapTimeMs(lapTimeMs);
            lap.setCrossingTime(crossingTime);
            lapRepository.save(lap);
        }

        // Broadcast positions
        List<PracticeTimingRowDto> rows = state.calculatePositions();
        timingHub.broadcastTimingUpdate(session.getId(), rows);

        // Broadcast unknown transponders if any
        Set<String> unknown = state.getUnknownTransponders();
        if (!unknown.isEmpty()) {
            timingHub.broadcastUnknownTransponders(session.getId(), unknown);
        }
    }

    // ---------------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------------

    /**
     * Get current timing snapshot.
     * Returns in-memory state if session is active, else rebuilds from DB (for stopped sessions).
     */
    public List<PracticeTimingRowDto> getSnapshot(Long sessionId) {
        LivePracticeState state = activeStates.get(sessionId);
        if (state != null) {
            return state.calculatePositions();
        }
        // Session not active — build from persisted laps
        return buildSnapshotFromDb(sessionId);
    }

    /**
     * Link an unknown transponder to a user in an active session.
     * Retroactively updates all lap rows for that transponder (in-memory only).
     */
    public void linkTransponder(Long sessionId, String transponderNumber, Long userId, String racerName) {
        LivePracticeState state = activeStates.get(sessionId);
        if (state != null) {
            state.linkTransponder(transponderNumber, userId, racerName);
            timingHub.broadcastTimingUpdate(sessionId, state.calculatePositions());
        }
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private List<PracticeTimingRowDto> buildSnapshotFromDb(Long sessionId) {
        PracticeSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return Collections.emptyList();
        }

        LivePracticeState state = new LivePracticeState(sessionId, session.getBestLapN());
        List<PracticeLap> laps = lapRepository.findByPracticeSessionIdOrderByCrossingTimeAsc(sessionId);

        for (PracticeLap lap : laps) {
            Long userId = lap.getUser() != null ? lap.getUser().getId() : null;
            String racerName = null;
            if (lap.getUser() != null) {
                racerName = lap.getUser().getFirstName() + " " + lap.getUser().getLastName();
            }
            state.recordLap(
                    lap.getTransponderNumber(),
                    userId,
                    racerName,
                    lap.getLapTimeMs(),
                    lap.getCrossingTime()
            );
        }

        return state.calculatePositions();
    }

    /** The competitor whose active entry in the session's event uses this transponder, if any. */
    private String resolveCompetitorName(PracticeSession session, String transponderNumber) {
        if (session.getEvent() == null) {
            return null;
        }
        return entryRepository.findByEventId(session.getEvent().getId()).stream()
                .filter(e -> e.getStatus() != EntryStatus.WITHDRAWN)
                .filter(e -> transponderNumber.equals(e.getTransponderNumberSnapshot())
                        || transponderNumber.equals(e.getSecondaryTransponderNumber()))
                .map(Entry::getCompetitorId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .flatMap(competitorRepository::findById)
                .map(Competitor::getDisplayName)
                .orElse(null);
    }
}
