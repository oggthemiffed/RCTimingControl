package dev.monkeypatch.rctiming.localday.race.dto;

import java.util.List;

public record AdvanceRoundRequest(Long nextScheduleId, List<Long> entryIdsInFinishingOrder) {
}
