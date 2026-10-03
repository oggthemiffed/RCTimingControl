package dev.monkeypatch.rctiming.localday.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Local cache of a race format config snapshot pulled from the cloud, stored as raw JSON.
 * Mirrors app/'s {@code Race.formatOverrides} column pattern ({@code @JdbcTypeCode(SqlTypes.JSON)}
 * over a {@code jsonb} column, mapped to a plain {@link String} — parsing/validating against the
 * sealed race-format config hierarchy is left to the caller, not this module).
 *
 * <p>Snapshot-at-assignment (FORMAT-06): once cached, this row is not retroactively affected by
 * edits to the cloud template it came from.
 */
@Entity
@Table(name = "cached_format_configs")
public class CachedFormatConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cloud_format_id", nullable = false)
    private Long cloudFormatId;

    @Column(nullable = false, length = 200)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String config;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCloudFormatId() { return cloudFormatId; }
    public void setCloudFormatId(Long cloudFormatId) { this.cloudFormatId = cloudFormatId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }

    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
