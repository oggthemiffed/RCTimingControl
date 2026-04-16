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
 *
 * Note: {@code Keys.hmacShaKeyFor} returns {@code javax.crypto.SecretKey} which is
 * part of Java SE core (not a Jakarta EE namespace). It is stored as the JJWT-typed
 * field to satisfy {@code verifyWith(SecretKey)} at call sites.
 */
@Service
public class JwtTokenService {

    // Keys.hmacShaKeyFor returns SecretKey (Java SE javax.crypto — not a Jakarta EE namespace)
    private final javax.crypto.SecretKey signingKey;
    private final long accessTokenTtlMs;
    private final long refreshTokenTtlMs;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.access-token-ttl-ms}") long accessTokenTtlMs,
            @Value("${app.jwt.refresh-token-ttl-ms}") long refreshTokenTtlMs) {
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
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshTokenValue() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getRefreshTokenTtlMs() {
        return refreshTokenTtlMs;
    }
}
