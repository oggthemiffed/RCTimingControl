package dev.monkeypatch.rctiming.resultsexport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ResultsOutboxRepository extends JpaRepository<ResultsOutboxItem, Long> {

    List<ResultsOutboxItem> findByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
            Collection<OutboxStatus> statuses, Instant due);

    List<ResultsOutboxItem> findByEventIdAndStatusIn(Long eventId, Collection<OutboxStatus> statuses);

    /** Records a delivered export. It was sent, so this holds even if a newer one superseded it meanwhile. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ResultsOutboxItem i SET i.status = dev.monkeypatch.rctiming.resultsexport.OutboxStatus.SENT,
                i.sentAt = :sentAt, i.attempts = i.attempts + 1, i.lastError = null
            WHERE i.id = :id""")
    int recordSent(@Param("id") long id, @Param("sentAt") Instant sentAt);

    /**
     * Records a failed attempt, only while the export is still waiting to go. One a newer export superseded
     * during the attempt stays superseded, so it is never sent after the newer one.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ResultsOutboxItem i SET i.status = dev.monkeypatch.rctiming.resultsexport.OutboxStatus.FAILED,
                i.attempts = i.attempts + 1, i.lastError = :error, i.nextAttemptAt = :nextAttemptAt
            WHERE i.id = :id
              AND i.status IN (dev.monkeypatch.rctiming.resultsexport.OutboxStatus.QUEUED,
                               dev.monkeypatch.rctiming.resultsexport.OutboxStatus.FAILED)""")
    int recordFailure(@Param("id") long id, @Param("error") String error,
                      @Param("nextAttemptAt") Instant nextAttemptAt);
}
