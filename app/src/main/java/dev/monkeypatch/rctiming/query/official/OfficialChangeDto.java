package dev.monkeypatch.rctiming.query.official;

import java.time.Instant;

/**
 * One change to an official, newest first on the Officials page (#61). {@code actorName} is null
 * when the change came from the laptop's command line.
 */
public record OfficialChangeDto(
        long id,
        Instant at,
        long officialId,
        String officialName,
        String action,
        String detail,
        Long actorId,
        String actorName
) {}
