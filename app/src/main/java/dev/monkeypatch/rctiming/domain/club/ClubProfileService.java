package dev.monkeypatch.rctiming.domain.club;

import dev.monkeypatch.rctiming.api.admin.dto.ClubProfileDto;
import dev.monkeypatch.rctiming.api.admin.dto.CreateClubProfileRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateGoverningBodyRequest;
import dev.monkeypatch.rctiming.api.admin.dto.GoverningBodyAffiliationDto;
import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@Transactional
public class ClubProfileService {

    private final ClubProfileRepository clubProfileRepository;
    private final GoverningBodyAffiliationRepository affiliationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService audit;

    public ClubProfileService(ClubProfileRepository clubProfileRepository,
                               GoverningBodyAffiliationRepository affiliationRepository,
                               ApplicationEventPublisher eventPublisher,
                               AuditService audit) {
        this.clubProfileRepository = clubProfileRepository;
        this.affiliationRepository = affiliationRepository;
        this.eventPublisher = eventPublisher;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public DecoderSettings getDecoderSettings() {
        return clubProfileRepository.findCurrent()
                .map(profile -> new DecoderSettings(
                        profile.getDecoderHost(), profile.getDecoderPort(), profile.getDecoderProtocol()))
                .orElseGet(() -> new DecoderSettings(null, null, null));
    }

    @Transactional(readOnly = true)
    public ClubProfileDto getProfile() {
        return clubProfileRepository.findCurrent()
                .map(ClubProfileDto::from)
                .orElseGet(() -> new ClubProfileDto(null, "", null, null, null, null, null, "UTC", null, null));
    }

    /** The announcer settings, or the defaults before a club profile exists. */
    @Transactional(readOnly = true)
    public ClubAudioSettings audioSettings() {
        return clubProfileRepository.findCurrent()
                .map(ClubProfile::getAudioSettings)
                .orElseGet(ClubAudioSettings::defaults);
    }

    /** The club's chosen announcer voice, or empty to use the Piper default. */
    @Transactional(readOnly = true)
    public Optional<String> defaultVoiceId() {
        return clubProfileRepository.findCurrent()
                .map(ClubProfile::getDefaultVoiceId)
                .filter(v -> !v.isBlank());
    }

    /**
     * The profile's id for a write, creating a blank profile first if the setup wizard has not saved one,
     * so the announcer settings or a logo can be stored before the club's details.
     */
    public Long getSingletonProfileId() {
        return clubProfileRepository.findCurrent()
                .map(ClubProfile::getId)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    ClubProfile blank = new ClubProfile();
                    blank.setName("");
                    blank.setTimezone("UTC");
                    blank.setCreatedAt(now);
                    blank.setUpdatedAt(now);
                    return clubProfileRepository.save(blank).getId();
                });
    }

    public ClubProfileDto createOrUpdateProfile(Actor actor, CreateClubProfileRequest request) {
        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timezone: " + request.timezone(), e);
        }

        ClubProfile profile = clubProfileRepository.findCurrent().orElseGet(ClubProfile::new);

        boolean isNew = profile.getId() == null;
        Map<String, Object> before = isNew ? null : profileValues(profile);

        profile.setName(request.name());
        profile.setEmail(request.email());
        profile.setPhone(request.phone());
        profile.setWebsiteUrl(request.websiteUrl());
        profile.setLatitude(request.latitude());
        profile.setLongitude(request.longitude());
        // Use the raw field setter to avoid double-validation
        profile.setTimezone(request.timezone());
        profile.setLogoType(request.logoType());

        Instant now = Instant.now();
        if (isNew) {
            profile.setCreatedAt(now);
        }
        profile.setUpdatedAt(now);

        ClubProfile saved = clubProfileRepository.save(profile);
        audit.entry(actor, isNew ? "CLUB_PROFILE_CREATED" : "CLUB_PROFILE_UPDATED")
                .entity("club_profile", saved.getId())
                .summary("Saved the club details for " + saved.getName())
                .before(before).after(profileValues(saved)).record();
        return ClubProfileDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<GoverningBodyAffiliationDto> listAffiliations() {
        return affiliationRepository.findAll().stream()
                .map(GoverningBodyAffiliationDto::from)
                .toList();
    }

    public GoverningBodyAffiliationDto createAffiliation(Actor actor, CreateGoverningBodyRequest request) {
        GoverningBodyAffiliation affiliation = new GoverningBodyAffiliation();
        affiliation.setCode(request.code());
        affiliation.setDisplayName(request.displayName());
        affiliation.setMembershipRequired(request.membershipRequired());
        affiliation.setCreatedAt(Instant.now());
        GoverningBodyAffiliation saved = affiliationRepository.save(affiliation);
        audit.entry(actor, "AFFILIATION_CREATED").entity("affiliation", saved.getId())
                .summary("Added the governing body " + saved.getDisplayName())
                .after(affiliationValues(saved)).record();
        return GoverningBodyAffiliationDto.from(saved);
    }

    public GoverningBodyAffiliationDto updateAffiliation(Actor actor, Long id, CreateGoverningBodyRequest request) {
        GoverningBodyAffiliation affiliation = affiliationRepository.getOrThrow(id);
        Map<String, Object> before = affiliationValues(affiliation);
        affiliation.setCode(request.code());
        affiliation.setDisplayName(request.displayName());
        affiliation.setMembershipRequired(request.membershipRequired());
        GoverningBodyAffiliation saved = affiliationRepository.save(affiliation);
        audit.entry(actor, "AFFILIATION_UPDATED").entity("affiliation", id)
                .summary("Changed the governing body " + saved.getDisplayName())
                .before(before).after(affiliationValues(saved)).record();
        return GoverningBodyAffiliationDto.from(saved);
    }

    public void deleteAffiliation(Actor actor, Long id) {
        GoverningBodyAffiliation affiliation = affiliationRepository.getOrThrow(id);
        affiliationRepository.deleteById(id);
        audit.entry(actor, "AFFILIATION_DELETED").entity("affiliation", id)
                .summary("Removed the governing body " + affiliation.getDisplayName())
                .before(affiliationValues(affiliation)).record();
    }

    /**
     * Changes the announcer settings with {@code change}, which edits the profile it is given, and records the
     * settings and default voice before and after. Both audio endpoints go through here.
     */
    public ClubProfile changeAudioSettings(Actor actor, Consumer<ClubProfile> change) {
        ClubProfile profile = clubProfileRepository.findById(getSingletonProfileId()).orElseThrow();
        Map<String, Object> before = audioValues(profile);
        change.accept(profile);
        ClubProfile saved = clubProfileRepository.save(profile);
        audit.entry(actor, "AUDIO_SETTINGS_CHANGED").entity("club_profile", saved.getId())
                .summary("Changed the announcer settings")
                .before(before).after(audioValues(saved)).record();
        return saved;
    }

    @Transactional
    public ClubProfile updateDecoderConfig(Actor actor, String host, Integer port, String protocol) {
        ClubProfile profile = clubProfileRepository.findCurrent()
                .orElseThrow(() -> new IllegalStateException("No club profile — complete club step first"));
        Map<String, Object> before = decoderValues(profile);
        profile.setDecoderHost(host);
        profile.setDecoderPort(port);
        profile.setDecoderProtocol(protocol);
        profile.setUpdatedAt(Instant.now());
        ClubProfile saved = clubProfileRepository.save(profile);
        audit.entry(actor, "DECODER_CONFIG_CHANGED").entity("club_profile", saved.getId())
                .summary(host == null || host.isBlank() ? "Cleared the decoder address"
                        : "Changed the decoder address to " + host + ":" + port + " (" + protocol + ")")
                .before(before).after(decoderValues(saved)).record();
        eventPublisher.publishEvent(new DecoderSettingsChangedEvent(new DecoderSettings(host, port, protocol)));
        return saved;
    }

    private static Map<String, Object> profileValues(ClubProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", p.getName());
        m.put("email", p.getEmail());
        m.put("phone", p.getPhone());
        m.put("websiteUrl", p.getWebsiteUrl());
        m.put("latitude", p.getLatitude());
        m.put("longitude", p.getLongitude());
        m.put("timezone", p.getTimezone());
        m.put("logoType", p.getLogoType());
        return m;
    }

    private static Map<String, Object> affiliationValues(GoverningBodyAffiliation a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", a.getCode());
        m.put("displayName", a.getDisplayName());
        m.put("membershipRequired", a.isMembershipRequired());
        return m;
    }

    private static Map<String, Object> audioValues(ClubProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("settings", p.getAudioSettings());
        m.put("defaultVoiceId", p.getDefaultVoiceId());
        return m;
    }

    private static Map<String, Object> decoderValues(ClubProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("host", p.getDecoderHost());
        m.put("port", p.getDecoderPort());
        m.put("protocol", p.getDecoderProtocol());
        return m;
    }
}
