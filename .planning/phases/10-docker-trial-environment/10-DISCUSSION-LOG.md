# Phase 10: Docker Trial Environment — Discussion Log

**Date:** 2026-05-16
**Format:** Interactive discuss-phase (default mode)

---

## Areas Discussed

### Frontend Serving
**Question:** How should the frontend be served in the trial stack?
**Options presented:** nginx container / Embedded in Spring Boot jar / Separate Vite preview server
**Selected:** nginx container
**Notes:** Single exposed port (80), most production-like, standard pattern for SPAs.

### Seed Data
**Question:** What scenario should the demo seed data represent?
**Options presented:** Active club mid-season / Single completed event / Minimal skeleton
**Selected:** Active club mid-season
**Notes:** 2 tracks, 3 formats, 8 racers, 6-round championship 3 rounds complete, upcoming event with entries. Gives evaluators the most to explore.

### Fake Decoder
**Question:** Where does the fake decoder replay file come from?
**Options presented:** Bundled synthetic .txt file / Bundled capture from real hardware / Procedurally generated at container start
**Selected:** Bundled synthetic .txt file
**Notes:** 8 transponder IDs matching seeded entries, 3-minute loop. Self-contained, no real data needed.

### GHCR Publish
**Question:** When should GHCR images be published?
**Options presented:** On version tags only / On every push to master + tags / Manual trigger only
**Selected:** On version tags only
**Notes:** Stable, tested builds. docker-compose.ghcr.yml pins to a tag.

### Port
**Question:** What port should the trial environment be accessible on?
**Options presented:** Port 80 via nginx / Port 8080 direct / Configurable (default 80)
**Selected:** Port 80 via nginx
**Notes:** `http://localhost` with no port number — most user-friendly for demos. HOST_PORT env var for overrides.

### Compose File
**Question:** Should the trial stack reuse the same docker-compose.yml or be a separate file?
**Options presented:** Separate docker-compose.trial.yml / Extend existing / Replace entirely
**Selected:** Separate docker-compose.trial.yml
**Notes:** Dev compose stays untouched. Trial stack is self-contained.

---

## Claude's Discretion Items

- Dockerfile base images
- nginx config specifics
- Health check intervals
- BuildKit cache mount usage
- Gradle build configuration for Docker

## Deferred Ideas

None.
