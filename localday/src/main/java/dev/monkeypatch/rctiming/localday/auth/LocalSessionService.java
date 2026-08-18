package dev.monkeypatch.rctiming.localday.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Core orchestrator for the local day-scoped authentication flow (U6): validating a login
 * attempt against a picked {@link LocalCredential} row, tracking lockout state, handling
 * recovery unlocks, and validating session tokens on subsequent requests.
 *
 * <p>Deliberately independent of the cloud's JWT session model (KTD5) — see {@link LocalSession}
 * for the reasoning behind anchoring session expiry on this device's own wall clock.
 */
@Service
public class LocalSessionService {

    /**
     * Full-event-day session validity window. ~18 hours matches the plan's stated full-event-day
     * coverage figure — a race day plus setup/teardown margin, so an official logged in at the
     * start of the day stays authenticated through packing up.
     */
    static final Duration SESSION_VALIDITY = Duration.ofHours(18);

    /**
     * A single mistyped PIN should not lock anyone out instantly — lockout only begins once
     * failures reach this threshold (the 3rd consecutive failure), giving officials two free
     * retries for a fat-fingered entry before any lock is applied.
     */
    static final int LOCK_THRESHOLD = 3;

    /**
     * Exponential backoff cap. Lock duration is {@code min(2^failedAttemptCount, CAP)} seconds;
     * capping at 5 minutes keeps a runaway failure streak from locking an official out for an
     * unreasonably long stretch of a live race day, while still making brute-forcing a
     * low-entropy PIN impractical at venue timescales.
     */
    static final long LOCK_DURATION_CAP_SECONDS = 300;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final LocalCredentialRepository credentialRepository;
    private final LocalSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalSessionService(LocalCredentialRepository credentialRepository,
                                LocalSessionRepository sessionRepository,
                                PasswordEncoder passwordEncoder) {
        this.credentialRepository = credentialRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<OfficialSummary> listOfficials() {
        return credentialRepository.findAll().stream()
                .map(c -> new OfficialSummary(c.getId(), c.getOfficialName(), c.isRecovery()))
                .toList();
    }

    @Transactional
    public LoginResult login(Long credentialId, String secret) {
        Optional<LocalCredential> found = credentialRepository.findById(credentialId);
        if (found.isEmpty()) {
            return new LoginResult.InvalidCredential();
        }
        LocalCredential credential = found.get();

        Instant now = Instant.now();
        if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(now)) {
            // Already locked: this attempt (even with the correct secret) is rejected without
            // touching the failure counter further.
            long retryAfterSeconds = Math.max(0, Duration.between(now, credential.getLockedUntil()).toSeconds());
            return new LoginResult.Locked(retryAfterSeconds);
        }

        if (passwordEncoder.matches(secret, credential.getSecretHash())) {
            credential.setFailedAttemptCount(0);
            credential.setLockedUntil(null);
            credentialRepository.save(credential);

            LocalSession session = new LocalSession();
            session.setCredentialId(credential.getId());
            session.setOfficialName(credential.getOfficialName());
            session.setSessionToken(generateSessionToken());
            session.setIssuedAt(Instant.now());
            sessionRepository.save(session);

            return new LoginResult.Success(session.getSessionToken(), credential.getOfficialName(), credential.getId());
        }

        int failedAttemptCount = credential.getFailedAttemptCount() + 1;
        credential.setFailedAttemptCount(failedAttemptCount);
        if (failedAttemptCount >= LOCK_THRESHOLD) {
            long lockDurationSeconds = Math.min(1L << Math.min(failedAttemptCount, 20), LOCK_DURATION_CAP_SECONDS);
            credential.setLockedUntil(Instant.now().plusSeconds(lockDurationSeconds));
        }
        credentialRepository.save(credential);

        // A bad secret is always reported as invalid_credential — even if this very attempt just
        // crossed the lock threshold. It's the *next* attempt against this row that should see
        // 423 Locked.
        return new LoginResult.InvalidCredential();
    }

    @Transactional
    public RecoverResult recover(Long recoveryCredentialId, String recoverySecret, Long targetCredentialId) {
        Optional<LocalCredential> recoveryCredential = credentialRepository.findById(recoveryCredentialId);
        if (recoveryCredential.isEmpty()
                || !recoveryCredential.get().isRecovery()
                || !passwordEncoder.matches(recoverySecret, recoveryCredential.get().getSecretHash())) {
            return new RecoverResult.InvalidRecoveryCredential();
        }

        // A nonexistent target is a different failure than the recovery credential's own
        // problem, but the contract only documents two response shapes for this endpoint — reuse
        // the same 401 rather than inventing a third, since there is nothing meaningful to unlock.
        Optional<LocalCredential> target = credentialRepository.findById(targetCredentialId);
        if (target.isEmpty()) {
            return new RecoverResult.InvalidRecoveryCredential();
        }

        LocalCredential targetCredential = target.get();
        targetCredential.setFailedAttemptCount(0);
        targetCredential.setLockedUntil(null);
        credentialRepository.save(targetCredential);

        return new RecoverResult.Success();
    }

    @Transactional(readOnly = true)
    public Optional<SessionPrincipal> validateSession(String token) {
        Optional<LocalSession> found = sessionRepository.findBySessionToken(token);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        LocalSession session = found.get();
        if (Duration.between(session.getIssuedAt(), Instant.now()).compareTo(SESSION_VALIDITY) > 0) {
            // Expired — deliberately not deleted here; background cleanup is out of scope for
            // this unit and a stale row is harmless to leave behind.
            return Optional.empty();
        }
        return Optional.of(new SessionPrincipal(session.getCredentialId(), session.getOfficialName()));
    }

    private static String generateSessionToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
