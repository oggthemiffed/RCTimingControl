package dev.monkeypatch.rctiming.domain.entryimport;

import dev.monkeypatch.rctiming.domain.Names;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An event's classes as the entry imports place rows in them: the class mappings an official set
 * up, the classes in order and by racing class name. Loaded once per import by {@link EntryImports}.
 */
public final class EventClasses {

    private final Map<String, Long> mapped;
    private final List<Long> inOrder;
    private final Map<String, List<Long>> byName;
    private final Map<Long, String> names;

    EventClasses(Map<String, Long> mapped, List<Long> inOrder, Map<String, List<Long>> byName, Map<Long, String> names) {
        this.mapped = mapped;
        this.inOrder = inOrder;
        this.byName = byName;
        this.names = names;
    }

    /** The event class a stored mapping puts {@code mappingKey} in. */
    public Optional<Long> mapped(String mappingKey) {
        return Optional.ofNullable(mapped.get(mappingKey));
    }

    /** The one event class whose racing class has this name ({@link Names#matchKey}); none when several do. */
    public Optional<Long> byName(String className) {
        List<Long> matches = byName.getOrDefault(Names.matchKey(className), List.of());
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    /** The event's class at a 1-based position, in the order the event lists them. */
    public Optional<Long> atPosition(int position) {
        return position >= 1 && position <= inOrder.size() ? Optional.of(inOrder.get(position - 1)) : Optional.empty();
    }

    /** The class's racing class name, for messages. */
    public String name(Long eventClassId) {
        if (eventClassId == null) {
            return null;
        }
        String name = names.get(eventClassId);
        return name != null ? name : "event class " + eventClassId;
    }
}
