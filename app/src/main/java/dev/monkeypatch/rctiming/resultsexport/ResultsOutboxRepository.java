package dev.monkeypatch.rctiming.resultsexport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ResultsOutboxRepository extends JpaRepository<ResultsOutboxItem, Long> {

    List<ResultsOutboxItem> findByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
            Collection<OutboxStatus> statuses, Instant due);

    List<ResultsOutboxItem> findByEventIdAndStatusIn(Long eventId, Collection<OutboxStatus> statuses);
}
