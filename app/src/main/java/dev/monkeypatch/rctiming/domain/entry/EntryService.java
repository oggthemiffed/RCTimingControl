package dev.monkeypatch.rctiming.domain.entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.admin.dto.AdminUpdateTransponderRequest;
import dev.monkeypatch.rctiming.api.racer.dto.EntryDto;
import dev.monkeypatch.rctiming.api.racer.dto.EntryResult;
import dev.monkeypatch.rctiming.api.racer.dto.SubmitEntryRequest;
import dev.monkeypatch.rctiming.domain.car.Car;
import dev.monkeypatch.rctiming.domain.car.CarRepository;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.event.EventStatus;
import dev.monkeypatch.rctiming.domain.transponder.Transponder;
import dev.monkeypatch.rctiming.domain.transponder.TransponderRepository;
import dev.monkeypatch.rctiming.domain.user.UserGoverningBodyMembershipRepository;
import jakarta.persistence.EntityNotFoundException;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;

@Service
@Transactional
public class EntryService {

    private final EntryRepository entryRepository;
    private final EventRepository eventRepository;
    private final DSLContext dsl;
    private final CarRepository carRepository;
    private final TransponderRepository transponderRepository;
    private final UserGoverningBodyMembershipRepository membershipRepository;
    private final EntryAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public EntryService(EntryRepository entryRepository,
                        EventRepository eventRepository,
                        DSLContext dsl,
                        CarRepository carRepository,
                        TransponderRepository transponderRepository,
                        UserGoverningBodyMembershipRepository membershipRepository,
                        EntryAuditLogRepository auditLogRepository,
                        ObjectMapper objectMapper) {
        this.entryRepository = entryRepository;
        this.eventRepository = eventRepository;
        this.dsl = dsl;
        this.carRepository = carRepository;
        this.transponderRepository = transponderRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public EntryResult submitEntry(Long userId, SubmitEntryRequest req) {
        Event event = eventRepository.findById(req.eventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + req.eventId()));

        // Event must be OPEN or PUBLISHED for entry submission
        if (event.getStatus() != EventStatus.OPEN && event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Event is not open for entries");
        }
        if (event.getEntryClosesAt() != null && event.getEntryClosesAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Event entry has closed");
        }

        // Use jOOQ to fetch only the two columns we need — avoids loading the JSONB config_snapshot
        // via Hibernate's JsonType which has deserialization issues in the test environment
        var ecRow = dsl.select(EVENT_CLASSES.EVENT_ID, EVENT_CLASSES.REQUIRED_GOVERNING_BODY_CODE)
                .from(EVENT_CLASSES)
                .where(EVENT_CLASSES.ID.eq(req.eventClassId()))
                .fetchOptional()
                .orElseThrow(() -> new EntityNotFoundException("Event class not found: " + req.eventClassId()));
        Long ecEventId = ecRow.get(EVENT_CLASSES.EVENT_ID);
        if (ecEventId == null || !ecEventId.equals(req.eventId())) {
            throw new IllegalArgumentException("Event class does not belong to the submitted event");
        }
        String requiredCode = ecRow.get(EVENT_CLASSES.REQUIRED_GOVERNING_BODY_CODE);

        Transponder transponder = transponderRepository.findById(req.transponderId())
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Transponder not found"));

        Car car = carRepository.findById(req.carId())
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));

        // RACER-14 hard block: if class requires membership, racer must hold it (unless admin overrides later)
        if (requiredCode != null && !requiredCode.isBlank()) {
            boolean holds = membershipRepository
                    .findByUserIdAndGoverningBodyCode(userId, requiredCode).isPresent();
            if (!holds) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Membership in " + requiredCode + " is required for this class");
            }
        }

        Instant now = Instant.now();
        Entry entry = new Entry();
        entry.setUserId(userId);
        entry.setEventId(event.getId());
        entry.setEventClassId(req.eventClassId());
        entry.setCarId(car.getId());
        entry.setTransponderId(transponder.getId());
        entry.setTransponderNumberSnapshot(transponder.getTransponderNumber());
        entry.setTransponderLabelSnapshot(transponder.getLabel());
        entry.setStatus(EntryStatus.PENDING);
        entry.setSubmittedAt(now);
        entry.setUpdatedAt(now);
        Entry persisted = entryRepository.save(entry);

        // Auto-confirm in the same transaction (D-10)
        persisted.setStatus(EntryStatus.CONFIRMED);
        persisted.setConfirmedAt(now);
        persisted.setUpdatedAt(now);

        // RACER-09 soft warning (non-blocking)
        List<String> warnings = new ArrayList<>();
        List<Entry> conflicts = entryRepository
                .findByEventIdAndTransponderNumberSnapshotAndStatusAndUserIdNot(
                        event.getId(), transponder.getTransponderNumber(), EntryStatus.CONFIRMED, userId);
        if (!conflicts.isEmpty()) {
            warnings.add("Transponder " + transponder.getTransponderNumber()
                    + " has already been entered for this event by another racer.");
        }

        return new EntryResult(EntryDto.from(persisted), warnings);
    }

    public void withdraw(Long entryId, Long userId) {
        Entry entry = entryRepository.findById(entryId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Entry not found: " + entryId));
        if (entry.getStatus() == EntryStatus.WITHDRAWN) {
            throw new IllegalArgumentException("Entry already withdrawn");
        }
        Instant now = Instant.now();
        entry.setStatus(EntryStatus.WITHDRAWN);
        entry.setWithdrawnAt(now);
        entry.setUpdatedAt(now);
    }

    public EntryDto adminUpdateTransponder(Long entryId, Long adminUserId, AdminUpdateTransponderRequest req) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entry not found: " + entryId));
        Transponder newTransponder = transponderRepository.findById(req.transponderId())
                .orElseThrow(() -> new EntityNotFoundException("Transponder not found: " + req.transponderId()));

        String beforeJson = writeJson(Map.of(
                "transponderId", String.valueOf(entry.getTransponderId()),
                "transponderNumberSnapshot", entry.getTransponderNumberSnapshot(),
                "transponderLabelSnapshot", String.valueOf(entry.getTransponderLabelSnapshot())
        ));
        entry.setTransponderId(newTransponder.getId());
        entry.setTransponderNumberSnapshot(newTransponder.getTransponderNumber());
        entry.setTransponderLabelSnapshot(newTransponder.getLabel());
        entry.setUpdatedAt(Instant.now());
        String afterJson = writeJson(Map.of(
                "transponderId", String.valueOf(newTransponder.getId()),
                "transponderNumberSnapshot", newTransponder.getTransponderNumber(),
                "transponderLabelSnapshot", String.valueOf(newTransponder.getLabel())
        ));
        writeAudit(entry.getId(), adminUserId, "TRANSPONDER_SWAP", req.reason(), beforeJson, afterJson);
        return EntryDto.from(entry);
    }

    public EntryDto adminApplyMembershipOverride(Long entryId, Long adminUserId, String reason) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entry not found: " + entryId));
        String beforeJson = writeJson(Map.of(
                "status", entry.getStatus().name(),
                "membershipOverrideByAdminId", String.valueOf(entry.getMembershipOverrideByAdminId())
        ));
        entry.setMembershipOverrideByAdminId(adminUserId);
        entry.setStatus(EntryStatus.CONFIRMED);
        entry.setConfirmedAt(Instant.now());
        entry.setUpdatedAt(Instant.now());
        String afterJson = writeJson(Map.of(
                "status", entry.getStatus().name(),
                "membershipOverrideByAdminId", String.valueOf(adminUserId)
        ));
        writeAudit(entry.getId(), adminUserId, "MEMBERSHIP_OVERRIDE", reason, beforeJson, afterJson);
        return EntryDto.from(entry);
    }

    private void writeAudit(Long entryId, Long adminId, String action, String reason,
                            String beforeJson, String afterJson) {
        EntryAuditLog log = new EntryAuditLog();
        log.setEntryId(entryId);
        log.setAdminUserId(adminId);
        log.setAction(action);
        log.setReason(reason);
        log.setBeforeSnapshot(beforeJson);
        log.setAfterSnapshot(afterJson);
        log.setCreatedAt(Instant.now());
        auditLogRepository.save(log);
    }

    private String writeJson(Map<String, ?> m) {
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize audit snapshot", e);
        }
    }
}
