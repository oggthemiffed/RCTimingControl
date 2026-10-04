package dev.monkeypatch.rctiming.domain.format;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.monkeypatch.rctiming.persistence.convert.JsonTextConverter;

/** Stores a race format config as JSON, keeping its {@code type} discriminator. */
public class RaceFormatConfigConverter extends JsonTextConverter<RaceFormatConfig> {

    public RaceFormatConfigConverter() {
        super(new TypeReference<>() {});
    }
}
