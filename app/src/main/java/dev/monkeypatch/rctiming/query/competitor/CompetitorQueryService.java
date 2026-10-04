package dev.monkeypatch.rctiming.query.competitor;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Competitors.COMPETITORS;

@Service
@Transactional(readOnly = true)
public class CompetitorQueryService {

    private final DSLContext dsl;

    public CompetitorQueryService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<CompetitorSummaryDto> listAll() {
        return dsl.select(COMPETITORS.ID, COMPETITORS.DISPLAY_NAME, COMPETITORS.BRCA_NUMBER, COMPETITORS.HOME_CLUB)
                .from(COMPETITORS)
                .orderBy(COMPETITORS.DISPLAY_NAME.asc(), COMPETITORS.ID.asc())
                .fetch(r -> new CompetitorSummaryDto(
                        r.get(COMPETITORS.ID),
                        r.get(COMPETITORS.DISPLAY_NAME),
                        r.get(COMPETITORS.BRCA_NUMBER),
                        r.get(COMPETITORS.HOME_CLUB)));
    }
}
