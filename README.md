# RCTimingControl

Web-based RC club management and race timing system. Replaces RCResults with a modern browser-based race control client and self-service racer portal.

## What's here

| Component | Description |
|-----------|-------------|
| `app/` | Spring Boot 3.4 backend — REST API, JWT auth, WebSocket timing hub |
| `frontend/` | React 18 + Vite + Tailwind + shadcn/ui |
| `forwarder/` | Separate module — connects to AMB/MyLaps decoder hardware |
| `docker-compose.yml` | PostgreSQL 16 + Mailpit (dev email) |

## Quick start

**Prerequisites:** Java 21, Docker, Node 20+

```bash
# 1. Start dev infrastructure
docker compose up -d

# 2. Run the backend (auto-runs Flyway migrations)
./gradlew :app:bootRun --args='--spring.profiles.active=dev'

# 3. Run the frontend (separate terminal)
cd frontend && npm install && npm run dev
```

Backend: `http://localhost:8080`  
Frontend: `http://localhost:5173`  
Mailpit (catch-all email): `http://localhost:8025`

## Running tests

```bash
# Unit tests only (no Docker needed)
./gradlew :app:test --tests "dev.monkeypatch.rctiming.domain.*"

# Full integration tests (requires Docker for Testcontainers)
./gradlew :app:test
```

## Current state — Phase 1 complete

- Flyway schema: users, auth tokens, club, tracks, racing classes, race formats (JSONB)
- JWT auth: register, login, token refresh (HttpOnly cookie), password reset
- Admin APIs: club profile, tracks + decoder loops, racing classes, race format templates (JSON/YAML export/import)
- Frontend: auth screens (login, register, forgot/reset password), protected routing, silent token refresh

Phases 2–7 (Racer Portal → Results & Championship) are planned. See `.planning/ROADMAP.md`.

## Docs

- [API reference](docs/api.md) — all endpoints with example requests
- [Development guide](docs/development.md) — environment setup, config, env vars
- [Architecture](docs/architecture.md) — module structure, design decisions
