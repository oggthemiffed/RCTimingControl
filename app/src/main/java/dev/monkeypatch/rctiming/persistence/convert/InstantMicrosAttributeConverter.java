package dev.monkeypatch.rctiming.persistence.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;

/** Stores every {@link Instant} entity attribute as UTC microseconds. */
@Converter(autoApply = true)
public class InstantMicrosAttributeConverter implements AttributeConverter<Instant, Long> {

    @Override
    public Long convertToDatabaseColumn(Instant attribute) {
        return InstantMicros.toMicros(attribute);
    }

    @Override
    public Instant convertToEntityAttribute(Long dbData) {
        return InstantMicros.fromMicros(dbData);
    }
}
