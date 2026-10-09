package dev.monkeypatch.rctiming.domain.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

/** Reads and writes a format's config as a JSON or YAML file, for export and import (FORMAT-14). */
@Component
public class RaceFormatCodec {

    public enum FileType {
        JSON("application/json"),
        YAML("application/yaml");

        private final String mediaType;

        FileType(String mediaType) {
            this.mediaType = mediaType;
        }

        public String mediaType() {
            return mediaType;
        }

        /** YAML when the media type names it, JSON otherwise. */
        public static FileType of(String mediaType) {
            return mediaType != null && mediaType.contains("yaml") ? YAML : JSON;
        }
    }

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    private final ObjectMapper jsonMapper;

    public RaceFormatCodec(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String write(RaceFormatConfig config, FileType fileType) {
        try {
            return mapper(fileType).writerWithDefaultPrettyPrinter().writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write the format config", e);
        }
    }

    /** @throws IllegalArgumentException when the text is not a format config */
    public RaceFormatConfig read(String text, FileType fileType) {
        try {
            RaceFormatConfig config = mapper(fileType).readValue(text, RaceFormatConfig.class);
            if (config == null) {
                throw new IllegalArgumentException("The format file is empty");
            }
            return config;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse format config: " + e.getOriginalMessage(), e);
        }
    }

    private ObjectMapper mapper(FileType fileType) {
        return fileType == FileType.YAML ? YAML_MAPPER : jsonMapper;
    }
}
