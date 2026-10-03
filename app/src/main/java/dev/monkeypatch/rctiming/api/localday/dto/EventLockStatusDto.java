package dev.monkeypatch.rctiming.api.localday.dto;

import dev.monkeypatch.rctiming.domain.localday.EventOfflineLock;

import java.time.Instant;
import java.util.Optional;

public record EventLockStatusDto(Long eventId, boolean locked, Instant lockedAt, Instant unlockedAt, long generation) {

    public static EventLockStatusDto from(Long eventId, Optional<EventOfflineLock> lock, long generation) {
        if (lock.isEmpty()) {
            return new EventLockStatusDto(eventId, false, null, null, generation);
        }
        EventOfflineLock l = lock.get();
        return new EventLockStatusDto(eventId, l.getUnlockedAt() == null, l.getLockedAt(), l.getUnlockedAt(), generation);
    }
}
