package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.forwarder.ForwarderStatusPublisher;
import dev.monkeypatch.rctiming.forwarder.dto.ForwarderStatusDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 5: returns current forwarder/decoder connection state so the frontend
 * status bar can initialise correctly on page load rather than waiting for the
 * next STOMP push (which only arrives on state change).
 */
@RestController
@RequestMapping("/api/v1/race-control/forwarder/status")
public class ForwarderStatusController {

    private final ForwarderStatusPublisher statusPublisher;

    public ForwarderStatusController(ForwarderStatusPublisher statusPublisher) {
        this.statusPublisher = statusPublisher;
    }

    @GetMapping
    public ForwarderStatusDto getStatus() {
        return statusPublisher.getLastKnownStatus();
    }
}
