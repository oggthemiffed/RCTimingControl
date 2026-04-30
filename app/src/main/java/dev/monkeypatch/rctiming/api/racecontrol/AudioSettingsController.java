package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.domain.club.ClubAudioSettings;
import dev.monkeypatch.rctiming.domain.club.ClubProfile;
import dev.monkeypatch.rctiming.domain.club.ClubProfileRepository;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /** DTO for race-control audio settings view/update */
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
     * Only fields included in the request body are applied.
     */
    @PatchMapping
    public ResponseEntity<AudioSettingsDto> updateSettings(@RequestBody AudioSettingsDto dto) {
        Long profileId = clubProfileService.getSingletonProfileId();
        ClubProfile profile = clubProfileRepository.findById(profileId).orElseThrow();
        ClubAudioSettings newSettings = new ClubAudioSettings(
                dto.announceCountdown(),
                dto.announceStagger(),
                dto.announceLapBeep(),
                dto.announceFinish(),
                dto.announceRunningOrder(),
                dto.runningOrderDepth(),
                dto.countdownIntervals()
        );
        profile.setAudioSettings(newSettings);
        clubProfileRepository.save(profile);
        return ResponseEntity.ok(dto);
    }
}
