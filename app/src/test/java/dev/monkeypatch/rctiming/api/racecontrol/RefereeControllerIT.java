package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

@Disabled("wave 0 stub — enabled in plan 06")
public class RefereeControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // TODO: plan 06 sets up test user and login
    }

    @Test
    void raiseIncident_createsRecordLinkedToRaceAndEntry() {
        // TODO: plan 06 implements assertions — OFFICIAL-03
    }

    @Test
    void applyLapPenalty_recalculatesPositionsImmediately() {
        // TODO: plan 06 implements assertions — OFFICIAL-04
    }

    @Test
    void applyTimePenalty_recordedAgainstRaceResult() {
        // TODO: plan 06 implements assertions — OFFICIAL-04
    }

    @Test
    void proximityAlertLogic_computedFromLiveTimingStream() {
        // TODO: plan 06 implements assertions — OFFICIAL-01 (manual marker)
    }

    @Test
    void backmarkerDetection_flagsLappedCars() {
        // TODO: plan 06 implements assertions — OFFICIAL-02 (manual marker)
    }
}
