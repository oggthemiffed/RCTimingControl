package dev.monkeypatch.rctiming.practice;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled("Wave 2 — implement in Plan 05")
class PracticeSessionControllerIT extends AbstractIntegrationTest {
    @Test void createSession_validRequest_returns201() { fail("Plan 05"); }
    @Test void createSession_withEventLink_associatesEvent() { fail("Plan 05"); }
    @Test void startSession_idleSession_transitionsToRunning() { fail("Plan 05"); }
    @Test void stopSession_runningSession_transitionsToStopped() { fail("Plan 05"); }
    @Test void startSession_alreadyRunning_returns409() { fail("Plan 05"); }
    @Test void getResults_stoppedSession_returnsFinalSnapshot() { fail("Plan 05"); }
}
