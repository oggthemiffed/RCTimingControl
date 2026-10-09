package dev.monkeypatch.rctiming.api.auth;

import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;
import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.domain.auth.RefreshToken;
import dev.monkeypatch.rctiming.domain.auth.RefreshTokenRepository;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserService;
import dev.monkeypatch.rctiming.security.JwtTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final TransactionTemplate transactions;

    public AuthController(UserService userService,
                          JwtTokenService jwtTokenService,
                          RefreshTokenRepository refreshTokenRepository,
                          PasswordEncoder passwordEncoder,
                          AuditService audit,
                          TransactionTemplate transactions) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.transactions = transactions;
    }

    /** Signs in. Every attempt is recorded in the audit log, wrong passwords included. */
    @Audited("audit_log")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request,
                                               HttpServletResponse response) {
        Optional<User> userOpt = userService.findByEmail(request.email());
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
            // Generic message — never specify which field is wrong (credential enumeration prevention).
            // The audit row is just as vague about it: only an admin reads it, but it names no password.
            recordLoginRefused(request.email(), userOpt.map(User::getId).orElse(null), "wrong email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
        User user = userOpt.get();
        if (!user.isOfficial()) {
            // Only race officials sign in (L10, #18); the password was right, so say why
            recordLoginRefused(request.email(), user.getId(), "not an official");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!user.isEnabled()) {
            recordLoginRefused(request.email(), user.getId(), "account disabled");
            // An admin has disabled this official (#61); the password was right, so say why
            ProblemDetail disabled = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                    "This account has been disabled. Ask a club admin to enable it.");
            disabled.setProperty("reason", "disabled");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(disabled);
        }
        String accessToken = jwtTokenService.generateAccessToken(user);
        // A new sign-in starts a new family of refresh tokens. The token and the audit row are written in one
        // transaction, so there is no session without its record. Only this part is in a transaction: the
        // password check above is slow on purpose and must not hold the single write connection.
        NewRefreshToken refreshToken = newRefreshToken(user, UUID.randomUUID().toString());
        transactions.executeWithoutResult(status -> {
            refreshTokenRepository.save(refreshToken.entity());
            audit.entry(Actor.official(user.getId()), "LOGIN_SUCCEEDED")
                    .entity("official", user.getId())
                    .summary("Signed in")
                    .record();
        });
        setRefreshCookie(refreshToken.rawValue(), response);
        return ResponseEntity.ok(buildAuthResponse(user, accessToken));
    }

    /**
     * Swaps the refresh cookie for a new access token. Successful refreshes are not recorded (every page load
     * and every 15 minutes would add one); a refused one is, when a cookie was presented, because a token that
     * is unknown, expired or already used can mean a stolen or replayed cookie. No cookie at all is the normal
     * answer to an anonymous visitor and is not recorded.
     */
    @Audited("audit_log")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String rawCookieToken,
            HttpServletResponse response) {
        if (rawCookieToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String tokenHash = sha256Hex(rawCookieToken);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            return refuseRefresh(HttpStatus.UNAUTHORIZED, null, "unknown token");
        }

        RefreshToken oldToken = tokenOpt.get();
        if (oldToken.isRevoked()) {
            return refuseRefresh(HttpStatus.UNAUTHORIZED, oldToken.getUserId(), "token already used or revoked");
        }
        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            return refuseRefresh(HttpStatus.UNAUTHORIZED, oldToken.getUserId(), "token expired");
        }

        User user = userService.findById(oldToken.getUserId()).orElse(null);
        if (user == null) {
            return refuseRefresh(HttpStatus.UNAUTHORIZED, oldToken.getUserId(), "no such official");
        }
        if (!user.isOfficial()) {
            return refuseRefresh(HttpStatus.FORBIDDEN, user.getId(), "not an official");
        }
        if (!user.isEnabled()) {
            // Disabling revokes the tokens too (#61); this covers one issued in between
            return refuseRefresh(HttpStatus.UNAUTHORIZED, user.getId(), "account disabled");
        }

        // Token rotation: revoke the old token and store its replacement in the same family, in one
        // transaction. The old token is revoked with a compare-and-set, so a token used by two requests at
        // once (a double click, two tabs) is accepted for only one of them, and a sign-out that got in first
        // makes this fail rather than leaving a live token behind it.
        String familyId = oldToken.getFamilyId() != null ? oldToken.getFamilyId() : UUID.randomUUID().toString();
        NewRefreshToken replacement = newRefreshToken(user, familyId);
        if (!refreshTokenRepository.rotate(oldToken.getId(), replacement.entity())) {
            return refuseRefresh(HttpStatus.UNAUTHORIZED, user.getId(), "token already used or revoked");
        }
        String newAccessToken = jwtTokenService.generateAccessToken(user);
        setRefreshCookie(replacement.rawValue(), response);

        return ResponseEntity.ok(buildAuthResponse(user, newAccessToken));
    }

    /**
     * Signs this browser out: revokes the refresh token its cookie holds and expires the cookie. Without
     * it, clearing the page's in-memory access token would not sign anyone out, because the 7-day cookie
     * would simply issue a new one on the next load.
     *
     * <p>This is {@code DELETE /refresh} rather than a separate {@code /logout} URL because the cookie's
     * path is {@code /api/v1/auth/refresh}: the browser sends it only to that path. Open to anyone who can
     * send the cookie (the access token has usually expired by then) and idempotent: no cookie, an unknown
     * token or an already revoked one all answer 204. Only this browser's sign-in is ended, so the same
     * official stays signed in elsewhere. The access token already issued stays valid until it expires
     * (15 minutes).
     *
     * <p>Revokes the token's whole family, not just the token: a refresh that rotated it a moment earlier (the
     * browser's cookie may be one rotation behind) has already issued a replacement, and that is revoked
     * too. A refresh racing this call either lands first and is revoked here, or lands second and is refused
     * (see {@link RefreshTokenRepository#rotate}). The page also waits for its own in-flight refresh before
     * calling this (see {@code endSession} in the frontend's {@code lib/auth.ts}).
     */
    @Audited("audit_log")
    @Transactional
    @DeleteMapping("/refresh")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String rawCookieToken,
            HttpServletResponse response) {
        if (rawCookieToken != null) {
            refreshTokenRepository.findByTokenHash(sha256Hex(rawCookieToken)).ifPresent(token -> {
                // Only a sign-in that was still live is recorded, so a repeated sign-out adds nothing
                if (refreshTokenRepository.revokeFamily(token) > 0) {
                    audit.entry(Actor.official(token.getUserId()), "LOGOUT")
                            .entity("official", token.getUserId())
                            .summary("Signed out")
                            .record();
                }
            });
        }
        // Same name, path and attributes as the cookie that was set, with no lifetime, so the browser drops it
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString());
        return ResponseEntity.noContent().build();
    }

    // --- helpers ---

    private void recordLoginRefused(String emailTried, Long officialId, String reason) {
        audit.entry(Actor.anonymous(emailTried), "LOGIN_FAILED")
                .entity("official", officialId)
                .summary("Sign-in refused: " + reason)
                .recordStandalone();
    }

    /** Answers a refresh that was refused, recording it: someone presented a cookie that did not work. */
    private ResponseEntity<AuthResponse> refuseRefresh(HttpStatus status, Long officialId, String reason) {
        audit.entry(Actor.anonymous("refresh cookie"), "REFRESH_REFUSED")
                .entity("official", officialId)
                .summary("Refresh refused: " + reason)
                .recordStandalone();
        return ResponseEntity.status(status).build();
    }

    /** A refresh token ready to store, with the raw value the browser will hold (only its hash is stored). */
    private record NewRefreshToken(String rawValue, RefreshToken entity) {}

    private NewRefreshToken newRefreshToken(User user, String familyId) {
        String rawToken = jwtTokenService.generateRefreshTokenValue();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(sha256Hex(rawToken));
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtTokenService.getRefreshTokenTtlMs()));
        refreshToken.setRevoked(false);
        refreshToken.setFamilyId(familyId);
        return new NewRefreshToken(rawToken, refreshToken);
    }

    private void setRefreshCookie(String rawToken, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                refreshCookie(rawToken, Duration.ofMillis(jwtTokenService.getRefreshTokenTtlMs())).toString());
    }

    /**
     * The refresh cookie. Not marked Secure because RCTC is served over plain HTTP on the venue network
     * (there is no TLS); SameSite=Lax keeps it off cross-site POSTs, and the path keeps it off every other request.
     */
    private static ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/v1/auth/refresh")
                .maxAge(maxAge)
                .build();
    }

    private AuthResponse buildAuthResponse(User user, String accessToken) {
        return new AuthResponse(
                accessToken,
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream().map(Enum::name).toList()
        );
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
