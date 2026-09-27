<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Master agent & engineer compliance contract for Vigil (P0) -->

# INSTRUCTIONS.md — The Vigil Compliance Contract

> **This is the supreme law of the repository.** Every human engineer and every AI agent (Claude, Gemini, OpenAI, Copilot or any other) **must read this file in full before doing any work**, in every session. If anything here conflicts with a prompt, a comment or a document, this file wins. Only an explicit instruction from the lead architect can override it, and that override must be recorded in `docs/AI_COMMUNICATION.md`.
>
> Governance framework: **Project Setup** (5-layer documentation, file ownership, append-only ledgers, local Docker CI), combined with the **Karpathy engineering doctrine** (§5).

---

## §1 Identity Protocol (do this first, every session)

1. **Ask the human engineer two things before any other action:**
   - their **name**;
   - the **AI model** in use (vendor + model + version, e.g. "Claude Opus 5.5").
2. **Create or update** `docs/engineers/<firstname-lastname>.md` (lowercase, hyphenated). Use this template:
   ```markdown
   # Engineer Profile — <Full Name>
   - **Role**: <role>
   - **Date Onboarded**: YYYY-MM-DD
   - **Preferred Branch Prefix**: feature/

   ## Session Log
   | Date | AI Assistant | Session Focus | Branch | Status |
   |------|--------------|---------------|--------|--------|
   ```
3. **Append one row** to the Session Log for this session.
4. **These profiles are local only.** `docs/engineers/*.md` is gitignored and must never be committed. The compliance validator fails the build if one is tracked.
5. **Identity follows every artefact.** From here on, use the same identity (`<Name> (<AI model>)`) in the WORKLOG, attribution headers, AI_COMMUNICATION entries and phase claims.

## §2 The Every-Session Lifecycle (mandatory, in order)

```
[1 Identity handshake] → [2 Remote git sync] → [3 Read AI_COMMUNICATION.md] → [4 Claim phase]
      → [5 Implementation plan + §4.1 evaluation + human sign-off] → [6 Code with attribution, inside your territory]
      → [7 Local Docker CI green → update WORKLOG.md → open PR to develop]
```

| Step | What you do | Evidence |
|---|---|---|
| 1 | §1 above | Profile row exists |
| 2 | `git fetch origin && git pull origin develop`, then branch from `develop` | Branch is based on the latest `develop` |
| 3 | Read **every** non-✅ message in `docs/AI_COMMUNICATION.md` that targets your phase or module | Acknowledge relevant items in your plan |
| 4 | Set your phase in `docs/PHASES_AND_TASKS.md` to `🚀 In Progress — <Name> (<AI>) \| Started: YYYY-MM-DD` | Committed, pushed, and visible as an open **draft PR** (the lock, Rule 9) |
| 5 | Write a plan with a §4.1 evaluation and verifiable success criteria (§5.4). **Wait for the human's sign-off.** | Plan recorded in the PR description |
| 6 | Implement. Touch only files your phase owns. Add or extend attribution headers (§8). | Diff contains only owned paths |
| 7 | Run `just ci` (or `docker compose -f docker-compose.ci.yml run --rm <job>`), update `WORKLOG.md`, push, open a PR to `develop` | Green local CI, WORKLOG row, PR open |

## §3 Branch & Pull Request Rules

- **Protected branches:** `main` and `develop`.
  - No direct commits.
  - No force-pushes.
  - No history rewrites.
  - All work lands through PRs.
- **The single exception** is the repository's root commit on `main` at initialisation (P0). See `docs/BRANCHING_STRATEGY.md` §2.
- **Branch names:**
  - `feature/P<ID>-<slug>`
  - `bugfix/P<ID>-<slug>`
  - `hotfix/<slug>`
  - `release/v<X.Y.Z>`
  - `chore/<slug>` (tooling and docs only)
  - `fix/<slug>`
- **PR targets:**
  - Feature, bugfix, fix and chore branches go to `develop`.
  - Release and hotfix branches go to `main`, then back-merge to `develop`.
