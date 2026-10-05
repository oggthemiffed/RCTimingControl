package dev.monkeypatch.rctiming.resultsexport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.query.resultsexport.ResultsExportQuery;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

/**
 * Builds results exports and queues them for RaceHub (#27). Only events whose entries came from a RaceHub
 * import are queued, since RaceHub needs its own event id to place them; any event can be downloaded.
 */
@Service
public class ResultsExportService {

    private static final Logger log = LoggerFactory.getLogger(ResultsExportService.class);

    private final EventRepository eventRepository;
    private final RaceRepository raceRepository;
    private final RoundRepository roundRepository;
    private final ResultsOutboxRepository outboxRepository;
    private final ResultsExportQuery exportQuery;
    private final ObjectMapper objectMapper;

    public ResultsExportService(EventRepository eventRepository, RaceRepository raceRepository,
                                RoundRepository roundRepository, ResultsOutboxRepository outboxRepository,
                                ResultsExportQuery exportQuery, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.raceRepository = raceRepository;
        this.roundRepository = roundRepository;
        this.outboxRepository = outboxRepository;
        this.exportQuery = exportQuery;
        this.objectMapper = objectMapper;
    }

    /** Queues an export of the event a race belongs to. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ResultsOutboxItem> enqueueForRace(long raceId, ExportReason reason) {
        Long eventId = raceRepository.findById(raceId)
                .flatMap(race -> roundRepository.findById(race.getRoundId()))
                .map(round -> round.getEventId())
                .orElse(null);
        if (eventId == null) {
            log.warn("No event found for race {}; no results export queued", raceId);
            return Optional.empty();
        }
        return enqueue(eventId, reason);
    }

    /**
     * Builds the event's next export and queues it, replacing any of its exports that have not been sent yet:
     * each export carries the whole event, so only the newest needs to go.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ResultsOutboxItem> enqueue(long eventId, ExportReason reason) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        if (event.getRacehubEventId() == null) {
            log.debug("Event {} has no RaceHub event; results export not queued", eventId);
            return Optional.empty();
        }
        Instant now = Instant.now();
        long revision = event.nextResultsExportRevision();
        eventRepository.save(event);

        for (ResultsOutboxItem older : outboxRepository.findByEventIdAndStatusIn(eventId,
                EnumSet.of(OutboxStatus.QUEUED, OutboxStatus.FAILED))) {
            older.setStatus(OutboxStatus.SUPERSEDED);
        }

        ResultsOutboxItem item = new ResultsOutboxItem();
        item.setEventId(eventId);
        item.setRevision(revision);
        item.setReason(reason);
        item.setPayload(toJson(exportQuery.build(eventId, revision, now)));
        item.setStatus(OutboxStatus.QUEUED);
        item.setNextAttemptAt(now);
        item.setCreatedAt(now);
        ResultsOutboxItem saved = outboxRepository.save(item);
        log.info("Queued results export revision {} of event {} ({})", revision, eventId, reason);
        return Optional.of(saved);
    }

    /** The event's results as they stand now, for download. Carries the last revision handed out; queues nothing. */
    @Transactional(readOnly = true)
    public ResultsExportV1 current(long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        return exportQuery.build(eventId, event.getResultsExportRevision(), Instant.now());
    }

    /** Sends a queued or failed export on the sender's next pass instead of waiting for its retry time. */
    @Transactional
    public ResultsOutboxItem retryNow(long itemId) {
        ResultsOutboxItem item = outboxRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Results export not found: " + itemId));
        if (item.getStatus() == OutboxStatus.QUEUED || item.getStatus() == OutboxStatus.FAILED) {
            item.setNextAttemptAt(Instant.now());
        }
        return item;
    }

    private String toJson(ResultsExportV1 export) {
        try {
            return objectMapper.writeValueAsString(export);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write the results export", e);
        }
    }
}
