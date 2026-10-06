# Phase 8: First-Run Setup Wizard - Research

**Researched:** 2026-05-03
**Domain:** Multi-step onboarding wizard (Spring Boot + React/TanStack Query)
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**D-01:** Full-page `/setup` route with a left sidebar listing all 5 steps and their completion state (✓ done / ○ incomplete). Right panel shows the active step form. Consistent with `AdminPanelLayout` sidebar pattern.

**D-02:** Each step creates the first item only (one track, one format template, one additional staff account). A "Manage more in Admin →" link appears below each step for power users wanting to add more before continuing.

**D-03:** Final wizard screen is a setup complete summary page: lists all configured items (club name, track, format, staff, decoder) with edit links. Single "Go to Admin Panel →" button. Not a redirect — user sees what they set up.

**D-04:** Frontend route guard approach. On app load, React calls `GET /api/v1/setup/status` → `{ setupComplete: boolean }`. A root-level guard in `App.tsx` redirects to `/setup` when `setupComplete = false`. No Spring Security filter changes needed.

**D-05:** `setupComplete` is derived from data: `SELECT COUNT(*) FROM club_profiles > 0`. No separate flag or migration needed. The redirect clears as soon as the club profile step is saved.

**D-06:** `/setup` route is unprotected (accessible without login). It is only open to create the first admin before any user exists. Once a club profile exists, `setupComplete = true` and the redirect no longer fires; accessing `/setup` directly then requires ADMIN role (protected).

**D-07:** `GET /api/v1/setup/status` and `POST /api/v1/setup/bootstrap` (admin account creation) are publicly accessible endpoints (no auth required). All other setup endpoints require ADMIN role.

**D-08:** The pre-wizard gate is a single "Create your admin account" screen (email + password + confirm password) shown before the wizard sidebar appears. On submit, the account is created and auto-logged in. The 5-step wizard then proceeds authenticated.

**D-09:** Step completion is derived from data presence — no `wizard_progress` table. Logic: club saved → step 1 done; at least one track exists → step 2 done; at least one format template exists → step 3 done; at least one non-RACER staff user exists → step 4 done; decoder config saved on ClubProfile → step 5 done.

**D-10:** `GET /api/v1/setup/progress` returns per-step completion status: `{ club, track, format, staff, decoder }` booleans.

**D-11:** Club profile step has no Skip — club name is required to create the `club_profiles` row and clear the first-run redirect. Steps 2–5 all have a "Skip for now" option.

**D-12:** Wizard is accessible post-completion via a permanent "Setup Wizard" entry in the Admin panel sidebar.

**D-13:** Decoder config fields (`decoderHost`, `decoderPort`, `decoderProtocol`) stored on `ClubProfile` (new columns, V25 migration). Port is auto-derived from protocol (RC-4 → 5100, P3 → 5403) but editable.

**D-14:** The decoder step generates a downloadable `forwarder.env` file on demand from current DB state. Always generated fresh.

**D-15:** `.env` format chosen for Docker compatibility.

**D-16:** Token generation in the wizard step reuses `ForwarderTokenService`.

**D-17:** "Test connection" button polls `GET /api/v1/race-control/forwarder/status` every 2 seconds, 30-second timeout.

### Claude's Discretion

- Exact polling interval and timeout for the "Test connection" button (e.g., poll every 2s for up to 30s before showing a "not yet connected" message).
- Whether the admin account creation pre-gate validates password strength via Zod (same rules as `RegisterPage`) or has its own schema — reuse existing auth validation if it fits.
- Visual styling of the step sidebar completion indicators (icon choice for ✓/○/current step).
- Whether `GET /api/v1/setup/progress` is a new endpoint or merged into `GET /api/v1/setup/status` as extra fields.

### Deferred Ideas (OUT OF SCOPE)

- **Forwarder config pull on startup** (Route 2) — forwarder calls `GET /api/v1/forwarder/decoder-config` after connecting with its token instead of reading from env file. Deferred to post-Phase 10.
- **Environment variable bootstrap** (Option B) — `SETUP_ADMIN_EMAIL` / `SETUP_ADMIN_PASSWORD` env vars to seed first admin on startup. Deferred to Phase 10 Docker environment.
</user_constraints>

---

## Summary

Phase 8 is a focused integration phase — it wires together existing infrastructure (ClubProfile, ForwarderTokenService, existing admin forms) into a first-run wizard UX. There are no new algorithms or third-party libraries to introduce. The complexity sits in two areas: (1) the backend needs a new `/api/v1/setup/**` controller namespace with two public and several ADMIN-gated endpoints, and (2) the frontend needs a standalone `/setup` route with its own layout, a root-level setup guard, and re-entrant wizard state management.

