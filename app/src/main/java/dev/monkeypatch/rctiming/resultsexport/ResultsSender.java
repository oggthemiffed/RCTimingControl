package dev.monkeypatch.rctiming.resultsexport;

import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Sends queued results exports to RaceHub (#27). Runs on the scheduler thread, so race control never waits on
 * the network. A failed send is tried again with a growing delay, up to {@link #MAX_BACKOFF}; exports of one
 * event go in revision order, and a newer one replaces any that have not gone yet.
 *
 * <p>Each request carries an {@code Idempotency-Key} made from the event and revision, and the body carries the
 * revision, so sending the same export twice is safe.
 */
@Component
@EnableConfigurationProperties(RaceHubResultsProperties.class)
public class ResultsSender {

    static final Duration FIRST_BACKOFF = Duration.ofSeconds(30);
    static final Duration MAX_BACKOFF = Duration.ofMinutes(30);
    private static final int MAX_ERROR_LENGTH = 500;

    private static final Logger log = LoggerFactory.getLogger(ResultsSender.class);

    private final ResultsOutboxRepository outboxRepository;
    private final EventRepository eventRepository;
    private final RaceHubResultsProperties properties;
    private final RestClient restClient;

    public ResultsSender(ResultsOutboxRepository outboxRepository, EventRepository eventRepository,
                         RaceHubResultsProperties properties, RestClient.Builder restClientBuilder) {
        this.outboxRepository = outboxRepository;
        this.eventRepository = eventRepository;
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    @Scheduled(initialDelay = 15_000, fixedDelayString = "${rctiming.racehub.send-interval-ms:15000}")
    public void sendDue() {
        if (!properties.sendingEnabled()) {
            return;
        }
        List<ResultsOutboxItem> due = outboxRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
                EnumSet.of(OutboxStatus.QUEUED, OutboxStatus.FAILED), Instant.now());
        for (ResultsOutboxItem item : due) {
            if (!send(item)) {
                // RaceHub is unreachable or refusing; leave the rest for the next pass
                return;
            }
        }
    }

    /** Sends one export and records the outcome. Returns whether it was accepted. */
    boolean send(ResultsOutboxItem item) {
        String key = idempotencyKey(item);
        try {
            restClient.post()
                    .uri(properties.resultsUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        if (properties.token() != null) {
                            h.setBearerAuth(properties.token());
                        }
                        h.set("Idempotency-Key", key);
                    })
                    .body(item.getPayload())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            return failed(item, "RaceHub answered " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            return failed(item, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        item.setStatus(OutboxStatus.SENT);
        item.setSentAt(Instant.now());
        item.setAttempts(item.getAttempts() + 1);
        item.setLastError(null);
        outboxRepository.save(item);
        log.info("Sent results export revision {} of event {} to RaceHub", item.getRevision(), item.getEventId());
        return true;
    }

    private boolean failed(ResultsOutboxItem item, String error) {
        int attempts = item.getAttempts() + 1;
        item.setAttempts(attempts);
        item.setStatus(OutboxStatus.FAILED);
        item.setLastError(error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error);
        item.setNextAttemptAt(Instant.now().plus(backoff(attempts)));
        outboxRepository.save(item);
        log.warn("Results export revision {} of event {} not sent (attempt {}): {}",
                item.getRevision(), item.getEventId(), attempts, item.getLastError());
        return false;
    }

    /** 30 s after the first failure, doubling each time, never more than 30 minutes. */
    static Duration backoff(int attempts) {
        int doublings = Math.min(Math.max(attempts - 1, 0), 16);
        Duration delay = FIRST_BACKOFF.multipliedBy(1L << doublings);
        return delay.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : delay;
    }

    /** Stable for one revision of one RaceHub event, whichever laptop sends it and however often. */
    private String idempotencyKey(ResultsOutboxItem item) {
        String racehubEventId = eventRepository.findById(item.getEventId())
                .map(Event::getRacehubEventId)
                .orElse("rctc-event-" + item.getEventId());
        return "rctc-results-" + racehubEventId + "-r" + item.getRevision();
    }
}
