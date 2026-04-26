---
phase: 05
slug: live-timing-forwarder
status: verified
threats_open: 0
asvs_level: 1
created: 2026-04-26
---

# Phase 05 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Decoder TCP → Forwarder | Untrusted bytes from on-LAN decoder hardware; malformed frames possible | RC-4 text frames |
| Simulator TCP → Forwarder | Same trust class as real decoder (dev/test only) | RC-4 text frames |
| Properties file → Forwarder process | Operator-provided config; api-token is sensitive | API token (sensitive) |
| Browser (admin) → REST controller | Authenticated admin session; ADMIN role required | Token plaintext (one-time) |
| HTTP response body (one-time reveal) | Token plaintext crosses once on POST response | Token plaintext |
| Forwarder process → gRPC server (port 9090) | Untrusted on-site connection; must present valid API token | API token in metadata, LapPassing stream |
| gRPC metadata (x-forwarder-token) | Token plaintext on venue LAN wire; TLS deferred for v1 | API token plaintext |
| Race director browser → POST transponder link | Authenticated staff request; RACE_DIRECTOR or ADMIN required | Entry/transponder linkage |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-05-01 | Tampering | Wave-0 test stubs | mitigate | All stub bodies call `Assertions.fail(...)` — missing `@Disabled` causes immediate red | closed |
| T-05-02 | Information Disclosure | Wave-0 test stubs | accept | No production data handled in test stubs | closed |
| T-05-03 | Tampering | `Rc4TextParser` malformed input | mitigate | `f.length < 9` guard + `NumberFormatException` → `Optional.empty()`; `exceptionCaught` closes channel | closed |
| T-05-04 | Denial of Service | Infinite-length RC-4 line | mitigate | `LineBasedFrameDecoder(1024)` in Netty pipeline — frames >1024 bytes discarded | closed |
| T-05-05 | Information Disclosure | API token logged on startup | mitigate | `ForwarderApplication` logs `apiToken=<set>` or `<unset>` — never the value | closed |
| T-05-06 | Denial of Service | Reconnect storm against decoder | mitigate | Exponential backoff capped at 30 s (`Math.min(30L, 1L << ...)`) | closed |
| T-05-07 | Spoofing | Simulator LAN port open | accept | Dev-only tool; startup banner `[SIMULATOR] DEV-ONLY — do not run on production hosts` | closed |
| T-05-08 | Spoofing | Forwarder impersonation via stolen token | mitigate | 256-bit `SecureRandom` (43-char Base64URL); stored as BCrypt hash in DB | closed |
| T-05-09 | Tampering | Replay of revoked token | mitigate | `ForwarderTokenService.validate` only iterates `findAllByStatus(ACTIVE)` — revoked never matched | closed |
| T-05-10 | Information Disclosure | Token plaintext exposure | mitigate | Plaintext in POST response only; `ForwarderTokenStatusDto` has no token field | closed |
| T-05-11 | Information Disclosure | Token on venue LAN (no HTTPS) | accept | v1 venue LAN deployment; Javadoc notes HTTPS required for production | closed |
| T-05-12 | Elevation of Privilege | Non-ADMIN token endpoints | mitigate | `@PreAuthorize("hasRole('ADMIN')")` class-level on `ForwarderTokenController` | closed |
| T-05-13 | Denial of Service | Repeated token generation | accept | ADMIN-only; not internet-facing; no rate limit needed in v1 | closed |
| T-05-14 | Spoofing | Forwarder impersonation (absent token) | mitigate | `ForwarderTokenAuthInterceptor` validates every stream-connect; `UNAUTHENTICATED` before business logic | closed |
| T-05-15 | Spoofing | Replay of revoked token on reconnect | mitigate | `ForwarderTokenService.validate()` only matches `ACTIVE` tokens — revoked always fail | closed |
| T-05-16 | Tampering | Wrong transponder linked retroactively | mitigate | `UnknownTransponderLinkAudit` persists `userId`, `raceId`, `entryId`, `linkedAt` — full audit trail | closed |
| T-05-17 | Denial of Service | Unauthenticated gRPC connection flood | mitigate | Auth check in interceptor before `StreamObserver` allocated; resources never consumed | closed |
| T-05-18 | Elevation of Privilege | Racer calls transponder link endpoint | mitigate | `@PreAuthorize("hasAnyRole('RACE_DIRECTOR', 'ADMIN')")` on `TransponderLinkController` | closed |
| T-05-19 | Information Disclosure | Transponder IDs in STOMP broadcast | accept | Already broadcast in Phase 4; no new exposure surface | closed |
| T-05-20 | Tampering | gRPC token without TLS on LAN | accept | Venue LAN risk accepted for v1; `ForwarderGrpcClient` Javadoc documents TLS upgrade path | closed |
| T-05-05-01 | Information Disclosure | `ForwarderTokenPage` one-time reveal | mitigate | `handleDone()` sets token state to null; no `localStorage`; token shown only in one-time reveal branch | closed |
| T-05-05-02 | Spoofing | `linkUnknownTransponder` frontend | mitigate | Server-side role enforcement on endpoint; frontend makes authenticated requests | closed |
| T-05-05-03 | Tampering | Token clipboard | accept | User clipboard is user responsibility | closed |
| T-05-05-04 | Denial of Service | Token revoke endpoint | accept | Admin-only; requires authenticated session | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-05-01 | T-05-02 | No data handled in Wave-0 test stubs | project | 2026-04-26 |
| AR-05-02 | T-05-07 | Simulator is dev-only; startup banner warns against production use | project | 2026-04-26 |
| AR-05-03 | T-05-11 | Venue LAN deployment; HTTPS required for production per Javadoc | project | 2026-04-26 |
| AR-05-04 | T-05-13 | ADMIN-only, not internet-facing; rate limiting not needed for v1 | project | 2026-04-26 |
| AR-05-05 | T-05-19 | Transponder IDs already broadcast in Phase 4; no new surface | project | 2026-04-26 |
| AR-05-06 | T-05-20 | Venue LAN TLS risk accepted for v1; TLS upgrade path documented | project | 2026-04-26 |
| AR-05-07 | T-05-05-03 | Clipboard is user responsibility | project | 2026-04-26 |
| AR-05-08 | T-05-05-04 | Admin-only revoke; requires existing authenticated session | project | 2026-04-26 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-04-26 | 24 | 24 | 0 | gsd-security-auditor (claude-sonnet-4-6) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-04-26
