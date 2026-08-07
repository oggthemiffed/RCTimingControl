package dev.monkeypatch.rctiming.localday.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Local cache of a racer/entry snapshot pulled from the cloud before the venue goes offline
 * for race day. Read-mostly — this is not a system of record, the cloud entry is.
 */
@Entity
@Table(name = "cached_entries")
public class CachedEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cloud_entry_id", nullable = false)
    private Long cloudEntryId;

    @Column(name = "transponder_number", nullable = false, length = 20)
    private String transponderNumber;

    @Column(name = "racer_name", nullable = false, length = 200)
    private String racerName;

    @Column(name = "car_name", length = 200)
    private String carName;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "cloud_event_class_id")
    private Long cloudEventClassId;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCloudEntryId() { return cloudEntryId; }
    public void setCloudEntryId(Long cloudEntryId) { this.cloudEntryId = cloudEntryId; }

    public String getTransponderNumber() { return transponderNumber; }
    public void setTransponderNumber(String transponderNumber) { this.transponderNumber = transponderNumber; }

    public String getRacerName() { return racerName; }
    public void setRacerName(String racerName) { this.racerName = racerName; }

    public String getCarName() { return carName; }
    public void setCarName(String carName) { this.carName = carName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Long getCloudEventClassId() { return cloudEventClassId; }
    public void setCloudEventClassId(Long cloudEventClassId) { this.cloudEventClassId = cloudEventClassId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
