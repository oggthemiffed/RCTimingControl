package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.forwarder.dto.ForwarderStatusDto;
import dev.monkeypatch.rctiming.timing.LiveTimingHub;
import org.springframework.stereotype.Component;

/**
 * Phase 5 / TIMING-02: tracks gRPC stream lifecycle and broadcasts
 * ForwarderStatusDto to /topic/system/forwarder-status via LiveTimingHub.
 * Frontend status bar subscribes to this topic to show connection pills.
 * Last known state is cached so new subscribers can poll current state via REST.
 */
@Component
public class ForwarderStatusPublisher {

    private final LiveTimingHub liveTimingHub;
    private volatile ForwarderStatusDto lastKnownStatus =
            new ForwarderStatusDto("DISCONNECTED", "DISCONNECTED");

    public ForwarderStatusPublisher(LiveTimingHub liveTimingHub) {
        this.liveTimingHub = liveTimingHub;
    }

    public ForwarderStatusDto getLastKnownStatus() {
        return lastKnownStatus;
    }

    public void onForwarderConnected() {
        lastKnownStatus = new ForwarderStatusDto("CONNECTED", "CONNECTED");
        liveTimingHub.broadcastForwarderStatus(lastKnownStatus);
    }

    public void onForwarderDisconnected() {
        lastKnownStatus = new ForwarderStatusDto("DISCONNECTED", "DISCONNECTED");
        liveTimingHub.broadcastForwarderStatus(lastKnownStatus);
    }

    public void onDecoderReconnecting() {
        lastKnownStatus = new ForwarderStatusDto("RECONNECTING", "CONNECTED");
        liveTimingHub.broadcastForwarderStatus(lastKnownStatus);
    }
}
