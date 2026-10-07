package dev.monkeypatch.rctiming.query.competitor;

/** A competitor as officials see it in pickers and lists (L5, #13). */
public record CompetitorSummaryDto(
        Long id,
        String displayName,
        String brcaNumber,
        String homeClub,
        /** How the name is said aloud, or null when the display name is spoken as written (#119). */
        String spokenName,
        /** What the announcer says: the spoken name when set, else the display name tidied for speech (#120). */
        String speechName) {
}
