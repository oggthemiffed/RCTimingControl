package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CompetitorService {

    private final CompetitorRepository competitorRepository;
    private final CompetitorAuditLogRepository auditLogRepository;

    public CompetitorService(CompetitorRepository competitorRepository,
                             CompetitorAuditLogRepository auditLogRepository) {
        this.competitorRepository = competitorRepository;
        this.auditLogRepository = auditLogRepository;
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

    /**
     * Existing competitors a typed walk-in name may be: the same name, ignoring case and spacing, from
     * any source (#123). Empty when the name is new.
     */
    @Transactional(readOnly = true)
    public List<Competitor> findPossibleDuplicates(String displayName) {
        return competitorRepository.findByNormalizedName(displayName);
    }

    /** The longest spoken name accepted; it is a short pronunciation hint, not free text. */
    public static final int MAX_SPOKEN_NAME_LENGTH = 100;

    /**
     * Sets how a competitor's name is said aloud, or clears it when blank (#119). Only this field changes,
     * so an import can still update the rest. A change is recorded with who made it; saving the same
     * value again records nothing.
     *
     * @throws EntityNotFoundException if there is no such competitor
     * @throws IllegalArgumentException if the text is longer than {@link #MAX_SPOKEN_NAME_LENGTH}
     */
    @Transactional
    public Competitor setSpokenName(Long competitorId, String spokenName, Long actorUserId) {
        String text = spokenName == null ? "" : spokenName.trim();
        if (text.length() > MAX_SPOKEN_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "The spoken name can be at most " + MAX_SPOKEN_NAME_LENGTH + " characters");
        }
        Competitor competitor = competitorRepository.getOrThrow(competitorId);
        String before = competitor.getSpokenName();
        String after = text.isEmpty() ? null : text;
        if (java.util.Objects.equals(before, after)) {
            return competitor;
        }
        Instant now = Instant.now();
        competitor.setSpokenName(after);
        competitor.setUpdatedAt(now);
        Competitor saved = competitorRepository.save(competitor);

        CompetitorAuditLog log = new CompetitorAuditLog();
        log.setCompetitorId(competitorId);
        log.setActorUserId(actorUserId);
        log.setAction(CompetitorAuditLog.SPOKEN_NAME_CHANGED);
        log.setBeforeValue(before);
        log.setAfterValue(after);
        log.setCreatedAt(now);
        auditLogRepository.save(log);
        return saved;
    }
}