The data model change is minimal: three columns added to `club_profiles` for decoder config (V25 migration). The bootstrap flow creates the first admin via a new public endpoint that calls the same `UserService` as the existing `/api/v1/auth/register` endpoint, but assigns ADMIN role instead of RACER role. After bootstrap the user is auto-logged in by the same JWT mechanism.

The "Test connection" polling and `forwarder.env` download are both thin wrappers over already-built infrastructure (`ForwarderStatusPublisher`, `ForwarderTokenService`). The completion summary page and Admin sidebar nav entry are pure UI additions.

**Primary recommendation:** Build in four waves — Wave 0 (test stubs), Wave 1 (backend: migration + SetupController + ClubProfile decoder fields), Wave 2 (frontend: route + layout + guard + steps), Wave 3 (decoder step features: forwarder.env download + test connection polling).

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| First-run redirect | Frontend (React) | — | D-04: client-side route guard; backend only serves the status flag |
| Setup status/progress | API (Spring Boot) | — | Derives from DB state: club count, track count, etc. |
| Admin bootstrap (first user) | API (Spring Boot) | — | Must create user with ADMIN role, not RACER role; needs server-side guard to prevent replay |
| Auto-login after bootstrap | Frontend (React) | — | Store JWT from bootstrap response in AuthProvider; no separate login call |
| Wizard step state | Frontend (React) | — | Derived from `GET /api/v1/setup/progress` on mount; local React state tracks current step |
| Decoder config persistence | API (Spring Boot) | Database | Three new columns on ClubProfile; V25 Flyway migration |
| `forwarder.env` generation | API (Spring Boot) | — | Server-side file generation from DB state; streamed as HTTP attachment |
| Test connection polling | Frontend (React) | API | Frontend polls existing `ForwarderStatusController`; no new backend work needed |
| Admin sidebar "Setup Wizard" | Frontend (React) | — | Nav entry addition in `AdminPanelLayout.tsx` |

---

## Standard Stack

All tools and libraries below are already installed and in use. No new dependencies are required for this phase.

### Core (already installed)
| Library | Version | Purpose | Status in codebase |
|---------|---------|---------|-------------------|
| Spring Boot | 3.4.x | Backend framework | Active [VERIFIED: codebase] |
| Spring Security | 6.x | Auth + request matching | Active — `SecurityConfig.java` [VERIFIED: codebase] |
| Spring Data JPA + Hibernate | 6.x | Write-side persistence | Active [VERIFIED: codebase] |
| Flyway | — | Schema migrations | Active — next is V25 [VERIFIED: codebase grep] |
| React 18 + Vite | 18.x | Frontend framework | Active [VERIFIED: codebase] |
| TanStack Query v5 | 5.x | Data fetching, polling | Active — used in ForwarderStatusBar [VERIFIED: codebase] |
| React Hook Form v7 + Zod | 7.x / 3.x | Form validation | Active — all admin forms use this pattern [VERIFIED: codebase] |
| shadcn/ui (radix-luma/taupe) | — | Component library | Active — Card, Button, Input, Badge, Select, Sheet, Alert, Form all installed [VERIFIED: 08-UI-SPEC.md] |
| Sonner | — | Toast notifications | Active — `toast.success/error` in all pages [VERIFIED: codebase] |
| remixicon | — | Icon library | Active — `iconLibrary: remixicon` in components.json [VERIFIED: 08-UI-SPEC.md] |

**No new packages to install for this phase.** [VERIFIED: 08-UI-SPEC.md "No new shadcn components need to be installed"]

---

## Architecture Patterns

### System Architecture Diagram

```
Browser (unauthenticated)
  │
  ├─ GET /api/v1/setup/status
  │     └─ SetupController (public) ─→ ClubProfileRepository.count()
  │                                     → { setupComplete: true/false }
  │
  ├─ POST /api/v1/setup/bootstrap (public, guarded: fails if users exist)
  │     └─ SetupService ─→ UserService.createAdmin() ─→ users table
  │                      ─→ JwtTokenService.generateAccessToken() ─→ { token, roles... }
  │
Browser (authenticated as ADMIN after bootstrap)
  │
  ├─ GET /api/v1/setup/progress (ADMIN)
  │     └─ SetupController ─→ derives { club, track, format, staff, decoder } booleans
  │
  ├─ POST /api/v1/admin/club-profiles (existing — D-05: saves club → clears redirect)
  ├─ POST /api/v1/admin/tracks (existing)
  ├─ POST /api/v1/admin/formats (existing)
  ├─ POST /api/v1/admin/setup/staff (new: creates user with staff role)
  ├─ PATCH /api/v1/admin/club-profiles/{id}/decoder-config (new: saves decoder fields)
  │
  ├─ GET /api/v1/setup/forwarder-config-download (ADMIN)
  │     └─ SetupController ─→ ForwarderTokenService.getCurrentStatus()
  │                          ─→ ClubProfileRepository.findFirst()
  │                          ─→ stream forwarder.env as attachment
  │
  └─ GET /api/v1/race-control/forwarder/status (existing — "Test connection" polls this)
        └─ ForwarderStatusController ─→ ForwarderStatusPublisher.getLastKnownStatus()
```

