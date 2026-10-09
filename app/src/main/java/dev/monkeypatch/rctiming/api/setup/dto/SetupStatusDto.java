package dev.monkeypatch.rctiming.api.setup.dto;

import dev.monkeypatch.rctiming.service.SetupService;

public record SetupStatusDto(boolean bootstrapped, boolean setupComplete) {

    public static SetupStatusDto from(SetupService.Status s) {
        return new SetupStatusDto(s.bootstrapped(), s.setupComplete());
    }
}
