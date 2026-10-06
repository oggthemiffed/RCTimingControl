# Phase 8: First-Run Setup Wizard — Pattern Map

**Mapped:** 2026-05-03
**Files analyzed:** 19 new/modified files
**Analogs found:** 18 / 19

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `app/.../api/setup/SetupController.java` | controller | request-response | `app/.../api/auth/AuthController.java` | exact (public + gated endpoints mixed) |
| `app/.../api/setup/SetupService.java` | service | CRUD | `app/.../domain/user/UserService.java` | role-match |
| `app/.../api/setup/dto/SetupStatusDto.java` | dto | — | `app/.../api/auth/AuthResponse.java` | role-match (record DTO) |
| `app/.../api/setup/dto/SetupProgressDto.java` | dto | — | `app/.../api/auth/AuthResponse.java` | role-match (record DTO) |
| `app/.../api/setup/dto/BootstrapRequest.java` | dto | — | `app/.../api/auth/RegisterRequest.java` | exact |
| `app/.../security/SecurityConfig.java` (modify) | config | — | self | modify |
| `app/.../domain/club/ClubProfile.java` (modify) | model | — | self | modify |
| `app/.../domain/club/ClubProfileService.java` (modify) | service | CRUD | self | modify |
| `app/.../resources/db/migration/V25__phase8_decoder_config.sql` | migration | — | `V24__phase7_results_championship.sql` | exact |
| `frontend/src/pages/setup/SetupLayout.tsx` | component | request-response | `frontend/src/pages/admin/AdminPanelLayout.tsx` | exact (sidebar layout) |
| `frontend/src/pages/setup/SetupGuard.tsx` | component | request-response | `frontend/src/components/ProtectedRoute.tsx` | exact (guard pattern) |
| `frontend/src/pages/setup/steps/ClubProfileStep.tsx` | component | CRUD | `frontend/src/pages/admin/club/ClubProfilePage.tsx` | exact |
| `frontend/src/pages/setup/steps/TrackStep.tsx` | component | CRUD | `frontend/src/pages/admin/tracks/TracksPage.tsx` | role-match |
| `frontend/src/pages/setup/steps/FormatStep.tsx` | component | CRUD | `frontend/src/pages/admin/formats/FormatsPage.tsx` | role-match |
| `frontend/src/pages/setup/steps/StaffStep.tsx` | component | CRUD | `frontend/src/pages/auth/RegisterPage.tsx` | role-match |
| `frontend/src/pages/setup/steps/DecoderConfigStep.tsx` | component | request-response | `frontend/src/pages/admin/race-control/ForwarderTokenPage.tsx` | exact |
| `frontend/src/pages/setup/SetupCompletePage.tsx` | component | request-response | `frontend/src/pages/admin/AdminPanelLayout.tsx` | partial |
| `frontend/src/hooks/setup/useSetupProgress.ts` | hook | request-response | `frontend/src/hooks/admin/useAdminClub.ts` | exact |
| `frontend/src/lib/setupApi.ts` | utility | request-response | `frontend/src/lib/adminApi.ts` | exact |
| `frontend/src/App.tsx` (modify) | config | — | self | modify |
| `frontend/src/pages/admin/AdminPanelLayout.tsx` (modify) | component | — | self | modify |

---

## Pattern Assignments

### `app/.../api/setup/SetupController.java` (controller, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/auth/AuthController.java`

**Imports pattern** (lines 1–28):
```java
package dev.monkeypatch.rctiming.api.setup;

import dev.monkeypatch.rctiming.domain.user.UserService;
import dev.monkeypatch.rctiming.security.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
```