- **Merge commits only.** Squash and rebase merges are banned, to keep atomic, forensically auditable branch histories.
- **One phase per PR** (Rule 5).
- **PR titles follow the convention.** Example: `feat(P8): outbox relay with idempotent side effects`. The type is one of `feat|fix|chore|docs|test|refactor|perf|build|ci|security`, and the scope is the phase ID or `repo`.
- **AI agents never merge.** An AI agent must not merge, approve its own PR, enable auto-merge, or ask an admin to bypass protections for it. Only humans merge. Repository admins may bypass protections themselves in emergencies (see BRANCHING_STRATEGY §6), and an AI agent is never authorised to act on an admin's behalf.

## §4 Decision Standards

### §4.1 Solution Evaluation Standard ("Think Outside The Box" Mandate)

For **any non-trivial technical decision**, you must do the following *before writing code*:
- library or engine selection;
- schema or ontology design;
- algorithmic approach;
- API or event contract;
- build vs reuse;
- adding a dependency or a language;
- any change touching security, privacy or write-back.

1. **Identify at least 2–3 realistic candidate approaches.** Always include "do less" or "don't build it" as a candidate (§5.2).
2. **Evaluate each one against Vigil's constraints:**
   - security and Warden semantics;
   - POPIA/GDPR, including s57 prior authorisation, s71 automated decisions and s72 cross-border transfers;
   - audit trails;
   - sovereignty (no required egress);
   - licence policy (ADR-0003: no BSL/SSPL/ELv2/NC in anything shipped);
   - maintainability and active maintenance of the dependency;
   - air-gap operability;
   - cost;
   - the language charter (§6).
3. **Check whether the decision is already frozen** in `docs/ARCHITECTURE.md`, `docs/VIGIL_DOMAIN_BLUEPRINT.md`, `docs/DATA_MODEL.md` or an accepted ADR. If it is, follow it. If you believe the frozen decision is wrong, stop and propose an ADR. Never quietly deviate.
4. **Document the rationale** in the plan or PR description *before* coding. Record decisions with lasting impact as a new ADR in `docs/adr/`.

### §4.2 ADRs

- **Location and name:** `docs/adr/ADR-<NNNN>-<slug>.md`.
- **Sections:** Context, Options, Decision, Consequences, Status.
- **Status runs `Proposed` → `Accepted`** (by the lead architect; at P0, by the founders).
- **Accepted ADRs are immutable** (the validator enforces this). To change a decision, write a new ADR that supersedes it.

## §5 The Karpathy Engineering Doctrine

> The vision has no limits. Each change has tight ones. These four rules exist to reduce the most common mistakes LLM-assisted engineering makes (after A. Karpathy; source: `forrestchang/andrej-karpathy-skills`). They lean toward caution over speed; for trivial tasks, use judgement.

### §5.1 Think Before Coding
**Don't assume. Don't hide confusion. Surface tradeoffs.**
- **State your assumptions explicitly.** If you're uncertain, ask.
- If there are several interpretations, **present them.** Don't pick one silently.
- If a simpler approach exists, **say so.** Push back when it's warranted, including against this plan.
- If something is unclear, **stop.** Name what's confusing, and ask.

### §5.2 Simplicity First
**The minimum code that solves the problem. Nothing speculative.**
- No features beyond what the phase asked for.
- No abstractions for code used once.
- No "flexibility" or "configurability" nobody requested.
- No error handling for impossible scenarios.
- If you wrote 200 lines and it could be 50, rewrite it.
- **At Vigil this also means:**
  - build the phase's **v1 slice** first;
  - don't split services until a load test proves you need to;
  - don't add a language, datastore or framework the charter doesn't list.
- Ask yourself: *"Would a senior engineer say this is overcomplicated?"* If yes, simplify.

### §5.3 Surgical Changes
**Touch only what you must. Clean up only your own mess.**
- Don't "improve" adjacent code, comments or formatting.
- Don't refactor things that aren't broken.
- Match the existing style, even if you'd write it differently.
- If you notice unrelated dead code, **mention it** (as a 🐛 BUG or ℹ️ INFO entry). Don't delete it.
- Remove imports, variables or functions that **your** change made unused. Leave pre-existing dead code alone unless asked.
- **The test:** every changed line should trace directly to the task and to your phase's territory.

