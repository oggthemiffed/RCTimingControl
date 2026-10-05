package dev.monkeypatch.rctiming.domain.csvimport;

import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import dev.monkeypatch.rctiming.domain.competitor.Competitor;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorRepository;
import dev.monkeypatch.rctiming.domain.csvimport.CsvImportResult.Change;
import dev.monkeypatch.rctiming.domain.csvimport.CsvImportResult.Group;
import dev.monkeypatch.rctiming.domain.csvimport.CsvImportResult.UnmappedClass;
import dev.monkeypatch.rctiming.domain.csvimport.RcTimingCsvParser.Kind;
import dev.monkeypatch.rctiming.domain.csvimport.RcTimingCsvParser.ParsedCsv;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository.EventClassRef;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.racehub.RaceHubClassMapping;
import dev.monkeypatch.rctiming.domain.racehub.RaceHubClassMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
 * Imports an RC-Timing style driver CSV into an event (#39), as a lossy adapter beside the RaceHub
 * import. The file has no entry ids, versions or withdrawals, so nothing is applied on trust:
 * <ul>
 *   <li>Each booked row gets a stable key from its BRCA number and class, or its name and class when
 *       the BRCA number is 0. Competitors are matched by the BRCA number, or the name.</li>
 *   <li>The preview sorts rows into new, changed (with old and new values), unchanged and skipped
 *       ({@code update} rows), and lists the entries an earlier CSV import made that this file
 *       leaves out as missing.</li>
 *   <li>On confirm, new rows are created, and only the changed rows and missing entries the
 *       official picked are updated or withdrawn. Withdrawing never deletes.</li>
 *   <li>Only CSV-sourced entries can be missing, so walk-ins and RaceHub entries are never
 *       withdrawn by a CSV.</li>
 * </ul>
 * Classes are placed through the event's class mappings (keyed {@code CSV:<class name>}), then by
 * racing class name, then, for a row with only a Class Number, by the event's Nth class.
 */
@Service
public class CsvImportService {

    public static final String CSV_SOURCE = "CSV";

    private final EventRepository eventRepository;
    private final EventClassRepository eventClassRepository;
    private final RacingClassRepository racingClassRepository;
    private final RaceHubClassMappingRepository mappingRepository;
    private final EntryRepository entryRepository;
    private final CompetitorRepository competitorRepository;

    public CsvImportService(EventRepository eventRepository,
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

    /** What the official picked in the preview: changed rows to update (by key), missing entries to withdraw. */
    public record Selection(Set<String> update, Set<Long> withdraw) {
        public static final Selection NONE = new Selection(Set.of(), Set.of());

        public Selection {
            update = update == null ? Set.of() : Set.copyOf(update);
            withdraw = withdraw == null ? Set.of() : Set.copyOf(withdraw);
        }
    }

    @Transactional
    public CsvImportResult importCsv(Long eventId, String content, boolean dryRun, Selection selection) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found");
        }
        Selection picked = selection == null ? Selection.NONE : selection;
        ParsedCsv parsed = RcTimingCsvParser.parse(content);
        List<String> errors = new ArrayList<>(parsed.errors());
        List<String> warnings = new ArrayList<>(parsed.warnings());

        Classes classes = new Classes(eventId);
        Map<String, UnmappedAccumulator> unmapped = new TreeMap<>();
        Map<String, Competitor> competitors = new HashMap<>();
        Map<String, Integer> lineByKey = new HashMap<>();
        List<Planned> plan = new ArrayList<>();
        List<CsvImportResult.Row> skipped = new ArrayList<>();

        for (RcTimingCsvParser.Row row : parsed.rows()) {
            if (row.kind() == Kind.UPDATE) {
                skipped.add(new CsvImportResult.Row(Group.SKIPPED, null, row.line(), row.name(), row.brcaNumber(),
                        row.className(), row.classNumber(), null, null, text(row.primaryTransponder()),
                        text(row.secondaryTransponder()), List.of(), row.info(), false,
                        "Entry Desc is update, which only changes RC-Timing's member archive, so this row is not booked in"));
                continue;
            }
            String key = entryKey(eventId, row);
            Integer firstLine = lineByKey.putIfAbsent(key, row.line());
            if (firstLine != null) {
                errors.add("Line " + row.line() + " books " + row.name() + " into " + classLabel(row)
                        + " again (line " + firstLine + ")");
                continue;
            }
            Optional<Long> eventClassId = classes.resolve(row);
            if (eventClassId.isEmpty()) {
                unmapped.computeIfAbsent(classes.mappingKey(row), k -> new UnmappedAccumulator(row)).count++;
            }
            String competitorKey = competitorKey(row);
            Competitor competitor = competitors.computeIfAbsent(competitorKey, k ->
                    competitorRepository.findByExternalSourceAndExternalId(CSV_SOURCE, k).orElse(null));
            Entry existing = entryRepository.findByExternalSourceAndExternalEntryId(CSV_SOURCE, key).orElse(null);
            plan.add(new Planned(key, row, competitorKey, eventClassId.orElse(null), existing,
                    existing == null ? List.of() : changes(existing, row, eventClassId.orElse(null), classes)));
        }

        List<Entry> missing = entryRepository.findByEventId(eventId).stream()
                .filter(e -> CSV_SOURCE.equals(e.getExternalSource()) && e.getStatus() != EntryStatus.WITHDRAWN)
                .filter(e -> !lineByKey.containsKey(e.getExternalEntryId()))
                .toList();

        checkSelection(picked, plan, missing, errors);
        checkFinalState(eventId, plan, picked, missing, competitors, classes, errors, warnings);

        List<UnmappedClass> unmappedClasses = unmapped.entrySet().stream()
                .map(e -> new UnmappedClass(e.getKey(), e.getValue().className, e.getValue().classNumber, e.getValue().count))
                .toList();
        boolean blocked = !errors.isEmpty() || !unmappedClasses.isEmpty();
        boolean apply = !dryRun && !blocked;

        Map<Long, String> competitorNames = names(missing);
        Instant now = Instant.now();
        int created = 0;
        int updated = 0;
        int withdrawn = 0;
        List<CsvImportResult.Row> rows = new ArrayList<>();
        // Withdrawals first, so a replacement entry in the same class doesn't meet the old one in the
        // one-active-entry index.
        for (Entry e : missing) {
            boolean withdraw = apply && picked.withdraw().contains(e.getId());
            if (withdraw) {
                e.setStatus(EntryStatus.WITHDRAWN);
                e.setWithdrawnAt(now);
                e.setUpdatedAt(now);
                entryRepository.save(e);
                withdrawn++;
            }
            rows.add(new CsvImportResult.Row(Group.MISSING, null, null,
                    competitorNames.getOrDefault(e.getCompetitorId(), "entry " + e.getId()), null,
                    classes.name(e.getEventClassId()), null, e.getEventClassId(), e.getId(),
                    e.getTransponderNumberSnapshot(), e.getSecondaryTransponderNumber(), List.of(), Map.of(),
                    withdraw, null));
        }
        if (withdrawn > 0) {
            entryRepository.flush();
        }
        for (Planned p : plan) {
            Group group = p.group();
            boolean write = apply && (group == Group.NEW || group == Group.CHANGED && picked.update().contains(p.key));
            Long entryId = p.existing == null ? null : p.existing.getId();
            if (write) {
                entryId = save(eventId, p, competitors, now).getId();
                if (group == Group.NEW) {
                    created++;
                } else {
                    updated++;
                }
            }
            rows.add(new CsvImportResult.Row(group, p.key, p.row.line(), p.row.name(), p.row.brcaNumber(),
                    p.row.className(), p.row.classNumber(), p.eventClassId, entryId, primaryOf(p.row),
                    secondaryOf(p.row), p.changes, p.row.info(), write, null));
        }
        rows.addAll(skipped);

        Map<Group, Long> counts = rows.stream().collect(Collectors.groupingBy(CsvImportResult.Row::group, Collectors.counting()));
        var summary = new CsvImportResult.Summary(count(counts, Group.NEW), count(counts, Group.CHANGED),
                count(counts, Group.UNCHANGED), count(counts, Group.MISSING), count(counts, Group.SKIPPED),
                created, updated, withdrawn);
        return new CsvImportResult(dryRun, blocked, apply, summary, unmappedClasses, errors, warnings, rows);
    }

    // ── Planning ───────────────────────────────────────────────────────────────────

    /**
     * The entry's key, unique across events because the entries index is: the event, then the BRCA
     * number (or the name when it is 0), then the class as the file names it.
     */
    static String entryKey(Long eventId, RcTimingCsvParser.Row row) {
        String classPart = row.className() != null ? "class:" + normalise(row.className()) : "class#" + row.classNumber();
        return eventId + "/" + competitorKey(row) + "/" + classPart;
    }

    static String competitorKey(RcTimingCsvParser.Row row) {
        return row.brcaNumber() != null ? "brca:" + row.brcaNumber() : "name:" + normalise(row.name());
    }

    private static List<Change> changes(Entry existing, RcTimingCsvParser.Row row, Long eventClassId, Classes classes) {
        List<Change> changes = new ArrayList<>();
        if (existing.getStatus() == EntryStatus.WITHDRAWN) {
            changes.add(new Change("Status", "Withdrawn", "Entered"));
        }
        // Only a BRCA-keyed row can rename its driver: a name-keyed row with another name is a new key
        String name = classes.competitorName(existing.getCompetitorId());
        if (name != null && !name.equals(row.name())) {
            changes.add(new Change("Name", name, row.name()));
        }
        if (eventClassId != null && !eventClassId.equals(existing.getEventClassId())) {
            changes.add(new Change("Class", classes.name(existing.getEventClassId()), classes.name(eventClassId)));
        }
        if (!Objects.equals(blankToNull(existing.getTransponderNumberSnapshot()), blankToNull(primaryOf(row)))) {
            changes.add(new Change("Transponder", existing.getTransponderNumberSnapshot(), primaryOf(row)));
        }
        if (!Objects.equals(blankToNull(existing.getSecondaryTransponderNumber()), secondaryOf(row))) {
            changes.add(new Change("Second transponder", existing.getSecondaryTransponderNumber(), secondaryOf(row)));
        }
        return changes;
    }

    /** A pick that doesn't match this file means the preview is out of date, so nothing is applied. */
    private static void checkSelection(Selection picked, List<Planned> plan, List<Entry> missing, List<String> errors) {
        Set<String> changedKeys = plan.stream().filter(p -> p.group() == Group.CHANGED).map(p -> p.key).collect(Collectors.toSet());
        Set<Long> missingIds = missing.stream().map(Entry::getId).collect(Collectors.toSet());
        long staleUpdates = picked.update().stream().filter(k -> !changedKeys.contains(k)).count();
        long staleWithdrawals = picked.withdraw().stream().filter(id -> !missingIds.contains(id)).count();
        if (staleUpdates + staleWithdrawals > 0) {
            errors.add((staleUpdates + staleWithdrawals) + " of the picked entries no longer match this file or the "
                    + "event's entries. Preview the file again and pick again");
        }
    }

    /**
     * Looks at the event's entries as they would be after the import, from every source. A driver
     * entered twice in one class blocks the import (one active entry each); a transponder used by
     * more than one active entry is only a warning, as the referee resolves it on the day.
     */
    private void checkFinalState(Long eventId, List<Planned> plan, Selection picked, List<Entry> missing,
                                 Map<String, Competitor> competitors, Classes classes,
                                 List<String> errors, List<String> warnings) {
        record Active(String driverKey, String name, Long eventClassId, String primary, String secondary) {}
        Set<Long> touched = new HashSet<>();
        plan.stream().filter(p -> p.existing != null).forEach(p -> touched.add(p.existing.getId()));
        missing.stream().filter(e -> picked.withdraw().contains(e.getId())).forEach(e -> touched.add(e.getId()));

        List<Active> active = new ArrayList<>();
        List<Entry> current = entryRepository.findByEventId(eventId);
        Map<Long, String> names = names(current);
        for (Entry e : current) {
            if (!touched.contains(e.getId()) && e.getStatus() != EntryStatus.WITHDRAWN) {
                active.add(new Active("c:" + e.getCompetitorId(), names.getOrDefault(e.getCompetitorId(), "entry " + e.getId()),
                        e.getEventClassId(), e.getTransponderNumberSnapshot(), e.getSecondaryTransponderNumber()));
            }
        }
        for (Planned p : plan) {
            boolean fromFile = p.group() == Group.NEW || p.group() == Group.CHANGED && picked.update().contains(p.key);
            if (!fromFile && p.existing.getStatus() == EntryStatus.WITHDRAWN) {
                continue;
            }
            Competitor c = competitors.get(p.competitorKey);
            String driverKey = c == null ? "csv:" + p.competitorKey : "c:" + c.getId();
            Long eventClassId = fromFile ? p.eventClassId : p.existing.getEventClassId();
            active.add(fromFile
                    ? new Active(driverKey, p.row.name(), eventClassId, primaryOf(p.row), secondaryOf(p.row))
                    : new Active(driverKey, p.row.name(), eventClassId, p.existing.getTransponderNumberSnapshot(),
                            p.existing.getSecondaryTransponderNumber()));
            if (p.group() == Group.NEW && primaryOf(p.row).isEmpty()) {
                warnings.add(p.row.name() + " (line " + p.row.line() + ") has no transponder");
            }
        }

        active.stream().filter(a -> a.eventClassId() != null)
                .collect(Collectors.groupingBy(a -> a.driverKey() + "/" + a.eventClassId(), LinkedHashMap::new, Collectors.toList()))
                .values().stream().filter(l -> l.size() > 1)
                .forEach(l -> errors.add(l.get(0).name() + " would have " + l.size() + " entries in "
                        + classes.name(l.get(0).eventClassId())));

        Map<String, Set<String>> byTransponder = new TreeMap<>();
        Map<String, Integer> uses = new HashMap<>();
        for (Active a : active) {
            for (String number : new LinkedHashSet<>(Arrays.asList(a.primary(), a.secondary()))) {
                if (number != null && !number.isBlank()) {
                    byTransponder.computeIfAbsent(number, k -> new LinkedHashSet<>()).add(a.name());
                    uses.merge(number, 1, Integer::sum);
                }
            }
        }
        byTransponder.forEach((number, users) -> {
            if (uses.get(number) > 1) {
                warnings.add("Transponder " + number + " is used by more than one entry: " + String.join(", ", users));
            }
        });
    }

    // ── Applying ───────────────────────────────────────────────────────────────────

    private Entry save(Long eventId, Planned p, Map<String, Competitor> competitors, Instant now) {
        Competitor competitor = competitors.get(p.competitorKey);
        if (competitor == null) {
            competitor = new Competitor();
            competitor.setExternalSource(CSV_SOURCE);
            competitor.setExternalId(p.competitorKey);
            competitor.setCreatedAt(now);
        }
        if (!p.row.name().equals(competitor.getDisplayName())
                || !Objects.equals(text(p.row.brcaNumber()), competitor.getBrcaNumber())) {
            competitor.setDisplayName(p.row.name());
            competitor.setBrcaNumber(text(p.row.brcaNumber()));
            competitor.setUpdatedAt(now);
            competitor = competitorRepository.save(competitor);
        }
        competitors.put(p.competitorKey, competitor);

        Entry e = p.existing;
        if (e == null) {
            e = new Entry();
            e.setEventId(eventId);
            e.setExternalSource(CSV_SOURCE);
            e.setExternalEntryId(p.key);
            e.setSubmittedAt(now);
        }
        e.setCompetitorId(competitor.getId());
        e.setEventClassId(p.eventClassId);
        e.setTransponderNumberSnapshot(primaryOf(p.row));
        e.setSecondaryTransponderNumber(secondaryOf(p.row));
        if (e.getStatus() != EntryStatus.CONFIRMED) {
            e.setStatus(EntryStatus.CONFIRMED);
            e.setConfirmedAt(now);
            e.setWithdrawnAt(null);
        }
        e.setUpdatedAt(now);
        return entryRepository.save(e);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────────

    /** The entry's primary number. With only {@code PT No 2} given, that becomes the primary. */
    private static String primaryOf(RcTimingCsvParser.Row row) {
        Long primary = row.primaryTransponder() != null ? row.primaryTransponder() : row.secondaryTransponder();
        return primary == null ? "" : primary.toString();
    }

    private static String secondaryOf(RcTimingCsvParser.Row row) {
        return row.primaryTransponder() == null ? null : text(row.secondaryTransponder());
    }

    private Map<Long, String> names(Collection<Entry> entries) {
        return competitorRepository.findAllById(entries.stream().map(Entry::getCompetitorId)
                        .filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Competitor::getId, Competitor::getDisplayName));
    }

    private static String classLabel(RcTimingCsvParser.Row row) {
        return row.className() != null ? row.className() : "class " + row.classNumber();
    }

    private static int count(Map<Group, Long> counts, Group group) {
        return counts.getOrDefault(group, 0L).intValue();
    }

    private static String text(Long number) {
        return number == null ? null : number.toString();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static String normalise(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private record Planned(String key, RcTimingCsvParser.Row row, String competitorKey, Long eventClassId,
                           Entry existing, List<Change> changes) {
        Group group() {
            if (existing == null) {
                return Group.NEW;
            }
            return changes.isEmpty() ? Group.UNCHANGED : Group.CHANGED;
        }
    }

    private static final class UnmappedAccumulator {
        final String className;
        final Integer classNumber;
        int count;

        UnmappedAccumulator(RcTimingCsvParser.Row row) {
            this.className = row.className();
            this.classNumber = row.classNumber();
        }
    }

    /** The event's classes: placing a row, and naming a class or competitor in the preview. */
    private final class Classes {
        private final Map<String, Long> mapped;
        private final List<Long> inOrder;
        private final Map<String, List<Long>> byName;
        private final Map<Long, String> names = new HashMap<>();
        private final Map<Long, String> competitorNames = new HashMap<>();

        Classes(Long eventId) {
            mapped = mappingRepository.findByEventIdOrderByRacehubEventClassId(eventId).stream()
                    .collect(Collectors.toMap(RaceHubClassMapping::getRacehubEventClassId, RaceHubClassMapping::getEventClassId));
            List<EventClassRef> eventClasses = eventClassRepository.findRefsByEventId(eventId);
            inOrder = eventClasses.stream().map(EventClassRef::getId).toList();
            Map<Long, String> racingClassNames = racingClassRepository.findAllById(eventClasses.stream()
                            .map(EventClassRef::getRacingClassId).filter(Objects::nonNull).collect(Collectors.toSet()))
                    .stream().collect(Collectors.toMap(RacingClass::getId, RacingClass::getName));
            eventClasses.forEach(ec -> names.put(ec.getId(), racingClassNames.get(ec.getRacingClassId())));
            byName = eventClasses.stream()
                    .filter(ec -> racingClassNames.containsKey(ec.getRacingClassId()))
                    .collect(Collectors.groupingBy(ec -> normalise(racingClassNames.get(ec.getRacingClassId())),
                            Collectors.mapping(EventClassRef::getId, Collectors.toList())));
        }

        String mappingKey(RcTimingCsvParser.Row row) {
            return row.className() != null ? "CSV:" + normalise(row.className()) : "CSV:#" + row.classNumber();
        }

        /**
         * A stored mapping first, then the one event class whose racing class has the file's class
         * name. Class Number places a row only when it has no class name, so a misspelt name is
         * never put in the wrong class: it is reported for mapping instead.
         */
        Optional<Long> resolve(RcTimingCsvParser.Row row) {
            Long id = mapped.get(mappingKey(row));
            if (id != null) {
                return Optional.of(id);
            }
            if (row.className() != null) {
                List<Long> matches = byName.getOrDefault(normalise(row.className()), List.of());
                return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
            }
            int position = row.classNumber();
            return position >= 1 && position <= inOrder.size() ? Optional.of(inOrder.get(position - 1)) : Optional.empty();
        }

        String name(Long eventClassId) {
            return eventClassId == null ? null : names.getOrDefault(eventClassId, "event class " + eventClassId);
        }

        String competitorName(Long competitorId) {
            if (competitorId == null) {
                return null;
            }
            return competitorNames.computeIfAbsent(competitorId, id ->
                    competitorRepository.findById(id).map(Competitor::getDisplayName).orElse(null));
        }
    }
}
