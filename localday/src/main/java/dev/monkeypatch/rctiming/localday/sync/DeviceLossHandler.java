package dev.monkeypatch.rctiming.localday.sync;

import dev.monkeypatch.rctiming.localday.daylifecycle.CloudRequestException;
import dev.monkeypatch.rctiming.localday.daylifecycle.DayLifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Handles the local side of device-loss recovery (F5, R13, R16, R17) — recognizing a snapshot-
 * push rejection that means this instance has been superseded, as opposed to an ordinary
 * connectivity/server failure that {@link SnapshotPushService} should just back off and retry.
 *
 * <p>U14's real cloud implementation surfaces this two ways, both terminal for this instance:
 * {@code 401} when {@code DeviceLossController} has invalidated this instance's sync secret
 * (KTD9) — the primary, immediate signal, since a declaration invalidates the secret before any
 * replacement instance has necessarily claimed a new generation — and {@code 409} when this
 * instance's generation is compared and found stale (KTD4) — the "backstop" KTD9's own text
 * describes for the case where the secret hasn't been invalidated (e.g. it hasn't propagated
 * yet, or a future declaration path skips it). Both must halt automatic retries the same way:
 * neither condition can resolve itself without a person acting (a replacement instance opening
 * with a fresh secret and generation), so retrying is pure waste, and the local UI needs to
 * surface the same "this device has been superseded" state regardless of which one fired first.
 *
 * <p>Deliberately does not talk to the cloud itself: a replacement instance opening the same day
 * (via the existing U10 day-open flow) already claims a higher generation with no special-case
 * logic needed on that side, and the cloud's own auth/generation checks (KTD9/KTD4, U14) are what
 * reject this instance's later push attempts — this class only recognizes that rejection and
 * marks it, once, so {@link SnapshotPushService} stops retrying forever and the local UI can
 * surface a clear "this device has been superseded" state instead of silently discarding data.
 */
@Component
public class DeviceLossHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceLossHandler.class);

    /**
     * True if {@code failure} is one of the cloud's two terminal-for-this-instance rejections —
     * {@code 401} (KTD9: this instance's sync secret has been invalidated by a device-loss
     * declaration) or {@code 409} (KTD4: this instance's generation has been superseded) — not
     * an unreachable-cloud or other-status failure that should be retried normally.
     *
     * <p>This checks the bare status code, not a distinguishing error body — safe today only
     * because the only caller is {@link SnapshotPushService}, on a failure from
     * {@link SnapshotSyncClient#pushSnapshot}, and the plan's KTD4/KTD9 design reserves both
     * {@code 401} and {@code 409} on <em>that specific endpoint</em> exclusively for these two
     * terminal outcomes (its only other documented outcome is {@code 200}, including the
     * idempotent-replay case). {@code CloudRequestException} itself is shared more broadly
     * (day-lifecycle open/close also use {@code 409} for an unrelated "lifecycle conflict") —
     * that's fine as long as nothing routes one of *those* exceptions through this method. If a
     * future change gives the snapshot-ingest endpoint a second reason to return either status,
     * this check must be narrowed then (e.g. by having {@code SnapshotSyncClient} read a
     * distinguishing error code from the response body) rather than left as a bare status check.
     */
    public boolean isSuperseded(RuntimeException failure) {
        if (!(failure instanceof CloudRequestException cloudRequestException)) {
            return false;
        }
        int status = cloudRequestException.getStatusCode();
        return status == 401 || status == 409;
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
