package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.domain.user.OfficialService;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.query.official.OfficialChangeDto;
import dev.monkeypatch.rctiming.query.official.OfficialDto;
import dev.monkeypatch.rctiming.query.official.OfficialQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * The Officials page (#61): add officials, change their roles, set a new password, and disable or
 * re-enable them. Admins only. Refusals that would lock the club out answer 409.
 */
@RestController
@RequestMapping("/api/v1/admin/officials")
@PreAuthorize("hasRole('ADMIN')")
public class OfficialController {

    /** How many recent changes the page shows. */
    static final int RECENT_CHANGES = 50;

    private final OfficialService officialService;
    private final OfficialQueryService officialQueryService;

    public OfficialController(OfficialService officialService, OfficialQueryService officialQueryService) {
        this.officialService = officialService;
        this.officialQueryService = officialQueryService;
    }

    public record AddOfficialRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Size(min = OfficialService.MIN_PASSWORD_LENGTH) String password,
            @NotEmpty Set<Role> roles
    ) {}

    public record RolesRequest(@NotEmpty Set<Role> roles) {}

    public record PasswordRequest(@NotNull @Size(min = OfficialService.MIN_PASSWORD_LENGTH) String password) {}

    @GetMapping
    public List<OfficialDto> list() {
        return officialQueryService.listAll();
    }

    @GetMapping("/changes")
    public List<OfficialChangeDto> changes() {
        return officialQueryService.recentChanges(RECENT_CHANGES);
    }

    @Audited("official_audit_log")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfficialDto add(@RequestBody @Valid AddOfficialRequest request, Authentication auth) {
        User user = officialService.add(request.email(), request.firstName(), request.lastName(),
                request.password(), request.roles(), actorId(auth));
        return toDto(user);
    }

    @Audited("official_audit_log")
    @PutMapping("/{id}/roles")
    public OfficialDto changeRoles(@PathVariable long id, @RequestBody @Valid RolesRequest request, Authentication auth) {
        return toDto(officialService.changeRoles(id, request.roles(), actorId(auth)));
    }

    @Audited("official_audit_log")
    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(@PathVariable long id, @RequestBody @Valid PasswordRequest request, Authentication auth) {
        officialService.setPassword(id, request.password(), actorId(auth));
    }

    @Audited("official_audit_log")
    @PostMapping("/{id}/disable")
    public OfficialDto disable(@PathVariable long id, Authentication auth) {
        return toDto(officialService.disable(id, actorId(auth)));
    }

    @Audited("official_audit_log")
    @PostMapping("/{id}/enable")
    public OfficialDto enable(@PathVariable long id, Authentication auth) {
        return toDto(officialService.enable(id, actorId(auth)));
    }

    private static long actorId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    private static OfficialDto toDto(User user) {
        return new OfficialDto(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRoles().stream().map(Enum::name).sorted().toList(),
                user.isEnabled(), user.getDisabledAt(), user.getCreatedAt());
    }
}
