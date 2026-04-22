package dev.monkeypatch.rctiming.api.racer;

import dev.monkeypatch.rctiming.api.admin.dto.GoverningBodyAffiliationDto;
import dev.monkeypatch.rctiming.api.racer.dto.MembershipDto;
import dev.monkeypatch.rctiming.api.racer.dto.RacerProfileDto;
import dev.monkeypatch.rctiming.api.racer.dto.UpdateRacerProfileRequest;
import dev.monkeypatch.rctiming.api.racer.dto.UpsertMembershipRequest;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import dev.monkeypatch.rctiming.domain.user.RacerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/racer")
@PreAuthorize("hasRole('RACER')")
public class RacerProfileController {

    private final RacerProfileService profileService;
    private final ClubProfileService clubProfileService;

    public RacerProfileController(RacerProfileService profileService,
                                   ClubProfileService clubProfileService) {
        this.profileService = profileService;
        this.clubProfileService = clubProfileService;
    }

    @GetMapping("/affiliations")
    public List<GoverningBodyAffiliationDto> listAffiliations() {
        return clubProfileService.listAffiliations();
    }

    @GetMapping("/profile")
    public RacerProfileDto getProfile(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return profileService.getProfile(userId);
    }

    @PatchMapping("/profile")
    public RacerProfileDto updateProfile(Authentication auth,
                                          @RequestBody @Valid UpdateRacerProfileRequest req) {
        Long userId = Long.parseLong(auth.getName());
        return profileService.updateProfile(userId, req);
    }

    @GetMapping("/memberships")
    public List<MembershipDto> listMemberships(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return profileService.listMemberships(userId);
    }

    @PostMapping("/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipDto addMembership(Authentication auth,
                                        @RequestBody @Valid UpsertMembershipRequest req) {
        Long userId = Long.parseLong(auth.getName());
        return profileService.addMembership(userId, req.governingBodyCode(), req.membershipNumber());
    }

    @PutMapping("/memberships/{code}")
    public MembershipDto updateMembership(Authentication auth,
                                           @PathVariable String code,
                                           @RequestBody @Valid UpsertMembershipRequest req) {
        Long userId = Long.parseLong(auth.getName());
        // Only membershipNumber is mutable — governingBodyCode on path is authoritative
        return profileService.updateMembership(userId, code, req.membershipNumber());
    }

    @DeleteMapping("/memberships/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMembership(Authentication auth, @PathVariable String code) {
        Long userId = Long.parseLong(auth.getName());
        profileService.removeMembership(userId, code);
    }
}
