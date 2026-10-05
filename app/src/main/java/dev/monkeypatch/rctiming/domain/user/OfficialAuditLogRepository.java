package dev.monkeypatch.rctiming.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficialAuditLogRepository extends JpaRepository<OfficialAuditLog, Long> {

    List<OfficialAuditLog> findByOfficialUserIdOrderByCreatedAtAsc(Long officialUserId);
}
