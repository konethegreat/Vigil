<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — ADR-0008 deliberate adaptations of the Project Setup template for Vigil (P0) -->

# ADR-0008 — Deliberate Adaptations of the Project Setup Template

**Status:** Proposed (P0), to be accepted at founder sign-off (P0 task 0.7) · **Date:** 2026-09-25

## Context
The Project Setup skill is "the backbone of every project" and must be followed to the letter. Its examples come from a Python/FastAPI + Next.js due-diligence product ("Sentinel"). Vigil is a polyglot, sovereign platform, so a few template details can't apply literally. Karpathy's first rule ("surface tradeoffs, don't pick silently") requires every deviation to be visible and signed off rather than buried. This ADR lists them all.

## Options
1. **Apply the template literally,** including its example-specific details.
   - Against: several would break Vigil (for example, forbidding lockfiles defeats reproducible air-gapped builds).
2. **Adapt silently where needed.**
   - Against: violates "followed to the letter" and hides decisions from the founders.
3. **Keep every mandated artefact and rule, and record each necessary adaptation here for explicit sign-off (chosen).**

## Decision: the adaptations

| # | Template says | Vigil does | Why |
|---|---|---|---|
| 1 | The validator's `FORBIDDEN_TRACKED_PATTERNS` includes `uv.lock` and `*.lock` | **Lockfiles are committed and required** (INSTRUCTIONS §6 rule 5) | Reproducible, air-gapped, SBOM-verifiable builds. The template list was never enforced by its own script (MSG-005) |
| 2 | The validator reads `git rev-parse --abbrev-ref HEAD` | It reads `GITHUB_HEAD_REF` first | PR checkouts are detached in GitHub Actions, so the template check would fail every PR (MSG-004) |
| 3 | No direct commits to `main` / `develop` | The same, **plus one documented exception**: the repository's root commit (BRANCHING_STRATEGY §2) | A new repository needs a root commit before a PR can exist |
| 4 | Domain blueprint named `AI_DUE_DILIGENCE_ARCHITECTURE_PLAN.md` | `docs/VIGIL_DOMAIN_BLUEPRINT.md`, with the same role and contents (entity resolution, risk scoring, AI routing, ingestion) | The template name is the example product's; the Layer 4 slot is filled in full |
| 5 | Backend guide: FastAPI / SQLAlchemy / `Depends` / Pydantic | Kept in full for Python services, **plus** Java/Spring Modulith, Rust, Go, Kotlin and Elixir conventions | Polyglot charter (ADR-0001) |
| 6 | `ci-backend.yml` runs ruff and pytest | Per-language, path-filtered jobs (Python, Java, Rust, Go, Android, Elixir, Protobuf), each a no-op until its project exists | Polyglot; the P0 PR stays green without code |
| 7 | `docker-compose.ci.yml` uses `pgvector/pgvector:pg16` | An owned PostgreSQL 18 image with PostGIS and pgvector (`infra/docker/postgres/`), plus Valkey | Vigil needs PostGIS, PG 18 features and owned images (no Bitnami) |
| 8 | `deploy.yml` runs Terraform on push to `main` | **OpenTofu** (MPL-2.0), only for a non-sovereign `dev` environment, and only **after e2e-live succeeds**. Sovereign sites pull through Relay | HashiCorp's BSL (ADR-0003); sovereign-first delivery (ADR-0004) |
| 9 | Gitleaks through its GitHub Action | The MIT-licensed Gitleaks CLI, checksum-verified | The action needs a licence key for organisations and sends telemetry (MSG-018) |
| 10 | Phases `P0`–`P42+` | `P0`–`P43` across the template's five tiers | Within "P42+" |
| 11 | Frontend: npm (`npx …`) | pnpm (`pnpm exec …`), which never downloads during checks | Deterministic, air-gap-safe installs |
| 12 | The engineer-profile example names a specific person | The identity protocol is kept verbatim, and `docs/engineers/*.md` stays gitignored | — |
| 13 | §4.3 timeouts: unit tests and linters ≤ 3 min | Step-level 3-minute limits. **Compile-inclusive steps** (cargo clippy/test, Gradle, Android) get 15 minutes, and the local composite `just ci-backend` gets 10 minutes | Recorded exception (MSG-027): compilation is image-build-class work |

Everything else in the template (the five documentation layers, identity protocol, §4.1 standard, cumulative attribution, ledgers, 10 conflict rules, 7 workflows, local Docker CI with `act`, the validator, the 12-point checklist) is adopted as written.

## Consequences
The founders sign off one list instead of discovering deviations later. Any further adaptation needs a superseding ADR.
