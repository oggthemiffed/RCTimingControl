package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.api.racecontrol.dto.RunOrderItemDto;
import dev.monkeypatch.rctiming.query.racecontrol.RunOrderQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Race control REST API (CTRL-01, CTRL-03, CTRL-06, CTRL-08, CTRL-09, D-04).
 * All endpoints require RACE_DIRECTOR or ADMIN role.
 * POST endpoints are added in Task 3.
 */
@RestController
@RequestMapping("/api/v1/race-control")
@PreAuthorize("hasAnyRole('RACE_DIRECTOR','ADMIN')")
public class RaceControlController {

    private final RunOrderQuery runOrderQuery;

    public RaceControlController(RunOrderQuery runOrderQuery) {
        this.runOrderQuery = runOrderQuery;
    }

    @GetMapping("/event/{eventId}/run-order")
    public List<RunOrderItemDto> getRunOrder(@PathVariable long eventId) {
        return runOrderQuery.findForEvent(eventId);
    }
}