### §5.4 Goal-Driven Execution
**Define success criteria. Loop until they are verified.**
- Turn each task into a goal you can verify:
  - "Add validation" → write tests for invalid inputs, then make them pass.
  - "Fix the bug" → write a test that reproduces it, then make it pass.
  - "Refactor X" → the tests pass before and after.
- For multi-step work, state the plan:
  ```
  1. [Step] → verify: [check]
  2. [Step] → verify: [check]
  3. [Step] → verify: [check]
  ```
- Every phase in `PHASES_AND_TASKS.md` has a **"Done when"** line. That line is the contract.

**These rules are working if** diffs contain fewer unnecessary changes, fewer rewrites are needed because of overcomplication, and clarifying questions come before implementation rather than after mistakes.

## §6 Polyglot Language Charter (from ADR-0001)

| Tier | Language | Owns | Adopted |
|---|---|---|---|
| Primary | **Java 25** (Spring Boot 4, Spring Modulith 2) | Core ontology services, Forge datasets & connectors, domain packs | P1 |
| Primary | **TypeScript 7** (Next.js/React) | Cockpit, visualisation, generated SDK, TS functions | P1 |
| Primary | **Python 3.13+** (FastAPI, uv) | Mind (AI), entity resolution, document AI, transforms, EO, simulation | P1 |
| Specialist | **Rust** | Warden policy core, indexer, traversal, functions host, edge agent | P3 |
| Specialist | **Go** | Relay (delivery), `vigil` CLI, K8s operators | P20 |
| Bounded | **Kotlin** | Field Android app only | P27 |
| Bounded | **Elixir** | Comms gateway only, **if** the P28 ADR selects it (otherwise Go) | P28 |
| First-class | **SQL** | Postgres schemas, RLS, SQL/PGQ | P1 |
| Consumed only | C/C++/CUDA | Through bindings; never authored | — |

**Rules:**
1. **Cross language boundaries only through contracts:** Protobuf/Buf, OpenAPI 3.1, CloudEvents/AsyncAPI, Arrow, WIT. Never share an operational database across a language boundary.
2. **At most two languages per phase**, unless the phase card lists more.
3. **A new language, datastore, framework or major dependency needs a §4.1 evaluation plus an accepted ADR.**
4. **Every language uses its golden-path template** (P1): OpenTelemetry, Warden binding, health checks, config, SBOM, attribution header.
5. **Lockfiles are committed:** `Cargo.lock`, `go.sum`, `pnpm-lock.yaml`, `uv.lock`, `gradle.lockfile`, `mix.lock`. They are required for reproducible, air-gapped and SBOM-verifiable builds.

## §7 Phases, Territory and Collaboration

- **Claim before you work.** Claim the phase in `docs/PHASES_AND_TASKS.md`, and work only inside its **File Ownership** paths. Respect its **Exclusion Zone**.
- **Obey the 10 Conflict Prevention Rules** in `docs/PHASES_AND_TASKS.md`.
- **Record cross-phase needs, bugs, warnings and handoffs** in `docs/AI_COMMUNICATION.md`:
  - categories: `🐛 BUG`, `🔌 INTERFACE`, `🔄 HANDOFF`, `⚠️ WARNING`, `ℹ️ INFO`;
  - entries are **never deleted**; only their Status changes.
- **Frozen blueprints** (🧊) change only through an ADR.

## §8 Cumulative Code Attribution

**Every code file, class, service, API route and config file you create or modify** carries attribution comments at the top:
- Append a line for your change; **never delete or rewrite earlier lines**.
- One line per author per change, oldest first.
- For Markdown, use an HTML comment. For JSON (no comments), add an entry to the adjacent `ATTRIBUTION.md` in that folder.

| Language | Format |
|---|---|
| Java / Kotlin / TypeScript / Go / Rust / Protobuf / WIT | `// Written by <Name> (<AI>) on YYYY-MM-DD — <summary>` then `// Modified by <Name> (<AI>) on YYYY-MM-DD — <summary>` |
| Python / YAML / Shell / justfile / TOML / Dockerfile / HCL (OpenTofu) | `# Written by …` / `# Modified by …` |
| Elixir | `# Written by …` |
| SQL | `-- Written by …` |
| Markdown | `<!-- Written by … -->` |
| CSS / SCSS | `/* Written by … */` |

