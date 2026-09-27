<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Cockpit frontend engineering guide: App Router rules, route isolation, tokens, forms, markings, accessibility, i18n, performance, maps/graph, security, testing (P0) -->

# Vigil — Frontend Engineering Guide (Cockpit)

> **Status:** P0 v1.0 (2026-09-25). Owned by P0; material changes need an ADR.
> **Scope:** `apps/cockpit/` (P10, P13, P18, P19, P31). The Android field app follows [`BACKEND_GUIDE.md`](BACKEND_GUIDE.md) §6.
> **Read with:** [`INSTRUCTIONS.md`](../INSTRUCTIONS.md), [`ARCHITECTURE.md`](ARCHITECTURE.md) 🧊 (§3, §6.2, §10), [`API_DESIGN.md`](API_DESIGN.md), [`SECURITY_AND_COMPLIANCE.md`](SECURITY_AND_COMPLIANCE.md), [`SYSTEM_COMPLIANCE_GUARDRAILS.md`](SYSTEM_COMPLIANCE_GUARDRAILS.md) §4.
> Where this guide names a library that ARCHITECTURE does not, it is the **default** the owning phase adopts unless its §4.1 evaluation (INSTRUCTIONS §4.1) finds a better option and records it.

## 1. Stack

| Concern | Choice | Notes |
|---|---|---|
| Framework | Next.js (App Router) + React | Versions pinned by P1 |
| Language | TypeScript 7, `strict: true`, `noUncheckedIndexedAccess: true` | TS 7's stable programmatic API arrives in 7.1, so type-aware lint may need the TS 6 compatibility package until then (appendix 05 §1.5). P1 pins it. |
| Package manager | pnpm, `pnpm-lock.yaml` committed | `minimumReleaseAge` and a build-script allow-list against compromised fresh releases (P1 sets the values) |
| Styling | Tailwind CSS over Vigil design tokens (§5) | No CSS-in-JS runtime |
| API client | Generated Connect-ES client (P2) + TanStack Query | §4 |
| Forms | **React Hook Form + Zod**, schemas generated from Action-type metadata | §7 (decision) |
| Accessible primitives | React Aria Components, wrapped by P10 in `components/ui/` | Features never import primitives directly |
| i18n | ICU MessageFormat catalogues, via next-intl | §10 |
| Maps · 3D · graph | MapLibre GL JS + deck.gl + PMTiles · CesiumJS (self-hosted terrain) · Sigma.js + graphology | §12 |
| Tests | Vitest + Testing Library · Playwright · axe-core | §14 |

## 2. Layout and route isolation (Rule 3)

```
apps/cockpit/src/
├── app/
│   ├── layout.tsx            # P10: root layout, providers, classification banner, skip link
│   ├── (shell)/              # P10: home, navigation, tenant/space switcher, purpose/case selector, settings
│   ├── (explore)/            # P13: search, explorer, object pages, Action forms
│   ├── (map)/                # P18: Map / common operating picture
│   ├── (cases)/              # P19: link graph and case workbench
│   └── (builder)/            # P31: app builder
├── features/
│   ├── objects/  actions/    # P13
│   ├── map/                  # P18
│   ├── graph/  cases/        # P19
│   └── builder/              # P31
├── components/ui/            # P10: the component library (§6)
├── design/                   # P10: design tokens (§5)
└── lib/{auth,api}/           # P10: session, BFF calls, generated client wrapper, policy-context headers
```

- **One phase, one route group.** Each group has its own `layout.tsx`, `loading.tsx`, `error.tsx` and `not-found.tsx`. There is no shared router or central route registry; navigation entries are discovered from each group's exported manifest (Rule 3).
- **Features never import each other.** They may import `components/ui`, `design` and `lib`. A case page shows an object by **linking to its URL** in `(explore)`, not by importing P13 code. Lint enforces this (`import/no-restricted-paths`).
- A feature that needs a new shared component asks P10 through a 🔌 INTERFACE message and meanwhile keeps a local, clearly named stand-in inside its own feature folder (Rule 4).

