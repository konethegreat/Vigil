<!-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Implemented mission API -->

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
