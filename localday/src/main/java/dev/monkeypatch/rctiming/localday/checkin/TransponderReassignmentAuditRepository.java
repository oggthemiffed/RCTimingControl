package dev.monkeypatch.rctiming.localday.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransponderReassignmentAuditRepository extends JpaRepository<TransponderReassignmentAudit, Long> {

    List<TransponderReassignmentAudit> findAllByCachedEntryIdOrderByReassignedAtAsc(Long cachedEntryId);
}
