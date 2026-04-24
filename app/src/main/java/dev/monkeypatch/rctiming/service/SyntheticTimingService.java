package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.timing.LapPassingEvent;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * Dev-only synthetic timing event generator (D-07).
 * Gated to the "dev" Spring profile — bean is not registered in production.
 * This is the only insertion point that Phase 5's forwarder replaces with real AMB P3 events.
 */
@Service
@Profile("dev")
public class SyntheticTimingService {

    private final ApplicationEventPublisher eventPublisher;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final EntryRepository entryRepository;
    private final Random random = new Random();

    public SyntheticTimingService(ApplicationEventPublisher eventPublisher,
                                   RaceRepository raceRepository,
                                   RaceEntryRepository raceEntryRepository,
                                   EntryRepository entryRepository) {
        this.eventPublisher = eventPublisher;
        this.raceRepository = raceRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * Fire a synthetic lap passing event for a random entry in the given race.
     * Uses the entry's transponderNumberSnapshot as the transponder number,
     * and System.currentTimeMillis() * 1000 as the RTC time in microseconds.
     *
     * @param raceId the race to fire a synthetic passing for
     * @throws EntityNotFoundException if the race does not exist or has no entries
     */
    public void firePassing(long raceId) {
        raceRepository.findById(raceId)
                .orElseThrow(() -> new EntityNotFoundException("Race not found: " + raceId));

        List<RaceEntry> raceEntries = raceEntryRepository.findByRaceIdOrderByGridPosition(raceId);
        if (raceEntries.isEmpty()) {
            throw new EntityNotFoundException("Race " + raceId + " has no entries");
        }

        // Pick a random race entry
        RaceEntry raceEntry = raceEntries.get(random.nextInt(raceEntries.size()));

        // Load the Entry to get the transponder number snapshot
        Entry entry = entryRepository.findById(raceEntry.getEntryId())
                .orElseThrow(() -> new EntityNotFoundException("Entry not found: " + raceEntry.getEntryId()));

        String transponderNumber = entry.getTransponderNumberSnapshot();
        long rtcTimeMicros = System.currentTimeMillis() * 1000L;

        eventPublisher.publishEvent(new LapPassingEvent(raceId, transponderNumber, rtcTimeMicros));
    }
}
