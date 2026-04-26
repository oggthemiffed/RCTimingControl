package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Plan 03: ForwarderTokenService integration tests using Testcontainers. */
class ForwarderTokenServiceTest extends AbstractIntegrationTest {

    @Autowired
    ForwarderTokenService service;

    @Autowired
    ForwarderTokenRepository repo;

    @Test
    void generateTokenReturnsPlaintextOnce() {
        var result = service.generate();
        assertThat(result.plaintext()).hasSizeBetween(40, 50);
        assertThat(result.persisted().getTokenHash()).isNotEqualTo(result.plaintext());
    }

    @Test
    void validateAcceptsActiveToken() {
        var result = service.generate();
        var found = service.validate(result.plaintext());
        assertThat(found).isPresent();
    }

    @Test
    void validateRejectsRevokedToken() {
        var result = service.generate();
        service.revoke();
        var found = service.validate(result.plaintext());
        assertThat(found).isEmpty();
    }

    @Test
    void validateRejectsInvalidToken() {
        var found = service.validate("nonsense");
        assertThat(found).isEmpty();
    }

    @Test
    void regeneratePreviousTokenIsRevoked() {
        var first = service.generate();
        Long firstId = first.persisted().getId();
        service.generate();
        var firstReloaded = repo.findById(firstId).orElseThrow();
        assertThat(firstReloaded.getStatus()).isEqualTo(ForwarderTokenStatus.REVOKED);
    }

    @Test
    void tokenStoredAsBcryptHashNotPlaintext() {
        var result = service.generate();
        var entity = repo.findById(result.persisted().getId()).orElseThrow();
        assertThat(entity.getTokenHash()).matches("\\$2[ab]\\$.*");
        assertThat(entity.getTokenHash()).isNotEqualTo(result.plaintext());
    }
}
