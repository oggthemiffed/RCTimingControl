package dev.monkeypatch.rctiming.domain.race;

import dev.monkeypatch.rctiming.domain.event.IllegalStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RaceStateMachineServiceTest {

    private RaceStateMachineService service;

    @BeforeEach
    void setUp() {
        service = new RaceStateMachineService();
    }

    @Test
    void invalidTransition_pendingToFinished_throwsIllegalStateTransitionException() {
        Race race = new Race();
        race.setStatus(RaceStatus.PENDING);
        assertThatThrownBy(() -> service.transition(race, RaceStatus.FINISHED))
            .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void invalidTransition_finishedToAny_throwsIllegalStateTransitionException() {
        Race race = new Race();
        race.setStatus(RaceStatus.FINISHED);
        assertThatThrownBy(() -> service.transition(race, RaceStatus.PENDING))
            .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void validTransition_pendingToGrid_updatesStatus() {
        Race race = new Race();
        race.setStatus(RaceStatus.PENDING);
        service.transition(race, RaceStatus.GRID);
        assertThat(race.getStatus()).isEqualTo(RaceStatus.GRID);
    }

    @Test
    void validTransition_gridToRunning_updatesStatus() {
        Race race = new Race();
        race.setStatus(RaceStatus.GRID);
        service.transition(race, RaceStatus.RUNNING);
        assertThat(race.getStatus()).isEqualTo(RaceStatus.RUNNING);
    }
}
