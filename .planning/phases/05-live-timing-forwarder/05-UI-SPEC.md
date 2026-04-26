---
phase: 5
slug: live-timing-forwarder
status: draft
shadcn_initialized: true
preset: "style=radix-luma, baseColor=taupe, cssVariables=true, iconLibrary=remixicon"
created: 2026-04-26
---

# Phase 5 — UI Design Contract

> Visual and interaction contract for the three new frontend surfaces added in Phase 5:
> 1. Status bar — decoder/forwarder connection pills in the race control cockpit
> 2. Unknown transponder link dialog — in-race linking with retroactive credit
> 3. Admin token management page — generate, display, revoke forwarder API token

---

## Design System

| Property | Value | Source |
|----------|-------|--------|
| Tool | shadcn | components.json |
| Preset | radix-luma | components.json |
| Base color | taupe | components.json |
| Component library | Radix UI (via shadcn) | components.json |
| Icon library | Remixicon | components.json |
| Font — UI/body | Figtree Variable | index.css |
| Font — display/racing | Barlow Condensed | index.css |
| Font — lap times/mono | JetBrains Mono Variable | index.css |
| Tailwind version | v4 (CSS custom properties, no tailwind.config) | index.css |

No third-party registries. Registry safety gate: not applicable.

---

## Spacing Scale

Standard 8-point scale. All values are multiples of 4.

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4px | Icon-to-label gaps, dot-to-text gap within a status pill |
| sm | 8px | Pill internal horizontal padding, compact button padding |
| md | 16px | Dialog section gaps, token field padding, default element spacing |
| lg | 24px | Dialog section breaks, card padding |
| xl | 32px | Major panel spacing |
| 2xl | 48px | Page-level section breaks |
| 3xl | 64px | Not used in Phase 5 surfaces |

Exceptions:
- Status bar height: 32px (h-8) — slim intentionally; matches the 8-point scale at 4×8
- Status dot (indicator circle): 8px (w-2 h-2) — follows existing `LiveTimingPanel` dot pattern
- Touch targets on status bar pills: minimum 32×32px for mouse use; these are read-only indicators, not interactive

---

## Typography

Three roles used in Phase 5 surfaces. Display role is not used (status bar and dialogs are operational UI, not racing display).

| Role | Size | Weight | Line Height | Font | Usage |
|------|------|--------|-------------|------|-------|
| Body | 14px (text-sm) | 400 (regular) | 1.5 | Figtree Variable | Status pill labels, dialog body text, token page body copy |
| Label | 12px (text-xs) | 500 (medium) | 1.4 | Figtree Variable | Status dot annotations, table column headers, helper text |
| Heading | 16px (text-base) | 600 (semibold) | 1.2 | Figtree Variable | Dialog title, page section headings |
| Mono | 13px (text-xs in .laptime / .mono class) | 400 (regular) | 1.0 | JetBrains Mono Variable | API token value display, transponder IDs in link dialog |

Rules:
- Lap times and transponder IDs always use `.mono` / `font-mono` with `tabular-nums` — from existing `index.css` `.laptime` rule.
- Token string displayed in admin page uses `font-mono text-sm` — legible but monospaced for accurate copy/paste.
- Two weights in use: 400 (regular) and 600 (semibold). 500 (medium) used only for label/annotation text and is within the two-weight spirit of the system.

---

## Color

All values are CSS custom properties from the existing `index.css` color palette. Do not hard-code hex or oklch literals.

| Role | CSS Variable | Approx. Value | Usage |
|------|-------------|---------------|-------|
| Dominant (60%) | `--background` | oklch(0.985 0.002 60) light / oklch(0.120 0.003 250) dark | Page background, cockpit shell |
| Secondary (30%) | `--card` / `--secondary` | oklch(1 0 0) light / oklch(0.185 0.004 250) dark | Status bar container, dialog surface, token card |
| Accent (10%) | `--primary` | oklch(0.702 0.187 41.8) — Glasgow RC orange | Generate token button, focus rings |
| Destructive | `--destructive` | oklch(0.605 0.235 27) | Revoke token button and confirmation text only |

