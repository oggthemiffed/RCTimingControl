package dev.monkeypatch.rctiming.query.competitor;

/** A competitor as officials see it in pickers and lists (L5, #13). */
public record CompetitorSummaryDto(
        Long id,
        String displayName,
        String brcaNumber,
        String homeClub) {
}
