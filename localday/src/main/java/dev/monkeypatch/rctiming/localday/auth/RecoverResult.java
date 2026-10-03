package dev.monkeypatch.rctiming.localday.auth;

/**
 * Outcome of a {@link LocalSessionService#recover} attempt. A recovery credential "unlocks," it
 * does not "impersonate" (design decision 5) — success only clears the target credential's
 * lockout state; it never logs in as the target or issues a session for them.
 */
public sealed interface RecoverResult {

    record Success() implements RecoverResult {
    }

    record InvalidRecoveryCredential() implements RecoverResult {
    }
}
