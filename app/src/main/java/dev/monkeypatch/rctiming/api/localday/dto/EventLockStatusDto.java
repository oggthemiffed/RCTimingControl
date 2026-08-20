package dev.monkeypatch.rctiming.api.localday.dto;

import dev.monkeypatch.rctiming.domain.localday.EventOfflineLock;

import java.time.Instant;
import java.util.Optional;

public record EventLockStatusDto(Long eventId, boolean locked, Instant lockedAt, Instant unlockedAt) {

    public static EventLockStatusDto from(Long eventId, Optional<EventOfflineLock> lock) {
        if (lock.isEmpty()) {
            return new EventLockStatusDto(eventId, false, null, null);
        }
        EventOfflineLock l = lock.get();
        return new EventLockStatusDto(eventId, l.getUnlockedAt() == null, l.getLockedAt(), l.getUnlockedAt());
    }
}
