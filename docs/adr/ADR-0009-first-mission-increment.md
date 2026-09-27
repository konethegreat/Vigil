<!-- Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Record authorised first mission increment -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification -->

# ADR-0009: First complete mission increment

**Status:** Implementation decision under the founder's 2026-09-25 instruction to start the expanded concept and do as much work as possible, alone. Not a blanket acceptance of earlier Proposed ADRs.

## Context

The founder reviewed VIGIL_FUTURE_CONCEPT.md and explicitly instructed implementation. No Git remote is configured. The current host has Docker, Node and Python; Java 25 runs in a container. Existing uncommitted concept work is retained. Work is on `codex/mission-foundation`.

This increment spans P1/P3/P8/P10/P14/P15/P19 concepts as one complete local mission, superseding the original sequential phase ordering for this bounded increment. Three authored languages are needed: Java for durable core, Python for Mind, TypeScript for Cockpit, plus SQL. No repeat approval is required to perform the work the founder just authorised.

## Options

1. Static interface only: easiest to build, but cannot prove persistence, permission boundaries or action semantics.
2. Compact Java/PostgreSQL core, Python Mind and Next.js Cockpit: demonstrates the evidence-to-outcome loop with independent model execution. Selected.
3. Full initial distributed platform: defers operational learning behind many infrastructure services. Deferred.

## Decision

- Build Protect and Lifelines/Response mission workflows using synthetic fixtures, original evidence, evidence revisions, generated plans, explicit handoff states, outcomes and an append-only audit chain.
- Use Java 25 / Spring Boot 4, PostgreSQL, Python / FastAPI, Next.js / React / TypeScript. Use contracts captured in `contracts/mission.schema.json` and HTTP JSON with schema validation for this increment; full generated Protobuf clients remain later work.
- Default to local-only Docker networking and loopback published ports. Demo identities are explicitly synthetic, server-issued signed sessions, not production OIDC. App starts only in explicit demo mode. No real personal data or external dispatch.
- Implement bounded server-side policy checks and PostgreSQL RLS now. The cross-language Rust/WASM Warden engine is deferred; no claim of the complete P3 policy semantics. The Java policy boundary is mandatory in this increment and cannot be bypassed by client tenant headers.
- Store durable core state in PostgreSQL. Python never connects to its database. Mind receives an authorised snapshot and returns proposed structured work only.
- Use deterministic baseline planning when models are unavailable and label it honestly. Implement and, if a suitable local model is available, exercise local model routing through Ollama. No paid cloud calls or automatic model downloads.
- Use expected versions, scoped idempotency keys, action state checks, evidence-based plan invalidation, and transactional audit writes. External action states are manually recorded sandbox acknowledgments, not claims of real service delivery.
- The entire app is a local prototype with explicit boundaries, not a production emergency service.

## Consequences

The synthetic demo identity, bounded Java policy evaluator, JSON contracts and compact storage profile are explicit deviations from the eventual blueprints. They must not silently migrate into a production configuration. P0–P43 remain incomplete; this increment gets its own acceptance record.

Dependencies are pinned and lockfiles retained. Verify the first loop using integration tests against PostgreSQL, Python contract/model tests, TypeScript checks, production build and browser interaction. Verify tenant isolation, stale plans, idempotency conflicts, denied transitions, completion requirements and restart persistence. Record unavailable checks rather than claiming a full platform CI pass.
