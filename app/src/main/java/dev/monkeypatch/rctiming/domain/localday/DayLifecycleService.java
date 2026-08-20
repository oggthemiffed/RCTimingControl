package dev.monkeypatch.rctiming.domain.localday;

import dev.monkeypatch.rctiming.domain.event.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class DayLifecycleService {

    private final EventOfflineLockRepository eventOfflineLockRepository;
    private final EventRepository eventRepository;

    public DayLifecycleService(EventOfflineLockRepository eventOfflineLockRepository,
                                EventRepository eventRepository) {
        this.eventOfflineLockRepository = eventOfflineLockRepository;
        this.eventRepository = eventRepository;
    }

    public EventOfflineLock open(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found: " + eventId);
        }
        EventOfflineLock lock = eventOfflineLockRepository.findById(eventId)
                .orElseGet(() -> {
                    EventOfflineLock created = new EventOfflineLock();
                    created.setEventId(eventId);
                    return created;
                });
        lock.setLockedAt(Instant.now());
        lock.setUnlockedAt(null);
        return eventOfflineLockRepository.save(lock);
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
}
