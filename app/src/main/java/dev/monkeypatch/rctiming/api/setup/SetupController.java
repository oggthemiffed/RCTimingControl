package dev.monkeypatch.rctiming.api.setup;

import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.setup.dto.BootstrapRequest;
import dev.monkeypatch.rctiming.api.setup.dto.DecoderConfigUpdateRequest;
import dev.monkeypatch.rctiming.api.setup.dto.SetupProgressDto;
import dev.monkeypatch.rctiming.api.setup.dto.SetupStaffRequest;
import dev.monkeypatch.rctiming.api.setup.dto.SetupStatusDto;
import dev.monkeypatch.rctiming.domain.club.ClubProfileService;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/setup")
public class SetupController {

    private final SetupService setupService;
    private final ClubProfileService clubProfileService;
    private final UserService userService;

    public SetupController(SetupService setupService,
                           ClubProfileService clubProfileService,
                           UserService userService) {
        this.setupService = setupService;
        this.clubProfileService = clubProfileService;
        this.userService = userService;
    }

    @GetMapping("/status")
    public SetupStatusDto getStatus() {
        return setupService.getStatus();
    }

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

    @PatchMapping("/decoder-config")
    @PreAuthorize("hasRole('ADMIN')")
    public SetupProgressDto updateDecoderConfig(@RequestBody @Valid DecoderConfigUpdateRequest req) {
        clubProfileService.updateDecoderConfig(req.decoderHost(), req.decoderPort(), req.decoderProtocol());
        return setupService.getProgress();
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public void createStaff(@RequestBody @Valid SetupStaffRequest req) {
        Set<Role> roles = req.roles().stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());
        userService.createStaff(req.email(), req.password(), req.firstName(), req.lastName(), roles);
    }

    @GetMapping("/forwarder-config-download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadForwarderConfig(HttpServletRequest request) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"forwarder.env\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(setupService.generateForwarderEnv(request).getBytes(StandardCharsets.UTF_8));
    }
}
