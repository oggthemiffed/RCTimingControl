package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;
import dev.monkeypatch.rctiming.domain.race.RaceAuditLabels;
import dev.monkeypatch.rctiming.timing.LapTimingService;
import dev.monkeypatch.rctiming.timing.UnknownTransponderLinkAudit;
import dev.monkeypatch.rctiming.timing.UnknownTransponderLinkAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Links an unknown transponder to an entry during a live race (TIMING-08, D-12), crediting every passing it has
 * made since the start.
 */
@Service
@Transactional
public class TransponderLinkService {

    private final LapTimingService lapTimingService;
    private final UnknownTransponderLinkAuditRepository linkAuditRepository;
    private final AuditService audit;
    private final RaceAuditLabels labels;

    public TransponderLinkService(LapTimingService lapTimingService,
                                  UnknownTransponderLinkAuditRepository linkAuditRepository,
                                  AuditService audit,
                                  RaceAuditLabels labels) {
        this.lapTimingService = lapTimingService;
        this.linkAuditRepository = linkAuditRepository;
        this.audit = audit;
        this.labels = labels;
    }

    /**
     * The audit rows and the link are one transaction, with the link last: if it fails nothing is recorded as
     * linked, and if recording fails the link is not made.
     *
     * @return how many laps the link credited
     */
    public int link(long raceId, String transponderNumber, long entryId, long userId) {
        // Counted before linking, since linking credits them
        int lapsCredited = lapTimingService.countPassingsForTransponder(raceId, transponderNumber);

        linkAuditRepository.save(new UnknownTransponderLinkAudit(raceId, transponderNumber, entryId, userId));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("transponderNumber", transponderNumber);
        after.put("entryId", entryId);
        after.put("lapsCredited", lapsCredited);
        audit.entry(Actor.official(userId), "UNKNOWN_TRANSPONDER_LINKED")
                .entity("race", raceId).race(raceId).event(labels.eventOf(raceId))
                .summary("Linked transponder " + transponderNumber + " to " + labels.driver(entryId) + " in "
                        + labels.race(raceId) + ", crediting " + lapsCredited + " laps")
                .after(after).record();

        lapTimingService.linkTransponder(raceId, transponderNumber, entryId);
        return lapsCredited;
    }
}