**Mixed public + gated endpoint pattern** (AuthController.java lines 31–61):
```java
@RestController
@RequestMapping("/api/v1/setup")
// NOTE: No class-level @PreAuthorize — endpoints mix public and ADMIN-gated
public class SetupController {

    // Injected via constructor (no field injection)
    private final SetupService setupService;
    private final JwtTokenService jwtTokenService;

    public SetupController(SetupService setupService, JwtTokenService jwtTokenService) {
        this.setupService = setupService;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/status")
    // No @PreAuthorize — public endpoint (SecurityConfig permits this)
    public SetupStatusDto getStatus() { ... }

    @PostMapping("/bootstrap")
    // No @PreAuthorize — public endpoint (SecurityConfig permits this)
    public ResponseEntity<AuthResponse> bootstrap(@RequestBody @Valid BootstrapRequest req) { ... }

    @GetMapping("/progress")
    @PreAuthorize("hasRole('ADMIN')")
    public SetupProgressDto getProgress() { ... }

    @GetMapping("/forwarder-config-download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadForwarderConfig() { ... }
}
```

**AuthResponse construction pattern** (AuthController.java lines 148–157):
```java
// The bootstrap endpoint must return the same AuthResponse structure as /auth/login
private AuthResponse buildAuthResponse(User user, String accessToken) {
    return new AuthResponse(
            accessToken,
            user.getId().toString(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRoles().stream().map(Enum::name).toList()
    );
}
```

**Error response for 409 replay guard** (AuthController.java lines 63–75 pattern):
```java
// Return ResponseEntity with body null + 409 status for bootstrap replay
return ResponseEntity.status(HttpStatus.CONFLICT).build();
```

---

### `app/.../api/setup/SetupService.java` (service, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserService.java`

**Service class pattern** (UserService.java lines 1–44):
```java
package dev.monkeypatch.rctiming.api.setup;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SetupService {

    private final UserRepository userRepository;
    // ... other repos injected via constructor

    public SetupService(UserRepository userRepository, ...) {
        this.userRepository = userRepository;
    }
```

**createAdmin guard pattern — copy from UserService.createRacer()** (UserService.java lines 24–38):
```java
// In SetupService.bootstrap():
// Guard: replay protection (D-07)
if (userRepository.count() > 0) {
    throw new IllegalStateException("Bootstrap already complete — users exist");
}
User user = new User();
user.setEmail(email);
user.setPasswordHash(passwordEncoder.encode(password));
user.setFirstName(firstName);
user.setLastName(lastName);
user.setRoles(Set.of(Role.ADMIN));   // ADMIN, not RACER
Instant now = Instant.now();
user.setCreatedAt(now);
user.setUpdatedAt(now);
return userRepository.save(user);
```

**@Transactional(readOnly = true) for read methods** (UserService.java line 40):
```java
@Transactional(readOnly = true)
public SetupStatusDto getStatus() { ... }

@Transactional(readOnly = true)
public SetupProgressDto getProgress() { ... }
```

---

### `app/.../api/setup/dto/SetupStatusDto.java` and `SetupProgressDto.java` (DTOs)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/auth/AuthResponse.java`

**Java record DTO pattern** — verified from ForwarderStatusDto and AuthResponse patterns:
```java
// SetupStatusDto.java
package dev.monkeypatch.rctiming.api.setup.dto;

public record SetupStatusDto(boolean setupComplete) {}

// SetupProgressDto.java
package dev.monkeypatch.rctiming.api.setup.dto;

public record SetupProgressDto(
    boolean club,
    boolean track,
    boolean format,
    boolean staff,
    boolean decoder
) {}

// BootstrapRequest.java — mirrors RegisterRequest pattern
package dev.monkeypatch.rctiming.api.setup.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BootstrapRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email @NotBlank String email,
    @Size(min = 8) @NotBlank String password
) {}
```

---

### `app/.../security/SecurityConfig.java` (modify, config)

**Analog:** self — `app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java`

**Existing permitAll chain** (SecurityConfig.java lines 29–39):
```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/error").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/v1/results/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/v1/championships/**").permitAll()
        // ADD THESE TWO LINES FOR PHASE 8:
        .requestMatchers(HttpMethod.GET, "/api/v1/setup/status").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/v1/setup/bootstrap").permitAll()
        // All other /api/v1/setup/** requires auth (ADMIN role enforced at controller level)
        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "RACE_DIRECTOR", "REFEREE")
        ...
)
```

