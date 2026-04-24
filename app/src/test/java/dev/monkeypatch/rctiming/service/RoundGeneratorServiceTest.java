package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.user.UserClassRatingRepository;
import dev.monkeypatch.rctiming.service.dto.RoundGenerationRequest;
import dev.monkeypatch.rctiming.service.dto.RoundPreviewDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundGeneratorServiceTest {

    // --- Mocks for RoundGeneratorService ---
    @Mock
    private RoundRepository roundRepository;
    @Mock
    private RaceRepository raceRepository;
    @Mock
    private RaceEntryRepository raceEntryRepository;
    @Mock
    private EntryRepository entryRepository;
    @Mock
    private EventClassRepository eventClassRepository;
    @Mock
    private UserClassRatingRepository userClassRatingRepository;
    @Mock
    private BumpUpSeedingService bumpUpSeedingService;

    @InjectMocks
    private RoundGeneratorService service;

    // --- Mocks for BumpUpSeedingService (used in second test directly) ---
    @Mock
    private RaceRepository bumpRaceRepository;
    @Mock
    private RaceEntryRepository bumpRaceEntryRepository;

    @Test
    void heatSplit_fifteenDriversMaxEightPerHeat_createsTwoHeats() {
        // Arrange: one EventClass for event 1
        Long eventId = 1L;
        Long eventClassId = 10L;

        EventClass eventClass = new EventClass();
        eventClass.setId(eventClassId);
        eventClass.setEventId(eventId);
        eventClass.setRacingClassId(null); // no ratings

        when(eventClassRepository.findByEventId(eventId)).thenReturn(List.of(eventClass));

        // 15 entries with IDs 1..15
        List<Entry> entries = new ArrayList<>();
        for (long i = 1; i <= 15; i++) {
            Entry entry = new Entry();
            entry.setId(i);
            entry.setUserId(100L + i);
            entry.setEventId(eventId);
            entry.setEventClassId(eventClassId);
            entries.add(entry);
        }
        when(entryRepository.findByEventClassIdAndStatus(eventClassId, EntryStatus.CONFIRMED))
                .thenReturn(entries);
        when(userClassRatingRepository.findByRacingClassId(any())).thenReturn(List.of());

        // Act: preview with maxCarsPerHeat=8, 0 practice, 1 qualifying
        RoundGenerationRequest request = new RoundGenerationRequest(
                eventId,
                0,          // practiceRoundsCount
                1,          // qualifyingRoundsCount
                8,          // maxCarsPerHeat
                List.of()   // no class finals overrides
        );
        List<RoundPreviewDto> previews = service.preview(request);

        // Assert: exactly 2 heats for the qualifying round
        // (ceil(15 / 8) = 2)
        Set<Integer> heatNumbers = previews.stream()
                .filter(p -> p.finalLetter() == null) // exclude finals
                .map(RoundPreviewDto::heatNumber)
                .collect(Collectors.toSet());
        assertThat(heatNumbers).containsExactlyInAnyOrder(1, 2);

        // Total driver count across heats = 15
        long totalDrivers = previews.stream()
                .filter(p -> p.finalLetter() == null)
                .mapToLong(p -> p.driverNames().size())
                .sum();
        assertThat(totalDrivers).isEqualTo(15);
    }

    @Test
    void bumpUpSeeding_topNofBFinal_appendedToAFinal() {
        // Test BumpUpSeedingService.applyBumpUpResults directly using its own mocks.
        BumpUpSeedingService bumpService = new BumpUpSeedingService(bumpRaceRepository, bumpRaceEntryRepository);

        Long bFinalId = 200L;
        Long aFinalId = 201L;
        Long eventClassId = 10L;

        // B-final race
        Race bFinal = new Race();
        bFinal.setId(bFinalId);
        bFinal.setEventClassId(eventClassId);
        bFinal.setFinalLetter("B");

        // A-final race
        Race aFinal = new Race();
        aFinal.setId(aFinalId);
        aFinal.setEventClassId(eventClassId);
        aFinal.setFinalLetter("A");

        when(bumpRaceRepository.findById(bFinalId)).thenReturn(Optional.of(bFinal));
        when(bumpRaceRepository.findByEventClassIdAndFinalLetter(eventClassId, "A"))
                .thenReturn(List.of(aFinal));

        // A-final has 10 entries: positions 1-8 regular, 9-10 bump slots
        List<RaceEntry> aFinalEntries = new ArrayList<>();
        for (int pos = 1; pos <= 8; pos++) {
            RaceEntry e = new RaceEntry();
            e.setId((long) pos);
            e.setRaceId(aFinalId);
            e.setEntryId((long) (100 + pos));
            e.setGridPosition(pos);
            e.setBumped(false);
            aFinalEntries.add(e);
        }
        // Bump slots (bumped=true, gridPosition 9 and 10)
        RaceEntry bump1 = new RaceEntry();
        bump1.setId(9L);
        bump1.setRaceId(aFinalId);
        bump1.setEntryId(0L);
        bump1.setGridPosition(9);
        bump1.setBumped(true);
        aFinalEntries.add(bump1);

        RaceEntry bump2 = new RaceEntry();
        bump2.setId(10L);
        bump2.setRaceId(aFinalId);
        bump2.setEntryId(0L);
        bump2.setGridPosition(10);
        bump2.setBumped(true);
        aFinalEntries.add(bump2);

        when(bumpRaceEntryRepository.findByRaceIdOrderByGridPosition(aFinalId))
                .thenReturn(aFinalEntries);

        // Act: top 2 finishers from B-final bump up
        bumpService.applyBumpUpResults(bFinalId, List.of(501L, 502L));

        // Verify: save called for both bump slots with correct entryIds
        ArgumentCaptor<RaceEntry> captor = ArgumentCaptor.forClass(RaceEntry.class);
        verify(bumpRaceEntryRepository, times(2)).save(captor.capture());

        List<RaceEntry> saved = captor.getAllValues();
        // Sort by gridPosition to ensure order
        saved.sort((a, b) -> {
            int pa = a.getGridPosition() == null ? Integer.MAX_VALUE : a.getGridPosition();
            int pb = b.getGridPosition() == null ? Integer.MAX_VALUE : b.getGridPosition();
            return pa - pb;
        });

        assertThat(saved.get(0).getEntryId()).isEqualTo(501L);
        assertThat(saved.get(0).isBumped()).isTrue();
        assertThat(saved.get(0).getGridPosition()).isEqualTo(9);

        assertThat(saved.get(1).getEntryId()).isEqualTo(502L);
        assertThat(saved.get(1).isBumped()).isTrue();
        assertThat(saved.get(1).getGridPosition()).isEqualTo(10);
    }
}
