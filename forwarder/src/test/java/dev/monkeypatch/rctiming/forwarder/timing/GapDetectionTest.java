package dev.monkeypatch.rctiming.forwarder.timing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GapDetectionTest {

    @Test
    void logsGapWhenSeqNumSkips() {
        SeqGapDetector detector = new SeqGapDetector();
        assertThat(detector.observe(1)).isEqualTo(0); // first call: no gap
        assertThat(detector.observe(2)).isEqualTo(0); // contiguous: no gap
        assertThat(detector.observe(5)).isEqualTo(2); // gap: missed seqNums 3 and 4
    }

    @Test
    void noGapForContiguousSeqNums() {
        SeqGapDetector detector = new SeqGapDetector();
        detector.observe(1);
        int gap = detector.observe(2);
        assertThat(gap).isEqualTo(0);
    }

    @Test
    void noResendInRc4ProtocolGapsLoggedOnly() {
        // RC-4 has no RESEND mechanism; gap detection is informational only.
        // Verify that observe() returns 0 for an out-of-order (lower) seqNum after a gap.
        SeqGapDetector detector = new SeqGapDetector();
        detector.observe(1);
        detector.observe(2);
        detector.observe(5); // gap of 2
        int result = detector.observe(3); // lower seqNum: out-of-order, return 0
        assertThat(result).isEqualTo(0);
    }
}
