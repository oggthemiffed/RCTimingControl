package dev.monkeypatch.rctiming.localday.timing;

import dev.monkeypatch.rctiming.decoderprotocol.timing.AmbRc4TimingSource;
import dev.monkeypatch.rctiming.localday.race.RaceState;
import dev.monkeypatch.rctiming.localday.timing.dto.LiveTimingRowDto;
import dev.monkeypatch.rctiming.localday.timing.dto.MarshalAdjustmentDto;
import dev.monkeypatch.rctiming.localday.timing.dto.RaceStateChangeDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * STOMP broadcast hub. Broadcasts on:
 * <ul>
 *   <li>{@code /topic/race/{scheduleId}/timing} — live position updates</li>
 *   <li>{@code /topic/race/{scheduleId}/state} — race state transitions</li>
 *   <li>{@code /topic/race/{scheduleId}/marshal} — marshal adjustments</li>
 *   <li>{@code /topic/race/{scheduleId}/unknown-transponder} — first sighting of unknown transponder</li>
 *   <li>{@code /topic/system/decoder-status} — decoder TCP connection status; the cloud gets this
 *       via a different channel (ForwarderStatusPublisher/gRPC) that doesn't exist in
 *       {@code :localday}, so this method has no cloud equivalent to port from.</li>
 * </ul>
 *
 * <p>Ported from the cloud's {@code app/.../timing/LiveTimingHub.java} (KD3), minus
 * {@code broadcastForwarderStatus} (cloud-only, forwarder-specific) and
 * {@code broadcastBumpUpAlert} (out of this unit's scope).
 */
@Component
public class LiveTimingHub {

    private final SimpMessagingTemplate messagingTemplate;

    public LiveTimingHub(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastTimingUpdate(long scheduleId, List<LiveTimingRowDto> rows) {
        messagingTemplate.convertAndSend("/topic/race/" + scheduleId + "/timing", rows);
    }

    public void broadcastStateChange(long scheduleId, RaceState newState) {
        messagingTemplate.convertAndSend("/topic/race/" + scheduleId + "/state",
                new RaceStateChangeDto(scheduleId, newState.name()));
    }

    public void broadcastMarshalAdjustment(long scheduleId, MarshalAdjustmentDto dto) {
        messagingTemplate.convertAndSend("/topic/race/" + scheduleId + "/marshal", dto);
    }

    public void broadcastUnknownTransponder(long scheduleId, String transponderNumber) {
        messagingTemplate.convertAndSend("/topic/race/" + scheduleId + "/unknown-transponder",
                Map.of("scheduleId", scheduleId, "transponderNumber", transponderNumber));
    }

    /** Broadcasts decoder TCP connection status so the local UI can show a connection pill. */
    public void broadcastDecoderStatus(AmbRc4TimingSource.ConnectionState state) {
        messagingTemplate.convertAndSend("/topic/system/decoder-status", Map.of("state", state.name()));
    }
}