**Note:** Insert the two new `permitAll` lines before the `/api/v1/admin/**` line. All other `/api/v1/setup/**` endpoints fall through to `.anyRequest().authenticated()` and are additionally protected by `@PreAuthorize("hasRole('ADMIN')")` on the controller.

---

### `app/.../resources/db/migration/V25__phase8_decoder_config.sql` (migration)

**Analog:** `app/src/main/resources/db/migration/V24__phase7_results_championship.sql`

**Flyway plain-SQL ALTER TABLE pattern** (V24 lines 1–19):
```sql
-- V25: Phase 8 — First-run setup wizard.
-- Adds decoder configuration columns to club_profiles.
-- Nullable: existing rows from dev seed and integration tests have no decoder config.

ALTER TABLE club_profiles
    ADD COLUMN decoder_host     VARCHAR(255),
    ADD COLUMN decoder_port     INTEGER,
    ADD COLUMN decoder_protocol VARCHAR(10);
-- 'RC4' or 'P3' — matches DecoderProtocol enum name()

COMMENT ON COLUMN club_profiles.decoder_host IS
    'Hostname or IP address of the AMB decoder. '
    'RC-4 protocol (firmware < 4.5) defaults to port 5100; P3 to 5403. '
    'Null until configured via Phase 8 setup wizard.';

COMMENT ON COLUMN club_profiles.decoder_protocol IS
    'Decoder protocol variant: RC4 (text, firmware < 4.5) or P3 (binary, firmware >= 4.5). '
    'Null until configured via Phase 8 setup wizard.';
```

---

### `frontend/src/pages/setup/SetupLayout.tsx` (component, request-response)

**Analog:** `frontend/src/pages/admin/AdminPanelLayout.tsx`

**Full layout structure to mirror** (AdminPanelLayout.tsx lines 149–214):
```tsx
// Mirrors AdminPanelLayout exactly: fixed left sidebar (w-60) + main content (md:pl-60)
// Key differences from AdminPanelLayout:
// 1. Brand text: "RC Timing — Setup" instead of "RC Timing — Admin"
// 2. Sidebar content: numbered step list (not NavLinks in initial mode)
// 3. No bottom mobile nav (admin-only flow, laptop expected)
// 4. No user/logout block in sidebar during unauthenticated pre-gate
// 5. Mobile Sheet drawer: same pattern, step list instead of nav items

export default function SetupLayout() {
  const [sheetOpen, setSheetOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background">
      {/* Desktop: fixed left sidebar */}
      <aside className="hidden md:flex fixed inset-y-0 left-0 w-60 border-r bg-card flex-col z-10">
        <SidebarContent />
      </aside>

      {/* Mobile: top bar */}
      <header className="md:hidden flex items-center border-b h-14 px-4 sticky top-0 bg-background z-10">
        <Button variant="ghost" size="icon-sm" onClick={() => setSheetOpen(true)} aria-label="Open navigation">
          <Menu className="h-5 w-5" />
        </Button>
        <span className="ml-3 font-semibold text-sm">RC Timing — Setup</span>
      </header>

      {/* Mobile: Sheet drawer */}
      <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
        <SheetContent side="left" showCloseButton className="w-72 p-0">
          <SheetHeader className="sr-only"><SheetTitle>Setup Steps</SheetTitle></SheetHeader>
          <SidebarContent onNavClick={() => setSheetOpen(false)} />
        </SheetContent>
      </Sheet>

      {/* Main content */}
      <main className="md:pl-60">
        <div className="px-6 pt-6 pb-10 min-h-screen">
          <Outlet />  {/* or {children} depending on routing strategy */}
        </div>
      </main>
    </div>
  );
}
```

