<!-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Implemented mission API -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Investigation routes and source semantics -->

# First mission API

This document describes the implemented local increment, distinct from the broader future API catalogue. All development records are synthetic. Public create/command payloads are defined in `contracts/mission.schema.json`; response types are in `apps/cockpit/src/lib/contracts.ts`.

## Boundaries

The browser calls `/api/vigil/*` on Cockpit. Cockpit forwards only allowlisted paths to Core's `/api/v1/*`, carries the internal gateway credential server-side, and manages a signed HttpOnly session cookie. Core validates the signature, derives a fixed demonstration identity, and scopes PostgreSQL transactions to that identity's tenant and purpose. Client-supplied tenant or actor headers are not authority.

`operations` can manage operational missions; `protection` can manage protection missions; `reviewer` has read-only access to operational missions. The persona selector is deliberately a public synthetic demonstration feature. It is not production login or an account entitlement system.

## Core routes

| Method | Route under `/api/v1`     | Result                                                                            |
| ------ | ------------------------- | --------------------------------------------------------------------------------- |
| GET    | `/health`                 | Core/database liveness and synthetic mode; no mission content                     |
| POST   | `/session`                | Gateway-authenticated issuance of a signed demonstration session                  |
| GET    | `/session`                | Current derived identity                                                          |
| GET    | `/missions`               | Missions visible in the current workspace and identity                            |
| POST   | `/missions`               | Create a mission with title, domain, objective, priority and location             |
| GET    | `/missions/{id}`          | Scoped mission detail; inaccessible IDs return not found                          |
| POST   | `/missions/{id}/commands` | Apply a validated command with `expected_version`                                 |
| GET    | `/missions/{id}/audit`    | Ordered events and local hash-chain verification                                  |
| GET    | `/missions/{id}/export`   | Consistent mission/audit snapshot; a successful export access receipt is recorded |
| GET    | `/intelligence/status`    | Actual configured local models, endpoint reachability and readiness               |

The gateway maps these to matching `/api/vigil` paths. Its session response omits the signed token; the token is carried only in the cookie. Mutations require an allowed same-origin browser request, matching origin/host and bounded JSON.

## Commands and consistency

Every mutation uses `Idempotency-Key`. Its scope includes tenant, actor, resource and operation; reusing a key with changed input is a conflict. Mission commands also carry the last observed `expected_version`. One concurrent writer can succeed; a stale writer must refresh. The command receipt, mission change and audit event commit together.

- `add_evidence`: preserve the original text, source, kind, statement and SHA-256. Newly supplied evidence begins unverified; an observation time is not invented.
- `review_evidence`: append an assessment with the reviewer and reason. Preserve the original and earlier assessments; increment evidence revision.
- `generate_plan`: analyse an authorised evidence snapshot outside the database transaction, then recheck its revision before storing the proposal. Retrying a completed request with the same key returns the recorded result. Source revisions make older plans stale.
- `transition_task`: require a receipt and an allowed transition. Normal progression is PROPOSED → AUTHORISED → SENT → ACCEPTED → IN_PROGRESS → CONFIRMED. SENT can become DECLINED or OUTCOME_UNKNOWN; IN_PROGRESS can become OUTCOME_UNKNOWN. Explicit recovery transitions preserve uncertainty.
- `record_outcome`: require a current plan, every task confirmed, an outcome summary, and verification. The completed mission becomes immutable.

Unknown command fields are rejected. Denied operations do not partially alter mission state. A failed network response does not prove a mutation failed: retry the same request and key before creating a new operation.

## Mind routes and output

Core supplies a bounded snapshot to Mind's internal `/v1/plan` endpoint. Mind has no database connection. Its `/v1/status` endpoint checks the local runtime; both routes require the internal credential. `/health` carries no mission content.

Model output is strict structured data, with allowed source references checked against the supplied snapshot. Reasoner failure yields an explicitly labelled BASELINE checklist. A valid model response yields LOCAL_MODEL; an optional challenger returns concerns or an explicit unavailable state. Referencing an existing source ID does not prove factual entailment. No model role can send messages, dispatch resources, or mutate missions directly.

Audit hashes establish local consistency under the application boundary. A sufficiently privileged database administrator can rewrite underlying records; independent anchoring and production identity are future requirements. Access receipts currently cover successful scoped reads/exports, not a complete denied-access security-event pipeline.

## Investigation registry (increment 02)

| Method | Route under `/api/v1` | Result |
| --- | --- | --- |
| GET | `/investigations` | All authorised objects, assertions and assessment histories, plus current source provenance and registry limits |
| POST | `/investigations/commands` | Atomic typed record/review, updated mission and recorded identifier |

Commands are defined in `contracts/investigation.schema.json`; response types are in `apps/cockpit/src/lib/investigations.ts`. All use the existing session/origin boundary, `Idempotency-Key`, `mission_id` and `expected_version`. Successful writes increment the mission version and commit an audit event containing the validated assertion content. Successful graph reads record actor and purpose. The operations demo purpose is now `DISTRICT_OPERATIONS`; historical receipts retain their original purpose.

- `create_object`: immutable `PERSON`, `ORGANISATION`, `PLACE`, `ASSET`, `EVENT` or `DOCUMENT`, with title, summary and a source belonging to the cited mission. Optional `occurred_at` is accepted only for events and requires an explicit timezone.
- `create_link`: visible `from_id` and `to_id`, relation, cited source, assessment (`REPORTED`, `CORROBORATED`, `DISPUTED`) and reasoning. References may span missions in the same workspace. No identity matching or cross-tenant linkage occurs.
- `review_link`: existing `link_id`, `expected_revision`, assessment and reasoning. Requires the assertion's originating mission. Earlier assessments and the original source hash are preserved.

Typed pairs: `LOCATED_AT` links Asset/Event/Organisation to Place; `INVOLVES` links Event to a non-Event; `REPORTED_BY` links Event/Document to Person/Organisation; `SUPPLIED_BY` links Asset/Document to Organisation; `PRECEDES` links two Events; `RELATED_TO` links distinct supported objects. These are source-cited analyst assertions. `PRECEDES` does not compute or prove causality or validate a source clock.

Provenance pins the **mission evidence snapshot revision**, not a per-source revision. Any evidence addition or assessment change in that mission conservatively flags its earlier objects and assertions. Relationship review can pin the new revision; immutable objects retain the flag until a future versioned-object workflow. Source status and relationship assessment are independent and both remain visible.

Registry writes and graph reads serialize per workspace. Source provenance is evaluated against current mission evidence during each query; this is not an exported multi-mission historical snapshot. Local caps are 500 objects and 1,000 assertions per tenant. The canvas draws at most 30 objects with an explicit limit notice; Objects and search cover the complete bounded registry. `REGISTRY_LIMIT`, `INVALID_RELATION`, `LINK_EXISTS`, `NOT_FOUND`, `VERSION_CONFLICT`, `MISSION_COMPLETED` and existing policy/idempotency errors are explicit.

Mission exports include command audit details, but do not yet package the shared registry or all related missions. The planner continues to analyse mission evidence; the shared graph is not automatically included in model prompts. These are documented next increments, not implied existing features.
