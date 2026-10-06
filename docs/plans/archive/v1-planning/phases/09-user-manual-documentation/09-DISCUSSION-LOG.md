# Phase 9: User Manual & Documentation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-14
**Phase:** 9-user-manual-documentation
**Areas discussed:** In-app help surface, Printable guide format, Content home & format, Coverage depth

---

## In-App Help Surface

### How should the contextual '?' help trigger per page?

| Option | Description | Selected |
|--------|-------------|----------|
| Slide-out drawer | Help panel slides in from right, user stays in context, one '?' icon in page header | ✓ |
| Dedicated /help/ routes | '?' navigates to /help/race-control etc., full pages with ToC and bookmarkable URLs | |
| Inline page callout | Collapsible info section at top of each page, always visible on first visit | |

**User's choice:** Slide-out drawer

---

### Where does the '?' icon live on each page?

| Option | Description | Selected |
|--------|-------------|----------|
| Page header, top-right | Small '?' icon button in top-right of each page's header bar, consistent across all pages | ✓ |
| Floating action button | Fixed-position floating '?' button (bottom-right), present everywhere at all times | |
| Sidebar nav entry | 'Help' link in the sidebar navigation for each section | |

**User's choice:** Page header, top-right

---

### Should every page have its own help content, or just key workflow pages?

| Option | Description | Selected |
|--------|-------------|----------|
| Key workflow pages only | Help content for ~12 high-impact pages; simpler pages like login/print don't get help drawers | ✓ |
| Every page with a dedicated route | All ~25 pages get help content including simpler ones | |
| You decide | Claude picks which pages are high-enough priority | |

**User's choice:** Key workflow pages only

---

## Printable Guide Format

### How should the Race Meeting Guide and other printable guides be delivered?

| Option | Description | Selected |
|--------|-------------|----------|
| Print CSS React pages | /print/meeting-guide routes with @media print CSS, same pattern as PrintResultsPage | ✓ |
| Pre-authored PDFs in repo | Write PDFs externally, commit to docs/pdf/, link as downloads | |
| @react-pdf/renderer | Programmatic PDF generation from React components, new dependency | |

**User's choice:** Print CSS React pages (consistent with existing print page pattern)

---

### How many distinct printable guides should exist?

| Option | Description | Selected |
|--------|-------------|----------|
| Three guides | Race Meeting Guide (officials), Racer Quick-Start Guide, Admin Configuration Guide | ✓ |
| One combined guide | Single comprehensive PDF covering all three audiences | |
| Two guides | Officials Race Day Guide + combined Racer & Admin setup guide | |

**User's choice:** Three guides

---

### Should the guides be accessible from in-app navigation, or just via direct URL?

| Option | Description | Selected |
|--------|-------------|----------|
| In-app links in help drawer | 'Print this guide' link at bottom of relevant help articles | ✓ |
| Direct URL only | /print/* routes exist but not linked from main app UI | |
| Dedicated Guides page | /admin/guides page listing all three guides with preview thumbnails | |

**User's choice:** In-app links in help drawer

---

## Content Home & Format

### Where does the help article content live in the codebase?

| Option | Description | Selected |
|--------|-------------|----------|
| Plain JSX components | frontend/src/help/*.tsx, content written as JSX, no extra build plugins | ✓ |
| Markdown files via Vite ?raw | .md files imported as raw strings, rendered with react-markdown at runtime | |
| MDX files via @mdx-js/rollup | MDX processed at build time, most flexible but adds Vite plugin dependency | |

**User's choice:** Plain JSX components

---

### How should pages declare which help component they use?

| Option | Description | Selected |
|--------|-------------|----------|
| useHelp() hook + HelpProvider | Context wraps app, pages call useHelp({ content: <XxxHelp /> }), layouts render the drawer | ✓ |
| Prop drilling from page | Pages wrap in <PageWithHelp helpContent={...}> component | |
| Route-based lookup map | Static map of route path → help component in a central file | |

**User's choice:** useHelp() hook + HelpProvider

---

## Coverage Depth

### How deep should in-app help content be per page?

| Option | Description | Selected |
|--------|-------------|----------|
| Brief + key actions | 2-3 sentences + 3-5 key action bullets + common mistakes note | ✓ |
| Step-by-step walkthroughs | Numbered workflow steps for each page | |
| Mix by complexity | Brief blurbs for simple pages, step-by-step for complex pages | |

**User's choice:** Brief + key actions

---

### Should the printable guides be more detailed than the in-app help?

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — guides are comprehensive | In-app help is brief reminders; printable guides are full workflow documents | ✓ |
| Same depth as in-app help | Guides mirror the drawer content, just formatted for print | |
| You decide | Claude determines appropriate depth for each guide | |

**User's choice:** Yes — guides are comprehensive

---

### Who writes the actual documentation content?

| Option | Description | Selected |
|--------|-------------|----------|
| Claude writes it all | Claude authors all content from implemented codebase; David reviews | ✓ |
| Stubs only — you fill content | Claude builds infrastructure with TODO placeholder content | |
| Mix — Claude writes, flag uncertain areas | Claude writes all content, marks uncertain sections with [REVIEW: ...] | |

**User's choice:** Claude writes it all

---

## Claude's Discretion

- Exact shadcn/ui Sheet trigger placement within each layout's header
- Visual styling of the '?' button (ghost variant, icon size, aria-label)
- Whether HelpProvider passes a React node or a component reference
- Print guide page layout (margins, font sizes, section breaks, club name injection)
- Final list of ~12 key workflow pages within the implemented page list

## Deferred Ideas

None — discussion stayed within phase scope.
