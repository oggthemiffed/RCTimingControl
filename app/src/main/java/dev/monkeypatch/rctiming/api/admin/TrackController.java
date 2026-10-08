package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.api.admin.dto.CreateDecoderLoopRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateThresholdRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateTrackRequest;
import dev.monkeypatch.rctiming.api.admin.dto.DecoderLoopDto;
import dev.monkeypatch.rctiming.api.admin.dto.TrackDto;
import dev.monkeypatch.rctiming.api.admin.dto.TrackLapThresholdDto;
import dev.monkeypatch.rctiming.domain.track.TrackService;
import dev.monkeypatch.rctiming.security.CurrentOfficial;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @Audited("audit_log")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TrackDto createTrack(Authentication auth, @RequestBody @Valid CreateTrackRequest request) {
        return trackService.create(CurrentOfficial.actor(auth), request);
    }

    @Audited("audit_log")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TrackDto updateTrack(Authentication auth, @PathVariable Long id, @RequestBody @Valid CreateTrackRequest request) {
        return trackService.update(CurrentOfficial.actor(auth), id, request);
    }

    @Audited("audit_log")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrack(Authentication auth, @PathVariable Long id) {
        trackService.delete(CurrentOfficial.actor(auth), id);
    }

    @Audited("audit_log")
    @PostMapping("/{trackId}/loops")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DecoderLoopDto addDecoderLoop(Authentication auth, @PathVariable Long trackId,
                                          @RequestBody @Valid CreateDecoderLoopRequest request) {
        return trackService.addDecoderLoop(CurrentOfficial.actor(auth), trackId, request);
    }

    @Audited("audit_log")
    @PutMapping("/loops/{loopId}")
    @PreAuthorize("hasRole('ADMIN')")
    public DecoderLoopDto updateDecoderLoop(Authentication auth, @PathVariable Long loopId,
                                             @RequestBody @Valid CreateDecoderLoopRequest request) {
        return trackService.updateDecoderLoop(CurrentOfficial.actor(auth), loopId, request);
    }

    @Audited("audit_log")
    @DeleteMapping("/loops/{loopId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDecoderLoop(Authentication auth, @PathVariable Long loopId) {
        trackService.deleteDecoderLoop(CurrentOfficial.actor(auth), loopId);
    }

    @Audited("audit_log")
    @PostMapping("/{trackId}/thresholds")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TrackLapThresholdDto setLapThreshold(Authentication auth, @PathVariable Long trackId,
                                                 @RequestBody @Valid CreateThresholdRequest request) {
        return trackService.setLapThreshold(CurrentOfficial.actor(auth), trackId, request);
    }

    @Audited("audit_log")
    @DeleteMapping("/thresholds/{thresholdId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLapThreshold(Authentication auth, @PathVariable Long thresholdId) {
        trackService.deleteLapThreshold(CurrentOfficial.actor(auth), thresholdId);
    }
}
