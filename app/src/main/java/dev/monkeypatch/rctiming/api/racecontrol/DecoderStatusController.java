package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.timing.DecoderStatusPublisher;
import dev.monkeypatch.rctiming.timing.dto.DecoderStatusDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns the current decoder connection state, so the race-control status bar can show the
 * right state on page load rather than waiting for the next STOMP push.
 */
@RestController
@RequestMapping("/api/v1/race-control/decoder/status")
public class DecoderStatusController {

    private final DecoderStatusPublisher statusPublisher;

    public DecoderStatusController(DecoderStatusPublisher statusPublisher) {
        this.statusPublisher = statusPublisher;
    }

    @GetMapping
    public DecoderStatusDto getStatus() {
        return statusPublisher.getLastKnownStatus();
    }
}
