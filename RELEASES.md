# Release Process

This document describes how to cut a release of RCTimingControl.

## Overview

Releases are driven by git tags. Pushing a `v*` tag to GitHub automatically:
1. Builds and publishes three Docker images to GHCR
2. Creates a GitHub Release with `docker-compose.ghcr.yml` and `.env.example` attached
3. Marks the release as pre-release if the version starts with `0.` (i.e. `0.x.x`)

The `VERSION` file in the repo root is the single source of truth. It controls the Gradle build version, the Spring Boot build-info (displayed on the About page), and the default `RCTIMING_VERSION` in `.env.example`.

---

## Upgrade notes

### The release after 0.1.x: the database moved to SQLite (#26)

The app now keeps its data in a single SQLite file instead of a PostgreSQL server. There is no migration path: an existing PostgreSQL database cannot be upgraded and its data is not carried over.

- Start from an empty database. With Docker, remove the old stack and its volumes (`docker compose -f <compose file> down -v`) before starting the new version.
- The `postgres` and `demo-seed` services and `POSTGRES_PASSWORD` are gone. The database lives in the `app_db` volume (`RCTIMING_DATA_DIR`, default the user's app-data folder outside Docker).
- The trial stack loads its demo club through the app's `demo` profile rather than a seed container.
- Developers: delete any old local database with `make clean-db`.

---

## Step-by-step release checklist

### 1. Decide the version number

Follow [Semantic Versioning](https://semver.org/):

| Change | Example |
|--------|---------|
| Bug fixes only | `0.1.0` → `0.1.1` |
| New features, backwards-compatible | `0.1.0` → `0.2.0` |
| Breaking changes | `0.x.x` → `1.0.0` |

While version is `0.x.x` the software is considered **pre-release** and GitHub will mark it accordingly.

### 2. Update the VERSION file

```bash
echo "0.2.0" > VERSION
```

### 3. Update .env.example

Edit `.env.example` and set `RCTIMING_VERSION=v0.2.0` to match.

### 4. Commit

```bash
git add VERSION .env.example
git commit -m "chore: bump version to 0.2.0"
```

### 5. Verify CI is green

Check that the CI workflow passes on `main` before tagging:
https://github.com/oggthemiffed/RCTimingControl/actions

### 6. Tag and push

```bash
git tag v0.2.0
git push origin main
git push origin v0.2.0
```

### 7. Wait for the release workflow

GitHub Actions will:
- Build all 5 Docker images and push to GHCR (5–10 minutes)
- Create a GitHub Release at https://github.com/oggthemiffed/RCTimingControl/releases
- Attach `docker-compose.ghcr.yml` and `.env.example` to the release

Monitor progress at:
https://github.com/oggthemiffed/RCTimingControl/actions/workflows/publish-trial-images.yml

### 8. Verify the release

Once the workflow finishes:
- Open the release page and confirm the assets are attached
- Pull the new images locally and test:
  ```bash
  cp .env.example .env
  # Set RCTIMING_VERSION=v0.2.0 in .env
  docker compose -f docker-compose.ghcr.yml up
  ```
- Open http://localhost and confirm the About page shows `v0.2.0`

---

## Hotfix releases

For urgent bug fixes on a tagged release:

```bash
# Branch from the tag
git checkout -b hotfix/0.1.1 v0.1.0

# Fix the bug
# ... make changes ...

git add .
git commit -m "fix: description of the bug"

# Bump VERSION
echo "0.1.1" > VERSION
git add VERSION .env.example
git commit -m "chore: bump version to 0.1.1"

# Tag and push
git tag v0.1.1
git push origin hotfix/0.1.1
git push origin v0.1.1

# Merge the fix back into main
git checkout main
git merge hotfix/0.1.1
git push origin main
```

---

## What the CI/CD pipeline does

### On every push / pull request (`ci.yml`)

| Job | What it runs |
|-----|-------------|
| `test-backend` | Gradle test suite — JUnit 5 on temporary SQLite databases (Java 21, no Docker); also runs `decoder-simulator` and `decoder-protocol` |
| `test-frontend` | Vitest unit tests (Node 20) |
| `test-e2e` | Playwright smoke tests against the full `docker-compose.trial.yml` stack |

### On `v*` tag push (`publish-trial-images.yml`)

| Job | What it does |
|-----|-------------|
| `build-and-push` | Builds 3 Docker images and publishes them to GHCR tagged with the version |
| `create-release` | Creates a GitHub Release with install instructions and file assets |

---

## GHCR image locations

| Image | Registry path |
|-------|---------------|
| App (Spring Boot) | `ghcr.io/oggthemiffed/rctimingcontrol/app:<version>` |
| Frontend (nginx) | `ghcr.io/oggthemiffed/rctimingcontrol/frontend:<version>` |
| Fake decoder | `ghcr.io/oggthemiffed/rctimingcontrol/fake-decoder:<version>` |

Images are tagged with the exact semver (e.g. `0.1.0`) without the `v` prefix. There is no `latest` tag — consumers must pin to a specific version via `RCTIMING_VERSION` in `.env`.
