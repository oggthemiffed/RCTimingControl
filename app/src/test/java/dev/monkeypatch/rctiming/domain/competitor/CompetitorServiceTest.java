package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompetitorServiceTest {

    @Test
    void createWalkIn_savesATrimmedNameWithNoExternalIdentity() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.getOrThrow(anyLong())).thenCallRealMethod();
        CompetitorAuditLogRepository audit = mock(CompetitorAuditLogRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Competitor competitor = new CompetitorService(repo, audit).createWalkIn("  Ada Lovelace ");

        assertThat(competitor.getDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(competitor.getExternalSource()).isNull();
        assertThat(competitor.getExternalId()).isNull();
        assertThat(competitor.getCreatedAt()).isNotNull();
    }

    private static Competitor existing() {
        Competitor c = new Competitor();
        c.setId(7L);
        c.setDisplayName("Siobhan Keane");
        return c;
    }

    @Test
    void setSpokenName_savesATrimmedValue() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.getOrThrow(anyLong())).thenCallRealMethod();
        CompetitorAuditLogRepository audit = mock(CompetitorAuditLogRepository.class);
        when(repo.findById(7L)).thenReturn(Optional.of(existing()));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Competitor c = new CompetitorService(repo, audit).setSpokenName(7L, "  Shiv-awn Keen ", 3L);

        assertThat(c.getSpokenName()).isEqualTo("Shiv-awn Keen");
        assertThat(c.speechName()).isEqualTo("Shiv-awn Keen");
        assertThat(c.getDisplayName()).isEqualTo("Siobhan Keane");
        assertThat(c.getUpdatedAt()).isNotNull();
    }

    @Test
    void setSpokenName_recordsWhoChangedItAndWhatItWas() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.getOrThrow(anyLong())).thenCallRealMethod();
        CompetitorAuditLogRepository audit = mock(CompetitorAuditLogRepository.class);
        Competitor before = existing();
        before.setSpokenName("Old say-as");
        when(repo.findById(7L)).thenReturn(Optional.of(before));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        new CompetitorService(repo, audit).setSpokenName(7L, "New say-as", 42L);

        ArgumentCaptor<CompetitorAuditLog> logged = ArgumentCaptor.forClass(CompetitorAuditLog.class);
        verify(audit).save(logged.capture());
        assertThat(logged.getValue().getCompetitorId()).isEqualTo(7L);
        assertThat(logged.getValue().getActorUserId()).isEqualTo(42L);
        assertThat(logged.getValue().getAction()).isEqualTo(CompetitorAuditLog.SPOKEN_NAME_CHANGED);
        assertThat(logged.getValue().getBeforeValue()).isEqualTo("Old say-as");
        assertThat(logged.getValue().getAfterValue()).isEqualTo("New say-as");
        assertThat(logged.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void setSpokenName_clearingItIsRecordedAsNone() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.getOrThrow(anyLong())).thenCallRealMethod();
        CompetitorAuditLogRepository audit = mock(CompetitorAuditLogRepository.class);
        Competitor before = existing();
        before.setSpokenName("Old say-as");
        when(repo.findById(7L)).thenReturn(Optional.of(before));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        new CompetitorService(repo, audit).setSpokenName(7L, "  ", 42L);

        ArgumentCaptor<CompetitorAuditLog> logged = ArgumentCaptor.forClass(CompetitorAuditLog.class);
        verify(audit).save(logged.capture());
        assertThat(logged.getValue().getBeforeValue()).isEqualTo("Old say-as");
        assertThat(logged.getValue().getAfterValue()).isNull();
    }

    @Test
    void setSpokenName_savingTheSameValueChangesAndRecordsNothing() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.getOrThrow(anyLong())).thenCallRealMethod();
        CompetitorAuditLogRepository audit = mock(CompetitorAuditLogRepository.class);
        Competitor before = existing();
        before.setSpokenName("Same say-as");
        when(repo.findById(7L)).thenReturn(Optional.of(before));

        new CompetitorService(repo, audit).setSpokenName(7L, " Same say-as ", 42L);

        verify(repo, never()).save(any());
        verify(audit, never()).save(any());
    }

    @Test
    void setSpokenName_blankClearsItAndTheDisplayNameIsSpokenAgain() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.getOrThrow(anyLong())).thenCallRealMethod();
        CompetitorAuditLogRepository audit = mock(CompetitorAuditLogRepository.class);
        Competitor withSpoken = existing();
        withSpoken.setSpokenName("Shiv-awn Keen");
        when(repo.findById(7L)).thenReturn(Optional.of(withSpoken));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Competitor c = new CompetitorService(repo, audit).setSpokenName(7L, "   ", 3L);

        assertThat(c.getSpokenName()).isNull();
        assertThat(c.speechName()).isEqualTo("Siobhan Keane");
    }

    @Test
    void setSpokenName_refusesTextOverTheLimit() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.getOrThrow(anyLong())).thenCallRealMethod();
        CompetitorAuditLogRepository audit = mock(CompetitorAuditLogRepository.class);

        assertThatThrownBy(() -> new CompetitorService(repo, audit)
                .setSpokenName(7L, "x".repeat(CompetitorService.MAX_SPOKEN_NAME_LENGTH + 1), 3L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void setSpokenName_unknownCompetitorIsNotFound() {
        CompetitorRepository repo = mock(CompetitorRepository.class);
        when(repo.getOrThrow(anyLong())).thenCallRealMethod();
        CompetitorAuditLogRepository audit = mock(CompetitorAuditLogRepository.class);
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CompetitorService(repo, audit).setSpokenName(99L, "x", 3L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
