package dev.monkeypatch.rctiming.practice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled("Wave 2 — implement in Plan 05")
class PracticeTimingServiceTest {
    @Test void onLapPassingEvent_runningSession_recordsLap() { fail("Plan 05"); }
    @Test void onLapPassingEvent_noSession_ignored() { fail("Plan 05"); }
    @Test void onLapPassingEvent_unknownTransponder_recordsWithNullUser() { fail("Plan 05"); }
    @Test void getSnapshot_returnsCurrentPositions() { fail("Plan 05"); }
    @Test void broadcastsViaStompAfterEachPassing() { fail("Plan 05"); }
}
