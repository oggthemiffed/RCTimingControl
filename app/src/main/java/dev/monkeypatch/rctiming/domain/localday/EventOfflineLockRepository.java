package dev.monkeypatch.rctiming.domain.localday;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOfflineLockRepository extends JpaRepository<EventOfflineLock, Long> {
}
