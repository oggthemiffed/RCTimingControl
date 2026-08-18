package dev.monkeypatch.rctiming.localday.auth;

/**
 * The authenticated official resolved from a valid, unexpired session token. This module has no
 * stacked-role model yet (unlike the cloud's ADMIN/RACE_DIRECTOR/REFEREE) — R9 only distinguishes
 * "authenticated official" from "anonymous," so there is nothing here beyond identity.
 */
public record SessionPrincipal(Long credentialId, String officialName) {
}
