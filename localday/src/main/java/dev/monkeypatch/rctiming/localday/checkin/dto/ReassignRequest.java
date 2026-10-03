package dev.monkeypatch.rctiming.localday.checkin.dto;

public record ReassignRequest(Long cachedEntryId, String newTransponderNumber) {
}
