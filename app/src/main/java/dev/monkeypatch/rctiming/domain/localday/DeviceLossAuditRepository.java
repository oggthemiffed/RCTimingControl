package dev.monkeypatch.rctiming.domain.localday;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceLossAuditRepository extends JpaRepository<DeviceLossAudit, Long> {
}