**Step sidebar item pattern** (AdminPanelLayout.tsx NavItem lines 45–73, adapted for steps):
```tsx
// Step items use remixicon classes (not Lucide icons — wizard uses ri-* per UI-SPEC)
// Three visual states per UI-SPEC Step Sidebar Contract:
function StepItem({
  number, label, state, onClick
}: { number: number; label: string; state: 'complete' | 'current' | 'incomplete'; onClick?: () => void }) {
  return (
    <button
      onClick={onClick}
      aria-current={state === 'current' ? 'step' : undefined}
      className={
        state === 'current'
          ? 'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-semibold text-primary border-l-[3px] border-primary pl-[calc(0.75rem-3px)] w-full text-left'
          : state === 'complete'
          ? 'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium text-foreground w-full text-left'
          : 'flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-muted-foreground w-full text-left'
      }
    >
      <i className={
        state === 'complete' ? 'ri-checkbox-circle-fill text-[var(--flag-green)]'
        : state === 'current' ? 'ri-record-circle-line text-primary'
        : 'ri-circle-line'
      } aria-hidden="true" />
      {label}
    </button>
  );
}
```

---

### `frontend/src/pages/setup/SetupGuard.tsx` (component, request-response)

**Analog:** `frontend/src/components/ProtectedRoute.tsx`

**Guard pattern** (ProtectedRoute.tsx lines 11–32, inverted for setup):
```tsx
import { Navigate, useLocation } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getSetupStatus } from '@/lib/setupApi';

export default function SetupGuard({ children }: { children: React.ReactNode }) {
  const { data, isLoading } = useQuery({
    queryKey: ['setup-status'],
    queryFn: getSetupStatus,
    staleTime: 60_000,   // re-check once per minute
    retry: false,
  });
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  // Critical: exclude /setup itself to prevent infinite redirect loop (RESEARCH.md Pitfall 1)
  if (!data?.setupComplete && !location.pathname.startsWith('/setup')) {
    return <Navigate to="/setup" replace />;
  }

  return <>{children}</>;
}
```

**Loading spinner** — copy verbatim from ProtectedRoute.tsx lines 14–21.

---

### `frontend/src/pages/setup/steps/ClubProfileStep.tsx` (component, CRUD)

**Analog:** `frontend/src/pages/admin/club/ClubProfilePage.tsx`

**React Hook Form + Zod pattern** (ClubProfilePage.tsx lines 1–28):
```tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Form, FormField, FormItem, FormLabel, FormControl, FormMessage,
} from '@/components/ui/form';
```

**Wizard step uses shadcn Form components** (RegisterPage.tsx lines 99–178 — note: ClubProfilePage uses bare `register` but wizard steps MUST use `<Form>` + `<FormField>` + `<FormMessage>` per UI-SPEC and RegisterPage pattern):
```tsx
// Wizard steps use Form/FormField/FormMessage, not bare register + error spans
const schema = z.object({
  name: z.string().min(1, 'Club name is required'),
  timezone: z.string().min(1, 'Timezone is required'),
  // email/phone optional in wizard (only name required per D-11)
});

const form = useForm({ resolver: zodResolver(schema), mode: 'onBlur' });

async function onSave(values: typeof schema._type) {
  try {
    await updateClubProfile(values);  // via setupApi or adminApi
    queryClient.invalidateQueries({ queryKey: ['setup-status'] });
    queryClient.invalidateQueries({ queryKey: ['setup-progress'] });
    toast.success('Club profile saved');
    onNext();  // advance wizard step
  } catch {
    toast.error('Could not save club profile. Try again.');
  }
}
```

**Success → invalidate both queries** (Pitfall 3 from RESEARCH.md):
After any step save, always invalidate both `['setup-status']` and `['setup-progress']`.

---

### `frontend/src/pages/setup/steps/DecoderConfigStep.tsx` (component, request-response)

**Analog:** `frontend/src/pages/admin/race-control/ForwarderTokenPage.tsx`

**Token state management pattern** (ForwarderTokenPage.tsx lines 18–56):
```tsx
// Copy the confirmAction state machine exactly
type ConfirmAction = 'regenerate' | 'revoke' | null;
const [confirmAction, setConfirmAction] = useState<ConfirmAction>(null);
const [newToken, setNewToken] = useState<string | null>(null);
```

