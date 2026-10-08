package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.api.admin.dto.AdminCreateEntryRequest;
import dev.monkeypatch.rctiming.api.admin.dto.AdminWithdrawRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EntryDto;
import dev.monkeypatch.rctiming.api.admin.dto.EntryResult;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryService;
import dev.monkeypatch.rctiming.query.entry.AdminEntryDto;
import dev.monkeypatch.rctiming.query.entry.AdminEntryQueryService;
import dev.monkeypatch.rctiming.query.entry.EntryHistoryDto;
import dev.monkeypatch.rctiming.query.entry.EntryHistoryQuery;
import dev.monkeypatch.rctiming.security.CurrentOfficial;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/entries")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class AdminEntryController {

    private final EntryService entryService;
    private final AdminEntryQueryService adminEntryQueryService;
    private final EntryHistoryQuery entryHistoryQuery;
    private final EntryRepository entryRepository;

    public AdminEntryController(EntryService entryService,
                                AdminEntryQueryService adminEntryQueryService,
                                EntryHistoryQuery entryHistoryQuery,
                                EntryRepository entryRepository) {
        this.entryService = entryService;
        this.adminEntryQueryService = adminEntryQueryService;
        this.entryHistoryQuery = entryHistoryQuery;
        this.entryRepository = entryRepository;
    }

    @GetMapping("/events/{eventId}/classes/{classId}")
    public List<AdminEntryDto> listEntriesForClass(@PathVariable Long eventId,
                                                    @PathVariable Long classId) {
        return adminEntryQueryService.listEntriesForClass(eventId, classId);
    }

    /** What has happened to one entry, oldest first (#140). Admins only: it names the officials who acted. */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public List<EntryHistoryDto> getHistory(@PathVariable long id) {
        entryRepository.requireExists(id);
        return entryHistoryQuery.forEntry(id);
    }

    /** Adds a walk-in entry by hand (L9, #17). */
    @Audited("entry_audit_log")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public EntryResult createEntry(Authentication auth, @RequestBody @Valid AdminCreateEntryRequest req) {
        return entryService.adminCreateEntry(CurrentOfficial.id(auth), req);
    }

    @Audited("entry_audit_log")
    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public EntryDto withdrawEntry(@PathVariable Long id,
                                  Authentication auth,
                                  @RequestBody @Valid AdminWithdrawRequest req) {
        return entryService.adminWithdraw(id, CurrentOfficial.id(auth), req.reason());
    }
}
