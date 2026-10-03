package dev.monkeypatch.rctiming.localday.testsupport;

import dev.monkeypatch.rctiming.localday.auth.LocalCredential;
import dev.monkeypatch.rctiming.localday.auth.LocalCredentialRepository;
import dev.monkeypatch.rctiming.localday.daylifecycle.DayLifecycleState;
import dev.monkeypatch.rctiming.localday.daylifecycle.DayLifecycleStateRepository;
import dev.monkeypatch.rctiming.localday.daylifecycle.DayLifecycleStatus;
import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import dev.monkeypatch.rctiming.localday.auth.LocalSessionRepository;
import dev.monkeypatch.rctiming.localday.checkin.TransponderReassignmentAuditRepository;
import dev.monkeypatch.rctiming.localday.domain.LapPassingRepository;
import dev.monkeypatch.rctiming.localday.race.MarshalAdjustmentRepository;
import dev.monkeypatch.rctiming.localday.race.RaceResultEntryRepository;
import dev.monkeypatch.rctiming.localday.sync.SnapshotQueueRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Test-only fixture seeding for the {@code frontend-local} Playwright e2e suite (AE1, AE4). Only
 * registered under the {@code e2e} Spring profile — {@link dev.monkeypatch.rctiming.localday.config.LocalSecurityConfig}
 * only permits this path unauthenticated under that same profile, so this endpoint does not
 * exist at all in a normal run.
 *
 * <p>Bypasses the real pre-cache/day-open flow (which requires a reachable cloud) entirely —
 * deliberately so: these tests prove the local race-day program works with zero cloud
 * connectivity, so faking the cloud round-trip would be less faithful than skipping it.
 *
 * <p>Idempotent — each call clears every row it could have previously created (child tables
 * first, to respect FK constraints) before re-inserting, so several spec files can each call
 * this against one shared backend instance without colliding on a prior run's data. Still
 * expects a freshly migrated database with no unrelated rows in these tables — a throwaway
 * embedded-Postgres data directory stood up just for a test run, matching how this module's own
 * {@code RestartPersistenceIT} isolates itself.
 */
@RestController
@RequestMapping("/api/v1/test-support")
@Profile("e2e")
public class E2eSeedController {

    /** Plaintext PIN for the seeded official — known to the Playwright suite. */
    private static final String OFFICIAL_SECRET = "135790";

    private final DayLifecycleStateRepository dayLifecycleStateRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final LocalSessionRepository localSessionRepository;
    private final CachedEntryRepository cachedEntryRepository;
    private final CachedScheduleEntryRepository cachedScheduleEntryRepository;
    private final CachedRaceEntryRepository cachedRaceEntryRepository;
    private final LapPassingRepository lapPassingRepository;
    private final MarshalAdjustmentRepository marshalAdjustmentRepository;
    private final RaceResultEntryRepository raceResultEntryRepository;
    private final TransponderReassignmentAuditRepository transponderReassignmentAuditRepository;
    private final SnapshotQueueRepository snapshotQueueRepository;
    private final PasswordEncoder passwordEncoder;

