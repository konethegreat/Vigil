<!-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 - Source extraction boundary -->
# Source extraction contract

Core owns mission source text and extraction records. Mind has no database credentials. Browser requests use the same session, origin and idempotency boundary as missions.

| Route under Core `/api/v1` | Contract |
| --- | --- |
| GET `/missions/{id}/extractions` | Scoped saved runs, source freshness and recorded review decisions |
| POST `/extractions` | `mission_id`, `evidence_id`, `expected_version`; generate and persist a proposal, returning `{run, mission}` |
| POST `/extractions/{id}/review` | `expected_version`, `object_ids`, `link_ids`, `note`; record one decision and atomically publish selected records, returning `{run, mission}` |

All POSTs require `Idempotency-Key`. Unknown fields, unknown/duplicate IDs and selected links with unselected endpoints fail. The review note is 5–1,000 characters. Empty selections explicitly reject all proposals. No request may provide tenant, actor or approval authority. Reviews require the current mission version and unchanged evidence snapshot.

Mind POST `/v1/extract` takes `mission_id`, `evidence_id`, `evidence_version`, `original`, `sha256`. Text is 5–16,000 characters; SHA-256 must match the UTF-8 source text. Input/output structures reject unknown fields. The model receives escaped source text inside the existing untrusted-content boundary.

The proposal has `objects` (at most 8), `links` (at most 12) and `gaps` (at most 5). Every object contains `id`, `object_type`, `title`, `summary`, `quote`. Every link contains `id`, `from_id`, `to_id`, `relation`, `reason`, `quote`. IDs are local proposal keys, not registry identifiers. Titles are 3–160 characters; summaries/reasons 5–500; quotations 3–400 and exact substrings of the original source. Types and relation pairs match the existing investigation registry. Extraction does not invent occurrence timestamps.

Mind returns `model`, `proposal`, `review` (`NOT_RUN`, `COMPLETE`, `UNAVAILABLE`, model and concerns), and warnings. Missing/unavailable/invalid local analysis returns HTTP 503; malformed input returns HTTP 422. An unavailable challenger may preserve a valid proposal with an explicit warning. A model response is never a reviewed graph record.

Saved runs preserve their proposal and source hash. The one-time decision records accepted proposal IDs, generated registry ID mappings, the analyst, timestamp and note; rejected proposals remain in the saved proposal. The graph stores imported assertions as REPORTED. Successful review is atomic with mission version changes and audit events. A response lost after commit can be recovered with the original idempotency key.
