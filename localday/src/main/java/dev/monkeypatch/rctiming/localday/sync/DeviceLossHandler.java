package dev.monkeypatch.rctiming.localday.sync;

import dev.monkeypatch.rctiming.localday.daylifecycle.CloudRequestException;
import dev.monkeypatch.rctiming.localday.daylifecycle.DayLifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Handles the local side of device-loss recovery (F5, R13, R16, R17) — recognizing a {@code 409}
 * snapshot-push rejection as KTD4's "this instance's generation has been superseded" outcome, as
 * opposed to an ordinary connectivity/server failure that {@link SnapshotPushService} should just
 * back off and retry.
 *
 * <p>Deliberately does not talk to the cloud itself: a replacement instance opening the same day
 * (via the existing U10 day-open flow) already claims a higher generation with no special-case
 * logic needed on that side, and the cloud's own generation comparison (KTD4, U14) is what
 * rejects this instance's later push attempts — this class only recognizes that rejection and
 * marks it, once, so {@link SnapshotPushService} stops retrying forever and the local UI can
 * surface a clear "this device has been superseded" state instead of silently discarding data.
 */
@Component
public class DeviceLossHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceLossHandler.class);

    /**
     * True if {@code failure} is the cloud's KTD4 generation-fencing rejection (a reachable
     * cloud returning {@code 409}), not an unreachable-cloud or other-status failure that should
     * be retried normally.
     */
    public boolean isSuperseded(RuntimeException failure) {
        return failure instanceof CloudRequestException cloudRequestException
                && cloudRequestException.getStatusCode() == 409;
    }

    /**
     * Marks {@code state} permanently superseded. Idempotent — a repeat call (e.g. a second
     * rejected push before the caller stops attempting) does not overwrite the original
     * {@code supersededAt}. Does not persist {@code state} itself; the caller (already holding
     * and about to save it) owns that.
     */
    public void markSuperseded(DayLifecycleState state, Instant at) {
        if (state.isSuperseded()) {
            return;
        }
        log.warn("This instance's snapshot was rejected as superseded (device-loss declared "
                + "and a replacement instance has taken over) — halting automatic sync.");
        state.setSuperseded(true);
        state.setSupersededAt(at);
    }
}
