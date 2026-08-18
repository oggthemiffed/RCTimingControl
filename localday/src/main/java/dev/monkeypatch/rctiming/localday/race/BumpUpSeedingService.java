package dev.monkeypatch.rctiming.localday.race;

import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Local reimplementation of the cloud's {@code BumpUpSeedingService} — seeding finals grids
 * after qualifying closes, and filling bump-up slots as lower finals finish.
 *
 * <p>Deliberately independent of {@code app}'s {@code BumpUpSeedingService} (KD3) — same target
 * behavior, no shared code or dependency between the two modules.
 *
 * <p>Two substitutions from the cloud reference, per this unit's scoping:
 * <ul>
 *   <li>{@code className} (a plain string on {@link CachedScheduleEntry}) is used in place of
 *       the cloud's numeric {@code eventClassId} to group finals belonging to the same class —
 *       {@code :localday} has no synced event-class id.</li>
 *   <li>An unfilled bump slot's entry reference ({@link CachedRaceEntry#getCachedEntryId()}) is
 *       represented as {@code null}, not the cloud's {@code entryId == 0L} sentinel.</li>
 * </ul>
 */
@Service
public class BumpUpSeedingService {

    private final CachedScheduleEntryRepository cachedScheduleEntryRepository;
    private final CachedRaceEntryRepository cachedRaceEntryRepository;

    public BumpUpSeedingService(CachedScheduleEntryRepository cachedScheduleEntryRepository,
                                 CachedRaceEntryRepository cachedRaceEntryRepository) {
        this.cachedScheduleEntryRepository = cachedScheduleEntryRepository;
        this.cachedRaceEntryRepository = cachedRaceEntryRepository;
    }

    /**
     * Seeds all finals grids for a class after qualifying closes.
     * Algorithm (example: 20 drivers, 10/final, bumpCount=2, 2 finals A+B):
     *   B-Final positions 1-10: drivers ranked 11-20 (lowest qualifiers)
     *   A-Final positions 1-8:  drivers ranked 1-8  (top qualifiers)
     *   A-Final positions 9-10: bump slots (bumped=true, gridPosition set, cachedEntryId=null)
     *                           filled by applyBumpUpResults after B-Final finishes
     *
     * <p>The lowest final draws its regular slots from the worst-ranked end of standings;
     * every other ("non-lowest") final draws from the best-ranked end, processed from the top
     * final downward, so the fastest qualifiers always land in the top final first rather than
     * in whichever final happens to be processed first. The two passes are bounded against each
     * other so they can never claim the same standings entry twice when the qualifying field is
     * smaller than total regular capacity.
     */
    public void seedFinals(String className, List<Long> qualifyingStandings,
                            int finalsCount, int carsPerFinal, int bumpCount) {
        List<CachedScheduleEntry> finals =
                cachedScheduleEntryRepository.findByClassNameAndFinalLetterIsNotNull(className);
        finals.sort(Comparator.comparing(CachedScheduleEntry::getFinalLetter).reversed()); // C, B, A order

        int[] regularSlots = new int[finals.size()];
        for (int fi = 0; fi < finals.size(); fi++) {
            boolean isLowestFinal = (fi == 0);
            regularSlots[fi] = isLowestFinal ? carsPerFinal : carsPerFinal - bumpCount;
        }

        List<List<Long>> slotEntriesByFinal = new ArrayList<>(finals.size());
        for (int fi = 0; fi < finals.size(); fi++) {
            slotEntriesByFinal.add(null);
        }

        int bottomPtr = qualifyingStandings.size() - 1;
        int lowestSlots = regularSlots[0];
        List<Long> lowestEntries = new ArrayList<>(lowestSlots);
        for (int s = 0; s < lowestSlots && bottomPtr >= 0; s++) {
            lowestEntries.add(0, qualifyingStandings.get(bottomPtr--)); // prepend to reverse order
        }
        slotEntriesByFinal.set(0, lowestEntries);

        int topPtr = 0;
        for (int fi = finals.size() - 1; fi >= 1; fi--) {
            int slots = regularSlots[fi];
            List<Long> entries = new ArrayList<>(slots);
            for (int s = 0; s < slots && topPtr <= bottomPtr; s++) {
                entries.add(qualifyingStandings.get(topPtr++));
            }
            slotEntriesByFinal.set(fi, entries);
        }

        for (int fi = 0; fi < finals.size(); fi++) {
            CachedScheduleEntry finalRace = finals.get(fi);
            boolean isLowestFinal = (fi == 0);
            List<Long> slotEntries = slotEntriesByFinal.get(fi);

            cachedRaceEntryRepository.deleteAllByCachedScheduleId(finalRace.getId());

            for (int i = 0; i < slotEntries.size(); i++) {
                long entryId = slotEntries.get(i);
                int carNum = qualifyingStandings.indexOf(entryId) + 1;
                CachedRaceEntry entry = new CachedRaceEntry();
                entry.setCachedScheduleId(finalRace.getId());
                entry.setCachedEntryId(entryId);
                entry.setGridPosition(i + 1);
                entry.setCarNumber(carNum > 0 ? carNum : null);
                entry.setBumped(false);
                cachedRaceEntryRepository.save(entry);
            }

            if (!isLowestFinal) {
                for (int bump = 0; bump < bumpCount; bump++) {
                    CachedRaceEntry bumpEntry = new CachedRaceEntry();
                    bumpEntry.setCachedScheduleId(finalRace.getId());
                    bumpEntry.setCachedEntryId(null); // unfilled until applyBumpUpResults fills it
                    bumpEntry.setGridPosition(regularSlots[fi] + 1 + bump);
                    bumpEntry.setCarNumber(null);
                    bumpEntry.setBumped(true);
                    cachedRaceEntryRepository.save(bumpEntry);
                }
            }
        }
    }

