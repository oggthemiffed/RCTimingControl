package dev.monkeypatch.rctiming.domain.practice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PracticeLapRepository extends JpaRepository<PracticeLap, Long> {

    List<PracticeLap> findByPracticeSessionIdOrderByCrossingTimeAsc(Long practiceSessionId);

    List<PracticeLap> findByPracticeSessionIdAndTransponderNumberOrderByLapNumberAsc(
            Long practiceSessionId, String transponderNumber);
}
