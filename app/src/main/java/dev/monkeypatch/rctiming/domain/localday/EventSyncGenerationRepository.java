package dev.monkeypatch.rctiming.domain.localday;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventSyncGenerationRepository extends JpaRepository<EventSyncGeneration, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM EventSyncGeneration g WHERE g.eventId = :eventId")
    Optional<EventSyncGeneration> findByIdForUpdate(@Param("eventId") Long eventId);
}
