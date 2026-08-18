package dev.monkeypatch.rctiming.localday.auth;

/**
 * Public listing row for {@code GET /api/v1/local-auth/officials} — names only, never secrets,
 * never lockout state, so this can be served unauthenticated for the login picker.
 */
public record OfficialSummary(Long credentialId, String officialName, boolean recovery) {
}
