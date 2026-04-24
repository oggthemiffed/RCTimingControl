package dev.monkeypatch.rctiming.domain.race;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RaceRepository extends JpaRepository<Race, Long> {
    List<Race> findByRoundIdOrderBySequenceInRound(Long roundId);

    // For BumpUpSeedingService: find all finals for a class
    @Query("SELECT r FROM Race r JOIN Round ro ON r.roundId = ro.id WHERE r.eventClassId = :eventClassId AND ro.type = :roundType")
    List<Race> findByEventClassIdAndRoundType(@Param("eventClassId") Long eventClassId,
                                               @Param("roundType") RoundType roundType);

    // For BumpUpSeedingService: find a specific final letter race for a class
    List<Race> findByEventClassIdAndFinalLetter(Long eventClassId, String finalLetter);
}
