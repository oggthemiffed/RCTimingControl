package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.forwarder.dto.ForwarderStatusDto;
import dev.monkeypatch.rctiming.timing.LiveTimingHub;
import org.springframework.stereotype.Component;

/**
 * Phase 5 / TIMING-02: tracks gRPC stream lifecycle and decoder TCP state, broadcasting
 * ForwarderStatusDto to /topic/system/forwarder-status via LiveTimingHub.
 * Frontend status bar subscribes to this topic to show connection pills.
 * Last known state is cached so new subscribers can poll current state via REST.
 *
 * <p>Decoder and forwarder states are tracked independently:
 * - forwarder state changes when the gRPC stream opens/closes
 * - decoder state changes when the forwarder reports TCP connection changes via ReportStatus RPC
 * - forwarder disconnect resets decoder to DISCONNECTED (no decoder info without a forwarder)
 */
@Component
public class ForwarderStatusPublisher {

    private final LiveTimingHub liveTimingHub;
    private volatile String decoderState   = "DISCONNECTED";
    private volatile String forwarderState = "DISCONNECTED";

    public ForwarderStatusPublisher(LiveTimingHub liveTimingHub) {
        this.liveTimingHub = liveTimingHub;
    }

    public ForwarderStatusDto getLastKnownStatus() {
        return new ForwarderStatusDto(decoderState, forwarderState);
    }

    public void onForwarderConnected() {
        forwarderState = "CONNECTED";
        broadcast();
    }

    public void onForwarderDisconnected() {
        forwarderState = "DISCONNECTED";
        decoderState   = "DISCONNECTED";
        broadcast();
    }

    /** Called when the forwarder reports a decoder TCP state change via ReportStatus RPC. */
    public void onDecoderStatus(String state) {
        decoderState = state;
        broadcast();
    }

    private void broadcast() {
        liveTimingHub.broadcastForwarderStatus(new ForwarderStatusDto(decoderState, forwarderState));
    }
}
