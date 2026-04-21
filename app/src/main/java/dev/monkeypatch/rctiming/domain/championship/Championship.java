package dev.monkeypatch.rctiming.domain.championship;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "championships")
public class Championship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "best_x_from_y_x")
    private Integer bestXFromYX;

    @Column(name = "best_x_from_y_y")
    private Integer bestXFromYY;

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_source", nullable = false, length = 20)
    private ScoringSource scoringSource = ScoringSource.FINALS;

    @Column(name = "tq_bonus_points", nullable = false)
    private int tqBonusPoints = 0;

    @Column(name = "afinal_winner_bonus_points", nullable = false)
    private int afinalWinnerBonusPoints = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Championship() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getBestXFromYX() { return bestXFromYX; }
    public void setBestXFromYX(Integer v) { this.bestXFromYX = v; }

    public Integer getBestXFromYY() { return bestXFromYY; }
    public void setBestXFromYY(Integer v) { this.bestXFromYY = v; }

    public ScoringSource getScoringSource() { return scoringSource; }
    public void setScoringSource(ScoringSource s) { this.scoringSource = s; }

    public int getTqBonusPoints() { return tqBonusPoints; }
    public void setTqBonusPoints(int v) { this.tqBonusPoints = v; }

    public int getAfinalWinnerBonusPoints() { return afinalWinnerBonusPoints; }
    public void setAfinalWinnerBonusPoints(int v) { this.afinalWinnerBonusPoints = v; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant t) { this.createdAt = t; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant t) { this.updatedAt = t; }
}
