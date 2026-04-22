package dev.monkeypatch.rctiming.domain.club;

import dev.monkeypatch.rctiming.api.admin.dto.ClubProfileDto;
import dev.monkeypatch.rctiming.api.admin.dto.CreateClubProfileRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateGoverningBodyRequest;
import dev.monkeypatch.rctiming.api.admin.dto.GoverningBodyAffiliationDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class ClubProfileService {

    private final ClubProfileRepository clubProfileRepository;
    private final GoverningBodyAffiliationRepository affiliationRepository;

    public ClubProfileService(ClubProfileRepository clubProfileRepository,
                               GoverningBodyAffiliationRepository affiliationRepository) {
        this.clubProfileRepository = clubProfileRepository;
        this.affiliationRepository = affiliationRepository;
    }

    @Transactional(readOnly = true)
    public ClubProfileDto getProfile() {
        return clubProfileRepository.findAll().stream()
                .findFirst()
                .map(ClubProfileDto::from)
                .orElseGet(() -> new ClubProfileDto(null, "", null, null, null, null, null, "UTC", null, null));
    }

    public Long getSingletonProfileId() {
        return clubProfileRepository.findAll().stream()
                .findFirst()
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

    public ClubProfileDto createOrUpdateProfile(CreateClubProfileRequest request) {
        // Validate timezone — throws DateTimeException on invalid
        ZoneId.of(request.timezone());

        ClubProfile profile = clubProfileRepository.findAll().stream()
                .findFirst()
                .orElseGet(ClubProfile::new);

        boolean isNew = profile.getId() == null;

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

        return ClubProfileDto.from(clubProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<GoverningBodyAffiliationDto> listAffiliations() {
        return affiliationRepository.findAll().stream()
                .map(GoverningBodyAffiliationDto::from)
                .toList();
    }

    public GoverningBodyAffiliationDto createAffiliation(CreateGoverningBodyRequest request) {
        GoverningBodyAffiliation affiliation = new GoverningBodyAffiliation();
        affiliation.setCode(request.code());
        affiliation.setDisplayName(request.displayName());
        affiliation.setMembershipRequired(request.membershipRequired());
        affiliation.setCreatedAt(Instant.now());
        return GoverningBodyAffiliationDto.from(affiliationRepository.save(affiliation));
    }

    public GoverningBodyAffiliationDto updateAffiliation(Long id, CreateGoverningBodyRequest request) {
        GoverningBodyAffiliation affiliation = affiliationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Governing body affiliation not found: " + id));
        affiliation.setCode(request.code());
        affiliation.setDisplayName(request.displayName());
        affiliation.setMembershipRequired(request.membershipRequired());
        return GoverningBodyAffiliationDto.from(affiliationRepository.save(affiliation));
    }

    public void deleteAffiliation(Long id) {
        if (!affiliationRepository.existsById(id)) {
            throw new EntityNotFoundException("Governing body affiliation not found: " + id);
        }
        affiliationRepository.deleteById(id);
    }
}
