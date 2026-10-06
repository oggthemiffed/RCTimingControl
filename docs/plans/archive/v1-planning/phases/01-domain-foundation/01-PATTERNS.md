# Phase 1: Domain Foundation - Pattern Map

**Mapped:** 2026-04-16
**Files analyzed:** 47 new files (blank-slate project — no existing source code)
**Analogs found:** 0 / 47 (no codebase yet — canonical framework patterns documented below)

> This is a greenfield project. No existing source analogs exist. This document records the
> canonical framework and library patterns that every file in this phase MUST follow. These
> conventions become the reference baseline for all subsequent phases.

---

## File Classification

### Backend — Build & Configuration

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `settings.gradle.kts` | config | — | Spring Boot multi-module convention | framework |
| `build.gradle.kts` (root) | config | — | Spring Boot Gradle convention | framework |
| `app/build.gradle.kts` | config | — | Spring Boot Gradle Kotlin DSL convention | framework |
| `forwarder/build.gradle.kts` | config | — | Gradle stub module convention | framework |
| `gradle/wrapper/gradle-wrapper.properties` | config | — | Gradle wrapper convention | framework |
| `app/src/main/resources/application.yml` | config | — | Spring Boot profile convention | framework |
| `app/src/main/resources/application-dev.yml` | config | — | Spring Boot dev profile convention | framework |
| `docker-compose.yml` | config | — | Docker Compose postgres+mailpit convention | framework |

### Backend — Application Entry Point & Config Beans

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `app/src/main/java/dev/monkeypatch/rctiming/RcTimingApplication.java` | config | — | Spring Boot `@SpringBootApplication` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/config/JacksonConfig.java` | config | — | Spring Boot `@Configuration` bean convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/config/MailConfig.java` | config | — | Spring Boot `JavaMailSender` config convention | framework |

### Backend — Security

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java` | config | request-response | Spring Security 6 lambda DSL convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/security/JwtTokenService.java` | service | request-response | JJWT 0.12.x documentation pattern | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/security/JwtAuthenticationFilter.java` | middleware | request-response | Spring Security `OncePerRequestFilter` convention | framework |

### Backend — Domain Entities & Repositories

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `app/src/main/java/dev/monkeypatch/rctiming/domain/user/User.java` | model | CRUD | Spring Data JPA `@Entity` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/user/Role.java` | model | — | Java enum convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserRepository.java` | service | CRUD | Spring Data `JpaRepository` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserService.java` | service | CRUD | Spring `@Service` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/auth/PasswordResetToken.java` | model | CRUD | Spring Data JPA `@Entity` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/auth/PasswordResetService.java` | service | request-response | Spring `@Service` + `JavaMailSender` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfile.java` | model | CRUD | Spring Data JPA `@Entity` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/club/GoverningBodyAffiliation.java` | model | CRUD | Spring Data JPA `@Entity` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/track/Track.java` | model | CRUD | Spring Data JPA `@Entity` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/track/DecoderLoop.java` | model | CRUD | Spring Data JPA `@Entity` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/track/TrackLapThreshold.java` | model | CRUD | Spring Data JPA `@Entity` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/raceclass/RacingClass.java` | model | CRUD | Spring Data JPA `@Entity` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/raceclass/RacingClassRepository.java` | service | CRUD | Spring Data `JpaRepository` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/format/RaceFormatConfig.java` | model | transform | Sealed interface + Jackson polymorphism — RESEARCH.md Pattern 2 | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/format/TimedRaceConfig.java` | model | transform | Java record + sealed interface convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/format/BumpUpConfig.java` | model | transform | Java record + sealed interface convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/format/PointsFinalsConfig.java` | model | transform | Java record + sealed interface convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/format/RaceFormatTemplate.java` | model | CRUD | Hypersistence Utils `@Type(JsonType.class)` — RESEARCH.md Code Examples | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/format/EventClass.java` | model | CRUD | Two-column JSONB snapshot+override pattern — CONTEXT.md D-13 | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/format/RaceFormatService.java` | service | CRUD + transform | Spring `@Service` convention | framework |