**useMutation with invalidation** (ForwarderTokenPage.tsx lines 28–50):
```tsx
const generateMutation = useMutation({
  mutationFn: generateForwarderToken,
  onSuccess: (result) => {
    setNewToken(result.token);
    setConfirmAction(null);
    queryClient.invalidateQueries({ queryKey: ['forwarder-token-status'] });
  },
  onError: (error: Error) => {
    toast.error(`Failed to generate token: ${error.message}`);
  },
});
```

**Inline confirmation box pattern** (ForwarderTokenPage.tsx lines 223–260):
```tsx
// Regenerate confirm box: bg-muted/50
{confirmAction === 'regenerate' && (
  <div className="space-y-3 p-3 rounded-md border bg-muted/50">
    <p className="text-sm">
      Regenerating will disconnect the forwarder until you update its config. Continue?
    </p>
    <div className="flex gap-2">
      <Button variant="ghost" onClick={() => setConfirmAction(null)}>Keep current token</Button>
      <Button onClick={() => generateMutation.mutate()} disabled={generateMutation.isPending}>
        {generateMutation.isPending ? 'Regenerating…' : 'Confirm Regenerate'}
      </Button>
    </div>
  </div>
)}

// Revoke confirm box: bg-destructive/10
{confirmAction === 'revoke' && (
  <div className="space-y-3 p-3 rounded-md border bg-destructive/10">
    ...
    <Button variant="destructive" ...>Confirm Revoke</Button>
  </div>
)}
```

**One-time token reveal pattern** (ForwarderTokenPage.tsx lines 83–127):
```tsx
// One-time reveal Alert + mono Input + Copy button
<div className="flex items-start gap-2 p-3 rounded-md bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800">
  <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
  <p className="text-sm text-amber-800 dark:text-amber-200">
    Copy this token now. It will not be shown again.
  </p>
</div>
<div className="flex gap-2">
  <Input value={newToken} readOnly className="font-mono text-sm" aria-label="Forwarder API token" />
  <Button variant="outline" size="icon" onClick={handleCopyToken} aria-label="Copy token to clipboard">
    <Copy className="h-4 w-4" />
  </Button>
</div>
```

**"Test connection" polling pattern** — copy from ForwarderStatusBar.tsx polling, adapted:
```tsx
// From ForwarderStatusBar.tsx lines 73–78 (refetchInterval pattern):
const MAX_ATTEMPTS = 15;  // 15 × 2s = 30s timeout (CONTEXT.md D-17)
const [attempts, setAttempts] = useState(0);
const [polling, setPolling] = useState(false);

const { data } = useQuery({
  queryKey: ['forwarder-status'],
  queryFn: fetchForwarderStatus,   // same fn as ForwarderStatusBar.fetchForwarderStatus
  staleTime: 0,
  enabled: polling && attempts < MAX_ATTEMPTS,
  refetchInterval: (polling && attempts < MAX_ATTEMPTS) ? 2000 : false,
});
```

**"Download forwarder.env" button** — file download via anchor tag pattern:
```tsx
// Browser-native download: create anchor, click, revoke
async function handleDownload() {
  const response = await api.get('/api/v1/setup/forwarder-config-download', {
    responseType: 'blob',
  });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const a = document.createElement('a');
  a.href = url;
  a.download = 'forwarder.env';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}
```

---

### `frontend/src/pages/setup/steps/StaffStep.tsx` (component, CRUD)

**Analog:** `frontend/src/pages/auth/RegisterPage.tsx`

**Form pattern** (RegisterPage.tsx lines 37–84):
```tsx
// Re-use registerSchema fields for the staff account form
// Staff form needs: firstName, lastName, email, password, confirmPassword, role selector
const staffSchema = z.object({
  firstName: z.string().min(1, 'First name required').max(100),
  lastName: z.string().min(1, 'Last name required').max(100),
  email: z.string().email('Valid email required'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
  role: z.enum(['ADMIN', 'RACE_DIRECTOR', 'REFEREE']),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});
```