**Status pill color semantics — use race-domain semantic tokens, not generic colors:**

| Pill State | Dot | Pill variant | Tailwind class |
|------------|-----|-------------|----------------|
| Connected | `--flag-green` → `bg-[var(--flag-green)]` | shadcn `badge` outline | `text-[var(--flag-green)]` |
| Reconnecting | `--flag-yellow` → `bg-[var(--flag-yellow)]` | shadcn `badge` outline | `text-[var(--flag-yellow)]` |
| Disconnected | `--flag-red` → `bg-[var(--flag-red)]` | shadcn `badge` outline | `text-[var(--flag-red)]` |

Rationale: `--flag-green/yellow/red` are already defined in `index.css` for racing semantics. Using them for connection status is a domain-consistent reuse.

Accent reserved for:
- Primary action button: "Generate Token" / "Regenerate Token" on admin token page
- Focus ring on the select-entry combobox in the transponder link dialog

Accent is NOT used for: status pills, table rows, section headings, error text, or the token display field.

---

## Component Inventory

All components already installed in `frontend/src/components/ui/`. No new shadcn installs required for Phase 5.

| Component | Usage in Phase 5 |
|-----------|-----------------|
| `Badge` | Status pills (DECODER / FORWARDER) — use `variant="outline"` with custom dot span inside |
| `Dialog` | Unknown transponder link dialog — use existing `dialog.tsx` |
| `Select` | Entry selector in link dialog |
| `Button` | Generate token, Revoke token, Link transponder confirm |
| `Card` | Token management page container |
| `Table` | Token status row (generated date, last used, actions) |
| `Separator` | Status bar separator from header (already used in `RaceControlLayout`) |
| `Input` | Token display field (read-only, `readOnly`, monospace class) |
| `Badge` | Token status indicator (Active / Revoked) |
| `Sonner (toast)` | Success toast on token copy, error toast on link failure |

New component to create (no existing pattern):
- `ForwarderStatusBar` — `frontend/src/components/race-control/ForwarderStatusBar.tsx`
  - Slim bar (h-8) inserted between the existing `<Separator />` and the `<div className="flex-1 overflow-hidden">` in `RaceControlLayout.tsx`
  - Contains two `Badge` pills with animated dot
  - Subscribes to `/topic/system/forwarder-status` via the existing `useStomp` hook

---

## Surface Specifications

### Surface 1 — Forwarder Status Bar

**Location:** `RaceControlLayout.tsx` — inserted as a new child between the existing `<Separator />` and the page content `<div className="flex-1 overflow-hidden">`.

**Layout:** `flex h-8 items-center gap-3 px-4 bg-card border-b shrink-0`

**Content:** Two pills, left-aligned. No other controls.

```
[ ● DECODER connected ]  [ ● FORWARDER connected ]
```

**Pill anatomy:**
- A `<span>` dot: `inline-block h-2 w-2 rounded-full` with color from semantic token table above
- Connected state dot: add `animate-pulse` class (matches existing `LiveTimingPanel` dot pattern)
- Reconnecting state dot: add `animate-spin` class on a spinner variant — or `animate-pulse` with amber; executor's discretion
- Label text: `text-xs font-medium` using `text-[var(--flag-*)]` matching dot state
- Wrapper: shadcn `Badge` with `variant="outline"` and `className="gap-1.5 h-6 px-2.5"`

**States:**

| State | Dot color token | Label |
|-------|----------------|-------|
| Connected | `--flag-green` | DECODER connected / FORWARDER connected |
| Reconnecting | `--flag-yellow` | DECODER reconnecting… / FORWARDER reconnecting… |
| Disconnected | `--flag-red` | DECODER disconnected / FORWARDER disconnected |
| Unknown (no status received yet) | `--muted-foreground` | DECODER — / FORWARDER — |

