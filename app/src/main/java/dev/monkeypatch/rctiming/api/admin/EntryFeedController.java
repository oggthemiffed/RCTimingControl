package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.domain.entryfeed.EntryFeed;
import dev.monkeypatch.rctiming.domain.entryfeed.EntryFeedService;
import dev.monkeypatch.rctiming.domain.entryfeed.EntryFeedStatus;
import dev.monkeypatch.rctiming.domain.racehub.RaceHubImportResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** An event's entry feed: where its entries are pulled from, fetching, and confirming what was fetched (#42). */
@RestController
@RequestMapping("/api/v1/admin/events/{eventId}/entry-feed")
@PreAuthorize("hasRole('ADMIN')")
public class EntryFeedController {

    private final EntryFeedService feedService;

    public EntryFeedController(EntryFeedService feedService) {
        this.feedService = feedService;
    }

    /** The feed's settings and latest fetch, or 204 when the event has none. */
    @GetMapping
    public ResponseEntity<EntryFeedDto> get(@PathVariable long eventId) {
        return feedService.find(eventId).map(EntryFeedDto::of).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Saves the settings. A null token keeps the saved one; an empty one removes it. */
    @Audited("audit_log")
    @PutMapping
    public EntryFeedDto save(Authentication auth, @PathVariable long eventId, @RequestBody SaveRequest request) {
        return EntryFeedDto.of(feedService.save(actor(auth), eventId, request.url(), request.token(),
                Boolean.TRUE.equals(request.autoFetch())));
    }

    @Audited("audit_log")
    @DeleteMapping
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable long eventId) {
        feedService.delete(actor(auth), eventId);
        return ResponseEntity.noContent().build();
    }

    /** Fetches the file now and holds a new revision for an official to confirm. Failures are in the result. */
    @PostMapping("/fetch")
    public EntryFeedDto fetch(@PathVariable long eventId) {
        return EntryFeedDto.of(feedService.fetchNow(eventId));
    }

    /** What importing the held file would do. Saves nothing. */
    @PostMapping("/preview")
    public RaceHubImportResult preview(@PathVariable long eventId) {
        return feedService.previewHeld(eventId);
    }

    /** Imports the held file, or answers 422 with the preview when something blocks it. */
    @Audited("audit_log")
    @PostMapping("/apply")
    public ResponseEntity<RaceHubImportResult> apply(Authentication auth, @PathVariable long eventId) {
        RaceHubImportResult result = feedService.applyHeld(actor(auth), eventId);
        return ResponseEntity.status(result.blocked() ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK).body(result);
    }

    /** The signed-in official, taken from the token and never from the request body. */
    private static Actor actor(Authentication auth) {
        return Actor.official(Long.parseLong(auth.getName()));
    }

    public record SaveRequest(String url, String token, Boolean autoFetch) {
    }

    /** The token itself is never sent back: only whether one is saved and its last four characters. */
    public record EntryFeedDto(String url, boolean tokenSaved, String tokenHint, boolean autoFetch,
                               Instant lastFetchAt, EntryFeedStatus lastStatus, String lastMessage,
                               Long appliedRevision, boolean waiting, Long waitingRevision) {
        static EntryFeedDto of(EntryFeed f) {
            return new EntryFeedDto(f.getUrl(), f.getTokenEncrypted() != null, f.getTokenHint(), f.isAutoFetch(),
                    f.getLastFetchAt(), f.getLastStatus(), f.getLastError(), f.getAppliedRevision(),
                    f.getHeldDocument() != null, f.getHeldRevision());
        }
    }
}
