package dev.monkeypatch.rctiming.livefeed;

import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.format.BumpUpConfig;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.format.PointsFinalsConfig;
import dev.monkeypatch.rctiming.domain.format.RaceFormatConfig;
import dev.monkeypatch.rctiming.domain.format.RaceFormatService;
import dev.monkeypatch.rctiming.domain.format.TimedRaceConfig;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Reads what the live feed needs to know about a race, fresh each time (#28). */
@Component
public class LiveFeedRaceLookup {

    private final RaceRepository raceRepository;
    private final RoundRepository roundRepository;
    private final EventRepository eventRepository;
    private final EventClassRepository eventClassRepository;
    private final RacingClassRepository racingClassRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceFormatService raceFormatService;

    public LiveFeedRaceLookup(RaceRepository raceRepository, RoundRepository roundRepository,
                              EventRepository eventRepository, EventClassRepository eventClassRepository,
                              RacingClassRepository racingClassRepository, RaceEntryRepository raceEntryRepository,
                              RaceFormatService raceFormatService) {
        this.raceRepository = raceRepository;
        this.roundRepository = roundRepository;
        this.eventRepository = eventRepository;
        this.eventClassRepository = eventClassRepository;
        this.racingClassRepository = racingClassRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.raceFormatService = raceFormatService;
    }

    /**
     * A race as the feed describes it.
     *
     * @param feedOn     whether its event has the live feed turned on
     * @param durationMs its length from the class's format, or null
     * @param carNumbers entry id to car number, for the cars that have one
     */
    public record RaceInfo(long raceId, String status, boolean feedOn, LiveFeedV1.Event event, String className,
                           String roundType, int roundNumber, int heatNumber, String finalLetter, Long durationMs,
                           Map<Long, Integer> carNumbers) {
    }

    @Transactional(readOnly = true)
    public Optional<RaceInfo> find(long raceId) {
        Race race = raceRepository.findById(raceId).orElse(null);
        if (race == null) {
            return Optional.empty();
        }
        Round round = roundRepository.findById(race.getRoundId()).orElse(null);
        Event event = round == null ? null : eventRepository.findById(round.getEventId()).orElse(null);
        if (event == null) {
            return Optional.empty();
        }
        EventClass eventClass = race.getEventClassId() == null ? null
                : eventClassRepository.findById(race.getEventClassId()).orElse(null);
        String className = eventClass == null || eventClass.getRacingClassId() == null ? null
                : racingClassRepository.findById(eventClass.getRacingClassId()).map(c -> c.getName()).orElse(null);
        Map<Long, Integer> carNumbers = new HashMap<>();
        for (RaceEntry raceEntry : raceEntryRepository.findByRaceIdOrderByGridPosition(raceId)) {
            if (raceEntry.getCarNumber() != null) {
                carNumbers.put(raceEntry.getEntryId(), raceEntry.getCarNumber());
            }
        }
        return Optional.of(new RaceInfo(
                raceId,
                race.getStatus().name(),
                event.isLiveFeedEnabled(),
                new LiveFeedV1.Event(event.getId(), event.getName(), String.valueOf(event.getEventDate())),
                className,
                round.getType().name(),
                round.getRoundNumber(),
                race.getHeatNumber(),
                race.getFinalLetter(),
                durationMs(eventClass, round.getType()),
                carNumbers));
    }

    /** The race length its class's format gives, by round type. */
    private Long durationMs(EventClass eventClass, RoundType roundType) {
        if (eventClass == null || eventClass.getConfigSnapshot() == null) {
            return null;
        }
        RaceFormatConfig config = raceFormatService.getEffectiveConfig(eventClass);
        int minutes = switch (config) {
            case TimedRaceConfig timed -> timed.durationMinutes();
            case BumpUpConfig bumpUp -> bumpUp.heatDurationMinutes();
            case PointsFinalsConfig points -> roundType == RoundType.FINAL
                    ? points.finalDurationMinutes() : points.heatDurationMinutes();
        };
        return minutes > 0 ? minutes * 60_000L : null;
    }
}
