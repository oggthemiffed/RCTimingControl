package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.ClubProfileDto;
import dev.monkeypatch.rctiming.api.admin.dto.CreateClubProfileRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateGoverningBodyRequest;
import dev.monkeypatch.rctiming.api.admin.dto.GoverningBodyAffiliationDto;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/club")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class ClubProfileController {

    private final ClubProfileService clubProfileService;

    public ClubProfileController(ClubProfileService clubProfileService) {
        this.clubProfileService = clubProfileService;
    }

    @GetMapping("/profile")
    public ClubProfileDto getProfile() {
        return clubProfileService.getProfile();
    }

    @PutMapping("/profile")
    public ClubProfileDto createOrUpdateProfile(@RequestBody @Valid CreateClubProfileRequest request) {
        return clubProfileService.createOrUpdateProfile(request);
    }

    @GetMapping("/affiliations")
    public List<GoverningBodyAffiliationDto> listAffiliations() {
        return clubProfileService.listAffiliations();
    }

    @PostMapping("/affiliations")
    @ResponseStatus(HttpStatus.CREATED)
    public GoverningBodyAffiliationDto createAffiliation(@RequestBody @Valid CreateGoverningBodyRequest request) {
        return clubProfileService.createAffiliation(request);
    }

    @PutMapping("/affiliations/{id}")
    public GoverningBodyAffiliationDto updateAffiliation(@PathVariable Long id,
                                                          @RequestBody @Valid CreateGoverningBodyRequest request) {
        return clubProfileService.updateAffiliation(id, request);
    }

    @DeleteMapping("/affiliations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAffiliation(@PathVariable Long id) {
        clubProfileService.deleteAffiliation(id);
    }
}