    public E2eSeedController(DayLifecycleStateRepository dayLifecycleStateRepository,
                              LocalCredentialRepository localCredentialRepository,
                              LocalSessionRepository localSessionRepository,
                              CachedEntryRepository cachedEntryRepository,
                              CachedScheduleEntryRepository cachedScheduleEntryRepository,
                              CachedRaceEntryRepository cachedRaceEntryRepository,
                              LapPassingRepository lapPassingRepository,
                              MarshalAdjustmentRepository marshalAdjustmentRepository,
                              RaceResultEntryRepository raceResultEntryRepository,
                              TransponderReassignmentAuditRepository transponderReassignmentAuditRepository,
                              SnapshotQueueRepository snapshotQueueRepository,
                              PasswordEncoder passwordEncoder) {
        this.dayLifecycleStateRepository = dayLifecycleStateRepository;
        this.localCredentialRepository = localCredentialRepository;
        this.localSessionRepository = localSessionRepository;
        this.cachedEntryRepository = cachedEntryRepository;
        this.cachedScheduleEntryRepository = cachedScheduleEntryRepository;
        this.cachedRaceEntryRepository = cachedRaceEntryRepository;
        this.lapPassingRepository = lapPassingRepository;
        this.marshalAdjustmentRepository = marshalAdjustmentRepository;
        this.raceResultEntryRepository = raceResultEntryRepository;
        this.transponderReassignmentAuditRepository = transponderReassignmentAuditRepository;
        this.snapshotQueueRepository = snapshotQueueRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/seed")
    @Transactional
    public SeedResponseDto seed() {
        // Idempotent: a test run may call this more than once against the same backend instance
        // (e.g. several spec files sharing one dev-server run) — clear every row this endpoint
        // itself could have created, child tables (FK dependents) first, so re-seeding never
        // trips a unique-constraint or FK violation.
        // *InBatch() issues an immediate bulk DELETE statement rather than registering each row
        // as a managed-entity removal — plain deleteAll() defers the physical DELETE to flush
        // time, and Hibernate's fixed flush action ordering runs this method's later inserts
        // before those deferred deletes regardless of code order, which reintroduces exactly the
        // unique-constraint collision this cleanup exists to avoid.
        lapPassingRepository.deleteAllInBatch();
        marshalAdjustmentRepository.deleteAllInBatch();
        raceResultEntryRepository.deleteAllInBatch();
        cachedRaceEntryRepository.deleteAllInBatch();
        transponderReassignmentAuditRepository.deleteAllInBatch();
        localSessionRepository.deleteAllInBatch();
        snapshotQueueRepository.deleteAllInBatch();
        cachedScheduleEntryRepository.deleteAllInBatch();
        cachedEntryRepository.deleteAllInBatch();
        localCredentialRepository.deleteAllInBatch();

        DayLifecycleState state = dayLifecycleStateRepository.findById(DayLifecycleState.SINGLETON_ID)
                .orElseGet(DayLifecycleState::new);
        state.setInstanceId(UUID.randomUUID().toString());
        state.setCloudEventId(999L);
        state.setStatus(DayLifecycleStatus.OPEN);
        state.setGeneration(1L);
        state.setSplitBrainWarning(false);
        state.setSuperseded(false);
        dayLifecycleStateRepository.save(state);

        LocalCredential official = new LocalCredential();
        official.setOfficialName("E2E Official");
        official.setSecretHash(passwordEncoder.encode(OFFICIAL_SECRET));
        localCredentialRepository.save(official);

        CachedEntry alice = cachedEntry(1L, "11111", "Alice Racer", "Car A");
        CachedEntry bob = cachedEntry(2L, "22222", "Bob Racer", "Car B");
        CachedEntry carol = cachedEntry(3L, "33333", "Carol Racer", "Car C");
        cachedEntryRepository.saveAll(List.of(alice, bob, carol));

        CachedScheduleEntry raceA = cachedRace(101L, 1, 1, 1);
        CachedScheduleEntry raceB = cachedRace(102L, 2, 1, 2);
        cachedScheduleEntryRepository.saveAll(List.of(raceA, raceB));

        // Race B's grid is pre-populated with the same entrants (as a real next-round bracket
        // would be) so advance-round has rows to reorder — RoundGeneratorService only updates
        // gridPosition on existing rows, it does not create new ones.
        cachedRaceEntryRepository.saveAll(List.of(
                raceEntry(raceA.getId(), alice.getId(), 1, 1),
                raceEntry(raceA.getId(), bob.getId(), 2, 2),
                raceEntry(raceA.getId(), carol.getId(), 3, 3),
                raceEntry(raceB.getId(), alice.getId(), 1, 1),
                raceEntry(raceB.getId(), bob.getId(), 2, 2),
                raceEntry(raceB.getId(), carol.getId(), 3, 3)
        ));

        return new SeedResponseDto(official.getOfficialName(), OFFICIAL_SECRET, raceA.getId(), raceB.getId());
    }

    private static CachedEntry cachedEntry(long cloudEntryId, String transponder, String racerName, String carName) {
        CachedEntry entry = new CachedEntry();
        entry.setCloudEntryId(cloudEntryId);
        entry.setTransponderNumber(transponder);
        entry.setRacerName(racerName);
        entry.setCarName(carName);
        entry.setClassName("Stock Buggy");
        return entry;
    }

    private static CachedScheduleEntry cachedRace(long cloudRaceId, int roundNumber, int heatNumber, int sequence) {
        CachedScheduleEntry race = new CachedScheduleEntry();
        race.setCloudRaceId(cloudRaceId);
        race.setRoundNumber(roundNumber);
        race.setHeatNumber(heatNumber);
        race.setSequence(sequence);
        race.setClassName("Stock Buggy");
        return race;
    }

    private static CachedRaceEntry raceEntry(Long scheduleId, Long entryId, int gridPosition, int carNumber) {
        CachedRaceEntry raceEntry = new CachedRaceEntry();
        raceEntry.setCachedScheduleId(scheduleId);
        raceEntry.setCachedEntryId(entryId);
        raceEntry.setGridPosition(gridPosition);
        raceEntry.setCarNumber(carNumber);
        return raceEntry;
    }

    public record SeedResponseDto(String officialName, String officialSecret, Long raceAId, Long raceBId) {}
}