**Data source:** STOMP topic `/topic/system/forwarder-status`. Use existing `useStomp<ForwarderStatusDto>` pattern. Show "unknown" state until first message received.

---

### Surface 2 — Unknown Transponder Link Dialog

**Trigger:** Existing cockpit alert (broadcast on `/topic/race/{raceId}/unknown-transponder`). The alert already exists from Phase 4. Phase 5 adds a "Link to entry" button that opens this dialog.

**Dialog title:** "Link Unknown Transponder"

**Dialog size:** `sm:max-w-md` (follows existing `IncidentDialog` and `PenaltyDialog` pattern)

**Layout:**

```
[Dialog header]
  Link Unknown Transponder

[Body]
  Transponder ID
  ┌─────────────────────────────────┐
  │  00123456  (monospace, read-only) │
  └─────────────────────────────────┘

  [Info text, text-xs text-muted-foreground]
  This transponder has N passing(s) since race start.
  All will be credited to the linked entry immediately.

  Link to entry
  ┌─────────────────────────────────┐
  │  Select entry…                  ▼│
  └─────────────────────────────────┘
  [entries from current race shown as "Car # — Racer Name"]

[Footer]
  [Cancel]  [Link Entry →]
```

**Interaction rules:**
- Transponder ID field: `<Input readOnly className="font-mono text-sm" />`
- Entry selector: shadcn `Select` component; options populated from current race entries; labeled `"Car {carNumber} — {racerName}"`; no raw IDs shown (per project memory `feedback_no_ids_in_ui.md`)
- "Link Entry" button: `variant="default"` (primary/orange), disabled until an entry is selected
- On confirm: call `PATCH /api/v1/race-control/race/{raceId}/unknown-transponder/{transponderId}/link` (or equivalent); show loading state on button
- On success: close dialog, show Sonner toast "Transponder linked. N laps credited to {racerName}."
- On error: show Sonner toast (error variant) "Failed to link transponder. {message}"

**Destructive consideration:** Linking is not destructive but is irreversible mid-race. No confirmation step — the dialog itself is the confirmation. The "Link Entry" button label is explicit enough.

---

### Surface 3 — Admin Token Management Page

**Location:** Admin panel — new page at `/admin/forwarder-token` linked from the admin sidebar.

**Page title:** "Forwarder Token"

**Breadcrumb / nav label:** "Forwarder Token" (in admin sidebar under a "System" section or standalone nav item)

**Layout:** Single-column content area, `max-w-2xl`, centered in the admin panel content zone (follows existing admin page pattern from `ClubProfilePage.tsx`).

**States:**

**State A — No token generated yet:**

```
┌─ Card ──────────────────────────────────────┐
│  Forwarder API Token                         │
│  ──────────────────────────────────────────  │
│  No token has been generated yet.            │
│  Generate a token to allow the forwarder     │
│  application to connect to this service.     │
│                                              │
│  [Generate Token]                            │
└──────────────────────────────────────────────┘
```

**State B — Token exists (normal view):**

```
┌─ Card ──────────────────────────────────────┐
│  Forwarder API Token                         │
│  ──────────────────────────────────────────  │
│  Status    ● Active                          │
│  Generated  26 Apr 2026, 09:14               │
│                                              │
│  The token value is not shown for security.  │
│  Regenerate to get a new token.              │
│                                              │
│  [Regenerate Token]   [Revoke Token]         │
└──────────────────────────────────────────────┘
```

**State C — Token just generated / regenerated (one-time reveal):**

```
┌─ Card ──────────────────────────────────────┐
│  Forwarder API Token                         │
│  ──────────────────────────────────────────  │
│  ⚠ Copy this token now. It will not be       │
│  shown again.                                │
│                                              │
│  ┌─────────────────────────────────── [Copy]│
│  │  eyJhbGciOiJIUzI1NiIsInR5cCI6...  (mono) │
│  └───────────────────────────────────────── │
│                                              │
│  Paste this value into forwarder.properties  │
│  as forwarder.api-token=<value>              │
│                                              │
│  [Done]                                      │
└──────────────────────────────────────────────┘
```

