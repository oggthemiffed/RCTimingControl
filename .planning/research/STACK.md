# Stack Research — RC Timing System

**Project:** RCTimingControl
**Researched:** 2026-04-15
**Research method:** Training knowledge (cutoff August 2025). External network tools unavailable in this environment. Version numbers flagged LOW confidence must be verified against spring.io, npmjs.com, and MVNRepository before adoption.

---

## Recommended Stack

| Concern | Choice | Version | Confidence |
|---------|--------|---------|------------|
| Backend framework | Spring Boot | 3.4.x (verify at spring.io) | MEDIUM — training says 3.3 GA in mid-2024; 3.4 likely by late 2024 |
| Java version | Java 21 (LTS) | 21 | HIGH — Spring Boot 3.x requires Java 17+; 21 is current LTS and Spring's preferred baseline |
| Build tool | Maven or Gradle (Kotlin DSL) | Maven 3.9+ / Gradle 8.x | HIGH — both fully supported; Maven is lower ceremony for Java-first teams |
| Real-time push | Spring WebSocket (STOMP over WS) | via spring-boot-starter-websocket | HIGH — bidirectional, pub/sub via STOMP topics suits live lap timing broadcast |
| TCP decoder client | Netty (custom AMB protocol codec) | 4.1.x | HIGH — no dedicated Java AMB library exists; Netty's pipeline/codec model is the correct abstraction |
| Frontend framework | React (Vite) | React 18.x, Vite 5.x | HIGH — React's component model handles both admin CRUD and live-updating timing displays cleanly |
| Frontend state/real-time | Zustand + native WebSocket | Zustand 4.x | MEDIUM — lightweight, no Redux overhead; pairs well with WS event dispatch |
| Admin UI components | shadcn/ui (Tailwind CSS) | shadcn/ui current, Tailwind 3.x | MEDIUM — composable, no bundle bloat from unused components |
| Persistence | PostgreSQL | 16.x | HIGH — relational model fits race/event/racer/lap data; strong JSON support for flexible scoring configs |
| ORM / data access | Spring Data JPA + Hibernate | via spring-boot-starter-data-jpa | HIGH — standard Spring Boot persistence; Hibernate 6 ships with Boot 3.x |
| Database migrations | Flyway | 10.x | HIGH — SQL-first, deterministic; preferred over Liquibase for teams comfortable with SQL |
| Auth | Spring Security + JWT (stateless API) | via spring-boot-starter-security | HIGH — fits SPA + WebSocket architecture; JWT issued at login, sent in WS handshake header |
| JWT library | jjwt (JJWT) | 0.12.x | MEDIUM — well-maintained, widely used; verify 0.12 is still current at github.com/jwtk/jjwt |
| API style | REST (JSON) for CRUD + STOMP/WS for push | — | HIGH — clean separation of concerns; REST for racer portal/admin, STOMP for timing events |
| Print/export | JasperReports or Apache POI | JasperReports 7.x / POI 5.x | MEDIUM — JasperReports for formatted race result PDFs; POI for Excel export |
| Testing (backend) | JUnit 5 + Mockito + Testcontainers | JUnit 5.x / Testcontainers 1.19+ | HIGH — standard Spring Boot test stack; Testcontainers for PostgreSQL integration tests |
| Testing (frontend) | Vitest + React Testing Library | Vitest 1.x | MEDIUM — native to Vite, fast, same API as Jest |

---

## Rationale

### Backend Framework: Spring Boot 3.4.x on Java 21

**Why Spring Boot 3.x:** The club already has Java expertise. Spring Boot 3 is the current generation — it requires Java 17+ (Java 21 LTS preferred), uses Jakarta EE 9+ namespaces, ships with Spring Framework 6, and has native GraalVM support if ever needed. Spring Boot 2.x reached end-of-life in November 2023; it must not be used for a greenfield project in 2026.

**Why Java 21 (not 17):** Java 21 is the current LTS (September 2023). Spring Boot 3.2+ actively supports virtual threads (Project Loom) via `spring.threads.virtual.enabled=true`, which simplifies high-throughput I/O without reactive complexity — relevant for holding many WebSocket sessions and the AMB TCP connection simultaneously. Java 17 works but 21 is the better long-term choice.

