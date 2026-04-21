package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.AddChampionshipClassRequest;
import dev.monkeypatch.rctiming.api.admin.dto.AddChampionshipEventRequest;
import dev.monkeypatch.rctiming.api.admin.dto.ChampionshipClassDto;
import dev.monkeypatch.rctiming.api.admin.dto.ChampionshipDetailDto;
import dev.monkeypatch.rctiming.api.admin.dto.ChampionshipDto;
import dev.monkeypatch.rctiming.api.admin.dto.ChampionshipEventLinkDto;
import dev.monkeypatch.rctiming.api.admin.dto.ChampionshipExclusionDto;
import dev.monkeypatch.rctiming.api.admin.dto.CreateChampionshipRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateExclusionRequest;
import dev.monkeypatch.rctiming.api.admin.dto.PointsScaleEntryDto;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateChampionshipRequest;
import dev.monkeypatch.rctiming.api.admin.dto.UpdatePointsScaleRequest;
import dev.monkeypatch.rctiming.domain.championship.ChampionshipService;
import dev.monkeypatch.rctiming.query.championship.ChampionshipStandingsQuery;
import dev.monkeypatch.rctiming.query.championship.StandingsRowDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/championships")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class ChampionshipController {

    private final ChampionshipService championshipService;
    private final ChampionshipStandingsQuery standingsQuery;

    public ChampionshipController(ChampionshipService championshipService,
                                   ChampionshipStandingsQuery standingsQuery) {
        this.championshipService = championshipService;
        this.standingsQuery = standingsQuery;
    }

    @GetMapping
    public List<ChampionshipDto> list() {
        return championshipService.listAll();
    }

    @GetMapping("/{id}")
    public ChampionshipDetailDto getDetail(@PathVariable Long id) {
        return championshipService.getDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChampionshipDto create(@RequestBody @Valid CreateChampionshipRequest request) {
        return championshipService.create(request);
    }

    @PutMapping("/{id}")
    public ChampionshipDto update(@PathVariable Long id,
                                   @RequestBody @Valid UpdateChampionshipRequest request) {
        return championshipService.update(id, request);
    }

    @PostMapping("/{id}/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public ChampionshipClassDto addClass(@PathVariable Long id,
                                          @RequestBody @Valid AddChampionshipClassRequest request) {
        return championshipService.addClass(id, request);
    }

    @DeleteMapping("/{id}/classes/{racingClassId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeClass(@PathVariable Long id, @PathVariable Long racingClassId) {
        championshipService.removeClass(id, racingClassId);
    }

    @PostMapping("/{id}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public ChampionshipEventLinkDto linkEvent(@PathVariable Long id,
                                               @RequestBody @Valid AddChampionshipEventRequest request) {
        return championshipService.linkEvent(id, request);
    }

    @DeleteMapping("/{id}/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkEvent(@PathVariable Long id, @PathVariable Long eventId) {
        championshipService.unlinkEvent(id, eventId);
    }

    @PutMapping("/{id}/points-scale")
    public List<PointsScaleEntryDto> replacePointsScale(@PathVariable Long id,
                                                         @RequestBody @Valid UpdatePointsScaleRequest request) {
        return championshipService.replacePointsScale(id, request);
    }

    @GetMapping("/{id}/exclusions")
    public List<ChampionshipExclusionDto> listExclusions(@PathVariable Long id) {
        return championshipService.listExclusions(id);
    }

    @PostMapping("/{id}/exclusions")
    @ResponseStatus(HttpStatus.CREATED)
    public ChampionshipExclusionDto createExclusion(@PathVariable Long id,
                                                     Authentication auth,
                                                     @RequestBody @Valid CreateExclusionRequest request) {
        Long actingAdminId = Long.parseLong(auth.getName());
        return championshipService.createExclusion(id, actingAdminId, request);
    }

    @DeleteMapping("/{id}/exclusions/{exclusionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExclusion(@PathVariable Long id, @PathVariable Long exclusionId) {
        championshipService.deleteExclusion(id, exclusionId);
    }

    /** Phase 3 returns a scaffold (empty rows). Phase 7 implements race_results aggregation. */
    @GetMapping("/{id}/standings")
    public List<StandingsRowDto> getStandings(@PathVariable Long id) {
        return standingsQuery.computeStandings(id);
    }
}
