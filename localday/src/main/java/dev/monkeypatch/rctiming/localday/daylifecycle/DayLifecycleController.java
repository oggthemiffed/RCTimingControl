package dev.monkeypatch.rctiming.localday.daylifecycle;

import dev.monkeypatch.rctiming.localday.daylifecycle.dto.DayCloseResponseDto;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.DayLifecycleStatusDto;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.DayOpenRequest;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.ErrorResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.PreCacheRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pre-cache pull and day-open/day-close endpoints (U10). {@code /pre-cache}, {@code /open}, and
 * {@code /status} are unauthenticated by design — they must be reachable before any local
 * officials' session exists (that's the bootstrap problem this unit solves), per
 * {@link dev.monkeypatch.rctiming.localday.config.LocalSecurityConfig}. {@code /close} is not
 * explicitly permitted there, so it falls under that config's default-deny
 * {@code anyRequest().authenticated()} rule — an official is always logged in locally by the
 * time they close the day.
 */
@RestController
@RequestMapping("/api/v1/day-lifecycle")
public class DayLifecycleController {

    private final DayLifecycleService dayLifecycleService;

    public DayLifecycleController(DayLifecycleService dayLifecycleService) {
        this.dayLifecycleService = dayLifecycleService;
    }

    @PostMapping("/pre-cache")
    public ResponseEntity<?> preCache(@RequestBody PreCacheRequest request) {
        try {
            DayLifecycleState state = dayLifecycleService.preCache(request.eventId(), request.email(), request.password());
            return ResponseEntity.ok(DayLifecycleStatusDto.from(state));
        } catch (CloudUnreachableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse("cloud_unreachable"));
        } catch (CloudRequestException e) {
            return ResponseEntity.status(resolveStatus(e.getStatusCode())).body(new ErrorResponse("cloud_request_failed"));
        }
    }

    @PostMapping("/open")
    public ResponseEntity<?> open(@RequestBody DayOpenRequest request) {
        try {
            DayLifecycleState state = dayLifecycleService.open(request.eventId(), request.email(), request.password());
            return ResponseEntity.ok(DayLifecycleStatusDto.from(state));
        } catch (OfflineOpenUnavailableException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("offline_open_unavailable"));
        } catch (CloudRequestException e) {
            return ResponseEntity.status(resolveStatus(e.getStatusCode())).body(new ErrorResponse("cloud_request_failed"));
        }
    }

    @PostMapping("/close")
    public ResponseEntity<DayCloseResponseDto> close() {
        // requestedSyncComplete is deliberately not read from a request body — the service's
        // actual gate is DayLifecycleState.pendingSyncCount, not a caller-asserted flag.
        DayCloseOutcome outcome = dayLifecycleService.close(true);
        return switch (outcome) {
            case DayCloseOutcome.Closed ignored ->
                    ResponseEntity.ok(new DayCloseResponseDto("closed", 0));
            case DayCloseOutcome.Pending pending ->
                    ResponseEntity.ok(new DayCloseResponseDto("pending", pending.pendingSyncCount()));
        };
    }

    @GetMapping("/status")
    public DayLifecycleStatusDto status() {
        return DayLifecycleStatusDto.from(dayLifecycleService.status());
    }

    private static HttpStatus resolveStatus(int statusCode) {
        HttpStatus resolved = HttpStatus.resolve(statusCode);
        return resolved != null ? resolved : HttpStatus.BAD_GATEWAY;
    }
}
