package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.CreateDecoderLoopRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateThresholdRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateTrackRequest;
import dev.monkeypatch.rctiming.api.admin.dto.DecoderLoopDto;
import dev.monkeypatch.rctiming.api.admin.dto.TrackDto;
import dev.monkeypatch.rctiming.api.admin.dto.TrackLapThresholdDto;
import dev.monkeypatch.rctiming.domain.track.TrackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tracks")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping
    public List<TrackDto> listTracks() {
        return trackService.findAll();
    }

    @GetMapping("/{id}")
    public TrackDto getTrack(@PathVariable Long id) {
        return trackService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrackDto createTrack(@RequestBody @Valid CreateTrackRequest request) {
        return trackService.create(request);
    }

    @PutMapping("/{id}")
    public TrackDto updateTrack(@PathVariable Long id, @RequestBody @Valid CreateTrackRequest request) {
        return trackService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrack(@PathVariable Long id) {
        trackService.delete(id);
    }

    @PostMapping("/{trackId}/loops")
    @ResponseStatus(HttpStatus.CREATED)
    public DecoderLoopDto addDecoderLoop(@PathVariable Long trackId,
                                          @RequestBody @Valid CreateDecoderLoopRequest request) {
        return trackService.addDecoderLoop(trackId, request);
    }

    @PutMapping("/loops/{loopId}")
    public DecoderLoopDto updateDecoderLoop(@PathVariable Long loopId,
                                             @RequestBody @Valid CreateDecoderLoopRequest request) {
        return trackService.updateDecoderLoop(loopId, request);
    }

    @DeleteMapping("/loops/{loopId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDecoderLoop(@PathVariable Long loopId) {
        trackService.deleteDecoderLoop(loopId);
    }

    @PostMapping("/{trackId}/thresholds")
    @ResponseStatus(HttpStatus.CREATED)
    public TrackLapThresholdDto setLapThreshold(@PathVariable Long trackId,
                                                 @RequestBody @Valid CreateThresholdRequest request) {
        return trackService.setLapThreshold(trackId, request);
    }

    @DeleteMapping("/thresholds/{thresholdId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLapThreshold(@PathVariable Long thresholdId) {
        trackService.deleteLapThreshold(thresholdId);
    }
}
