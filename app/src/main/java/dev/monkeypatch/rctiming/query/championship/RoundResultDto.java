package dev.monkeypatch.rctiming.query.championship;

public record RoundResultDto(
        int roundNumber,
        Long eventId,
        String eventName,
        int position,        // final position in that event (0 when not raced / excluded)
        int points,          // points awarded (0 when excluded / DNS / DQ)
        boolean excluded,    // CHAMP-02 / CHAMP-09 — admin exclusion for this round
        boolean dropped      // CHAMP-01 — worst rounds dropped below best-X-from-Y cutoff
) {}
