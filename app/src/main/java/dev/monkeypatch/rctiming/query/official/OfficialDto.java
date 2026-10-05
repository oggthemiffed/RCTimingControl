package dev.monkeypatch.rctiming.query.official;

import java.time.Instant;
import java.util.List;

/** One official as the Officials page lists them (#61). */
public record OfficialDto(
        long id,
        String email,
        String firstName,
        String lastName,
        List<String> roles,
        boolean enabled,
        Instant disabledAt,
        Instant createdAt
) {}