### Recommended Project Structure (new files only)

```
app/src/main/java/dev/monkeypatch/rctiming/
├── api/setup/
│   ├── SetupController.java        # /api/v1/setup/** endpoints
│   ├── SetupService.java           # bootstrap + progress logic
│   └── dto/
│       ├── SetupStatusDto.java     # { setupComplete: boolean }
│       ├── SetupProgressDto.java   # { club, track, format, staff, decoder }
│       └── BootstrapRequest.java   # { email, password, firstName, lastName }
app/src/main/resources/db/migration/
└── V25__phase8_decoder_config.sql  # adds decoder_host/port/protocol to club_profiles

frontend/src/
├── pages/setup/
│   ├── SetupLayout.tsx             # full-page layout: pre-gate or 5-step wizard sidebar
│   ├── SetupGuard.tsx              # root-level guard: checks setupComplete, redirects
│   ├── steps/
│   │   ├── ClubProfileStep.tsx
│   │   ├── TrackStep.tsx
│   │   ├── FormatStep.tsx
│   │   ├── StaffStep.tsx
│   │   └── DecoderConfigStep.tsx
│   └── SetupCompletePage.tsx       # final summary screen
├── hooks/setup/
│   └── useSetupProgress.ts         # TanStack Query hook for /api/v1/setup/progress
└── lib/
    └── setupApi.ts                 # API functions for setup endpoints
```

### Pattern 1: Root-Level Setup Guard in App.tsx

The guard wraps the entire router tree. It fires `GET /api/v1/setup/status` before rendering any route. When `setupComplete = false`, it redirects to `/setup`. When `setupComplete = true`, the user's original destination is rendered normally.

**Key constraint:** The guard must NOT block the `/setup` route itself, or infinite redirect loops occur. Implementation must check `location.pathname === '/setup'` before redirecting.

```typescript
// Source: established ProtectedRoute pattern in frontend/src/components/ProtectedRoute.tsx
// Extended concept for setup guard — research-derived pattern [VERIFIED: codebase inspection]
function SetupGuard({ children }: { children: React.ReactNode }) {
  const { data, isLoading } = useQuery({
    queryKey: ['setup-status'],
    queryFn: getSetupStatus,
    staleTime: 60_000,       // only re-check once per minute
    retry: false,
  });
  const location = useLocation();

  if (isLoading) return <LoadingSpinner />;

  // Don't redirect if already on /setup (avoid infinite loop)
  if (!data?.setupComplete && !location.pathname.startsWith('/setup')) {
    return <Navigate to="/setup" replace />;
  }

  return <>{children}</>;
}
```

**Placement:** Wrap the `<RouterProvider>` children inside `RootLayout`. The guard must sit inside `<AuthProvider>` so the auth context is available after bootstrap auto-login.

### Pattern 2: Wizard Step State (Derived, Re-entrant)

Step completion state comes from `GET /api/v1/setup/progress`. The wizard never stores step state locally in localStorage or sessionStorage — re-entry always starts from the first incomplete step by re-fetching progress.

```typescript
// Source: established TanStack Query pattern from ForwarderStatusBar.tsx [VERIFIED: codebase]
function useSetupProgress() {
  return useQuery({
    queryKey: ['setup-progress'],
    queryFn: getSetupProgress,
    staleTime: 0,            // always fresh after a step save
  });
}
```

After each step's form is saved successfully, invalidate `['setup-progress']` to trigger a re-fetch and update the sidebar indicators.

### Pattern 3: Admin Bootstrap Endpoint

The `POST /api/v1/setup/bootstrap` endpoint must be guarded server-side: if `userRepository.count() > 0`, return HTTP 409 or HTTP 403 to prevent replay attacks. This is the critical security seam — without this guard, any anonymous caller could create a second admin account after the wizard completes.

```java
// Source: established pattern from UserService.createRacer() [VERIFIED: codebase]
// New method needed: UserService.createAdmin(email, password, firstName, lastName)
public User createAdmin(String email, String password, String firstName, String lastName) {
    if (userRepository.count() > 0) {
        throw new IllegalStateException("Bootstrap already complete");
    }
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setRoles(Set.of(Role.ADMIN));   // ADMIN, not RACER
    // ... timestamps, save
    return userRepository.save(user);
}
```

