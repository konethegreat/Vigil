<!-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 - Authorised source-to-investigation workflow -->
# ADR-0011: Source extraction with explicit analyst decisions

## Status

Implementation decision under the founder's 27 September instruction to continue beyond the existing scope, with a direct interface and focused commits. Continues ADR-0009 and ADR-0010; does not replace the full platform architecture. Single-agent work, existing identity, branch `codex/mission-foundation` and draft PR #1 retained.

## Context

The graph and mission loop work, but every object and relationship is entered manually. Source intake and review should form one usable workflow. The founder rejects decorative progress bars and dashboard elements; the interface must prioritise sources, findings and decisions.

## Options

1. Keep manual object entry only. Lowest complexity, but fails to connect the existing local AI service to investigation work.
2. Preserve text sources using the mission boundary; generate strict, quoted proposals through Mind; store an immutable proposal and a single reviewed decision; atomically publish selected records through existing investigation commands. Selected.
3. Build general binary-file storage, PDF/OCR, entity resolution, streaming connectors and a distributed job system now. These require separate file handling, retention, identity, throughput and evaluation contracts. They remain platform directions rather than implied features of this text-source increment.

## Decision

- Add a Sources workspace with a plain source list, preserved text and reviewed proposals. Accept pasted text and bounded UTF-8 `.txt`/`.md` files. Preserve decoded source text exactly; its SHA-256 is a text-content hash, not a claim about original binary-file bytes. Reject invalid encoding or oversize content without truncation.
- Mind extracts bounded typed objects and relations, each with an exact quotation from the authorised source. It receives no database/action tools. Reuse the configured local reasoner and optional challenger; no hosted service or model download is required. No reasoner means explicitly unavailable extraction, not a fabricated deterministic extraction.
- Validate structure, quote membership, unique proposal IDs, endpoint references and relation types in Python and Java. Quote membership is not proof of factual support: proposals require analyst review. Model outputs cannot provide tenant, actor, final assessment or operational authority.
- Store proposals immutably with mission/source/hash/evidence snapshot, model and challenge result. Model execution occurs outside database transactions; recheck versions on return. At most one saved run per source and mission evidence snapshot prevents duplicate publication through concurrent analysis requests.
- Review selected object and relationship IDs with a required note. Both endpoints must be selected for a selected relationship. Unselected proposals are rejected; all decisions remain visible. Publish accepted records as REPORTED, retaining the proposal quotation, model and decision provenance in the extraction record and audited command receipt. No automatic person matching, identity merge or cross-tenant linking.
- Use existing expected versions, idempotency and forced RLS. A stale source snapshot cannot be published. A completed mission cannot receive a new extraction or review. Rejected/stale runs remain inspectable. Review and all graph writes commit or roll back together.
- Simplify navigation, headings and investigation summary chrome. Use actual state labels and actionable errors; no decorative progress meters or synthetic confidence percentages.

## Consequences

Existing Java/PostgreSQL, Python and TypeScript boundaries remain; no new runtime dependency or licence is introduced. This is bounded text ingestion, not full Forge ingestion, OCR or large-file processing. Synchronous local calls retain their provider timeout; durable background jobs are future work. Exact quotations can still support incorrect interpretations, and optional challenger output is advisory. Production identity, granular markings and the original platform gates remain open.

## Acceptance and commit plan

1. Record the source/proposal/review contract and this decision.
2. Implement strict local extraction and adversarial Python tests in one commit.
3. Implement durable proposals, transactional review and database integration tests in one commit.
4. Implement direct source intake/review UI and simplify decorative chrome, with relevant frontend checks.
5. Verify live local extraction, selection/rejection, graph publication, isolation, stale-source protection and restart persistence; update acceptance, PR and recovery copies separately.
