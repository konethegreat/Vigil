<!-- Written by Kone Tshivhinda (Hermes Agent) on 2026-11-17 - AgentOS working plan phases for vigil -->

# AgentOS Working Plan: Phases

This plan sequences the next incremental deliverables for `vigil` on `develop`, grounded in `docs/PHASES_AND_TASKS.md`, `docs/AI_COMMUNICATION.md`, and the current repository state.

## Phase 1: CI Hygiene, Dependency Refresh, and P1 Residual Closures
Goal: Clear the open Dependabot backlog (#95, #96, #97, #100, #102, #103, #104, #105, #106) and close remaining P1 tooling follow-ups (`MSG-017`, `MSG-028`) without breaking `pr-checks` or security scans.
Done when: Open dependency updates across `apps/cockpit`, `services/mind`, `services/core`, Gradle, and `.github/workflows` are reconciled on `develop`, `CODEOWNERS` and single-source version pins are in place, and `python3 scripts/validate-compliance.py` plus CI workflows pass.

## Phase 2: Core Indexer Funnel (P6)
Goal: Implement and merge P6 (`Core - Indexer (Funnel)`) in `proto/vigil/indexer/` and `services/indexer/`, bridging P4 Forge Datasets and P5 Ontology Metadata Service into typed PostgreSQL tables and OpenSearch documents under ADR-0007 and Warden markings.
Done when: `proto/vigil/indexer/v1` contracts, changelog diffing, property-level conflict merge (`SOURCE_WINS`, `EDIT_WINS`, `LATEST_TIMESTAMP`), bulk Postgres/OpenSearch indexing, alias/shadow-table hydration with `vigil.indexer.objects.indexed.v1` watermarks, and replay verification pass CI and P6 is marked complete in `docs/PHASES_AND_TASKS.md`.

## Phase 3: Core Object Set Service and Traversal (P7)
Goal: Implement P7 (`Core - Object Set Service & Traversal`) in `proto/vigil/objects/`, `services/core/src/main/java/vigil/core/objects/`, and `services/traversal/` for policy-enforced object querying, aggregation, and k-hop graph traversal.
Done when: Object set algebra, SQL/OpenSearch query compilation with `SET LOCAL vigil.policy_ctx`, property-level redaction with read audit, Rust Arrow k-hop traversal, and read-your-writes watermark tokens pass policy-conformance tests on PostgreSQL 18 and P7 is marked complete in `docs/PHASES_AND_TASKS.md`.

## Phase 4: Core Actions, Edit Log, and Write-back (P8)
Goal: Implement P8 (`Core - Actions, Edit Log & Write-back`) in `proto/vigil/actions/`, `services/core/src/main/java/vigil/core/actions/`, `services/core/src/main/resources/db/migration/actions/`, and `deploy/helm/temporal/` for atomic ontology mutations, pre-commit webhooks, outbox relay, and POPIA s71 approvals.
Done when: Action runtime, idempotent pre-commit write-back webhook, transactional `edit_log` + `outbox` Kafka relay, Temporal side-effect sagas, POPIA s71 approval enforcement, and the PG18 DATA_MODEL security suite (`MSG-023`) pass chaos tests with zero partial commits and P8 is marked complete in `docs/PHASES_AND_TASKS.md`.

## Phase 5: Cockpit Shell Hardening and Functions Runtime (P9 and P10)
Goal: Harden `apps/cockpit` to meet P10 (`Cockpit Shell & Design System`) standards (`MSG-020` BFF auth, WCAG 2.1 AA, redaction placeholders, Connect/TS policy-context client) and build the P9 (`Core - Functions Runtime`) WASM and sandboxed execution host.
Done when: `apps/cockpit` passes axe accessibility checks with 0 serious violations and renders redacted properties, P9 WASM (`vigil:udf`) and sandboxed function-backed actions pass containment and E2E tests, and `docs/PHASES_AND_TASKS.md` and `docs/AI_COMMUNICATION.md` reflect P9 and P10 completion.
