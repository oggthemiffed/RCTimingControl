package dev.monkeypatch.rctiming.api.racer;

import org.springframework.context.ApplicationEvent;

/**
 * Published when a racer's profile is updated (display name, phonetic name, or voice preference).
 * Listened to by {@code NameClipGenerationListener} to asynchronously regenerate the TTS name clip.
 */
public class UserProfileUpdatedEvent extends ApplicationEvent {

    private final Long userId;
    private final String displayName;
    private final String phoneticName;
    private final String preferredVoiceId;

    public UserProfileUpdatedEvent(Object source, Long userId, String displayName,
                                    String phoneticName, String preferredVoiceId) {
        super(source);
        this.userId = userId;
        this.displayName = displayName;
        this.phoneticName = phoneticName;
        this.preferredVoiceId = preferredVoiceId;
    }

    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getPhoneticName() { return phoneticName; }
    public String getPreferredVoiceId() { return preferredVoiceId; }
}
