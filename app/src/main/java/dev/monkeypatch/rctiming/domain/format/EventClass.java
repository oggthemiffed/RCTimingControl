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

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "required_governing_body_code", length = 50)
    private String requiredGoverningBodyCode;

    @Column(name = "racing_class_id")
    private Long racingClassId;

    @Column(name = "combined_race_group")
    private Long combinedRaceGroup;

    @Column(name = "finals_count")
    private Integer finalsCount;

    @Column(name = "cars_per_final")
    private Integer carsPerFinal;

    @Column(name = "bump_count")
    private Integer bumpCount;

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

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getRequiredGoverningBodyCode() { return requiredGoverningBodyCode; }
    public void setRequiredGoverningBodyCode(String requiredGoverningBodyCode) { this.requiredGoverningBodyCode = requiredGoverningBodyCode; }

    public Long getRacingClassId() { return racingClassId; }
    public void setRacingClassId(Long racingClassId) { this.racingClassId = racingClassId; }

    public Long getCombinedRaceGroup() { return combinedRaceGroup; }
    public void setCombinedRaceGroup(Long combinedRaceGroup) { this.combinedRaceGroup = combinedRaceGroup; }

    public Integer getFinalsCount() { return finalsCount; }
    public void setFinalsCount(Integer finalsCount) { this.finalsCount = finalsCount; }

    public Integer getCarsPerFinal() { return carsPerFinal; }
    public void setCarsPerFinal(Integer carsPerFinal) { this.carsPerFinal = carsPerFinal; }

    public Integer getBumpCount() { return bumpCount; }
    public void setBumpCount(Integer bumpCount) { this.bumpCount = bumpCount; }
}