    /**
     * Fills bump slots in the next-higher final after a lower final finishes.
     * Finds the next-higher final for the same class (C-&gt;B-&gt;A) and fills the bump slots
     * (bumped=true) with the top N entryIds in order. First bump-up finisher -&gt; first bump
     * slot (lowest gridPosition among bump slots).
     */
    public void applyBumpUpResults(Long finishedFinalScheduleId, List<Long> topNEntryIds) {
        CachedScheduleEntry finishedRace = cachedScheduleEntryRepository.findById(finishedFinalScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule row not found: " + finishedFinalScheduleId));
        String currentLetter = finishedRace.getFinalLetter();
        if (currentLetter == null || currentLetter.isEmpty()) {
            throw new IllegalArgumentException("Schedule row " + finishedFinalScheduleId + " is not a final");
        }
        char nextChar = (char) (currentLetter.charAt(0) - 1); // C -> B, B -> A
        if (nextChar < 'A') {
            throw new IllegalArgumentException("No higher final above " + currentLetter
                    + " for schedule row " + finishedFinalScheduleId);
        }
        String nextFinalLetter = String.valueOf(nextChar);

        List<CachedScheduleEntry> nextFinals =
                cachedScheduleEntryRepository.findByClassNameAndFinalLetterIsNotNull(finishedRace.getClassName())
                        .stream()
                        .filter(r -> nextFinalLetter.equals(r.getFinalLetter()))
                        .collect(Collectors.toList());
        if (nextFinals.isEmpty()) {
            throw new IllegalStateException(
                    "No " + nextFinalLetter + "-final found for class " + finishedRace.getClassName());
        }
        CachedScheduleEntry nextFinal = nextFinals.get(0);

        List<CachedRaceEntry> bumpSlots =
                cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(nextFinal.getId())
                        .stream()
                        .filter(CachedRaceEntry::isBumped)
                        .sorted(Comparator.comparingInt(e -> e.getGridPosition() == null ? Integer.MAX_VALUE : e.getGridPosition()))
                        .collect(Collectors.toList());

        for (int i = 0; i < topNEntryIds.size() && i < bumpSlots.size(); i++) {
            CachedRaceEntry slot = bumpSlots.get(i);
            slot.setCachedEntryId(topNEntryIds.get(i));
            cachedRaceEntryRepository.save(slot);
        }
    }
}