## 3. Server Components and Client Components

**Default to Server Components.** Add `"use client"` only for interactivity: forms, maps, the link graph, drag-and-drop, live subscriptions, the justification dialog.

| Rule | Why |
|---|---|
| Fetch data on the server, in Server Components or route handlers, through `lib/api` | The browser never holds API tokens (§13); less JavaScript reaches low-bandwidth sites |
| Keep Client Components as leaves; pass them plain, serialisable, already-redacted props | Redaction happens server-side (Warden/OSS). The client never receives a withheld value to hide. |
| Mark server-only modules with `import "server-only"` | Build fails if a secret-bearing module leaks into a client bundle |
| **Never cache tenant data in Next.js caches** (`fetch` cache, `unstable_cache`, `"use cache"`) | Those caches are keyed by URL, not by policy context, so they could serve one user's data to another. Core caches results keyed by `policy_ctx_hash` (ARCHITECTURE §10). Data routes are dynamic. |
| Stream with `<Suspense>` and show the **"data as of"** watermark on every data view | Indexer lag is normal (ARCHITECTURE §12); users must see freshness |
| Next.js Server Actions are **not** used to submit Vigil Actions | One submission path, the Action engine contract, with idempotency and approval semantics (§7) |

## 4. Data fetching

- All calls go through `lib/api` (P10), which wraps the **generated Connect-ES client**. Features never build URLs or headers by hand.
- The `lib/api` interceptor adds the headers from API_DESIGN §A.2 using the active selection: `X-Vigil-Tenant`, `X-Vigil-Purpose`, `X-Vigil-Case-Id` (when a case is active), `X-Vigil-Justification` (after a checkpoint), `X-Vigil-Watermark` (read-your-writes), `X-Request-Id` and W3C `traceparent`. Mutations also send `Idempotency-Key`.
- Client-side reads use **TanStack Query** through Connect-Query. **Query keys include tenant, purpose and case**, and switching any of them clears the query cache (`queryClient.clear()`), so results never cross contexts.
- Errors arrive in the API_DESIGN §A.6 envelope. `lib/api` maps `JUSTIFICATION_REQUIRED` to the justification prompt (§8.3), `PURPOSE_REQUIRED` and `CASE_REQUIRED` to the purpose and case selector, `WATERMARK_PENDING` and `RATE_LIMITED` to a retry that honours `Retry-After`, and `UNAVAILABLE` from Mind to the no-AI path. Everything else goes to the route's `error.tsx` with the `request_id`.
- **No optimistic updates for Actions.** Show "pending" until the engine confirms the commit, then render the edit overlay until the watermark catches up (ARCHITECTURE §6.1, P13.4).
- Lists paginate on the server with opaque page tokens (API_DESIGN §A.4). The UI requests 50 rows by default (the API default is 100, maximum 500), and `total_estimate` is shown as approximate.

## 5. Tailwind and design tokens (`apps/cockpit/src/design/`, P10)

- Tokens are the single source of colour, typography, spacing, radius, elevation and motion. They are defined in `design/tokens.ts`, emitted as CSS variables (`--vigil-color-surface`, `--vigil-space-4`) and mapped into the Tailwind theme.
- **No raw values in components:** no hex colours, pixel spacing or font stacks outside `design/`. Lint rejects arbitrary values such as `bg-[#123456]`.
- Light, dark and high-contrast themes. A CI script checks every declared foreground/background token pair against WCAG 2.1 AA (§9) and fails on any pair below its threshold.
- Classification and marking colours are tokens too, but **colour never carries meaning alone**: every badge also has text and an icon (§8.1).
- Fonts are self-hosted with `next/font/local` and cover the characters of every South African language on the roadmap (for example ḓ ḽ ṋ ṱ in Tshivenḓa and š in Sesotho sa Leboa). `next/font/google` is banned: it downloads at build time, which breaks air-gapped builds.

## 6. Component library (`apps/cockpit/src/components/ui/`, owned by P10)

