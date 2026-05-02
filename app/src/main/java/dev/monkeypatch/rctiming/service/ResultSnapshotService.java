package dev.monkeypatch.rctiming.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.ResultSnapshot;
import dev.monkeypatch.rctiming.domain.race.ResultSnapshotRepository;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.timing.LapTimingService;
import dev.monkeypatch.rctiming.timing.LiveRaceState;
import dev.monkeypatch.rctiming.timing.dto.LiveTimingRowDto;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ResultSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ResultSnapshotService.class);

    private final RaceRepository raceRepository;
    private final RoundRepository roundRepository;
    private final LapTimingService lapTimingService;
    private final ResultSnapshotRepository resultSnapshotRepository;
    private final ObjectMapper objectMapper;
    private final RaceEntryRepository raceEntryRepository;
    private final EntryRepository entryRepository;
    private final UserRepository userRepository;

    public ResultSnapshotService(RaceRepository raceRepository,
                                  RoundRepository roundRepository,
                                  LapTimingService lapTimingService,
                                  ResultSnapshotRepository resultSnapshotRepository,
                                  ObjectMapper objectMapper,
                                  RaceEntryRepository raceEntryRepository,
                                  EntryRepository entryRepository,
                                  UserRepository userRepository) {
        this.raceRepository = raceRepository;
        this.roundRepository = roundRepository;
        this.lapTimingService = lapTimingService;
        this.resultSnapshotRepository = resultSnapshotRepository;
        this.objectMapper = objectMapper;
        this.raceEntryRepository = raceEntryRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Persists the final race result snapshot on FINISHED transition.
     * Idempotent — upserts by raceId.
     */
    public void snapshot(long raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new EntityNotFoundException("Race not found: " + raceId));
        Round round = roundRepository.findById(race.getRoundId()).orElse(null);

        List<ResultSnapshotDto.ResultRow> positions;
        List<ResultSnapshotDto.PositionAtLap> lapHistory;

        Optional<LiveRaceState> stateOpt = lapTimingService.peek(raceId);
        if (stateOpt.isPresent()) {
            LiveRaceState state = stateOpt.get();
            List<LiveTimingRowDto> rows = state.calculatePositions();

            Map<Long, String[]> entryInfo = resolveEntryInfo(raceId);

            positions = new ArrayList<>();
            long raceStartMs = race.getStartedAt() != null ? race.getStartedAt().toEpochMilli()
                    : (rows.isEmpty() ? 0L : rows.get(0).lastPassingTimeMs());
            for (LiveTimingRowDto row : rows) {
                String[] info = entryInfo.getOrDefault(row.entryId(), new String[]{"Unknown", null});
                long totalTimeMs = (raceStartMs > 0 && row.lastPassingTimeMs() > raceStartMs)
                        ? row.lastPassingTimeMs() - raceStartMs : 0L;
                positions.add(new ResultSnapshotDto.ResultRow(
                        row.position(),
                        row.entryId(),
                        info[0],
                        info[1],
                        row.lapsCompleted(),
                        totalTimeMs,
                        row.bestLapMs(),
                        row.gapToLeaderMs(),
                        null  // carTags: not populated at snapshot time; enriched at read time in ResultSnapshotQuery
                ));
            }

            lapHistory = buildLapHistory(rows);
        } else {
            log.info("Race {} finished with no in-memory state — storing empty snapshot", raceId);
            positions = List.of();
            lapHistory = List.of();
        }

        ResultSnapshot snapshot = resultSnapshotRepository.findByRaceId(raceId)
                .orElseGet(ResultSnapshot::new);
        snapshot.setRaceId(raceId);
        snapshot.setFinishedAt(race.getFinishedAt() != null ? race.getFinishedAt() : Instant.now());
        snapshot.setCreatedAt(Instant.now());

        try {
            snapshot.setPositionsJson(objectMapper.writeValueAsString(positions));
            snapshot.setLapHistoryJson(objectMapper.writeValueAsString(lapHistory));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize result snapshot for race " + raceId, e);
        }

        resultSnapshotRepository.save(snapshot);
        lapTimingService.releaseState(raceId);
        log.info("Persisted result snapshot for race {}", raceId);
    }

    public void deleteByRaceId(long raceId) {
        resultSnapshotRepository.findByRaceId(raceId)
                .ifPresent(resultSnapshotRepository::delete);
        log.info("Deleted result snapshot for race {}", raceId);
    }

    private Map<Long, String[]> resolveEntryInfo(long raceId) {
        List<RaceEntry> raceEntries = raceEntryRepository.findByRaceIdOrderByGridPosition(raceId);
        return raceEntries.stream()
                .filter(re -> re.getEntryId() != 0L)
                .collect(Collectors.toMap(
                        RaceEntry::getEntryId,
                        re -> {
                            Optional<Entry> entry = entryRepository.findById(re.getEntryId());
                            if (entry.isEmpty()) return new String[]{"Unknown", null};
                            Optional<User> user = userRepository.findById(entry.get().getUserId());
                            String name = user.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown");
                            String carNum = re.getCarNumber() != null ? re.getCarNumber().toString() : null;
                            return new String[]{name, carNum};
                        },
                        (a, b) -> a  // keep first on duplicate real ID (defensive)
                ));
    }

    /**
     * Builds a simplified lap history from the final standings.
     * For each entry, records their final position at each lap they completed.
     */
    private List<ResultSnapshotDto.PositionAtLap> buildLapHistory(List<LiveTimingRowDto> rows) {
        List<ResultSnapshotDto.PositionAtLap> history = new ArrayList<>();
        int leaderLaps = rows.stream().mapToInt(LiveTimingRowDto::lapsCompleted).max().orElse(0);
        if (leaderLaps == 0) return history;

        for (int lap = 1; lap <= leaderLaps; lap++) {
            for (LiveTimingRowDto row : rows) {
                if (row.lapsCompleted() >= lap) {
                    history.add(new ResultSnapshotDto.PositionAtLap(lap, row.entryId(), row.position()));
                }
            }
        }
        return history;
    }
}
