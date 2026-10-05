package dev.monkeypatch.rctiming.domain.racehub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The published Entry Export v1 schema (#41) must accept every file the import tests use, so the schema and
 * the import can't drift apart.
 */
class EntryExportV1SchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {
            "entries-v1-initial.json",
            "entries-v1-update.json",
            "entries-v1-stale.json",
            "entries-v1-unmapped-class.json",
            "entries-v1-duplicate-transponder.json"})
    void everyImportFixtureSatisfiesTheSchema(String fixture) throws IOException {
        assertThat(validate(fixture(fixture))).isEmpty();
    }

    @Test
    void acceptsAnotherSystemsSource() throws IOException {
        ObjectNode document = fixture("entries-v1-initial.json");
        document.put("source", "OTHER_BOOKING");

        assertThat(validate(document)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"CSV", "other", "", "TOO_LONG_A_SOURCE_NAME_FOR_THIS_FIELD"})
    void rejectsASourceItCannotKeepApart(String source) throws IOException {
        ObjectNode document = fixture("entries-v1-initial.json");
        document.put("source", source);

        assertThat(validate(document)).isNotEmpty();
    }

    @Test
    void rejectsAnEntryWithoutItsId() throws IOException {
        ObjectNode document = fixture("entries-v1-initial.json");
        ((ObjectNode) document.at("/entries/0")).remove("entry_id");

        assertThat(validate(document)).isNotEmpty();
    }

    @Test
    void rejectsAnotherSchemaVersion() throws IOException {
        ObjectNode document = fixture("entries-v1-initial.json");
        document.put("schema_version", 2);

        assertThat(validate(document)).isNotEmpty();
    }

    private ObjectNode fixture(String name) throws IOException {
        String json = new ClassPathResource("racehub/" + name).getContentAsString(StandardCharsets.UTF_8)
                .replace("{{run}}", "1");
        return (ObjectNode) objectMapper.readTree(json);
    }

    private static Set<ValidationMessage> validate(JsonNode document) throws IOException {
        try (InputStream schema = new ClassPathResource("racehub/entry-export-v1.schema.json").getInputStream()) {
            JsonSchema jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schema);
            return jsonSchema.validate(document);
        }
    }
}
