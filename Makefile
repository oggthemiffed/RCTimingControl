.DEFAULT_GOAL := help

# ── colours ──────────────────────────────────────────────────────────────────
BOLD  := \033[1m
RESET := \033[0m

# ── compose shim: prefer docker compose (v2 plugin), fall back to docker-compose (v1) ──
COMPOSE := $(shell \
  if docker compose version >/dev/null 2>&1; then \
    echo 'docker compose'; \
  elif command -v docker-compose >/dev/null 2>&1; then \
    echo 'docker-compose'; \
  else \
    echo ''; \
  fi)

# ─────────────────────────────────────────────────────────────────────────────
# Help
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: help
help:
	@printf '$(BOLD)RCTimingControl — common tasks$(RESET)\n\n'
	@printf '  $(BOLD)Infrastructure$(RESET)\n'
	@printf '    make up          Start Piper for announcer voices (optional; needs Docker)\n'
	@printf '    make down        Stop and remove containers\n'
	@printf '    make clean-db    Delete the dev SQLite database (app/data/db)\n'
	@printf '\n'
	@printf '  $(BOLD)Backend$(RESET)\n'
	@printf '    make dev         Start backend in dev mode (no Docker needed)\n'
	@printf '    make dev-setup-test  Start backend without seed data, to try the setup wizard\n'
	@printf '    make generate-db Regenerate jOOQ sources from the SQLite migrations\n'
	@printf '    make build       Compile the backend (regenerates jOOQ if sources missing)\n'
	@printf '    make test        Run all backend + simulator integration tests\n'
	@printf '    make test-fast   Run tests skipping jOOQ codegen\n'
	@printf '\n'
	@printf '  $(BOLD)Decoder simulator$(RESET)\n'
	@printf '    make simulator   Run the fake decoder simulator (generative mode)\n'
	@printf '    make simulator-playback  Replay a .dump file through the fake decoder\n'
	@printf '\n'
	@printf '  $(BOLD)Frontend$(RESET)\n'
	@printf '    make ui          Start Vite dev server\n'
	@printf '    make ui-build    Type-check + production bundle\n'
	@printf '    make ui-lint     Run ESLint\n'
	@printf '\n'
	@printf '  $(BOLD)Combined$(RESET)\n'
	@printf '    make dev-start   Full dev environment: backend + frontend (+ Piper if Docker)\n'
	@printf '    make start       Same as dev-start (background; logs to /tmp/rc-*.log)\n'
	@printf '    make stop        Kill backend, frontend and docker containers\n'
	@printf '    make clean       Stop everything and wipe build artefacts\n'
	@printf '    make installer   Native installer for this system (msi, pkg or deb)\n'

# ─────────────────────────────────────────────────────────────────────────────
# Infrastructure
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: up
up:
	@if [ -z "$(COMPOSE)" ]; then \
		printf 'No Docker Compose found — skipping Piper, so announcer voices are off.\n'; \
	else \
		$(COMPOSE) up -d; \
	fi

.PHONY: down
down:
	@if [ -n "$(COMPOSE)" ]; then $(COMPOSE) down; fi

# The dev database is a SQLite file under app/data; the backend recreates it on the next start
.PHONY: clean-db
clean-db:
	rm -rf app/data/db app/data/db-setup-test
	@printf 'Dev database deleted. The next "make dev" migrates and seeds a fresh one.\n'

# ─────────────────────────────────────────────────────────────────────────────
# Backend
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: dev
dev:
	./gradlew :app:bootRun --args='--spring.profiles.active=dev'

# Runs backend without the dev seed data — use this when testing the setup wizard from scratch
.PHONY: dev-setup-test
dev-setup-test:
	./gradlew :app:bootRun --args='--spring.profiles.active=setup-test'

.PHONY: generate-db
generate-db:
	./gradlew :app:generateJooq

JOOQ_GENERATED := app/build/generated-sources/jooq/dev/monkeypatch/rctiming/jooq/generated

.PHONY: build
build:
	@if [ ! -d "$(JOOQ_GENERATED)" ]; then \
		printf 'jOOQ sources missing — running generateJooq first…\n'; \
		$(MAKE) generate-db; \
	fi
	./gradlew :app:build -x test -x generateJooq

.PHONY: test
test:
	./gradlew :app:test :decoder-simulator:test

.PHONY: test-fast
test-fast:
	./gradlew :app:test :decoder-simulator:test -x generateJooq

# ─────────────────────────────────────────────────────────────────────────────
# Decoder simulator
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: simulator
simulator:
	./gradlew :decoder-simulator:runSimulator --args='--mode=generative --transponders=101,102,103,104,105,106 --interval-ms=12500 --jitter-ms=2500'

DUMP_FILE ?= decoder-simulator/src/main/resources/samples/sample-passings.dump
.PHONY: simulator-playback
simulator-playback:
	./gradlew :decoder-simulator:runSimulator --args='--mode=playback --file=$(DUMP_FILE)'

# ─────────────────────────────────────────────────────────────────────────────
# Frontend
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: ui
ui:
	cd frontend && npm run dev

.PHONY: ui-build
ui-build:
	cd frontend && npm run build

.PHONY: ui-lint
ui-lint:
	cd frontend && npm run lint

# ─────────────────────────────────────────────────────────────────────────────
# Packaging
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: installer
installer:
	./gradlew -PbundleFrontend :app:installer -x generateJooq
	@printf 'Installer written to app/build/installer/out/\n'

# ─────────────────────────────────────────────────────────────────────────────
# Combined
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: dev-start
dev-start: start

.PHONY: start
start: up
	@if [ ! -d "$(JOOQ_GENERATED)" ]; then \
		printf 'jOOQ sources missing — running generateJooq first…\n'; \
		$(MAKE) generate-db; \
	fi
	@printf 'Starting backend (log: /tmp/rc-backend.log)…\n'
	@PROFILE=$$([ "$(SEED)" = "no" ] && echo "setup-test" || echo "dev"); \
	  ./gradlew :app:bootRun --no-daemon --args="--spring.profiles.active=$$PROFILE" -x generateJooq \
		> /tmp/rc-backend.log 2>&1 & echo $$! > /tmp/rc-backend.pid
	@printf 'Starting frontend (log: /tmp/rc-frontend.log)…\n'
	@cd frontend && npm run dev > /tmp/rc-frontend.log 2>&1 & echo $$! > /tmp/rc-frontend.pid
	@printf 'Services starting — backend on :8080, frontend on :5173\n'
	@printf 'The app reads the AMB decoder directly. Set its host and port in Setup.\n'
	@printf 'Run "make simulator" to use the fake decoder instead.\n'
	@printf 'Logs: tail -f /tmp/rc-backend.log  |  tail -f /tmp/rc-frontend.log\n'

.PHONY: stop
stop:
	@if [ -f /tmp/rc-backend.pid ]; then \
		kill $$(cat /tmp/rc-backend.pid) 2>/dev/null || true; rm /tmp/rc-backend.pid; \
	fi
	@if [ -f /tmp/rc-frontend.pid ]; then \
		kill $$(cat /tmp/rc-frontend.pid) 2>/dev/null || true; rm /tmp/rc-frontend.pid; \
	fi
	@timeout 10 ./gradlew --stop >/dev/null 2>&1 || true
	@pkill -f '[n]ode.*vite' 2>/dev/null || true
	@$(MAKE) down
	@printf 'Services stopped.\n'

.PHONY: clean
clean: stop
	./gradlew clean
	rm -rf frontend/dist
	@$(MAKE) down
