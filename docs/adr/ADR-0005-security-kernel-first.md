<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — ADR-0005 security kernel first (P0) -->

# ADR-0005 — Security Kernel First (Vigil Warden)

**Status:** Proposed (P0), to be accepted at founder sign-off (P0 task 0.7) · **Date:** 2026-09-25

## Context
Palantir's security model is mandatory-first:
- markings are conjunctive and propagate along data dependencies;
- classification has disjunctive releasability;
- restricted views give row-level control;
- Checkpoints force written justifications;
- agents are bound by the same policies as humans.

This model cannot be retrofitted, because every dataset, object, link, column and derived artefact must carry markings from creation. Vigil's legal posture adds its own needs:
- POPIA s57 linkage gates and s71 human review;
- the hessenDATA purpose and threshold safeguards (Germany);
- gated modules.

All of these need one consistent enforcement semantics across Java, Python, TypeScript, Rust and Go. Research: [appendix 02 §5](../research/appendix/02-palantir-technical.md), [appendix 04 §10](../research/appendix/04-legal-regulatory-ethics.md), [appendix 05 §4k](../research/appendix/05-polyglot-architecture.md).

## Options
1. **Authorisation code in each service.**
   - Against: semantics diverge across five runtimes, and propagation through lineage is inconsistent.
2. **An external policy decision point only (OPA or Cedar).**
   - Against: marking propagation isn't native, every read pays network latency, and decisions can't be compiled into RLS/DLS consistently.
3. **A Rust policy core with bindings and pushdown compilers (chosen).**

## Decision
- Build **Warden (P3) before any data-bearing service.**
- Implement the policy semantics once, in a **Rust policy core**: marking evaluation, releasability, purpose/case checks, propagation and redaction obligations.
- Expose the core as:
  - native (Java FFM, Python PyO3);
  - a WASM core module (browser/TS; Go via wazero from P20). Use the Component Model only where the host supports it; P3 verifies wazero support.
- Every binding must pass a single shared conformance suite.
- Keycloak handles identity, and OpenFGA handles discretionary relationships (orgs, spaces, projects, roles).
- Enforcement is pushed down into Postgres RLS, OpenSearch DLS/FLS, scoped storage credentials and ClickHouse row policies, with policies compiled from the same core.
- Audit is hash-chained from day one.

## Consequences
- **For:** a single source of truth for the platform's biggest moat and biggest liability.
- **Against:** P3 is on the critical path and needs a senior Rust hire early. Differential fuzzing of the bindings is mandatory (P34).
