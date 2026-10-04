package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.domain.club.ClubAudioSettings;
import dev.monkeypatch.rctiming.domain.club.ClubProfile;
import dev.monkeypatch.rctiming.domain.club.ClubProfileRepository;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.infrastructure.profanity.ProfanityBlocklistEntry;
import dev.monkeypatch.rctiming.infrastructure.profanity.ProfanityBlocklistRepository;
import dev.monkeypatch.rctiming.infrastructure.profanity.ProfanityFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only audio management endpoints (AUDIO-07, AUDIO-14, AUDIO-15).
 * All endpoints require {@code ADMIN} role.
 */
@RestController
@RequestMapping("/api/v1/admin/audio")
public class AdminAudioController {

    private final ClubProfileRepository clubProfileRepository;
    private final ClubProfileService clubProfileService;
    private final ProfanityBlocklistRepository blocklistRepository;
    private final ProfanityFilter profanityFilter;
    private final UserRepository userRepository;

    public AdminAudioController(ClubProfileRepository clubProfileRepository,
                                ClubProfileService clubProfileService,
                                ProfanityBlocklistRepository blocklistRepository,
                                ProfanityFilter profanityFilter,
                                UserRepository userRepository) {
        this.clubProfileRepository = clubProfileRepository;
        this.clubProfileService = clubProfileService;
        this.blocklistRepository = blocklistRepository;
        this.profanityFilter = profanityFilter;
        this.userRepository = userRepository;
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

    // ========== Profanity Blocklist (AUDIO-14) ==========

    /** DTO for a single blocklist term */
    public record BlocklistTermDto(Long id, String word, String addedAt) {}

    /** Request body for adding a blocklist term */
    public record AddTermRequest(String word) {}

    @GetMapping("/blocklist")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public List<BlocklistTermDto> getBlocklist() {
        return blocklistRepository.findAll().stream()
                .map(e -> new BlocklistTermDto(e.getId(), e.getWord(), e.getAddedAt().toString()))
                .toList();
    }

    @PostMapping("/blocklist")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public ResponseEntity<BlocklistTermDto> addTerm(
            @RequestBody AddTermRequest request,
            Authentication authentication) {
        String word = request.word() == null ? "" : request.word().trim().toLowerCase();
        if (word.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (blocklistRepository.findByWordIgnoreCase(word).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User admin = null;
        if (authentication != null) {
            try {
                Long userId = Long.parseLong(authentication.getName());
                admin = userRepository.findById(userId).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        ProfanityBlocklistEntry entry = new ProfanityBlocklistEntry();
        entry.setWord(word);
        entry.setAddedBy(admin);
        blocklistRepository.save(entry);
        profanityFilter.reload();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BlocklistTermDto(entry.getId(), entry.getWord(), entry.getAddedAt().toString()));
    }

    @DeleteMapping("/blocklist/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public ResponseEntity<Void> removeTerm(@PathVariable Long id) {
        if (!blocklistRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        blocklistRepository.deleteById(id);
        profanityFilter.reload();
        return ResponseEntity.noContent().build();
    }
}
