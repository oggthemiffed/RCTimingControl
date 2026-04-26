package dev.monkeypatch.rctiming.forwarder;

import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public ForwarderTokenService(ForwarderTokenRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public record GenerateResult(String plaintext, ForwarderToken persisted) {}

    public record CurrentStatus(ForwarderTokenStatus status, Instant generatedAt, Instant revokedAt) {}

    public GenerateResult generate() {
        repo.findAllByStatus(ACTIVE).forEach(t -> {
            t.setStatus(REVOKED);
            t.setRevokedAt(Instant.now());
        });
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String plaintext = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ForwarderToken token = new ForwarderToken();
        token.setTokenHash(passwordEncoder.encode(plaintext));
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
                .filter(t -> passwordEncoder.matches(plaintext, t.getTokenHash()))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public CurrentStatus getCurrentStatus() {
        var active = repo.findFirstByStatusOrderByGeneratedAtDesc(ACTIVE);
        if (active.isPresent()) {
            return new CurrentStatus(ACTIVE, active.get().getGeneratedAt(), null);
        }
        var any = repo.findFirstByOrderByGeneratedAtDesc();
        return any.map(t -> new CurrentStatus(REVOKED, t.getGeneratedAt(), t.getRevokedAt()))
                  .orElse(new CurrentStatus(null, null, null));
    }
}
