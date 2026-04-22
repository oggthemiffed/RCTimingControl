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
	@printf '    make build       Compile the backend (skips jOOQ codegen if schema unchanged)\n'
	@printf '    make test        Run all backend integration tests\n'
	@printf '    make test-fast   Run tests skipping jOOQ codegen\n'
	@printf '\n'
	@printf '  $(BOLD)Frontend$(RESET)\n'
	@printf '    make ui          Start Vite dev server\n'
	@printf '    make ui-build    Type-check + production bundle\n'
	@printf '    make ui-lint     Run ESLint\n'
	@printf '\n'
	@printf '  $(BOLD)Combined$(RESET)\n'
	@printf '    make dev-start   Full dev environment: docker + backend + frontend\n'
	@printf '    make start       Same as dev-start (background; logs to /tmp/rc-*.log)\n'
	@printf '    make stop        Kill backend, frontend and docker containers\n'
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

.PHONY: build
build:
	./gradlew :app:build -x test -x generateJooq

.PHONY: test
test:
	./gradlew :app:test

.PHONY: test-fast
test-fast:
	./gradlew :app:test -x generateJooq

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
	@printf 'Services starting — backend on :8080, frontend on :5173\n'
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
	@docker compose down
	@printf 'Services stopped.\n'

.PHONY: clean
clean: stop
	./gradlew clean
	rm -rf frontend/dist
	docker compose down
