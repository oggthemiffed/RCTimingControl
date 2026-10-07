package dev.monkeypatch.rctiming.api.setup;

import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.setup.dto.BootstrapRequest;
import dev.monkeypatch.rctiming.api.setup.dto.DecoderConfigDto;
import dev.monkeypatch.rctiming.api.setup.dto.DecoderConfigUpdateRequest;
import dev.monkeypatch.rctiming.api.setup.dto.SetupProgressDto;
import dev.monkeypatch.rctiming.api.setup.dto.SetupStaffRequest;
import dev.monkeypatch.rctiming.api.setup.dto.SetupStatusDto;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import dev.monkeypatch.rctiming.domain.club.DecoderSettings;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.UserService;
import dev.monkeypatch.rctiming.timing.DecoderProbe;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/setup")
public class SetupController {

    private final SetupService setupService;
    private final ClubProfileService clubProfileService;
    private final UserService userService;
    private final DecoderProbe decoderProbe;

    public SetupController(SetupService setupService,
                           ClubProfileService clubProfileService,
                           UserService userService,
                           DecoderProbe decoderProbe) {
        this.setupService = setupService;
        this.clubProfileService = clubProfileService;
        this.userService = userService;
        this.decoderProbe = decoderProbe;
    }

    @GetMapping("/status")
    public SetupStatusDto getStatus() {
        return setupService.getStatus();
    }

    @Audited("official_audit_log")
    @PostMapping("/bootstrap")
    public ResponseEntity<AuthResponse> bootstrap(@RequestBody @Valid BootstrapRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(setupService.bootstrap(req));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/progress")
    @PreAuthorize("hasRole('ADMIN')")
    public SetupProgressDto getProgress() {
        return setupService.getProgress();
    }

    @GetMapping("/decoder-config")
    @PreAuthorize("hasRole('ADMIN')")
    public DecoderConfigDto getDecoderConfig() {
        DecoderSettings settings = clubProfileService.getDecoderSettings();
        return new DecoderConfigDto(settings.host(), settings.port(), settings.protocol());
    }

    /** Tests the decoder address on the form. Does not save it or change the live listener. */
    @PostMapping("/decoder-config/test")
    @PreAuthorize("hasRole('ADMIN')")
    public DecoderProbe.Result testDecoderConfig(@RequestBody @Valid DecoderConfigUpdateRequest req) {
        return decoderProbe.probe(req.decoderHost(), req.decoderPort(), req.decoderProtocol());
    }

    @PatchMapping("/decoder-config")
    @PreAuthorize("hasRole('ADMIN')")
    public SetupProgressDto updateDecoderConfig(@RequestBody @Valid DecoderConfigUpdateRequest req) {
        clubProfileService.updateDecoderConfig(req.decoderHost(), req.decoderPort(), req.decoderProtocol());
        return setupService.getProgress();
    }

    @Audited("official_audit_log")
    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public void createStaff(Authentication auth, @RequestBody @Valid SetupStaffRequest req) {
        Set<Role> roles = req.roles().stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());
        userService.createStaff(req.email(), req.password(), req.firstName(), req.lastName(), roles,
                Long.parseLong(auth.getName()));
    }
}