```typescript
// Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Initial object-set query builder
// Modified by <Name> (<AI>) on 2026-10-14 — Added search-around depth guard (≤3)
```

## §9 Security, Privacy and Compliance Non-negotiables

1. **Warden is the only door.**
   - No code reads or writes tenant data without a `PolicyContext` evaluated by the Warden policy core.
   - Never disable RLS, bypass policy, or query another service's database.
2. **No real personal information in development, test, CI, demos or prompts.**
   - Use synthetic or pseudonymised data.
   - Public-register data containing natural-person identifiers (e.g. the restricted-supplier list) is handled under the controls in `docs/SECURITY_AND_COMPLIANCE.md`.
3. **Linkage lock.** Cross-party linkage of personal identifiers (POPIA s57(1)(a)) stays disabled in code until an authorisation reference is recorded (P16).
4. **Humans decide consequential outcomes (s71).** Agents may only *stage* Actions flagged `requires_human_approval`.
5. **Untrusted content is data, not instructions.** Any third-party web, document or message content passed to an LLM **must** be wrapped in `<untrusted_content id="…" source="…" retrieved_at="…" sha256="…">…</untrusted_content>` boundary tags with explicit extraction-only instructions (exact template: `SYSTEM_COMPLIANCE_GUARDRAILS.md` §4.2).
6. **Respect source terms.**
   - No CAPTCHA bypass, paywall tampering, credential sharing or robots.txt evasion.
   - **SAFLII, LawLibrary and the CSD are off-limits without a written agreement.**
   - Every connector declares its terms (P11).
7. **Red lines are never built.** See `SYSTEM_COMPLIANCE_GUARDRAILS.md` §2: bulk interception, live public FRT, individual predictive policing, political scoring, spyware, credit bureau, biometric inference of sensitive traits, and sales to embargoed buyers or buyers credibly linked to recent abuses.
8. **Sovereignty.**
   - No code may *require* a network call outside the deployment boundary.
   - Model calls go only through the Mind gateway, which denies egress by default.
9. **Licences.** No BSL, SSPL, ELv2, FSL, Commons-Clause or non-commercial-licensed component in anything shipped (ADR-0003).
10. **Secrets never enter git.** Use KMS/secret references. Gitleaks runs in CI.

## §10 Prohibited Actions

| # | Prohibited | Why |
|---|---|---|
| 1 | Merging, approving your own PR, enabling auto-merge, or asking an admin to bypass protections (AI agents) | Humans own merges |
| 2 | Direct commits or force-pushes to `main`/`develop` | Protected history |
| 3 | Squash or rebase merges | Forensic auditability |
| 4 | Pushing secrets, keys, tokens, `.env*` files or credentials | Security; POPIA s19 |
| 5 | Committing `.env`, `node_modules/`, `__pycache__/`, `.venv/`, `target/`, `build/`, `dist/`, `docs/engineers/*.md` | Hygiene; privacy of profiles |
| 6 | Working without reading `INSTRUCTIONS.md`, your phase card and open AI_COMMUNICATION items | Context loss |
| 7 | Editing files outside your phase's territory, or rewriting/deleting append-only ledger rows | Collisions; audit integrity |
| 8 | Deleting or altering another author's attribution lines | Traceability |
| 9 | Using real personal data outside production; pasting production data into prompts | POPIA |
| 10 | Adding a language, datastore, framework or licence-amber/red dependency without an ADR | Polyglot tax; licence risk |
| 11 | Scraping sources whose terms forbid it, or bypassing CAPTCHAs, paywalls or rate limits | Law and ethics |
| 12 | Building any red-line capability, or weakening a gated-module control | Legitimacy |
| 13 | Letting an AI agent execute an Action flagged `requires_human_approval` | POPIA s71 |
| 14 | Disabling tests, linters or security scans to get a green build | Integrity |
| 15 | Marking a phase ✅ without meeting its "Done when" | Honesty |

## §11 Pre-Commit Compliance Checklist (12 points, verify before every push)

