package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.domain.club.ClubAudioSettings;
import dev.monkeypatch.rctiming.domain.club.ClubProfile;
import dev.monkeypatch.rctiming.domain.club.ClubProfileRepository;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Club-wide announcer settings (AUDIO-07), for admins and race directors.
 */
@RestController
@RequestMapping("/api/v1/admin/audio")
public class AdminAudioController {

    private final ClubProfileRepository clubProfileRepository;
    private final ClubProfileService clubProfileService;

    public AdminAudioController(ClubProfileRepository clubProfileRepository,
                                ClubProfileService clubProfileService) {
        this.clubProfileRepository = clubProfileRepository;
        this.clubProfileService = clubProfileService;
    }

    // ========== Audio Settings (AUDIO-07) ==========

    /** DTO for club-wide audio toggle settings */
    public record AudioSettingsDto(
            boolean announceCountdown,
            boolean announceStagger,
            boolean announceLapBeep,
            boolean announceFinish,
            boolean announceRunningOrder,
            int runningOrderDepth,
            String defaultVoiceId
    ) {}

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public ResponseEntity<AudioSettingsDto> getAudioSettings() {
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
                profile.getDefaultVoiceId()
        ));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public ResponseEntity<AudioSettingsDto> saveAudioSettings(@RequestBody AudioSettingsDto dto) {
        Long profileId = clubProfileService.getSingletonProfileId();
        ClubProfile profile = clubProfileRepository.findById(profileId).orElseThrow();
        ClubAudioSettings newSettings = new ClubAudioSettings(
                dto.announceCountdown(),
                dto.announceStagger(),
                dto.announceLapBeep(),
                dto.announceFinish(),
                dto.announceRunningOrder(),
                dto.runningOrderDepth(),
                null  // preserve default intervals
        );
        profile.setAudioSettings(newSettings);
        profile.setDefaultVoiceId(dto.defaultVoiceId());
        clubProfileRepository.save(profile);
        return ResponseEntity.ok(dto);
    }
}
