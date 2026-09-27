<!-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Authorised investigation foundation -->
# ADR-0010: Evidence-backed investigations across missions

## Status

Implementation decision under the founder's 26 September instruction to move Vigil forward as a broad intelligence and operations platform. Continues the local increment authorised in ADR-0009; does not claim acceptance or completion of the full frozen ontology architecture.

## Context

The initial mission loop works, but evidence and operations are isolated per mission. The founder explicitly clarified that Vigil is intended to serve crime and wider operational intelligence at platform scale, with GBV as one domain. Preserve this product direction in architecture and interface. Work remains single-agent, synthetic, on `codex/mission-foundation`; no remote exists.

Relevant open ledger items remain in force: MSG-020 server-side sessions, MSG-023 RLS verification, MSG-025 sanctioned writes, MSG-029 future tenancy ownership, and MSG-032 scoped implementation limitations. Existing security and source constraints remain intact.

## Options

1. Add more domain-specific mission forms. Lowest effort, but leaves information isolated and does not establish an investigation capability.
2. Add a bounded object and relationship registry to the existing Java core, with source provenance, transactionally audited commands, forced PostgreSQL tenant policies, an object explorer, graph and timeline. Selected; no new language, runtime or paid dependency.
3. Implement the complete ontology metadata service, typed object materializations, search engine, traversal service and federation now. This remains the destination described by the blueprints, but involves separate migration, scale and security acceptance criteria beyond this increment.

## Decision

- Add immutable, typed investigation records (Person, Organisation, Place, Asset, Event, Document), with mandatory mission-source provenance and optional explicit occurrence time for Event records only.
- Model relationships as analyst assertions with their own source, evidence revision, assessment, reason and revision history. A graph connection is not a finding of guilt or proof of causality. Reassess a relationship explicitly; do not overwrite its history.
- Reuse visible objects across missions in the same workspace, without automatic person matching or cross-tenant linkage. Both endpoints and cited source must be visible under the same policy context. Reviewer sessions remain read-only.
- Tie writes to an active mission's expected version. Reuse existing scoped idempotency, transaction and audit mechanisms. A source revision marks dependent object/relationship provenance as needing review; no automatic claim promotion.
- Render a bounded interactive graph, searchable object registry, chronological event view and source inspector, linked back to operational missions. Scope and counts come from authorised data. Graph geometry has no geographic or confidence meaning.
- Seed connected infrastructure-crime and procurement-verification scenarios using fictional organisations and source material. Add investigation-specific task labels for new missions.
- Apply an idempotent forward SQL migration without resetting the database. Test tenancy, forged references, invalid type pairs, stale versions, retries, source revisions, append-only history and closure immutability.

## Consequences

This is a concrete investigation foundation, not Palantir feature parity. The compact registry has fixed columns and a bounded ontology; it is not the future per-type materialization system from DATA_MODEL. Java remains its only application writer. Existing P3/Warden, OIDC and field-level marking limitations from ADR-0009 remain explicit. There is no real personal data ingestion, automatic identity resolution, public surveillance, autonomous enforcement or external dispatch.

Local-only dependencies preserve the current deployment boundary and introduce no new runtime licences. Full-scale ontology publishing, connector ingestion, granular authorization, asynchronous jobs, model evaluation and scale testing remain separate increments. No production or legal certification is implied.

Primary architectural reference reviewed: [Palantir Ontology overview](https://www.palantir.com/docs/foundry/ontology/overview/) describes shared objects, links and actions; [object edits](https://www.palantir.com/docs/foundry/object-edits/overview) describes changes through Actions. The design above is Vigil's implementation choice.
