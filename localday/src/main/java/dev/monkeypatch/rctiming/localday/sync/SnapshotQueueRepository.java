package dev.monkeypatch.rctiming.localday.sync;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotQueueRepository extends JpaRepository<SnapshotQueue, Long> {
}