After creating the user, the bootstrap endpoint returns a JWT access token (same structure as `AuthController.buildAuthResponse()`) so the frontend can auto-login without a second request.

### Pattern 4: forwarder.env Download

The download endpoint generates a fresh env file string on every request and streams it as an HTTP attachment. It does NOT cache. The server pulls all values from DB at request time.

```java
// Source: standard Spring Boot file download pattern [ASSUMED]
@GetMapping("/forwarder-config-download")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<byte[]> downloadForwarderConfig() {
    ClubProfile profile = clubProfileRepository.findFirst().orElseThrow();
    ForwarderTokenService.CurrentStatus tokenStatus = forwarderTokenService.getCurrentStatus();

    if (tokenStatus.status() == null || tokenStatus.status() == REVOKED) {
        return ResponseEntity.badRequest().build();  // no active token
    }

    // Token is hashed in DB — cannot recover plaintext; frontend must show the token once on generation
    // The env file includes the ACTIVE status indicator but NOT the raw token value
    // Resolution: the download only works if we store the app server URL; token is user-responsibility
    // NOTE: See Open Question 1 — the token plaintext cannot be recovered from DB after the reveal screen
    String content = buildEnvContent(profile, tokenStatus);
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"forwarder.env\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(bytes);
}
```

### Pattern 5: "Test Connection" Polling

The polling is pure frontend — no new backend endpoint needed. It reuses the existing `GET /api/v1/race-control/forwarder/status` endpoint via `setInterval` or TanStack Query's `refetchInterval`.

```typescript
// Source: established pattern from ForwarderStatusBar.tsx [VERIFIED: codebase]
// Poll every 2s for up to 30s (15 attempts), then show timeout message [CONTEXT.md D-17]
const MAX_ATTEMPTS = 15;
const [attempts, setAttempts] = useState(0);
const [polling, setPolling] = useState(false);

const { data } = useQuery({
  queryKey: ['forwarder-status'],
  queryFn: fetchForwarderStatus,
  enabled: polling && attempts < MAX_ATTEMPTS,
  refetchInterval: polling && attempts < MAX_ATTEMPTS ? 2000 : false,
});
```

### Pattern 6: V25 Migration

Three nullable columns added to `club_profiles`. Nullable because existing rows (from dev seed) have no decoder config and must not fail the migration.

```sql
-- V25__phase8_decoder_config.sql
ALTER TABLE club_profiles
    ADD COLUMN decoder_host     VARCHAR(255),
    ADD COLUMN decoder_port     INTEGER,
    ADD COLUMN decoder_protocol VARCHAR(10);  -- 'RC4' or 'P3'
```

### Anti-Patterns to Avoid

- **Storing wizard step progress in a DB table:** D-09 explicitly prohibits this. Progress is always derived from data presence. A separate `wizard_progress` table would go stale relative to the actual data and cause re-entry bugs.
- **Blocking the `/setup` route redirect in Spring Security:** D-04 specifies frontend route guard only. Mixing Spring Security filter-level redirect with frontend guard creates double-redirect scenarios and breaks the JWT flow.
- **Recovering the token plaintext for the env file:** The forwarder token is stored as a bcrypt hash in the DB — the plaintext is shown exactly once to the user on generation. The env file download cannot include the raw token because it cannot be recovered. This is a real open question (see Open Questions section).
- **Using `userRepository.count()` race condition in bootstrap:** In a concurrent multi-request scenario, two bootstrap requests could both pass the count check before either commits. The DB should have a uniqueness constraint on `users` email, but there is no single-user constraint. Mitigation: use a DB-level sequence or a unique constraint on a `setup_complete` flag. However, because this is a first-run tool used exactly once by a single operator, this is an acceptable low-probability risk.
- **Using AdminPanelLayout directly for the wizard:** The wizard is at `/setup` outside the admin shell — it must be its own layout component (`SetupLayout.tsx`). Nesting it under `AdminPanelLayout` would break the pre-gate (unauthenticated) flow.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| File download as attachment | Custom byte-write servlet | Spring `ResponseEntity<byte[]>` with `Content-Disposition` header | Spring handles chunking, content-length, encoding correctly |
| Password hashing in bootstrap | Custom crypto | `PasswordEncoder` (BCrypt) — already wired in `UserService` | Same bean used everywhere in the project |
| Admin role assignment | Manual DB insert | `UserService.createAdmin()` (new method) calling `userRepository.save()` | Keeps role assignment logic in one place |
| Token status check | New DB query in SetupController | `ForwarderTokenService.getCurrentStatus()` | Already tested, handles ACTIVE/REVOKED/NONE states |
| Forwarder connection status polling | New WebSocket subscription | `GET /api/v1/race-control/forwarder/status` (existing `ForwarderStatusController`) | Already built in Phase 5, no new backend code needed |

