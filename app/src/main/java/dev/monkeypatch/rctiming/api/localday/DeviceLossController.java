package dev.monkeypatch.rctiming.api.localday;

import dev.monkeypatch.rctiming.api.localday.dto.DeviceLossRequestDto;
import dev.monkeypatch.rctiming.api.localday.dto.DeviceLossResponseDto;
import dev.monkeypatch.rctiming.domain.localday.DeviceLossService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Device-loss declaration (F5, R16, R17) — an elevated official confirming on-site that a Local
 * Race Day Program instance is physically down, unlocking the day for a replacement instance and
 * permanently flagging the resulting data gap. Gated to {@code ADMIN} specifically (narrower than
 * {@code DayLifecycleController}/{@code PreCacheController}'s {@code ADMIN}-or-{@code
 * RACE_DIRECTOR}), since this is the irreversible, day-affecting action the plan singles out as
 * deserving the stricter role.
 */
@RestController
@RequestMapping("/api/v1/localday/events/{eventId}/device-loss")
@PreAuthorize("hasRole('ADMIN')")
public class DeviceLossController {

    private final DeviceLossService deviceLossService;

    public DeviceLossController(DeviceLossService deviceLossService) {
        this.deviceLossService = deviceLossService;
    }

    @PostMapping
    public DeviceLossResponseDto declare(@PathVariable Long eventId, @RequestBody DeviceLossRequestDto request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long adminUserId = Long.parseLong(auth.getName());

        DeviceLossService.Result result =
                deviceLossService.declare(eventId, request.instanceId(), adminUserId, request.reason());
        return new DeviceLossResponseDto(result.eventId(), result.instanceId(), true, result.declaredAt());
    }
}
