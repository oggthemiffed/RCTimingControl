package dev.monkeypatch.rctiming.localday.race;

import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link RoundGeneratorService#applyPreviousRoundFinishingOrder}. Mirrors
 * {@code RaceStateMachineServiceTest}'s style — plain JUnit 5 + AssertJ + a mocked repository,
 * no Spring context.
 */
class RoundGeneratorServiceTest {

    private CachedRaceEntryRepository cachedRaceEntryRepository;
    private RoundGeneratorService service;

    @BeforeEach
    void setUp() {
        cachedRaceEntryRepository = Mockito.mock(CachedRaceEntryRepository.class);
        service = new RoundGeneratorService(cachedRaceEntryRepository);
    }

    private CachedRaceEntry entry(Long scheduleId, Long entryId) {
        CachedRaceEntry e = new CachedRaceEntry();
        e.setCachedScheduleId(scheduleId);
        e.setCachedEntryId(entryId);
        return e;
    }

    @Test
    void applyPreviousRoundFinishingOrder_setsGridPositionsInFinishingOrder() {
        CachedRaceEntry e1 = entry(10L, 101L);
        CachedRaceEntry e2 = entry(10L, 102L);
        CachedRaceEntry e3 = entry(10L, 103L);
        Mockito.when(cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(10L))
                .thenReturn(List.of(e1, e2, e3));
        Mockito.when(cachedRaceEntryRepository.save(Mockito.any(CachedRaceEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Finishing order: 103 first, then 101, then 102
        service.applyPreviousRoundFinishingOrder(10L, List.of(103L, 101L, 102L));

        assertThat(e3.getGridPosition()).isEqualTo(1);
        assertThat(e1.getGridPosition()).isEqualTo(2);
        assertThat(e2.getGridPosition()).isEqualTo(3);
    }

    @Test
    void applyPreviousRoundFinishingOrder_leavesUnrelatedScheduleRowsUntouched() {
        // Only schedule 10's entries are returned by the mocked finder — entries belonging to
        // a different schedule id are never fetched, and therefore never touched.
        CachedRaceEntry other = entry(99L, 999L);
        other.setGridPosition(7);

        Mockito.when(cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(10L))
                .thenReturn(List.of());

        service.applyPreviousRoundFinishingOrder(10L, List.of(101L, 102L));

        assertThat(other.getGridPosition()).isEqualTo(7);
        Mockito.verify(cachedRaceEntryRepository, Mockito.never()).save(other);
    }

    @Test
    void applyPreviousRoundFinishingOrder_ignoresEntriesNotInFinishingOrderList() {
        CachedRaceEntry known = entry(10L, 101L);
        CachedRaceEntry unknown = entry(10L, 555L); // e.g. a late DNS entry not in the results
        Mockito.when(cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(10L))
                .thenReturn(List.of(known, unknown));
        Mockito.when(cachedRaceEntryRepository.save(Mockito.any(CachedRaceEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.applyPreviousRoundFinishingOrder(10L, List.of(101L));

        assertThat(known.getGridPosition()).isEqualTo(1);
        assertThat(unknown.getGridPosition()).isNull();
    }
}
