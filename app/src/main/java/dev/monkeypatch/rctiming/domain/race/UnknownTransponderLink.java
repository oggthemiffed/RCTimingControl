package dev.monkeypatch.rctiming.domain.race;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "unknown_transponder_links")
public class UnknownTransponderLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "race_id", nullable = false)
    private Long raceId;

    @Column(name = "transponder_number", nullable = false, length = 20)
    private String transponderNumber;

    @Column(name = "linked_entry_id")
    private Long linkedEntryId;

    @Column(name = "linked_by", nullable = false)
    private Long linkedBy;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRaceId() { return raceId; }
    public void setRaceId(Long raceId) { this.raceId = raceId; }

    public String getTransponderNumber() { return transponderNumber; }
    public void setTransponderNumber(String transponderNumber) { this.transponderNumber = transponderNumber; }

    public Long getLinkedEntryId() { return linkedEntryId; }
    public void setLinkedEntryId(Long linkedEntryId) { this.linkedEntryId = linkedEntryId; }

    public Long getLinkedBy() { return linkedBy; }
    public void setLinkedBy(Long linkedBy) { this.linkedBy = linkedBy; }

    public Instant getLinkedAt() { return linkedAt; }
    public void setLinkedAt(Instant linkedAt) { this.linkedAt = linkedAt; }
}
