package dev.monkeypatch.rctiming.api.racecontrol.dto;

/** Result of a check-in. {@code alreadyCheckedIn} is true when this was a repeat. */
public record CheckInConfirmResponse(CheckInEntryDto entry, boolean alreadyCheckedIn) {}
