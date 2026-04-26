.DEFAULT_GOAL := help

# ── colours ──────────────────────────────────────────────────────────────────
BOLD  := \033[1m
RESET := \033[0m

# ─────────────────────────────────────────────────────────────────────────────
# Help
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: help
help:
	@printf '$(BOLD)RCTimingControl — common tasks$(RESET)\n\n'
	@printf '  $(BOLD)Infrastructure$(RESET)\n'
	@printf '    make up          Start PostgreSQL + Mailpit (docker compose up -d)\n'
	@printf '    make down        Stop and remove containers\n'
	@printf '    make clean-db    Drop the pgdata volume and restart fresh\n'
	@printf '\n'
	@printf '  $(BOLD)Backend$(RESET)\n'
	@printf '    make dev         Start backend in dev mode (requires: make up)\n'
	@printf '    make generate-db Regenerate jOOQ sources from live schema (requires: make up)\n'
	@printf '    make build       Compile the backend (regenerates jOOQ if sources missing)\n'
	@printf '    make test        Run all backend + forwarder integration tests\n'
	@printf '    make test-fast   Run tests skipping jOOQ codegen\n'
	@printf '\n'
	@printf '  $(BOLD)Forwarder$(RESET)\n'
	@printf '    make forwarder   Build and run the forwarder (connects to live AMB decoder)\n'
	@printf '    make simulator   Run the fake decoder simulator (generative mode)\n'
	@printf '    make simulator-playback  Replay a .dump file through the fake decoder\n'
	@printf '    make forwarder-build     Compile the forwarder module only\n'
	@printf '    make forwarder-test      Run forwarder unit + integration tests\n'
	@printf '\n'
	@printf '  $(BOLD)Frontend$(RESET)\n'
	@printf '    make ui          Start Vite dev server\n'
	@printf '    make ui-build    Type-check + production bundle\n'
	@printf '    make ui-lint     Run ESLint\n'
	@printf '\n'
	@printf '  $(BOLD)Combined$(RESET)\n'
	@printf '    make dev-start   Full dev environment: docker + backend + frontend\n'
	@printf '    make start       Same as dev-start (background; logs to /tmp/rc-*.log)\n'
	@printf '    make stop        Kill backend, frontend, forwarder and docker containers\n'
	@printf '    make clean       Stop everything and wipe build artefacts\n'

# ─────────────────────────────────────────────────────────────────────────────
# Infrastructure
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: up
up:
	docker compose up -d

.PHONY: down
down:
	docker compose down

.PHONY: clean-db
clean-db:
	docker compose down -v
	docker compose up -d

# ─────────────────────────────────────────────────────────────────────────────
# Backend
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: dev
dev:
	./gradlew :app:bootRun --args='--spring.profiles.active=dev'

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
	./gradlew :app:test :forwarder:test

.PHONY: test-fast
test-fast:
	./gradlew :app:test :forwarder:test -x generateJooq

# ─────────────────────────────────────────────────────────────────────────────
# Forwarder
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: forwarder-build
forwarder-build:
	./gradlew :forwarder:build -x test

.PHONY: forwarder-test
forwarder-test:
	./gradlew :forwarder:test

.PHONY: forwarder
forwarder:
	./gradlew :forwarder:run

.PHONY: simulator
simulator:
	./gradlew :forwarder:runSimulator

DUMP_FILE ?= forwarder/src/main/resources/samples/sample-passings.dump
.PHONY: simulator-playback
simulator-playback:
	./gradlew :forwarder:runSimulator --args='--mode=playback --file=$(DUMP_FILE)'

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
# Combined
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: dev-start
dev-start: start

.PHONY: start
start: up
	@printf 'Starting backend (log: /tmp/rc-backend.log)…\n'
	@./gradlew :app:bootRun --no-daemon --args='--spring.profiles.active=dev' -x generateJooq \
		> /tmp/rc-backend.log 2>&1 & echo $$! > /tmp/rc-backend.pid
	@printf 'Starting frontend (log: /tmp/rc-frontend.log)…\n'
	@cd frontend && npm run dev > /tmp/rc-frontend.log 2>&1 & echo $$! > /tmp/rc-frontend.pid
	@printf 'Services starting — backend on :8080, frontend on :5173, gRPC on :9090\n'
	@printf 'Run "make forwarder" in a separate terminal to connect the AMB decoder.\n'
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
	@if [ -f /tmp/rc-forwarder.pid ]; then \
		kill $$(cat /tmp/rc-forwarder.pid) 2>/dev/null || true; rm /tmp/rc-forwarder.pid; \
	fi
	@timeout 10 ./gradlew --stop >/dev/null 2>&1 || true
	@pkill -f '[n]ode.*vite' 2>/dev/null || true
	@docker compose down
	@printf 'Services stopped.\n'

.PHONY: clean
clean: stop
	./gradlew clean
	rm -rf frontend/dist
	docker compose down
