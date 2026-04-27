# SECURITY.md — Phase 05: Live Timing Forwarder

**Generated:** 2026-04-26
**Phase:** 05 — live-timing-forwarder (Plans 01–05)
**ASVS Level:** 1
**Block on:** open_mitigate_threats

---

## Threat Verification

| Threat ID | Category | Disposition | Status | Evidence |
|-----------|----------|-------------|--------|----------|
| T-05-01 | Tampering | mitigate | CLOSED | All Wave-0 test method bodies contain `Assertions.fail(...)`. Confirmed by 05-01-SUMMARY.md: "all 9 files compile and skip cleanly" with fail-by-default bodies. |
| T-05-02 | Information Disclosure | accept | CLOSED | Accepted risk logged below. No data handled in Wave-0 test stubs. |
| T-05-03 | Tampering | mitigate | CLOSED | `Rc4TextParser.java:50` — `NumberFormatException` caught, returns `Optional.empty()`. `Rc4TextParser.java:40` — `f.length < 9` guard. All malformed/wrong-field-count paths return empty. `Rc4InboundHandler.java:76` — `exceptionCaught` logs and calls `ctx.close()`. |
| T-05-04 | Denial of Service | mitigate | CLOSED | `AmbRc4TimingSource.java:108` — pipeline includes `new LineBasedFrameDecoder(1024)` enforcing max frame length of 1024 bytes. |
| T-05-05 | Information Disclosure | mitigate | CLOSED | `ForwarderApplication.java:29` — `log.info("apiToken={}", cfg.apiToken() != null && !cfg.apiToken().isBlank() ? "<set>" : "<unset>")`. Token value never logged. |
| T-05-06 | Denial of Service | mitigate | CLOSED | `AmbRc4TimingSource.java:145` — `Math.min(30L, 1L << Math.min(backoffIdx - 1, 4))` caps delay at 30s. Backoff sequence 1, 2, 4, 8, 16, 30, 30... verified. |
| T-05-07 | Spoofing | accept | CLOSED | Accepted risk logged below. Simulator logs `[SIMULATOR] DEV-ONLY — do not run on production hosts` banner at `FakeDecoderServer.java:81`. |
| T-05-08 | Spoofing | mitigate | CLOSED | `ForwarderTokenService.java:39` — `SecureRandom` 32-byte → Base64URL (43-char); `ForwarderTokenService.java:41` — `passwordEncoder.encode(plaintext)` stores BCrypt hash. |
| T-05-09 | Tampering | mitigate | CLOSED | `ForwarderTokenService.java:58` — `repo.findAllByStatus(ACTIVE).stream().filter(...)` — only ACTIVE tokens are ever matched during validation. |
| T-05-10 | Information Disclosure | mitigate | CLOSED | `ForwarderTokenController.java:43` — POST response returns `ForwarderTokenGenerateResponseDto(r.plaintext(), ...)`. GET returns `ForwarderTokenStatusDto` which has fields `(String status, Instant generatedAt, Instant revokedAt)` — no token field. |
| T-05-11 | Information Disclosure | accept | CLOSED | Accepted risk logged below. `ForwarderTokenController.java` Javadoc: "HTTPS strongly recommended in production; one-time-reveal pattern means token plaintext is in HTTP response body (T-05-11 accepted risk — venue LAN v1 deployment)." |
| T-05-12 | Elevation of Privilege | mitigate | CLOSED | `ForwarderTokenController.java:22` — `@PreAuthorize("hasRole('ADMIN')")` on class level. |
| T-05-13 | Denial of Service | accept | CLOSED | Accepted risk logged below. |
| T-05-14 | Spoofing | mitigate | CLOSED | `ForwarderTokenAuthInterceptor.java:35-39` — validates `x-forwarder-token` via `tokenService.validate(token).isEmpty()`; closes stream with `Status.UNAUTHENTICATED` before business logic runs. |
| T-05-15 | Spoofing | mitigate | CLOSED | `ForwarderTokenService.java:58` — `repo.findAllByStatus(ACTIVE)` — revoked tokens are never returned, replay of revoked token fails on every reconnect. |
| T-05-16 | Tampering | mitigate | CLOSED | `TransponderLinkController.java:67-68` — persists `UnknownTransponderLinkAudit(raceId, transponderNumber, entryId, userId)` with `linkedAt = Instant.now()` on every link operation. |
| T-05-17 | Denial of Service | mitigate | CLOSED | `ForwarderTokenAuthInterceptor.java:35-39` — auth check occurs in the interceptor before `StreamObserver` is allocated in `ForwarderGrpcService.streamPassings()`. Unauthenticated connections are rejected without entering the service layer. |
| T-05-18 | Elevation of Privilege | mitigate | CLOSED | `TransponderLinkController.java:44` — `@PreAuthorize("hasAnyRole('RACE_DIRECTOR', 'ADMIN')")` on the POST mapping. |
| T-05-19 | Information Disclosure | accept | CLOSED | Accepted risk logged below. |
| T-05-20 | Tampering | accept | CLOSED | Accepted risk logged below. `ForwarderGrpcClient.java` Javadoc: "T-05-20: Token transmitted without TLS on venue LAN (accepted risk for v1). For production: set grpcPlaintext=false and provision TLS certificates." |
| T-05-05-01 | Information Disclosure | mitigate | CLOSED | `ForwarderTokenPage.tsx:59-61` — `handleDone()` calls `setNewToken(null)`, clearing token from React state. No `localStorage` usage found. Token shown only during the `newToken` render branch. |
| T-05-05-02 | Spoofing | mitigate | CLOSED | `TransponderLinkController.java:44` — `@PreAuthorize("hasAnyRole('RACE_DIRECTOR', 'ADMIN')")` enforced server-side; frontend makes authenticated requests. |
| T-05-05-03 | Tampering | accept | CLOSED | Accepted risk logged below. |
| T-05-05-04 | Denial of Service | accept | CLOSED | Accepted risk logged below. |

