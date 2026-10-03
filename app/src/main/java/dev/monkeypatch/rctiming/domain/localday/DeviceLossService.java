package dev.monkeypatch.rctiming.domain.localday;

import dev.monkeypatch.rctiming.domain.event.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * The write side of device-loss recovery (F5, R16, R17) — an elevated official confirming
 * on-site that a Local Race Day Program instance is physically down, not merely unreachable,
 * unlocking the event day for a replacement instance and permanently flagging the resulting data
 * gap. This is deliberately not the mechanism that rejects the original instance's later
 * reconnect attempts — that is {@link SnapshotIngestService}'s ordinary KTD4 generation
 * comparison, once a replacement instance opens at a higher generation (see the plan's
 * device-loss sequence note). This service's job is narrower: invalidate the sync credential so
 * a rejected reconnect can't retry indefinitely against a still-valid secret, unlock the day, set
 * the permanent incomplete-data flag, and write the mandatory audit record.
 */
@Service
@Transactional
public class DeviceLossService {

    private final EventRepository eventRepository;
    private final LocaldayInstanceSecretRepository instanceSecretRepository;
    private final EventOfflineLockRepository eventOfflineLockRepository;
    private final DeviceLossAuditRepository deviceLossAuditRepository;

    public DeviceLossService(EventRepository eventRepository,
                              LocaldayInstanceSecretRepository instanceSecretRepository,
                              EventOfflineLockRepository eventOfflineLockRepository,
                              DeviceLossAuditRepository deviceLossAuditRepository) {
        this.eventRepository = eventRepository;
        this.instanceSecretRepository = instanceSecretRepository;
        this.eventOfflineLockRepository = eventOfflineLockRepository;
        this.deviceLossAuditRepository = deviceLossAuditRepository;
    }

    public record Result(Long eventId, String instanceId, Instant declaredAt) {
    }

    public Result declare(Long eventId, String instanceId, Long adminUserId, String reason) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found: " + eventId);
        }
        LocaldayInstanceSecret secret = instanceSecretRepository.findByEventIdAndInstanceId(eventId, instanceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No instance secret found for event " + eventId + ", instance " + instanceId));

        Instant now = Instant.now();

        secret.setInvalidatedAt(now);
        instanceSecretRepository.save(secret);

        EventOfflineLock lock = eventOfflineLockRepository.findById(eventId)
                .orElseGet(() -> {
                    EventOfflineLock created = new EventOfflineLock();
                    created.setEventId(eventId);
                    created.setLockedAt(now);
                    return created;
                });
        lock.setUnlockedAt(now);
        lock.setIncompleteData(true);
        lock.setIncompleteDataDeclaredAt(now);
        eventOfflineLockRepository.save(lock);

        DeviceLossAudit audit = new DeviceLossAudit();
        audit.setEventId(eventId);
        audit.setAdminUserId(adminUserId);
        audit.setInstanceId(instanceId);
        audit.setReason(reason);
        audit.setCreatedAt(now);
        deviceLossAuditRepository.save(audit);

        return new Result(eventId, instanceId, now);
    }
}
