package dev.monkeypatch.rctiming.query.competitor;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.monkeypatch.rctiming.domain.competitor.CompetitorRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Competitors.COMPETITORS;

@Service
@Transactional(readOnly = true)
public class CompetitorQueryService {

    private final DSLContext dsl;

    public CompetitorQueryService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<CompetitorSummaryDto> listAll() {
        return dsl.select(COMPETITORS.ID, COMPETITORS.DISPLAY_NAME, COMPETITORS.BRCA_NUMBER, COMPETITORS.HOME_CLUB,
                        COMPETITORS.SPOKEN_NAME)
                .from(COMPETITORS)
                .orderBy(COMPETITORS.DISPLAY_NAME.asc(), COMPETITORS.ID.asc())
                .fetch(r -> new CompetitorSummaryDto(
                        r.get(COMPETITORS.ID),
                        r.get(COMPETITORS.DISPLAY_NAME),
                        r.get(COMPETITORS.BRCA_NUMBER),
                        r.get(COMPETITORS.HOME_CLUB),
                        r.get(COMPETITORS.SPOKEN_NAME)));
    }

    /**
     * Competitors that may be one person entered twice: the same name (ignoring case and spacing) or
     * the same BRCA number. A pair that shares both is listed once (#123).
     */
    public List<CompetitorDuplicateGroupDto> listPossibleDuplicates() {
        List<CompetitorSummaryDto> all = listAll();
        Map<String, List<CompetitorSummaryDto>> byName = all.stream()
                .collect(Collectors.groupingBy(c -> CompetitorRepository.normalizeName(c.displayName()),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, List<CompetitorSummaryDto>> byBrca = all.stream()
                .filter(c -> c.brcaNumber() != null && !c.brcaNumber().isBlank())
                .collect(Collectors.groupingBy(c -> c.brcaNumber().trim(), LinkedHashMap::new, Collectors.toList()));

        List<CompetitorDuplicateGroupDto> groups = new ArrayList<>();
        Set<Set<Long>> sharedBoth = new HashSet<>();
        for (List<CompetitorSummaryDto> sameName : byName.values()) {
            if (sameName.size() < 2) continue;
            Set<Long> ids = sameName.stream().map(CompetitorSummaryDto::id).collect(Collectors.toSet());
            boolean sameBrcaToo = byBrca.values().stream()
                    .anyMatch(g -> g.size() >= 2 && g.stream().map(CompetitorSummaryDto::id).collect(Collectors.toSet()).equals(ids));
            if (sameBrcaToo) sharedBoth.add(ids);
            groups.add(new CompetitorDuplicateGroupDto(
                    sameBrcaToo ? "Same name and BRCA number" : "Same name", sameName));
        }
        for (List<CompetitorSummaryDto> sameBrca : byBrca.values()) {
            if (sameBrca.size() < 2) continue;
            if (sharedBoth.contains(sameBrca.stream().map(CompetitorSummaryDto::id).collect(Collectors.toSet()))) continue;
            groups.add(new CompetitorDuplicateGroupDto("Same BRCA number", sameBrca));
        }
        return groups;
    }
}
