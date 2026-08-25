package dev.monkeypatch.rctiming.domain.localday;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * KTD4's generation-fencing and snapshotId-idempotency checks for the periodic snapshot push
 * (R11, R18) — the write-side counterpart to {@link PreCacheService}'s credential/secret
 * minting. Authenticates the caller against the per-day-instance secret (KTD9) before the
 * generation check runs, per the plan's explicit ordering requirement.
 */
@Service
@Transactional
public class SnapshotIngestService {

    private final LocaldayInstanceSecretRepository instanceSecretRepository;
    private final EventSyncGenerationRepository eventSyncGenerationRepository;
    private final EventSnapshotStateRepository eventSnapshotStateRepository;
    private final LocaldaySnapshotRepository localdaySnapshotRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public SnapshotIngestService(LocaldayInstanceSecretRepository instanceSecretRepository,
                                  EventSyncGenerationRepository eventSyncGenerationRepository,
                                  EventSnapshotStateRepository eventSnapshotStateRepository,
                                  LocaldaySnapshotRepository localdaySnapshotRepository,
                                  PasswordEncoder passwordEncoder,
                                  ObjectMapper objectMapper) {
        this.instanceSecretRepository = instanceSecretRepository;
        this.eventSyncGenerationRepository = eventSyncGenerationRepository;
        this.eventSnapshotStateRepository = eventSnapshotStateRepository;
        this.localdaySnapshotRepository = localdaySnapshotRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    public SnapshotIngestOutcome ingest(Long eventId, String instanceId, String presentedSecret,
                                         long generation, String snapshotId, JsonNode payload) {
        authenticate(eventId, instanceId, presentedSecret);

        if (localdaySnapshotRepository.existsByEventIdAndSnapshotId(eventId, snapshotId)) {
            // KTD4 idempotent replay: this exact snapshotId was already accepted and applied —
            // return the same outcome without reprocessing (no duplicate state update).
            return new SnapshotIngestOutcome.Accepted(currentGeneration(eventId));
        }

        // Single atomic unit for the compare-and-update: the pessimistic write lock (same idiom
        // DayLifecycleService.claimGeneration already uses for this table) is held for the rest
        // of this transaction, so a concurrently-racing push for the same event serializes behind
        // it rather than racing a separate read-then-write.
        EventSyncGeneration generationRow = eventSyncGenerationRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No sync generation found for event " + eventId + " — has the day ever been opened?"));

        if (generation < generationRow.getGeneration()) {
            return new SnapshotIngestOutcome.Rejected(generationRow.getGeneration());
        }
        if (generation > generationRow.getGeneration()) {
            generationRow.setGeneration(generation);
            eventSyncGenerationRepository.save(generationRow);
        }

        upsertSnapshotState(eventId, payload);
        recordSnapshot(eventId, instanceId, snapshotId, generation);

        return new SnapshotIngestOutcome.Accepted(generationRow.getGeneration());
    }

    /**
     * KTD9: authenticates the calling {@code :localday} instance against its minted, unrevoked
     * secret — before the generation check runs. A missing/wrong/invalidated secret is a 401,
     * distinct from KTD4's own 409 rejection (that's "you're a legitimate but superseded
     * instance"; this is "you're not who you claim to be at all").
     */
    private void authenticate(Long eventId, String instanceId, String presentedSecret) {
        boolean valid = instanceId != null && presentedSecret != null
                && instanceSecretRepository.findByEventIdAndInstanceId(eventId, instanceId)
                        .filter(secret -> secret.getInvalidatedAt() == null)
                        .filter(secret -> passwordEncoder.matches(presentedSecret, secret.getSecretHash()))
                        .isPresent();
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or invalidated instance secret");
        }
    }

    private long currentGeneration(Long eventId) {
        return eventSyncGenerationRepository.findById(eventId)
                .map(EventSyncGeneration::getGeneration)
                .orElse(0L);
    }

    private void upsertSnapshotState(Long eventId, JsonNode payload) {
        EventSnapshotState state = eventSnapshotStateRepository.findById(eventId)
                .orElseGet(EventSnapshotState::new);
        state.setEventId(eventId);
        state.setLastSyncedAt(Instant.now());
        try {
            state.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize snapshot payload for event " + eventId, e);
        }
        eventSnapshotStateRepository.save(state);
    }

    private void recordSnapshot(Long eventId, String instanceId, String snapshotId, long generation) {
        LocaldaySnapshot snapshot = new LocaldaySnapshot();
        snapshot.setEventId(eventId);
        snapshot.setInstanceId(instanceId);
        snapshot.setSnapshotId(snapshotId);
        snapshot.setGeneration(generation);
        snapshot.setReceivedAt(Instant.now());
        try {
            localdaySnapshotRepository.saveAndFlush(snapshot);
        } catch (DataIntegrityViolationException e) {
            // Two identical retries raced past the existsBy... pre-check above; the loser here
            // already had its state update applied by the winner — still an idempotent replay,
            // not an error.
        }
    }
}
