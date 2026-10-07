package dev.monkeypatch.rctiming.domain.competitor;

import java.util.List;

/** Two competitors can't be merged as they are; the reasons are listed. The API answers 409. */
public class CompetitorMergeRefusedException extends RuntimeException {

    private final transient List<String> blockers;

    public CompetitorMergeRefusedException(List<String> blockers) {
        super(blockers.get(0));
        this.blockers = List.copyOf(blockers);
    }

    public List<String> getBlockers() {
        return blockers;
    }
}
