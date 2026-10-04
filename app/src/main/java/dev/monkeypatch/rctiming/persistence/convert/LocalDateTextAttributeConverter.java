package dev.monkeypatch.rctiming.persistence.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/** Stores every {@link LocalDate} entity attribute as ISO-8601 text (YYYY-MM-DD). */
@Converter(autoApply = true)
public class LocalDateTextAttributeConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LocalDate.parse(dbData);
    }
}