### Backend — REST Controllers & DTOs

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `app/src/main/java/dev/monkeypatch/rctiming/api/auth/AuthController.java` | controller | request-response | Spring MVC `@RestController` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/api/admin/ClubProfileController.java` | controller | CRUD | Spring MVC `@RestController` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/api/admin/TrackController.java` | controller | CRUD | Spring MVC `@RestController` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/api/admin/RacingClassController.java` | controller | CRUD | Spring MVC `@RestController` convention | framework |
| `app/src/main/java/dev/monkeypatch/rctiming/api/admin/RaceFormatController.java` | controller | CRUD + file-I/O | Spring MVC + Jackson content negotiation convention | framework |

### Backend — Flyway Migrations

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `app/src/main/resources/db/migration/V1__create_users_and_roles.sql` | migration | — | Flyway `V{n}__{description}.sql` convention | framework |
| `app/src/main/resources/db/migration/V2__create_club.sql` | migration | — | Flyway naming convention | framework |
| `app/src/main/resources/db/migration/V3__create_tracks.sql` | migration | — | Flyway naming convention | framework |
| `app/src/main/resources/db/migration/V4__create_racing_classes.sql` | migration | — | Flyway naming convention | framework |
| `app/src/main/resources/db/migration/V5__create_race_formats.sql` | migration | — | Flyway naming convention | framework |

### Backend — Tests

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `app/src/test/java/dev/monkeypatch/rctiming/AbstractIntegrationTest.java` | test | — | Spring Boot `@ServiceConnection` Testcontainers — RESEARCH.md Pattern 3 | framework |
| `app/src/test/java/dev/monkeypatch/rctiming/api/auth/AuthControllerIT.java` | test | request-response | Spring Boot integration test convention | framework |
| `app/src/test/java/dev/monkeypatch/rctiming/api/admin/ClubControllerIT.java` | test | CRUD | Spring Boot integration test convention | framework |
| `app/src/test/java/dev/monkeypatch/rctiming/api/admin/TrackControllerIT.java` | test | CRUD | Spring Boot integration test convention | framework |
| `app/src/test/java/dev/monkeypatch/rctiming/api/admin/RacingClassControllerIT.java` | test | CRUD | Spring Boot integration test convention | framework |
| `app/src/test/java/dev/monkeypatch/rctiming/api/admin/FormatControllerIT.java` | test | CRUD + file-I/O | Spring Boot integration test convention | framework |
| `app/src/test/java/dev/monkeypatch/rctiming/domain/format/RaceFormatConfigSerdeTest.java` | test | transform | JUnit 5 unit test convention | framework |
| `app/src/test/java/dev/monkeypatch/rctiming/domain/format/RaceFormatServiceTest.java` | test | transform | JUnit 5 unit test convention | framework |
| `app/src/test/java/dev/monkeypatch/rctiming/security/SecurityIT.java` | test | request-response | Spring Boot integration test convention | framework |

### Frontend

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `frontend/src/main.tsx` | config | — | React 19 + Vite entry point convention | framework |
| `frontend/src/App.tsx` | config | — | React Router v7 `createBrowserRouter` convention | framework |
| `frontend/src/providers/AuthProvider.tsx` | provider | request-response | React Context + TanStack Query convention | framework |
| `frontend/src/providers/QueryProvider.tsx` | provider | — | TanStack Query `QueryClient` wrapper convention | framework |
| `frontend/src/lib/api.ts` | utility | request-response | Axios interceptor + JWT header convention | framework |
| `frontend/src/lib/auth.ts` | utility | request-response | In-memory token storage convention | framework |
| `frontend/src/hooks/useAuth.ts` | hook | — | React `useContext` hook convention | framework |
| `frontend/src/components/layout/AuthLayout.tsx` | component | — | shadcn/ui `Card` layout convention | framework |
| `frontend/src/pages/auth/LoginPage.tsx` | component | request-response | React Hook Form + Zod + shadcn/ui convention | framework |
| `frontend/src/pages/auth/RegisterPage.tsx` | component | request-response | React Hook Form + Zod + shadcn/ui convention | framework |
| `frontend/src/pages/auth/ForgotPasswordPage.tsx` | component | request-response | React Hook Form + Zod + shadcn/ui convention | framework |
| `frontend/src/pages/auth/ResetPasswordPage.tsx` | component | request-response | React Hook Form + Zod + shadcn/ui convention | framework |
| `frontend/src/pages/admin/AdminPlaceholderPage.tsx` | component | — | React stub page convention | framework |
| `frontend/src/pages/racer/RacerPlaceholderPage.tsx` | component | — | React stub page convention | framework |

---

## Pattern Assignments

### Gradle Multi-Module Build

**Pattern source:** Spring Boot 3.4.x Gradle Kotlin DSL documentation

**`settings.gradle.kts` (repo root):**
```kotlin
rootProject.name = "rctiming"
include(":app", ":forwarder")
```

**`build.gradle.kts` (repo root):**
```kotlin
plugins {
    id("org.springframework.boot") version "3.4.7" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("nu.studer.jooq") version "9.0" apply false
}
```

**`app/build.gradle.kts` — full dependency block:**
```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("nu.studer.jooq")
    java
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jooq:jooq")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.11")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}

