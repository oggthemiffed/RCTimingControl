package dev.monkeypatch.rctiming.domain.racehub;

import dev.monkeypatch.rctiming.domain.competitor.Competitor;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorRepository;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository.EventClassRef;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.racehub.RaceHubEntryExport.ExportEntry;
import dev.monkeypatch.rctiming.domain.racehub.RaceHubImportResult.Action;
import dev.monkeypatch.rctiming.domain.racehub.RaceHubImportResult.Row;
import dev.monkeypatch.rctiming.domain.racehub.RaceHubImportResult.UnmappedClass;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Imports RaceHub Entry Export v1 into an event (L7, #15).
 *
 * <p>The import first builds a plan (one action per export entry), then applies it unless it is a
 * dry run or something blocks it. Entries are upserted by RaceHub's {@code entry_id}, and a row
 * changes an entry only when its {@code entry_version} is higher than the one already applied,
 * so replaying an export is a no-op. A withdrawn entry is marked WITHDRAWN and never deleted.
 */
@Service
public class RaceHubImportService {

    public static final String RACEHUB_SOURCE = "RACEHUB";
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    private static final Set<String> ENTRY_STATUSES = Set.of("CONFIRMED", "WITHDRAWN");
    private static final Set<String> RACE_DAY_STATUSES = Set.of("NOT_ARRIVED", "ARRIVED");

    private final EventRepository eventRepository;
    private final EventClassRepository eventClassRepository;
    private final RacingClassRepository racingClassRepository;
    private final RaceHubClassMappingRepository mappingRepository;
    private final EntryRepository entryRepository;
    private final CompetitorRepository competitorRepository;

    public RaceHubImportService(EventRepository eventRepository,
                                EventClassRepository eventClassRepository,
                                RacingClassRepository racingClassRepository,
                                RaceHubClassMappingRepository mappingRepository,
                                EntryRepository entryRepository,
                                CompetitorRepository competitorRepository) {
        this.eventRepository = eventRepository;
        this.eventClassRepository = eventClassRepository;
        this.racingClassRepository = racingClassRepository;
        this.mappingRepository = mappingRepository;
        this.entryRepository = entryRepository;
        this.competitorRepository = competitorRepository;
    }

    @Transactional
    public RaceHubImportResult importEntries(Long eventId, RaceHubEntryExport export, boolean dryRun) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));
        if (export == null || !Objects.equals(export.schemaVersion(), SUPPORTED_SCHEMA_VERSION)) {
            throw new IllegalArgumentException("Unsupported RaceHub export schema_version: expected "
                    + SUPPORTED_SCHEMA_VERSION + ", got " + (export == null ? null : export.schemaVersion()));
        }
        List<ExportEntry> exportEntries = export.entries() == null ? List.of() : export.entries();

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        ClassResolver classes = new ClassResolver(eventId);
        Map<String, UnmappedAccumulator> unmapped = new TreeMap<>();
        List<Planned> plan = new ArrayList<>();
        Set<String> seenEntryIds = new HashSet<>();

        for (ExportEntry row : exportEntries) {
            if (row == null) {
                errors.add("The export has an empty (null) entry");
                continue;
            }
            String label = row.entryId() == null ? "entry without entry_id" : "entry " + row.entryId();
            List<String> rowErrors = validate(row, label);
            if (rowErrors.isEmpty() && !seenEntryIds.add(row.entryId())) {
                rowErrors.add(label + " appears more than once in the export");
            }
            if (!rowErrors.isEmpty()) {
                errors.addAll(rowErrors);
                continue;
            }

            Entry existing = entryRepository
                    .findByExternalSourceAndExternalEntryId(RACEHUB_SOURCE, row.entryId())
                    .orElse(null);
            if (existing != null && !existing.getEventId().equals(eventId)) {
                errors.add(label + " was imported into another event");
                continue;
            }

            boolean withdrawn = "WITHDRAWN".equals(row.entryStatus());
            Action action;
            if (existing == null) {
                action = withdrawn ? Action.SKIP : Action.CREATE;
            } else {
                long applied = existing.getExternalEntryVersion() == null ? Long.MIN_VALUE : existing.getExternalEntryVersion();
                if (row.entryVersion() < applied) {
                    action = Action.STALE;
                } else if (row.entryVersion() == applied) {
                    action = Action.UNCHANGED;
                } else {
                    action = withdrawn ? Action.WITHDRAW : Action.UPDATE;
                }
            }

            // A class is needed for every row that leaves an active entry behind.
            Long eventClassId = existing == null ? null : existing.getEventClassId();
            if (action == Action.CREATE || action == Action.UPDATE) {
                Optional<Long> resolved = classes.resolve(row);
                if (resolved.isPresent()) {
                    eventClassId = resolved.get();
                } else {
                    unmapped.computeIfAbsent(row.eventClassId(), k -> new UnmappedAccumulator(row)).count++;
                }
            }
            plan.add(new Planned(row, action, existing, eventClassId));
        }

        Map<String, Competitor> competitors = loadCompetitors(plan);
        checkFinalState(eventId, plan, competitors, errors, warnings);

        List<UnmappedClass> unmappedClasses = unmapped.entrySet().stream()
                .map(e -> new UnmappedClass(e.getKey(), e.getValue().rcClassName, e.getValue().className, e.getValue().count))
                .toList();
        boolean blocked = !errors.isEmpty() || !unmappedClasses.isEmpty();
        boolean apply = !dryRun && !blocked;

        Map<Planned, Long> savedIds = new IdentityHashMap<>();
        if (apply) {
            // Withdrawals first, so a driver's replacement entry in the same class does not meet
            // the old one in the one-active-entry index.
            plan.stream().filter(p -> p.action == Action.WITHDRAW)
                    .forEach(p -> savedIds.put(p, applyRow(eventId, p, competitors)));
            entryRepository.flush();
            plan.stream().filter(p -> p.action != Action.WITHDRAW)
                    .forEach(p -> savedIds.put(p, applyRow(eventId, p, competitors)));
            event.setRacehubLastImportAt(Instant.now());
            event.setRacehubLastRevision(export.revision());
            eventRepository.save(event);
        }

        List<Row> rows = new ArrayList<>();
        int[] counts = new int[Action.values().length];
        for (Planned p : plan) {
            Long rctcEntryId = apply ? savedIds.get(p) : (p.existing == null ? null : p.existing.getId());
            counts[p.action.ordinal()]++;
            rows.add(new Row(p.row.entryId(), p.row.entryVersion(), p.row.driverDisplayName(),
                    p.action, p.eventClassId, rctcEntryId));
        }

        var summary = new RaceHubImportResult.Summary(
                counts[Action.CREATE.ordinal()], counts[Action.UPDATE.ordinal()],
                counts[Action.WITHDRAW.ordinal()], counts[Action.UNCHANGED.ordinal()],
                counts[Action.STALE.ordinal()], counts[Action.SKIP.ordinal()]);
        return new RaceHubImportResult(dryRun, blocked, apply,
                export.event() == null ? null : export.event().name(), export.revision(),
                summary, unmappedClasses, errors, warnings, rows);
    }

    // ── Class mappings ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RaceHubClassMapping> listMappings(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found");
        }
        return mappingRepository.findByEventIdOrderByRacehubEventClassId(eventId);
    }

    /** Replaces the event's class mappings with {@code mappings} (RaceHub event_class_id → event class id). */
    @Transactional
    public List<RaceHubClassMapping> replaceMappings(Long eventId, Map<String, Long> mappings) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found");
        }
        Set<Long> eventClassIds = eventClassRepository.findRefsByEventId(eventId).stream()
                .map(EventClassRef::getId).collect(Collectors.toSet());
        mappings.forEach((racehubId, eventClassId) -> {
            if (racehubId == null || racehubId.isBlank()) {
                throw new IllegalArgumentException("RaceHub event_class_id is required");
            }
            if (!eventClassIds.contains(eventClassId)) {
                throw new IllegalArgumentException("Event class " + eventClassId + " is not in this event");
            }
        });
        mappingRepository.deleteByEventId(eventId);
        mappingRepository.flush();
        Instant now = Instant.now();
        List<RaceHubClassMapping> saved = new ArrayList<>();
        new TreeMap<>(mappings).forEach((racehubId, eventClassId) -> {
            RaceHubClassMapping m = new RaceHubClassMapping();
            m.setEventId(eventId);
            m.setRacehubEventClassId(racehubId);
            m.setEventClassId(eventClassId);
            m.setCreatedAt(now);
            m.setUpdatedAt(now);
            saved.add(mappingRepository.save(m));
        });
        return saved;
    }

    // ── Planning ───────────────────────────────────────────────────────────────────

    private static List<String> validate(ExportEntry row, String label) {
        List<String> problems = new ArrayList<>();
        if (row.entryId() == null || row.entryId().isBlank()) {
            problems.add("An entry has no entry_id");
            return problems;
        }
        if (row.entryVersion() == null) {
            problems.add(label + " has no entry_version");
        }
        if (row.entryStatus() == null || !ENTRY_STATUSES.contains(row.entryStatus())) {
            problems.add(label + " has an unknown entry_status: " + row.entryStatus());
        }
        if (row.raceDayStatus() != null && !RACE_DAY_STATUSES.contains(row.raceDayStatus())) {
            problems.add(label + " has an unknown race_day_status: " + row.raceDayStatus());
        }
        if (row.driverProfileId() == null || row.driverProfileId().isBlank()) {
            problems.add(label + " has no driver_profile_id");
        }
        if (row.eventClassId() == null || row.eventClassId().isBlank()) {
            problems.add(label + " has no event_class_id");
        }
        return problems;
    }

    private Map<String, Competitor> loadCompetitors(List<Planned> plan) {
        Map<String, Competitor> byProfile = new HashMap<>();
        for (Planned p : plan) {
            byProfile.computeIfAbsent(p.row.driverProfileId(), id ->
                    competitorRepository.findByExternalSourceAndExternalId(RACEHUB_SOURCE, id).orElse(null));
        }
        return byProfile;
    }

    /**
     * Looks at the event's entries as they would be after the import. A driver entered twice in
     * the same class blocks the import (the database allows only one active entry). A transponder
     * used by more than one active entry is only a warning: the referee resolves it on the day.
     */
    private void checkFinalState(Long eventId, List<Planned> plan, Map<String, Competitor> competitors,
                                 List<String> errors, List<String> warnings) {
        Map<Long, Planned> touched = new HashMap<>();
        for (Planned p : plan) {
            if (p.existing != null) {
                touched.put(p.existing.getId(), p);
            }
        }

        record Active(String driverKey, String name, Long eventClassId, String primary, String secondary) {}
        List<Active> active = new ArrayList<>();

        List<Entry> current = entryRepository.findByEventId(eventId);
        Map<Long, String> names = competitorRepository.findAllById(current.stream()
                        .map(Entry::getCompetitorId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Competitor::getId, Competitor::getDisplayName));
        for (Entry e : current) {
            if (touched.containsKey(e.getId()) || e.getStatus() == EntryStatus.WITHDRAWN) {
                continue;
            }
            active.add(new Active("c:" + e.getCompetitorId(), names.getOrDefault(e.getCompetitorId(), "entry " + e.getId()),
                    e.getEventClassId(), e.getTransponderNumberSnapshot(), e.getSecondaryTransponderNumber()));
        }
        for (Planned p : plan) {
            boolean activeAfter = switch (p.action) {
                case CREATE, UPDATE -> true;
                case UNCHANGED, STALE -> p.existing.getStatus() != EntryStatus.WITHDRAWN;
                case WITHDRAW, SKIP -> false;
            };
            if (!activeAfter) {
                continue;
            }
            boolean rowWins = p.action == Action.CREATE || p.action == Action.UPDATE;
            Competitor c = competitors.get(p.row.driverProfileId());
            String driverKey = c == null ? "rh:" + p.row.driverProfileId() : "c:" + c.getId();
            String name = p.row.driverDisplayName() != null ? p.row.driverDisplayName() : "entry " + p.row.entryId();
            String primary = rowWins ? primaryOf(p.row) : p.existing.getTransponderNumberSnapshot();
            String secondary = rowWins ? secondaryOf(p.row) : p.existing.getSecondaryTransponderNumber();
            active.add(new Active(driverKey, name, p.eventClassId, primary, secondary));

            if (rowWins && primaryOf(p.row).isEmpty()) {
                warnings.add(name + " (entry " + p.row.entryId() + ") has no transponder");
            }
        }

        Map<String, List<Active>> byDriverClass = active.stream()
                .filter(a -> a.eventClassId() != null)
                .collect(Collectors.groupingBy(a -> a.driverKey() + "/" + a.eventClassId(), LinkedHashMap::new, Collectors.toList()));
        byDriverClass.values().stream().filter(l -> l.size() > 1).forEach(l ->
                errors.add(l.get(0).name() + " has " + l.size() + " active entries in event class " + l.get(0).eventClassId()));

        Map<String, Set<String>> byTransponder = new TreeMap<>();
        for (Active a : active) {
            for (String number : new String[] { a.primary(), a.secondary() }) {
                if (number != null && !number.isBlank()) {
                    byTransponder.computeIfAbsent(number, k -> new LinkedHashSet<>()).add(a.name());
                }
            }
        }
        byTransponder.forEach((number, users) -> {
            long uses = active.stream().filter(a -> number.equals(a.primary()) || number.equals(a.secondary())).count();
            if (uses > 1) {
                warnings.add("Transponder " + number + " is used by more than one entry: " + String.join(", ", users));
            }
        });
    }

    // ── Applying ───────────────────────────────────────────────────────────────────

    private Long applyRow(Long eventId, Planned p, Map<String, Competitor> competitors) {
        ExportEntry row = p.row;
        Instant now = Instant.now();
        switch (p.action) {
            case UNCHANGED, STALE -> {
                return p.existing.getId();
            }
            case SKIP -> {
                return null;
            }
            case WITHDRAW -> {
                Entry e = p.existing;
                e.setStatus(EntryStatus.WITHDRAWN);
                e.setWithdrawnAt(now);
                e.setExternalEntryVersion(row.entryVersion());
                e.setRacehubArrival(row.raceDayStatus());
                e.setUpdatedAt(now);
                return entryRepository.save(e).getId();
            }
            default -> {
                Competitor competitor = upsertCompetitor(row, competitors, now);
                Entry e = p.existing;
                if (e == null) {
                    e = new Entry();
                    e.setEventId(eventId);
                    e.setExternalSource(RACEHUB_SOURCE);
                    e.setExternalEntryId(row.entryId());
                    e.setSubmittedAt(now);
                }
                e.setCompetitorId(competitor.getId());
                e.setEventClassId(p.eventClassId);
                e.setTransponderNumberSnapshot(primaryOf(row));
                e.setSecondaryTransponderNumber(secondaryOf(row));
                if (e.getStatus() != EntryStatus.CONFIRMED) {
                    e.setStatus(EntryStatus.CONFIRMED);
                    e.setConfirmedAt(now);
                    e.setWithdrawnAt(null);
                }
                e.setExternalEntryVersion(row.entryVersion());
                e.setRacehubArrival(row.raceDayStatus());
                e.setUpdatedAt(now);
                return entryRepository.save(e).getId();
            }
        }
    }

    private Competitor upsertCompetitor(ExportEntry row, Map<String, Competitor> competitors, Instant now) {
        Competitor c = competitors.get(row.driverProfileId());
        if (c == null) {
            c = new Competitor();
            c.setExternalSource(RACEHUB_SOURCE);
            c.setExternalId(row.driverProfileId());
            c.setCreatedAt(now);
        }
        String name = row.driverDisplayName();
        c.setDisplayName(name == null || name.isBlank() ? "RaceHub driver " + row.driverProfileId() : name.trim());
        c.setBrcaNumber(blankToNull(row.brcaNumber()));
        c.setHomeClub(blankToNull(row.homeClub()));
        c.setUpdatedAt(now);
        c = competitorRepository.save(c);
        competitors.put(row.driverProfileId(), c);
        return c;
    }

    /** The entry's primary number. With only a secondary given, that becomes the primary. */
    private static String primaryOf(ExportEntry row) {
        String primary = blankToNull(row.primaryTransponder());
        if (primary != null) {
            return primary;
        }
        String secondary = blankToNull(row.secondaryTransponder());
        return secondary != null ? secondary : "";
    }

    private static String secondaryOf(ExportEntry row) {
        return blankToNull(row.primaryTransponder()) == null ? null : blankToNull(row.secondaryTransponder());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private record Planned(ExportEntry row, Action action, Entry existing, Long eventClassId) {}

    private static final class UnmappedAccumulator {
        final String rcClassName;
        final String className;
        int count;

        UnmappedAccumulator(ExportEntry row) {
            this.rcClassName = row.rcClassName();
            this.className = row.className();
        }
    }

    /**
     * Finds the event class for a RaceHub class: a stored mapping first, then the one event class
     * whose racing class name equals {@code rc_class_name}, ignoring case.
     */
    private final class ClassResolver {
        private final Map<String, Long> mapped;
        private final Map<String, List<Long>> byName;

        ClassResolver(Long eventId) {
            mapped = mappingRepository.findByEventIdOrderByRacehubEventClassId(eventId).stream()
                    .collect(Collectors.toMap(RaceHubClassMapping::getRacehubEventClassId,
                            RaceHubClassMapping::getEventClassId));
            List<EventClassRef> eventClasses = eventClassRepository.findRefsByEventId(eventId);
            Map<Long, String> racingClassNames = racingClassRepository.findAllById(eventClasses.stream()
                            .map(EventClassRef::getRacingClassId).filter(Objects::nonNull).collect(Collectors.toSet()))
                    .stream().collect(Collectors.toMap(RacingClass::getId, RacingClass::getName));
            byName = eventClasses.stream()
                    .filter(ec -> racingClassNames.containsKey(ec.getRacingClassId()))
                    .collect(Collectors.groupingBy(ec -> normalise(racingClassNames.get(ec.getRacingClassId())),
                            Collectors.mapping(EventClassRef::getId, Collectors.toList())));
        }

        Optional<Long> resolve(ExportEntry row) {
            Long id = mapped.get(row.eventClassId());
            if (id != null) {
                return Optional.of(id);
            }
            if (row.rcClassName() == null) {
                return Optional.empty();
            }
            List<Long> matches = byName.getOrDefault(normalise(row.rcClassName()), List.of());
            return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
        }

        private static String normalise(String name) {
            return name.trim().toLowerCase(Locale.ROOT);
        }
    }
}
