package dev.monkeypatch.rctiming.api.setup;

import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.setup.dto.BootstrapRequest;
import dev.monkeypatch.rctiming.api.setup.dto.SetupProgressDto;
import dev.monkeypatch.rctiming.api.setup.dto.SetupStatusDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/setup")
public class SetupController {

    private final SetupService setupService;

    public SetupController(SetupService setupService) {
        this.setupService = setupService;
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
}