**Total: 24/24 threats closed.**

---

## Accepted Risks Log

| Threat ID | Category | Rationale |
|-----------|----------|-----------|
| T-05-02 | Information Disclosure | No data handled in Wave-0 test stubs; stubs contain no production data paths. |
| T-05-07 | Spoofing | Simulator is a dev-only tool. It logs a `[SIMULATOR] DEV-ONLY — do not run on production hosts` banner (`FakeDecoderServer.java:81`). Not deployed in production. |
| T-05-11 | Information Disclosure | v1 venue LAN deployment. Token plaintext is transmitted in HTTP response body once. Controller Javadoc notes HTTPS required for production. Risk accepted by design decision D-08/D-09. |
| T-05-13 | Denial of Service | Repeated token generation via POST is ADMIN-only and not internet-facing. No rate limit required in v1. |
| T-05-19 | Information Disclosure | Transponder IDs were already broadcast to race control clients in Phase 4. The forwarder introduces no new exposure surface. |
| T-05-20 | Tampering | gRPC token transmitted without TLS on venue LAN. Accepted for v1. Production upgrade path: set `grpcPlaintext=false` and provision TLS certificates. Documented in `ForwarderGrpcClient` Javadoc. |
| T-05-05-03 | Tampering | Token clipboard contents are the user's responsibility. No frontend persistence mechanism involved. |
| T-05-05-04 | Denial of Service | Token revoke is an admin-only action requiring an existing authenticated session. Low risk in v1. |

---

## Unregistered Threat Flags

The `05-05-SUMMARY.md ## Threat Flags` section records one flag:

| Flag | Source File | Description | Mapping |
|------|-------------|-------------|---------|
| `threat_flag: token-display` | `ForwarderTokenPage.tsx` | One-time token reveal in browser | Maps to T-05-05-01 (mitigate, CLOSED) |

No unregistered flags without an existing threat ID.

---

## Implementation Notes

- `ForwarderTokenAuthInterceptor` implements the auth check at interceptor level (not service level), satisfying T-05-17's requirement that StreamObserver resource allocation is gated behind auth.
- The `ForwarderTokenStatusDto` record has fields `(String status, Instant generatedAt, Instant revokedAt)` with no token or hash field, satisfying T-05-10.
- Plan 04 introduced a naming deviation: the audit entity is `UnknownTransponderLinkAudit` (table: `unknown_transponder_link` singular) rather than `UnknownTransponderLink` as specified, due to a pre-existing Phase 4 entity with the same class name. The T-05-16 mitigation is fully satisfied by the implemented entity.
- `ForwarderStatusPublisher` deviates from the plan's two-method design (`onDecoderReconnecting` replaced by `onDecoderStatus(String)`) to support the `ReportStatus` gRPC RPC added during Plan 04 implementation. This does not affect any threat mitigation.
