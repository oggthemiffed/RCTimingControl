package dev.monkeypatch.rctiming.localday.sync.dto;

import java.time.Instant;

/** {@code cloudRaceId} is null for a passing not yet attached to an active race locally (e.g.
 * practice, or a decoder still transmitting between races) — the cloud still durably receives it. */
public record LapPassingSummary(String transponderNumber, Long cloudRaceId, Instant passingAt,
                                 Long lapTimeMs, Integer lapNumber) {
}
