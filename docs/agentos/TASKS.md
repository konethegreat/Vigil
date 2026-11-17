<!-- Written by Kone Tshivhinda (Hermes Agent) on 2026-11-17 - AgentOS working plan tasks for vigil -->

# AgentOS Working Plan: Tasks

Each task is sized for a single pull request targeting `develop`, following `chore|fix|feature/<slug>` branch naming, `type(repo|P<ID>): summary` PR titles, and `WORKLOG.md` append-only logging.

## Phase 1: CI Hygiene, Dependency Refresh, and P1 Residual Closures
- [ ] Reconcile open Next.js and npm minor/patch updates in `apps/cockpit` (PRs #100, #105, #106) and verify `ci-frontend` (`pnpm test`, `tsc`, `eslint`) passes.
- [ ] Reconcile open Python dependency updates (`mypy`, `pytest-asyncio`, `uv` group in `services/mind`, PRs #96, #97, #102) and verify `ruff`, `mypy --strict`, and `pytest` pass.
- [ ] Reconcile open Gradle, GitHub Actions, and `eclipse-temurin` base image pin updates (PRs #95, #103, #104) and verify `ci-backend` and `security-scan` pass.
- [ ] Add `CODEOWNERS` and document/consolidate CI toolchain version pins across `infra/docker/ci/Dockerfile`, `.github/workflows/`, and `scripts/ci/run-backend.sh` (`MSG-017`).
- [ ] Update `docs/PHASES_AND_TASKS.md` and `docs/AI_COMMUNICATION.md` to record resolved P1 residual items (`MSG-017`, `MSG-028`).

## Phase 2: Core Indexer Funnel (P6)
- [ ] Define `proto/vigil/indexer/v1` contracts (`IndexerService`, watermark events, conflict policies) and verify `buf lint`, `buf breaking`, and `sdks/_codegen/verify.sh`.
- [ ] Implement P6 changelog differ (6.1) and property-level conflict merge (`SOURCE_WINS`, `EDIT_WINS`, `LATEST_TIMESTAMP`) (6.2) in `services/indexer` with unit tests.
- [ ] Implement P6 bulk upsert into typed PostgreSQL tables and OpenSearch documents carrying Warden markings (6.3) with integration tests.
- [ ] Implement P6 alias swaps, >80% shadow-table full reindex with W8 label diff guard, and `vigil.indexer.objects.indexed.v1` watermark emission (6.4).
- [ ] Implement P6 backfill and replay from Iceberg + edit log (6.5), verify byte-identical replay, and close P6 in `docs/PHASES_AND_TASKS.md` and `docs/AI_COMMUNICATION.md`.

## Phase 3: Core Object Set Service and Traversal (P7)
- [ ] Define `proto/vigil/objects/v1` contracts and implement object set algebra (filter, set ops, <=3-hop search-around, aggregations, pagination) (7.1).
- [ ] Implement SQL (`SET LOCAL vigil.policy_ctx`) and OpenSearch DSL query compiler (7.2) with query-plan unit tests.
- [ ] Implement RLS/DLS security pushdown, column-marking property redaction, and read audit logging (7.3) with PG18 policy-conformance tests.
- [ ] Implement Rust Arrow link-snapshot k-hop traversal service in `services/traversal/` with in-process Warden policy filtering (7.4).
- [ ] Implement read-your-writes watermark tokens and edit overlay (7.5), verify p95 latency and zero-leak conformance, and close P7 in `docs/PHASES_AND_TASKS.md`.

## Phase 4: Core Actions, Edit Log, and Write-back (P8)
- [ ] Define `proto/vigil/actions/v1` contracts, `actions` Flyway migrations, and Action type runtime with validation and submission criteria (8.1).
- [ ] Implement idempotent pre-commit write-back webhook (`action_run` first, `Idempotency-Key`, <=10s timeout) (8.2) with timeout/retry tests.
- [ ] Implement atomic edits + `edit_log` + `outbox` transaction and Kafka `<tenant>.ontology.edits.v1` CloudEvent relay (8.3).
- [ ] Implement Temporal side-effect sagas (`deploy/helm/temporal/`) (8.4), POPIA s71 human approval gate (8.5), and tenant limits/metering (8.6).
- [ ] Run chaos tests and the PG18 DATA_MODEL security suite (`MSG-023`) for `actions` tables, and close P8 in `docs/PHASES_AND_TASKS.md`.

## Phase 5: Cockpit Shell Hardening and Functions Runtime (P9 and P10)
- [ ] Implement P10 Keycloak OIDC PKCE BFF session flow (`MSG-020`) and purpose/case `PolicyContext` selector in `apps/cockpit`.
- [ ] Add P10 WCAG 2.1 AA contrast/axe verification and standardized redaction placeholders across `apps/cockpit` UI primitives.
- [ ] Implement P9 WASM host (`services/functions-host/`, Wasmtime `vigil:udf` Component Model with fuel/epoch limits and no ambient I/O) (9.1).
- [ ] Implement P9 TS/Python sandbox isolation (9.2), function-backed Action SPI integration with P8 (9.3), and OTel metering/logs (9.4), then close P9 and P10 in `docs/PHASES_AND_TASKS.md`.
