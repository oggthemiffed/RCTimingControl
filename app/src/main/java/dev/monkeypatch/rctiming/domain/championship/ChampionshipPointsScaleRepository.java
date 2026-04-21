package dev.monkeypatch.rctiming.domain.championship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChampionshipPointsScaleRepository
        extends JpaRepository<ChampionshipPointsScaleEntry, ChampionshipPointsScaleEntryId> {

    List<ChampionshipPointsScaleEntry> findByChampionshipIdOrderByPositionAsc(Long championshipId);

    @Modifying
    @Query("delete from ChampionshipPointsScaleEntry e where e.championshipId = :championshipId")
    int deleteAllByChampionshipId(@Param("championshipId") Long championshipId);
}