- [ ] 1. **Identity:** my profile exists and has today's session row. I am on a correctly named branch (not `main`/`develop`).
- [ ] 2. **Sync:** the branch is based on the latest `develop`, and I've read the open AI_COMMUNICATION items for my phase.
- [ ] 3. **Claim:** my phase shows `🚀 In Progress` with my name, AI model and start date.
- [ ] 4. **Territory:** every changed file is inside my phase's File Ownership, and nothing is in its Exclusion Zone.
- [ ] 5. **§4.1:** non-trivial decisions are evaluated and documented (PR description or ADR).
- [ ] 6. **Karpathy:** the diff is minimal. Every changed line traces to the task, and there are no drive-by refactors.
- [ ] 7. **Attribution:** every created or modified file has my attribution line, with earlier lines preserved.
- [ ] 8. **Tests:** new behaviour has tests (the reproduction test first for bugs), and "Done when" criteria are verified or explicitly pending.
- [ ] 9. **Local CI green:** `just ci` / `docker compose -f docker-compose.ci.yml run --rm <job>` passes, with exit code checked (container and stale-task monitoring rules in `docs/BACKEND_GUIDE.md` §10.3).
- [ ] 10. **Security & privacy:** no secrets and no real personal data. Warden enforcement is intact, the untrusted-content boundary is used, and licences are green.
- [ ] 11. **Contracts:** `buf breaking` passes, and API or event changes are appended to `docs/API_DESIGN.md` / AsyncAPI.
- [ ] 12. **Ledgers:** there is a `WORKLOG.md` row for this change, and AI_COMMUNICATION entries are added or updated as needed.

Run `python3 scripts/validate-compliance.py` (add `--ci` to mirror the PR check). It checks item 1 (branch name, and the PR's base branch in `--ci`), item 3 (phase claim, as a warning), item 7 (attribution headers, including preserved earlier lines, in `--ci`), part of item 10 (tracked secrets and forbidden files) and item 12 (WORKLOG and append-only ledgers, in `--ci`). It also rejects edits to accepted ADRs. It runs in `pr-checks.yml`.

## §12 Ledgers

- **`WORKLOG.md` (append-only):** add one row **before every commit**: `| Date | Engineer | AI Assistant | Branch | Description |`.
- **`docs/AI_COMMUNICATION.md` (append-only):** the asynchronous ledger between engineers and AI agents across sessions and vendors. Rows are never deleted. When something is fixed, its Status becomes `✅ RESOLVED`.

## §13 Definition of Done (per PR)

A PR is done when **all** of these hold:
- the phase's "Done when" criteria that the PR claims are verified by tests;
- local Docker CI and GitHub checks are green;
- the WORKLOG row is in place;
- attribution headers are present;
- docs and contracts are updated (append-only where required);
- a human reviewer has approved it;
- a human has merged it with a merge commit.

## §14 Document Map

| Layer | Documents |
|---|---|
| Contract | `INSTRUCTIONS.md` (this file) |
| 1 · Roadmap & ownership | `docs/PHASES_AND_TASKS.md`, `docs/BRANCHING_STRATEGY.md` |
| 2 · Collaboration | `docs/AI_COMMUNICATION.md`, `WORKLOG.md`, `docs/engineers/` (local) |
| 3 · Technical blueprints | `docs/ARCHITECTURE.md` 🧊, `docs/VIGIL_DOMAIN_BLUEPRINT.md` 🧊, `docs/DATA_MODEL.md` 🧊, `docs/API_DESIGN.md`, `docs/FRONTEND_GUIDE.md`, `docs/BACKEND_GUIDE.md`, `docs/adr/` |
| 4 · Safety & audit | `docs/SECURITY_AND_COMPLIANCE.md`, `docs/SYSTEM_COMPLIANCE_GUARDRAILS.md`, `docs/LAUNCH_CHECKLIST.md`, `docs/INCIDENT_RESPONSE.md`, `docs/reviews/` |
| Strategy | `docs/research/VIGIL_RESEARCH_DOSSIER.md` + `docs/research/appendix/` |

> **The Golden Rule:** Never write code without reading the documentation first. Never make an architectural choice without evaluating alternatives. Never delete another engineer's attribution. Always log your work, test in Docker/CI, and land changes through Pull Requests.
