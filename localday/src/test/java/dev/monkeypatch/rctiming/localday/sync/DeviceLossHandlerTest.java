package dev.monkeypatch.rctiming.localday.sync;

import dev.monkeypatch.rctiming.localday.daylifecycle.CloudRequestException;
import dev.monkeypatch.rctiming.localday.daylifecycle.CloudUnreachableException;
import dev.monkeypatch.rctiming.localday.daylifecycle.DayLifecycleState;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link DeviceLossHandler} in isolation — no Spring context, no dependency
 * on a real cloud endpoint (KTD4's 409 rejection is simulated directly via
 * {@link CloudRequestException}), per U12's explicit "runnable independent of a real cloud
 * endpoint" test scenario.
 */
class DeviceLossHandlerTest {

    private final DeviceLossHandler handler = new DeviceLossHandler();

    // --- isSuperseded: recognizing the rejection ---

    @Test
    void isSuperseded_409_true() {
        assertThat(handler.isSuperseded(new CloudRequestException(409, "superseded", new RuntimeException())))
                .isTrue();
    }

    @Test
    void isSuperseded_otherStatusCodes_false() {
        assertThat(handler.isSuperseded(new CloudRequestException(500, "server error", new RuntimeException())))
                .isFalse();
        assertThat(handler.isSuperseded(new CloudRequestException(401, "unauthorized", new RuntimeException())))
                .isFalse();
        assertThat(handler.isSuperseded(new CloudRequestException(404, "not found", new RuntimeException())))
                .isFalse();
    }

    @Test
    void isSuperseded_unreachableCloud_false() {
        // A connectivity failure is not a rejection at all — treated as ordinary backoff-worthy
        // failure by SnapshotPushService, never as supersession.
        assertThat(handler.isSuperseded(new CloudUnreachableException("down", new RuntimeException())))
                .isFalse();
    }

    // --- markSuperseded: transitioning local sync state ---

    @Test
    void markSuperseded_setsSupersededAndSupersededAt() {
        DayLifecycleState state = new DayLifecycleState();
        Instant at = Instant.parse("2026-08-24T12:00:00Z");

        handler.markSuperseded(state, at);

        assertThat(state.isSuperseded()).isTrue();
        assertThat(state.getSupersededAt()).isEqualTo(at);
    }

    @Test
    void markSuperseded_alreadySuperseded_doesNotOverwriteOriginalTimestamp() {
        DayLifecycleState state = new DayLifecycleState();
        Instant firstRejection = Instant.parse("2026-08-24T12:00:00Z");
        handler.markSuperseded(state, firstRejection);

        Instant laterRejection = Instant.parse("2026-08-24T12:05:00Z");
        handler.markSuperseded(state, laterRejection);

        assertThat(state.getSupersededAt()).isEqualTo(firstRejection);
    }
}
