package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.forwarder.dto.ForwarderStatusDto;
import dev.monkeypatch.rctiming.timing.LiveTimingHub;
import org.springframework.stereotype.Component;

/**
 * Phase 5 / TIMING-02: tracks gRPC stream lifecycle and broadcasts
 * ForwarderStatusDto to /topic/system/forwarder-status via LiveTimingHub.
 * Frontend status bar subscribes to this topic to show connection pills.
 */
@Component
public class ForwarderStatusPublisher {

    private final LiveTimingHub liveTimingHub;

    public ForwarderStatusPublisher(LiveTimingHub liveTimingHub) {
        this.liveTimingHub = liveTimingHub;
    }

    public void onForwarderConnected() {
        liveTimingHub.broadcastForwarderStatus(
                new ForwarderStatusDto("CONNECTED", "CONNECTED"));
    }

    public void onForwarderDisconnected() {
        liveTimingHub.broadcastForwarderStatus(
                new ForwarderStatusDto("DISCONNECTED", "DISCONNECTED"));
    }

    public void onDecoderReconnecting() {
        liveTimingHub.broadcastForwarderStatus(
                new ForwarderStatusDto("RECONNECTING", "CONNECTED"));
    }
}
