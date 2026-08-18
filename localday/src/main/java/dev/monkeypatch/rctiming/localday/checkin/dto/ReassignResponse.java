package dev.monkeypatch.rctiming.localday.checkin.dto;

public record ReassignResponse(Long cachedEntryId, String oldTransponderNumber, String newTransponderNumber) {
}