**Axios 409 error handling** (RegisterPage.tsx lines 63–80):
```tsx
// Re-use the isAxiosError + status check pattern from RegisterPage
if (isAxiosError(err)) {
  if (err.response?.status === 409) {
    form.setError('email', { message: 'An account with this email already exists.' });
  } else {
    toast.error('Something went wrong. Check your connection and try again.');
  }
}
```

**Form with shadcn Form components** — use `<Form>`, `<FormField>`, `<FormMessage>` as in RegisterPage.tsx lines 99–178 (NOT the bare `register` pattern from ClubProfilePage).

---

### `frontend/src/hooks/setup/useSetupProgress.ts` (hook, request-response)

**Analog:** `frontend/src/hooks/admin/useAdminClub.ts`

**useQuery hook pattern** (useAdminClub.ts lines 1–11):
```typescript
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getSetupProgress, getSetupStatus } from '@/lib/setupApi';

export function useSetupStatus() {
  return useQuery({
    queryKey: ['setup-status'],
    queryFn: getSetupStatus,
    staleTime: 60_000,
    retry: false,
  });
}

export function useSetupProgress() {
  return useQuery({
    queryKey: ['setup-progress'],
    queryFn: getSetupProgress,
    staleTime: 0,   // always fresh — re-fetch on every step save (Pitfall 3)
  });
}
```

---

### `frontend/src/lib/setupApi.ts` (utility, request-response)

**Analog:** `frontend/src/lib/adminApi.ts` / `frontend/src/lib/raceControlApi.ts`

**API module pattern** (adminApi.ts top of file, raceControlApi.ts lines 1–18):
```typescript
import api from './api';

// ── Types ──────────────────────────────────────────────────────────────────

export type SetupStatusDto = { setupComplete: boolean };

export type SetupProgressDto = {
  club: boolean;
  track: boolean;
  format: boolean;
  staff: boolean;
  decoder: boolean;
};

export type BootstrapRequest = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
};

// ── API functions ──────────────────────────────────────────────────────────

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

---

### `frontend/src/App.tsx` (modify, config)

**Analog:** self

**Route addition pattern** (App.tsx lines 52–138 — add `/setup` entry in the same children array):
```tsx
// Add SetupGuard wrapping RootLayout children:
function RootLayout() {
  return (
    <AuthProvider>
      <SetupGuard>    {/* ADD: wraps all routes except /setup itself */}
        <Outlet />
        <Toaster />
      </SetupGuard>
    </AuthProvider>
  );
}

// Add /setup route (unprotected in router definition — guard is inside SetupLayout):
{ path: '/setup', element: <SetupLayout /> }
// SetupLayout handles pre-gate vs wizard rendering internally based on setupComplete + auth state
```

**Unprotected route pattern** (App.tsx lines 131–135 — existing unprotected routes):
```tsx
// Public routes: no ProtectedRoute wrapper
{ path: '/events', element: <EventSchedulePage /> },
{ path: '/results/:raceId', element: <PublicResultsPage /> },
// /setup follows the same pattern — no ProtectedRoute wrapper in the router definition
```

---

### `frontend/src/pages/admin/AdminPanelLayout.tsx` (modify, component)

**Analog:** self

**NavItem group pattern** (AdminPanelLayout.tsx lines 29–41 — add Setup Wizard to operationsGroup):
```tsx
// Add to operationsGroup (uses Lucide icon to match existing nav items — NOT remixicon):
import { Wand2 } from 'lucide-react';  // or Settings2 — choose Lucide icon matching nav style

