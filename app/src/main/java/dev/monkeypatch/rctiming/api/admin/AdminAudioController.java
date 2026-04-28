package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.domain.club.ClubAudioSettings;
import dev.monkeypatch.rctiming.domain.club.ClubProfile;
import dev.monkeypatch.rctiming.domain.club.ClubProfileRepository;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.infrastructure.profanity.ProfanityBlocklistEntry;
import dev.monkeypatch.rctiming.infrastructure.profanity.ProfanityBlocklistRepository;
import dev.monkeypatch.rctiming.infrastructure.profanity.ProfanityFilter;
import dev.monkeypatch.rctiming.infrastructure.tts.TtsClipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminAudioController {

    private final ClubProfileRepository clubProfileRepository;
    private final ProfanityBlocklistRepository blocklistRepository;
    private final ProfanityFilter profanityFilter;
    private final UserRepository userRepository;
    private final TtsClipService clipService;

    public AdminAudioController(ClubProfileRepository clubProfileRepository,
                                ProfanityBlocklistRepository blocklistRepository,
                                ProfanityFilter profanityFilter,
                                UserRepository userRepository,
                                TtsClipService clipService) {
        this.clubProfileRepository = clubProfileRepository;
        this.blocklistRepository = blocklistRepository;
        this.profanityFilter = profanityFilter;
        this.userRepository = userRepository;
        this.clipService = clipService;
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
    public ResponseEntity<AudioSettingsDto> getAudioSettings() {
        ClubProfile profile = clubProfileRepository.findAll().stream().findFirst().orElse(null);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
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

    @PostMapping("/settings")
    public ResponseEntity<AudioSettingsDto> saveAudioSettings(@RequestBody AudioSettingsDto dto) {
        ClubProfile profile = clubProfileRepository.findAll().stream().findFirst().orElse(null);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
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

    @GetMapping("/blocklist")
    public List<BlocklistTermDto> getBlocklist() {
        return blocklistRepository.findAll().stream()
                .map(e -> new BlocklistTermDto(e.getId(), e.getWord(), e.getAddedAt().toString()))
                .toList();
    }

    @PostMapping("/blocklist")
    public ResponseEntity<BlocklistTermDto> addTerm(
            @RequestBody String word,
            @AuthenticationPrincipal UserDetails userDetails) {
        word = word.trim().toLowerCase();
        if (word.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (blocklistRepository.findByWordIgnoreCase(word).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User admin = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        ProfanityBlocklistEntry entry = new ProfanityBlocklistEntry();
        entry.setWord(word);
        entry.setAddedBy(admin);
        blocklistRepository.save(entry);
        profanityFilter.reload();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BlocklistTermDto(entry.getId(), entry.getWord(), entry.getAddedAt().toString()));
    }

    @DeleteMapping("/blocklist/{id}")
    public ResponseEntity<Void> removeTerm(@PathVariable Long id) {
        if (!blocklistRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        blocklistRepository.deleteById(id);
        profanityFilter.reload();
        return ResponseEntity.noContent().build();
    }

    // ========== Racer Phonetic Override (AUDIO-15) ==========

    /** DTO for racer phonetic name admin view/override */
    public record PhoneticDto(String displayName, String phoneticName) {}

    @GetMapping("/racer/{userId}/phonetic")
    public ResponseEntity<PhoneticDto> getRacerPhonetic(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        String displayName = user.getFirstName() + " " + user.getLastName();
        return ResponseEntity.ok(new PhoneticDto(displayName, user.getPhoneticName()));
    }

    @PutMapping("/racer/{userId}/phonetic")
    public ResponseEntity<PhoneticDto> updateRacerPhonetic(
            @PathVariable Long userId,
            @RequestBody PhoneticDto dto) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // Admin override bypasses profanity check per AUDIO-15
        user.setPhoneticName(dto.phoneticName());
        userRepository.save(user);
        String displayName = user.getFirstName() + " " + user.getLastName();
        return ResponseEntity.ok(new PhoneticDto(displayName, user.getPhoneticName()));
    }

    @DeleteMapping("/racer/{userId}/name-clip")
    public ResponseEntity<Void> regenerateNameClip(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        String text = (user.getPhoneticName() != null && !user.getPhoneticName().isBlank())
                ? user.getPhoneticName()
                : user.getFirstName() + " " + user.getLastName();
        clipService.generateNameClip(userId, text, user.getPreferredVoiceId());
        return ResponseEntity.noContent().build();
    }
}
