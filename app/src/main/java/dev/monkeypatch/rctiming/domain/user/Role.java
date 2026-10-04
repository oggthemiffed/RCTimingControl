package dev.monkeypatch.rctiming.domain.user;

public enum Role {
    ADMIN, RACE_DIRECTOR, REFEREE;

    /** Only officials can sign in (L10, #18). */
    public static final java.util.Set<Role> OFFICIAL_ROLES = java.util.EnumSet.of(ADMIN, RACE_DIRECTOR, REFEREE);
}
