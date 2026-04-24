package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Recalculates qualifying standings for an EventClass.
 *
 * <p>Called after every QUALIFIER race finishes. Returns entryIds ordered by
 * qualifying position (best first).
 *
 * <p><b>Open Question #2</b> (from RESEARCH.md): The timing service (plan 04/05) will
 * maintain in-memory {@code LiveRaceState} result snapshots. This service currently
 * accepts pre-computed per-entry results as a parameter. In the final integration,
 * the caller (race state machine plan 05) will extract these from the LiveRaceState
 * result snapshots and pass them in. The sort rule used here is FTQ (fastest-time-quality):
 * primary sort by laps completed DESC, secondary by best lap time ASC — per FORMAT-09.
 * The open question is whether ties in laps and time are broken by position in the heat
 * or by total time for the run; this implementation uses best lap time as the tiebreak
 * which aligns with FORMAT-09's FTQ definition.
 */
@Service
@Transactional
public class QualifyingStandingsService {

    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;

    public QualifyingStandingsService(RaceRepository raceRepository,
                                       RaceEntryRepository raceEntryRepository) {
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
    }

    /**
     * Sorts qualifying results and returns entryIds in standings order (best first).
     *
     * <p>Sort rule (FTQ — FORMAT-09): laps completed DESC, then best lap time ASC.
     *
     * @param eventClassId the EventClass being ranked (reserved for future use — e.g.
     *                     fetching results directly from the DB in a later phase)
     * @param results      pre-computed per-entry qualifying results
     * @return entryIds ordered by qualifying position, best first
     */
    public List<Long> recalculateStandings(Long eventClassId, List<QualifyingResult> results) {
        return results.stream()
                .sorted(Comparator
                        .comparingInt(QualifyingResult::lapsCompleted).reversed()
                        .thenComparingLong(QualifyingResult::bestLapMs))
                .map(QualifyingResult::entryId)
                .toList();
    }

    /**
     * A single driver's qualifying result across all heats for an EventClass.
     *
     * @param entryId       the Entry identifier
     * @param bestLapMs     best single-lap time in milliseconds across all heats (lower = faster)
     * @param lapsCompleted total laps completed across all heats
     */
    public record QualifyingResult(Long entryId, long bestLapMs, int lapsCompleted) {}
}
