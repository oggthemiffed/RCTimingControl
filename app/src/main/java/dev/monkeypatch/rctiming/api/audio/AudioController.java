package dev.monkeypatch.rctiming.api.audio;

import dev.monkeypatch.rctiming.infrastructure.tts.PiperTtsClient;
import dev.monkeypatch.rctiming.infrastructure.tts.VoiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Audio REST API — voice listing.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>GET  /api/v1/audio/voices — list available Piper voice models (AUDIO-13)</li>
 * </ul>
 * The racer name preview and voice preference went with racer accounts (L10, #18).
 */
@RestController
public class AudioController {

    private final PiperTtsClient piperClient;

    public AudioController(PiperTtsClient piperClient) {
        this.piperClient = piperClient;
    }

    /**
     * List available Piper TTS voices (AUDIO-13).
     */
    @GetMapping("/api/v1/audio/voices")
    public List<VoiceInfo> listVoices() {
        return piperClient.listVoices();
    }
}
