package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.race.RoundStatus;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import dev.monkeypatch.rctiming.domain.race.StartType;
import dev.monkeypatch.rctiming.domain.user.UserClassRating;
import dev.monkeypatch.rctiming.domain.user.UserClassRatingRepository;
import dev.monkeypatch.rctiming.service.dto.RoundGenerationRequest;
import dev.monkeypatch.rctiming.service.dto.RoundGenerationRequest.ClassFinalsConfig;
import dev.monkeypatch.rctiming.service.dto.RoundPreviewDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates all Round, Race, and RaceEntry records for an event in a single transaction.
 *
 * <p>Heat assignment (which drivers are in Heat 1 vs Heat 2) is fixed at generation time
 * using a snake-draft algorithm seeded by ability rating. The same heat membership persists
 * across all Practice and Qualifying rounds.
 *
 * <p>Finals grids are created empty; seeding is deferred to {@link BumpUpSeedingService}
 * once qualifying completes.
 */
@Service
@Transactional
public class RoundGeneratorService {

    private final RoundRepository roundRepository;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final EntryRepository entryRepository;
    private final EventClassRepository eventClassRepository;
    private final UserClassRatingRepository userClassRatingRepository;
    private final BumpUpSeedingService bumpUpSeedingService;

    public RoundGeneratorService(RoundRepository roundRepository,
                                  RaceRepository raceRepository,
                                  RaceEntryRepository raceEntryRepository,
                                  EntryRepository entryRepository,
                                  EventClassRepository eventClassRepository,
                                  UserClassRatingRepository userClassRatingRepository,
                                  BumpUpSeedingService bumpUpSeedingService) {
        this.roundRepository = roundRepository;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.entryRepository = entryRepository;
        this.eventClassRepository = eventClassRepository;
        this.userClassRatingRepository = userClassRatingRepository;
        this.bumpUpSeedingService = bumpUpSeedingService;
    }

    /**
     * Computes what would be generated without persisting anything.
     *
     * @param request generation parameters
     * @return preview rows ordered by sequenceInEvent
     */
    public List<RoundPreviewDto> preview(RoundGenerationRequest request) {
        GenerationPlan plan = buildPlan(request);
        return plan.toPreviewDtos();
    }

    /**
     * Persists all Round, Race, and RaceEntry records in a single transaction.
     *
     * @param request generation parameters
     * @throws IllegalStateException if run order already exists for the event
     */
    public void generate(RoundGenerationRequest request) {
        if (roundRepository.existsByEventId(request.eventId())) {
            throw new IllegalStateException(
                    "Run order already generated for event " + request.eventId());
        }
        GenerationPlan plan = buildPlan(request);
        plan.persist();
    }

