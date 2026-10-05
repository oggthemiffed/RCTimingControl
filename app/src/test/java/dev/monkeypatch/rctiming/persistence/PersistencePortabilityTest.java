package dev.monkeypatch.rctiming.persistence;

import dev.monkeypatch.rctiming.RcTimingApplication;
import org.jooq.PlainSQL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the database swappable (#26): only {@code DatabaseConfig} and the
 * {@code persistence/vendor/} package may know which database the app runs on. Everywhere else,
 * reads and writes go through the jOOQ DSL, with no vendor classes and no SQL strings.
 *
 * <p>Two checks: the sources are scanned for vendor imports, native queries and SQL literals, and
 * the compiled classes are scanned for any call to a jOOQ method marked {@link PlainSQL}, which
 * also catches SQL passed in a variable.
 */
class PersistencePortabilityTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");
    private static final Path ALLOWED_PACKAGE = Path.of("dev/monkeypatch/rctiming/persistence/vendor");
    private static final Path ALLOWED_CONFIG = Path.of("dev/monkeypatch/rctiming/persistence/DatabaseConfig.java");
    /** Generated from the schema by jOOQ codegen, so it is per database by nature. */
    private static final Path GENERATED_PACKAGE = Path.of("dev/monkeypatch/rctiming/jooq/generated");

    private static final Map<String, Pattern> RULES = Map.of(
            "imports a database driver or dialect",
            Pattern.compile("import\\s+(static\\s+)?(org\\.sqlite|org\\.postgresql|org\\.jooq\\.SQLDialect)\\b"),
            "names a jOOQ dialect",
            Pattern.compile("\\bSQLDialect\\."),
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

    @Test
    void compiledMainClassesMakeNoPlainSqlCalls() throws IOException, URISyntaxException {
        Path classes = Path.of(RcTimingApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(classes)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                Path relative = classes.relativize(file);
                if (relative.startsWith(ALLOWED_PACKAGE) || relative.startsWith(GENERATED_PACKAGE)
                        || relative.toString().startsWith("dev/monkeypatch/rctiming/persistence/DatabaseConfig")) {
                    continue;
                }
                try (InputStream in = Files.newInputStream(file)) {
                    violations.addAll(plainSqlCalls(new ClassReader(in)));
                }
            }
        }
        assertThat(violations).as("jOOQ plain-SQL calls outside the database seam").isEmpty();
    }

    @Test
    void plainSqlInAVariableOrOverSeveralLinesIsCaught() throws IOException {
        try (InputStream in = PlainSqlExamples.class.getResourceAsStream("PlainSqlExamples.class")) {
            List<String> found = plainSqlCalls(new ClassReader(in));
            assertThat(found).anyMatch(v -> v.contains("fromVariable") && v.contains("fetch"));
            assertThat(found).anyMatch(v -> v.contains("acrossLines") && v.contains("where"));
            assertThat(found).anyMatch(v -> v.contains("plainField") && v.contains("field"));
            assertThat(found).noneMatch(v -> v.contains("dslOnly"));
        }
    }

    @Test
    void aStringLiteralOnTheNextLineIsCaught() {
        String source = """
                class Example {
                    Object find() {
                        return dsl.fetch(
                                "select * from races");
                    }
                }
                """;
        assertThat(violations("Example.java", source)).singleElement().asString().contains("Example.java:3");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "import org.sqlite.SQLiteConfig;",
            "import org.postgresql.util.PGobject;",
            "import org.jooq.SQLDialect;",
            "DSL.using(connection, SQLDialect.SQLITE);",
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
    void theJooqDslIsAllowed() {
        String source = """
                import static dev.monkeypatch.rctiming.jooq.generated.Tables.RACES;
                class Example {
                    // a comment may mention field("x")
                    Object find() {
                        return dsl.select(RACES.ID, DSL.coalesce(RACES.NAME, DSL.val("Unknown")).as("name"))
                                .from(RACES).where(RACES.ID.eq(1L)).fetch();
                    }
                }
                """;
        assertThat(violations("Example.java", source)).isEmpty();
    }

    /** Matches each rule against the whole file, so a call split over several lines is still seen. */
    private static List<String> violations(String file, String source) {
        // Blank out comments but keep their line breaks, so reported line numbers stay right
        String code = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(source)
                .replaceAll(m -> m.group().replaceAll("[^\n]", ""))
                .replaceAll("(?m)^\\s*//.*$", "");
        List<String> found = new ArrayList<>();
        for (Map.Entry<String, Pattern> rule : RULES.entrySet()) {
            Matcher matcher = rule.getValue().matcher(code);
            while (matcher.find()) {
                int line = 1 + (int) code.substring(0, matcher.start()).chars().filter(c -> c == '\n').count();
                found.add(file + ":" + line + " " + rule.getKey() + ": " + matcher.group().replaceAll("\\s+", " "));
            }
        }
        return found;
    }

    /** Every call in the class to a jOOQ method annotated {@link PlainSQL}. */
    private static List<String> plainSqlCalls(ClassReader reader) {
        List<String> found = new ArrayList<>();
        String className = reader.getClassName().replace('/', '.');
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String caller, String callerDesc, String signature,
                                             String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                        if (owner.startsWith("org/jooq/") && isPlainSql(owner, name, desc)) {
                            found.add(className + "." + caller + " calls plain SQL "
                                    + owner.substring(owner.lastIndexOf('/') + 1) + "." + name + desc);
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return found;
    }

    private static boolean isPlainSql(String owner, String name, String desc) {
        try {
            Class<?> type = Class.forName(owner.replace('/', '.'), false, PersistencePortabilityTest.class.getClassLoader());
            return Stream.concat(Arrays.stream(type.getMethods()), Arrays.stream(type.getDeclaredMethods()))
                    .filter(m -> m.getName().equals(name) && Type.getMethodDescriptor(m).equals(desc))
                    .anyMatch(PersistencePortabilityTest::isPlainSql);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isPlainSql(Method method) {
        return method.isAnnotationPresent(PlainSQL.class);
    }
}
