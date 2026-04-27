package dev.monkeypatch.rctiming.forwarder.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Emits synthetic RC-4 PASSING records for a configured list of transponder IDs.
 *
 * <p>Each transponder emits a PASSING frame every {@code intervalMs} milliseconds,
 * round-robined. A STATUS (heartbeat) frame is emitted every 5 seconds. Sequence
 * numbers are monotonically increasing across both PASSING and STATUS records,
 * matching real decoder behaviour.
 */
public class GenerativeMode {

    private static final Logger log = LoggerFactory.getLogger(GenerativeMode.class);

    private GenerativeMode() {}

    /**
     * Start emitting RC-4 frames. Blocks until the thread is interrupted or the stream closes.
     *
     * @param out          client output stream
     * @param transponders list of transponder ID strings to simulate
     * @param intervalMs   milliseconds between PASSING frames per transponder
     * @throws IOException if the stream write fails
     */
    public static void run(OutputStream out, List<String> transponders, long intervalMs) throws IOException {
        if (transponders.isEmpty()) {
            log.warn("[SIMULATOR] GenerativeMode started with no transponders — emitting STATUS only");
        }
        long   seqNum        = 0;
        int    txpIdx        = 0;
        double timeSinceStart = 1.0;
        long   lastStatusMs  = System.currentTimeMillis();
        long   lastPassingMs = System.currentTimeMillis();

        // Poll interval: short enough that STATUS is never more than ~1 s late,
        // regardless of how large intervalMs is.
        final long TICK_MS = Math.min(intervalMs, 1_000);

        while (!Thread.currentThread().isInterrupted()) {
            long nowMs = System.currentTimeMillis();

            // Emit STATUS heartbeat every 5 s — always fires on schedule
            if (nowMs - lastStatusMs >= 5_000) {
                String status = String.format("#\t20\t%d\t72\t0\txDEAD", seqNum++);
                emit(out, status);
                lastStatusMs = nowMs;
                log.debug("[SIMULATOR] STATUS emitted: {}", status);
            }

            // Emit PASSING record when the configured interval has elapsed
            if (!transponders.isEmpty() && nowMs - lastPassingMs >= intervalMs) {
                String transponder = transponders.get(txpIdx % transponders.size());
                String passing = String.format("@\t20\t%d\t%s\t%.3f\t300\t130\t2\txDEAD",
                                               seqNum++, transponder, timeSinceStart);
                emit(out, passing);
                log.debug("[SIMULATOR] PASSING emitted: {}", passing);
                timeSinceStart += intervalMs / 1000.0;
                txpIdx++;
                lastPassingMs = nowMs;
            }

            sleep(TICK_MS);
        }
    }

    private static void emit(OutputStream out, String line) throws IOException {
        out.write(0x01);                                          // SOH
        out.write(line.getBytes(StandardCharsets.US_ASCII));
        out.write('\r');
        out.write('\n');
        out.flush();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