const operationsGroup = [
  { to: '/admin/race-control', label: 'Race Control', Icon: Flag },
  { to: '/admin/forwarder', label: 'Forwarder Token', Icon: Radio },
  { to: '/admin/racers', label: 'Racers', Icon: Users },
  { to: '/setup', label: 'Setup Wizard', Icon: Wand2 },  // ADD: between Staff and others
] as const;
```

**Note:** Use a Lucide icon here (not remixicon) to match all other nav entries. The `ri-*` icons are only used inside `SetupLayout.tsx` step indicators.

---

### Integration test: `app/.../api/setup/SetupControllerIT.java`

**Analog:** `app/src/test/java/dev/monkeypatch/rctiming/api/admin/ClubControllerIT.java`

**Integration test pattern** (ClubControllerIT.java lines 1–53):
```java
package dev.monkeypatch.rctiming.api.setup;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SetupControllerIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    @Test
    void getStatus_returnsSetupComplete_false_whenNoClub() {
        ResponseEntity<SetupStatusDto> resp = restTemplate.getForEntity(
                "/api/v1/setup/status", SetupStatusDto.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().setupComplete()).isFalse();
    }

    @Test
    void bootstrap_returns409_whenUsersExist() {
        // Call bootstrap once (succeeds)
        // Call bootstrap again → expect 409
    }
}
```

**No @BeforeEach createAdminUser needed** — SetupControllerIT tests the pre-admin state; some tests call bootstrap first to create the user, then re-test protected endpoints with the returned JWT.

---

## Shared Patterns

### React Hook Form + shadcn Form components (all wizard step forms)

**Source:** `frontend/src/pages/auth/RegisterPage.tsx` lines 99–178
**Apply to:** `ClubProfileStep.tsx`, `TrackStep.tsx`, `FormatStep.tsx`, `StaffStep.tsx`, `DecoderConfigStep.tsx`

All wizard step forms MUST use `<Form>`, `<FormField>`, `<FormItem>`, `<FormLabel>`, `<FormControl>`, `<FormMessage>` — the shadcn Form components — rather than the bare `register` + error-span pattern used in `ClubProfilePage.tsx`. This matches the UI-SPEC validation contract ("Required fields: red `FormMessage` below the field").

```tsx
<Form {...form}>
  <form onSubmit={form.handleSubmit(onSave)} className="space-y-4">
    <FormField
      control={form.control}
      name="fieldName"
      render={({ field }) => (
        <FormItem>
          <FormLabel>Label</FormLabel>
          <FormControl><Input {...field} /></FormControl>
          <FormMessage />   {/* renders Zod error automatically */}
        </FormItem>
      )}
    />
  </form>
</Form>
```

### TanStack Query cache invalidation after step save (all wizard steps)

**Source:** `frontend/src/pages/admin/race-control/ForwarderTokenPage.tsx` lines 33–34
**Apply to:** Every step's `onSuccess` callback

```tsx
// Always invalidate BOTH queries after any wizard step saves successfully
queryClient.invalidateQueries({ queryKey: ['setup-status'] });
queryClient.invalidateQueries({ queryKey: ['setup-progress'] });
```

### Toast notifications (all wizard steps)

**Source:** `frontend/src/pages/admin/club/ClubProfilePage.tsx` lines 74–80
**Apply to:** All wizard step forms

```tsx
// Success
toast.success('Club profile saved');
// Error — generic catch
toast.error('Could not save. Try again.');
// Error — server validation
toast.error('Something went wrong. Check your connection and try again.');
```

### @PreAuthorize + constructor injection (all new Spring controllers/services)

**Source:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/ForwarderTokenController.java` lines 1–50
**Apply to:** `SetupController.java`, `SetupService.java`

Use constructor injection (not `@Autowired` field injection). Apply `@PreAuthorize` per-method for mixed public/gated controllers (not at class level).

### Java record DTOs with Bean Validation annotations

**Source:** `app/src/main/java/dev/monkeypatch/rctiming/api/auth/RegisterRequest.java` (pattern)
**Apply to:** `BootstrapRequest.java`, `SetupStatusDto.java`, `SetupProgressDto.java`

All new DTOs are Java records. Request DTOs use `jakarta.validation.constraints.*` annotations. Response DTOs have no validation annotations.

---

## No Analog Found

All new files have close analogs in the codebase. No entries.

---

## Metadata

**Analog search scope:** `app/src/main/java/`, `app/src/test/java/`, `frontend/src/`
**Files scanned:** 21
**Pattern extraction date:** 2026-05-03
