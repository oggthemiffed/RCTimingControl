package dev.monkeypatch.rctiming.query.racecontrol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;
import jakarta.persistence.EntityNotFoundException;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.ClubProfiles.CLUB_PROFILES;
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

            return new ResultSnapshotDto(raceId, raceLabel, finishedAt, positions, lapHistory, branding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize result snapshot for race " + raceId, e);
        }
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
