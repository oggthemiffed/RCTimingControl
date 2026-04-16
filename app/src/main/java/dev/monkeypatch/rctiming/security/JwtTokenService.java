package dev.monkeypatch.rctiming.security;

import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;

/**
 * JWT token creation and validation using JJWT 0.12.x API.
 * Uses HMAC-SHA256 signing. All builder methods use the 0.12.x fluent API
 * (.subject(), .claim(), .expiration()) — NOT the deprecated 0.11 setters.
 */
@Service
public class JwtTokenService {

    // SecretKey is from Java SE (java.security / javax.crypto) — not a Jakarta EE namespace.
    // There is no jakarta.crypto equivalent; this import is correct for Spring Boot 3.x.
    private final io.jsonwebtoken.security.MacAlgorithm signingAlg;
    private final java.security.Key signingKey;
    private final long accessTokenTtlMs;
    private final long refreshTokenTtlMs;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.access-token-ttl-ms}") long accessTokenTtlMs,
            @Value("${app.jwt.refresh-token-ttl-ms}") long refreshTokenTtlMs) {
        this.signingAlg = Jwts.SIG.HS256;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessTokenTtlMs = accessTokenTtlMs;
        this.refreshTokenTtlMs = refreshTokenTtlMs;
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("roles", user.getRoles().stream().map(Role::name).toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenTtlMs))
                .signWith(signingKey, signingAlg)
                .compact();
    }

    public String generateRefreshTokenValue() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public Claims parseToken(String token) {
        // Keys.hmacShaKeyFor always returns a SecretKey — safe unchecked cast
        @SuppressWarnings("unchecked")
        var secretKey = (javax.crypto.SecretKey) signingKey;
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getRefreshTokenTtlMs() {
        return refreshTokenTtlMs;
    }
}