// jOOQ codegen wired after flywayMigrate; generated sources committed to git
tasks.named("jooqCodegen") {
    dependsOn(tasks.named("flywayMigrate"))
    inputs.files(fileTree("src/main/resources/db/migration"))
    outputs.dir("src/generated/jooq")
}
```

**CRITICAL — `gradle/wrapper/gradle-wrapper.properties`:**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.2-bin.zip
```
Gradle 9.3.1 (system) is incompatible with Spring Boot 3.4.x. The wrapper MUST pin 8.14.2.
The wrapper must be initialised with: `gradle wrapper --gradle-version 8.14.2`

---

### JPA Entity Convention

**Pattern source:** Spring Data JPA + Hibernate 6 (Jakarta EE namespace)

**Apply to:** All `domain/**/*.java` entity files

```java
// All imports use jakarta.*, never javax.*  (Hibernate 6 / Spring Boot 3.x)
import jakarta.persistence.*;

@Entity
@Table(name = "table_name")
public class ExampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Standard JPA accessor pattern (or use Lombok @Getter/@Setter)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
```

**User entity specifics** (`domain/user/User.java`) — stackable roles via join table:
```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();
}

public enum Role {
    RACER, ADMIN, RACE_DIRECTOR, REFEREE
}
```

---

### JSONB Sealed Interface (RaceFormatConfig)

**Pattern source:** CONTEXT.md D-10, D-11; RESEARCH.md Pattern 2; Hypersistence Utils README

**Apply to:** `domain/format/RaceFormatConfig.java`, `domain/format/RaceFormatTemplate.java`, `domain/format/EventClass.java`

**Sealed interface with Jackson polymorphism:**
```java
// domain/format/RaceFormatConfig.java
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TimedRaceConfig.class,    name = "TIMED"),
    @JsonSubTypes.Type(value = BumpUpConfig.class,       name = "BUMP_UP"),
    @JsonSubTypes.Type(value = PointsFinalsConfig.class, name = "POINTS_FINALS")
})
public sealed interface RaceFormatConfig
    permits TimedRaceConfig, BumpUpConfig, PointsFinalsConfig {}
```

**Record subtype pattern:**
```java
// domain/format/TimedRaceConfig.java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TimedRaceConfig(
    int durationMinutes,
    StartType startType,
    QualifyingType qualifyingType,
    int racePaddingMinutes,
    int staggerIntervalSeconds
) implements RaceFormatConfig {}
```

**Entity JSONB mapping via Hypersistence Utils (hibernate-63 artifact):**
```java
// domain/format/RaceFormatTemplate.java
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "race_format_templates")
public class RaceFormatTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private RaceFormatConfig config;
}
```

