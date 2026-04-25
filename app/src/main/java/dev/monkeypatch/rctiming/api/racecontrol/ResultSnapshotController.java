package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;
import dev.monkeypatch.rctiming.query.racecontrol.ResultSnapshotQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for result snapshot retrieval (CTRL-04).
 * Any authenticated user may view results — results are non-sensitive venue data.
 */
@RestController
@RequestMapping("/api/v1/race-control")
public class ResultSnapshotController {

    private final ResultSnapshotQuery query;

    public ResultSnapshotController(ResultSnapshotQuery query) {
        this.query = query;
    }

    @GetMapping("/race/{raceId}/result-snapshot")
    @PreAuthorize("isAuthenticated()")
    public ResultSnapshotDto get(@PathVariable long raceId) {
        return query.load(raceId);
    }
}
