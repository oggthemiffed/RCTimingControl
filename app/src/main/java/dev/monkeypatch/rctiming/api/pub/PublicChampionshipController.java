package dev.monkeypatch.rctiming.api.pub;

import dev.monkeypatch.rctiming.query.championship.ChampionshipStandingsQuery;
import dev.monkeypatch.rctiming.query.championship.StandingsRowDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public (no-auth) endpoint for championship standings.
 * CHAMP-05.
 *
 * No @PreAuthorize — permitted via SecurityConfig.requestMatchers().permitAll().
 * EntityNotFoundException from ChampionshipStandingsQuery.computeStandings() is mapped to 404
 * by GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/championships")
public class PublicChampionshipController {

    private final ChampionshipStandingsQuery query;

    public PublicChampionshipController(ChampionshipStandingsQuery query) {
        this.query = query;
    }

    @GetMapping("/{id}")
    public List<StandingsRowDto> getStandings(@PathVariable Long id) {
        return query.computeStandings(id);
    }
}
