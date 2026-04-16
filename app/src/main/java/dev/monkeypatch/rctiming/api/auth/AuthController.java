package dev.monkeypatch.rctiming.api.auth;

import dev.monkeypatch.rctiming.domain.auth.PasswordResetService;
import dev.monkeypatch.rctiming.domain.auth.RefreshToken;
import dev.monkeypatch.rctiming.domain.auth.RefreshTokenRepository;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserService;
import dev.monkeypatch.rctiming.security.JwtTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService,
                          JwtTokenService jwtTokenService,
                          RefreshTokenRepository refreshTokenRepository,
                          PasswordResetService passwordResetService,
                          PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetService = passwordResetService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request,
                                                  HttpServletResponse response) {
        User user = userService.createRacer(
                request.email(), request.password(), request.firstName(), request.lastName());
        String accessToken = jwtTokenService.generateAccessToken(user);
        setRefreshCookie(user, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildAuthResponse(user, accessToken));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request,
                                               HttpServletResponse response) {
        Optional<User> userOpt = userService.findByEmail(request.email());
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
            // Generic message — never specify which field is wrong (credential enumeration prevention)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
        User user = userOpt.get();
        String accessToken = jwtTokenService.generateAccessToken(user);
        setRefreshCookie(user, response);
        return ResponseEntity.ok(buildAuthResponse(user, accessToken));
    }

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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshToken oldToken = tokenOpt.get();
        if (oldToken.isRevoked() || oldToken.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Token rotation: revoke old, issue new
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        User user = oldToken.getUser();
        String newAccessToken = jwtTokenService.generateAccessToken(user);
        setRefreshCookie(user, response);

        return ResponseEntity.ok(buildAuthResponse(user, newAccessToken));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @RequestBody @Valid PasswordResetRequestDto request) {
        // Always return 200 — security requirement: prevent email enumeration (T-01-11)
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @RequestBody @Valid PasswordResetConfirmDto request) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    // --- helpers ---

    private void setRefreshCookie(User user, HttpServletResponse response) {
        String rawToken = jwtTokenService.generateRefreshTokenValue();
        String tokenHash = sha256Hex(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtTokenService.getRefreshTokenTtlMs()));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", rawToken)
                .httpOnly(true)
                .secure(false)   // false for dev; override in prod via config
                .sameSite("Lax")
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofMillis(jwtTokenService.getRefreshTokenTtlMs()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