P10 owns every shared component (P10.4). Feature phases consume it and request changes through 🔌 INTERFACE (P13 Exclusion Zone). The minimum set:

| Component | Contract |
|---|---|
| `DataTable` | Keyboard-navigable grid, server pagination, column-level redaction placeholders, "data as of" footer |
| Form fields (`TextField`, `NumberField`, `DateField`, `ObjectRefPicker`, `GeoPointField`, …) | Labels, descriptions and errors wired for assistive technology; controlled by React Hook Form |
| `Dialog`, `Drawer`, `Toast` | Focus trap, `Esc` closes (except where it would cancel a pending submission), announced status |
| `MarkingBadge`, `ClassificationBanner` | §8.1 |
| `RedactedValue` | §8.2 |
| `JustificationPrompt` | §8.3 |
| `DataAsOf`, `EmptyState`, `ErrorPanel` | Freshness, empty and error states with the request ID and no internal detail |

**Every component documents how it renders a redacted property** (the P10 Done-when), including in tables, charts, map tooltips and exports.

## 7. Forms: React Hook Form + Zod generated from Action-type metadata (decision)

**Decision.** Action forms (P13.3) use **React Hook Form** for state and **Zod** for client-side validation. The Zod schema is **generated at runtime from the Action type's parameter metadata** served by OMS, so a form can never drift from its Action type.
*Alternatives considered:* TanStack Form (younger ecosystem) and Conform with Next.js Server Actions (rejected in §3). React Hook Form keeps re-renders low on low-end devices, and its uncontrolled inputs work well with accessible primitives.

```tsx
// features/actions/ActionForm.tsx (P13), illustrative
const schema = useMemo(() => zodFromActionType(actionType), [actionType]); // string, integer, decimal, date,
                                                                         // timestamp, geopoint, object ref, arrays
const form = useForm<ActionParams>({ resolver: zodResolver(schema), mode: "onBlur" });
const submit = useSubmitAction(actionType.rid);           // lib/api: Idempotency-Key + policy-context headers

async function onSubmit(values: ActionParams) {
  const result = await submit.mutateAsync({ values, idempotencyKey: attemptKey.current }); // same key on retry
  if (result.kind === "validation") applyServerErrors(form.setError, result.fieldErrors);  // VigilError → fields
  if (result.kind === "staged") announce(t("action.stagedForApproval"));                   // requires_human_approval
}
```

- **Client validation is a courtesy; the server decides.** Submission criteria, bounds and invariants are re-checked by the Action engine, and its field errors map back onto the form.
- One idempotency key per submission attempt, reused on retry. The submit button is disabled while a request is in flight.
- If the Action type requires a justification, the form includes the justification field (§8.3) and it cannot be pre-filled.
- Actions flagged `requires_human_approval` show "Staged for approval", the approver role and the approval state. Nothing implies the outcome has happened (POPIA s71).
- Parameters that would reveal marked values show `RedactedValue` placeholders, and pickers only offer objects the user can see.

## 8. Markings, redaction and justification

### 8.1 Marking badges and the classification banner

- `MarkingBadge` renders a label as **text, icon and token colour** together, for example `CONFIDENTIAL · PII · REL-ZA`, with an accessible name that reads the full labels.
- Badges show only the labels the caller already satisfies: the API's `security` block contains nothing else (API_DESIGN §A.3). Withheld content never shows marking names (§8.2).
- `ClassificationBanner` (top and bottom of the viewport) shows the **highest** classification of anything currently on screen, and updates as content changes. Exports and printouts carry the same banner.
- The classification ladder and marking vocabulary are defined in `docs/VIGIL_DOMAIN_BLUEPRINT.md` §4. The Cockpit displays them and never re-derives them.

### 8.2 Redacted properties

