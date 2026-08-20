package dev.monkeypatch.rctiming.localday.boards;

import dev.monkeypatch.rctiming.localday.boards.dto.NowNextDto;
import dev.monkeypatch.rctiming.localday.boards.dto.RaceResultRowDto;
import dev.monkeypatch.rctiming.localday.boards.dto.ResultsDto;
import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import dev.monkeypatch.rctiming.localday.race.RaceResultEntry;
import dev.monkeypatch.rctiming.localday.race.RaceResultEntryRepository;
import dev.monkeypatch.rctiming.localday.race.RaceState;
import dev.monkeypatch.rctiming.localday.race.dto.ScheduleEntryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Anonymous, read-only spectator board endpoints for the local race-day program (U9) — current
 * heat, next-up schedule, and last-completed results for venue monitors with no login.
 *
 * <p>This is the deliberate counterpart to {@link dev.monkeypatch.rctiming.localday.race.RaceControlController},
 * whose class Javadoc points forward to exactly this API surface: "the anonymous read-only
 * board/attendee view is a distinct, not-yet-built API surface for a later unit". Per R9,
 * officials-only write actions and the anonymous board/attendee view are deliberately split —
 * every endpoint here is read-only and requires no session, permitted explicitly in
 * {@code LocalSecurityConfig} rather than falling under its default-deny
 * {@code anyRequest().authenticated()} rule.
 */
@RestController
@RequestMapping("/api/v1/boards")
public class BoardController {

    private final CachedScheduleEntryRepository cachedScheduleEntryRepository;
    private final RaceResultEntryRepository raceResultEntryRepository;
    private final CachedEntryRepository cachedEntryRepository;

    public BoardController(CachedScheduleEntryRepository cachedScheduleEntryRepository,
                            RaceResultEntryRepository raceResultEntryRepository,
                            CachedEntryRepository cachedEntryRepository) {
        this.cachedScheduleEntryRepository = cachedScheduleEntryRepository;
        this.raceResultEntryRepository = raceResultEntryRepository;
        this.cachedEntryRepository = cachedEntryRepository;
    }

    @GetMapping("/now-next")
    public NowNextDto nowNext() {
        Optional<CachedScheduleEntry> current = cachedScheduleEntryRepository.findFirstByStatus(RaceState.RUNNING)
                .or(() -> cachedScheduleEntryRepository.findFirstByStatus(RaceState.STOPPED));

        Optional<CachedScheduleEntry> next = cachedScheduleEntryRepository
                .findFirstByStatusInOrderBySequenceAsc(List.of(RaceState.PENDING, RaceState.GRID));

        Optional<CachedScheduleEntry> lastCompleted = findLastCompletedRace();

        return new NowNextDto(
                current.map(ScheduleEntryDto::from).orElse(null),
                next.map(ScheduleEntryDto::from).orElse(null),
                lastCompleted.map(ScheduleEntryDto::from).orElse(null));
    }

    @GetMapping("/results")
    public ResultsDto results() {
        Optional<CachedScheduleEntry> lastCompleted = findLastCompletedRace();
        if (lastCompleted.isEmpty()) {
            return new ResultsDto(null, List.of());
        }
        CachedScheduleEntry race = lastCompleted.get();

        List<RaceResultEntry> resultRows = raceResultEntryRepository.findByRaceIdOrderByPositionAsc(race.getId());

        List<Long> entryIds = resultRows.stream()
                .map(RaceResultEntry::getEntryId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, CachedEntry> entriesById = cachedEntryRepository.findAllById(entryIds).stream()
                .collect(java.util.stream.Collectors.toMap(CachedEntry::getId, e -> e));

        List<RaceResultRowDto> rows = resultRows.stream()
                .map(r -> {
                    CachedEntry entry = entriesById.get(r.getEntryId());
                    return new RaceResultRowDto(
                            r.getEntryId(),
                            entry == null ? null : entry.getRacerName(),
                            entry == null ? null : entry.getTransponderNumber(),
                            r.getPosition(),
                            r.getLapsCompleted(),
                            r.getBestLapMs());
                })
                .toList();

        return new ResultsDto(ScheduleEntryDto.from(race), rows);
    }

    /** The most recently finished race, shared by both endpoints so they can't disagree. */
    private Optional<CachedScheduleEntry> findLastCompletedRace() {
        return cachedScheduleEntryRepository.findFirstByStatusOrderByFinishedAtDesc(RaceState.FINISHED);
    }
}
