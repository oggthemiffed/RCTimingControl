package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.audit.AuditService;
import dev.monkeypatch.rctiming.domain.race.RaceAuditLabels;
import dev.monkeypatch.rctiming.timing.LapTimingService;
import dev.monkeypatch.rctiming.timing.UnknownTransponderLinkAudit;
import dev.monkeypatch.rctiming.timing.UnknownTransponderLinkAuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransponderLinkServiceTest {

    @Mock LapTimingService lapTimingService;
    @Mock UnknownTransponderLinkAuditRepository linkAuditRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) AuditService audit;
    @Mock RaceAuditLabels labels;

    private TransponderLinkService service() {
        return new TransponderLinkService(lapTimingService, linkAuditRepository, audit, labels);
    }

    @Test
    void link_recordsItThenCreditsThePassingsCountedBeforehand() {
        when(lapTimingService.countPassingsForTransponder(7L, "1234567")).thenReturn(4);

        int lapsCredited = service().link(7L, "1234567", 11L, 3L);

        assertThat(lapsCredited).isEqualTo(4);
        ArgumentCaptor<UnknownTransponderLinkAudit> row = ArgumentCaptor.forClass(UnknownTransponderLinkAudit.class);
        InOrder order = inOrder(lapTimingService, linkAuditRepository);
        order.verify(lapTimingService).countPassingsForTransponder(7L, "1234567");
        order.verify(linkAuditRepository).save(row.capture());
        order.verify(lapTimingService).linkTransponder(7L, "1234567", 11L);
        assertThat(row.getValue().getTransponderNumber()).isEqualTo("1234567");
        assertThat(row.getValue().getEntryId()).isEqualTo(11L);
        assertThat(row.getValue().getLinkedByUserId()).isEqualTo(3L);
    }

    @Test
    void link_whenRecordingFails_doesNotLink() {
        when(linkAuditRepository.save(any())).thenThrow(new IllegalStateException("disk full"));

        assertThatThrownBy(() -> service().link(7L, "1234567", 11L, 3L)).isInstanceOf(IllegalStateException.class);

        verify(lapTimingService, never()).linkTransponder(any(Long.class), any(), any(Long.class));
    }
}