**EventClass snapshot+override pattern (D-13):**
```java
// domain/format/EventClass.java
@Entity
@Table(name = "event_classes")
public class EventClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Full copy of template config at assignment time (FORMAT-06)
    @Type(JsonType.class)
    @Column(name = "config_snapshot", columnDefinition = "jsonb", nullable = false)
    private RaceFormatConfig configSnapshot;

    // Nullable patch — only overridden fields present (FORMAT-07)
    @Type(JsonType.class)
    @Column(name = "config_override", columnDefinition = "jsonb")
    private Map<String, Object> configOverride;
}
```

---

### Spring Data Repository Convention

**Pattern source:** Spring Data JPA `JpaRepository` documentation

**Apply to:** All `domain/**/*Repository.java` files

```java
// Example: domain/user/UserRepository.java
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Repositories are interfaces — no implementation class required. Spring Data generates the
implementation at startup. Do not add `@Repository` annotation — `JpaRepository` implies it.

---

### JWT Authentication — Security Config

**Pattern source:** Spring Security 6 lambda DSL; RESEARCH.md Pattern 1

**Apply to:** `security/SecurityConfig.java`

```java
// security/SecurityConfig.java
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "RACE_DIRECTOR", "REFEREE")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**JJWT 0.12.x token service pattern:**
```java
// security/JwtTokenService.java
// NOTE: Use .expiration()/.subject()/.claim() — NOT .setExpiration()/.setSubject()/.setClaims()
//       The 0.12.x builder renamed all setter methods (breaking change from 0.11)

SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));

String accessToken = Jwts.builder()
    .subject(userId.toString())
    .claim("roles", roles.stream().map(Role::name).toList())
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + 900_000L))   // 15 min
    .signWith(signingKey)
    .compact();

Claims claims = Jwts.parser()
    .verifyWith(signingKey)
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

**JWT filter pattern (`OncePerRequestFilter`):**
```java
// security/JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenService.parseToken(token);
                // build UsernamePasswordAuthenticationToken, set on SecurityContextHolder
                List<String> roles = claims.get("roles", List.class);
                List<GrantedAuthority> authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
                var auth = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                // invalid token — do not set context; let request proceed unauthenticated
            }
        }
        chain.doFilter(request, response);
    }
}
```

---

### REST Controller Convention

**Pattern source:** Spring MVC `@RestController` + `@RequestMapping` documentation

**Apply to:** All `api/**/*Controller.java` files

```java
// api/admin/TrackController.java — illustrative CRUD controller skeleton
@RestController
@RequestMapping("/api/v1/admin/tracks")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {   // constructor injection, no @Autowired
        this.trackService = trackService;
    }

    @GetMapping
    public List<TrackDto> listTracks() {
        return trackService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrackDto createTrack(@RequestBody @Valid CreateTrackRequest request) {
        return trackService.create(request);
    }

    @PutMapping("/{id}")
    public TrackDto updateTrack(@PathVariable Long id,
                                @RequestBody @Valid UpdateTrackRequest request) {
        return trackService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrack(@PathVariable Long id) {
        trackService.delete(id);
    }
}
```

**Auth controller pattern (no `@PreAuthorize` — auth endpoints are public):**
```java
// api/auth/AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody @Valid RegisterRequest request) { ... }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request,
                                               HttpServletResponse response) {
        // sets HttpOnly refresh cookie on response
        ...
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @RequestBody @Valid PasswordResetRequestDto request) {
        // ALWAYS return 200 — never leak whether email exists (Pitfall 5)
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok().build();
    }
}
```

**URL convention (locked by UI-SPEC § endpoints):** `/api/v1/{resource}` — all REST endpoints use `/api/v1/` prefix.

---

### Service Layer Convention

**Pattern source:** Spring `@Service` + `@Transactional` documentation

**Apply to:** All `domain/**/*Service.java` and `api/` supporting services

```java
@Service
@Transactional          // class-level default; read-only methods override with @Transactional(readOnly = true)
public class TrackService {

    private final TrackRepository trackRepository;

    public TrackService(TrackRepository trackRepository) {   // constructor injection
        this.trackRepository = trackRepository;
    }

    @Transactional(readOnly = true)
    public List<TrackDto> findAll() {
        return trackRepository.findAll().stream()
            .map(TrackDto::from)
            .toList();
    }

