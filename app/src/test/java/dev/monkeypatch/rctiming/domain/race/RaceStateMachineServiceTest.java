package dev.monkeypatch.rctiming.domain.race;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Disabled("wave 0 stub — enabled once RaceStateMachineService exists in plan 02")
@ExtendWith(MockitoExtension.class)
class RaceStateMachineServiceTest {

    // private RaceStateMachineService service; // uncomment in plan 02 once production class exists

    @Test
    void invalidTransition_pendingToFinished_throwsIllegalStateTransitionException() {
        // TODO: plan 02 implements assertions
    }

    @Test
    void invalidTransition_finishedToAny_throwsIllegalStateTransitionException() {
        // TODO: plan 02 implements assertions
    }

    @Test
    void validTransition_pendingToGrid_updatesStatus() {
        // TODO: plan 02 implements assertions
    }

    @Test
    void validTransition_gridToRunning_updatesStatus() {
        // TODO: plan 02 implements assertions
    }
}
