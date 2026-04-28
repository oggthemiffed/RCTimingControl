package dev.monkeypatch.rctiming.audio;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled("Wave 1 — implement in Plan 03")
class PiperTtsClientTest {
    @Test void synthesize_validText_returnsWavBytes() { fail("Plan 03"); }
    @Test void synthesize_emptyText_throwsIllegalArgumentException() { fail("Plan 03"); }
    @Test void synthesize_piperUnavailable_throwsTtsUnavailableException() { fail("Plan 03"); }
    @Test void synthesize_correctVoiceName_sendsInWyomingEvent() { fail("Plan 03"); }
    @Test void assembleWav_validPcm_returns44ByteHeaderPlusPcm() { fail("Plan 03"); }
    @Test void describe_returnsList_ofAvailableVoices() { fail("Plan 03"); }
}
