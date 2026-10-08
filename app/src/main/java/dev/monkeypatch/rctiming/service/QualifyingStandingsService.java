package dev.monkeypatch.rctiming.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.ResultSnapshotRepository;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out the qualifying order of an event class from the results stored when its qualifying heats
 * finished, so the order cannot be made up by whoever asks for the finals to be seeded (#137). The stored
 * results already have penalties and marshal laps applied.
 *
 * <p>The rule is FTQ (FORMAT-09): total laps over all the class's qualifying heats, most first, then the
 * best single lap, quickest first.
 */
@Service
public class QualifyingStandingsService {

    private static final TypeReference<List<ResultSnapshotDto.ResultRow>> ROWS = new TypeReference<>() {};

    private final RaceRepository raceRepository;
    private final ResultSnapshotRepository resultSnapshotRepository;
    private final ObjectMapper objectMapper;

    public QualifyingStandingsService(RaceRepository raceRepository,
                                      ResultSnapshotRepository resultSnapshotRepository,
                                      ObjectMapper objectMapper) {
        this.raceRepository = raceRepository;
        this.resultSnapshotRepository = resultSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Entry ids in qualifying order, best first, for everyone with a result in one of the class's finished
     * qualifying heats. Empty when no qualifying heat has finished.
     */
    public List<Long> standingsFor(Long eventClassId) {
        Map<Long, QualifyingResult> byEntry = new LinkedHashMap<>();
        for (Race race : raceRepository.findByEventClassIdAndRoundType(eventClassId, RoundType.QUALIFIER)) {
            if (race.getStatus() != RaceStatus.FINISHED) {
                continue;
            }
            resultSnapshotRepository.findByRaceId(race.getId()).ifPresent(snapshot -> {
                for (ResultSnapshotDto.ResultRow row : rowsOf(race.getId(), snapshot.getPositionsJson())) {
                    long bestLap = row.bestLapMs() == null ? Long.MAX_VALUE : row.bestLapMs();
                    byEntry.merge(row.entryId(), new QualifyingResult(row.entryId(), bestLap, row.lapsCompleted()),
                            QualifyingResult::plus);
                }
            });
        }
        return rank(List.copyOf(byEntry.values()));
    }

    /** Laps completed, most first, then best lap, quickest first. */
    static List<Long> rank(List<QualifyingResult> results) {
        return results.stream()
                .sorted(Comparator
                        .comparingInt(QualifyingResult::lapsCompleted).reversed()
                        .thenComparingLong(QualifyingResult::bestLapMs))
                .map(QualifyingResult::entryId)
                .toList();
    }

    private List<ResultSnapshotDto.ResultRow> rowsOf(long raceId, String positionsJson) {
        try {
            return objectMapper.readValue(positionsJson, ROWS);
        } catch (JsonProcessingException e) {
            // Seeding finals from the other heats alone would put drivers in the wrong finals
            throw new IllegalStateException("The stored result of race " + raceId + " could not be read", e);
        }
    }

    /**
     * One driver's qualifying result across the class's heats.
     *
     * @param bestLapMs     best single lap in milliseconds across all heats; {@link Long#MAX_VALUE} for no lap
     * @param lapsCompleted total laps across all heats
     */
    record QualifyingResult(Long entryId, long bestLapMs, int lapsCompleted) {

        QualifyingResult plus(QualifyingResult other) {
            return new QualifyingResult(entryId, Math.min(bestLapMs, other.bestLapMs),
                    lapsCompleted + other.lapsCompleted);
        }
    }
}
