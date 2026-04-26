package dev.monkeypatch.rctiming.forwarder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Phase 5 / TIMING-08: Hibernate audit entity for retroactive transponder links.
 * Maps to unknown_transponder_link (singular) created by V22 migration.
 * Stores actor, race, transponder, and linked entry for full audit trail (T-05-16).
 *
 * Note: distinct from domain.race.UnknownTransponderLink (V18 unknown_transponder_links, plural),
 * which is the CTRL-06 in-race registration record. This entity is for retroactive
 * lap-credit operations performed by RACE_DIRECTOR or ADMIN role.
 */
@Entity
@Table(name = "unknown_transponder_link")
public class UnknownTransponderLinkAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "race_id", nullable = false)
    private Long raceId;

    @Column(name = "transponder_number", nullable = false, length = 50)
    private String transponderNumber;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "linked_by_user_id")
    private Long linkedByUserId;

    @Column(name = "linked_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant linkedAt;

    public UnknownTransponderLinkAudit() {}

    public UnknownTransponderLinkAudit(Long raceId, String transponderNumber,
                                       Long entryId, Long linkedByUserId) {
        this.raceId = raceId;
        this.transponderNumber = transponderNumber;
        this.entryId = entryId;
        this.linkedByUserId = linkedByUserId;
        this.linkedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getRaceId() { return raceId; }
    public String getTransponderNumber() { return transponderNumber; }
    public Long getEntryId() { return entryId; }
    public Long getLinkedByUserId() { return linkedByUserId; }
    public Instant getLinkedAt() { return linkedAt; }
}
