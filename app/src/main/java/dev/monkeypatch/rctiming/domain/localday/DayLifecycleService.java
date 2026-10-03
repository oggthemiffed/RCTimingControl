package dev.monkeypatch.rctiming.domain.localday;

import dev.monkeypatch.rctiming.domain.event.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class DayLifecycleService {

    private final EventOfflineLockRepository eventOfflineLockRepository;
    private final EventRepository eventRepository;
    private final EventSyncGenerationRepository eventSyncGenerationRepository;

    public DayLifecycleService(EventOfflineLockRepository eventOfflineLockRepository,
                                EventRepository eventRepository,
                                EventSyncGenerationRepository eventSyncGenerationRepository) {
        this.eventOfflineLockRepository = eventOfflineLockRepository;
        this.eventRepository = eventRepository;
        this.eventSyncGenerationRepository = eventSyncGenerationRepository;
    }

    /** Result of opening a day: the current lock state plus the freshly claimed sync generation. */
    public record OpenResult(EventOfflineLock lock, long generation) {}

    public OpenResult open(Long eventId, String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            throw new IllegalArgumentException("instanceId is required");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found: " + eventId);
        }

        EventOfflineLock lock = upsertLock(eventId, Instant.now());
        long generation = claimGeneration(eventId);

        return new OpenResult(lock, generation);
    }

    public String close(Long eventId, boolean syncComplete) {
        EventOfflineLock lock = eventOfflineLockRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("No offline lock found for event: " + eventId));
        if (!syncComplete) {
            return "pending";
        }
        lock.setUnlockedAt(Instant.now());
        eventOfflineLockRepository.save(lock);
        return "closed";
    }

    @Transactional(readOnly = true)
    public Optional<EventOfflineLock> lockStatus(Long eventId) {
        return eventOfflineLockRepository.findById(eventId);
    }

    private EventOfflineLock upsertLock(Long eventId, Instant now) {
        Optional<EventOfflineLock> existing = eventOfflineLockRepository.findById(eventId);
        if (existing.isPresent()) {
            EventOfflineLock lock = existing.get();
            lock.setLockedAt(now);
            lock.setUnlockedAt(null);
            return eventOfflineLockRepository.save(lock);
        }
        EventOfflineLock lock = new EventOfflineLock();
        lock.setEventId(eventId);
        lock.setLockedAt(now);
        lock.setUnlockedAt(null);
        try {
            return eventOfflineLockRepository.saveAndFlush(lock);
        } catch (DataIntegrityViolationException e) {
            EventOfflineLock winner = eventOfflineLockRepository.findById(eventId).orElseThrow();
            winner.setLockedAt(now);
            winner.setUnlockedAt(null);
            return eventOfflineLockRepository.save(winner);
        }
    }

    private long claimGeneration(Long eventId) {
        EventSyncGeneration generationRow = eventSyncGenerationRepository.findByIdForUpdate(eventId)
                .orElseGet(() -> {
                    EventSyncGeneration created = new EventSyncGeneration();
                    created.setEventId(eventId);
                    created.setGeneration(0);
                    return eventSyncGenerationRepository.save(created);
                });
        generationRow.setGeneration(generationRow.getGeneration() + 1);
        eventSyncGenerationRepository.save(generationRow);
        return generationRow.getGeneration();
    }
}
