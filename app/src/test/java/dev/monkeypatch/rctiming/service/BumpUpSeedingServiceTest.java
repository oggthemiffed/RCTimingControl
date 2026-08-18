package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BumpUpSeedingService#seedFinals} previously had no dedicated test coverage — this
 * class exercises it directly against the algorithm's own documented worked example (20
 * qualifiers, 2 finals A+B, 10 cars/final, bumpCount=2), which the original implementation did
 * not actually satisfy: it silently dropped the top 2 qualifiers from any regular slot instead
 * of seeding them into the A-Final. The fix makes non-lowest finals draw from the top of
 * standings (processed best-final-first) while the lowest final still draws from the bottom.
 */
@ExtendWith(MockitoExtension.class)
class BumpUpSeedingServiceTest {

    @Mock
    private RaceRepository raceRepository;
    @Mock
    private RaceEntryRepository raceEntryRepository;

    private BumpUpSeedingService service() {
        return new BumpUpSeedingService(raceRepository, raceEntryRepository);
    }

    @Test
    void seedFinals_twoFinals_topQualifiersSeedIntoAFinal_bottomQualifiersSeedIntoBFinal() {
        Long eventClassId = 10L;
        Race bFinal = new Race();
        bFinal.setId(200L);
        bFinal.setEventClassId(eventClassId);
        bFinal.setFinalLetter("B");
        Race aFinal = new Race();
        aFinal.setId(201L);
        aFinal.setEventClassId(eventClassId);
        aFinal.setFinalLetter("A");

        when(raceRepository.findByEventClassIdAndRoundType(eventClassId, RoundType.FINAL))
                .thenReturn(new ArrayList<>(List.of(bFinal, aFinal)));
        when(raceEntryRepository.findByRaceIdOrderByGridPosition(bFinal.getId())).thenReturn(List.of());
        when(raceEntryRepository.findByRaceIdOrderByGridPosition(aFinal.getId())).thenReturn(List.of());

        // Ranks 1..20 (index 0 = rank 1, best).
        List<Long> standings = new ArrayList<>();
        for (long i = 1; i <= 20; i++) standings.add(i);

        service().seedFinals(eventClassId, standings, 2, 10, 2);

        ArgumentCaptor<RaceEntry> captor = ArgumentCaptor.forClass(RaceEntry.class);
        verify(raceEntryRepository, times(20)).save(captor.capture());
        List<RaceEntry> saved = captor.getAllValues();

        List<RaceEntry> bEntries = saved.stream().filter(e -> e.getRaceId().equals(bFinal.getId()))
                .sorted(Comparator.comparingInt(RaceEntry::getGridPosition)).toList();
        List<RaceEntry> aEntries = saved.stream().filter(e -> e.getRaceId().equals(aFinal.getId()))
                .sorted(Comparator.comparingInt(RaceEntry::getGridPosition)).toList();

        // B-Final: 10 regular slots, no bump reservation (lowest final) — ranks 11..20.
        assertThat(bEntries).hasSize(10);
        assertThat(bEntries).extracting(RaceEntry::getEntryId)
                .containsExactly(11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L);
        assertThat(bEntries).allMatch(e -> !e.isBumped());

        // A-Final: 8 regular slots (the TOP qualifiers, ranks 1..8 — this is the fix) plus
        // 2 bumped=true placeholder slots with entryId=0 (unfilled until applyBumpUpResults).
        assertThat(aEntries).hasSize(10);
        List<RaceEntry> aRegular = aEntries.stream().filter(e -> !e.isBumped()).toList();
        List<RaceEntry> aBump = aEntries.stream().filter(RaceEntry::isBumped).toList();
        assertThat(aRegular).extracting(RaceEntry::getEntryId)
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
        assertThat(aBump).hasSize(2);
        assertThat(aBump).allMatch(e -> e.getEntryId() == 0L);
    }

    @Test
    void seedFinals_singleFinal_allRegularSlotsFromTopOfStandings() {
        Long eventClassId = 20L;
        Race aFinal = new Race();
        aFinal.setId(300L);
        aFinal.setEventClassId(eventClassId);
        aFinal.setFinalLetter("A");

        when(raceRepository.findByEventClassIdAndRoundType(eventClassId, RoundType.FINAL))
                .thenReturn(new ArrayList<>(List.of(aFinal)));
        when(raceEntryRepository.findByRaceIdOrderByGridPosition(aFinal.getId())).thenReturn(List.of());

        List<Long> standings = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

        service().seedFinals(eventClassId, standings, 1, 10, 0);

        ArgumentCaptor<RaceEntry> captor = ArgumentCaptor.forClass(RaceEntry.class);
        verify(raceEntryRepository, times(10)).save(captor.capture());
        List<RaceEntry> saved = captor.getAllValues().stream()
                .sorted(Comparator.comparingInt(RaceEntry::getGridPosition)).toList();

        assertThat(saved).extracting(RaceEntry::getEntryId)
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        assertThat(saved).allMatch(e -> !e.isBumped());
    }

    @Test
    void seedFinals_fewerQualifiersThanTotalCapacity_doesNotThrow_leavesShortfallUnassigned() {
        Long eventClassId = 30L;
        Race bFinal = new Race();
        bFinal.setId(400L);
        bFinal.setEventClassId(eventClassId);
        bFinal.setFinalLetter("B");
        Race aFinal = new Race();
        aFinal.setId(401L);
        aFinal.setEventClassId(eventClassId);
        aFinal.setFinalLetter("A");

        when(raceRepository.findByEventClassIdAndRoundType(eventClassId, RoundType.FINAL))
                .thenReturn(new ArrayList<>(List.of(bFinal, aFinal)));
        when(raceEntryRepository.findByRaceIdOrderByGridPosition(bFinal.getId())).thenReturn(List.of());
        when(raceEntryRepository.findByRaceIdOrderByGridPosition(aFinal.getId())).thenReturn(List.of());

        // Only 5 qualifiers total, far fewer than the 18 regular + 2 bump capacity.
        List<Long> standings = List.of(1L, 2L, 3L, 4L, 5L);

        service().seedFinals(eventClassId, standings, 2, 10, 2);

        ArgumentCaptor<RaceEntry> captor = ArgumentCaptor.forClass(RaceEntry.class);
        verify(raceEntryRepository, times(4)).save(captor.capture());
        List<RaceEntry> saved = captor.getAllValues();

        List<RaceEntry> bEntries = saved.stream().filter(e -> e.getRaceId().equals(bFinal.getId())).toList();
        List<RaceEntry> aRegular = saved.stream()
                .filter(e -> e.getRaceId().equals(aFinal.getId()) && !e.isBumped()).toList();

        // Lowest final (B) draws from the bottom: with only 5 standings and 10 lowest-final
        // slots, all 5 land in B.
        assertThat(bEntries).hasSize(5);
        assertThat(bEntries).extracting(RaceEntry::getEntryId)
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);
        // Top-down pass for A starts after the bottom pass has already consumed everything
        // (both pointers walk the same list), so no regular slots remain for A here.
        assertThat(aRegular).isEmpty();
    }
}