- The server sends an explicit redacted value, `{"redacted": true, "reason": "MARKING"}` (Blueprint W2; API_DESIGN §A.8), never `null` (BACKEND_GUIDE §2.6). `RedactedValue` renders a neutral placeholder ("Withheld") and an accessible description ("Value withheld: you do not hold the required marking").
- **No existence oracle** (Blueprint W11): an object the user may not see comes back as "not found", and the UI must never distinguish "forbidden" from "does not exist", in messages, timing or layout.
- The `reason` (`MARKING`, `CLASSIFICATION`, `RELEASABILITY`, `PURPOSE`, `CASE`, `LINKAGE_LOCKED`, `ERASED`, `NO_AI`) selects the wording. Marking names are never shown (API_DESIGN §A.8).
- `cipher_text` for a holder of the column marking arrives as `{"masked": true, "display": "******4521"}`. Showing the clear value needs an explicit **Reveal** action, a `REVEAL` justification (§8.3) and an audit event. The revealed value is never cached or kept in client state after the view closes.
- **Never reveal the shape of withheld data:** no length, no first characters, no count of hidden items unless the server supplies it as a permitted aggregate.
- There is no client-side "unredact". Requesting access goes to the access-request flow (P3), which is audited.
- Copy, CSV export, chart tooltips, map popups and graph labels all keep the placeholder. Components are tested for this.

### 8.3 Justification prompt (Checkpoints)

- Triggered when the API answers `JUSTIFICATION_REQUIRED` (P3.5), or before an Action type that demands a justification.
- A modal `JustificationPrompt`: a required free-text field of 20–2,000 characters (Blueprint W7), an optional case selector, the stated purpose, and a notice that the justification is recorded and reviewable.
- On submit, `lib/api` records the justification and retries the original request with the returned reference. Cancel abandons the operation: nothing is fetched.
- No autofill, no suggested wording, no "remember my reason". A justification is bound to the user, purpose, case, checkpoint and scope, and lasts at most 8 hours (W7). The UI reuses it only within that binding and asks again when it lapses.
- Accessibility: `role="dialog"`, `aria-modal="true"`, labelled by its title, focus moved to the text field on open and returned to the trigger on close.

## 9. Accessibility: WCAG 2.1 AA (P10 Done-when: axe, 0 serious violations)

| Area | Requirement |
|---|---|
| Contrast | Text ≥ 4.5:1; large text ≥ 3:1; UI components, focus rings and meaningful graphics ≥ 3:1 (SC 1.4.3, 1.4.11). Verified per token pair in CI (§5). |
| Keyboard | Everything works by keyboard (2.1.1) with no traps (2.1.2); logical focus order (2.4.3); a skip link to the main content; roving tabindex inside grids and toolbars |
| Focus | Always visible (2.4.7), never removed with `outline: none` without a token-based replacement |
| ARIA | Native elements first; landmarks on every page; `aria-live="polite"` for async status (staged, committed, "data as of" changes), used sparingly |
| Forms | Programmatic labels, error identification and suggestions (3.3.1, 3.3.3); errors linked with `aria-describedby` |
| Non-text content | Maps, charts and graphs have text alternatives: a **list or table view of the same object set** with the same filters (1.1.1) |
| Reflow and zoom | Usable at 320 CSS px and at 200% text size (1.4.4, 1.4.10) |
| Motion and time | Respect `prefers-reduced-motion`; session-expiry warnings give the user a way to extend the session (2.2.1) |
| Colour | Never the only carrier of meaning (1.4.1), including marking badges and map layers |

**Testing:** axe-core in component tests and in Playwright (`@axe-core/playwright`). CI fails on any **serious or critical** violation. Before each release, a keyboard-only walkthrough and a screen-reader pass (NVDA + Firefox) of the critical journeys. Target sizes of at least 24 × 24 CSS px are a house rule (from WCAG 2.2).

## 10. Internationalisation