**State D — Token revoked:**

```
┌─ Card ──────────────────────────────────────┐
│  Forwarder API Token                         │
│  ──────────────────────────────────────────  │
│  Status    ○ Revoked                         │
│  Revoked   26 Apr 2026, 11:32                │
│                                              │
│  The forwarder cannot connect until a new   │
│  token is generated.                         │
│                                              │
│  [Generate Token]                            │
└──────────────────────────────────────────────┘
```

**Interaction rules:**
- "Copy" icon button next to token field: copies to clipboard, shows Sonner toast "Token copied to clipboard."
- "Regenerate Token" button: `variant="outline"` — opens inline confirmation (not a separate dialog): replaces button area with `"Regenerating will disconnect the forwarder until you update its config. Continue? [Cancel] [Confirm Regenerate]"` — inline, no modal
- "Revoke Token" button: `variant="destructive"` — opens same inline confirmation pattern: `"Revoking will disconnect the forwarder immediately. Continue? [Cancel] [Confirm Revoke]"`
- "Generate Token" / "Confirm Regenerate" buttons: `variant="default"` (primary/orange)
- Token display field: `<Input readOnly className="font-mono text-sm" />`, full width
- No raw database IDs shown anywhere on this page

---

## Copywriting Contract

| Element | Copy |
|---------|------|
| Primary CTA — generate new token | "Generate Token" |
| Primary CTA — after token exists | "Regenerate Token" |
| Primary CTA — transponder link | "Link Entry" |
| Empty state — no token generated | "No token has been generated yet. Generate a token to allow the forwarder application to connect to this service." |
| Empty state — no unknown transponders | (Not a surface in this phase — unknown transponder alerts are transient toasts/alerts, not a list) |
| One-time reveal warning | "Copy this token now. It will not be shown again." |
| Regenerate inline confirmation | "Regenerating will disconnect the forwarder until you update its config. Continue?" |
| Revoke inline confirmation | "Revoking will disconnect the forwarder immediately. Continue?" |
| Confirm revoke button | "Confirm Revoke" |
| Confirm regenerate button | "Confirm Regenerate" |
| Token copied toast | "Token copied to clipboard." |
| Transponder linked success toast | "Transponder linked. {N} lap(s) credited to {racerName}." |
| Transponder link error toast | "Failed to link transponder. {serverMessage}" |
| Token page sidebar label | "Forwarder Token" |
| Revoked status badge | "Revoked" (Badge variant="destructive") |
| Active status badge | "Active" (Badge variant="outline" with green dot) |
| Status bar pill — connected | "DECODER connected" / "FORWARDER connected" |
| Status bar pill — disconnected | "DECODER disconnected" / "FORWARDER disconnected" |
| Status bar pill — reconnecting | "DECODER reconnecting…" / "FORWARDER reconnecting…" |
| Status bar pill — unknown | "DECODER —" / "FORWARDER —" |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | dialog, select, button, card, table, input, badge, separator, switch, sonner (all pre-installed) | not required |

No third-party registries declared. Registry vetting gate: not applicable.

---

## Accessibility Notes

- Status bar pills: each `<Badge>` must have a descriptive `aria-label` — e.g., `aria-label="Decoder connection status: connected"`. The colored dot is `aria-hidden="true"`.
- Transponder link dialog: focus must move to the dialog on open (`autoFocus` on the entry Select); "Link Entry" button must be `disabled` until selection is made; all form fields have visible labels.
- Token copy button: `aria-label="Copy token to clipboard"`.
- Revoke/Regenerate destructive confirmation: inline copy appears in the same card without moving focus; keyboard users can Tab to the Cancel/Confirm buttons naturally.

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS

**Approval:** pending