**Key starters needed:**
- `spring-boot-starter-web` — REST API
- `spring-boot-starter-websocket` — STOMP/WebSocket for live timing
- `spring-boot-starter-data-jpa` — persistence
- `spring-boot-starter-security` — authentication and authorization
- `spring-boot-starter-validation` — bean validation
- `spring-boot-starter-actuator` — health checks, metrics

**Why not Spring WebFlux (reactive):** The system is moderately concurrent — dozens of WebSocket clients, one AMB TCP connection, standard CRUD. WebFlux adds substantial complexity (reactive programming model, R2DBC instead of JPA, different testing patterns) for no meaningful benefit at this scale. Virtual threads on Spring MVC give comparable throughput without the paradigm shift. Do not use WebFlux.

---

### Real-time Push: STOMP over WebSockets (not SSE)

**Why WebSocket + STOMP:** Live timing has two directions of concern: the server pushes lap times to all connected browsers, and the race control client sends commands (start race, marshal lap) to the server. WebSocket is inherently bidirectional — you get both directions on a single connection. STOMP (Simple Text Oriented Messaging Protocol) layered over WebSocket gives you topic-based pub/sub at near zero cost (`/topic/race.123.laps` for broadcast, `/user/queue/errors` for targeted messages). Spring's `spring-boot-starter-websocket` ships full STOMP broker support out of the box.

**Why not SSE (Server-Sent Events):** SSE is unidirectional (server to client only). Race control commands going the other direction would need a separate REST call per action, creating a split-channel design and making race control state harder to reason about. SSE is appropriate for read-only dashboards; this system has an interactive race control client, so SSE is the wrong fit.

**Why not raw WebSocket without STOMP:** Without STOMP you build your own message envelope format, your own topic routing, and your own client-side dispatcher. STOMP + SockJS gives you all of that for free and the JavaScript `@stomp/stompjs` client library (npm) handles reconnection, heartbeat, and subscription management.

**In-memory broker vs. RabbitMQ/ActiveMQ:** For a single-venue deployment serving tens to low hundreds of concurrent WebSocket connections, the Spring in-memory STOMP broker is entirely sufficient. Do not introduce a message broker (RabbitMQ, Kafka) for v1 — it adds operational complexity with no benefit at this scale.

---

### AMB/MyLaps TCP Protocol: Netty with Custom Codec

**Why Netty:** There is no published Java library that implements the AMB/MyLaps decoder TCP protocol. The protocol is a binary, framed, proprietary format documented in MyLaps's hardware SDK documentation (available through the MyLaps developer program). The correct approach is to implement a Netty channel pipeline with a custom `ByteToMessageDecoder` that frames and parses AMB messages.

Netty is the right tool because:
1. It handles TCP framing cleanly via its `ByteBuf` pipeline — AMB messages are length-prefixed binary frames, exactly the pattern Netty's codec pipeline was built for.
2. It manages reconnection logic (the decoder is at-venue hardware; the server must reconnect if the TCP link drops).
3. It runs entirely on the application thread pool without blocking Spring's web threads.
4. Netty 4.1.x is mature, widely used, and has no significant competition for this use case.

**Implementation approach:**
- Run the Netty client as a Spring `@Component` with `@PostConstruct` startup.
- On each parsed AMB message (transponder ID, timestamp, loop ID), publish a Spring `ApplicationEvent` or push directly to the STOMP broker topic for the active race.
- The AMB TCP client should be isolated in its own package (`timing.decoder`) with a clean interface so the protocol implementation is swappable.

**Protocol documentation:** The MyLaps/AMB binary protocol is described in the "AMB RC4 Decoder" documentation and the "MyLaps SDK for Timing Systems." You will need to either obtain this from MyLaps directly or reverse-engineer it from Wireshark captures against real hardware. Several open-source timing projects (Python-based RC timing tools, OpenRC-related projects) have partial implementations that can serve as reference. Budget time for this — it is the highest-risk unknown in the entire stack.

**Why not plain Java NIO:** Java NIO's `Selector`-based API is lower-level and more error-prone than Netty's pipeline model. Netty provides the abstraction layer you want without the verbosity.

