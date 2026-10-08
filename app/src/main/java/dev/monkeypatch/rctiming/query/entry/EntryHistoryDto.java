package dev.monkeypatch.rctiming.query.entry;

import java.time.Instant;

/**
 * One thing that happened to an entry.
 *
 * @param actor  the official, or the system job, that did it; null when the record never held one
 * @param reason what the official gave as the reason, when the action asks for one
 */
public record EntryHistoryDto(Instant at, String actor, String summary, String reason) {
}
