<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — ADR-0007 typed object tables: two sanctioned writers inside Core (P0) -->

# ADR-0007 — Typed Object Tables Have Exactly Two Sanctioned Writers, Both Inside Core

**Status:** Proposed (P0), to be accepted at founder sign-off (P0 task 0.7) · **Date:** 2026-09-25

## Context
Two P0 documents pull in different directions:
- ARCHITECTURE principle A7 and ADR-0001 forbid sharing an operational database across a language boundary.
- ARCHITECTURE §6.1 and §6.3 have **two** components writing the tenant's typed object tables:
  - the **Indexer** (Rust, P6), which hydrates source-derived properties from datasets;
  - the **Action engine** (Java, P8), which applies governed edits in the same transaction as `edit_log` and `outbox`.

The DATA_MODEL design review (P0) flagged this conflict. Palantir's OSv2 has the same shape: every edit goes through Actions and reaches the indexes "immediately", while the Funnel merges datasource changes with user edits ([appendix 02 §1.4](../research/appendix/02-palantir-technical.md)).

## Options
1. **A single writer (Indexer only).**
   - The Action engine commits only `edit_log` + `outbox`.
   - The Indexer applies edits asynchronously.
   - For: strict A7 compliance.
   - Against: Actions become eventually consistent. Submission criteria and read-your-writes need an edit overlay everywhere, which adds complexity to every read path.
2. **Two sanctioned writers inside one bounded context (Core), coordinated by a write-ownership matrix (chosen).**
3. **Both components in one language.**
   - For: removes the boundary.
   - Against: loses Rust's indexing throughput, or Java's transactional and ecosystem strengths (ADR-0001).

## Decision
- A7 means **no shared database across services or bounded contexts**. The Core ontology plane is one bounded context implemented in two languages. Inside it, the **Indexer** and the **Action engine** are the only permitted writers of typed object tables.
- **Coordination** follows DATA_MODEL §2 (write-ownership matrix):
  - per-property ownership, via the `edited_props` mask and the declared conflict policy (`SOURCE_WINS | EDIT_WINS | LATEST_TIMESTAMP`);
  - optimistic row versions;
  - the Action engine writes under the caller's signed policy context and RLS. The Indexer writes under an explicit, unconditional system policy: it never serves user reads, uses a pod-scoped credential, and is audited per batch. Neither role has `BYPASSRLS`.
- **No other service** (Mind, Forge, Relay, Cockpit BFF) may write these tables. They go through Actions or datasets.
- **Enforcement** is a shared conformance test suite that runs against both writers in P6 and P8 (concurrent-writer races, conflict policies, RLS).

## Consequences
- **For:** Actions stay synchronous and atomic (Palantir parity), and reads stay simple.
- **Against:**
  - Schema knowledge exists in two languages. It is mitigated by generating DDL and accessor metadata from OMS (P5) for both.
  - If contention or complexity grows, we fall back to Option 1 under a superseding ADR.
