package dev.monkeypatch.rctiming.domain.championship;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "championship_classes")
public class ChampionshipClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "championship_id", nullable = false)
    private Long championshipId;

    @Column(name = "racing_class_id", nullable = false)
    private Long racingClassId;

    @Column(name = "best_x_from_y_x")
    private Integer bestXFromYX;

    @Column(name = "best_x_from_y_y")
    private Integer bestXFromYY;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ChampionshipClass() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChampionshipId() { return championshipId; }
    public void setChampionshipId(Long v) { this.championshipId = v; }

    public Long getRacingClassId() { return racingClassId; }
    public void setRacingClassId(Long v) { this.racingClassId = v; }

    public Integer getBestXFromYX() { return bestXFromYX; }
    public void setBestXFromYX(Integer v) { this.bestXFromYX = v; }

    public Integer getBestXFromYY() { return bestXFromYY; }
    public void setBestXFromYY(Integer v) { this.bestXFromYY = v; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant t) { this.createdAt = t; }
}
