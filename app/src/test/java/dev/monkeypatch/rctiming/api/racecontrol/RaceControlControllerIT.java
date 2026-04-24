package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

@Disabled("wave 0 stub — enabled across plans 05 and 06 as production endpoints arrive")
public class RaceControlControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // TODO: plan 05/06 sets up test user and login
    }

    @Test
    void callGrid_returnsOkAndTransitionsToGrid() {
        // TODO: plan 05 implements assertions — CTRL-01
    }

    @Test
    void startRace_returnsOkAndTransitionsToRunning() {
        // TODO: plan 05 implements assertions — CTRL-01
    }

    @Test
    void conflictingTransitionFromSecondSession_returns409() {
        // TODO: plan 05 implements assertions — CTRL-05
    }

    @Test
    void marshalAdjustment_persistsAllAuditFields() {
        // TODO: plan 05 implements assertions — CTRL-03
    }

    @Test
    void unknownTransponderLink_createsRecord() {
        // TODO: plan 05 implements assertions — CTRL-06
    }

    @Test
    void abandonRace_savesResultSnapshotAndReturnsFinished() {
        // TODO: plan 06 implements assertions — CTRL-08
    }

    @Test
    void getPrintResults_returns200WithLapHistory() {
        // TODO: plan 06 implements assertions — CTRL-04
    }

    @Test
    void skipToLaterRace_overridesAutoAdvance() {
        // TODO: plan 06 implements assertions — CTRL-09
    }
}
