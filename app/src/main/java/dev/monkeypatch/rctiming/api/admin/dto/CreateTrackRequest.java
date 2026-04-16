package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTrackRequest(
        @NotBlank @Size(max = 255) String name,
        String venueNotes,
        Double trackLength) {
}
