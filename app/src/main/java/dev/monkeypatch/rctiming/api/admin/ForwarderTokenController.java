package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.forwarder.ForwarderTokenService;
import dev.monkeypatch.rctiming.forwarder.dto.ForwarderTokenGenerateResponseDto;
import dev.monkeypatch.rctiming.forwarder.dto.ForwarderTokenStatusDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 5: admin lifecycle for forwarder API token (D-08, D-09, FORWARDER-05).
 * NOTE: HTTPS strongly recommended in production; one-time-reveal pattern means token
 * plaintext is in HTTP response body (T-05-11 accepted risk — venue LAN v1 deployment).
 */
@RestController
@RequestMapping("/api/v1/admin/forwarder/token")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
public class ForwarderTokenController {

    private final ForwarderTokenService service;

    public ForwarderTokenController(ForwarderTokenService service) {
        this.service = service;
    }

    @GetMapping
    public ForwarderTokenStatusDto getStatus() {
        var s = service.getCurrentStatus();
        String name = s.status() == null ? "NONE" : s.status().name();
        return new ForwarderTokenStatusDto(name, s.generatedAt(), s.revokedAt());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ForwarderTokenGenerateResponseDto generate() {
        var r = service.generate();
        return new ForwarderTokenGenerateResponseDto(r.plaintext(), "ACTIVE", r.persisted().getGeneratedAt());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke() {
        service.revoke();
    }
}
