package dev.monkeypatch.rctiming.localday.checkin.dto;

import java.time.Instant;

public record ConfirmResponse(Long cachedEntryId, String racerName, boolean checkedIn,
                               Instant checkedInAt, boolean alreadyCheckedIn) {
}
