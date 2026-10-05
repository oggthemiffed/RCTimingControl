package dev.monkeypatch.rctiming.timing;

import java.time.Instant;

/**
 * Phase 5 / TIMING-08: audit record for retroactive transponder links.
 * Maps to unknown_transponder_link (singular) created by V22 migration.
 * Stores actor, race, transponder, and linked entry for full audit trail (T-05-16).
 *
 * Note: distinct from domain.race.UnknownTransponderLink (V18 unknown_transponder_links, plural),
 * which is the CTRL-06 in-race registration record. This entity is for retroactive
 * lap-credit operations performed by RACE_DIRECTOR or ADMIN role.
 */
public class UnknownTransponderLinkAudit {

    private Long id;

    private Long raceId;

    private String transponderNumber;

    private Long entryId;

    private Long linkedByUserId;

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

    /** For the repository: an audit row as stored. */
    UnknownTransponderLinkAudit(Long id, Long raceId, String transponderNumber, Long entryId,
                                Long linkedByUserId, Instant linkedAt) {
        this(raceId, transponderNumber, entryId, linkedByUserId);
        this.id = id;
        this.linkedAt = linkedAt;
    }

    public Long getId() { return id; }
    void setId(Long id) { this.id = id; }
    public Long getRaceId() { return raceId; }
    public String getTransponderNumber() { return transponderNumber; }
    public Long getEntryId() { return entryId; }
    public Long getLinkedByUserId() { return linkedByUserId; }
    public Instant getLinkedAt() { return linkedAt; }
}
