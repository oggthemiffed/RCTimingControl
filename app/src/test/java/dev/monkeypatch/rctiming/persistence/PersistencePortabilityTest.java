package dev.monkeypatch.rctiming.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the database swappable (#26): only {@code DatabaseConfig} and the
 * {@code persistence/vendor/} package may know which database the app runs on. Everywhere else,
 * reads go through the jOOQ DSL and writes through JPA, with no vendor classes and no SQL strings.
 */
class PersistencePortabilityTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");
    private static final Path ALLOWED_PACKAGE = Path.of("dev/monkeypatch/rctiming/persistence/vendor");
    private static final Path ALLOWED_CONFIG = Path.of("dev/monkeypatch/rctiming/persistence/DatabaseConfig.java");

    private static final Map<String, Pattern> RULES = Map.of(
            "imports a database driver or dialect",
            Pattern.compile("import\\s+(static\\s+)?(org\\.sqlite|org\\.postgresql|org\\.hibernate\\.community\\.dialect"
                    + "|org\\.hibernate\\.dialect|org\\.jooq\\.SQLDialect)\\b"),
            "names a jOOQ dialect",
            Pattern.compile("\\bSQLDialect\\."),
            "uses a native JPA query",
            Pattern.compile("nativeQuery\\s*=\\s*true|createNativeQuery\\s*\\("),
            "writes column DDL in Java",
            Pattern.compile("columnDefinition\\s*="),
            "uses jOOQ plain SQL",
            Pattern.compile("\\b(sql|field|condition|table|query|resultQuery|fetch|fetchOne|fetchLazy|fetchMany|execute"
                    + "|where|and|or|having|on|orderBy|groupBy)\\(\\s*\""),
            "runs SQL through JDBC directly",
            Pattern.compile("import\\s+org\\.springframework\\.jdbc\\.core\\.|\\b(prepareStatement|createStatement)\\s*\\("));

    @Test
    void mainSourcesKeepVendorDetailsInsideTheSeam() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Path relative = MAIN_SOURCES.relativize(file);
                if (relative.startsWith(ALLOWED_PACKAGE) || relative.equals(ALLOWED_CONFIG)) {
                    continue;
                }
                violations.addAll(violations(relative.toString(), Files.readString(file)));
            }
        }
        assertThat(violations).as("vendor-specific persistence code outside the database seam").isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "import org.sqlite.SQLiteConfig;",
            "import org.postgresql.util.PGobject;",
            "import org.hibernate.community.dialect.SQLiteDialect;",
            "import org.jooq.SQLDialect;",
            "DSL.using(connection, SQLDialect.SQLITE);",
            "@Query(value = \"select 1\", nativeQuery = true)",
            "entityManager.createNativeQuery(\"select 1\");",
            "@Column(columnDefinition = \"jsonb\")",
            "dsl.select(DSL.field(\"now()\")).fetch();",
            "dsl.fetch(\"select * from races\");",
            "dsl.selectFrom(RACES).where(\"status = 'RUNNING'\");",
            "import org.springframework.jdbc.core.JdbcTemplate;",
            "connection.prepareStatement(\"select 1\");"
    })
    void aDeliberateViolationIsCaught(String line) {
        assertThat(violations("Example.java", "class Example {\n    " + line + "\n}\n")).isNotEmpty();
    }

    @Test
    void theJooqDslAndJpqlAreAllowed() {
        String source = """
                import static dev.monkeypatch.rctiming.jooq.generated.Tables.RACES;
                class Example {
                    // a comment may mention nativeQuery = true or field("x")
                    @Query("SELECT r FROM Race r WHERE r.id = :id")
                    Object find() {
                        return dsl.select(RACES.ID, DSL.coalesce(RACES.NAME, DSL.val("Unknown")).as("name"))
                                .from(RACES).where(RACES.ID.eq(1L)).fetch();
                    }
                }
                """;
        assertThat(violations("Example.java", source)).isEmpty();
    }

    private static List<String> violations(String file, String source) {
        String code = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(source).replaceAll("")
                .replaceAll("(?m)^\\s*//.*$", "");
        List<String> found = new ArrayList<>();
        String[] lines = code.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            for (Map.Entry<String, Pattern> rule : RULES.entrySet()) {
                if (rule.getValue().matcher(lines[i]).find()) {
                    found.add(file + " " + rule.getKey() + ": " + lines[i].trim());
                }
            }
        }
        return found;
    }
}
