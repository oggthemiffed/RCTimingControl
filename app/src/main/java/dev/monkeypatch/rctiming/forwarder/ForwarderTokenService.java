package dev.monkeypatch.rctiming.forwarder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static dev.monkeypatch.rctiming.forwarder.ForwarderTokenStatus.ACTIVE;
import static dev.monkeypatch.rctiming.forwarder.ForwarderTokenStatus.REVOKED;

@Service
@Transactional
public class ForwarderTokenService {

    private final ForwarderTokenRepository repo;
    private final SecureRandom secureRandom = new SecureRandom();

    public ForwarderTokenService(ForwarderTokenRepository repo) {
        this.repo = repo;
    }

    public record GenerateResult(String plaintext, ForwarderToken persisted) {}

    public record CurrentStatus(ForwarderTokenStatus status, Instant generatedAt, Instant revokedAt, String tokenValue) {}

    public GenerateResult generate() {
        repo.findAllByStatus(ACTIVE).forEach(t -> {
            t.setStatus(REVOKED);
            t.setRevokedAt(Instant.now());
        });
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String plaintext = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ForwarderToken token = new ForwarderToken();
        token.setTokenHash(plaintext); // kept for schema compatibility; value is the token itself
        token.setTokenValue(plaintext);
        token.setStatus(ACTIVE);
        token.setGeneratedAt(Instant.now());
        repo.save(token);
        return new GenerateResult(plaintext, token);
    }

    public void revoke() {
        repo.findFirstByStatusOrderByGeneratedAtDesc(ACTIVE).ifPresent(t -> {
            t.setStatus(REVOKED);
            t.setRevokedAt(Instant.now());
        });
    }

    @Transactional(readOnly = true)
    public Optional<ForwarderToken> validate(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return Optional.empty();
        return repo.findAllByStatus(ACTIVE).stream()
                .filter(t -> plaintext.equals(t.getTokenValue()))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public CurrentStatus getCurrentStatus() {
        var active = repo.findFirstByStatusOrderByGeneratedAtDesc(ACTIVE);
        if (active.isPresent()) {
            var t = active.get();
            return new CurrentStatus(ACTIVE, t.getGeneratedAt(), null, t.getTokenValue());
        }
        var any = repo.findFirstByOrderByGeneratedAtDesc();
        return any.map(t -> new CurrentStatus(REVOKED, t.getGeneratedAt(), t.getRevokedAt(), null))
                  .orElse(new CurrentStatus(null, null, null, null));
    }
}
