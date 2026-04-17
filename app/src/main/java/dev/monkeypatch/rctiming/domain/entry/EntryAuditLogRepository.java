package dev.monkeypatch.rctiming.domain.entry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntryAuditLogRepository extends JpaRepository<EntryAuditLog, Long> {

    List<EntryAuditLog> findByEntryIdOrderByCreatedAtAsc(Long entryId);
}
