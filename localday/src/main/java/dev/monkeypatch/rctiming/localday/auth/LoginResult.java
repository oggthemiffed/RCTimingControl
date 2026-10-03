package dev.monkeypatch.rctiming.localday.auth;

/**
 * Outcome of a {@link LocalSessionService#login} attempt. Sealed so
 * {@link LocalAuthController} can exhaustively map each variant to the exact HTTP status the
 * fixed API contract specifies (200 / 401 / 423) with a switch expression.
 */
public sealed interface LoginResult {

    record Success(String sessionToken, String officialName, Long credentialId) implements LoginResult {
    }

    record InvalidCredential() implements LoginResult {
    }

    record Locked(long retryAfterSeconds) implements LoginResult {
    }
}
