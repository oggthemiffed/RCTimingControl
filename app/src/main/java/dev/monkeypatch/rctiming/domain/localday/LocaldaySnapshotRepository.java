package dev.monkeypatch.rctiming.domain.localday;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocaldaySnapshotRepository extends JpaRepository<LocaldaySnapshot, Long> {

    boolean existsByEventIdAndSnapshotId(Long eventId, String snapshotId);
}
