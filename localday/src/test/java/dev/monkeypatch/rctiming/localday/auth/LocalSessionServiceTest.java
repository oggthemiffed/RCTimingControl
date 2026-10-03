package dev.monkeypatch.rctiming.localday.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * Pure unit tests for {@link LocalSessionService}'s login/lockout/recovery/session-validation
 * logic. Mirrors the style of {@code RaceStateMachineServiceTest} — plain JUnit 5 + AssertJ, no
 * Spring context. {@link LocalCredentialRepository}, {@link LocalSessionRepository}, and
 * {@link PasswordEncoder} are all mocked; the encoder is mocked with plain-equality {@code
 * matches} semantics rather than real BCrypt so these tests stay fast and isolate this class's
 * own branching logic (real BCrypt hashing behavior is exercised separately in
 * {@code LocalAuthControllerIT}).
 */
class LocalSessionServiceTest {

    private LocalCredentialRepository credentialRepository;
    private LocalSessionRepository sessionRepository;
    private PasswordEncoder passwordEncoder;
    private LocalSessionService service;

    @BeforeEach
    void setUp() {
        credentialRepository = Mockito.mock(LocalCredentialRepository.class);
        sessionRepository = Mockito.mock(LocalSessionRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        service = new LocalSessionService(credentialRepository, sessionRepository, passwordEncoder);

        // save(...) round-trips the argument, like a real JPA repository would within one
        // in-memory "row".
        Mockito.when(credentialRepository.save(any(LocalCredential.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(sessionRepository.save(any(LocalSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private LocalCredential credential(Long id, String hash, boolean recovery) {
        LocalCredential c = new LocalCredential();
        c.setId(id);
        c.setOfficialName("Jane Doe");
        c.setSecretHash(hash);
        c.setRecovery(recovery);
        c.setFailedAttemptCount(0);
        c.setLockedUntil(null);
        return c;
    }

    // --- Happy path ---

    @Test
    void login_correctSecret_returnsSuccessAndIssuesSession() {
        LocalCredential cred = credential(1L, "hashed-1234", false);
        Mockito.when(credentialRepository.findById(1L)).thenReturn(Optional.of(cred));
        Mockito.when(passwordEncoder.matches("1234", "hashed-1234")).thenReturn(true);

        LoginResult result = service.login(1L, "1234");

        assertThat(result).isInstanceOf(LoginResult.Success.class);
        LoginResult.Success success = (LoginResult.Success) result;
        assertThat(success.officialName()).isEqualTo("Jane Doe");
        assertThat(success.credentialId()).isEqualTo(1L);
        assertThat(success.sessionToken()).isNotBlank();

        assertThat(cred.getFailedAttemptCount()).isEqualTo(0);
        assertThat(cred.getLockedUntil()).isNull();

        ArgumentCaptor<LocalSession> sessionCaptor = ArgumentCaptor.forClass(LocalSession.class);
        Mockito.verify(sessionRepository).save(sessionCaptor.capture());
        LocalSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getSessionToken()).isEqualTo(success.sessionToken());
        assertThat(savedSession.getCredentialId()).isEqualTo(1L);
        assertThat(savedSession.getOfficialName()).isEqualTo("Jane Doe");
        assertThat(savedSession.getIssuedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void login_issuedSessionToken_thenValidatesSuccessfully() {
        LocalCredential cred = credential(1L, "hashed-1234", false);
        Mockito.when(credentialRepository.findById(1L)).thenReturn(Optional.of(cred));
        Mockito.when(passwordEncoder.matches("1234", "hashed-1234")).thenReturn(true);

        LoginResult.Success success = (LoginResult.Success) service.login(1L, "1234");

        LocalSession persisted = new LocalSession();
        persisted.setId(99L);
        persisted.setCredentialId(1L);
        persisted.setOfficialName("Jane Doe");
        persisted.setSessionToken(success.sessionToken());
        persisted.setIssuedAt(Instant.now());
        Mockito.when(sessionRepository.findBySessionToken(success.sessionToken()))
                .thenReturn(Optional.of(persisted));

        Optional<SessionPrincipal> principal = service.validateSession(success.sessionToken());

        assertThat(principal).isPresent();
        assertThat(principal.get().credentialId()).isEqualTo(1L);
        assertThat(principal.get().officialName()).isEqualTo("Jane Doe");
    }

    @Test
    void login_unknownCredentialId_returnsInvalidCredential() {
        Mockito.when(credentialRepository.findById(42L)).thenReturn(Optional.empty());

        LoginResult result = service.login(42L, "0000");

        assertThat(result).isInstanceOf(LoginResult.InvalidCredential.class);
        Mockito.verifyNoInteractions(sessionRepository);
    }

    // --- Edge case: session expiry ---

    @Test
    void validateSession_issuedAtMoreThan18HoursAgo_returnsEmptyButRowNotDeleted() {
        LocalSession expired = new LocalSession();
        expired.setId(7L);
        expired.setCredentialId(1L);
        expired.setOfficialName("Jane Doe");
        expired.setSessionToken("expired-token");
        expired.setIssuedAt(Instant.now().minus(java.time.Duration.ofHours(18).plusMinutes(1)));
        Mockito.when(sessionRepository.findBySessionToken("expired-token")).thenReturn(Optional.of(expired));

        Optional<SessionPrincipal> principal = service.validateSession("expired-token");

        assertThat(principal).isEmpty();
        Mockito.verify(sessionRepository, Mockito.never()).delete(any());
        Mockito.verify(sessionRepository, Mockito.never()).deleteById(any());
    }

    @Test
    void validateSession_issuedAtJustUnder18HoursAgo_stillValid() {
        LocalSession notYetExpired = new LocalSession();
        notYetExpired.setId(8L);
        notYetExpired.setCredentialId(1L);
        notYetExpired.setOfficialName("Jane Doe");
        notYetExpired.setSessionToken("fresh-token");
        notYetExpired.setIssuedAt(Instant.now().minus(java.time.Duration.ofHours(17).plusMinutes(59)));
        Mockito.when(sessionRepository.findBySessionToken("fresh-token")).thenReturn(Optional.of(notYetExpired));

        Optional<SessionPrincipal> principal = service.validateSession("fresh-token");

        assertThat(principal).isPresent();
    }

    @Test
    void validateSession_unknownToken_returnsEmpty() {
        Mockito.when(sessionRepository.findBySessionToken("nope")).thenReturn(Optional.empty());

        assertThat(service.validateSession("nope")).isEmpty();
    }

    // --- Edge case: lockout ---

    @Test
    void login_wrongSecret_belowThreshold_returnsInvalidCredentialWithoutLocking() {
        LocalCredential cred = credential(1L, "hashed-1234", false);
        Mockito.when(credentialRepository.findById(1L)).thenReturn(Optional.of(cred));
        Mockito.when(passwordEncoder.matches(Mockito.anyString(), Mockito.eq("hashed-1234"))).thenReturn(false);

        LoginResult first = service.login(1L, "wrong");
        assertThat(first).isInstanceOf(LoginResult.InvalidCredential.class);
        assertThat(cred.getFailedAttemptCount()).isEqualTo(1);
        assertThat(cred.getLockedUntil()).isNull();

        LoginResult second = service.login(1L, "wrong");
        assertThat(second).isInstanceOf(LoginResult.InvalidCredential.class);
        assertThat(cred.getFailedAttemptCount()).isEqualTo(2);
        assertThat(cred.getLockedUntil()).isNull();
    }

    @Test
    void login_repeatedWrongSecret_crossesThreshold_locksAccount_butThisAttemptStillReturnsInvalidCredential() {
        LocalCredential cred = credential(1L, "hashed-1234", false);
        Mockito.when(credentialRepository.findById(1L)).thenReturn(Optional.of(cred));
        Mockito.when(passwordEncoder.matches(Mockito.anyString(), Mockito.eq("hashed-1234"))).thenReturn(false);

        service.login(1L, "wrong"); // 1
        service.login(1L, "wrong"); // 2
        LoginResult third = service.login(1L, "wrong"); // 3 - crosses LOCK_THRESHOLD

        // The attempt that itself crosses the threshold is still reported as invalid_credential,
        // not locked — the *next* attempt is what should see Locked.
        assertThat(third).isInstanceOf(LoginResult.InvalidCredential.class);
        assertThat(cred.getFailedAttemptCount()).isEqualTo(3);
        assertThat(cred.getLockedUntil()).isNotNull().isAfter(Instant.now());
    }

    @Test
    void login_subsequentAttemptAfterLockCrossed_returnsLockedEvenWithCorrectSecret() {
        LocalCredential cred = credential(1L, "hashed-1234", false);
        Mockito.when(credentialRepository.findById(1L)).thenReturn(Optional.of(cred));
        Mockito.when(passwordEncoder.matches("wrong", "hashed-1234")).thenReturn(false);
        Mockito.when(passwordEncoder.matches("1234", "hashed-1234")).thenReturn(true);

        service.login(1L, "wrong");
        service.login(1L, "wrong");
        service.login(1L, "wrong"); // locks

        // A CORRECT secret submitted while still locked must still return Locked, not Success.
        LoginResult afterLock = service.login(1L, "1234");
        assertThat(afterLock).isInstanceOf(LoginResult.Locked.class);
        assertThat(((LoginResult.Locked) afterLock).retryAfterSeconds()).isGreaterThanOrEqualTo(0);

        Mockito.verify(sessionRepository, Mockito.never()).save(any());
    }

    @Test
    void login_lockExpired_allowsRetryToProceedToSecretCheck() {
        LocalCredential cred = credential(1L, "hashed-1234", false);
        cred.setFailedAttemptCount(3);
        cred.setLockedUntil(Instant.now().minusSeconds(5)); // lock already expired
        Mockito.when(credentialRepository.findById(1L)).thenReturn(Optional.of(cred));
        Mockito.when(passwordEncoder.matches("1234", "hashed-1234")).thenReturn(true);

        LoginResult result = service.login(1L, "1234");

        assertThat(result).isInstanceOf(LoginResult.Success.class);
        assertThat(cred.getFailedAttemptCount()).isEqualTo(0);
        assertThat(cred.getLockedUntil()).isNull();
    }

    // --- Edge case: recovery ---

    @Test
    void recover_validRecoveryCredential_clearsTargetLockoutState() {
        LocalCredential recoveryCred = credential(2L, "hashed-recovery", true);
        LocalCredential target = credential(1L, "hashed-1234", false);
        target.setFailedAttemptCount(5);
        target.setLockedUntil(Instant.now().plusSeconds(120));

        Mockito.when(credentialRepository.findById(2L)).thenReturn(Optional.of(recoveryCred));
        Mockito.when(credentialRepository.findById(1L)).thenReturn(Optional.of(target));
        Mockito.when(passwordEncoder.matches("5678", "hashed-recovery")).thenReturn(true);

        RecoverResult result = service.recover(2L, "5678", 1L);

        assertThat(result).isInstanceOf(RecoverResult.Success.class);
        assertThat(target.getFailedAttemptCount()).isEqualTo(0);
        assertThat(target.getLockedUntil()).isNull();
    }

    @Test
    void recover_wrongRecoverySecret_fails_doesNotUnlockTarget() {
        LocalCredential recoveryCred = credential(2L, "hashed-recovery", true);
        LocalCredential target = credential(1L, "hashed-1234", false);
        target.setFailedAttemptCount(5);
        target.setLockedUntil(Instant.now().plusSeconds(120));

        Mockito.when(credentialRepository.findById(2L)).thenReturn(Optional.of(recoveryCred));
        Mockito.when(passwordEncoder.matches("badsecret", "hashed-recovery")).thenReturn(false);

        RecoverResult result = service.recover(2L, "badsecret", 1L);

        assertThat(result).isInstanceOf(RecoverResult.InvalidRecoveryCredential.class);
        assertThat(target.getFailedAttemptCount()).isEqualTo(5);
        assertThat(target.getLockedUntil()).isNotNull();
        Mockito.verify(credentialRepository, Mockito.never()).findById(1L);
    }

    @Test
    void recover_nonRecoveryCredentialUsedAsRecoveryCredential_fails() {
        LocalCredential notRecovery = credential(3L, "hashed-notrecovery", false);
        Mockito.when(credentialRepository.findById(3L)).thenReturn(Optional.of(notRecovery));

        RecoverResult result = service.recover(3L, "whatever", 1L);

        assertThat(result).isInstanceOf(RecoverResult.InvalidRecoveryCredential.class);
        Mockito.verify(passwordEncoder, Mockito.never()).matches(any(), any());
    }

    @Test
    void recover_nonexistentRecoveryCredentialId_fails() {
        Mockito.when(credentialRepository.findById(999L)).thenReturn(Optional.empty());

        RecoverResult result = service.recover(999L, "whatever", 1L);

        assertThat(result).isInstanceOf(RecoverResult.InvalidRecoveryCredential.class);
    }

    @Test
    void recover_nonexistentTargetCredential_failsWithoutInventingThirdResponseShape() {
        LocalCredential recoveryCred = credential(2L, "hashed-recovery", true);
        Mockito.when(credentialRepository.findById(2L)).thenReturn(Optional.of(recoveryCred));
        Mockito.when(credentialRepository.findById(1L)).thenReturn(Optional.empty());
        Mockito.when(passwordEncoder.matches("5678", "hashed-recovery")).thenReturn(true);

        RecoverResult result = service.recover(2L, "5678", 1L);

        assertThat(result).isInstanceOf(RecoverResult.InvalidRecoveryCredential.class);
    }

    @Test
    void listOfficials_mapsRowsWithoutExposingSecretsOrLockoutState() {
        LocalCredential c1 = credential(1L, "hashed-1234", false);
        LocalCredential c2 = credential(2L, "hashed-recovery", true);
        Mockito.when(credentialRepository.findAll()).thenReturn(List.of(c1, c2));

        List<OfficialSummary> officials = service.listOfficials();

        assertThat(officials).hasSize(2);
        assertThat(officials.get(0).credentialId()).isEqualTo(1L);
        assertThat(officials.get(0).recovery()).isFalse();
        assertThat(officials.get(1).credentialId()).isEqualTo(2L);
        assertThat(officials.get(1).recovery()).isTrue();
    }
}
