package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.api.admin.dto.ClubProfileDto;
import dev.monkeypatch.rctiming.api.admin.dto.CreateClubProfileRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateGoverningBodyRequest;
import dev.monkeypatch.rctiming.api.admin.dto.GoverningBodyAffiliationDto;
import dev.monkeypatch.rctiming.api.admin.dto.LogoUploadResponse;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import dev.monkeypatch.rctiming.domain.club.LogoUploadService;
import dev.monkeypatch.rctiming.security.CurrentOfficial;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/club")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class ClubProfileController {

    private final ClubProfileService clubProfileService;
    private final LogoUploadService logoUploadService;

    public ClubProfileController(ClubProfileService clubProfileService,
                                  LogoUploadService logoUploadService) {
        this.clubProfileService = clubProfileService;
        this.logoUploadService = logoUploadService;
    }

    @GetMapping("/profile")
    public ClubProfileDto getProfile() {
        return clubProfileService.getProfile();
    }

    @Audited("audit_log")
    @PutMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ClubProfileDto createOrUpdateProfile(Authentication auth, @RequestBody @Valid CreateClubProfileRequest request) {
        return clubProfileService.createOrUpdateProfile(CurrentOfficial.actor(auth), request);
    }

    @GetMapping("/affiliations")
    public List<GoverningBodyAffiliationDto> listAffiliations() {
        return clubProfileService.listAffiliations();
    }

    @Audited("audit_log")
    @PostMapping("/affiliations")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public GoverningBodyAffiliationDto createAffiliation(Authentication auth,
                                                          @RequestBody @Valid CreateGoverningBodyRequest request) {
        return clubProfileService.createAffiliation(CurrentOfficial.actor(auth), request);
    }

    @Audited("audit_log")
    @PutMapping("/affiliations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GoverningBodyAffiliationDto updateAffiliation(Authentication auth, @PathVariable Long id,
                                                          @RequestBody @Valid CreateGoverningBodyRequest request) {
        return clubProfileService.updateAffiliation(CurrentOfficial.actor(auth), id, request);
    }

    @Audited("audit_log")
    @DeleteMapping("/affiliations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAffiliation(Authentication auth, @PathVariable Long id) {
        clubProfileService.deleteAffiliation(CurrentOfficial.actor(auth), id);
    }

    @Audited("audit_log")
    @PutMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public LogoUploadResponse uploadLogo(Authentication auth, @RequestPart("file") MultipartFile file) {
        Long profileId = clubProfileService.getSingletonProfileId();
        String url = logoUploadService.uploadLogo(CurrentOfficial.actor(auth), profileId, file);
        return new LogoUploadResponse(url);
    }
}
