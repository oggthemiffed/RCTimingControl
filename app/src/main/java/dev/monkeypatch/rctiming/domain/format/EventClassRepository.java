package dev.monkeypatch.rctiming.domain.format;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventClassRepository extends JpaRepository<EventClass, Long> {
    List<EventClass> findByEventId(Long eventId);

    /** Ids and racing classes of an event's classes, without loading their format config. */
    @Query("select ec.id as id, ec.racingClassId as racingClassId from EventClass ec where ec.eventId = :eventId")
    List<EventClassRef> findRefsByEventId(@Param("eventId") Long eventId);

    interface EventClassRef {
        Long getId();
        Long getRacingClassId();
    }
}
