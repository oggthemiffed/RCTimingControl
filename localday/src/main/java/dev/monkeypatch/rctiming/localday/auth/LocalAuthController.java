package dev.monkeypatch.rctiming.localday.auth;

import dev.monkeypatch.rctiming.localday.auth.dto.ErrorResponse;
import dev.monkeypatch.rctiming.localday.auth.dto.LockedResponse;
import dev.monkeypatch.rctiming.localday.auth.dto.LoginRequest;
import dev.monkeypatch.rctiming.localday.auth.dto.LoginResponse;
import dev.monkeypatch.rctiming.localday.auth.dto.OfficialSummaryDto;
import dev.monkeypatch.rctiming.localday.auth.dto.RecoverRequest;
import dev.monkeypatch.rctiming.localday.auth.dto.RecoverResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Day-scoped local authentication endpoints (U6, R9). All three endpoints here are
 * unauthenticated by design — {@code /officials} is a public read of names only,
 * {@code /login} and {@code /recover} ARE the authentication step itself. Every other
 * endpoint, present or future, requires {@code Authorization: Bearer <sessionToken>} per
 * {@link dev.monkeypatch.rctiming.localday.config.LocalSecurityConfig}'s default-deny rule.
 */
@RestController
@RequestMapping("/api/v1/local-auth")
public class LocalAuthController {

    private final LocalSessionService localSessionService;

    public LocalAuthController(LocalSessionService localSessionService) {
        this.localSessionService = localSessionService;
    }

    @GetMapping("/officials")
    public List<OfficialSummaryDto> listOfficials() {
        return localSessionService.listOfficials().stream()
                .map(o -> new OfficialSummaryDto(o.credentialId(), o.officialName(), o.recovery()))
                .toList();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        LoginResult result = localSessionService.login(request.credentialId(), request.secret());
        return switch (result) {
            case LoginResult.Success success -> ResponseEntity.ok(
                    new LoginResponse(success.sessionToken(), success.officialName(), success.credentialId()));
            case LoginResult.InvalidCredential ignored -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("invalid_credential"));
            case LoginResult.Locked locked -> ResponseEntity.status(HttpStatus.LOCKED)
                    .body(new LockedResponse("locked", locked.retryAfterSeconds()));
        };
    }

    @PostMapping("/recover")
    public ResponseEntity<?> recover(@RequestBody RecoverRequest request) {
        RecoverResult result = localSessionService.recover(
                request.recoveryCredentialId(), request.recoverySecret(), request.targetCredentialId());
        return switch (result) {
            case RecoverResult.Success ignored -> ResponseEntity.ok(new RecoverResponse(true));
            case RecoverResult.InvalidRecoveryCredential ignored -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("invalid_recovery_credential"));
        };
    }
}
