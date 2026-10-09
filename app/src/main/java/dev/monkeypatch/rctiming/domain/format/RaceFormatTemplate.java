package dev.monkeypatch.rctiming.domain.format;

import dev.monkeypatch.rctiming.persistence.CreatedAt;
import dev.monkeypatch.rctiming.persistence.UpdatedAt;
import java.time.Instant;

public class RaceFormatTemplate implements CreatedAt, UpdatedAt {

    private Long id;
    private String name;
    private RaceFormatConfig config;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RaceFormatConfig getConfig() { return config; }
    public void setConfig(RaceFormatConfig config) { this.config = config; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
