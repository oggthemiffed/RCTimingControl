package dev.monkeypatch.rctiming.query.competitor;

import java.time.Instant;

/** One change to a competitor: when, who, and the spoken name before and after (null means none). */
public record CompetitorChangeDto(
        Instant at,
        String by,
        String action,
        String before,
        String after) {
}
