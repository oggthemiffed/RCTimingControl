package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CompetitorService {

    /** External source for competitors that stand in for a login user. */
    public static final String RCTC_USER_SOURCE = "RCTC_USER";

    private final CompetitorRepository competitorRepository;
    private final UserRepository userRepository;

    public CompetitorService(CompetitorRepository competitorRepository, UserRepository userRepository) {
        this.competitorRepository = competitorRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns the competitor for a racer's login, creating it on first use. This is the bridge
     * until RaceHub supplies competitors (L7). Matches the key the V30 backfill uses.
     *
     * <p>The user row is locked for the rest of the caller's transaction. Two first-time requests
     * for the same racer therefore run one after the other, and the second finds the competitor
     * the first one created, instead of inserting a duplicate.
     */
    @Transactional
    public Competitor forUser(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new EntityNotFoundException("Racer not found"));
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

    /** Creates a competitor with no external identity, for a walk-in entered by hand (L9). */
    @Transactional
    public Competitor createWalkIn(String displayName) {
        Instant now = Instant.now();
        Competitor competitor = new Competitor();
        competitor.setDisplayName(displayName.trim());
        competitor.setCreatedAt(now);
        competitor.setUpdatedAt(now);
        return competitorRepository.save(competitor);
    }
}
