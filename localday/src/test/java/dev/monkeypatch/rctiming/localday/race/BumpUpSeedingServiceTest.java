package dev.monkeypatch.rctiming.localday.race;

import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for {@link BumpUpSeedingService}. Mirrors {@code RaceStateMachineServiceTest}'s
 * style — plain JUnit 5 + AssertJ + mocked repositories, no Spring context.
 *
 * <p>Note: the cloud reference's original {@code seedFinals} pointer arithmetic did not actually
 * satisfy its own javadoc narrative ("A-Final positions 1-8: drivers ranked 1-8") — verified by
 * hand-simulation, it silently dropped the top-ranked qualifiers from any regular slot instead.
 * Both the cloud (`app`) and this local reimplementation have since been corrected so non-lowest
 * finals draw from the *top* of standings (processed best-final-first) while the lowest final
 * still draws from the bottom — this now matches the documented worked example exactly. These
 * tests assert the corrected, intended behavior.
 */
class BumpUpSeedingServiceTest {

    private CachedScheduleEntryRepository cachedScheduleEntryRepository;
    private CachedRaceEntryRepository cachedRaceEntryRepository;
    private BumpUpSeedingService service;

    @BeforeEach
    void setUp() {
        cachedScheduleEntryRepository = Mockito.mock(CachedScheduleEntryRepository.class);
        cachedRaceEntryRepository = Mockito.mock(CachedRaceEntryRepository.class);
        service = new BumpUpSeedingService(cachedScheduleEntryRepository, cachedRaceEntryRepository);

        Mockito.when(cachedRaceEntryRepository.save(Mockito.any(CachedRaceEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private CachedScheduleEntry finalRace(Long id, String finalLetter) {
        CachedScheduleEntry race = new CachedScheduleEntry();
        race.setId(id);
        race.setClassName("Buggy Stock");
        race.setFinalLetter(finalLetter);
        return race;
    }

    private List<Long> ranksOneToTwenty() {
        List<Long> standings = new ArrayList<>();
        for (long i = 1; i <= 20; i++) {
            standings.add(i);
        }
        return standings;
    }

    // --- Happy path: clean, evenly-divisible-ish standings ---

    @Test
    void seedFinals_assignsBFinalTheBottomTenInPositionalOrder() {
        CachedScheduleEntry a = finalRace(201L, "A");
        CachedScheduleEntry b = finalRace(202L, "B");
        Mockito.when(cachedScheduleEntryRepository.findByClassNameAndFinalLetterIsNotNull("Buggy Stock"))
                .thenReturn(new ArrayList<>(List.of(a, b)));

        service.seedFinals("Buggy Stock", ranksOneToTwenty(), 2, 10, 2);

        ArgumentCaptor<CachedRaceEntry> captor = ArgumentCaptor.forClass(CachedRaceEntry.class);
        Mockito.verify(cachedRaceEntryRepository, Mockito.atLeastOnce()).save(captor.capture());

        List<CachedRaceEntry> bEntries = captor.getAllValues().stream()
                .filter(e -> e.getCachedScheduleId().equals(202L))
                .collect(Collectors.toList());

        // B is the lowest final: 10 regular slots, no bump slots, ranks 11-20 in positional order.
        assertThat(bEntries).hasSize(10);
        bEntries.sort((x, y) -> x.getGridPosition() - y.getGridPosition());
        for (int i = 0; i < 10; i++) {
            assertThat(bEntries.get(i).getGridPosition()).isEqualTo(i + 1);
            assertThat(bEntries.get(i).getCachedEntryId()).isEqualTo(11L + i);
            assertThat(bEntries.get(i).isBumped()).isFalse();
        }
    }

    @Test
    void seedFinals_assignsAFinalRegularSlotsPlusNullBumpSlots() {
        CachedScheduleEntry a = finalRace(201L, "A");
        CachedScheduleEntry b = finalRace(202L, "B");
        Mockito.when(cachedScheduleEntryRepository.findByClassNameAndFinalLetterIsNotNull("Buggy Stock"))
                .thenReturn(new ArrayList<>(List.of(a, b)));

        service.seedFinals("Buggy Stock", ranksOneToTwenty(), 2, 10, 2);

        ArgumentCaptor<CachedRaceEntry> captor = ArgumentCaptor.forClass(CachedRaceEntry.class);
        Mockito.verify(cachedRaceEntryRepository, Mockito.atLeastOnce()).save(captor.capture());

        List<CachedRaceEntry> aEntries = captor.getAllValues().stream()
                .filter(e -> e.getCachedScheduleId().equals(201L))
                .collect(Collectors.toList());

        // A is not the lowest final: carsPerFinal - bumpCount = 8 regular slots + 2 bump slots.
        assertThat(aEntries).hasSize(10);

        List<CachedRaceEntry> regular = aEntries.stream().filter(e -> !e.isBumped()).collect(Collectors.toList());
        List<CachedRaceEntry> bump = aEntries.stream().filter(CachedRaceEntry::isBumped).collect(Collectors.toList());

        assertThat(regular).hasSize(8);
        assertThat(bump).hasSize(2);

        regular.sort((x, y) -> x.getGridPosition() - y.getGridPosition());
        // A's regular slots draw from the TOP of standings — ranks 1-8, the fastest qualifiers.
        for (int i = 0; i < 8; i++) {
            assertThat(regular.get(i).getGridPosition()).isEqualTo(i + 1);
            assertThat(regular.get(i).getCachedEntryId()).isEqualTo(1L + i);
        }

        bump.sort((x, y) -> x.getGridPosition() - y.getGridPosition());
        assertThat(bump.get(0).getGridPosition()).isEqualTo(9);
        assertThat(bump.get(1).getGridPosition()).isEqualTo(10);
        assertThat(bump.get(0).getCachedEntryId()).isNull();
        assertThat(bump.get(1).getCachedEntryId()).isNull();
    }

    @Test
    void seedFinals_deletesExistingRowsForEachFinalBeforeReseeding() {
        CachedScheduleEntry a = finalRace(201L, "A");
        CachedScheduleEntry b = finalRace(202L, "B");
        Mockito.when(cachedScheduleEntryRepository.findByClassNameAndFinalLetterIsNotNull("Buggy Stock"))
                .thenReturn(new ArrayList<>(List.of(a, b)));

        service.seedFinals("Buggy Stock", ranksOneToTwenty(), 2, 10, 2);

        Mockito.verify(cachedRaceEntryRepository).deleteAllByCachedScheduleId(201L);
        Mockito.verify(cachedRaceEntryRepository).deleteAllByCachedScheduleId(202L);
    }

    // --- Edge case: adjacent standings at the A/B slot-count boundary, and the resulting gap ---

    @Test
    void seedFinals_boundaryEntriesGoToDifferentFinalsWithoutDuplication_middleRanksLeftUnassigned() {
        CachedScheduleEntry a = finalRace(201L, "A");
        CachedScheduleEntry b = finalRace(202L, "B");
        Mockito.when(cachedScheduleEntryRepository.findByClassNameAndFinalLetterIsNotNull("Buggy Stock"))
                .thenReturn(new ArrayList<>(List.of(a, b)));

        service.seedFinals("Buggy Stock", ranksOneToTwenty(), 2, 10, 2);

        ArgumentCaptor<CachedRaceEntry> captor = ArgumentCaptor.forClass(CachedRaceEntry.class);
        Mockito.verify(cachedRaceEntryRepository, Mockito.atLeastOnce()).save(captor.capture());

        List<Long> assignedEntryIds = captor.getAllValues().stream()
                .map(CachedRaceEntry::getCachedEntryId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        assertThat(assignedEntryIds).doesNotHaveDuplicates();

        // Rank 8 is A's last top-down regular slot; rank 11 is B's first bottom-up regular slot.
        // They land in different finals, each exactly once.
        CachedRaceEntry rank8 = captor.getAllValues().stream()
                .filter(e -> Long.valueOf(8L).equals(e.getCachedEntryId())).findFirst().orElseThrow();
        CachedRaceEntry rank11 = captor.getAllValues().stream()
                .filter(e -> Long.valueOf(11L).equals(e.getCachedEntryId())).findFirst().orElseThrow();
        assertThat(rank8.getCachedScheduleId()).isEqualTo(201L); // A
        assertThat(rank11.getCachedScheduleId()).isEqualTo(202L); // B

        // Ranks 9 and 10 sit in the gap between A's top-down allocation (1-8) and B's bottom-up
        // allocation (11-20) — with 18 regular slots for a 20-person field, this pair is
        // legitimately left unassigned by this call rather than duplicated into either final.
        assertThat(assignedEntryIds).doesNotContain(9L, 10L);
    }

    // --- Edge case: fewer entrants than configured slots ---

    @Test
    void seedFinals_fewerEntrantsThanCarsPerFinal_doesNotThrowAndLeavesShortfallUnassigned() {
        CachedScheduleEntry a = finalRace(201L, "A");
        CachedScheduleEntry b = finalRace(202L, "B");
        Mockito.when(cachedScheduleEntryRepository.findByClassNameAndFinalLetterIsNotNull("Buggy Stock"))
                .thenReturn(new ArrayList<>(List.of(a, b)));

        List<Long> onlySixStandings = List.of(1L, 2L, 3L, 4L, 5L, 6L);

        assertThatCode(() -> service.seedFinals("Buggy Stock", onlySixStandings, 2, 10, 2))
                .doesNotThrowAnyException();

        ArgumentCaptor<CachedRaceEntry> captor = ArgumentCaptor.forClass(CachedRaceEntry.class);
        Mockito.verify(cachedRaceEntryRepository, Mockito.atLeastOnce()).save(captor.capture());

        List<CachedRaceEntry> bEntries = captor.getAllValues().stream()
                .filter(e -> e.getCachedScheduleId().equals(202L)).collect(Collectors.toList());
        List<CachedRaceEntry> aEntries = captor.getAllValues().stream()
                .filter(e -> e.getCachedScheduleId().equals(201L)).collect(Collectors.toList());

        // B (lowest final) absorbs all 6 available entries rather than the requested 10 — no crash.
        assertThat(bEntries).hasSize(6);
        assertThat(bEntries).allMatch(e -> !e.isBumped());

        // A gets 0 regular slots (standings exhausted) but still gets its 2 bump placeholders,
        // since bump slots are unconditional for a non-lowest final.
        List<CachedRaceEntry> aRegular = aEntries.stream().filter(e -> !e.isBumped()).collect(Collectors.toList());
        List<CachedRaceEntry> aBump = aEntries.stream().filter(CachedRaceEntry::isBumped).collect(Collectors.toList());
        assertThat(aRegular).isEmpty();
        assertThat(aBump).hasSize(2);
        assertThat(aBump).allMatch(e -> e.getCachedEntryId() == null);
    }

    // --- applyBumpUpResults ---

    @Test
    void applyBumpUpResults_fillsBumpSlotsInGridPositionOrder() {
        CachedScheduleEntry finishedB = finalRace(202L, "B");
        Mockito.when(cachedScheduleEntryRepository.findById(202L)).thenReturn(Optional.of(finishedB));

        CachedScheduleEntry aFinal = finalRace(201L, "A");
        Mockito.when(cachedScheduleEntryRepository.findByClassNameAndFinalLetterIsNotNull("Buggy Stock"))
                .thenReturn(new ArrayList<>(List.of(aFinal)));

        CachedRaceEntry bumpSlot1 = new CachedRaceEntry();
        bumpSlot1.setCachedScheduleId(201L);
        bumpSlot1.setGridPosition(9);
        bumpSlot1.setBumped(true);
        CachedRaceEntry bumpSlot2 = new CachedRaceEntry();
        bumpSlot2.setCachedScheduleId(201L);
        bumpSlot2.setGridPosition(10);
        bumpSlot2.setBumped(true);
        CachedRaceEntry regular = new CachedRaceEntry();
        regular.setCachedScheduleId(201L);
        regular.setGridPosition(1);
        regular.setCachedEntryId(3L);
        regular.setBumped(false);

        Mockito.when(cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(201L))
                .thenReturn(new ArrayList<>(List.of(regular, bumpSlot1, bumpSlot2)));

        service.applyBumpUpResults(202L, List.of(111L, 112L));

        assertThat(bumpSlot1.getCachedEntryId()).isEqualTo(111L);
        assertThat(bumpSlot2.getCachedEntryId()).isEqualTo(112L);
        assertThat(regular.getCachedEntryId()).isEqualTo(3L); // untouched
    }

    @Test
    void applyBumpUpResults_fewerTopNThanBumpSlots_fillsOnlyWhatItHasAndLeavesRestUntouched() {
        CachedScheduleEntry finishedB = finalRace(202L, "B");
        Mockito.when(cachedScheduleEntryRepository.findById(202L)).thenReturn(Optional.of(finishedB));

        CachedScheduleEntry aFinal = finalRace(201L, "A");
        Mockito.when(cachedScheduleEntryRepository.findByClassNameAndFinalLetterIsNotNull("Buggy Stock"))
                .thenReturn(new ArrayList<>(List.of(aFinal)));

        CachedRaceEntry bumpSlot1 = new CachedRaceEntry();
        bumpSlot1.setCachedScheduleId(201L);
        bumpSlot1.setGridPosition(9);
        bumpSlot1.setBumped(true);
        CachedRaceEntry bumpSlot2 = new CachedRaceEntry();
        bumpSlot2.setCachedScheduleId(201L);
        bumpSlot2.setGridPosition(10);
        bumpSlot2.setBumped(true);

        Mockito.when(cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(201L))
                .thenReturn(new ArrayList<>(List.of(bumpSlot1, bumpSlot2)));

        assertThatCode(() -> service.applyBumpUpResults(202L, List.of(111L)))
                .doesNotThrowAnyException();

        assertThat(bumpSlot1.getCachedEntryId()).isEqualTo(111L);
        assertThat(bumpSlot2.getCachedEntryId()).isNull(); // untouched — still unfilled
    }

    // --- Error paths ---

    @Test
    void applyBumpUpResults_scheduleRowWithNoFinalLetter_throwsIllegalArgumentException() {
        CachedScheduleEntry notAFinal = new CachedScheduleEntry();
        notAFinal.setId(303L);
        notAFinal.setClassName("Buggy Stock");
        notAFinal.setFinalLetter(null);
        Mockito.when(cachedScheduleEntryRepository.findById(303L)).thenReturn(Optional.of(notAFinal));

        assertThatThrownBy(() -> service.applyBumpUpResults(303L, List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a final");
    }

    @Test
    void applyBumpUpResults_aFinal_hasNoHigherFinal_throwsIllegalArgumentException() {
        CachedScheduleEntry aFinal = finalRace(201L, "A");
        Mockito.when(cachedScheduleEntryRepository.findById(201L)).thenReturn(Optional.of(aFinal));

        assertThatThrownBy(() -> service.applyBumpUpResults(201L, List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No higher final");
    }

    @Test
    void applyBumpUpResults_scheduleRowNotFound_throwsIllegalArgumentException() {
        Mockito.when(cachedScheduleEntryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyBumpUpResults(999L, List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
