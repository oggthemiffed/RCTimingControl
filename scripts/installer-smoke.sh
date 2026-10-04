#!/usr/bin/env bash
# Checks an installed RC Timing package from the outside (#23), as a phone on the network would.
#   installer-smoke.sh fresh     after the first install: UI, API and a first official account
#   installer-smoke.sh upgraded  after installing a newer package: the account is still there
set -euo pipefail

BASE="${RCTIMING_URL:-http://localhost:8080}"
EMAIL="smoke@example.com"
PASSWORD="smoke-test-password"

echo "Waiting for the service at $BASE..."
for _ in $(seq 1 90); do
  if curl -sf "$BASE/actuator/health" | grep -q UP; then
    break
  fi
  sleep 2
done
curl -sf "$BASE/actuator/health" | grep -q UP || { echo "The service did not start"; exit 1; }

echo "The UI is served from the app, including deep links"
curl -sf "$BASE/" | grep -qi "<div id=\"root\""
curl -sf "$BASE/race-control" | grep -qi "<div id=\"root\""

echo "About lists addresses for other devices"
about=$(curl -sf "$BASE/api/v1/about")
echo "$about"
echo "$about" | grep -q '"addresses":\["http://'

login() {
  curl -sf -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
}

case "${1:-}" in
  fresh)
    echo "Creating the first official"
    curl -sf -X POST "$BASE/api/v1/setup/bootstrap" -H 'Content-Type: application/json' \
      -d "{\"firstName\":\"Smoke\",\"lastName\":\"Test\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
      | grep -q accessToken
    ;;
  upgraded)
    echo "The data survived the upgrade"
    curl -sf "$BASE/api/v1/setup/status" | grep -q '"bootstrapped":true'
    ;;
  *)
    echo "usage: $0 fresh|upgraded" >&2
    exit 2
    ;;
esac

echo "The official can sign in"
login | grep -q accessToken
echo "OK"
