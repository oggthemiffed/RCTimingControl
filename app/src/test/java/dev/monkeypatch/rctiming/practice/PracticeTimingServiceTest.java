package dev.monkeypatch.rctiming.practice;

import dev.monkeypatch.rctiming.domain.practice.PracticeLap;
import dev.monkeypatch.rctiming.domain.practice.PracticeLapRepository;
import dev.monkeypatch.rctiming.domain.practice.PracticeSession;
import dev.monkeypatch.rctiming.domain.practice.PracticeSessionRepository;
import dev.monkeypatch.rctiming.domain.practice.PracticeStatus;
import dev.monkeypatch.rctiming.domain.transponder.Transponder;
import dev.monkeypatch.rctiming.domain.transponder.TransponderRepository;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.practice.dto.PracticeTimingRowDto;
import dev.monkeypatch.rctiming.timing.LapPassingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PracticeTimingService.
 * Uses Mockito mocks for all repositories and the hub.
 * No Spring context required.
 */
@ExtendWith(MockitoExtension.class)
class PracticeTimingServiceTest {

    @Mock
    PracticeSessionRepository sessionRepository;
    @Mock
    PracticeLapRepository lapRepository;
    @Mock
    TransponderRepository transponderRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    PracticeTimingHub timingHub;

    PracticeTimingService service;
    PracticeSession runningSession;

    @BeforeEach
    void setUp() {
        service = new PracticeTimingService(
                sessionRepository, lapRepository,
                transponderRepository, userRepository, timingHub);

        runningSession = new PracticeSession();
        runningSession.setName("Test Session");
        // Use reflection to set id since JPA @GeneratedValue won't be called in unit tests
        try {
            var idField = PracticeSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(runningSession, 42L);
            var statusField = PracticeSession.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(runningSession, PracticeStatus.RUNNING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void onLapPassingEvent_runningSession_recordsLap() {
        // First passing (no previous RTC — no lap time)
        when(sessionRepository.findRunningSession()).thenReturn(Optional.of(runningSession));
        when(transponderRepository.findByTransponderNumber("T1"))
                .thenReturn(Optional.empty());
        when(lapRepository.findByPracticeSessionIdAndTransponderNumberOrderByLapNumberAsc(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());

        service.startSession(runningSession);
        service.onLapPassing(new LapPassingEvent(0L, "T1", 1_000_000_000L));

        // Second passing — lap time calculated
        service.onLapPassing(new LapPassingEvent(0L, "T1", 1_060_000_000L)); // 60s gap

        // A lap record should have been persisted for the second passing
        verify(lapRepository, times(1)).save(any(PracticeLap.class));
    }

    @Test
    void onLapPassingEvent_noSession_ignored() {
        when(sessionRepository.findRunningSession()).thenReturn(Optional.empty());

        service.onLapPassing(new LapPassingEvent(0L, "T1", 1_000_000_000L));

        verify(lapRepository, never()).save(any());
        verifyNoInteractions(timingHub);
    }

    @Test
    void onLapPassingEvent_unknownTransponder_recordsWithNullUser() {
        when(sessionRepository.findRunningSession()).thenReturn(Optional.of(runningSession));
        when(transponderRepository.findByTransponderNumber("UNKNOWN"))
                .thenReturn(Optional.empty());
        when(lapRepository.findByPracticeSessionIdAndTransponderNumberOrderByLapNumberAsc(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());

        service.startSession(runningSession);
        // Two passings to generate a lap time
        service.onLapPassing(new LapPassingEvent(0L, "UNKNOWN", 1_000_000_000L));
        service.onLapPassing(new LapPassingEvent(0L, "UNKNOWN", 1_060_000_000L));

        // Lap should be saved with null user
        ArgumentCaptor<PracticeLap> captor = ArgumentCaptor.forClass(PracticeLap.class);
        verify(lapRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isNull();
        assertThat(captor.getValue().getTransponderNumber()).isEqualTo("UNKNOWN");
    }

    @Test
    void getSnapshot_returnsCurrentPositions() {
        when(sessionRepository.findRunningSession()).thenReturn(Optional.of(runningSession));
        when(transponderRepository.findByTransponderNumber(anyString()))
                .thenReturn(Optional.empty());
        when(lapRepository.findByPracticeSessionIdAndTransponderNumberOrderByLapNumberAsc(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());

        service.startSession(runningSession);
        // Record 2 passings to get 1 lap time
        service.onLapPassing(new LapPassingEvent(0L, "T1", 1_000_000_000L));
        service.onLapPassing(new LapPassingEvent(0L, "T1", 1_060_000_000L));

        List<PracticeTimingRowDto> snapshot = service.getSnapshot(42L);

        assertThat(snapshot).hasSize(1);
        assertThat(snapshot.get(0).transponderNumber()).isEqualTo("T1");
        assertThat(snapshot.get(0).laps()).isEqualTo(1);
        assertThat(snapshot.get(0).isUnknown()).isTrue();
    }

    @Test
    void broadcastsViaStompAfterEachPassing() {
        when(sessionRepository.findRunningSession()).thenReturn(Optional.of(runningSession));
        when(transponderRepository.findByTransponderNumber(anyString()))
                .thenReturn(Optional.empty());
        when(lapRepository.findByPracticeSessionIdAndTransponderNumberOrderByLapNumberAsc(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());

        service.startSession(runningSession);
        service.onLapPassing(new LapPassingEvent(0L, "T1", 1_000_000_000L));
        service.onLapPassing(new LapPassingEvent(0L, "T1", 1_060_000_000L));

        // broadcastTimingUpdate called once per passing = 2 times total
        verify(timingHub, times(2)).broadcastTimingUpdate(eq(42L), any());
    }
}
