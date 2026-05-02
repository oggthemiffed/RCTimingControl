package dev.monkeypatch.rctiming.api.racer;

import dev.monkeypatch.rctiming.query.results.RacerResultHistoryDto;
import dev.monkeypatch.rctiming.query.results.RacerResultHistoryQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Authenticated endpoint for a racer's own result history.
 * RESULT-03.
 *
 * IDOR safety (T-07-04-02): userId is extracted exclusively from the JWT principal
 * via Authentication.getName() — there is no @RequestParam userId that a caller could supply.
 */
@RestController
@RequestMapping("/api/v1/racer")
@PreAuthorize("hasRole('RACER')")
public class RacerResultsController {

    private final RacerResultHistoryQuery query;

    public RacerResultsController(RacerResultHistoryQuery query) {
        this.query = query;
    }

    @GetMapping("/results")
    public List<RacerResultHistoryDto> getMyResults(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());  // JWT principal — IDOR safety: never from request param
        return query.findForUser(userId);
    }
}
