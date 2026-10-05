package dev.monkeypatch.rctiming.timing;

import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.format.RaceFormatService;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.RaceStatusChangedEvent;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import dev.monkeypatch.rctiming.timing.dto.RaceClockDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RaceClockServiceTest {

    private static final long RACE_ID = 7;

    private final RaceRepository raceRepository = mock(RaceRepository.class);
    private final RoundRepository roundRepository = mock(RoundRepository.class);
    private final EventClassRepository eventClassRepository = mock(EventClassRepository.class);
    private final RaceFormatService raceFormatService = mock(RaceFormatService.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-18T14:00:00Z"));
    private final RaceClockService service = new RaceClockService(raceRepository, roundRepository,
            eventClassRepository, raceFormatService, clock);
    private final Race race = new Race();

    @BeforeEach
    void setUp() {
        race.setId(RACE_ID);
        race.setRoundId(3L);
        race.setEventClassId(4L);
        Round round = new Round();
        round.setType(RoundType.QUALIFIER);
        EventClass eventClass = new EventClass();
        when(raceRepository.findById(RACE_ID)).thenReturn(Optional.of(race));
        when(roundRepository.findById(3L)).thenReturn(Optional.of(round));
        when(eventClassRepository.findById(4L)).thenReturn(Optional.of(eventClass));
        when(raceFormatService.raceDurationMs(any(), eq(RoundType.QUALIFIER))).thenReturn(300_000L);
    }

    @Test
    void countsWhileRunningAndPausesWhileStopped() {
        status(RaceStatus.GRID);
        status(RaceStatus.RUNNING);
        clock.advance(Duration.ofSeconds(30));
        status(RaceStatus.STOPPED);
        clock.advance(Duration.ofSeconds(20));
        assertThat(service.elapsedMs(RACE_ID)).isEqualTo(30_000);
        assertThat(service.running(RACE_ID)).isFalse();

        status(RaceStatus.RUNNING);
        clock.advance(Duration.ofSeconds(10));

        RaceClockDto now = service.clock(RACE_ID).orElseThrow();
        assertThat(now.elapsedMs()).isEqualTo(40_000);
        assertThat(now.running()).isTrue();
        assertThat(now.durationMs()).isEqualTo(300_000);
        assertThat(now.remainingMs()).isEqualTo(260_000);
    }

    @Test
    void theGridStartsTheClockAgain() {
        status(RaceStatus.GRID);
        status(RaceStatus.RUNNING);
        clock.advance(Duration.ofSeconds(30));
        status(RaceStatus.FINISHED);

        status(RaceStatus.GRID);

        assertThat(service.clock(RACE_ID).orElseThrow().elapsedMs()).isZero();
    }

    @Test
    void aRestartedRaceShowsNoTime() {
        status(RaceStatus.GRID);
        status(RaceStatus.RUNNING);
        clock.advance(Duration.ofSeconds(30));
        // A restart sends the race back to pending without a status event
        race.setStatus(RaceStatus.PENDING);

        RaceClockDto pending = service.clock(RACE_ID).orElseThrow();
        assertThat(pending.elapsedMs()).isZero();
        assertThat(pending.running()).isFalse();
    }

    @Test
    void aRaceNotSeenSinceTheAppStartedIsTimedFromItsStartAndFinish() {
        race.setStatus(RaceStatus.RUNNING);
        race.setStartedAt(clock.instant().minusSeconds(75));
        assertThat(service.clock(RACE_ID).orElseThrow().elapsedMs()).isEqualTo(75_000);

        race.setStatus(RaceStatus.FINISHED);
        race.setFinishedAt(clock.instant().minusSeconds(5));
        RaceClockDto finished = service.clock(RACE_ID).orElseThrow();
        assertThat(finished.elapsedMs()).isEqualTo(70_000);
        assertThat(finished.running()).isFalse();
    }

    @Test
    void anUnknownRaceHasNoClock() {
        assertThat(service.clock(99)).isEmpty();
        assertThat(service.elapsedMs(99)).isZero();
    }

    private void status(RaceStatus newStatus) {
        race.setStatus(newStatus);
        service.onRaceStatusChanged(new RaceStatusChangedEvent(this, RACE_ID, newStatus));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
