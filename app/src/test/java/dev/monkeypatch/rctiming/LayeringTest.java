package dev.monkeypatch.rctiming;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the layers pointing one way (#143): the domain, query and service packages never use a type from
 * {@code api}. Services take plain values and return entities or their own records, and query classes return
 * projections from their own packages; controllers map those to API DTOs.
 */
class LayeringTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java/dev/monkeypatch/rctiming");
    private static final List<String> LOWER_LAYERS = List.of("domain", "query", "service");
    private static final Pattern API_REFERENCE = Pattern.compile("\\bdev\\.monkeypatch\\.rctiming\\.api\\.");

    @Test
    void lowerLayersDoNotUseApiTypes() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String layer : LOWER_LAYERS) {
            try (Stream<Path> files = Files.walk(MAIN_SOURCES.resolve(layer))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    violations.addAll(violations(MAIN_SOURCES.relativize(file).toString(), Files.readString(file)));
                }
            }
        }
        assertThat(violations).as("domain, query or service code using an api type").isEmpty();
    }

    @Test
    void anImportOrAFullyQualifiedNameIsCaught() {
        assertThat(violations("Example.java", "import dev.monkeypatch.rctiming.api.admin.dto.EventDto;\n"))
                .singleElement().asString().contains("Example.java:1");
        assertThat(violations("Example.java", "class Example {\n    dev.monkeypatch.rctiming.api.Foo foo;\n}\n"))
                .singleElement().asString().contains("Example.java:2");
        assertThat(violations("Example.java", "import dev.monkeypatch.rctiming.domain.event.Event;\n")).isEmpty();
    }

    private static List<String> violations(String file, String source) {
        List<String> found = new ArrayList<>();
        Matcher matcher = API_REFERENCE.matcher(source);
        while (matcher.find()) {
            int line = 1 + (int) source.substring(0, matcher.start()).chars().filter(c -> c == '\n').count();
            found.add(file + ":" + line);
        }
        return found;
    }
}
