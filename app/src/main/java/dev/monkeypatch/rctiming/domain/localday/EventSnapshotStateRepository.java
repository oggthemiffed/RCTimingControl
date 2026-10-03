package dev.monkeypatch.rctiming.domain.localday;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSnapshotStateRepository extends JpaRepository<EventSnapshotState, Long> {
}
