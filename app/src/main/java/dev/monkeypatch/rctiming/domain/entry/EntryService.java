package dev.monkeypatch.rctiming.domain.entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.admin.dto.AdminCreateEntryRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EntryDto;
import dev.monkeypatch.rctiming.api.admin.dto.EntryResult;
import dev.monkeypatch.rctiming.domain.competitor.Competitor;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorRepository;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorService;
import dev.monkeypatch.rctiming.domain.competitor.PossibleDuplicateCompetitorException;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.event.EventStatus;
import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import dev.monkeypatch.rctiming.domain.StateConflictException;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;

@Service
@Transactional
public class EntryService {

    private final EntryRepository entryRepository;
    private final EventRepository eventRepository;
    private final DSLContext dsl;
    private final EntryAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final CompetitorService competitorService;
    private final CompetitorRepository competitorRepository;

    public EntryService(EntryRepository entryRepository,
                        EventRepository eventRepository,
                        DSLContext dsl,
                        EntryAuditLogRepository auditLogRepository,
                        ObjectMapper objectMapper,
                        CompetitorService competitorService,
                        CompetitorRepository competitorRepository) {
        this.entryRepository = entryRepository;
        this.eventRepository = eventRepository;
        this.dsl = dsl;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.competitorService = competitorService;
        this.competitorRepository = competitorRepository;
    }

    /**
     * Adds a walk-in entry by hand (L9, #17): an existing competitor or a new one by name, a primary
     * transponder and an optional secondary. The entry is confirmed straight away, has no login and
     * no external source. A transponder another active entry in the event already uses is a
     * warning, not an error. Class membership rules are not checked: staff add walk-ins on the day.
     */
    public EntryResult adminCreateEntry(Long adminUserId, AdminCreateEntryRequest req) {
        Event event = eventRepository.getOrThrow(req.eventId());
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new StateConflictException("Event is completed");
        }
        Long ecEventId = dsl.select(EVENT_CLASSES.EVENT_ID)
                .from(EVENT_CLASSES)
                .where(EVENT_CLASSES.ID.eq(req.eventClassId()))
                .fetchOptional(EVENT_CLASSES.EVENT_ID)
                .orElseThrow(() -> new EntityNotFoundException("Event class not found: " + req.eventClassId()));
        if (!req.eventId().equals(ecEventId)) {
            throw new IllegalArgumentException("Event class does not belong to the event");
        }

        String primary = req.primaryTransponder().trim();
        String secondary = req.secondaryTransponder() == null || req.secondaryTransponder().isBlank()
                ? null : req.secondaryTransponder().trim();
        if (primary.equals(secondary)) {
            throw new IllegalArgumentException("The secondary transponder must differ from the primary");
        }

        boolean hasName = req.competitorName() != null && !req.competitorName().isBlank();
        if (req.competitorId() != null && hasName) {
            throw new IllegalArgumentException("Give either an existing competitor or a new name, not both");
        }
        Competitor competitor;
        if (req.competitorId() != null) {
            competitor = competitorRepository.getOrThrow(req.competitorId());
        } else if (hasName) {
            List<Competitor> possible = competitorService.findPossibleDuplicates(req.competitorName());
            if (!possible.isEmpty() && !Boolean.TRUE.equals(req.confirmNewCompetitor())) {
                throw new PossibleDuplicateCompetitorException(possible);
            }
            competitor = competitorService.createWalkIn(req.competitorName());
        } else {
            throw new IllegalArgumentException("Choose a competitor or enter a name");
        }

        List<Entry> eventEntries = entryRepository.findByEventId(event.getId()).stream()
                .filter(e -> e.getStatus() != EntryStatus.WITHDRAWN)
                .toList();
        boolean duplicate = eventEntries.stream().anyMatch(e ->
                competitor.getId().equals(e.getCompetitorId()) && req.eventClassId().equals(e.getEventClassId()));
        if (duplicate) {
            throw new StateConflictException(competitor.getDisplayName() + " already has an entry in this class");
        }

        Instant now = Instant.now();
        Entry entry = new Entry();
        entry.setCompetitorId(competitor.getId());
        entry.setEventId(event.getId());
        entry.setEventClassId(req.eventClassId());
        entry.setTransponderNumberSnapshot(primary);
        entry.setSecondaryTransponderNumber(secondary);
        entry.setStatus(EntryStatus.CONFIRMED);
        entry.setSubmittedAt(now);
        entry.setConfirmedAt(now);
        entry.setUpdatedAt(now);
        Entry persisted = entryRepository.save(entry);

        List<String> warnings = new ArrayList<>();
        for (String number : secondary == null ? List.of(primary) : List.of(primary, secondary)) {
            boolean inUse = eventEntries.stream().anyMatch(e ->
                    number.equals(e.getTransponderNumberSnapshot()) || number.equals(e.getSecondaryTransponderNumber()));
            if (inUse) {
                warnings.add("Transponder " + number + " is already used by another entry in this event.");
            }
        }

        Map<String, Object> created = new LinkedHashMap<>();
        created.put("competitorId", String.valueOf(competitor.getId()));
        created.put("eventClassId", String.valueOf(req.eventClassId()));
        created.put("transponderNumberSnapshot", primary);
        created.put("secondaryTransponderNumber", secondary);
        String afterJson = EntryAuditLog.snapshot(objectMapper, created);
        auditLogRepository.save(
                EntryAuditLog.of(persisted.getId(), adminUserId, "ADMIN_CREATE", null, null, afterJson, now));
        return new EntryResult(EntryDto.from(persisted), warnings);
    }

    public EntryDto adminWithdraw(Long entryId, Long adminUserId, String reason) {
        Entry entry = entryRepository.getOrThrow(entryId);
        if (entry.getStatus() == EntryStatus.WITHDRAWN) {
            throw new StateConflictException("Entry already withdrawn");
        }
        String beforeJson = EntryAuditLog.snapshot(objectMapper, Map.of("status", entry.getStatus().name()));
        Instant now = Instant.now();
        entry.setStatus(EntryStatus.WITHDRAWN);
        entry.setWithdrawnAt(now);
        entry.setUpdatedAt(now);
        entryRepository.save(entry);
        String afterJson = EntryAuditLog.snapshot(objectMapper, Map.of("status", EntryStatus.WITHDRAWN.name()));
        auditLogRepository.save(EntryAuditLog.of(entry.getId(), adminUserId, "ADMIN_WITHDRAW", reason,
                beforeJson, afterJson, now));
        return EntryDto.from(entry);
    }
}
