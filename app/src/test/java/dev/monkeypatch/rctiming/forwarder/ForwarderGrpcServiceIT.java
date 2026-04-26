package dev.monkeypatch.rctiming.forwarder;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Wave 0 stub — implementation in Plan 04. */
@Disabled("Wave 2 — Plan 04")
class ForwarderGrpcServiceIT {

    @Test
    void streamWithValidTokenAccepted() {
        org.junit.jupiter.api.Assertions.fail("Wave 1 — implement in Plan 04");
    }

    @Test
    void streamWithMissingTokenRejectedUnauthenticated() {
        org.junit.jupiter.api.Assertions.fail("Wave 1 — implement in Plan 04");
    }

    @Test
    void streamWithRevokedTokenRejectedUnauthenticated() {
        org.junit.jupiter.api.Assertions.fail("Wave 1 — implement in Plan 04");
    }

    @Test
    void lapPassingMessagePublishedAsApplicationEvent() {
        org.junit.jupiter.api.Assertions.fail("Wave 1 — implement in Plan 04");
    }

    @Test
    void lapPassingDroppedWhenNoRunningRace() {
        org.junit.jupiter.api.Assertions.fail("Wave 1 — implement in Plan 04");
    }

    @Test
    void lapPassingResolvesRaceIdFromCurrentlyRunningRace() {
        org.junit.jupiter.api.Assertions.fail("Wave 1 — implement in Plan 04");
    }
}
