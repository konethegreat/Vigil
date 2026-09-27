<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — ADR-0001 language charter (P0) -->

# ADR-0001 — Polyglot Language Charter & Staged Adoption

**Status:** Proposed (P0), to be accepted at founder sign-off (P0 task 0.7) · **Date:** 2026-09-25 · **Deciders:** Lead architect, founders

## Context
The founders want Vigil to be powered by "the most robust languages, each doing what it's best at". Palantir itself is polyglot: Java services, Go infrastructure, TypeScript UIs and OSDK, Python transforms, and Rust on performance paths. The evidence also shows that uncontrolled polyglotism costs a lot. Uber consolidated onto Java and Go, and Google supports five languages, each with its own readability process. Vigil is starting with a small team whose strengths are Java, TypeScript, Python and SQL. Research: [dossier §7](../research/VIGIL_RESEARCH_DOSSIER.md#7-the-polyglot-engineering-strategy), [appendix 05 §1](../research/appendix/05-polyglot-architecture.md).

## Options
1. **Mono-language (Java or TypeScript everywhere).**
   - For: simple.
   - Against: weak at edge (footprint), policy-core memory safety, ML ecosystem and Kubernetes tooling.
2. **Free choice per team.**
   - For: maximum fit.
   - Against: an unbounded polyglot tax, fragmented tooling, and security semantics that differ per runtime.
3. **A tiered charter with hard boundaries and staged adoption (chosen).**

## Decision

| Tier | Language | Scope | Adopted at |
|---|---|---|---|
| Primary | Java 25 + Spring Boot 4 / Spring Modulith 2 | Core ontology services, Forge datasets/connectors, packs | P1 |
| Primary | TypeScript 7 + Next.js/React | Cockpit, visualisation, SDK, TS functions | P1 |
| Primary | Python 3.13+ + FastAPI + uv | Mind, ER, doc AI, transforms, EO, simulation | P1 |
| Specialist | Rust | Warden policy core, indexer, traversal, functions host, edge agent | P3 |
| Specialist | Go | Relay, CLI, operators | P20 |
| Bounded | Kotlin | Android field app only | P27 |
| Bounded | Elixir | Comms gateway only, decided by a dedicated ADR at P28 (Go is the fallback) | P28 |
| First-class | SQL | Schemas, RLS, SQL/PGQ | P1 |
| Consumed only | C/C++/CUDA | Via bindings | — |
| Rejected | Scala, C#/.NET, Julia, Zig, Gleam; Mojo (watch); Dart/Flutter (only if iOS parity is forced) | — | — |

**Boundary rules:**
- Services talk only through contracts (Protobuf/Buf, OpenAPI, CloudEvents, Arrow, WIT). No operational database is shared across a language boundary.
- A phase uses at most two languages, unless its phase card lists more (INSTRUCTIONS §6 rule 2).
- Authorisation semantics are implemented **once**, in the Rust policy core, and exposed to every runtime (ADR-0005).
- Each language has a golden-path template.

## Consequences
- **For:** each concern uses the ecosystem that leads it (JVM data ecosystem, Rust safety and footprint, Python ML, Go cloud-native, Kotlin Android at 81% share in Africa, BEAM for flaky networks).
- **Against:** hiring must cover Rust and Go early, and Elixir only if P28 justifies it. CI must build five to seven toolchains (P1). Revisit at 40 engineers or when CI exceeds 20 minutes (possible move to Bazel 9).
