package dev.monkeypatch.rctiming.domain.format;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "event_classes")
public class EventClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Type(JsonType.class)
    @Column(name = "config_snapshot", columnDefinition = "jsonb", nullable = false)
    private RaceFormatConfig configSnapshot;

    @Type(JsonType.class)
    @Column(name = "config_override", columnDefinition = "jsonb")
    private Map<String, Object> configOverride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private RaceFormatTemplate template;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RaceFormatConfig getConfigSnapshot() { return configSnapshot; }
    public void setConfigSnapshot(RaceFormatConfig configSnapshot) { this.configSnapshot = configSnapshot; }

    public Map<String, Object> getConfigOverride() { return configOverride; }
    public void setConfigOverride(Map<String, Object> configOverride) { this.configOverride = configOverride; }

    public RaceFormatTemplate getTemplate() { return template; }
    public void setTemplate(RaceFormatTemplate template) { this.template = template; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