**Why not Spring Integration (TCP):** Spring Integration's TCP support works but is verbose to configure for a binary protocol with custom framing. Netty's `ByteToMessageDecoder` is more direct for binary protocol parsing.

---

### Frontend: React 18 + Vite

**Why React:** This project has two distinct UI modes:
1. **Admin / racer portal** — CRUD-heavy forms (event creation, racer registration, championship config). Standard form-heavy SPA.
2. **Live timing display + race control** — real-time updating tables driven by WebSocket events, low-latency re-render on every lap message.

React handles both well. The live timing display requires frequent, targeted DOM updates as lap times stream in — React's component re-render model with proper memoization (`React.memo`, `useMemo`) prevents unnecessary repaints. Vue would also work here, but React has a larger ecosystem for data table and grid components (TanStack Table, AG Grid) which are likely needed for lap-by-lap timing displays.

**Why not HTMX:** HTMX is excellent for server-rendered HTML partial replacements, but live timing requires sub-second DOM updates driven by WebSocket push, not HTTP responses. HTMX's model (swap HTML fragments returned from HTTP requests) does not map well to WebSocket event streams. You would need to layer in Alpine.js or similar for reactive state, at which point you are building a fragmented version of React anyway. HTMX is the wrong tool for a system with a hard real-time UI requirement.

**Why not Vue 3:** Vue 3 with Composition API is a legitimate alternative and technically comparable to React for this use case. The tiebreaker: React's ecosystem for data-dense admin UIs (TanStack Table, React Hook Form, React Query) is more mature and has more active maintenance. The live timing display is essentially a high-frequency updating data grid — TanStack Table handles this pattern well and is React-first.

**Why not Angular:** Angular's opinionated structure, RxJS dependency, and higher initial complexity are not justified for a team building a single focused application. Angular's advantages (enterprise consistency at scale) are irrelevant for a club timing system with one team.

**Why Vite (not Create React App / Next.js):** Vite is the current standard React build tool. Create React App is effectively unmaintained as of 2023. Next.js adds SSR complexity you don't need — all data fetching is client-side from the REST API, and SSR for race results adds no SEO benefit for a venue-local system. Vite gives fast HMR, native ESM, and zero lock-in.

**Key frontend libraries:**
- `@stomp/stompjs` — WebSocket STOMP client (do not use SockJS in modern browsers; native WS is fine for venue LAN)
- `TanStack Query (React Query) v5` — server state management for REST API calls
- `TanStack Table v8` — live timing display tables with virtualization
- `React Hook Form v7` — form handling for racer portal and admin forms
- `Zod` — schema validation, shared types with backend if using TypeScript
- `shadcn/ui` — component library built on Radix UI primitives + Tailwind; avoids the MUI bundle weight
- `Tailwind CSS 3.x` — utility-first styling; consistent with shadcn/ui

---

### Database: PostgreSQL 16

**Why PostgreSQL:** The data model is relational at its core — racers, transponders, events, races, classes, entries, lap records, championship standings. PostgreSQL handles all of this natively. Specific advantages for this domain:
- `TIMESTAMPTZ` with microsecond precision for lap timestamps — critical for accurate timing.
- JSONB columns for flexible championship scoring config (`best_x_from_y`, bonus points rules) without schema migrations every time scoring rules change.
- Window functions (`RANK() OVER`, `LAG()`) for computing positions, gaps, and championship standings directly in SQL.
- Strong Spring Data JPA support.

**Why not MySQL/MariaDB:** MySQL is a viable alternative, but PostgreSQL's window function support, JSONB, and stricter SQL standards compliance give a measurable advantage for the scoring computation queries this system will need.

**Why not SQLite:** SQLite is not appropriate for a networked server application. It does not support concurrent writes and cannot be accessed by multiple application processes.

**Why not MongoDB or a document store:** The data is fundamentally relational (a lap belongs to a race run, which belongs to a race, which belongs to an event, which involves a racer and transponder). Forcing this into a document model creates join pain without benefit.

**Schema design note:** Lap timestamps must be stored as UTC microseconds (`TIMESTAMPTZ` or `BIGINT` microseconds). Do not store local time — venue time zones change, DST exists, and precision matters for tight racing.

