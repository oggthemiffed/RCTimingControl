package dev.monkeypatch.rctiming.domain.championship;

import java.io.Serializable;
import java.util.Objects;

public class ChampionshipPointsScaleEntryId implements Serializable {
    private Long championshipId;
    private Integer position;

    public ChampionshipPointsScaleEntryId() {}

    public ChampionshipPointsScaleEntryId(Long championshipId, Integer position) {
        this.championshipId = championshipId;
        this.position = position;
    }

    public Long getChampionshipId() { return championshipId; }
    public void setChampionshipId(Long v) { this.championshipId = v; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer v) { this.position = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChampionshipPointsScaleEntryId other)) return false;
        return Objects.equals(championshipId, other.championshipId)
                && Objects.equals(position, other.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(championshipId, position);
    }
}
