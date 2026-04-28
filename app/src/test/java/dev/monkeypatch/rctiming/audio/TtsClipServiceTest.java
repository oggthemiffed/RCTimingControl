package dev.monkeypatch.rctiming.audio;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled("Wave 1 — implement in Plan 03")
class TtsClipServiceTest {
    @Test void generateNameClip_storesInMinIO_returnsUrl() { fail("Plan 03"); }
    @Test void getNameClipUrl_cachedClipExists_returnsExistingUrl() { fail("Plan 03"); }
    @Test void getNameClipUrl_voiceChanged_regeneratesClip() { fail("Plan 03"); }
    @Test void generateCountdownClip_storesWithCorrectKey() { fail("Plan 03"); }
    @Test void generateCarNumberClip_storesWithCorrectKey() { fail("Plan 03"); }
}