---

## Common Pitfalls

### Pitfall 1: Infinite Redirect Loop in SetupGuard
**What goes wrong:** The setup guard redirects to `/setup`. The `/setup` route loads and calls `GET /api/v1/setup/status`. If the status is still `false` (e.g., first page load before club profile is saved), the guard fires again and redirects back to `/setup`, creating an infinite redirect.
**Why it happens:** The guard checks `setupComplete` without excluding the `/setup` path from the check.
**How to avoid:** Guard must include `!location.pathname.startsWith('/setup')` before issuing the redirect. See Pattern 1 above.
**Warning signs:** Browser network tab shows repeated calls to `/api/v1/setup/status`; page never renders.

### Pitfall 2: Bootstrap Endpoint Replay After Wizard Completion
**What goes wrong:** After setup is complete, an anonymous attacker calls `POST /api/v1/setup/bootstrap` with their own credentials and gets an ADMIN account.
**Why it happens:** The endpoint is publicly accessible (D-07) and lacks a server-side guard.
**How to avoid:** Check `userRepository.count() > 0` at the top of the bootstrap service method and throw an exception (HTTP 409) if any users exist. This makes bootstrap idempotent-safe.
**Warning signs:** Verified by integration test: call bootstrap twice, confirm second call returns 409.

### Pitfall 3: TanStack Query Cache Stale After Step Save
**What goes wrong:** User completes Step 1 (club profile). The sidebar still shows Step 1 as incomplete because `['setup-progress']` query is cached.
**Why it happens:** TanStack Query caches results and does not re-fetch unless explicitly invalidated or staleTime is 0.
**How to avoid:** In each step's `onSuccess` callback, call `queryClient.invalidateQueries({ queryKey: ['setup-status'] })` and `queryClient.invalidateQueries({ queryKey: ['setup-progress'] })`.
**Warning signs:** Sidebar completion indicators do not update after saving a step.

### Pitfall 4: forwarder.env Token Plaintext Recovery
**What goes wrong:** The `forwarder.env` download includes `APP_FORWARDER_TOKEN=<value>`, but the token plaintext was only shown once to the user and cannot be recovered from the bcrypt hash in the DB.
**Why it happens:** `ForwarderTokenService` stores only the hash, not the plaintext — correct security practice.
**How to avoid:** Two options: (A) the env file does NOT include the token value — instead it includes a placeholder with instructions to paste the token manually. (B) Store the plaintext encrypted using an app-level secret key (adds complexity). Recommend option A: the env file includes `APP_FORWARDER_TOKEN=<paste-your-token-here>` as a placeholder comment.
**Warning signs:** Attempting to call `forwarderTokenService.getCurrentStatus().plaintext()` — no such field exists on `CurrentStatus` record.

### Pitfall 5: `/setup` Route Access After Setup Complete
**What goes wrong:** An authenticated non-admin user navigates to `/setup` after setup is complete and sees the wizard UI, which makes admin-gated API calls that return 403.
**Why it happens:** D-06 states the route must be protected (ADMIN role required) once `setupComplete = true`. The frontend guard only handles the redirect-to-setup case, not the post-setup access case.
**How to avoid:** `SetupLayout.tsx` should check: if `setupComplete = true` AND the current user does not have ADMIN role, redirect to `/admin` or show an "Access denied" message. Alternatively, wrap the `/setup` route with `ProtectedRoute roles={['ADMIN']}` but only when `setupComplete = true` — this requires a conditional wrapper component.
**Simplest safe approach:** Always require ADMIN role to access `/setup`. The pre-gate (unauthenticated) case is handled within `SetupLayout`: if `setupComplete = false` AND the user is not logged in, show the bootstrap gate screen; if `setupComplete = false` AND the user IS logged in as ADMIN, skip the gate and go directly to the wizard.

### Pitfall 6: AdminPanelLayout NavItem Icon Library Mismatch
**What goes wrong:** The "Setup Wizard" nav entry added to `AdminPanelLayout.tsx` uses a Lucide icon (current pattern) but the wizard sidebar step indicators use remixicon per the UI-SPEC.
**Why it happens:** The Admin sidebar uses Lucide icons throughout (`Calendar`, `Trophy`, etc.). The wizard sidebar uses remixicon for step state indicators (RI icons).
**How to avoid:** Add the "Setup Wizard" entry to `AdminPanelLayout.tsx` using a Lucide icon (matching existing nav items). The wizard layout uses remixicon only within `SetupLayout.tsx`.
**Warning signs:** Console errors about `ri-*` class not rendering correctly in Lucide context.

---

## Code Examples

### SetupStatusDto (Backend)
```java
// Source: established DTO pattern from ForwarderStatusDto.java [VERIFIED: codebase]
public record SetupStatusDto(boolean setupComplete) {}
```

