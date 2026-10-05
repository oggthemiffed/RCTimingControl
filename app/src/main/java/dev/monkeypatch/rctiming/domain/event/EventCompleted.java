package dev.monkeypatch.rctiming.domain.event;

/** Published when an event is marked COMPLETED: the race day is closed. */
public record EventCompleted(long eventId) {
}