    public TrackDto create(CreateTrackRequest request) {
        var track = new Track();
        track.setName(request.name());
        // ...
        return TrackDto.from(trackRepository.save(track));
    }

    public void delete(Long id) {
        if (!trackRepository.existsById(id)) {
            throw new EntityNotFoundException("Track not found: " + id);
        }
        trackRepository.deleteById(id);
    }
}
```

---

### Flyway Migration Convention

**Pattern source:** Flyway SQL migration documentation; RESEARCH.md Code Examples

**Apply to:** All `db/migration/*.sql` files

```
Naming: V{version}__{description}.sql  (DOUBLE underscore between version and description)

Examples:
  V1__create_users_and_roles.sql
  V2__create_club.sql
  V3__create_tracks.sql
  V4__create_racing_classes.sql
  V5__create_race_formats.sql
```

**SQL conventions for this project:**
```sql
-- Use lowercase SQL keywords; PostgreSQL 16 dialect
-- Always include NOT NULL where appropriate
-- Use BIGSERIAL or IDENTITY for surrogate PKs (not UUID — no requirement for it)
-- Use TIMESTAMPTZ (not TIMESTAMP) for all datetime columns
-- Use JSONB (not JSON or TEXT) for polymorphic config columns
-- Foreign keys always have explicit ON DELETE behavior

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);
```

---

### Integration Test Base Class

**Pattern source:** Spring Boot 3.4 `@ServiceConnection` documentation; RESEARCH.md Pattern 3

**Apply to:** `AbstractIntegrationTest.java`; all `*IT.java` test classes extend it

```java
// AbstractIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    // @ServiceConnection auto-configures spring.datasource.* — no @DynamicPropertySource needed
    // Flyway migrations run automatically on Spring context startup
    // Static container is shared across all test classes in the same JVM — improves speed
}
```

**Integration test class pattern:**
```java
// api/auth/AuthControllerIT.java
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void register_validRequest_returns201() {
        var request = new RegisterRequest("Alice", "Smith", "alice@example.com", "password123");
        var response = restTemplate.postForEntity("/api/v1/auth/register", request, AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

**Unit test pattern (no Spring context, no Docker required):**
```java
// domain/format/RaceFormatConfigSerdeTest.java
class RaceFormatConfigSerdeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void timedConfig_roundTrips_preservesType() throws Exception {
        var config = new TimedRaceConfig(10, StartType.GRID, QualifyingType.FTQ, 5, 1);
        String json = mapper.writeValueAsString(config);
        assertThat(json).contains("\"type\":\"TIMED\"");

        var parsed = mapper.readValue(json, RaceFormatConfig.class);
        assertThat(parsed).isInstanceOf(TimedRaceConfig.class);
    }
}
```

---

### Format Config Export/Import (Content-Type Negotiation)

**Pattern source:** CONTEXT.md D-12; RESEARCH.md Pattern 5

**Apply to:** `api/admin/RaceFormatController.java`, `config/JacksonConfig.java`

```java
// config/JacksonConfig.java — register YAML mapper as a NAMED bean (never @Primary)
@Configuration
public class JacksonConfig {

    @Bean("yamlObjectMapper")    // named, not @Primary — prevents breaking Spring MVC JSON
    public ObjectMapper yamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory())
            .findAndRegisterModules();
    }
}
```

```java
// api/admin/RaceFormatController.java — export + import endpoints
@GetMapping(value = "/{id}/export",
            produces = {MediaType.APPLICATION_JSON_VALUE, "application/yaml"})
public ResponseEntity<RaceFormatConfig> exportFormat(@PathVariable Long id) {
    return ResponseEntity.ok(raceFormatService.findById(id).config());
}

@PostMapping(value = "/import",
             consumes = {MediaType.APPLICATION_JSON_VALUE, "application/yaml"})
public ResponseEntity<RaceFormatTemplateDto> importFormat(
        @RequestBody @Valid RaceFormatConfig config) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(raceFormatService.createFromConfig(config));
}
```

---

### Frontend Entry Point and Router Setup

**Pattern source:** React 19 + Vite 8 + React Router v7 Data Router API

**Apply to:** `frontend/src/main.tsx`, `frontend/src/App.tsx`

```typescript
// frontend/src/main.tsx — dark mode bootstrap + React root
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

// Apply dark class based on OS preference (Phase 1 — no manual toggle yet)
if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
  document.documentElement.classList.add('dark');
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

```typescript
// frontend/src/App.tsx — createBrowserRouter (Data Router API)
// Use createBrowserRouter, NOT <BrowserRouter> (React Router v7 — legacy API)
import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom';
import { AuthProvider } from './providers/AuthProvider';
import { QueryProvider } from './providers/QueryProvider';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';
import AdminPlaceholderPage from './pages/admin/AdminPlaceholderPage';
import RacerPlaceholderPage from './pages/racer/RacerPlaceholderPage';

const router = createBrowserRouter([
  { path: '/',               element: <Navigate to="/login" replace /> },
  { path: '/login',          element: <LoginPage /> },
  { path: '/register',       element: <RegisterPage /> },
  { path: '/forgot-password',element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  {
    path: '/admin/*',
    element: (
      <ProtectedRoute roles={['ADMIN', 'RACE_DIRECTOR', 'REFEREE']}>
        <AdminPlaceholderPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/racer/*',
    element: (
      <ProtectedRoute>
        <RacerPlaceholderPage />
      </ProtectedRoute>
    )
  },
  { path: '*', element: <NotFoundPage /> },
]);

export default function App() {
  return (
    <QueryProvider>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryProvider>
  );
}
```

---

### AuthProvider Contract

**Pattern source:** React Context API + UI-SPEC § 6

**Apply to:** `frontend/src/providers/AuthProvider.tsx`, `frontend/src/hooks/useAuth.ts`

```typescript
// frontend/src/providers/AuthProvider.tsx
interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: Array<'RACER' | 'ADMIN' | 'RACE_DIRECTOR' | 'REFEREE'>;
}

interface AuthContextValue {
  user: AuthUser | null;
  accessToken: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  isLoading: boolean;   // true during initial session restore check
}

// On mount: attempt POST /api/v1/auth/refresh silently
// If it succeeds: populate user + accessToken
// If it fails: user remains null, isLoading → false
// While isLoading: render a centered <Loader2 animate-spin> instead of RouterProvider
//
// Access token: in-memory state ONLY — never localStorage/sessionStorage
// Refresh token: HttpOnly cookie managed by backend — JS never reads it
```

---

### Axios API Client

**Pattern source:** Axios interceptor documentation; UI-SPEC § 6

**Apply to:** `frontend/src/lib/api.ts`

```typescript
// frontend/src/lib/api.ts
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',  // empty = uses Vite proxy in dev
  withCredentials: true,   // sends HttpOnly refresh cookie on /api/v1/auth/refresh
});

// Attach in-memory access token on every request
api.interceptors.request.use(config => {
  const token = getAccessToken();   // from AuthProvider state via closure or import
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// On 401: attempt refresh once, then retry; on second 401: logout
api.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      try {
        await api.post('/api/v1/auth/refresh');
        return api(error.config);
      } catch {
        logout();
      }
    }
    return Promise.reject(error);
  }
);
```

---

### Auth Screen Form Pattern

**Pattern source:** React Hook Form v7 + Zod v4 + shadcn/ui Card + UI-SPEC § 4

**Apply to:** All `frontend/src/pages/auth/*.tsx` page components

```typescript
// frontend/src/pages/auth/LoginPage.tsx — canonical auth form skeleton
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Form, FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form';
import { Loader2 } from 'lucide-react';
import AuthLayout from '@/components/layout/AuthLayout';

const loginSchema = z.object({
  email: z.string().email('Valid email required'),
  password: z.string().min(1, 'Password required'),
});
type LoginForm = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const form = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur',   // validate email on blur; full schema on submit
  });

  const { login } = useAuth();
  const [isPending, setIsPending] = useState(false);

  async function onSubmit(values: LoginForm) {
    setIsPending(true);
    try {
      await login(values.email, values.password);
      // role-based redirect handled in AuthProvider.login()
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 401) {
        form.setError('password', { message: 'Invalid email or password' });
      } else {
        toast({ title: 'Unable to reach server. Please try again.', duration: 8000 });
      }
    } finally {
      setIsPending(false);
    }
  }

  return (
    <AuthLayout title="Sign in" subtitle="Enter your email and password">
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <FormField control={form.control} name="email" render={({ field }) => (
            <FormItem>
              <FormLabel>Email</FormLabel>
              <FormControl><Input type="email" {...field} /></FormControl>
              <FormMessage />   {/* text-destructive text-sm */}
            </FormItem>
          )} />
          {/* ... password field ... */}
          <Button type="submit" className="w-full" disabled={isPending}>
            {isPending ? <><Loader2 className="animate-spin mr-2 h-4 w-4" />Signing in...</> : 'Sign in'}
          </Button>
        </form>
      </Form>
    </AuthLayout>
  );
}
```

**Key constraints from UI-SPEC:**
- Submit button uses `disabled` (not `aria-disabled`) while in flight
- `<Loader2 className="animate-spin">` from `lucide-react` for spinner
- Field errors rendered via `<FormMessage>` below the field, not above
- Toast for network errors (not field errors); 5s for success, 8s for errors

---

### AuthLayout Component

**Pattern source:** shadcn/ui `Card` components; UI-SPEC § 4.1

**Apply to:** `frontend/src/components/layout/AuthLayout.tsx`

```typescript
// frontend/src/components/layout/AuthLayout.tsx
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';

