package dev.monkeypatch.rctiming.domain.practice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {

    @Query("SELECT ps FROM PracticeSession ps WHERE ps.status = dev.monkeypatch.rctiming.domain.practice.PracticeStatus.RUNNING")
    Optional<PracticeSession> findRunningSession();

    List<PracticeSession> findByEvent_Id(Long eventId);
}
