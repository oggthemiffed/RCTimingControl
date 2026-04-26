package dev.monkeypatch.rctiming.forwarder;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Phase 5 / TIMING-08: repository for retroactive transponder link audit records.
 */
public interface UnknownTransponderLinkAuditRepository
        extends JpaRepository<UnknownTransponderLinkAudit, Long> {
}
