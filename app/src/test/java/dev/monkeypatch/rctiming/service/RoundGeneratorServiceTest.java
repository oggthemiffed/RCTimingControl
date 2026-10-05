package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.service.dto.RoundGenerationRequest;
import dev.monkeypatch.rctiming.service.dto.RoundPreviewDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
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
    private BumpUpSeedingService bumpUpSeedingService;

    @InjectMocks
    private RoundGeneratorService service;

    @Test
    void heatSplit_fifteenDriversMaxEightPerHeat_createsTwoHeats() {
        // Arrange: one EventClass for event 1
        Long eventId = 1L;
        Long eventClassId = 10L;

        EventClass eventClass = new EventClass();
        eventClass.setId(eventClassId);
        eventClass.setEventId(eventId);
        eventClass.setRacingClassId(null);

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
}
