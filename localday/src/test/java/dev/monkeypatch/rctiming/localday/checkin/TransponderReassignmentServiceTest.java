package dev.monkeypatch.rctiming.localday.checkin;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Pure unit tests for {@link TransponderReassignmentService}'s reassignment/conflict logic.
 * Mirrors the style of {@code LocalSessionServiceTest} — plain JUnit 5 + AssertJ, no Spring
 * context. {@link CachedEntryRepository} and {@link TransponderReassignmentAuditRepository} are
 * both mocked.
 */
class TransponderReassignmentServiceTest {

    private CachedEntryRepository cachedEntryRepository;
    private TransponderReassignmentAuditRepository auditRepository;
    private TransponderReassignmentService service;

    @BeforeEach
    void setUp() {
        cachedEntryRepository = Mockito.mock(CachedEntryRepository.class);
        auditRepository = Mockito.mock(TransponderReassignmentAuditRepository.class);
        service = new TransponderReassignmentService(cachedEntryRepository, auditRepository);

        Mockito.when(cachedEntryRepository.save(any(CachedEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(auditRepository.save(any(TransponderReassignmentAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CachedEntry entry(Long id, String transponderNumber, String racerName) {
        CachedEntry e = new CachedEntry();
        e.setId(id);
        e.setCloudEntryId(id);
        e.setTransponderNumber(transponderNumber);
        e.setRacerName(racerName);
        return e;
    }

    // --- Happy path ---

    @Test
    void reassign_toFreeTransponderNumber_succeeds_updatesEntry_persistsAuditRow() {
        CachedEntry target = entry(5L, "1234567", "Jane Doe");
        Mockito.when(cachedEntryRepository.findById(5L)).thenReturn(Optional.of(target));
        Mockito.when(cachedEntryRepository.findByTransponderNumber("7654321")).thenReturn(Optional.empty());

        ReassignResult result = service.reassign(5L, "7654321", 42L, "Race Director Bob");

        assertThat(result).isInstanceOf(ReassignResult.Success.class);
        ReassignResult.Success success = (ReassignResult.Success) result;
        assertThat(success.oldTransponderNumber()).isEqualTo("1234567");
        assertThat(success.entry().getTransponderNumber()).isEqualTo("7654321");
        assertThat(target.getTransponderNumber()).isEqualTo("7654321");

        Mockito.verify(cachedEntryRepository).save(target);

        ArgumentCaptor<TransponderReassignmentAudit> auditCaptor =
                ArgumentCaptor.forClass(TransponderReassignmentAudit.class);
        Mockito.verify(auditRepository).save(auditCaptor.capture());
        TransponderReassignmentAudit audit = auditCaptor.getValue();
        assertThat(audit.getCachedEntryId()).isEqualTo(5L);
        assertThat(audit.getOldTransponderNumber()).isEqualTo("1234567");
        assertThat(audit.getNewTransponderNumber()).isEqualTo("7654321");
        assertThat(audit.getActingCredentialId()).isEqualTo(42L);
        assertThat(audit.getActingOfficialName()).isEqualTo("Race Director Bob");
        assertThat(audit.getReassignedAt()).isNotNull();
    }

    // --- Edge case: reassign to own current number ---

    @Test
    void reassign_toOwnCurrentTransponderNumber_isHarmlessNoOpSuccess_notConflict() {
        CachedEntry target = entry(5L, "1234567", "Jane Doe");
        Mockito.when(cachedEntryRepository.findById(5L)).thenReturn(Optional.of(target));

        ReassignResult result = service.reassign(5L, "1234567", 42L, "Race Director Bob");

        assertThat(result).isInstanceOf(ReassignResult.Success.class);
        ReassignResult.Success success = (ReassignResult.Success) result;
        assertThat(success.oldTransponderNumber()).isEqualTo("1234567");
        assertThat(success.entry().getTransponderNumber()).isEqualTo("1234567");

        // No mutation, no audit trail, and no lookup for a "conflicting" holder needed for a
        // same-number no-op.
        Mockito.verify(cachedEntryRepository, Mockito.never()).save(any());
        Mockito.verifyNoInteractions(auditRepository);
    }

    // --- Error path: conflict ---

    @Test
    void reassign_toTransponderHeldByDifferentEntry_returnsAlreadyAssigned_doesNotMutateEitherEntry() {
        CachedEntry target = entry(5L, "1234567", "Jane Doe");
        CachedEntry otherHolder = entry(9L, "7654321", "John Smith");
        Mockito.when(cachedEntryRepository.findById(5L)).thenReturn(Optional.of(target));
        Mockito.when(cachedEntryRepository.findByTransponderNumber("7654321")).thenReturn(Optional.of(otherHolder));

        ReassignResult result = service.reassign(5L, "7654321", 42L, "Race Director Bob");

        assertThat(result).isInstanceOf(ReassignResult.TransponderAlreadyAssigned.class);
        assertThat(target.getTransponderNumber()).isEqualTo("1234567");
        assertThat(otherHolder.getTransponderNumber()).isEqualTo("7654321");
        Mockito.verify(cachedEntryRepository, Mockito.never()).save(any());
        Mockito.verifyNoInteractions(auditRepository);
    }

    // --- Error path: entry not found ---

    @Test
    void reassign_nonexistentEntry_returnsEntryNotFound() {
        Mockito.when(cachedEntryRepository.findById(999L)).thenReturn(Optional.empty());

        ReassignResult result = service.reassign(999L, "7654321", 42L, "Race Director Bob");

        assertThat(result).isInstanceOf(ReassignResult.EntryNotFound.class);
        Mockito.verify(cachedEntryRepository, Mockito.never()).save(any());
        Mockito.verifyNoInteractions(auditRepository);
    }
}
