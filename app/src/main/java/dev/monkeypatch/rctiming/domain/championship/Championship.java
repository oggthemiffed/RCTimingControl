package dev.monkeypatch.rctiming.domain.championship;


import dev.monkeypatch.rctiming.persistence.CreatedAt;
import dev.monkeypatch.rctiming.persistence.UpdatedAt;
import java.time.Instant;

public class Championship implements CreatedAt, UpdatedAt {

    private Long id;

    private String name;

    private Integer bestXFromYX;

    private Integer bestXFromYY;

    private ScoringSource scoringSource = ScoringSource.FINALS;

    private int tqBonusPoints = 0;

    private int afinalWinnerBonusPoints = 0;

    private Instant createdAt;

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
