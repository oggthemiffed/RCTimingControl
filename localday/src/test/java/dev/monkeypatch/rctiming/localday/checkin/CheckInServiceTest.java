package dev.monkeypatch.rctiming.localday.checkin;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Pure unit tests for {@link CheckInService}'s resolve/search/confirm logic. Mirrors the style of
 * {@code LocalSessionServiceTest} — plain JUnit 5 + AssertJ, no Spring context.
 * {@link CachedEntryRepository} is mocked.
 */
class CheckInServiceTest {

    private CachedEntryRepository cachedEntryRepository;
    private CheckInService service;

    @BeforeEach
    void setUp() {
        cachedEntryRepository = Mockito.mock(CachedEntryRepository.class);
        service = new CheckInService(cachedEntryRepository);

        Mockito.when(cachedEntryRepository.save(any(CachedEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CachedEntry entry(Long id, String transponderNumber, String racerName) {
        CachedEntry e = new CachedEntry();
        e.setId(id);
        e.setCloudEntryId(id);
        e.setTransponderNumber(transponderNumber);
        e.setRacerName(racerName);
        e.setCarName("TC-01");
        e.setClassName("Touring Stock");
        return e;
    }

    // --- Happy path ---

    @Test
    void resolveByTransponderNumber_findsExistingEntry() {
        CachedEntry cached = entry(5L, "1234567", "Jane Doe");
        Mockito.when(cachedEntryRepository.findByTransponderNumber("1234567")).thenReturn(Optional.of(cached));

        Optional<CachedEntry> result = service.resolveByTransponderNumber("1234567");

        assertThat(result).isPresent();
        assertThat(result.get().getRacerName()).isEqualTo("Jane Doe");
    }

    @Test
    void confirm_notYetCheckedIn_setsCheckedInAndFreshTimestamp_reportsNotAlreadyCheckedIn() {
        CachedEntry cached = entry(5L, "1234567", "Jane Doe");
        cached.setCheckedIn(false);
        cached.setCheckedInAt(null);
        Mockito.when(cachedEntryRepository.findById(5L)).thenReturn(Optional.of(cached));

        Instant before = Instant.now();
        CheckInResult result = service.confirm(5L);
        Instant after = Instant.now();

        assertThat(result).isInstanceOf(CheckInResult.Success.class);
        CheckInResult.Success success = (CheckInResult.Success) result;
        assertThat(success.alreadyCheckedIn()).isFalse();
        assertThat(success.entry().isCheckedIn()).isTrue();
        assertThat(success.entry().getCheckedInAt()).isNotNull()
                .isBetween(before, after);

        Mockito.verify(cachedEntryRepository).save(cached);
    }

    // --- Edge case: repeat confirm ---

    @Test
    void confirm_alreadyCheckedIn_doesNotOverwriteOriginalTimestamp_reportsAlreadyCheckedIn() {
        Instant originalCheckInTime = Instant.parse("2026-08-18T10:00:00Z");
        CachedEntry cached = entry(5L, "1234567", "Jane Doe");
        cached.setCheckedIn(true);
        cached.setCheckedInAt(originalCheckInTime);
        Mockito.when(cachedEntryRepository.findById(5L)).thenReturn(Optional.of(cached));

        CheckInResult result = service.confirm(5L);

        assertThat(result).isInstanceOf(CheckInResult.Success.class);
        CheckInResult.Success success = (CheckInResult.Success) result;
        assertThat(success.alreadyCheckedIn()).isTrue();
        assertThat(success.entry().isCheckedIn()).isTrue();
        assertThat(success.entry().getCheckedInAt()).isEqualTo(originalCheckInTime);

        Mockito.verify(cachedEntryRepository, Mockito.never()).save(any());
    }

    // --- Error path ---

    @Test
    void confirm_nonexistentEntry_returnsNotFound() {
        Mockito.when(cachedEntryRepository.findById(999L)).thenReturn(Optional.empty());

        CheckInResult result = service.confirm(999L);

        assertThat(result).isInstanceOf(CheckInResult.NotFound.class);
        Mockito.verify(cachedEntryRepository, Mockito.never()).save(any());
    }

    // --- Edge case: search ---

    @Test
    void searchByName_blankQuery_returnsEmptyListWithoutQueryingRepository() {
        List<CachedEntry> resultBlank = service.searchByName("   ");
        List<CachedEntry> resultEmpty = service.searchByName("");
        List<CachedEntry> resultNull = service.searchByName(null);

        assertThat(resultBlank).isEmpty();
        assertThat(resultEmpty).isEmpty();
        assertThat(resultNull).isEmpty();
        Mockito.verifyNoInteractions(cachedEntryRepository);
    }

    @Test
    void searchByName_realQuery_isCaseInsensitiveSubstringMatch() {
        CachedEntry cached = entry(5L, "1234567", "Jane Doe");
        Mockito.when(cachedEntryRepository.findByRacerNameContainingIgnoreCase("doe"))
                .thenReturn(List.of(cached));

        List<CachedEntry> result = service.searchByName("doe");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRacerName()).isEqualTo("Jane Doe");
        Mockito.verify(cachedEntryRepository).findByRacerNameContainingIgnoreCase("doe");
    }
}
