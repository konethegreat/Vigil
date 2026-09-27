<!-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Observation and agency exchange contract -->
# Local connected operations v1

Approved by the founder's instruction to implement the proposed two-agency workflow. Continues ADR-0012 with synthetic Gauteng and Limpopo teams, separate from existing operations/protection tenants. No real feed, external message or dispatch is implied.

## Decision and verification

Options: (1) add a static scenario only; (2) add a bounded transactional observation registry and explicit two-party case exchange to Core; (3) deploy a separate federation broker now. Choose (2): it verifies ownership, visibility, retries and outcomes in the existing runtime, uses no new dependencies and remains local. Option (1) cannot prove coordination; option (3) adds infrastructure before interface semantics are proven. Production identity and federation remain separate work.

1. Preserve source capabilities and immutable observations → verify RLS, producer deduplication, conflicting retries, event/receipt times, search windows and missing coverage.
2. Create joint cases and draft requests with selected observation snapshots → verify unreleased drafts stay private and other tenants cannot join by supplying IDs.
3. Release, acknowledge, accept, start, return a result and close → verify actor-specific transitions, versions, atomic receipts, replay, expiry and revocation.
4. Connect Observations and Agency inbox in Cockpit → verify synthetic end-to-end work, map/timeline selection, source inspection and responsive layout.

## Routes under `/api/v1/coordination`

| Route | Use |
| --- | --- |
| GET `/workspace` | Identity, own capabilities/observations, permitted joint cases and request metadata |
| POST `/sources` | Register source with name, kind, location, coordinates, capability, coverage interval and current availability |
| POST `/sources/{id}/status` | Versioned availability update with explanation |
| POST `/observations` | Preserve producer event ID, source, event time, clock uncertainty, observed description, original text and optional superseded observation |
| POST `/search` | Read-only bounded time/source/text query; returns matching observations and per-source coverage, never interpreting missing coverage as no match |
| POST `/cases` | Create an incident case with one named peer agency, shared purpose, incident time and location |
| GET `/cases/{id}` | Shared case, visible request metadata and receipts; packet contents only under an active release |
| GET `/cases/{id}/export` | Same authorisation as the case view, with verification fingerprints |
| POST `/requests` | Create private draft, selected own observations, task, deadline, release reason and expiry |
| POST `/requests/{id}/commands` | Actor-specific lifecycle, final result with selected recipient observations, or revoke own outgoing evidence release |

Every mutation requires an idempotency key; unknown fields, authority fields and stale versions fail. Session identity supplies the agency. Commands return metadata only, so replay cannot resurrect expired/revoked packet content. Server time is the receipt time. Producer identity is `(tenant, source, producer_event_id)`; the same payload returns the original record, different content conflicts. Observations are immutable; corrections are new records referencing their predecessor. Sharing copies only selected observation fields and the selected originals into a fingerprinted packet. Whole mission exports and unrelated source records are never included.

## Shared case and request model

Case creation explicitly shares the entered case header and purpose with the named peer. Each side may originate a request. Draft requests are visible only to their sender. Releasing a request exposes it to its named recipient's durable local inbox. The recipient records delivery acknowledgment; this local implementation does not claim network delivery to an external agency.

`DRAFT → RELEASED → DELIVERED → ACCEPTED → IN_PROGRESS → RESULT_RECORDED → CLOSED`

Recipient alone records DELIVERED, ACCEPTED, IN_PROGRESS, RESULT_RECORDED, DECLINED or OUTCOME_UNKNOWN. Sender alone releases, cancels, revokes its original packet or closes a recorded result/unknown outcome. Every transition has an append-only receipt with actor, agency, timestamp and explanation. Acceptance names the responsible actor. A result requires a result explanation and at least one own source observation; those selected originals are explicitly released back to the requester. Result evidence shares the original request's expiry.

Expired or revoked releases deny further recipient workflow transitions and packet reads. The sender can still cancel or revoke; packet owners retain their own originals. Basic request metadata and already communicated lifecycle notes remain visible to both case participants after expiry/revocation. Revocation restricts future packet retrieval; it does not erase an already downloaded copy or recall communicated notes. Export applies the same restrictions. There is no unrestricted cross-tenant service identity.

## Bounded environment

At most 50 sources, 2,000 observations, 100 originated cases and 100 requests per case. A packet contains 1–20 selected observations. Source text is limited to 8,000 characters; request/receipt text is limited. Source coordinates and the map are illustrative; no live geographic feed, image recognition, identity matching or inference of driver identity is implemented. Existing local AI source review remains available; this increment's timing and workflow logic is deterministic.

The local database is the durable inbox. Restart and duplicate delivery are testable; a distributed broker, signed transport, field offline queues and automatic retries across organisations are later work. Source coverage is a declared interval and current status, not a measured uptime history. A search without a match inside declared available coverage is labelled accordingly, without claiming that an event did not occur.
