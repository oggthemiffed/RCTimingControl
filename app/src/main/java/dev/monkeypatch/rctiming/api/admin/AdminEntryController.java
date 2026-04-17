package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.AdminUpdateTransponderRequest;
import dev.monkeypatch.rctiming.api.admin.dto.MembershipOverrideRequest;
import dev.monkeypatch.rctiming.api.racer.dto.EntryDto;
import dev.monkeypatch.rctiming.domain.entry.EntryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/entries")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class AdminEntryController {

    private final EntryService entryService;

    public AdminEntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @PatchMapping("/{id}/transponder")
    public EntryDto updateTransponder(@PathVariable Long id,
                                      Authentication auth,
                                      @RequestBody @Valid AdminUpdateTransponderRequest req) {
        Long adminId = Long.parseLong(auth.getName());
        return entryService.adminUpdateTransponder(id, adminId, req);
    }

    @PostMapping("/{id}/membership-override")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public EntryDto applyMembershipOverride(@PathVariable Long id,
                                             Authentication auth,
                                             @RequestBody @Valid MembershipOverrideRequest req) {
        Long adminId = Long.parseLong(auth.getName());
        return entryService.adminApplyMembershipOverride(id, adminId, req.reason());
    }
}
