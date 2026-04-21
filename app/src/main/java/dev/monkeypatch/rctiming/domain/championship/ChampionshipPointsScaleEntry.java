package dev.monkeypatch.rctiming.domain.championship;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "championship_points_scale")
@IdClass(ChampionshipPointsScaleEntryId.class)
public class ChampionshipPointsScaleEntry {

    @Id
    @Column(name = "championship_id")
    private Long championshipId;

    @Id
    @Column(name = "position")
    private Integer position;

    @Column(nullable = false)
    private int points;

    public ChampionshipPointsScaleEntry() {}

    public ChampionshipPointsScaleEntry(Long championshipId, Integer position, int points) {
        this.championshipId = championshipId;
        this.position = position;
        this.points = points;
    }

    public Long getChampionshipId() { return championshipId; }
    public void setChampionshipId(Long v) { this.championshipId = v; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer v) { this.position = v; }

    public int getPoints() { return points; }
    public void setPoints(int v) { this.points = v; }
}
