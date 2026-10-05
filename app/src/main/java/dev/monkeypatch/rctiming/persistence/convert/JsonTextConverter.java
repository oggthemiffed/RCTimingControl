package dev.monkeypatch.rctiming.persistence.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Turns a value into JSON text for a column and back. Subclass it per value type; the jOOQ
 * repositories use the subclass to map that column.
 */
public abstract class JsonTextConverter<T> {

    private static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final TypeReference<T> type;

    protected JsonTextConverter(TypeReference<T> type) {
        this.type = type;
    }

    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't write " + attribute.getClass().getSimpleName() + " as JSON", e);
        }
    }

    public T convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't read stored JSON as " + type.getType(), e);
        }
    }
}