### SetupProgressDto (Backend)
```java
// Source: established DTO record pattern [VERIFIED: codebase]
public record SetupProgressDto(
    boolean club,
    boolean track,
    boolean format,
    boolean staff,
    boolean decoder
) {}
```

### SetupController skeleton
```java
// Source: established controller pattern [VERIFIED: codebase AuthController + ClubProfileController]
@RestController
@RequestMapping("/api/v1/setup")
public class SetupController {

    @GetMapping("/status")
    public SetupStatusDto getStatus() { ... }  // public — no auth

    @PostMapping("/bootstrap")
    public ResponseEntity<AuthResponse> bootstrap(@RequestBody @Valid BootstrapRequest req) { ... }  // public

    @GetMapping("/progress")
    @PreAuthorize("hasRole('ADMIN')")
    public SetupProgressDto getProgress() { ... }

    @GetMapping("/forwarder-config-download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadForwarderConfig() { ... }
}
```

### SecurityConfig addition
```java
// Source: SecurityConfig.java — add to existing permitAll chain [VERIFIED: codebase]
.requestMatchers("/api/v1/setup/status").permitAll()
.requestMatchers(HttpMethod.POST, "/api/v1/setup/bootstrap").permitAll()
// All other /api/v1/setup/** endpoints fall through to .anyRequest().authenticated()
// and are protected by @PreAuthorize("hasRole('ADMIN')") on the controller methods
```

### SetupApi (Frontend)
```typescript
// Source: established pattern from raceControlApi.ts and adminApi.ts [VERIFIED: codebase]
import api from './api';

export type SetupStatusDto = { setupComplete: boolean };
export type SetupProgressDto = { club: boolean; track: boolean; format: boolean; staff: boolean; decoder: boolean };

export async function getSetupStatus(): Promise<SetupStatusDto> {
  const { data } = await api.get<SetupStatusDto>('/api/v1/setup/status');
  return data;
}

export async function getSetupProgress(): Promise<SetupProgressDto> {
  const { data } = await api.get<SetupProgressDto>('/api/v1/setup/progress');
  return data;
}

export async function bootstrap(req: BootstrapRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/api/v1/setup/bootstrap', req);
  return data;
}
```

### Bootstrap Zod schema (Frontend)
```typescript
// Source: RegisterPage.tsx — reuse password validation rules [VERIFIED: codebase]
// Admin bootstrap form omits firstName/lastName (kept minimal per UI-SPEC)
// But admin account needs firstName+lastName for user.firstName display in AdminPanelLayout
// Decision: include firstName/lastName on the bootstrap form (same as RegisterPage)
const bootstrapSchema = z.object({
  firstName: z.string().min(1, 'First name required').max(100),
  lastName:  z.string().min(1, 'Last name required').max(100),
  email:     z.string().email('Valid email required'),
  password:  z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});
```

Note: The UI-SPEC shows only email + password + confirm password fields for the bootstrap gate. However the `User` entity requires `firstName` and `lastName` (NOT NULL in DB). Resolution: include firstName/lastName in the bootstrap form. This is a discretion area (CONTEXT.md: "Whether the admin account creation pre-gate validates password strength via Zod (same rules as RegisterPage) or has its own schema"). Recommendation: reuse the full RegisterPage schema — it already includes firstName/lastName.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Server-side redirect for first-run detection | Client-side route guard + status API | Phase 8 decision (D-04) | Simpler — no Spring Security filter changes needed |
| Separate `wizard_progress` table | Derive completion from data presence | Phase 8 decision (D-09) | No extra table; progress is always accurate to actual data state |
| forwarder.properties format | forwarder.env (Docker-compatible) | Phase 8 decision (D-15) | Env file works with `env_file:` in docker-compose and Spring Boot's native env var binding |

---

## Open Questions (RESOLVED)

1. **forwarder.env Token Plaintext**
   - What we know: The token bcrypt hash is in the DB. The plaintext is shown once to the user after `ForwarderTokenService.generate()`.
   - What's unclear: Should the env file include a placeholder `APP_FORWARDER_TOKEN=<paste-token-here>` and instruct the user to fill it in manually? Or should the wizard require token generation before allowing the download (ensuring the user already copied the token)?
   - RESOLVED: The download button is disabled until the token is generated (enforced by UI). The env file includes `# Paste your forwarder token here (generated in the Decoder Config step)` above a blank `APP_FORWARDER_TOKEN=` line. This is safe and honest — never a recoverable value.

