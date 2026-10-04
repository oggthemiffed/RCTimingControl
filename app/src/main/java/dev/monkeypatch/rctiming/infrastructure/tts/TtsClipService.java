package dev.monkeypatch.rctiming.infrastructure.tts;

import dev.monkeypatch.rctiming.infrastructure.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Generates TTS announcement clips, storing them via ObjectStorageService.
 * <p>
 * Key conventions:
 * <ul>
 *   <li>{@code audio/race/{raceId}/countdown-{seconds}-{voiceId}.wav} — countdown clip</li>
 *   <li>{@code audio/race/{raceId}/car-{carNumber}-{voiceId}.wav} — car number stagger clip</li>
 *   <li>{@code audio/race/{raceId}/finish-{competitorId}-{voiceId}.wav} — finish announcement clip</li>
 * </ul>
 * When Piper is unavailable, methods log a warning and return {@code null} (graceful degradation).
 */
@Service
public class TtsClipService {

    private static final Logger log = LoggerFactory.getLogger(TtsClipService.class);
    private static final String CONTENT_TYPE_WAV = "audio/wav";

    private final PiperTtsClient piperClient;
    private final ObjectStorageService storageService;
    private final TtsProperties properties;

    public TtsClipService(PiperTtsClient piperClient,
                          ObjectStorageService storageService,
                          TtsProperties properties) {
        this.piperClient = piperClient;
        this.storageService = storageService;
        this.properties = properties;
    }

    /**
     * Generate and store a countdown interval clip.
     *
     * @param raceId   database ID of the race
     * @param seconds  seconds remaining to announce (e.g. 300, 120, 60, 30)
     * @param text     full announcement text (e.g. "Race Finals, 5 minutes")
     * @param voiceId  Piper voice model name, or null to use default
     * @return storage public URL, or null if Piper was unavailable
     */
    public String generateCountdownClip(Long raceId, int seconds, String text, String voiceId) {
        String effectiveVoice = resolve(voiceId);
        String key = String.format("audio/race/%d/countdown-%d-%s.wav", raceId, seconds, effectiveVoice);
        return synthesizeAndUpload(key, text, effectiveVoice,
                "race {} countdown {}s clip", raceId, seconds);
    }

    /**
     * Generate and store a car number stagger call clip.
     *
     * @param raceId    database ID of the race
     * @param carNumber car number to announce
     * @param text      announcement text (e.g. "Car 42")
     * @param voiceId   Piper voice model name, or null to use default
     * @return storage public URL, or null if Piper was unavailable
     */
    public String generateCarNumberClip(Long raceId, int carNumber, String text, String voiceId) {
        String effectiveVoice = resolve(voiceId);
        String key = String.format("audio/race/%d/car-%d-%s.wav", raceId, carNumber, effectiveVoice);
        return synthesizeAndUpload(key, text, effectiveVoice,
                "race {} car {} clip", raceId, carNumber);
    }

    /**
     * Generate and store a finish announcement clip.
     *
     * @param raceId    database ID of the race
     * @param competitorId database ID of the competitor
     * @param text      announcement text (e.g. "Alan Smith has finished")
     * @param voiceId   Piper voice model name, or null to use default
     * @return storage public URL, or null if Piper was unavailable
     */
    public String generateFinishClip(Long raceId, Long competitorId, String text, String voiceId) {
        String effectiveVoice = resolve(voiceId);
        String key = String.format("audio/race/%d/finish-%d-%s.wav", raceId, competitorId, effectiveVoice);
        return synthesizeAndUpload(key, text, effectiveVoice,
                "race {} finish competitor {} clip", raceId, competitorId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String resolve(String voiceId) {
        return (voiceId != null && !voiceId.isBlank()) ? voiceId : properties.defaultVoice();
    }

    private String synthesizeAndUpload(String key, String text, String voice,
                                        String logPattern, Object... logArgs) {
        try {
            byte[] wavData = piperClient.synthesize(text, voice);
            return storageService.upload(key, wavData, CONTENT_TYPE_WAV);
        } catch (TtsUnavailableException e) {
            Object[] fullArgs = new Object[logArgs.length + 1];
            System.arraycopy(logArgs, 0, fullArgs, 0, logArgs.length);
            fullArgs[logArgs.length] = e.getMessage();
            log.warn("TTS unavailable generating " + logPattern + ": {}", fullArgs);
            return null;
        }
    }
}
