package dev.monkeypatch.rctiming.timing;

import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.format.RaceFormatService;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.RaceStatusChangedEvent;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.timing.dto.RaceClockDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps each race's clock from its status changes: it resets when the race is called to the grid, counts while it
 * runs and pauses while it is stopped. The live feed (#28) and the streaming overlay (#29) read it.
 *
 * <p>Clocks are kept in memory only. For a race the app hasn't seen change since it started, {@link #clock} works
 * the time out from the race's start and finish times instead, which can't allow for stops.
 */
@Component
public class RaceClockService {

    private final RaceRepository raceRepository;
    private final RoundRepository roundRepository;
    private final EventClassRepository eventClassRepository;
    private final RaceFormatService raceFormatService;
    private final Clock clock;
    private final Map<Long, RaceClock> clocks = new ConcurrentHashMap<>();

    @Autowired
    public RaceClockService(RaceRepository raceRepository, RoundRepository roundRepository,
                            EventClassRepository eventClassRepository, RaceFormatService raceFormatService) {
        this(raceRepository, roundRepository, eventClassRepository, raceFormatService, Clock.systemUTC());
    }

    public RaceClockService(RaceRepository raceRepository, RoundRepository roundRepository,
                            EventClassRepository eventClassRepository, RaceFormatService raceFormatService,
                            Clock clock) {
        this.raceRepository = raceRepository;
        this.roundRepository = roundRepository;
        this.eventClassRepository = eventClassRepository;
        this.raceFormatService = raceFormatService;
        this.clock = clock;
    }

    @EventListener
    public void onRaceStatusChanged(RaceStatusChangedEvent event) {
        Instant now = clock.instant();
        long raceId = event.getRaceId();
        switch (event.getNewStatus()) {
            case GRID -> clocks.put(raceId, new RaceClock());
            case RUNNING -> clocks.computeIfAbsent(raceId, id -> new RaceClock()).start(now);
            case STOPPED, FINISHED -> clocks.computeIfAbsent(raceId, id -> new RaceClock()).stop(now);
            default -> { }
        }
    }

    /** Race time so far, not counting time stopped; 0 for a race whose clock isn't kept. */
    public long elapsedMs(long raceId) {
        RaceClock raceClock = clocks.get(raceId);
        return raceClock == null ? 0 : raceClock.elapsedMs(clock.instant());
    }

    /** Whether the race's clock is counting now. */
    public boolean running(long raceId) {
        RaceClock raceClock = clocks.get(raceId);
        return raceClock != null && raceClock.running();
    }

    /** The race's clock with its length, or empty for an unknown race. */
    @Transactional(readOnly = true)
    public Optional<RaceClockDto> clock(long raceId) {
        return raceRepository.findById(raceId).map(this::clockOf);
    }

    private RaceClockDto clockOf(Race race) {
        Instant now = clock.instant();
        RaceStatus status = race.getStatus();
        RaceClock kept = clocks.get(race.getId());
        long elapsed;
        boolean running;
        if (status == RaceStatus.PENDING) {
            // A restart puts a race back to pending without a status event, so a kept clock may be stale
            elapsed = 0;
            running = false;
        } else if (kept != null) {
            elapsed = kept.elapsedMs(now);
            running = kept.running();
        } else {
            running = status == RaceStatus.RUNNING;
            Instant until = status == RaceStatus.FINISHED ? race.getFinishedAt() : running ? now : null;
            elapsed = race.getStartedAt() == null || until == null ? 0
                    : Math.max(0, Duration.between(race.getStartedAt(), until).toMillis());
        }
        Long duration = durationMs(race);
        Long remaining = duration == null ? null : Math.max(0, duration - elapsed);
        return new RaceClockDto(race.getId(), status.name(), elapsed, duration, remaining, running);
    }

    private Long durationMs(Race race) {
        if (race.getEventClassId() == null) {
            return null;
        }
        return roundRepository.findById(race.getRoundId())
                .flatMap(round -> eventClassRepository.findById(race.getEventClassId())
                        .map(eventClass -> raceFormatService.raceDurationMs(eventClass, round.getType())))
                .orElse(null);
    }

    /** Race time so far, not counting time stopped. */
    private static final class RaceClock {
        private long accumulatedMs;
        private Instant runningSince;

        synchronized void start(Instant now) {
            if (runningSince == null) {
                runningSince = now;
            }
        }

        synchronized void stop(Instant now) {
            if (runningSince != null) {
                accumulatedMs += Duration.between(runningSince, now).toMillis();
                runningSince = null;
            }
        }

        synchronized long elapsedMs(Instant now) {
            return accumulatedMs + (runningSince == null ? 0 : Duration.between(runningSince, now).toMillis());
        }

        synchronized boolean running() {
            return runningSince != null;
        }
    }
}
