package dev.monkeypatch.rctiming.domain.checkin;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryAuditLog;
import dev.monkeypatch.rctiming.domain.entry.EntryAuditLogRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Swaps an entry's primary or secondary transponder on the day (L11). Every change is written to the entry audit log as
 * TRANSPONDER_SWAP. Lap timing looks transponders up on each passing, so the new number
 * counts from the next passing.
 */
@Service
public class TransponderSwapService {

    static final String AUDIT_ACTION = "TRANSPONDER_SWAP";

    private final EventRepository eventRepository;
    private final EntryRepository entryRepository;
    private final EntryAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public TransponderSwapService(EventRepository eventRepository,
                                  EntryRepository entryRepository,
                                  EntryAuditLogRepository auditLogRepository,
                                  ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.entryRepository = entryRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Swaps for one event are serialized by locking the event row, so two officials cannot both
     * give the same free number to different competitors.
     *
     * @param newNumber the new number; blank removes a secondary transponder
     */
    @Transactional
    public SwapResult swap(long eventId, long entryId, TransponderSlot slot, String newNumber, long actingUserId) {
        if (eventRepository.findByIdForUpdate(eventId).isEmpty()) {
            return new SwapResult.EntryNotFound();
        }
        Entry entry = entryRepository.findByIdForUpdate(entryId).orElse(null);
        if (entry == null || !Objects.equals(entry.getEventId(), eventId)) {
            return new SwapResult.EntryNotFound();
        }
        if (entry.getStatus() == EntryStatus.WITHDRAWN) {
            return new SwapResult.EntryWithdrawn();
        }

        String normalized = newNumber == null || newNumber.isBlank() ? null : newNumber.trim();
        if (normalized == null && slot == TransponderSlot.PRIMARY) {
            return new SwapResult.PrimaryRequired();
        }

        String oldNumber = slot == TransponderSlot.PRIMARY
                ? entry.getTransponderNumberSnapshot()
                : entry.getSecondaryTransponderNumber();
        if (Objects.equals(oldNumber, normalized)) {
            return new SwapResult.Success(entry, slot, oldNumber, normalized);
        }

        if (normalized != null) {
            String otherSlot = slot == TransponderSlot.PRIMARY
                    ? entry.getSecondaryTransponderNumber()
                    : entry.getTransponderNumberSnapshot();
            if (normalized.equals(otherSlot)) {
                return new SwapResult.SameAsOtherSlot();
            }
            if (heldByAnotherCompetitor(entry, normalized)) {
                return new SwapResult.TransponderAlreadyAssigned();
            }
        }

        if (slot == TransponderSlot.PRIMARY) {
            entry.setTransponderNumberSnapshot(normalized);
        } else {
            entry.setSecondaryTransponderNumber(normalized);
        }
        Instant now = Instant.now();
        entry.setUpdatedAt(now);
        entryRepository.save(entry);
        writeAudit(entry.getId(), actingUserId, slot, oldNumber, normalized, now);
        return new SwapResult.Success(entry, slot, oldNumber, normalized);
    }

    /**
     * A competitor racing two classes may share one transponder across their entries, so only
     * another competitor's live entry in the same event blocks the number.
     */
    private boolean heldByAnotherCompetitor(Entry entry, String number) {
        return entryRepository.findByEventId(entry.getEventId()).stream()
                .filter(other -> !other.getId().equals(entry.getId()))
                .filter(other -> other.getStatus() != EntryStatus.WITHDRAWN)
                .filter(other -> !Objects.equals(other.getCompetitorId(), entry.getCompetitorId()))
                .anyMatch(other -> number.equals(other.getTransponderNumberSnapshot())
                        || number.equals(other.getSecondaryTransponderNumber()));
    }

    private void writeAudit(Long entryId, long actingUserId, TransponderSlot slot,
                            String oldNumber, String newNumber, Instant at) {
        EntryAuditLog log = new EntryAuditLog();
        log.setEntryId(entryId);
        log.setAdminUserId(actingUserId);
        log.setAction(AUDIT_ACTION);
        log.setBeforeSnapshot(json(slot, oldNumber));
        log.setAfterSnapshot(json(slot, newNumber));
        log.setCreatedAt(at);
        auditLogRepository.save(log);
    }

    private String json(TransponderSlot slot, String number) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("slot", slot.name());
        m.put("transponderNumber", number);
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize audit snapshot", e);
        }
    }
}
