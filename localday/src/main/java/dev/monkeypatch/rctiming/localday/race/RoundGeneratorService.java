package dev.monkeypatch.rctiming.localday.race;

import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Local reimplementation of the runtime portion of the cloud's {@code RoundGeneratorService} —
 * propagating a finished race's finishing order into the next round's starting grid.
 *
 * <p>Deliberately independent of {@code app}'s {@code RoundGeneratorService} (KD3) — same
 * target behavior, no shared code or dependency between the two modules. This unit intentionally
 * does <b>not</b> reimplement the cloud's initial event-wide schedule generation (ability-rating
 * snake-draft heat assignment) — that requires {@code EventClass}/{@code UserClassRating}/
 * {@code Entry} data {@code :localday} does not have; the cloud remains authoritative for the
 * initial schedule and hands a pre-generated snapshot to {@code :localday} (a later unit).
 *
 * <p>Operates on {@link CachedRaceEntry}, using {@code cachedScheduleId} in place of the cloud's
 * {@code raceId} and {@code cachedEntryId} in place of the cloud's {@code entryId}.
 */
@Service
public class RoundGeneratorService {

    private final CachedRaceEntryRepository cachedRaceEntryRepository;

    public RoundGeneratorService(CachedRaceEntryRepository cachedRaceEntryRepository) {
        this.cachedRaceEntryRepository = cachedRaceEntryRepository;
    }

    /**
     * Sets gridPosition on CachedRaceEntry rows for a specific schedule row (race/heat) using
     * the finishing order from the previous round's same heat. Best finisher (index 0) gets
     * gridPosition=1.
     */
    public void applyPreviousRoundFinishingOrder(Long newScheduleId, List<Long> entryIdsInFinishingOrder) {
        List<CachedRaceEntry> entries =
                cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(newScheduleId);
        Map<Long, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < entryIdsInFinishingOrder.size(); i++) {
            positionMap.put(entryIdsInFinishingOrder.get(i), i + 1);
        }
        for (CachedRaceEntry entry : entries) {
            Integer pos = positionMap.get(entry.getCachedEntryId());
            if (pos != null) {
                entry.setGridPosition(pos);
                cachedRaceEntryRepository.save(entry);
            }
        }
    }
}
