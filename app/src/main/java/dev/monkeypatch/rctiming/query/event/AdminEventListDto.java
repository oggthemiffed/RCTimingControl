package dev.monkeypatch.rctiming.query.event;

import dev.monkeypatch.rctiming.domain.event.EventStatus;

import java.time.LocalDate;

public record AdminEventListDto(
        Long id,
        String name,
        LocalDate eventDate,
        EventStatus status,
        String trackName       // nullable — LEFT JOIN on tracks
) {}