---

### ORM and Migrations: Spring Data JPA + Flyway

**Why Spring Data JPA:** It is the natural choice with Spring Boot and saves significant boilerplate for repository-style data access. For complex scoring queries (championship standings with best-X-from-Y filtering), use `@Query` with native SQL or JPQL rather than trying to express them with Criteria API.

**Why Flyway over Liquibase:** Flyway uses plain SQL migration scripts — easy to read, easy to review, no XML/YAML abstraction layer. For a small team building a domain-specific application, Flyway's simplicity wins. Liquibase's XML DSL adds friction without benefit at this scale.

**Use Hibernate's DDL generation for development only** (`spring.jpa.hibernate.ddl-auto=validate` in production). Never use `create-drop` in any environment that has real data.

---

### Auth: Spring Security + JWT (stateless)

**Why JWT (stateless API tokens):** The system has a React SPA frontend communicating with a Spring REST API, plus WebSocket connections that need authentication. JWT fits this pattern:
- The racer logs in via REST, receives a JWT.
- The SPA stores the JWT in memory (not localStorage — avoids XSS token theft; use a short-lived access token + HttpOnly cookie for refresh token).
- The JWT is sent in the `Authorization: Bearer` header on REST calls.
- For the WebSocket STOMP connection, the JWT is passed in the STOMP `CONNECT` frame headers and validated on the server before subscription is allowed.

**Why not session-based auth (Spring Security sessions):** Session-based auth with WebSockets requires sticky sessions if you ever scale horizontally and complicates WebSocket authentication. JWT is stateless and cleanly handles both REST and WebSocket auth from a single token.

**Why not Spring Authorization Server (OAuth2):** OAuth2 is appropriate when you have third-party client integrations or need a full identity provider. A club timing system with its own user table does not need OAuth2 complexity. Use Spring Authorization Server only if the club later wants "Login with Google" for racers — that can be added incrementally. Start with simple username/password + JWT.

**JJWT 0.12.x:** The `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` triple is the standard Java JWT library. Version 0.12 introduced a cleaner builder API. Verify the current version at `github.com/jwtk/jjwt` before pinning.

**Roles to model:** Two roles cover the requirements — `RACER` (self-service portal) and `OFFICIAL` (admin + race control). A superuser admin role can be modeled as `OFFICIAL` with an additional flag or a third role `ADMIN` — keep it simple.

---

### Print/Export: JasperReports for PDFs

**Why JasperReports:** The requirement "print or export race results at the venue" needs formatted output — a table with positions, names, best lap, total laps, gap. JasperReports produces high-quality PDFs from `.jrxml` templates. It integrates with Spring via direct API call (no starter needed). Alternatively, Apache FOP (XSL-FO to PDF) is viable but has worse tooling.

**Apache POI for Excel:** If Excel export is needed (some officials like to keep records in spreadsheets), Apache POI's XSSF (xlsx) API is the standard choice. POI is heavy — only add it if Excel export is a confirmed requirement; defer until needed.

---

## What NOT to Use

- **Spring Boot 2.x** — End-of-life November 2023. Security patches have stopped. Do not use.
- **Spring WebFlux / reactive stack** — Adds paradigm complexity (Project Reactor, R2DBC) with no throughput benefit at venue scale (tens of concurrent connections). Stick with Spring MVC + virtual threads.
- **HTMX** — Cannot drive a WebSocket-fed live timing display. Its HTTP-fragment-swap model is fundamentally incompatible with the real-time push requirements.
- **Angular** — Over-engineered for this application's scope. RxJS complexity not justified.
- **Create React App** — Unmaintained since 2023. Use Vite.
- **Next.js** — SSR complexity adds no value for a venue-local SPA talking to its own API.
- **Redux (legacy)** — Zustand or TanStack Query cover all state management needs without Redux's boilerplate. If you find yourself reaching for Redux, reconsider the component architecture.
- **SockJS** — Only needed for IE11 and older proxies that block WebSocket upgrades. A venue LAN in 2026 will have no such constraints. Use native WebSocket.
- **MySQL/MariaDB** — Viable but PostgreSQL's window functions and JSONB support are meaningfully better for race scoring queries. PostgreSQL is the correct choice.
- **MongoDB** — The data model is relational. A document store creates join pain without benefit.
- **Kafka / RabbitMQ** — Over-engineered for single-venue deployment. Spring's in-memory STOMP broker is sufficient for the concurrency levels this system will see.
- **Liquibase** — Functional, but Flyway's plain-SQL approach is simpler and faster to reason about for a small team.
- **Hibernate `ddl-auto=create` / `create-drop` in production** — Will silently destroy race data. Only `validate` or `none` in production.