2. **Admin bootstrap form: firstName/lastName required?**
   - What we know: `User` entity has `firstName VARCHAR NOT NULL` and `lastName VARCHAR NOT NULL` in the schema. The UI-SPEC shows only email/password/confirm fields.
   - What's unclear: Should the bootstrap form collect firstName/lastName?
   - RESOLVED: Yes, include them. The `AdminPanelLayout` displays `user.firstName user.lastName` in the sidebar — a blank name there on first login is a poor experience. The UI-SPEC is silent on this; it is in "Claude's Discretion" territory.

3. **`/setup/progress` merged into `/setup/status` or separate?**
   - What we know: D-10 defines `/setup/progress` as its own endpoint. Claude's Discretion says "whether merged or separate".
   - RESOLVED: Keep them separate. `status` is called on every page load (public, no auth) and is lightweight. `progress` is called only within the wizard (ADMIN-gated, richer response). Merging would require the public endpoint to do more DB work on every unauthenticated request.

---

## Environment Availability

Step 2.6 audit: this phase is backend + frontend code changes with no new external tools. Existing tools confirmed available:

| Dependency | Required By | Available | Notes |
|------------|------------|-----------|-------|
| PostgreSQL 16 | V25 migration | Yes | Running via Testcontainers in tests [VERIFIED: AbstractIntegrationTest.java] |
| Java 21 | Spring Boot backend | Yes | Project prerequisite [VERIFIED: codebase] |
| Node / npm | Frontend build | Yes | Project prerequisite [VERIFIED: codebase] |

No missing dependencies. No new services required.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Backend framework | JUnit 5 + Testcontainers (PostgreSQL 16-alpine) |
| Backend base class | `AbstractIntegrationTest` — `@SpringBootTest(RANDOM_PORT)`, `@TestPropertySource(properties = "app.grpc.port=0")` |
| Frontend framework | Vitest 4.1.x + React Testing Library |
| Frontend config | `vite.config.ts` — `test.environment: jsdom`, `setupFiles: ./src/test/setup.ts` |
| Quick run (frontend) | `npm test -- --run` |
| Quick run (backend) | `./gradlew test --tests "*.SetupControllerIT"` |
| Full suite (backend) | `./gradlew test` |
| Full suite (frontend) | `npm test -- --run` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SC-1 | `/setup` redirect fires when no club record exists | Integration | `./gradlew test --tests "*.SetupControllerIT.getStatus_returnsSetupComplete_false_whenNoClub"` | No — Wave 0 |
| SC-1 | Redirect clears after club profile saved | Integration | `./gradlew test --tests "*.SetupControllerIT.getStatus_returnsSetupComplete_true_afterClubSaved"` | No — Wave 0 |
| SC-2 | Bootstrap creates ADMIN user; returns JWT | Integration | `./gradlew test --tests "*.SetupControllerIT.bootstrap_createsAdminUserAndReturnsToken"` | No — Wave 0 |
| SC-2 | Bootstrap blocked after first user exists (replay guard) | Integration | `./gradlew test --tests "*.SetupControllerIT.bootstrap_returns409_whenUsersExist"` | No — Wave 0 |
| SC-3 | Progress endpoint returns correct booleans for each step | Integration | `./gradlew test --tests "*.SetupControllerIT.getProgress_reflectsDataState"` | No — Wave 0 |
| SC-4 | Forwarder.env download returns attachment with correct content | Integration | `./gradlew test --tests "*.SetupControllerIT.downloadForwarderConfig_returnsEnvAttachment"` | No — Wave 0 |
| SC-4 | Frontend: "Test connection" polling shows success when CONNECTED | Unit (frontend) | `npm test -- --run src/pages/setup/__tests__/DecoderConfigStep.test.tsx` | No — Wave 0 |
| SC-5 | Setup wizard accessible from Admin sidebar (nav entry exists) | Unit (frontend) | `npm test -- --run src/pages/admin/__tests__/AdminPanelLayout.test.tsx` | No — Wave 0 |
| SC-1 | SetupGuard redirects to /setup when setupComplete=false | Unit (frontend) | `npm test -- --run src/pages/setup/__tests__/SetupGuard.test.tsx` | No — Wave 0 |
| SC-1 | SetupGuard does NOT redirect when already on /setup | Unit (frontend) | included in SetupGuard.test.tsx | No — Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew test --tests "*.SetupControllerIT" && npm test -- --run src/pages/setup/`
- **Per wave merge:** Full backend + frontend suite
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/java/.../api/setup/SetupControllerIT.java` — covers SC-1 through SC-4 backend behaviors
- [ ] `frontend/src/pages/setup/__tests__/SetupGuard.test.tsx` — covers SC-1 redirect behavior
- [ ] `frontend/src/pages/setup/__tests__/DecoderConfigStep.test.tsx` — covers SC-4 polling UI
- [ ] `frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx` — covers SC-5 nav entry (may already exist; verify)

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | Yes | BCrypt via Spring Security `PasswordEncoder`; same as existing auth |
| V3 Session Management | Yes | JWT stateless; same `JwtTokenService` used for bootstrap response |
| V4 Access Control | Yes | Bootstrap endpoint replay guard (`userRepository.count() > 0` check) |
| V5 Input Validation | Yes | Zod on frontend; `@Valid` + Bean Validation on backend `BootstrapRequest` |
| V6 Cryptography | No | No new crypto; existing BCrypt and JWT patterns reused |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Bootstrap endpoint replay (second admin creation) | Elevation of Privilege | Server-side count guard in `SetupService.bootstrap()` — return 409 if `userRepository.count() > 0` |
| Unauthenticated access to wizard forms after setup complete | Elevation of Privilege | D-06: once `setupComplete = true`, `/setup` requires ADMIN role |
| CSRF on public bootstrap endpoint | Tampering | Stateless JWT — no session cookies; CSRF disabled in `SecurityConfig` (existing) |
| Decoder IP/host stored in DB (SSRF vector in future) | Information Disclosure | Out of scope for v1 — decoder config is admin-only; no server-side proxy to decoder in this phase |