interface AuthLayoutProps {
  title: string;
  subtitle?: string;
  footer?: React.ReactNode;
  children: React.ReactNode;
}

export default function AuthLayout({ title, subtitle, footer, children }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background px-4">
      <div className="mb-6 text-2xl font-semibold tracking-tight">
        RC Timing Control
      </div>
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>{title}</CardTitle>
          {subtitle && <CardDescription>{subtitle}</CardDescription>}
        </CardHeader>
        <CardContent>{children}</CardContent>
        {footer && <CardFooter>{footer}</CardFooter>}
      </Card>
    </div>
  );
}
```

---

### Vite Configuration

**Pattern source:** Vite 8.x + TypeScript + Tailwind v4; UI-SPEC § 9

**Apply to:** `frontend/vite.config.ts`

```typescript
// frontend/vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',   // avoids CORS in dev; no CORS config needed on Spring side
    },
  },
});
```

---

### Application YAML Profile Structure

**Pattern source:** Spring Boot `application.yml` multi-profile convention

**Apply to:** `app/src/main/resources/application.yml`, `application-dev.yml`

```yaml
# application.yml — shared across all profiles
spring:
  jpa:
    hibernate:
      ddl-auto: validate          # NEVER update or create-drop — Flyway owns schema
    open-in-view: false           # disable OSIV — prevents lazy-load surprises in web tier
  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  jwt:
    secret: ${JWT_SECRET}         # base64-encoded 256-bit key; injected via env var
    access-token-ttl-ms: 900000   # 15 min
    refresh-token-ttl-ms: 604800000  # 7 days