---

## Dependency Matrix

```
AMB Decoder (Netty)
    └── publishes ApplicationEvent / STOMP message
            └── Spring WebSocket broker
                    └── broadcasts to /topic/race.{id}.laps
                            └── React frontend (STOMP client)
                                    └── TanStack Table (live timing display)

Racer portal / Admin
    └── React SPA (React Hook Form + TanStack Query)
            └── Spring REST API (@RestController)
                    └── Spring Data JPA
                            └── PostgreSQL

Race Control Client
    └── React SPA (STOMP send + REST)
            ├── STOMP → Spring WebSocket (start/stop/marshal commands)
            └── REST  → Spring API (marshal lap adjustments, result publication)
```

---

## Version Verification Checklist

These versions were accurate as of August 2025 training cutoff. Verify before adopting:

| Library | Verify At | What to Check |
|---------|-----------|---------------|
| Spring Boot | spring.io/projects/spring-boot | Current GA version (expected 3.4.x) |
| Java 21 | jdk.java.net | Still current LTS (Java 25 LTS due Sept 2025 — confirm) |
| React | npmjs.com/package/react | 18.x vs 19.x GA status |
| Vite | vitejs.dev | 5.x vs 6.x |
| TanStack Query | tanstack.com | v5 stable |
| TanStack Table | tanstack.com | v8 stable |
| JJWT | github.com/jwtk/jjwt | 0.12.x current |
| Flyway | flywaydb.org | 10.x (check PostgreSQL 16 compatibility) |
| Netty | netty.io | 4.1.x (5.x was cancelled; 4.1.x is current) |
| shadcn/ui | ui.shadcn.com | No version pin — copy-paste model; check Tailwind v4 migration |
| @stomp/stompjs | npmjs.com/package/@stomp/stompjs | 7.x |

**Note on Java LTS:** Java 25 was projected for September 2025. If it has shipped by the time this project begins, Java 25 LTS is the better long-term choice. Spring Boot 3.4+ will support it. Verify at adoptium.net.

**Note on Tailwind CSS:** Tailwind v4 (alpha/beta in mid-2024, GA potentially in late 2024 or 2025) changed the configuration model significantly. shadcn/ui may have migrated to Tailwind v4 by now. Check ui.shadcn.com for current Tailwind compatibility before starting the frontend.

---

## AMB/MyLaps Protocol — Special Note

**This is the highest-risk unknown in the project.** The AMB/MyLaps decoder TCP protocol is proprietary. As of August 2025 training data:

1. MyLaps publishes a developer SDK (requires registration at mylaps.com/developers). The SDK documentation describes the binary message format for RC4 and similar decoders.
2. Several community projects have reverse-engineered portions of the protocol — search GitHub for `mylaps decoder protocol` and `AMB rc4 protocol`. Python implementations exist (used in open-source timing software for karting and RC) and can serve as implementation reference.
3. The protocol uses a binary frame format with a sync byte sequence, message type byte, payload length, payload, and checksum. Exact byte layout must be confirmed against hardware documentation or Wireshark captures.

**Action required before starting implementation:**
- Register at mylaps.com/developers and download the SDK documentation.
- If documentation is unavailable, capture Wireshark traces from the club's existing RCResults installation communicating with the decoder hardware.
- The Netty `ByteToMessageDecoder` implementation will be straightforward once the frame format is confirmed — the risk is in obtaining the specification, not in the Java implementation.

---

## Sources

- Training knowledge (cutoff August 2025) — confidence varies by area; see verification checklist above.
- All version numbers flagged MEDIUM or LOW confidence require verification at official sources before use.
- No live documentation was accessible during this research session (network tools restricted in environment).
