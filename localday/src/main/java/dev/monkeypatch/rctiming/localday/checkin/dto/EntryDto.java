package dev.monkeypatch.rctiming.localday.checkin.dto;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;

import java.time.Instant;

/**
 * Shared response shape for {@code /resolve} and {@code /search} — a pre-entered racer's roster
 * details plus current check-in state.
 */
public record EntryDto(Long cachedEntryId, String racerName, String carName, String className,
                        String transponderNumber, boolean checkedIn, Instant checkedInAt) {

    public static EntryDto from(CachedEntry entry) {
        return new EntryDto(entry.getId(), entry.getRacerName(), entry.getCarName(),
                entry.getClassName(), entry.getTransponderNumber(), entry.isCheckedIn(),
                entry.getCheckedInAt());
    }
}
