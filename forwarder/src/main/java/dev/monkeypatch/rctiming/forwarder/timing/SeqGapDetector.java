package dev.monkeypatch.rctiming.forwarder.timing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors RC-4 PASSING sequence numbers for gaps (TIMING-07).
 *
 * <p>The RC-4 text protocol has <strong>no RESEND mechanism</strong>. Gaps are informational
 * only and are logged at INFO level. Callers should not treat a gap as a fatal error.
 *
 * <p>Returns the gap size from {@link #observe(int)} so callers can also react if needed
 * (e.g. the integration test verifies the return value), without throwing an exception.
 */
public class SeqGapDetector {

    private static final Logger log = LoggerFactory.getLogger(SeqGapDetector.class);

    private Integer lastSeqNum;

    /**
     * Observe a PASSING sequence number and detect gaps.
     *
     * @param seqNum the seq_num field from the latest PASSING record
     * @return number of missed records (0 if first call or contiguous; positive if gap; 0 if out-of-order)
     */
    public int observe(int seqNum) {
        if (lastSeqNum == null) {
            lastSeqNum = seqNum;
            return 0;
        }
        int gap = seqNum - lastSeqNum - 1;
        lastSeqNum = seqNum;
        if (gap > 0) {
            log.info("RC-4 seq_num gap detected: missed {} record(s) between {} and {}",
                     gap, seqNum - gap - 1, seqNum);
            return gap;
        }
        return 0;
    }
}