---
# application-dev.yml — activated with SPRING_PROFILES_ACTIVE=dev
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/rctiming_dev
    username: rctiming
    password: rctiming
  mail:
    host: localhost
    port: 1025                    # MailPit SMTP port
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false
```

---

## Shared Patterns

### Jakarta EE Namespace (Critical — applies to every Java file)

**Source:** Spring Boot 3.x / Hibernate 6 migration guide
**Apply to:** All Java source files

```java
// CORRECT — Spring Boot 3.x / Hibernate 6
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import jakarta.transaction.Transactional;

// WRONG — will not compile under Spring Boot 3.x
// import javax.persistence.*;
// import javax.validation.constraints.*;
```

Any Java code copied from pre-2022 sources or AI training data that uses `javax.*` imports
will fail to compile. Every entity and DTO must use `jakarta.*`.

---

### Constructor Injection (All Spring Beans)

**Source:** Spring Framework best practices
**Apply to:** All `@Service`, `@RestController`, `@Component`, `@Configuration` classes

```java
// CORRECT — constructor injection; allows unit testing without Spring context
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
}

// AVOID — field injection; breaks unit testing without Spring context
@Service
public class UserService {
    @Autowired private UserRepository userRepository;   // do not use
}
```

---

### Global Exception Handler

**Source:** Spring MVC `@RestControllerAdvice` convention
**Apply to:** A single `api/GlobalExceptionHandler.java` (create in Phase 1)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        // map field errors to { field: message } structure
        var detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (a, b) -> a)));
        return detail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleConflict(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Resource already exists");
    }
}
```

