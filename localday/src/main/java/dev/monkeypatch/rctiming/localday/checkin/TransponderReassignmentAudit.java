package dev.monkeypatch.rctiming.localday.checkin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Audit record of a transponder reassignment applied to a cached entry (R8) — e.g. for
 * equipment failure on race day, with no cloud dependency. {@link #cachedEntryId} references a
 * local row ({@code CachedEntry.id}), not a cloud ID. Mirrors
 * {@link dev.monkeypatch.rctiming.localday.race.MarshalAdjustment}'s established audit-entity
 * style for this module.
 */
@Entity
@Table(name = "transponder_reassignment_audit")
public class TransponderReassignmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cached_entry_id", nullable = false)
    private Long cachedEntryId;

    @Column(name = "old_transponder_number", nullable = false, length = 20)
    private String oldTransponderNumber;

    @Column(name = "new_transponder_number", nullable = false, length = 20)
    private String newTransponderNumber;

    @Column(name = "acting_credential_id", nullable = false)
    private Long actingCredentialId;

    @Column(name = "acting_official_name", nullable = false, length = 200)
    private String actingOfficialName;

    @Column(name = "reassigned_at", nullable = false)
    private Instant reassignedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCachedEntryId() { return cachedEntryId; }
    public void setCachedEntryId(Long cachedEntryId) { this.cachedEntryId = cachedEntryId; }

    public String getOldTransponderNumber() { return oldTransponderNumber; }
    public void setOldTransponderNumber(String oldTransponderNumber) { this.oldTransponderNumber = oldTransponderNumber; }

    public String getNewTransponderNumber() { return newTransponderNumber; }
    public void setNewTransponderNumber(String newTransponderNumber) { this.newTransponderNumber = newTransponderNumber; }

    public Long getActingCredentialId() { return actingCredentialId; }
    public void setActingCredentialId(Long actingCredentialId) { this.actingCredentialId = actingCredentialId; }

    public String getActingOfficialName() { return actingOfficialName; }
    public void setActingOfficialName(String actingOfficialName) { this.actingOfficialName = actingOfficialName; }

    public Instant getReassignedAt() { return reassignedAt; }
    public void setReassignedAt(Instant reassignedAt) { this.reassignedAt = reassignedAt; }
}
