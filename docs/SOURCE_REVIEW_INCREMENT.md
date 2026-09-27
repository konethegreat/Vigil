<!-- Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Local source review acceptance and limits -->

# Source review increment 03

**Implemented and verified locally, 27 September 2026.** Continues the mission and investigation increments under [ADR-0011](adr/ADR-0011-source-extraction-review.md). Changes are published as focused commits in the personal private repository and draft PR #1; the PR remains unmerged. This is a working synthetic environment, not a production agency connection.

## What an analyst can do

Open **Sources**, choose a mission, and preserve pasted text or a UTF-8 `.txt`/`.md` file. The original, its fingerprint, the custodian and the separate assessment remain inspectable. Text-file intake preserves leading spaces, CRLF line endings and a UTF-8 BOM rather than normalising the source through a browser textarea.

Request extraction from the configured local reasoning model. Mind proposes typed objects and relationships with exact source quotations. Core independently checks quotations, types, relationship endpoints and the source snapshot before storing an immutable proposal. Failed or unavailable extraction produces an explicit error; it does not fabricate a baseline proposal.

Select individual records, explain the decision, and publish or reject all. A relationship requires both selected endpoints. One transaction records the final review, selected objects, reported relationships, mission version, command receipt and audit events. Unselected proposals remain visible as rejected. Open the published records directly in the originating investigation. Mission export includes the complete proposals and decisions.

The shell now uses direct names: Investigations, Sources, Missions, AI operations and Decision history. Source originals and proposals have a reading workspace; model availability has a plain table. Promotional hero sections and decorative summary cards have been removed from the affected screens.

## Verification evidence

| Boundary | Result |
| --- | --- |
| Python intelligence | 29 tests pass; 98% aggregate statement coverage. Ruff lint and format checks pass. Includes exact quotes, malformed output, untrusted input, local model failures and optional challenger paths. |
| Java/PostgreSQL | 34 integration tests and Spotless pass. Includes forced failure after object creation to prove whole-publication rollback, duplicate retries, concurrent reviewers, stale snapshots, immutable decisions, read-only policy and cross-tenant denial. |
| Cockpit | 16 tests pass; ESLint, TypeScript and production image build pass. Tests cover gateway routes, UTF-8/CRLF/BOM preservation, invalid file content and dependent selection. |
| Source HTTP acceptance | Six groups pass through the running browser gateway, Core, Mind and database. Mission `VGL-2672FA7B`, extraction `EXT-f51f4f7f-0f35-4ca5-800c-7e3fb956938f`: two objects, one reported relationship, four verified audit events. |
| Investigation regression | All six groups pass; mission `VGL-3011F5AF`, seven verified audit events. |
| Mission regression | All eleven groups pass; completed mission `VGL-6EB7920B` used `LOCAL_MODEL`, with 21 verified audit events. |
| Live model | Installed local `llama3.1:latest` produced the accepted proposals. The challenger is unconfigured in this run; successful live two-model review is not claimed. |
| Browser publication | In `VGL-I002`, a synthetic receiving note produced two objects and one reported location relationship. Analyst selection, dependent-link clearing, publication and the saved review receipt were exercised. |
| File intake and navigation | In `VGL-I001`, source `SRC-8efa12a5` preserved all 71 characters, including two leading spaces and two CRLF pairs, verified against the imported file through Core. Pointer selection, dependency clearing, two-object/one-link publication and navigation to the published Station South object passed. |
| Browser access and layout | Independent review could inspect sources with intake/extraction disabled; Protection services showed only its own mission choices. Sources was inspected at 1600px and 390px widths; no page-level horizontal overflow at the narrow width. |

To rerun the live extraction acceptance, start the local profile with an installed reasoner configured, then run `uv run --project services/mind python scripts/verify-extractions.py`. The script requires a useful connected proposal from its synthetic fixture; stochastic model output can fail that requirement and should be inspected rather than counted as a passing run. It accepts only localhost and leaves its named synthetic mission for inspection. It does not call a hosted provider.

## Persistence and publication

The forward migration adds extraction records to the retained PostgreSQL database without a reset. Row-level security applies to the new table. Database permissions and a trigger prevent proposal changes, record deletion and rewriting a final review. These controls complement, rather than replace, the service's policy and transaction checks.

Source, service, interface and acceptance changes are separate commits on `codex/mission-foundation`. [Draft PR #1](https://github.com/konethegreatest/Vigil/pull/1) targets `develop`. No remote status checks were attached at inspection; these local checks do not establish the original full-platform CI. Existing branch-protection limitations are recorded in [GITHUB_REPOSITORY.md](GITHUB_REPOSITORY.md).

Recovery copies are stored outside the checkout at `C:\Users\Kone\Vigil-recovery\20260927-source-review`: pre-change and final Git bundles and database dumps. They are local recovery copies, not off-device backups.

## Current limits

- Source intake is text only: 5–16,000 characters, with a 64,000-byte file cap. There is no PDF, OCR, audio or camera ingestion in this increment.
- Proposals contain at most eight objects, twelve relationships and five gaps. A mission accepts at most 100 extraction runs. The local investigation registry retains its 500-object and 1,000-relationship workspace caps.
- There is one run per source and mission evidence revision. Any evidence change makes pending runs from older revisions stale, even when the particular source text is unchanged. This conservative rule favours freshness over incremental reuse.
- A new revision can produce a new proposal for an already described object. Publication creates records; it does not perform identity resolution or silently merge them with existing records.
- Exact quotation proves a textual reference, not the truth of a claim or that every part of an explanatory summary is entailed. Review remains necessary. Imported relationships begin as reported claims.
- Extraction is a bounded synchronous request. It is not a durable background model job with a resumable queue. The original command key can recover a committed response; a pre-commit timeout may require another model call.
- Mission export carries proposals and decisions, but is not a combined export of the entire cross-mission investigation registry.
- Production login, granular release grants, joint agency cases, external messaging, live surveillance, field synchronisation and remote deployment remain unimplemented.

[Connected operations](CONNECTED_OPERATIONS.md) carries the founder's surveillance and agency-coordination scenario into the next implementation direction. It specifies observations, source coverage, evidence release, requests, acknowledgments and results; these proposed contracts are not existing integrations.
