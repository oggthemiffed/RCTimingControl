package dev.monkeypatch.rctiming.persistence;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that a jOOQ repository (#69) saves and loads every field of its entity: the entity is
 * inserted, read back, changed, updated and read back again.
 */
public final class RoundTrip {

    private RoundTrip() {
    }

    /**
     * @param repository the repository under test
     * @param entity     an unsaved entity with every field set to a non-default value
     * @param change     changes every saved field of a loaded entity to another value
     * @param id         the entity's id
     * @param ignoring   fields that aren't saved, such as ones filled in on load
     */
    public static <E> E assertSavedAndReloaded(JooqRepository<E, ?> repository, E entity,
                                               Function<E, E> change, Function<E, Long> id,
                                               String... ignoring) {
        E inserted = repository.save(entity);
        Long entityId = id.apply(inserted);
        assertThat(entityId).as("id set on insert").isNotNull();
        E loaded = repository.findById(entityId).orElseThrow();
        assertThat(loaded).usingRecursiveComparison().ignoringFields(ignoring).isEqualTo(inserted);

        E changed = change.apply(loaded);
        repository.save(changed);
        E reloaded = repository.findById(entityId).orElseThrow();
        assertThat(reloaded).usingRecursiveComparison().ignoringFields(ignoring).isEqualTo(changed);
        assertThat(reloaded).usingRecursiveComparison().ignoringFields(ignoring).isNotEqualTo(inserted);
        return reloaded;
    }
}
