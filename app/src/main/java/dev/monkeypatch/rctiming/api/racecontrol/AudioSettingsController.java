package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.domain.club.ClubAudioSettings;
import dev.monkeypatch.rctiming.domain.club.ClubProfile;
import dev.monkeypatch.rctiming.domain.club.ClubProfileRepository;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import dev.monkeypatch.rctiming.security.CurrentOfficial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Race control–facing audio settings endpoint (AUDIO-07).
 * <p>
 * Allows race directors to view and update audio toggle settings during a race day
 * without navigating to the admin panel. Settings are persisted to the club profile
 * so they survive page reloads.
 */
@RestController
@RequestMapping("/api/v1/race-control/settings/audio")
@PreAuthorize("hasAnyRole('RACE_DIRECTOR', 'ADMIN')")
public class AudioSettingsController {

    private final ClubProfileRepository clubProfileRepository;
    private final ClubProfileService clubProfileService;

    public AudioSettingsController(ClubProfileRepository clubProfileRepository,
                                   ClubProfileService clubProfileService) {
        this.clubProfileRepository = clubProfileRepository;
        this.clubProfileService = clubProfileService;
    }

    /** The settings as they are now. */
    public record AudioSettingsDto(
            boolean announceCountdown,
            boolean announceStagger,
            boolean announceLapBeep,
            boolean announceFinish,
            boolean announceRunningOrder,
            int runningOrderDepth,
            int[] countdownIntervals
    ) {}

    /**
     * A change to the settings. A field left out (null) keeps its saved value, so a client can send just the
     * toggle it changed.
     */
    public record AudioSettingsPatch(
            Boolean announceCountdown,
            Boolean announceStagger,
            Boolean announceLapBeep,
            Boolean announceFinish,
            Boolean announceRunningOrder,
            @Min(1) @Max(20) Integer runningOrderDepth,
            int[] countdownIntervals
    ) {}

    /**
     * Returns the current audio settings from the club profile.
     */
    @GetMapping
    public ResponseEntity<AudioSettingsDto> getSettings() {
        Long profileId = clubProfileService.getSingletonProfileId();
        ClubProfile profile = clubProfileRepository.findById(profileId).orElseThrow();
        ClubAudioSettings s = profile.getAudioSettings();
        return ResponseEntity.ok(new AudioSettingsDto(
                s.announceCountdown(),
                s.announceStagger(),
                s.announceLapBeep(),
                s.announceFinish(),
                s.announceRunningOrder(),
                s.runningOrderDepth(),
                s.countdownIntervals()
        ));
    }

    /**
     * Updates and persists audio settings to the club profile.
     * Only fields included in the request body are applied; the answer is the settings as saved.
     */
    @Audited("audit_log")
    @PatchMapping
    public ResponseEntity<AudioSettingsDto> updateSettings(Authentication auth,
                                                           @RequestBody @Valid AudioSettingsPatch patch) {
        ClubProfile saved = clubProfileService.changeAudioSettings(CurrentOfficial.actor(auth),
                profile -> {
                    ClubAudioSettings s = profile.getAudioSettings();
                    profile.setAudioSettings(new ClubAudioSettings(
                            patch.announceCountdown() != null ? patch.announceCountdown() : s.announceCountdown(),
                            patch.announceStagger() != null ? patch.announceStagger() : s.announceStagger(),
                            patch.announceLapBeep() != null ? patch.announceLapBeep() : s.announceLapBeep(),
                            patch.announceFinish() != null ? patch.announceFinish() : s.announceFinish(),
                            patch.announceRunningOrder() != null ? patch.announceRunningOrder() : s.announceRunningOrder(),
                            patch.runningOrderDepth() != null ? patch.runningOrderDepth() : s.runningOrderDepth(),
                            patch.countdownIntervals() != null ? patch.countdownIntervals() : s.countdownIntervals()
                    ));
                });
        ClubAudioSettings newSettings = saved.getAudioSettings();
        return ResponseEntity.ok(new AudioSettingsDto(
                newSettings.announceCountdown(),
                newSettings.announceStagger(),
                newSettings.announceLapBeep(),
                newSettings.announceFinish(),
                newSettings.announceRunningOrder(),
                newSettings.runningOrderDepth(),
                newSettings.countdownIntervals()
        ));
    }
}
