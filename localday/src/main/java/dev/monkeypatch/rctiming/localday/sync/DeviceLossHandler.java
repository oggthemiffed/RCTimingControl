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
     *
     * <p>This checks the bare status code, not a distinguishing error body — safe today only
     * because the only caller is {@link SnapshotPushService}, on a failure from
     * {@link SnapshotSyncClient#pushSnapshot}, and the plan's KTD4 sequence diagram reserves
     * {@code 409} on <em>that specific endpoint</em> exclusively for "superseded" (its only other
     * documented outcome is {@code 200}, including the idempotent-replay case). {@code
     * CloudRequestException} itself is shared more broadly (day-lifecycle open/close also use
     * {@code 409} for an unrelated "lifecycle conflict") — that's fine as long as nothing routes
     * one of *those* exceptions through this method. If U14's real snapshot-ingest endpoint ever
     * needs to return {@code 409} for a second reason, this check must be narrowed then (e.g. by
     * having {@code SnapshotSyncClient} read a distinguishing error code from the response body)
     * rather than left as a bare status check.
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
