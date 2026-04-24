package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.service.SyntheticTimingService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only endpoint for firing synthetic lap passings (D-07).
 * Gated to the "dev" Spring profile — bean is not registered in production.
 * In production, POST /api/v1/dev/race/{id}/synthetic-passing returns 404 (bean not registered).
 */
@RestController
@RequestMapping("/api/v1/dev")
@Profile("dev")
@PreAuthorize("hasAnyRole('RACE_DIRECTOR','ADMIN')")
public class DevSyntheticTimingController {

    private final SyntheticTimingService syntheticTimingService;

    public DevSyntheticTimingController(SyntheticTimingService syntheticTimingService) {
        this.syntheticTimingService = syntheticTimingService;
    }

    @PostMapping("/race/{raceId}/synthetic-passing")
    public ResponseEntity<Void> fireSyntheticPassing(@PathVariable long raceId) {
        syntheticTimingService.firePassing(raceId);
        return ResponseEntity.accepted().build();
    }
}