    /**
     * Sets gridPosition on RaceEntry rows for a specific race using the finishing order
     * from the previous round's same heat. Best finisher (index 0) gets gridPosition=1.
     *
     * <p>Called by the race state machine (plan 05) when a round completes, to set up
     * start positions for the corresponding race in the next round.
     *
     * @param newRaceId              the race whose grid positions to update
     * @param entryIdsInFinishingOrder entry IDs ordered best-first from the previous round
     */
    public void applyPreviousRoundFinishingOrder(Long newRaceId,
                                                  List<Long> entryIdsInFinishingOrder) {
        List<RaceEntry> entries = raceEntryRepository.findByRaceIdOrderByGridPosition(newRaceId);
        // Build a map from entryId → finishing position (1-based)
        Map<Long, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < entryIdsInFinishingOrder.size(); i++) {
            positionMap.put(entryIdsInFinishingOrder.get(i), i + 1);
        }
        for (RaceEntry entry : entries) {
            Integer pos = positionMap.get(entry.getEntryId());
            if (pos != null) {
                entry.setGridPosition(pos);
                raceEntryRepository.save(entry);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private implementation
    // -------------------------------------------------------------------------

    /**
     * Builds the complete generation plan (rounds, races, heat assignments) without any I/O.
     */
    private GenerationPlan buildPlan(RoundGenerationRequest request) {
        List<EventClass> eventClasses = eventClassRepository.findByEventId(request.eventId());

        // For each class, load CONFIRMED entries and compute snake-draft heat assignment
        Map<Long, List<List<Long>>> heatsByClass = new HashMap<>(); // classId -> list of heats (each heat is list of entryIds)
        Map<Long, Map<Long, String>> driverNamesByClass = new HashMap<>(); // classId -> entryId -> display name (stub: entryId as string)

        for (EventClass ec : eventClasses) {
            List<Entry> entries = entryRepository.findByEventClassIdAndStatus(
                    ec.getId(), EntryStatus.CONFIRMED);

            // Load ratings for this class
            Long racingClassId = ec.getRacingClassId();
            Map<Long, Integer> ratingByUserId = new HashMap<>();
            if (racingClassId != null) {
                List<UserClassRating> ratings = userClassRatingRepository.findByRacingClassId(racingClassId);
                for (UserClassRating r : ratings) {
                    ratingByUserId.put(r.getUserId(), r.getRating().intValue());
                }
            }

            // Sort entries: abilityRating DESC, then entryId ASC (tie-break / no-rating default 50)
            entries.sort(Comparator
                    .<Entry>comparingInt(e -> -(ratingByUserId.getOrDefault(e.getUserId(), 50)))
                    .thenComparingLong(Entry::getId));

            int heatCount = entries.isEmpty() ? 0
                    : (int) Math.ceil((double) entries.size() / request.maxCarsPerHeat());
            if (heatCount == 0) {
                heatsByClass.put(ec.getId(), List.of());
                driverNamesByClass.put(ec.getId(), Map.of());
                continue;
            }

            // Snake draft: fill heats 0..heatCount-1 in snake order
            List<List<Long>> heats = new ArrayList<>();
            for (int h = 0; h < heatCount; h++) {
                heats.add(new ArrayList<>());
            }
            for (int i = 0; i < entries.size(); i++) {
                int pass = i / heatCount;
                int offset = i % heatCount;
                int heatIndex = (pass % 2 == 0) ? offset : (heatCount - 1 - offset);
                heats.get(heatIndex).add(entries.get(i).getId());
            }

            heatsByClass.put(ec.getId(), heats);

            // Driver names: use entryId as placeholder (no User join in this layer)
            Map<Long, String> names = new HashMap<>();
            for (Entry e : entries) {
                names.put(e.getId(), "Entry#" + e.getId());
            }
            driverNamesByClass.put(ec.getId(), names);
        }

        return new GenerationPlan(request, eventClasses, heatsByClass, driverNamesByClass);
    }

    // -------------------------------------------------------------------------
    // Inner plan class
    // -------------------------------------------------------------------------

    private class GenerationPlan {

        private final RoundGenerationRequest request;
        private final List<EventClass> eventClasses;
        private final Map<Long, List<List<Long>>> heatsByClass;
        private final Map<Long, Map<Long, String>> driverNamesByClass;

        GenerationPlan(RoundGenerationRequest request,
                       List<EventClass> eventClasses,
                       Map<Long, List<List<Long>>> heatsByClass,
                       Map<Long, Map<Long, String>> driverNamesByClass) {
            this.request = request;
            this.eventClasses = eventClasses;
            this.heatsByClass = heatsByClass;
            this.driverNamesByClass = driverNamesByClass;
        }

        List<RoundPreviewDto> toPreviewDtos() {
            List<RoundPreviewDto> result = new ArrayList<>();
            int sequence = 1;

            // Practice rounds
            for (int r = 1; r <= request.practiceRoundsCount(); r++) {
                sequence = addPreviewRound(result, RoundType.PRACTICE, r, sequence, false);
            }
            // Qualifying rounds
            for (int r = 1; r <= request.qualifyingRoundsCount(); r++) {
                sequence = addPreviewRound(result, RoundType.QUALIFIER, r, sequence, false);
            }
            // Finals rounds (one "round" per final letter per class)
            sequence = addFinalsPreview(result, sequence);

            return result;
        }

        void persist() {
            Instant now = Instant.now();
            int sequence = 1;

            // Practice rounds
            for (int r = 1; r <= request.practiceRoundsCount(); r++) {
                sequence = persistRound(RoundType.PRACTICE, r, sequence, now,
                        r == 1 && request.practiceRoundsCount() >= 1);
            }
            // Qualifying rounds
            for (int r = 1; r <= request.qualifyingRoundsCount(); r++) {
                boolean isFirstRound = (r == 1 && request.practiceRoundsCount() == 0);
                sequence = persistRound(RoundType.QUALIFIER, r, sequence, now, isFirstRound);
            }
            // Finals (empty grids — seeded by BumpUpSeedingService after qualifying)
            persistFinals(sequence, now);
        }

        // --- Preview helpers ---

        private int addPreviewRound(List<RoundPreviewDto> result,
                                     RoundType type, int roundNumber,
                                     int sequenceStart, boolean isFinals) {
            int seq = sequenceStart;
            for (EventClass ec : eventClasses) {
                List<List<Long>> heats = heatsByClass.getOrDefault(ec.getId(), List.of());
                Map<Long, String> names = driverNamesByClass.getOrDefault(ec.getId(), Map.of());
                for (int h = 0; h < heats.size(); h++) {
                    List<String> driverNames = heats.get(h).stream()
                            .map(eid -> names.getOrDefault(eid, "Entry#" + eid))
                            .collect(Collectors.toList());
                    result.add(new RoundPreviewDto(
                            seq++,
                            typeLabel(type, roundNumber),
                            roundNumber,
                            "Class#" + ec.getId(),
                            h + 1,
                            null,
                            driverNames
                    ));
                }
            }
            return seq;
        }

        private int addFinalsPreview(List<RoundPreviewDto> result, int sequenceStart) {
            int seq = sequenceStart;
            for (EventClass ec : eventClasses) {
                int[] finalsConfig = resolveFinalsConfig(ec);
                int finalsCount = finalsConfig[0];
                for (int f = 0; f < finalsCount; f++) {
                    // finals go from highest letter to lowest (C runs first, then B, then A)
                    // but in preview we list them by final letter A, B, C ordering
                    String finalLetter = String.valueOf((char) ('A' + f));
                    result.add(new RoundPreviewDto(
                            seq++,
                            finalLetter + " Final",
                            1,
                            "Class#" + ec.getId(),
                            1,
                            finalLetter,
                            List.of() // empty until seeded
                    ));
                }
            }
            return seq;
        }

        // --- Persist helpers ---

        private int persistRound(RoundType type, int roundNumber, int sequenceStart,
                                  Instant now, boolean isFirstRound) {
            Round round = new Round();
            round.setEventId(request.eventId());
            round.setType(type);
            round.setRoundNumber(roundNumber);
            round.setSequenceInEvent(sequenceStart);
            round.setStatus(RoundStatus.PENDING);
            round.setCreatedAt(now);
            round.setUpdatedAt(now);
            Round savedRound = roundRepository.save(round);

            int seq = sequenceStart;
            for (EventClass ec : eventClasses) {
                List<List<Long>> heats = heatsByClass.getOrDefault(ec.getId(), List.of());
                for (int h = 0; h < heats.size(); h++) {
                    Race race = new Race();
                    race.setRoundId(savedRound.getId());
                    race.setEventClassId(ec.getId());
                    race.setHeatNumber(h + 1);
                    race.setSequenceInRound(seq++);
                    race.setFinalLetter(null);
                    race.setStartType(StartType.STAGGER);
                    race.setStatus(RaceStatus.PENDING);
                    race.setCreatedAt(now);
                    race.setUpdatedAt(now);
                    Race savedRace = raceRepository.save(race);

                    List<Long> entryIds = heats.get(h);
                    for (int pos = 0; pos < entryIds.size(); pos++) {
                        RaceEntry entry = new RaceEntry();
                        entry.setRaceId(savedRace.getId());
                        entry.setEntryId(entryIds.get(pos));
                        // Round 1: set gridPosition to seed order within heat (1-based).
                        // Subsequent rounds: gridPosition=null — will be assigned when the
                        // previous round finishes via applyPreviousRoundFinishingOrder (plan 05).
                        entry.setGridPosition(isFirstRound ? pos + 1 : null);
                        entry.setCarNumber(pos + 1);  // car_number = 1-N, same as heat slot order for qualifying heats
                        entry.setBumped(false);
                        raceEntryRepository.save(entry);
                    }
                }
            }
            return seq;
        }

        private void persistFinals(int sequenceStart, Instant now) {
            // Finals: one round per final "level" (A, B, C).
            // Run order: lowest final first (C runs before B before A).
            // We create rounds in A→B→C order here; the sequenceInEvent determines run order.
            for (EventClass ec : eventClasses) {
                int[] finalsConfig = resolveFinalsConfig(ec);
                int finalsCount = finalsConfig[0];
                // finals run lowest-first (C→B→A), so we persist C first with lower sequence
                // Actually we store them A=1, B=2, C=3 in finalLetter, but run C first by
                // assigning sequenceInEvent in reverse (C gets lower seq than B, B lower than A).
                // Per HEAT-STRUCTURE-SPEC: C runs first, bumps go to B, then B runs, bumps to A.
                // We create rounds in reverse letter order so sequenceInEvent matches run order.
                for (int f = finalsCount - 1; f >= 0; f--) {
                    String finalLetter = String.valueOf((char) ('A' + f));
                    Instant roundNow = Instant.now();
                    Round round = new Round();
                    round.setEventId(request.eventId());
                    round.setType(RoundType.FINAL);
                    round.setRoundNumber(f + 1);
                    round.setSequenceInEvent(sequenceStart++);
                    round.setStatus(RoundStatus.PENDING);
                    round.setCreatedAt(roundNow);
                    round.setUpdatedAt(roundNow);
                    Round savedRound = roundRepository.save(round);

                    Race race = new Race();
                    race.setRoundId(savedRound.getId());
                    race.setEventClassId(ec.getId());
                    race.setHeatNumber(1);
                    race.setSequenceInRound(1);
                    race.setFinalLetter(finalLetter);
                    race.setStartType(StartType.GRID);
                    race.setStatus(RaceStatus.PENDING);
                    race.setCreatedAt(roundNow);
                    race.setUpdatedAt(roundNow);
                    Race savedRace = raceRepository.save(race);

                    // Create empty RaceEntry rows with no gridPosition — seeded by BumpUpSeedingService
                    // after qualifying completes (called from plan 05 state machine transition).
                    int carsPerFinal = finalsConfig[1];
                    for (int slot = 0; slot < carsPerFinal; slot++) {
                        RaceEntry entry = new RaceEntry();
                        entry.setRaceId(savedRace.getId());
                        entry.setEntryId(0L); // placeholder — BumpUpSeedingService fills this
                        entry.setGridPosition(null);
                        entry.setBumped(f < finalsCount - 1 && slot >= (carsPerFinal - finalsConfig[2]));
                        raceEntryRepository.save(entry);
                    }
                }
            }
        }

        // --- Config helpers ---

        private int[] resolveFinalsConfig(EventClass ec) {
            // Look for per-class override in request first
            ClassFinalsConfig override = request.classFinalsConfigs().stream()
                    .filter(c -> ec.getId().equals(c.eventClassId()))
                    .findFirst()
                    .orElse(null);

            int finalsCount = coalesce(
                    override != null ? override.finalsCount() : null,
                    ec.getFinalsCount(),
                    1);
            int carsPerFinal = coalesce(
                    override != null ? override.carsPerFinal() : null,
                    ec.getCarsPerFinal(),
                    request.maxCarsPerHeat());
            int bumpCount = coalesce(
                    override != null ? override.bumpCount() : null,
                    ec.getBumpCount(),
                    0);

            return new int[]{finalsCount, carsPerFinal, bumpCount};
        }

        private int coalesce(Integer... values) {
            for (Integer v : values) {
                if (v != null) return v;
            }
            return 0;
        }

        private String typeLabel(RoundType type, int roundNumber) {
            return switch (type) {
                case PRACTICE -> "Practice " + roundNumber;
                case QUALIFIER -> "Qualifying " + roundNumber;
                case FINAL -> throw new IllegalArgumentException("Use finalLetter for finals");
            };
        }
    }
}
