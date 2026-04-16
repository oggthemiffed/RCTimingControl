# Phase 1: Domain Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-16
**Phase:** 01-domain-foundation
**Areas discussed:** Build tool, Project structure, Admin UI scope, Format config modeling

---

## Build Tool

| Option | Description | Selected |
|--------|-------------|----------|
| Gradle Kotlin DSL | Type-safe build scripts, good multi-module support, Spring Boot + jOOQ codegen plugins | ✓ |
| Maven | XML POM, more common in enterprise; multi-module more boilerplate | |

**User's choice:** Gradle Kotlin DSL

---

## Build Tool — Module structure

| Option | Description | Selected |
|--------|-------------|----------|
| Multi-module from day one | :app + :forwarder stub in settings.gradle.kts; avoids restructuring later | ✓ |
| Single module for now | Just :app until Phase 5; simpler initially | |

**User's choice:** Multi-module from day one

---

## Project Structure — Package root

| Option | Description | Selected |
|--------|-------------|----------|
| dev.monkeypatch.rctiming | Matches user's email domain | ✓ |
| com.rctimingcontrol | Generic product-named root | |
| io.github.{username} | GitHub-style open source root | |

**User's choice:** dev.monkeypatch.rctiming

---

## Project Structure — Internal package layout

| Option | Description | Selected |
|--------|-------------|----------|
| By layer | domain/, api/, query/, security/, config/ | ✓ |
| By feature slice | auth/, club/, track/, format/ etc. | |

**User's choice:** By layer

---

## Project Structure — Frontend location

| Option | Description | Selected |
|--------|-------------|----------|
| frontend/ at repo root | Independent Vite project, clean separation | ✓ |
| app/src/main/resources/frontend/ | Frontend inside Gradle module, tighter coupling | |

**User's choice:** frontend/ at repo root

---

## Project Structure — Local Postgres

| Option | Description | Selected |
|--------|-------------|----------|
| Docker Compose at repo root | postgres:16, reproducible, no local install needed | ✓ |
| Local Postgres install | Environment-specific, version inconsistency risk | |

**User's choice:** Docker Compose at repo root

---

## Admin UI Scope

| Option | Description | Selected |
|--------|-------------|----------|
| Auth UI + React scaffold only | Login/register/reset screens; admin forms in Phase 3; APIs verified by tests | ✓ |
| Full admin config UI | Club, track, format forms in Phase 1 too | |
| No frontend in Phase 1 | Backend only | |

**User's choice:** Auth UI + React scaffold only

---

## Admin UI — Password reset email

| Option | Description | Selected |
|--------|-------------|----------|
| Spring JavaMailSender + SMTP | Config-driven, MailPit in dev, any relay in prod | ✓ |
| SendGrid SDK | Provider SDK lock-in from day one | |
| Stub only | Token in API response, email wired later | |

**User's choice:** Spring JavaMailSender + SMTP

---

## Format Config — Java modeling

| Option | Description | Selected |
|--------|-------------|----------|
| Sealed interface + Jackson polymorphism | @JsonTypeInfo + @JsonSubTypes, record subtypes, exhaustive switch | ✓ |
| Single entity with nullable fields | Flat config, nullable for irrelevant fields | |
| Separate entity per type | Three JPA entities, union queries | |

**User's choice:** Sealed interface + Jackson polymorphism

---

## Format Config — Import/export format

| Option | Description | Selected |
|--------|-------------|----------|
| Both JSON and YAML | jackson-dataformat-yaml, auto-detect from Content-Type | ✓ |
| YAML only | Diverges from FORMAT-14 spec | |
| JSON only | Matches FORMAT-14 as written | |

**User's choice:** Both JSON and YAML
**Notes:** User noted YAML is nicer for hand-authoring configs.

---

## Format Config — Hibernate JSONB storage

| Option | Description | Selected |
|--------|-------------|----------|
| Hypersistence Utils | @Type(JsonBinaryType.class), direct mapping | ✓ |
| String column + manual Jackson | ObjectMapper in service, no extra dep | |

**User's choice:** Hypersistence Utils

---

## Format Config — Override patches (FORMAT-07)

| Option | Description | Selected |
|--------|-------------|----------|
| Snapshot + second JSONB override column | config_snapshot + config_override on EventClass, merge at read | ✓ |
| Copy-on-assign + mutate | Full copy mutated for overrides | |
| Claude's discretion | Planner decides | |

**User's choice:** Snapshot + second JSONB override column
**Notes:** User asked about schema evolution risk — confirmed understanding that template changes don't affect existing snapshots (FORMAT-06 guarantee), and that field renames require Flyway data migrations. Accepted this approach with that understanding.

---

## Claude's Discretion

- Flyway migration numbering scheme
- REST API URL structure (prefix or not)
- application.yml profile structure for dev/prod/test

## Deferred Ideas

- Full admin config UI (club/track/format forms) — Phase 3
- Forwarder implementation — Phase 5
- Multi-decoder operation — post-v1
