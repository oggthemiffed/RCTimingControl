package dev.monkeypatch.rctiming.api.audio;

import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.infrastructure.tts.PiperTtsClient;
import dev.monkeypatch.rctiming.infrastructure.tts.TtsClipService;
import dev.monkeypatch.rctiming.infrastructure.tts.TtsUnavailableException;
import dev.monkeypatch.rctiming.infrastructure.tts.VoiceInfo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Audio REST API — voice listing and name clip preview.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>GET /api/v1/audio/voices — list available Piper voice models (AUDIO-13)</li>
 *   <li>GET /api/v1/audio/preview?voice={voiceId} — preview current user's name clip (AUDIO-13)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/audio")
public class AudioController {

    private final PiperTtsClient piperClient;
    private final TtsClipService clipService;
    private final UserRepository userRepository;

    public AudioController(PiperTtsClient piperClient,
                           TtsClipService clipService,
                           UserRepository userRepository) {
        this.piperClient = piperClient;
        this.clipService = clipService;
        this.userRepository = userRepository;
    }

    /**
     * List available Piper TTS voices (AUDIO-13).
     * Queries the running Piper container; returns empty list if unavailable.
     */
    @GetMapping("/voices")
    public List<VoiceInfo> listVoices() {
        return piperClient.listVoices();
    }

    /**
     * Generate and return a WAV preview of the authenticated racer's name (AUDIO-13).
     * Uses phonetic name if set, otherwise first+last name.
     * Responds with 503 if Piper is unavailable.
     *
     * @param auth    authenticated principal (JWT-derived; getName() returns user ID)
     * @param voice   optional voice override; defaults to user's preferredVoiceId or system default
     */
    @GetMapping(value = "/preview", produces = "audio/wav")
    public ResponseEntity<byte[]> previewNameClip(
            Authentication auth,
            @RequestParam(required = false) String voice) {

        Long userId = Long.parseLong(auth.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // Prefer phonetic name; fall back to full name
        String text = (user.getPhoneticName() != null && !user.getPhoneticName().isBlank())
                ? user.getPhoneticName()
                : user.getFirstName() + " " + user.getLastName();

        // Prefer explicit voice param → user preference → system default (resolved inside TtsClipService)
        String effectiveVoice = (voice != null && !voice.isBlank())
                ? voice
                : user.getPreferredVoiceId();

        try {
            byte[] wavData = clipService.generatePreview(text, effectiveVoice);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"name-preview.wav\"")
                    .contentType(MediaType.parseMediaType("audio/wav"))
                    .body(wavData);
        } catch (TtsUnavailableException e) {
            return ResponseEntity.status(503).build();
        }
    }
}
