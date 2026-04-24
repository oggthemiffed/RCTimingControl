package dev.monkeypatch.rctiming.timing;

import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.timing.dto.LiveTimingRowDto;
import dev.monkeypatch.rctiming.timing.dto.MarshalAdjustmentDto;
import dev.monkeypatch.rctiming.timing.dto.RaceStateChangeDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * STOMP broadcast hub (Pattern 5 from RESEARCH.md).
 * Broadcasts on three topics per CLAUDE.md:
 * - /topic/race/{raceId}/timing — live position updates
 * - /topic/race/{raceId}/state  — race state transitions
 * - /topic/race/{raceId}/marshal — marshal adjustments
 * - /topic/race/{raceId}/unknown-transponder — first sighting of unknown transponder
 */
@Component
public class LiveTimingHub {

    private final SimpMessagingTemplate messagingTemplate;

    public LiveTimingHub(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastTimingUpdate(long raceId, List<LiveTimingRowDto> rows) {
        messagingTemplate.convertAndSend("/topic/race/" + raceId + "/timing", rows);
    }

    public void broadcastStateChange(long raceId, RaceStatus newStatus) {
        messagingTemplate.convertAndSend("/topic/race/" + raceId + "/state",
                new RaceStateChangeDto(raceId, newStatus.name()));
    }

    public void broadcastMarshalAdjustment(long raceId, MarshalAdjustmentDto dto) {
        messagingTemplate.convertAndSend("/topic/race/" + raceId + "/marshal", dto);
    }

    public void broadcastUnknownTransponder(long raceId, String transponderNumber) {
        messagingTemplate.convertAndSend("/topic/race/" + raceId + "/unknown-transponder",
                Map.of("raceId", raceId, "transponderNumber", transponderNumber));
    }
}
