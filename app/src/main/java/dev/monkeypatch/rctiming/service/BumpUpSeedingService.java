package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Seeds finals grids from qualifying standings and applies bump-up promotions
 * after a lower final finishes.
 *
 * <p>Called by the race control layer (plan 05) after qualifying closes and after
 * each lower final completes.
 *
 * <p>Algorithm reference: HEAT-STRUCTURE-SPEC §"Bump-Up Finals Seeding Algorithm".
 */
@Service
@Transactional
public class BumpUpSeedingService {

    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;

    public BumpUpSeedingService(RaceRepository raceRepository,
                                 RaceEntryRepository raceEntryRepository) {
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
    }

    /**
     * Seeds all finals grids for a class after qualifying closes.
     *
     * <p>Algorithm (example: 20 drivers, 10/final, bumpCount=2, 2 finals A+B):
     * <pre>
     *   B-Final positions 1–10: drivers ranked 11–20 (lowest qualifiers)
     *   A-Final positions 1–8:  drivers ranked 1–8  (top qualifiers)
     *   A-Final positions 9–10: bump slots (bumped=true, gridPosition set, entryId=0)
     *                           filled by applyBumpUpResults after B-Final finishes
     * </pre>
     *
     * @param eventClassId        the EventClass to seed
     * @param qualifyingStandings entryIds in standings order (best first = index 0)
     * @param finalsCount         number of finals (1=A, 2=A+B, 3=A+B+C)
     * @param carsPerFinal        total cars per final
     * @param bumpCount           how many promote from each lower final
     */
    public void seedFinals(Long eventClassId,
                           List<Long> qualifyingStandings,
                           int finalsCount,
                           int carsPerFinal,
                           int bumpCount) {
        // Load all final races for this event class
        List<Race> finals = raceRepository.findByEventClassIdAndRoundType(eventClassId, RoundType.FINAL);
        // Sort by finalLetter DESC: C before B before A (lowest final first for assignment)
        finals.sort(Comparator.comparing(Race::getFinalLetter).reversed());

        // We assign standings from the bottom (slowest) upward.
        // standings position pointer: we'll track how many we've assigned
        int assigned = 0;

        // Build assignment list for each final (from lowest to highest letter)
        // Lowest final: positions 1..carsPerFinal = ranks (qualifyingStandings.size()-carsPerFinal+1)..last
        // Higher finals: positions 1..(carsPerFinal-bumpCount) = next block of regular qualifiers
        //                positions (carsPerFinal-bumpCount+1)..carsPerFinal = bump slots (empty)

        // Work out regular slot counts per final
        int[] regularSlots = new int[finals.size()];
        for (int fi = 0; fi < finals.size(); fi++) {
            boolean isLowestFinal = (fi == 0);
            regularSlots[fi] = isLowestFinal ? carsPerFinal : carsPerFinal - bumpCount;
        }

        // Total regular slots = sum of all regular slots
        // We assign from bottom of standings upward (lowest final gets the worst qualifiers)
        int totalRegular = 0;
        for (int s : regularSlots) totalRegular += s;

        // Start assigning from the bottom of the standings
        int standingsPtr = qualifyingStandings.size() - 1;

        for (int fi = 0; fi < finals.size(); fi++) {
            Race finalRace = finals.get(fi);
            boolean isLowestFinal = (fi == 0);
            int slots = regularSlots[fi];

            // Collect regular slot entries (slowest first, then reverse so best qualifier = pos 1)
            List<Long> slotEntries = new ArrayList<>(slots);
            for (int s = 0; s < slots && standingsPtr >= 0; s++) {
                slotEntries.add(0, qualifyingStandings.get(standingsPtr--)); // prepend to reverse order
            }

            // Remove placeholder entries for this final race
            List<RaceEntry> existing = raceEntryRepository.findByRaceIdOrderByGridPosition(finalRace.getId());
            raceEntryRepository.deleteAll(existing);

            // Create regular slot entries
            for (int i = 0; i < slotEntries.size(); i++) {
                RaceEntry entry = new RaceEntry();
                entry.setRaceId(finalRace.getId());
                entry.setEntryId(slotEntries.get(i));
                entry.setGridPosition(i + 1);
                entry.setBumped(false);
                raceEntryRepository.save(entry);
            }

            // Create bump slots for non-lowest finals
            if (!isLowestFinal) {
                for (int bump = 0; bump < bumpCount; bump++) {
                    RaceEntry bumpEntry = new RaceEntry();
                    bumpEntry.setRaceId(finalRace.getId());
                    bumpEntry.setEntryId(0L); // placeholder until applyBumpUpResults fills it
                    bumpEntry.setGridPosition(slots + 1 + bump);
                    bumpEntry.setBumped(true);
                    raceEntryRepository.save(bumpEntry);
                }
            }
        }
    }

    /**
     * Fills bump slots in the next-higher final after a lower final finishes.
     *
     * <p>Finds the next-higher final for the same EventClass (C→B→A) and fills
     * the bump slots (bumped=true) with the top N entryIds in order.
     * First bump-up finisher → first bump slot (lowest gridPosition among bump slots).
     *
     * @param finishedFinalRaceId the Race ID of the final that just finished
     * @param topNEntryIds        entry IDs of the top N finishers (first = best)
     */
    public void applyBumpUpResults(Long finishedFinalRaceId, List<Long> topNEntryIds) {
        Race finishedRace = raceRepository.findById(finishedFinalRaceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Race not found: " + finishedFinalRaceId));

        String currentLetter = finishedRace.getFinalLetter();
        if (currentLetter == null || currentLetter.isEmpty()) {
            throw new IllegalArgumentException(
                    "Race " + finishedFinalRaceId + " is not a final");
        }

        // Next higher final: 'C' → 'B', 'B' → 'A'
        char nextChar = (char) (currentLetter.charAt(0) - 1);
        if (nextChar < 'A') {
            throw new IllegalArgumentException(
                    "No higher final above " + currentLetter
                    + " for race " + finishedFinalRaceId);
        }
        String nextFinalLetter = String.valueOf(nextChar);

        // Find the next final race for the same EventClass
        List<Race> nextFinals = raceRepository.findByEventClassIdAndFinalLetter(
                finishedRace.getEventClassId(), nextFinalLetter);
        if (nextFinals.isEmpty()) {
            throw new IllegalStateException(
                    "No " + nextFinalLetter + "-final found for event class "
                    + finishedRace.getEventClassId());
        }
        Race nextFinal = nextFinals.get(0);

        // Find bump slots in the next final, ordered by gridPosition asc
        List<RaceEntry> bumpSlots = raceEntryRepository.findByRaceIdOrderByGridPosition(nextFinal.getId())
                .stream()
                .filter(RaceEntry::isBumped)
                .sorted(Comparator.comparingInt(e ->
                        e.getGridPosition() == null ? Integer.MAX_VALUE : e.getGridPosition()))
                .collect(Collectors.toList());

        // Fill bump slots with topNEntryIds
        for (int i = 0; i < topNEntryIds.size() && i < bumpSlots.size(); i++) {
            RaceEntry slot = bumpSlots.get(i);
            slot.setEntryId(topNEntryIds.get(i));
            raceEntryRepository.save(slot);
        }
    }
}
