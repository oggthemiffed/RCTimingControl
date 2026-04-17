package dev.monkeypatch.rctiming.api.racer.dto;

import dev.monkeypatch.rctiming.domain.transponder.Transponder;

public record TransponderDto(Long id, String transponderNumber, String label) {

    public static TransponderDto from(Transponder t) {
        return new TransponderDto(t.getId(), t.getTransponderNumber(), t.getLabel());
    }
}
