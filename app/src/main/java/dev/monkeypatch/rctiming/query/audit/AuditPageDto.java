package dev.monkeypatch.rctiming.query.audit;

import java.util.List;

/** One page of audit rows, newest first, with how many rows match in all. */
public record AuditPageDto(List<AuditEntryDto> entries, long total, int page, int size) {
}
