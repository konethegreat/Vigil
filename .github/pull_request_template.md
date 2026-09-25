<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Pull request template: phase scope, §4.1 evaluation, Karpathy plan, security notes, 12-point checklist, attribution (P0) -->

<!--
Title (checked by pr-checks):  type(P<ID>|repo): summary
  type: feat | fix | chore | docs | test | refactor | perf | build | ci | security
  e.g.  feat(P8): outbox relay with idempotent side effects
One phase per PR (Rule 5). Feature, bugfix, fix and chore branches target `develop`; release and hotfix
branches target `main`. Merge commits only (BRANCHING_STRATEGY §4).
-->

## Phase & scope
- **Phase:** P<ID>: <title>, claimed as `🚀 In Progress` in `docs/PHASES_AND_TASKS.md`
- **Branch:** `feature/P<ID>-<slug>` → `develop`
- **Tasks covered:** <e.g. 8.1, 8.3>
- **Territory:** every changed path is inside this phase's File Ownership; nothing is in its Exclusion Zone.
- **Open AI_COMMUNICATION items acknowledged:** <MSG-NNN, … or "none">

## Summary
<!-- What changed and why, in 2–5 sentences. -->

## §4.1 Solution Evaluation
<!-- Required for any non-trivial decision (INSTRUCTIONS §4.1). Always include a "do less" option. -->

| Candidate | Evaluation against Vigil's constraints (security & Warden, POPIA, audit, sovereignty, ADR-0003 licences, maintenance, air-gap, cost, language charter) |
|---|---|
| A: … | … |
| B: … | … |
| C: do less / don't build it | … |

- **Decision:** <chosen candidate and why>
- **Frozen-doc check:** is this already decided in `ARCHITECTURE.md`, `VIGIL_DOMAIN_BLUEPRINT.md`, `DATA_MODEL.md` or an accepted ADR?
  - [ ] Yes, and this PR follows it as written.
  - [ ] No. The rationale is above; a decision with lasting impact gets a new ADR in `docs/adr/`.

## Plan (Karpathy: step → verify)
1. <step> → verify: <check>
2. <step> → verify: <check>
3. <step> → verify: <check>

**"Done when" criteria covered** (quote the phase card):
- [ ] <criterion>: verified by <test or evidence>
- [ ] <criterion>: pending, because <reason>

## Security, privacy & sovereignty notes
- **Warden / PolicyContext:** <how reads and writes stay behind the policy core; RLS untouched>
- **Personal information:** <synthetic or pseudonymised data only, in dev, test, CI, demos and prompts>
- **Untrusted content:** <wrapped in `<untrusted_content>`, or n/a>
- **Sovereignty / egress:** <no required call outside the deployment boundary>
- **Licences (ADR-0003):** <new dependencies and their licences; amber needs `.github/licence-allowlist.yml` plus an ADR note>
- **Secrets:** <none committed; KMS or secret references only>

## Contracts changed
- [ ] None
- [ ] Protobuf (`buf breaking` passes): <packages>
- [ ] REST: appended to `docs/API_DESIGN.md` (append-only)
- [ ] Events: CloudEvents / AsyncAPI updated
- [ ] 🔌 INTERFACE entry in `docs/AI_COMMUNICATION.md`: MSG-<NNN>

## Screenshots
<!-- UI changes: before/after, light and dark, plus the axe result. Otherwise write "n/a". -->

## Pre-commit compliance checklist (INSTRUCTIONS §11)
- [ ] 1. **Identity:** my profile exists and has today's session row. I am on a correctly named branch (not `main`/`develop`).
- [ ] 2. **Sync:** the branch is based on the latest `develop`, and I've read the open AI_COMMUNICATION items for my phase.
- [ ] 3. **Claim:** my phase shows `🚀 In Progress` with my name, AI model and start date.
- [ ] 4. **Territory:** every changed file is inside my phase's File Ownership, and nothing is in its Exclusion Zone.
- [ ] 5. **§4.1:** non-trivial decisions are evaluated and documented (PR description or ADR).
- [ ] 6. **Karpathy:** the diff is minimal. Every changed line traces to the task, and there are no drive-by refactors.
- [ ] 7. **Attribution:** every created or modified file has my attribution line, with earlier lines preserved.
- [ ] 8. **Tests:** new behaviour has tests (the reproduction test first for bugs), and "Done when" criteria are verified or explicitly pending.
- [ ] 9. **Local CI green:** `just ci` / `docker compose -f docker-compose.ci.yml run --rm <job>` passes, with exit code checked (§4.3 monitoring rules in `docs/BACKEND_GUIDE.md`).
- [ ] 10. **Security & privacy:** no secrets and no real personal data. Warden enforcement is intact, the untrusted-content boundary is used, and licences are green.
- [ ] 11. **Contracts:** `buf breaking` passes, and API or event changes are appended to `docs/API_DESIGN.md` / AsyncAPI.
- [ ] 12. **Ledgers:** there is a `WORKLOG.md` row for this change, and AI_COMMUNICATION entries are added or updated as needed.

## Attribution
- **Engineer & AI model:** <Name> (<AI model>)
- [ ] Every created or modified file carries `Written by` / `Modified by <Name> (<AI model>) on YYYY-MM-DD — <summary>`, and earlier attribution lines are preserved (INSTRUCTIONS §8).
- [ ] AI-assisted commits carry a `Co-Authored-By:` trailer (BRANCHING_STRATEGY §4).

> ⛔ **AI agents must not merge.** An AI agent may not merge this PR, approve its own PR, enable auto-merge, or ask an admin to bypass branch protection (INSTRUCTIONS §3, §10 #1). Only a human merges, with a merge commit.
