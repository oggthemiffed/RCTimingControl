package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.forwarder.dto.ForwarderStatusDto;
import dev.monkeypatch.rctiming.timing.LiveTimingHub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Phase 5 / TIMING-02: tracks gRPC stream lifecycle and decoder TCP state, broadcasting
 * ForwarderStatusDto to /topic/system/forwarder-status via LiveTimingHub.
 * Frontend status bar subscribes to this topic to show connection pills.
 * Last known state is cached so new subscribers can poll current state via REST.
 *
 * <p>Decoder and forwarder states are tracked independently:
 * - forwarder state changes when the gRPC stream opens/closes
 * - decoder state is set by the direct {@code DecoderListener} (L1) through
 *   {@link #onDirectDecoderStatus(String)}
 * - the legacy forwarder's ReportStatus RPC sets decoder state only when the direct listener is
 *   disabled. With both active, a stale or disconnecting forwarder must not overwrite the
 *   decoder state the listener reports.
 * - forwarder disconnect resets decoder to DISCONNECTED only in legacy mode
 */
@Component
public class ForwarderStatusPublisher {

    private final LiveTimingHub liveTimingHub;
    private final boolean directListenerEnabled;
    private volatile String decoderState   = "DISCONNECTED";
    private volatile String forwarderState = "DISCONNECTED";

    public ForwarderStatusPublisher(LiveTimingHub liveTimingHub,
                                    @Value("${app.decoder.listener.enabled:true}") boolean directListenerEnabled) {
        this.liveTimingHub = liveTimingHub;
        this.directListenerEnabled = directListenerEnabled;
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
        if (!directListenerEnabled) {
            decoderState = "DISCONNECTED";
        }
        broadcast();
    }

    /** Called when the forwarder reports a decoder TCP state change via ReportStatus RPC (legacy path). */
    public void onDecoderStatus(String state) {
        if (directListenerEnabled) {
            return;
        }
        decoderState = state;
        broadcast();
    }

    /** Called by the direct decoder listener whenever its TCP connection state changes. */
    public void onDirectDecoderStatus(String state) {
        decoderState = state;
        broadcast();
    }

    private void broadcast() {
        liveTimingHub.broadcastForwarderStatus(new ForwarderStatusDto(decoderState, forwarderState));
    }
}
