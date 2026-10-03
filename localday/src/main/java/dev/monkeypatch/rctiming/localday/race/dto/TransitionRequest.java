package dev.monkeypatch.rctiming.localday.race.dto;

/** {@code target} is the name of the {@link dev.monkeypatch.rctiming.localday.race.RaceState} to transition to. */
public record TransitionRequest(String target) {
}
