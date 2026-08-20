package dev.monkeypatch.rctiming.domain.localday;

import dev.monkeypatch.rctiming.domain.car.Car;
import dev.monkeypatch.rctiming.domain.car.CarRepository;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PreCacheService {

    private final EventRepository eventRepository;
    private final EntryRepository entryRepository;
    private final RaceRepository raceRepository;
    private final RoundRepository roundRepository;
    private final EventClassRepository eventClassRepository;
    private final RacingClassRepository racingClassRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final EventSyncGenerationRepository eventSyncGenerationRepository;
    private final LocaldayCredentialRepository localdayCredentialRepository;
    private final LocaldayInstanceSecretRepository localdayInstanceSecretRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PreCacheService(EventRepository eventRepository,
                            EntryRepository entryRepository,
                            RaceRepository raceRepository,
                            RoundRepository roundRepository,
                            EventClassRepository eventClassRepository,
                            RacingClassRepository racingClassRepository,
                            CarRepository carRepository,
                            UserRepository userRepository,
                            EventSyncGenerationRepository eventSyncGenerationRepository,
                            LocaldayCredentialRepository localdayCredentialRepository,
                            LocaldayInstanceSecretRepository localdayInstanceSecretRepository,
                            PasswordEncoder passwordEncoder) {
        this.eventRepository = eventRepository;
        this.entryRepository = entryRepository;
        this.raceRepository = raceRepository;
        this.roundRepository = roundRepository;
        this.eventClassRepository = eventClassRepository;
        this.racingClassRepository = racingClassRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.eventSyncGenerationRepository = eventSyncGenerationRepository;
        this.localdayCredentialRepository = localdayCredentialRepository;
        this.localdayInstanceSecretRepository = localdayInstanceSecretRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Internal holder carrying everything the controller needs to assemble the response DTO. */
    public record PreCacheResult(
            Event event,
            List<EntryRow> entries,
            List<ScheduleRow> schedule,
            List<CredentialRow> officialCredentials,
            String instanceId,
            String instanceSecret,
            long generation
    ) {}

    public record EntryRow(Long cloudEntryId, String transponderNumber, String racerName, String carName, String className) {}

    public record ScheduleRow(Long cloudRaceId, int roundNumber, int heatNumber, int sequence,
                               String className, String finalLetter, String status) {}

    public record CredentialRow(Long cloudUserId, String officialName, String pin) {}

    public PreCacheResult buildPreCache(Long eventId, String instanceId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        List<Entry> entries = entryRepository.findByEventIdAndStatus(eventId, EntryStatus.CONFIRMED);
        List<Race> races = raceRepository.findByEventId(eventId);

        // --- Batch-resolve entry display data ---
        Set<Long> userIds = entries.stream().map(Entry::getUserId).collect(Collectors.toSet());
        Set<Long> carIds = entries.stream().map(Entry::getCarId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> entryEventClassIds = entries.stream().map(Entry::getEventClassId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Car> carsById = carRepository.findAllById(carIds).stream()
                .collect(Collectors.toMap(Car::getId, c -> c));

        // --- Batch-resolve className for both entries and races via EventClass -> RacingClass ---
        Set<Long> allEventClassIds = new HashSet<>(entryEventClassIds);
        races.stream().map(Race::getEventClassId).filter(java.util.Objects::nonNull).forEach(allEventClassIds::add);

        Map<Long, EventClass> eventClassesById = eventClassRepository.findAllById(allEventClassIds).stream()
                .collect(Collectors.toMap(EventClass::getId, ec -> ec));
        Set<Long> racingClassIds = eventClassesById.values().stream()
                .map(EventClass::getRacingClassId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, RacingClass> racingClassesById = racingClassRepository.findAllById(racingClassIds).stream()
                .collect(Collectors.toMap(RacingClass::getId, rc -> rc));

        List<EntryRow> entryRows = new ArrayList<>();
        for (Entry entry : entries) {
            User user = usersById.get(entry.getUserId());
            Car car = entry.getCarId() != null ? carsById.get(entry.getCarId()) : null;
            String className = resolveClassName(entry.getEventClassId(), eventClassesById, racingClassesById);
            entryRows.add(new EntryRow(
                    entry.getId(),
                    entry.getTransponderNumberSnapshot(),
                    user != null ? user.getFirstName() + " " + user.getLastName() : null,
                    car != null ? car.getName() : null,
                    className
            ));
        }

        // --- Batch-resolve schedule display data ---
        Set<Long> roundIds = races.stream().map(Race::getRoundId).collect(Collectors.toSet());
        Map<Long, Round> roundsById = roundRepository.findAllById(roundIds).stream()
                .collect(Collectors.toMap(Round::getId, r -> r));

        List<ScheduleRow> scheduleRows = new ArrayList<>();
        for (Race race : races) {
            Round round = roundsById.get(race.getRoundId());
            String className = resolveClassName(race.getEventClassId(), eventClassesById, racingClassesById);
            scheduleRows.add(new ScheduleRow(
                    race.getId(),
                    round != null ? round.getRoundNumber() : 0,
                    race.getHeatNumber(),
                    race.getSequenceInRound(),
                    className,
                    race.getFinalLetter(),
                    race.getStatus().name()
            ));
        }

        // --- Mint officials' credentials ---
        List<User> officials = userRepository.findByRolesIn(Set.of(Role.ADMIN, Role.RACE_DIRECTOR, Role.REFEREE));
        List<CredentialRow> credentialRows = new ArrayList<>();
        for (User official : officials) {
            String pin = String.format("%06d", secureRandom.nextInt(1_000_000));
            String hash = passwordEncoder.encode(pin);

            // Replace in place rather than delete-then-insert: Hibernate flushes inserts before
            // deletes within a single transaction, which would otherwise trip the unique index
            // on (event_id, user_id) when re-calling pre-cache for the same official.
            LocaldayCredential credential = localdayCredentialRepository.findByEventIdAndUserId(eventId, official.getId())
                    .orElseGet(LocaldayCredential::new);
            credential.setEventId(eventId);
            credential.setUserId(official.getId());
            credential.setSecretHash(hash);
            credential.setIssuedAt(Instant.now());
            localdayCredentialRepository.save(credential);

            credentialRows.add(new CredentialRow(
                    official.getId(),
                    official.getFirstName() + " " + official.getLastName(),
                    pin
            ));
        }

        // --- Mint instance secret ---
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        String instanceSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        String instanceSecretHash = passwordEncoder.encode(instanceSecret);

        // Same replace-in-place rationale as the official credentials above.
        LocaldayInstanceSecret secretEntity = localdayInstanceSecretRepository.findByEventIdAndInstanceId(eventId, instanceId)
                .orElseGet(LocaldayInstanceSecret::new);
        secretEntity.setEventId(eventId);
        secretEntity.setInstanceId(instanceId);
        secretEntity.setSecretHash(instanceSecretHash);
        secretEntity.setIssuedAt(Instant.now());
        localdayInstanceSecretRepository.save(secretEntity);

        // --- Claim a generation ---
        EventSyncGeneration generationRow = eventSyncGenerationRepository.findByIdForUpdate(eventId)
                .orElseGet(() -> {
                    EventSyncGeneration created = new EventSyncGeneration();
                    created.setEventId(eventId);
                    created.setGeneration(0);
                    return eventSyncGenerationRepository.save(created);
                });
        generationRow.setGeneration(generationRow.getGeneration() + 1);
        eventSyncGenerationRepository.save(generationRow);

        return new PreCacheResult(
                event,
                entryRows,
                scheduleRows,
                credentialRows,
                instanceId,
                instanceSecret,
                generationRow.getGeneration()
        );
    }

    private String resolveClassName(Long eventClassId,
                                     Map<Long, EventClass> eventClassesById,
                                     Map<Long, RacingClass> racingClassesById) {
        if (eventClassId == null) return null;
        EventClass eventClass = eventClassesById.get(eventClassId);
        if (eventClass == null || eventClass.getRacingClassId() == null) return null;
        RacingClass racingClass = racingClassesById.get(eventClass.getRacingClassId());
        return racingClass != null ? racingClass.getName() : null;
    }
}
