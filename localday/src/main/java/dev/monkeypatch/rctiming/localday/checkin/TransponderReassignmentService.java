package dev.monkeypatch.rctiming.localday.checkin;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Transponder reassignment for an existing entry (R8) — e.g. for equipment failure on race day,
 * entirely local with no cloud dependency (AE1). Every reassignment is recorded in
 * {@link TransponderReassignmentAudit} with the acting official's identity.
 */
@Service
public class TransponderReassignmentService {

    private final CachedEntryRepository cachedEntryRepository;
    private final TransponderReassignmentAuditRepository auditRepository;

    public TransponderReassignmentService(CachedEntryRepository cachedEntryRepository,
                                           TransponderReassignmentAuditRepository auditRepository) {
        this.cachedEntryRepository = cachedEntryRepository;
        this.auditRepository = auditRepository;
    }

    public ReassignResult reassign(Long cachedEntryId, String newTransponderNumber,
                                    Long actingCredentialId, String actingOfficialName) {
        Optional<CachedEntry> found = cachedEntryRepository.findById(cachedEntryId);
        if (found.isEmpty()) {
            return new ReassignResult.EntryNotFound();
        }
        CachedEntry entry = found.get();

        // Reassigning an entry to the transponder it already has is a harmless no-op, not a
        // conflict.
        if (newTransponderNumber.equals(entry.getTransponderNumber())) {
            return new ReassignResult.Success(entry, entry.getTransponderNumber());
        }

        Optional<CachedEntry> holder = cachedEntryRepository.findByTransponderNumber(newTransponderNumber);
        if (holder.isPresent() && !holder.get().getId().equals(entry.getId())) {
            return new ReassignResult.TransponderAlreadyAssigned();
        }

        String oldTransponderNumber = entry.getTransponderNumber();
        entry.setTransponderNumber(newTransponderNumber);
        cachedEntryRepository.save(entry);

        TransponderReassignmentAudit audit = new TransponderReassignmentAudit();
        audit.setCachedEntryId(entry.getId());
        audit.setOldTransponderNumber(oldTransponderNumber);
        audit.setNewTransponderNumber(newTransponderNumber);
        audit.setActingCredentialId(actingCredentialId);
        audit.setActingOfficialName(actingOfficialName);
        audit.setReassignedAt(Instant.now());
        auditRepository.save(audit);

        return new ReassignResult.Success(entry, oldTransponderNumber);
    }
}
