package dev.monkeypatch.rctiming.infrastructure.tts;

import dev.monkeypatch.rctiming.api.racer.UserProfileUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronously generates a TTS name clip whenever a racer's profile is updated (AUDIO-08).
 * <p>
 * Runs in the {@code taskExecutor} thread pool (configured in AsyncConfig).
 * Failures are logged as warnings — they do NOT propagate back to the caller.
 */
@Component
public class NameClipGenerationListener {

    private static final Logger log = LoggerFactory.getLogger(NameClipGenerationListener.class);

    private final TtsClipService clipService;

    public NameClipGenerationListener(TtsClipService clipService) {
        this.clipService = clipService;
    }

    @Async
    @EventListener
    public void onProfileUpdated(UserProfileUpdatedEvent event) {
        // Prefer phonetic name for TTS; fall back to full display name
        String text = (event.getPhoneticName() != null && !event.getPhoneticName().isBlank())
                ? event.getPhoneticName()
                : event.getDisplayName();

        log.info("Generating name clip for user {} (voice: {})", event.getUserId(), event.getPreferredVoiceId());
        String url = clipService.generateNameClip(event.getUserId(), text, event.getPreferredVoiceId());
        if (url != null) {
            log.info("Name clip generated for user {}: {}", event.getUserId(), url);
        } else {
            log.warn("Name clip generation skipped for user {} (Piper unavailable)", event.getUserId());
        }
    }
}
