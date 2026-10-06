package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CompetitorService {

    private final CompetitorRepository competitorRepository;

    public CompetitorService(CompetitorRepository competitorRepository) {
        this.competitorRepository = competitorRepository;
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

    /** The longest spoken name accepted; it is a short pronunciation hint, not free text. */
    public static final int MAX_SPOKEN_NAME_LENGTH = 100;

    /**
     * Sets how a competitor's name is said aloud, or clears it when blank (#119). Only this field changes,
     * so an import can still update the rest.
     *
     * @throws EntityNotFoundException if there is no such competitor
     * @throws IllegalArgumentException if the text is longer than {@link #MAX_SPOKEN_NAME_LENGTH}
     */
    @Transactional
    public Competitor setSpokenName(Long competitorId, String spokenName) {
        String text = spokenName == null ? "" : spokenName.trim();
        if (text.length() > MAX_SPOKEN_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "The spoken name can be at most " + MAX_SPOKEN_NAME_LENGTH + " characters");
        }
        Competitor competitor = competitorRepository.findById(competitorId)
                .orElseThrow(() -> new EntityNotFoundException("Competitor not found: " + competitorId));
        competitor.setSpokenName(text.isEmpty() ? null : text);
        competitor.setUpdatedAt(Instant.now());
        return competitorRepository.save(competitor);
    }
}
