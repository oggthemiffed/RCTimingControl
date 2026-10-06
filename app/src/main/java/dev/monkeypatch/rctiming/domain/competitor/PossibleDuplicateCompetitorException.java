package dev.monkeypatch.rctiming.domain.competitor;

import java.util.List;

/**
 * A walk-in's typed name matches competitors that already exist, so the official must say whether it
 * is one of them or a different person (#123). The API answers 409 with the matches.
 */
public class PossibleDuplicateCompetitorException extends RuntimeException {

    private final transient List<Competitor> matches;

    public PossibleDuplicateCompetitorException(List<Competitor> matches) {
        super(matches.get(0).getDisplayName() + " already exists. Use them, or confirm this is a different person.");
        this.matches = List.copyOf(matches);
    }

    public List<Competitor> getMatches() {
        return matches;
    }
}
