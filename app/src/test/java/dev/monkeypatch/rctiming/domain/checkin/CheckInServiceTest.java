package dev.monkeypatch.rctiming.domain.checkin;

import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Ported from localday's CheckInServiceTest (L11). */
class CheckInServiceTest {

    private EntryRepository entryRepository;
    private CheckInService service;

    @BeforeEach
    void setUp() {
        entryRepository = Mockito.mock(EntryRepository.class);
        service = new CheckInService(entryRepository);
        Mockito.when(entryRepository.save(any(Entry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Entry entry(long id, long eventId, EntryStatus status) {
        Entry e = new Entry();
        e.setId(id);
        e.setEventId(eventId);
        e.setStatus(status);
        e.setTransponderNumberSnapshot("1234567");
        Mockito.when(entryRepository.findByIdForUpdate(id)).thenReturn(Optional.of(e));
        return e;
    }

    @Test
    void confirm_firstTime_setsCheckedInAtAndOfficial() {
        Entry e = entry(5L, 1L, EntryStatus.CONFIRMED);

        CheckInResult result = service.confirm(1L, 5L, 42L);

        assertThat(result).isInstanceOf(CheckInResult.Success.class);
        assertThat(((CheckInResult.Success) result).alreadyCheckedIn()).isFalse();
        assertThat(e.getCheckedInAt()).isNotNull();
        assertThat(e.getCheckedInByUserId()).isEqualTo(42L);
        verify(entryRepository).save(e);
    }

    @Test
    void confirm_repeat_keepsTheFirstCheckInTime() {
        Entry e = entry(5L, 1L, EntryStatus.CONFIRMED);
        Instant first = Instant.parse("2026-10-04T08:00:00Z");
        e.setCheckedInAt(first);
        e.setCheckedInByUserId(42L);

        CheckInResult result = service.confirm(1L, 5L, 43L);

        assertThat(((CheckInResult.Success) result).alreadyCheckedIn()).isTrue();
        assertThat(e.getCheckedInAt()).isEqualTo(first);
        assertThat(e.getCheckedInByUserId()).isEqualTo(42L);
        verify(entryRepository, never()).save(any());
    }

    @Test
    void confirm_doesNotTouchTheRaceHubArrivalMark() {
        Entry e = entry(5L, 1L, EntryStatus.CONFIRMED);
        e.setRacehubArrival("NOT_ARRIVED");

        service.confirm(1L, 5L, 42L);

        assertThat(e.getRacehubArrival()).isEqualTo("NOT_ARRIVED");
    }

    @Test
    void confirm_unknownEntry_isNotFound() {
        Mockito.when(entryRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());

        assertThat(service.confirm(1L, 9L, 42L)).isInstanceOf(CheckInResult.NotFound.class);
    }

    @Test
    void confirm_entryInAnotherEvent_isNotFound() {
        entry(5L, 2L, EntryStatus.CONFIRMED);

        assertThat(service.confirm(1L, 5L, 42L)).isInstanceOf(CheckInResult.NotFound.class);
    }

    @Test
    void confirm_withdrawnEntry_isRefused() {
        Entry e = entry(5L, 1L, EntryStatus.WITHDRAWN);

        assertThat(service.confirm(1L, 5L, 42L)).isInstanceOf(CheckInResult.Withdrawn.class);
        assertThat(e.getCheckedInAt()).isNull();
    }
}
