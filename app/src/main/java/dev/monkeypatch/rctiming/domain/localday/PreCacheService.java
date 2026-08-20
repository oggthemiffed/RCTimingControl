package dev.monkeypatch.rctiming.domain.localday;

import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Write side of pre-caching a local race day: mints officials' one-time PINs and the
 * per-instance secret used to authenticate the local day service against the cloud.
 * The read-side entries/schedule projection lives in {@code query.localday.PreCacheQuery} (jOOQ).
 */
@Service
@Transactional
public class PreCacheService {

    private final UserRepository userRepository;
    private final LocaldayCredentialRepository localdayCredentialRepository;
    private final LocaldayInstanceSecretRepository localdayInstanceSecretRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PreCacheService(UserRepository userRepository,
                            LocaldayCredentialRepository localdayCredentialRepository,
                            LocaldayInstanceSecretRepository localdayInstanceSecretRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.localdayCredentialRepository = localdayCredentialRepository;
        this.localdayInstanceSecretRepository = localdayInstanceSecretRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Internal holder carrying everything the controller needs to assemble the response DTO. */
    public record MintResult(
            List<CredentialRow> officialCredentials,
            String instanceId,
            String instanceSecret
    ) {}

    public record CredentialRow(Long cloudUserId, String officialName, String pin) {}

    /**
     * Mints officials' one-time PINs and a fresh instance secret for the given event.
     * Callers must validate {@code instanceId} before invoking this method — it has side
     * effects (credential/secret rows are persisted) and performs no validation of its own.
     */
    public MintResult mintCredentialsAndSecret(Long eventId, String instanceId) {
        // --- Mint officials' credentials ---
        List<User> officials = userRepository.findByRolesIn(Set.of(Role.ADMIN, Role.RACE_DIRECTOR, Role.REFEREE));
        List<CredentialRow> credentialRows = new ArrayList<>();
        for (User official : officials) {
            String pin = String.format("%06d", secureRandom.nextInt(1_000_000));
            String hash = passwordEncoder.encode(pin);

            upsertCredential(eventId, official.getId(), hash);

            credentialRows.add(new CredentialRow(
                    official.getId(),
                    official.getFirstName() + " " + official.getLastName(),
                    pin
            ));
        }

        // --- Mint instance secret ---
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        String instanceSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        String instanceSecretHash = passwordEncoder.encode(instanceSecret);

        upsertInstanceSecret(eventId, instanceId, instanceSecretHash);

        return new MintResult(credentialRows, instanceId, instanceSecret);
    }

    private void upsertCredential(Long eventId, Long userId, String secretHash) {
        Instant now = Instant.now();
        Optional<LocaldayCredential> existing = localdayCredentialRepository.findByEventIdAndUserId(eventId, userId);
        if (existing.isPresent()) {
            LocaldayCredential credential = existing.get();
            credential.setSecretHash(secretHash);
            credential.setIssuedAt(now);
            localdayCredentialRepository.save(credential);
            return;
        }
        LocaldayCredential credential = new LocaldayCredential();
        credential.setEventId(eventId);
        credential.setUserId(userId);
        credential.setSecretHash(secretHash);
        credential.setIssuedAt(now);
        try {
            localdayCredentialRepository.saveAndFlush(credential);
        } catch (DataIntegrityViolationException e) {
            LocaldayCredential winner = localdayCredentialRepository.findByEventIdAndUserId(eventId, userId)
                    .orElseThrow();
            winner.setSecretHash(secretHash);
            winner.setIssuedAt(now);
            localdayCredentialRepository.save(winner);
        }
    }

    private void upsertInstanceSecret(Long eventId, String instanceId, String secretHash) {
        Instant now = Instant.now();
        Optional<LocaldayInstanceSecret> existing =
                localdayInstanceSecretRepository.findByEventIdAndInstanceId(eventId, instanceId);
        if (existing.isPresent()) {
            LocaldayInstanceSecret secretEntity = existing.get();
            secretEntity.setSecretHash(secretHash);
            secretEntity.setIssuedAt(now);
            localdayInstanceSecretRepository.save(secretEntity);
            return;
        }
        LocaldayInstanceSecret secretEntity = new LocaldayInstanceSecret();
        secretEntity.setEventId(eventId);
        secretEntity.setInstanceId(instanceId);
        secretEntity.setSecretHash(secretHash);
        secretEntity.setIssuedAt(now);
        try {
            localdayInstanceSecretRepository.saveAndFlush(secretEntity);
        } catch (DataIntegrityViolationException e) {
            LocaldayInstanceSecret winner = localdayInstanceSecretRepository
                    .findByEventIdAndInstanceId(eventId, instanceId)
                    .orElseThrow();
            winner.setSecretHash(secretHash);
            winner.setIssuedAt(now);
            localdayInstanceSecretRepository.save(winner);
        }
    }
}
