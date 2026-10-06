package dev.monkeypatch.rctiming.query.competitor;

import java.util.List;

/** Competitors that may be the same person, and why they look alike (#123). */
public record CompetitorDuplicateGroupDto(String reason, List<CompetitorSummaryDto> competitors) {
}
