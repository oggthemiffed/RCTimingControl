package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.query.competitor.CompetitorQueryService;
import dev.monkeypatch.rctiming.query.competitor.CompetitorSummaryDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/competitors")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class AdminCompetitorController {

    private final CompetitorQueryService competitorQueryService;

    public AdminCompetitorController(CompetitorQueryService competitorQueryService) {
        this.competitorQueryService = competitorQueryService;
    }

    @GetMapping
    public List<CompetitorSummaryDto> listCompetitors() {
        return competitorQueryService.listAll();
    }
}