---

## Project Constraints (from CLAUDE.md)

| Directive | Impact on This Phase |
|-----------|---------------------|
| No `spring.jpa.hibernate.ddl-auto=update/create-drop` | V25 migration must be a proper Flyway SQL file |
| No SockJS | "Test connection" polling uses REST, not WebSocket |
| No Redux | React state for current wizard step uses `useState` only |
| No Next.js / SSR | Setup guard is client-side React |
| Use `gh` CLI for GitHub operations | Not applicable to implementation tasks |
| Stop all processes after use | Executor must not leave `bootRun` or `vite dev` running |
| TanStack Query covers state management | All server state in wizard steps via TanStack Query |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `forwarder.env` download should use a placeholder for the token value, not attempt to recover plaintext | Common Pitfalls / Open Questions | If the product decision is to recover token (impossible) or require token re-generation at download time (possible UX approach), the download endpoint design changes |
| A2 | Bootstrap form should include firstName/lastName despite UI-SPEC showing only email/password | Code Examples | If firstName/lastName can be NULL in the DB schema, this is unnecessary; but User entity requires them NOT NULL [VERIFIED: seed SQL] |
| A3 | `userRepository.count() > 0` is sufficient replay guard for bootstrap | Architecture Patterns | In a highly concurrent environment this could race; for a single-operator first-run tool this is acceptable risk |
| A4 | Standard Spring `ResponseEntity<byte[]>` with `Content-Disposition: attachment` is correct pattern for `.env` download | Code Examples | If the Spring Boot version has specific streaming behavior differences, may need `StreamingResponseBody`; but byte[] is simpler and correct for small files |

---

## Sources

### Primary (HIGH confidence)
- `frontend/src/components/ProtectedRoute.tsx` — verified auth guard pattern for SetupGuard design
- `frontend/src/pages/race-control/panels/ForwarderStatusBar.tsx` — verified polling pattern for "Test connection"
- `frontend/src/pages/admin/race-control/ForwarderTokenPage.tsx` — verified token generate/revoke UX for wizard decoder step
- `frontend/src/pages/admin/AdminPanelLayout.tsx` — verified sidebar layout for wizard layout
- `frontend/src/pages/auth/RegisterPage.tsx` — verified Zod schema for bootstrap form
- `app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java` — verified permitAll pattern
- `app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenService.java` — verified token API
- `app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderStatusPublisher.java` — verified status polling target
- `app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserService.java` — verified createRacer() pattern for createAdmin()
- `app/src/main/resources/db/migration/` — verified next migration is V25
- `.planning/phases/08-first-run-setup-wizard/08-CONTEXT.md` — locked decisions
- `.planning/phases/08-first-run-setup-wizard/08-UI-SPEC.md` — visual contract

### Secondary (MEDIUM confidence)
- `app/src/test/java/.../AbstractIntegrationTest.java` — confirmed Testcontainers test pattern
- `app/src/test/java/.../api/admin/ClubControllerIT.java` — confirmed integration test style

### Tertiary (LOW confidence — tagged [ASSUMED])
- Standard Spring `ResponseEntity<byte[]>` file download pattern — not verified against current Spring Boot 3.4.x docs; risk is minimal given stable API

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries confirmed present in codebase
- Architecture: HIGH — decisions locked in CONTEXT.md; patterns verified against existing code
- Pitfalls: HIGH — derived from direct code inspection of integration points
- Security: HIGH — existing patterns reused; one new guard (bootstrap replay) documented

**Research date:** 2026-05-03
**Valid until:** 2026-06-03 (stable stack, 30-day window)
