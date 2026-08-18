package dev.monkeypatch.rctiming.localday.timing;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
import dev.monkeypatch.rctiming.localday.timing.dto.LiveTimingRowDto;
import dev.monkeypatch.rctiming.localday.timing.dto.MarshalAdjustmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Pure Mockito unit tests for {@link LapTimingService} — no Spring context.
 */
class LapTimingServiceTest {

    private LiveTimingHub liveTimingHub;
    private CachedEntryRepository cachedEntryRepository;
    private CachedRaceEntryRepository cachedRaceEntryRepository;
    private LapTimingService service;

    @BeforeEach
    void setUp() {
        liveTimingHub = Mockito.mock(LiveTimingHub.class);
        cachedEntryRepository = Mockito.mock(CachedEntryRepository.class);
        cachedRaceEntryRepository = Mockito.mock(CachedRaceEntryRepository.class);
        Mockito.when(cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(anyLong()))
                .thenReturn(List.of());
        service = new LapTimingService(liveTimingHub, cachedEntryRepository, cachedRaceEntryRepository);
    }

    // --- Edge case: no active race ---

    @Test
    void onLapPassing_nullScheduleId_doesNotCreateStateOrBroadcast() {
        LapPassingEvent event = new LapPassingEvent(null, "1234567", 1_000_000L);

        service.onLapPassing(event);

        assertThat(service.peek(1L)).isEmpty();
        Mockito.verifyNoInteractions(liveTimingHub);
        Mockito.verifyNoInteractions(cachedEntryRepository);
    }

    // --- Happy path: marshal adjustment ---

    @Test
    void applyMarshalAdjustment_updatesLapsCompletedAndBroadcastsBoth() {
        long scheduleId = 9L;
        long entryId = 42L;

        // Seed the entry with 2 laps via a lap passing first.
        CachedEntry entry = new CachedEntry();
        entry.setId(entryId);
        entry.setTransponderNumber("1234567");
        entry.setRacerName("Ada Lovelace");
        Mockito.when(cachedEntryRepository.findByTransponderNumber("1234567"))
                .thenReturn(Optional.of(entry));

        service.onLapPassing(new LapPassingEvent(scheduleId, "1234567", 1_000_000L));
        service.onLapPassing(new LapPassingEvent(scheduleId, "1234567", 11_000_000L));

        MarshalAdjustmentDto dto = new MarshalAdjustmentDto(
                scheduleId, entryId, "1234567", 1, "Referee Bob", 123L);

        service.applyMarshalAdjustment(scheduleId, entryId, 1, dto);

        Optional<LiveRaceState> state = service.peek(scheduleId);
        assertThat(state).isPresent();
        List<LiveTimingRowDto> rows = state.get().calculatePositions();
        assertThat(rows).hasSize(1);
        // 2 laps from passings + 1 marshal delta = 3.
        assertThat(rows.get(0).lapsCompleted()).isEqualTo(3);

        Mockito.verify(liveTimingHub).broadcastMarshalAdjustment(eq(scheduleId), eq(dto));
        // broadcastTimingUpdate is called once per passing plus once for the adjustment.
        Mockito.verify(liveTimingHub, Mockito.times(3))
                .broadcastTimingUpdate(eq(scheduleId), Mockito.anyList());
    }

    @Test
    void onLapPassing_unknownTransponder_broadcastsUnknownTransponderOnlyOnce() {
        long scheduleId = 3L;
        Mockito.when(cachedEntryRepository.findByTransponderNumber("9999999"))
                .thenReturn(Optional.empty());

        service.onLapPassing(new LapPassingEvent(scheduleId, "9999999", 1_000_000L));
        service.onLapPassing(new LapPassingEvent(scheduleId, "9999999", 11_000_000L));

        Mockito.verify(liveTimingHub, Mockito.times(1))
                .broadcastUnknownTransponder(scheduleId, "9999999");
        Mockito.verify(liveTimingHub, Mockito.times(2))
                .broadcastTimingUpdate(eq(scheduleId), Mockito.anyList());
    }
}