- **English (South Africa) first.** South African languages follow, starting with those named by the first customers, then the rest of the official languages.
- **No hard-coded user-facing strings.** All copy lives in ICU MessageFormat catalogues keyed by feature (`objects.page.title`). Lint enforces this (`react/jsx-no-literals`); test IDs and log keys are exempt.
- Dates, numbers and currency are formatted with `Intl` for the user's locale, `en-ZA` by default. Times are stored in UTC and shown with an explicit time zone (default `Africa/Johannesburg`). Money is shown in ZAR unless the data says otherwise.
- Allow for about 30% text expansion. CI renders a pseudo-locale to catch truncation and concatenated strings.
- Data values (names, places) are rendered exactly as stored: no transliteration, no case changes. Entity resolution handles name variants on the server (P16).

## 11. Performance for low-bandwidth and sovereign sites

- **Nothing loads from outside the deployment:** no external CDNs, web fonts, analytics, session replay, error-reporting SaaS, map tiles or icon services (ADR-0004; the CSP in §13 enforces it). All assets are built into the image and served by the Cockpit.
- **Budgets, checked in CI against the build manifest:**

  | Budget | Limit (compressed) |
  |---|---|
  | First-load JavaScript for `(shell)` | ≤ 180 KB |
  | Additional JavaScript per route group | ≤ 120 KB |
  | Lazy WebGL chunks on `(map)` and `(cases)` (MapLibre, deck.gl, Sigma.js) | ≤ 450 KB, loaded only on those routes, after first paint |
  | LCP / INP at p75, "Slow 4G" profile (~1.6 Mbps, 150 ms RTT) on a mid-range Android | ≤ 2.5 s / ≤ 200 ms |

- Load heavy client libraries with `next/dynamic` and `ssr: false`. Serve images through `next/image` with the built-in local loader only.
- Respect `Save-Data` and `navigator.connection.saveData`: smaller pages, no background refresh, no prefetching of heavy routes.
- Static assets are immutable and fingerprinted, with long cache lifetimes. Data responses are never stored by a service worker (§13).

## 12. Maps and graphs

**Maps (P18):**
- MapLibre GL JS for the basemap, with deck.gl layers for object sets. Offline basemaps are **Protomaps PMTiles** bundles served from inside the boundary (`deploy/helm/geo/`, P18.2). No Mapbox, Google or other hosted tiles. CesiumJS (3D) runs only with self-hosted terrain, never Cesium ion.
- OpenStreetMap and Overture ODbL layers show their attribution in the map's attribution control and come from their **separate** databases (ADR-0003; GUARDRAILS §5).
- Send the map only what it draws: RIDs, geometry and display-safe properties. Above about 100,000 points, aggregate on the server with H3. The P18 target is ≥ 30 fps with 100,000 objects.
- Each layer shows a `MarkingBadge` with the highest marking of its contents. Popups follow §8.2, and exports or screenshots keep the classification banner.
- Coordinates cross the API as EPSG:4326. Draw-to-filter and geofence tools send geometries to the server; the server filters.

**Link graph (P19):**
- Sigma.js (WebGL) over graphology. Expansion, path-finding and neighbourhoods run on the server through the traversal service, which applies policy in-process (ARCHITECTURE §5). The browser never infers hidden links.
- A node or edge the user may not see is either absent or shown as a "restricted" placeholder with no label, exactly as the server returns it.
- Run layouts (ForceAtlas2) in a Web Worker. Cap the visible graph at about 5,000 nodes; beyond that, cluster or ask the user to filter.
- Offer an accessible adjacency-list panel that mirrors the selection (§9).

## 13. Security

- **Sessions (BFF pattern).** The OIDC authorisation-code flow with PKCE completes server-side (P10 `lib/auth`, with the Java BFF in core; ARCHITECTURE §3.1). Tokens stay in the BFF session store (Valkey; ARCHITECTURE §5). The browser holds only an opaque session cookie: `__Host-` prefix, `Secure`, `HttpOnly`, `SameSite=Strict`. The short-lived PKCE/state cookie uses `SameSite=Lax`, because it has to survive the identity provider's redirect.
- **No tokens or personal information in `localStorage`, `sessionStorage` or IndexedDB.** No service worker caches API responses. Recent searches and saved views live on the server, under policy.
- **CSRF.** `SameSite=Strict`, plus an `Origin`/`Sec-Fetch-Site` check on every mutating request, plus Connect's non-simple content types, which force a CORS preflight.
- **Strict CSP**, with a per-request nonce set in middleware:
  ```
  default-src 'self'; script-src 'self' 'nonce-{n}' 'strict-dynamic'; style-src 'self' 'nonce-{n}';
  img-src 'self' blob: data:; font-src 'self'; connect-src 'self'; worker-src 'self' blob:;
  object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'; upgrade-insecure-requests
  ```
  Also send `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` and a `Permissions-Policy` that disables camera, microphone and geolocation unless a feature needs them. Run Trusted Types in report-only mode until P10 confirms framework compatibility, then enforce them.