Use `ProblemDetail` (RFC 9457 — built into Spring 6). Do not roll a custom error envelope.

---

### Password Reset — Always Return 200 (Security Requirement)

**Source:** RESEARCH.md Pitfall 5; UI-SPEC § 4.4
**Apply to:** `api/auth/AuthController.java` password reset request endpoint

The reset request endpoint MUST return `200 OK` whether or not the email exists in the
system. This is a security requirement, not a UX choice. The controller should never branch
on email existence in the response — delegate to `PasswordResetService` which handles the
conditional email send internally.

---

### Refresh Token — Database-Backed (A5 Resolution)

**Source:** RESEARCH.md Open Question 3 (recommendation: database-backed)
**Apply to:** `domain/auth/` refresh token handling

Use a `refresh_tokens` table (not stateless JWT cookie). This enables immediate revocation
on password reset — users who change their password should not remain logged in on other
devices. Schema:

```sql
-- In V1__create_users_and_roles.sql or a dedicated migration
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 of the raw token
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked     BOOLEAN NOT NULL DEFAULT false
);
```

---

## No Analog Found

This section is not applicable — the entire codebase is new. All files listed above have
no existing analog to copy from. The patterns documented above ARE the reference.

Files that rely entirely on external library documentation (no extractable code pattern):
- `docker-compose.yml` — follow Docker Compose Postgres + MailPit documentation
- `components.json` — generated by `npx shadcn@latest init --preset b1GyYWRfE`; do not hand-author
- `frontend/src/components/ui/**` — generated by shadcn CLI; do not hand-author

---

## Key Anti-Patterns (Enforced)

These must not appear in any Phase 1 file. They are documented here so the planner's
verification steps can check for them.

| Anti-Pattern | Where It Would Appear | Correct Alternative |
|---|---|---|
| `javax.persistence.*` imports | Any Java entity or service | `jakarta.persistence.*` |
| `spring.jpa.hibernate.ddl-auto: update` | `application.yml` | `validate` only |
| `@Autowired` field injection | Any Spring bean | Constructor injection |
| JJWT 0.11 methods (`.setClaims()`, `.setExpiration()`) | `JwtTokenService.java` | `.claim()`, `.expiration()` 0.12.x API |
| `hypersistence-utils-hibernate-62` artifact | `app/build.gradle.kts` | `hypersistence-utils-hibernate-63:3.9.11` |
| `gradle build` using system Gradle (9.x) | CI or developer shell | `./gradlew build` always |
| `localStorage.setItem('token', ...)` | Any frontend file | In-memory state in `AuthProvider` only |
| `<BrowserRouter>` wrapper | `App.tsx` | `createBrowserRouter` + `RouterProvider` |
| `yamlObjectMapper` registered as `@Primary` | `JacksonConfig.java` | Named bean `@Bean("yamlObjectMapper")` |
| `@JsonTypeInfo` on record subtype (not interface) | Format config classes | On the sealed interface only |

---

## Metadata

**Analog search scope:** Full repository — only `CLAUDE.md` exists; no source code found
**Files scanned:** 1 (CLAUDE.md at repo root)
**Framework documentation sources:** Spring Boot 3.4 reference, JJWT 0.12.x, Hypersistence Utils, Flyway 10, jOOQ 3.19, React Router v7, React Hook Form v7, shadcn/ui
**Pattern extraction date:** 2026-04-16
**Valid until:** 2026-07-16 (library versions verified in RESEARCH.md; re-verify at upgrade)
