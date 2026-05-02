package dev.monkeypatch.rctiming.query.racecontrol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;
import jakarta.persistence.EntityNotFoundException;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.monkeypatch.rctiming.jooq.generated.tables.CarTagCategories.CAR_TAG_CATEGORIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.CarTagValues.CAR_TAG_VALUES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ClubProfiles.CLUB_PROFILES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RacingClasses.RACING_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Races.RACES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ResultSnapshots.RESULT_SNAPSHOTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Rounds.ROUNDS;

@Component
@Transactional(readOnly = true)
public class ResultSnapshotQuery {

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public ResultSnapshotQuery(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }

    public ResultSnapshotDto load(long raceId) {
        var row = dsl
                .select(
                        RESULT_SNAPSHOTS.RACE_ID,
                        RESULT_SNAPSHOTS.FINISHED_AT,
                        RESULT_SNAPSHOTS.POSITIONS_JSON,
                        RESULT_SNAPSHOTS.LAP_HISTORY_JSON,
                        ROUNDS.ROUND_NUMBER,
                        ROUNDS.TYPE,
                        RACES.HEAT_NUMBER,
                        RACES.FINAL_LETTER,
                        RACING_CLASSES.NAME.as("className"))
                .from(RESULT_SNAPSHOTS)
                .join(RACES).on(RACES.ID.eq(RESULT_SNAPSHOTS.RACE_ID))
                .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
                .join(EVENT_CLASSES).on(EVENT_CLASSES.ID.eq(RACES.EVENT_CLASS_ID))
                .join(RACING_CLASSES).on(RACING_CLASSES.ID.eq(EVENT_CLASSES.RACING_CLASS_ID))
                .where(RESULT_SNAPSHOTS.RACE_ID.eq(raceId))
                .fetchOne();

        if (row == null) {
            throw new EntityNotFoundException("No result snapshot for race " + raceId);
        }

        String roundType = row.get(ROUNDS.TYPE);
        int roundNumber = row.get(ROUNDS.ROUND_NUMBER);
        String className = row.get(RACING_CLASSES.NAME.as("className"), String.class);
        int heatNumber = row.get(RACES.HEAT_NUMBER);
        String finalLetter = row.get(RACES.FINAL_LETTER);
        String raceLabel = buildRaceLabel(roundType, roundNumber, className, heatNumber, finalLetter);

        Instant finishedAt = row.get(RESULT_SNAPSHOTS.FINISHED_AT).toInstant();

        JSONB positionsJsonb = row.get(RESULT_SNAPSHOTS.POSITIONS_JSON);
        JSONB lapHistoryJsonb = row.get(RESULT_SNAPSHOTS.LAP_HISTORY_JSON);

        try {
            List<ResultSnapshotDto.ResultRow> positions = objectMapper.readValue(
                    positionsJsonb.data(),
                    new TypeReference<List<ResultSnapshotDto.ResultRow>>() {});
            List<ResultSnapshotDto.PositionAtLap> lapHistory = objectMapper.readValue(
                    lapHistoryJsonb.data(),
                    new TypeReference<List<ResultSnapshotDto.PositionAtLap>>() {});

            ResultSnapshotDto.ClubBrandingDto branding = fetchClubBranding();
            boolean showCarTags = fetchShowCarTagsInResults();

            if (showCarTags && !positions.isEmpty()) {
                positions = enrichWithCarTags(positions);
            }

            return new ResultSnapshotDto(raceId, raceLabel, finishedAt, positions, lapHistory, branding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize result snapshot for race " + raceId, e);
        }
    }

    /**
     * Performs a two-pass car tag lookup for positions in the result.
     *
     * Pass 1: resolve entryId → carId from the entries table.
     * Pass 2: fetch all car tags for the resolved car IDs.
     * Returns a new positions list with carTags populated per row.
     *
     * T-07-04-03: car tags are non-sensitive (chassis/motor spec); admin controls display via club setting.
     */
    private List<ResultSnapshotDto.ResultRow> enrichWithCarTags(List<ResultSnapshotDto.ResultRow> positions) {
        // Collect entry IDs from the positions list
        List<Long> entryIds = positions.stream()
                .map(ResultSnapshotDto.ResultRow::entryId)
                .collect(Collectors.toList());

        // Pass 1: entryId → carId
        Map<Long, Long> carIdByEntryId = dsl
                .select(ENTRIES.ID, ENTRIES.CAR_ID)
                .from(ENTRIES)
                .where(ENTRIES.ID.in(entryIds))
                .and(ENTRIES.CAR_ID.isNotNull())
                .fetchMap(ENTRIES.ID, ENTRIES.CAR_ID);

        List<Long> carIds = new ArrayList<>(carIdByEntryId.values());
        if (carIds.isEmpty()) {
            // No entries have a car — return positions with empty carTags lists
            return positions.stream()
                    .map(r -> new ResultSnapshotDto.ResultRow(
                            r.position(), r.entryId(), r.driverName(), r.carNumber(),
                            r.lapsCompleted(), r.totalTimeMs(), r.bestLapMs(), r.gapToLeaderMs(),
                            List.of()))
                    .collect(Collectors.toList());
        }

        // Pass 2: carId → List<CarTagDto>
        Map<Long, List<ResultSnapshotDto.CarTagDto>> tagsByCarId = new HashMap<>();
        dsl.select(CAR_TAG_VALUES.CAR_ID, CAR_TAG_CATEGORIES.NAME, CAR_TAG_VALUES.VALUE)
                .from(CAR_TAG_VALUES)
                .join(CAR_TAG_CATEGORIES).on(CAR_TAG_CATEGORIES.ID.eq(CAR_TAG_VALUES.CATEGORY_ID))
                .where(CAR_TAG_VALUES.CAR_ID.in(carIds))
                .fetch()
                .forEach(r -> {
                    long carId = r.get(CAR_TAG_VALUES.CAR_ID);
                    String key = r.get(CAR_TAG_CATEGORIES.NAME);
                    String value = r.get(CAR_TAG_VALUES.VALUE);
                    tagsByCarId.computeIfAbsent(carId, k -> new ArrayList<>())
                               .add(new ResultSnapshotDto.CarTagDto(key, value));
                });

        // Build entryId → List<CarTagDto> by joining the two result sets
        Map<Long, List<ResultSnapshotDto.CarTagDto>> tagsByEntryId = new HashMap<>();
        carIdByEntryId.forEach((entryId, carId) ->
                tagsByEntryId.put(entryId, tagsByCarId.getOrDefault(carId, List.of())));

        // Re-map positions with carTags
        return positions.stream()
                .map(r -> new ResultSnapshotDto.ResultRow(
                        r.position(), r.entryId(), r.driverName(), r.carNumber(),
                        r.lapsCompleted(), r.totalTimeMs(), r.bestLapMs(), r.gapToLeaderMs(),
                        tagsByEntryId.getOrDefault(r.entryId(), List.of())))
                .collect(Collectors.toList());
    }

    /**
     * Reads show_car_tags_in_results from club_profiles.
     * The column was added in V24 — not present in pre-V24 jOOQ generated code,
     * so we use a plain DSL.field() reference.
     */
    private boolean fetchShowCarTagsInResults() {
        var result = dsl.select(DSL.field(DSL.name("show_car_tags_in_results"), Boolean.class))
                .from(CLUB_PROFILES)
                .limit(1)
                .fetchOne();
        if (result == null) return false;
        Boolean val = result.get(DSL.field(DSL.name("show_car_tags_in_results"), Boolean.class));
        return Boolean.TRUE.equals(val);
    }

    private ResultSnapshotDto.ClubBrandingDto fetchClubBranding() {
        var club = dsl.select(CLUB_PROFILES.NAME, CLUB_PROFILES.LOGO_URL)
                .from(CLUB_PROFILES)
                .limit(1)
                .fetchOne();
        if (club == null) return null;
        String logoUrl = club.get(CLUB_PROFILES.LOGO_URL);
        if (logoUrl == null || logoUrl.isBlank()) {
            return new ResultSnapshotDto.ClubBrandingDto(club.get(CLUB_PROFILES.NAME), null);
        }
        return new ResultSnapshotDto.ClubBrandingDto(club.get(CLUB_PROFILES.NAME), logoUrl);
    }

    private String buildRaceLabel(String roundType, int roundNumber, String className,
                                   int heatNumber, String finalLetter) {
        return switch (roundType) {
            case "PRACTICE" -> "Practice " + roundNumber + " — " + className + " — Heat " + heatNumber;
            case "QUALIFIER" -> "Qualifying " + roundNumber + " — " + className + " — Heat " + heatNumber;
            case "FINAL" -> (finalLetter != null ? finalLetter : "A") + " Final — " + className;
            default -> roundType + " " + roundNumber + " — " + className + " — Heat " + heatNumber;
        };
    }
}