- **No `dangerouslySetInnerHTML` on untrusted content.** `react/no-danger` is a lint error, and any exception needs the security lead's review. Markdown, including AI output, is rendered with raw HTML disabled.
- **AI output is untrusted content** (GUARDRAILS §4). Render it as text or Markdown only, label it "AI-drafted: review before use", and link citations to internal source records. External URLs appear as plain text, and remote images are never fetched (they are a known exfiltration channel; `img-src 'self'` blocks them anyway).
- Untrusted documents are previewed as server-rendered images or in a sandboxed viewer, never as inline HTML or SVG from a source.
- No `eval`, `new Function` or string-based timers. Third-party scripts are not allowed.

## 14. Testing and local CI

| Layer | Tool | Gate |
|---|---|---|
| Unit and component | Vitest + Testing Library, `vitest-axe` | ≥ 85% line coverage for `apps/cockpit/src`; **100%** for `lib/auth`, `lib/api`, `MarkingBadge`, `ClassificationBanner`, `RedactedValue`, `JustificationPrompt` (policy-adjacent) |
| Journeys | Playwright + `@axe-core/playwright` | 0 serious or critical axe violations; each phase's Done-when journey |
| Types, lint, format | `tsc --noEmit`, ESLint, Prettier | Zero errors |
| Build | `next build` + bundle budgets (§11) | Budgets met |

- P33 owns `tests/e2e/`. Until then, each feature phase keeps the Playwright specs for its own Done-when journey inside its feature folder, and P33 consolidates them.
- Test data is synthetic, and every redaction path has a test.
- Local runs:
  ```bash
  docker compose -f docker-compose.ci.yml run --rm ci-frontend   # prettier, lint, tsc --noEmit, vitest, next build
  act pull_request -W .github/workflows/ci-frontend.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
  docker compose -f docker-compose.ci.yml down -v
  ```
  Supervise long runs under the monitoring protocol in BACKEND_GUIDE §10.3 (Playwright: 10-minute maximum).

## 15. Attribution headers (INSTRUCTIONS §8)

- `.ts`, `.tsx`, `.js`, `.mjs`: `// Written by <Name> (<AI>) on YYYY-MM-DD — <summary>`, then `// Modified by …` lines.
- `.css`: the same text inside `/* … */`.
- JSON (message catalogues, configs): an entry in the folder's `ATTRIBUTION.md`.
- Never delete or rewrite an earlier author's line. Generated clients carry the generator's banner instead.

## 16. Cockpit PR review checklist

- [ ] Every changed file sits inside the phase's route group or feature folder (§2), and no feature imports another feature.
- [ ] Data is fetched on the server, or through `lib/api` with the policy-context headers. Nothing uses a Next.js cache for tenant data (§3, §4).
- [ ] Redacted values, marking badges and the classification banner render correctly in every new view, export and tooltip (§8).
- [ ] New UI copy is in the message catalogues; the pseudo-locale renders without truncation (§10).
- [ ] axe reports 0 serious or critical violations, and the new flow works by keyboard alone (§9).
- [ ] Bundle budgets hold (§11), and nothing loads from outside the deployment.
- [ ] No token or personal information in browser storage, no `dangerouslySetInnerHTML`, and AI output rendered as untrusted (§13).
- [ ] Coverage gates are met, including 100% on policy-adjacent components (§14).
