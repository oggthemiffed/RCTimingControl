package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CompetitorService {

    /** External source for competitors that stand in for a login user. */
    public static final String RCTC_USER_SOURCE = "RCTC_USER";

    private final CompetitorRepository competitorRepository;

    public CompetitorService(CompetitorRepository competitorRepository) {
        this.competitorRepository = competitorRepository;
    }

    /**
     * Returns the competitor for a racer's login, creating it on first use. This is the bridge
     * until RaceHub supplies competitors (L7). Matches the key the V30 backfill uses.
     */
    @Transactional
    public Competitor forUser(User user) {
        String externalId = user.getId().toString();
        return competitorRepository.findByExternalSourceAndExternalId(RCTC_USER_SOURCE, externalId)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    Competitor competitor = new Competitor();
                    competitor.setDisplayName((user.getFirstName() + " " + user.getLastName()).trim());
                    competitor.setExternalSource(RCTC_USER_SOURCE);
                    competitor.setExternalId(externalId);
                    competitor.setCreatedAt(now);
                    competitor.setUpdatedAt(now);
                    return competitorRepository.save(competitor);
                });
    }
}
