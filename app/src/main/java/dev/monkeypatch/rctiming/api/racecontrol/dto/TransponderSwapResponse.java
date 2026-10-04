package dev.monkeypatch.rctiming.api.racecontrol.dto;

import dev.monkeypatch.rctiming.domain.checkin.TransponderSlot;

/** A saved transponder swap. {@code newTransponderNumber} is null when a secondary was removed. */
public record TransponderSwapResponse(
        long entryId,
        TransponderSlot slot,
        String oldTransponderNumber,
        String newTransponderNumber
) {}
