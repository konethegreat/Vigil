<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Authoritative API conventions, endpoint specifications (OMS, objects, actions, functions, datasets, policy, audit, Mind, ER, cases) and event catalogue (P0) -->

# Vigil — API Design

> **Status: SHARED, APPEND-ONLY** (PHASES Rule 2; File Ownership Master Index).
> - Owning phases **append** new entries. **Existing entries are never edited or deleted**, not even to fix a typo.
> - To change an endpoint, append a new entry that supersedes it. To retire one, append a `DEPRECATION` entry (§A.10).
> - New endpoint entries go at the **end of Part B** (above Part C). New events go at the **end of Part C**. Every change also appends a row to the **Part D log**, which is always the last section.
>
> **Contract sources of truth.** Protobuf in `proto/vigil/<domain>/v1/` (Buf: `buf lint`, `buf breaking`), OpenAPI 3.1 generated into `proto/openapi/`, and AsyncAPI 3 in `docs/asyncapi/` (P2). This file is the human-authoritative registry of names, semantics, shapes and errors. Generated specifications MUST match it, and an append here comes with the matching `proto/` change (Rule 6).
> **Read with:** [`ARCHITECTURE.md`](ARCHITECTURE.md) §6 and §8 · [`VIGIL_DOMAIN_BLUEPRINT.md`](VIGIL_DOMAIN_BLUEPRINT.md) (semantics, rules W1–W12) · [`DATA_MODEL.md`](DATA_MODEL.md).
> **Examples** use the fictional tenant `demo`; all names and identifiers are fictional.

---

## Part A — Conventions (P0)

### A.1 Protocols, base paths and packages

| Surface | Standard | Base |
|---|---|---|
| Public REST | OpenAPI 3.1, JSON over HTTPS (TLS 1.3) | `https://<pod-host>/api/v1`, served by the gateway and the Java BFF in `core` |
| Service RPC | Protobuf + gRPC; **Connect** for browser and mobile | Package `vigil.<domain>.v1`, in `proto/vigil/<domain>/v1/` |
| Streaming responses | Server-Sent Events (`Accept: text/event-stream`), used for Mind turns only | same REST path |
| Events | CloudEvents 1.0 over Kafka 4 (core) and NATS JetStream (edge, control) | Part C |

gRPC packages used in this file:
- `vigil.ontology.v1` (P5), `vigil.objects.v1` (P7), `vigil.actions.v1` (P8), `vigil.forge.datasets.v1` (P4), `vigil.warden.v1` (P3; includes the audit service);
- `vigil.functions.v1` (P9), `vigil.mind.gateway.v1` (P14), `vigil.mind.agents.v1` (P15), `vigil.er.v1` (P16), `vigil.cases.v1` (P19). Owners follow the File Ownership Master Index (`proto/vigil/<domain>/`).

### A.2 Headers

| Header | Direction | Required on | Rules |
|---|---|---|---|
| `Authorization: Bearer <token>` | request | every call except `/healthz` | Keycloak OIDC access token (PKCE for Cockpit, client credentials for services), audience `vigil-api`, lifetime ≤ 15 min. Agents use an RFC 8693 token-exchange token whose `act` claim names the agent session. That claim, not the request body, is how the platform knows a call came from an agent |
| `X-Vigil-Tenant` | request | every call | Tenant slug. Must equal the token's `vigil_tenant` claim, or `POLICY_DENIED` |
| `X-Vigil-Purpose` | request | every data-plane call (objects, actions, functions, datasets, policy/justifications, audit, Mind, ER, cases) | A registered purpose code (`warden.purpose`) that the caller may declare. Missing or unknown: `PURPOSE_REQUIRED` |
| `X-Vigil-Case-Id` | request | when the purpose is case-predicated; optional otherwise | Case PK. It must be OPEN and the caller a member; otherwise `CASE_REQUIRED` (the same error whether the case is missing or invisible) |
| `X-Vigil-Justification` | request | when a checkpoint applies (W7) | A `justification_id` from POL-02. Missing: `JUSTIFICATION_REQUIRED` |
| `Idempotency-Key` | request | **every mutating call** (POST, PUT, PATCH, DELETE that changes state). Not required for pure queries: OBJ-01, ACT-01, FN-01, POL-01 | 8–128 characters `[A-Za-z0-9_-]`; UUIDv7 recommended. Scope: (tenant, principal, endpoint). Semantics in §A.9 |
| `X-Vigil-Watermark` | both | optional on requests; always on responses to reads and actions | Opaque token. On a request: "reflect at least this state". On a response: the current watermark |
| `X-Request-Id` | both | optional on requests; always on responses | UUID; generated if absent; echoed in errors and audit |
| `traceparent` | both | optional | W3C Trace Context. Spans carry tenant, purpose, case and policy-context hash, never values |
| `RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset`, `Retry-After` | response | every call (`Retry-After` on 429 and 503) | §A.7 |
| `Idempotent-Replayed: true` | response | replays | §A.9 |
| `Deprecation`, `Sunset`, `Link` | response | deprecated endpoints | §A.10 |

### A.3 JSON conventions

- **Field names** are snake_case (proto field names are preserved; Connect JSON uses `use_proto_names`). Object property keys are the property API names, which are snake_case too (DATA_MODEL §3.3).
- **Enums** are UPPER_SNAKE strings. Clients MUST tolerate unknown enum values and unknown fields.
- **Numbers:** 64-bit integers are JSON **strings** (proto3 JSON mapping): an object's `version`, `edit_seq`, `seq`, `snapshot_id`, `total_estimate`, row and bucket counts. 32-bit integers (for example type versions and `duration_ms`) and doubles are numbers. Decimals (money) are strings: `"742500.00"`, with a separate currency property.
- **Times** are RFC 3339 UTC with milliseconds (`2026-09-25T10:15:30.123Z`). Durations are `*_ms` integers. Bytes are base64.
- **Geometry** is GeoJSON (RFC 7946), so coordinates are `[longitude, latitude]`.
- **Missing values:** an absent field was not requested or does not apply; `null` means the value is null; a redacted value is an object (§A.8).
- **Object references** are `{"object_type": "Supplier", "pk": "…"}` or a RID string (Blueprint §1.2).
- **Object representation** (used by every endpoint that returns objects):

```json
{
  "object_type": "Supplier",
  "pk": "ocds:ocds-xxxxxx-2026-000123:party-7",
  "rid": "ri.objects.demo.object.Supplier.6f1c2a9e-4b7d-5e3a-9c21-7d8e9f0a1b2c",
  "version": "4",
  "title": "Zenzele Example Civils (Pty) Ltd",
  "properties": {
    "name_raw": "Zenzele Example Civils (Pty) Ltd",
    "registration_number": "2015/123456/99",
    "review_status": "UNDER_REVIEW",
    "contact_phone": { "redacted": true, "reason": "MARKING" }
  },
  "security": { "classification": "PUBLIC", "markings": [], "releasability": [], "purpose_tags": ["PROCUREMENT_INTEGRITY", "DUE_DILIGENCE"] },
  "provenance": { "source_id": "ocds-etenders", "dataset_rid": "ri.forge.demo.dataset.01926e5c-3a1b-7c4d-8e2f-9a0b1c2d3e4f",
                  "dataset_txn_id": "ri.forge.demo.transaction.01926e5d-0b2c-7d3e-9f40-5a6b7c8d9e0f",
                  "fetched_at": "2026-09-24T03:00:12.000Z", "terms_ref": "terms/ocds-etenders@1" },
  "updated_at": "2026-09-24T03:12:44.120Z"
}
```

`security` shows only labels the caller already satisfies. `provenance` is included when `include.provenance` is true.

### A.4 Pagination

- **Request:** `page_size` (default 100, **maximum 500**; larger is `VALIDATION_FAILED`) and `page_token` (opaque). These are query parameters on GET and body fields on POST.
- **Response:** `data` (array), `next_page_token` (absent or `null` on the last page), and `total_estimate` (int64 string or `null`).
  - `total_estimate` counts **only rows visible to the caller**. It is approximate unless small, and never reveals invisible rows.
- **Tokens:**
  - signed and bound to (tenant, principal, policy-context hash, query hash);
  - valid for 15 minutes;
  - a token presented with a changed context or query gets `VALIDATION_FAILED` (reason `PAGE_TOKEN_INVALID`); an expired one gets `EXPIRED`.
- Ordering is stable keyset pagination on the `order_by` keys plus `pk`.

### A.5 Object-set definition and filter DSL (used by OBJ-01, functions and Mind tools)

**Object set** (a tree):

| Node | Shape |
|---|---|
| `base` | `{"base": "Award"}` |
| `interface` | `{"interface": "Party"}` |
| `static` | `{"static": {"object_type": "Supplier", "pks": ["…"]}}` (at most 1,000 PKs) |
| `filter` | `{"filter": {"object_set": <set>, "where": <predicate>}}` |
| `search_around` | `{"search_around": {"object_set": <set>, "link": "awarded_to", "where": <predicate>}}` (`where` optional) |
| Set algebra | `{"union": [<set>, …]}`, `{"intersect": [<set>, …]}`, `{"subtract": [<a>, <b>]}` |

**Predicates:**

| Operator | Shape |
|---|---|
| Logical | `{"and": [ … ]}` · `{"or": [ … ]}` · `{"not": <p>}` |
| Comparison | `{"eq" \| "gt" \| "gte" \| "lt" \| "lte": {"property": "p", "value": v}}` · `{"in": {"property": "p", "values": [ … ]}}` (at most 1,000) · `{"is_null": {"property": "p"}}` · `{"starts_with": {"property": "p", "value": "…"}}` |
| Text (OpenSearch) | `{"match": {"property": "title", "query": "…", "operator": "AND"}}` · `{"phrase": {…}}` · `{"fuzzy_name": {"property": "name_raw", "query": "Mokwena", "min_score": 0.80}}` (uses the Vigil name analysers, DATA_MODEL §10) |
| Geo | `{"within_distance": {"property": "location", "point": {"type": "Point", "coordinates": [31.03, -29.87]}, "metres": 5000}}` · `{"within_bbox": {…}}` · `{"intersects": {"property": "area", "geometry": <GeoJSON>}}` · `{"in_h3": {"property": "location", "resolution": 7, "cells": ["…"]}}` |
| Vector | `{"knn": {"property": "embedding", "text": "…" or "vector": [ … ], "k": 50}}` (k ≤ 200; text is embedded through the Mind gateway, route E) |
| Link existence | `{"has_link": {"link": "restricted_by", "where": <predicate>}}` (counts as a search-around hop) |

**Ordering:** `order_by: [{"property": "p", "direction": "ASC" | "DESC"}]`, at most 3 keys.

**Aggregations** (nesting ≤ 2 levels; at most 10,000 buckets): `{"<name>": {…}}` with metric types `count`, `sum`, `avg`, `min`, `max`, `approx_distinct` and `percentiles` (`{"property": "p", "percents": [50, 90]}`), and bucket types `terms` (`size` ≤ 1,000), `date_histogram` (`interval`: DAY, WEEK or MONTH), `range` and `h3` (`resolution`).

**Limits** (Blueprint §1.10): depth ≤ 10, at most 100 leaf predicates, **at most 3 search-around hops** (including `has_link`), and at most 100,000 objects in an intermediate set. Beyond these limits the error is `LIMIT_EXCEEDED`; deep paths go to traversal (TRV-01).

The OSS picks the engine (Postgres, OpenSearch or traversal). The choice is invisible to clients, and every engine enforces the same policy.

### A.6 Errors

Every error body has exactly this shape (`Content-Type: application/json`):

```json
{
  "error": "LINKAGE_LOCKED",
  "message": "Person-level linkage across responsible parties is locked: no active s57(1)(a) authorisation covers this scope.",
  "status_code": 423,
  "timestamp": "2026-09-25T10:15:30.123Z",
  "request_id": "01926e5c-3a1b-7c4d-8e2f-9a0b1c2d3e4f",
  "details": [
    { "type": "GATE", "reason": "S57_1A_LINKAGE", "metadata": { "responsible_parties": ["CIPC", "NT_OCPO"], "authorisation": "NONE" } }
  ]
}
```

| Field | Rule |
|---|---|
| `error` | Machine-readable code from the catalogue below |
| `message` | Human-readable. **Never** contains property values, names of invisible resources, or markings the caller does not hold |
| `status_code` | The HTTP status, repeated |
| `timestamp` | RFC 3339 UTC |
| `request_id` | Mirrors `X-Request-Id` |
| `details[]` | Optional. Each item: `type` ∈ `FIELD_VIOLATION`, `CRITERION`, `LIMIT`, `POLICY`, `GATE`, `CONFLICT`, `RETRY`, `ACTION_RUN`, plus optional `field`, `reason`, `message`, `metadata` |

**Catalogue.** New codes are appended here; clients treat unknown codes by their HTTP class.

| Code | HTTP | Meaning | Retry? |
|---|---|---|---|
| `VALIDATION_FAILED` | 400 / 422 | 400: malformed request or parameter. 422: an action rejected on submission criteria (the run is recorded as `REJECTED`; details carry `CRITERION` and `ACTION_RUN`) | No |
| `PURPOSE_REQUIRED` | 400 | `X-Vigil-Purpose` missing or not registered | No |
| `CASE_REQUIRED` | 400 | The purpose is case-predicated and no valid, open, accessible case was given | No |
| `UNAUTHENTICATED` | 401 | Token missing, expired or invalid | After re-authentication |
| `POLICY_DENIED` | 403 | Warden or OpenFGA denied: markings, clearance, releasability, purpose not permitted, role, tenant mismatch, separation of duties, no cleared model | No |
| `JUSTIFICATION_REQUIRED` | 403 | A checkpoint needs a justification (POL-02) | After capture |
| `APPROVAL_REQUIRED` | 403 | The caller demanded immediate application (`staging: "FORBID"`) but the action needs approval | No |
| `NOT_FOUND` | 404 | The resource does not exist **or is not visible to the caller** (W11: no existence oracle) | No |
| `CONFLICT` | 409 | Idempotency key reused with a different body; version conflict; approval already decided; transaction not OPEN | After re-read |
| `EXPIRED` | 410 | Staged run, approval request, page token, transaction or download link expired | No |
| `LIMIT_EXCEEDED` | 422 | A platform limit (Blueprint §1.10); details name the limit, the maximum and the actual value | No |
| `QUALITY_CHECK_FAILED` | 422 | A `BLOCK` data-quality check failed; the transaction is `ABORTED` | No |
| `LINKAGE_LOCKED` | 423 | The s57(1)(a) gate is closed for this scope (Blueprint §5.9) | No |
| `MODULE_GATED` | 423 | Another gated module lacks an active authorisation (Blueprint §4.4) | No |
| `RATE_LIMITED` | 429 | Rate limit exceeded; see `Retry-After` | Yes |
| `INTERNAL` | 500 | Unexpected; quote `request_id` | Yes, with the same idempotency key |
| `WRITEBACK_FAILED` | 502 | The writeback webhook failed or timed out. **Nothing was committed**; the run is `FAILED` | New key |
| `UNAVAILABLE` | 503 | A dependency is down (for example model serving, which means no-AI mode) | Yes |
| `WATERMARK_PENDING` | 503 | The requested watermark was not reached within 2 s | Yes (`Retry-After: 1`) |
| `DEADLINE_EXCEEDED` | 504 | The synchronous budget (60 s for actions) ran out before commit; nothing was committed | New key |

### A.7 Rate limiting

- Token buckets in Valkey, per (tenant, principal hash, route class) (DATA_MODEL §12).
- Default limits: reads 600/min · object-set queries 120/min · mutations 60/min · Mind turns 20/min · exports 10/hour · policy evaluate 30/min.
- Per-tenant aggregate caps also apply, and tenants may tighten the defaults.
- Every response carries `RateLimit-Limit`, `RateLimit-Remaining` and `RateLimit-Reset` (seconds). A 429 also carries `Retry-After`.
- The header names follow the IETF httpapi RateLimit-headers draft; switch to the published RFC field syntax when it appears (**confirm at P10**).

### A.8 Redaction representation

| Situation | Representation |
|---|---|
| A property the caller may not see (W2) | `{"redacted": true, "reason": "MARKING"}`. `reason` ∈ `MARKING`, `CLASSIFICATION`, `RELEASABILITY`, `PURPOSE`, `CASE`, `LINKAGE_LOCKED`, `ERASED`, `NO_AI`. Marking *names* are never disclosed |
| `cipher_text` for a holder of the column marking | `{"masked": true, "display": "******4521"}`. The clear value only with `reveal=<property>`, a `REVEAL` justification and an audit event |
| A foreign key or link to an invisible object (W3) | `{"redacted": true, "reason": "MARKING"}` in place of the value; the link is omitted from link listings |
| Invisible objects | Absent from results and counts; direct fetch returns `NOT_FOUND` |
| Erased values (DATA_MODEL §6.3) | `{"redacted": true, "reason": "ERASED"}` |
| Aggregations | Invisible rows simply do not contribute (small-cell suppression applies to aggregate exports, DATA_MODEL §13) |

A pseudonym is its own property (for example `id_psn`), filterable under W1–W2; it is not a redaction form.

### A.9 Idempotency, watermarks and consistency

- **Idempotency.**
  - The request hash is SHA-256 over method, path and the canonical JSON body (RFC 8785).
  - The same key with the same hash **replays** the original status and body, with `Idempotent-Replayed: true`. The same key with a different hash gets `CONFLICT`.
  - Keys are kept for at least 24 h in Valkey, and durably in `actions.action_run` for actions.
  - A `FAILED` or `REJECTED` run is terminal. Retrying needs a **new** key.
  - Writeback targets receive `X-Vigil-Action-Run-Id`, so they can deduplicate (ACT-06).
- **Watermarks.**
  - Edits are visible immediately to Postgres-served reads, because the action commits in the same database.
  - OpenSearch-served reads wait up to 2 s for the requested `X-Vigil-Watermark`, or apply the edit overlay (P7.5). Otherwise they return `WATERMARK_PENDING`.
  - Responses report the watermark they reflect. Cockpit shows "data as of" from it.
- **Consistency:** each query is snapshot-consistent within its engine; across engines it is consistent up to the watermark.

### A.10 Versioning and deprecation

- The major version is in the URL (`/api/v1`) and in proto packages (`v1`). There are **no breaking changes inside v1**, and `buf breaking` gates CI.
- **Additive changes only:** new endpoints, optional fields, enum values and error codes.
- A breaking change means `/api/v2` and `v2` packages, run side by side.
- **Deprecation:**
  - append a `DEPRECATION` entry to Part B naming the endpoint ID, its successor and the sunset date;
  - responses then carry `Deprecation: @<unix-seconds>` (RFC 9745), `Sunset: <HTTP-date>` (RFC 8594) and `Link: <successor>; rel="successor-version"`;
  - the minimum window is 6 months or two `stable` releases, and 12 months for `lts` and air-gapped customers.
- Ontology-scoped SDKs (P31) are versioned per ontology version.

### A.11 Security and audit on every endpoint

1. Authenticate, then check the tenant, then build a PolicyContext (Warden), then evaluate (W1–W12), then run engines with pushdown.
2. Every call emits an audit event: `<domain>.<verb>` with actor, purpose, case, resource RID, decision, and query hash or row count. Denials are audited too.
3. Request and response bodies are never logged. Errors never leak existence (W11).
4. Agents call through the same endpoints with the same checks, *as the user*. Only `agent_executable` actions without approval can be applied by an agent; everything else is staged (Blueprint §1.7).

---

## Part B — Endpoint specifications

> Entry format: `ID · METHOD path — owner`. **Append new entries at the end of Part B.** Deprecations are appended entries, never edits.

### B.1 Ontology Metadata Service (P5) · `vigil.ontology.v1.OntologyService`

#### OMS-01 · `GET /api/v1/ontology/object-types` — P5
- **Query:** `branch` (default `main`), `status` (repeatable: ACTIVE, EXPERIMENTAL, DEPRECATED), `implements` (interface API name), `page_size`, `page_token`.
- **Headers:** `Authorization`, `X-Vigil-Tenant`. No purpose is needed for metadata.
- Only types whose type-level markings the caller holds are listed (W11).

```json
{
  "data": [
    { "api_name": "Supplier", "rid": "ri.ontology.demo.object-type.01926e5c-3a1b-7c4d-8e2f-9a0b1c2d3e4f",
      "display_name": "Supplier", "status": "ACTIVE", "version": 3, "primary_key": "supplier_key", "title_property": "name_raw",
      "implements": ["PartyRecord"], "pack": { "name": "integrity", "version": "1.0.0" },
      "security": { "classification": "PUBLIC", "markings": [] } }
  ],
  "next_page_token": null,
  "total_estimate": "11"
}
```

Errors: `VALIDATION_FAILED`, `UNAUTHENTICATED`, `POLICY_DENIED`, `RATE_LIMITED`.

#### OMS-02 · `GET /api/v1/ontology/object-types/{objectType}` — P5
- **Query:** `branch`, `version` (default: current).
- **Response:** the OMS-01 summary plus:
  - `properties[]`: `api_name`, `base_type`, `is_array`, `value_type`, `nullable`, `editable`, `conflict_policy`, `searchable`, `column_security` {`classification`, `markings`}, and `accessible` (whether the caller can read it);
  - `links[]`: `api_name`, `inverse_api_name`, `cardinality`, `target`;
  - `interfaces[]`;
  - `datasources[]` (only those the caller can see);
  - `existence_policy`.
- Errors: `NOT_FOUND` (missing or invisible), `VALIDATION_FAILED`.

#### OMS-03 · `POST /api/v1/ontology/object-types/{objectType}/versions` — P5
Publishes a new immutable version (Blueprint §1.11). Needs the OpenFGA role `ontology-editor` on the branch, or `ontology-publisher` on `main`. Headers: `Idempotency-Key`.

```json
{
  "branch": "main",
  "definition_format": "YAML",
  "definition": "api_version: vigil.ontology/v1\nkind: ObjectType\nmetadata:\n  api_name: Supplier\n…",
  "expected_current_version": 3,
  "migration_plan": null,
  "dry_run": false
}
```

**Validation:**
- the definition validates against `packs/_schema`;
- `metadata.api_name` equals `{objectType}`;
- `expected_current_version` matches, otherwise `CONFLICT`;
- a `BREAKING` result without `migration_plan` is `VALIDATION_FAILED` (422);
- a definition that *removes* markings or lowers classification is `POLICY_DENIED` (use declassification, W8);
- every editable property declares `conflict_policy`.

**201** (for a `dry_run`, 200 with the computed diff and no publish):

```json
{
  "type_version": { "rid": "ri.ontology.demo.type-version.01926e60-1c2d-7e3f-8a4b-5c6d7e8f9a0b", "api_name": "Supplier",
                    "version": 4, "compat": "COMPATIBLE", "definition_sha256": "5b0e2c…", "branch": "main",
                    "published_by": "user:3f2a…", "published_at": "2026-09-25T10:02:11.004Z" },
  "diff": [{ "change": "ADD_PROPERTY", "property": "vat_number", "class": "COMPATIBLE" }],
  "physical_ddl": { "status": "PENDING", "ddl_id": "01926e60-2d3e-7f4a-9b5c-6d7e8f9a0b1c" }
}
```

Emits `vigil.ontology.type.published.v1`. Errors: `VALIDATION_FAILED`, `CONFLICT`, `POLICY_DENIED`, `NOT_FOUND`.

#### OMS-04 · `GET /api/v1/ontology/type-versions/{typeVersionRid}` — P5
Returns the canonical definition (JSON), `definition_sha256`, `compat`, `branch`, `version`, `published_by`, `published_at` and `has_migration_plan`. Errors: `NOT_FOUND`.

#### OMS-05 · `GET /api/v1/ontology/action-types/{actionType}` — P5
For generated forms (P13) and agent tool descriptions (P15). Returns:
- `parameters[]` (name, type, constraints);
- `submission_criteria[]` (name and failure message; expressions only for `ontology-editor`);
- `outcome_class`, `requires_human_approval`, `agent_executable`, `justification_required`, `allowed_purposes`;
- `approval` {`approver_role`, `required_approvals`, `separation_of_duties`, `expires_after_ms`};
- `has_writeback_webhook`, `limits`.

Errors: `NOT_FOUND`.

### B.2 Objects and object sets (P7) · `vigil.objects.v1.ObjectSetService`

#### OBJ-01 · `POST /api/v1/object-sets/query` — P7
- **Headers:** `X-Vigil-Purpose` (required), `X-Vigil-Case-Id`, `X-Vigil-Justification`, `X-Vigil-Watermark`. No `Idempotency-Key` (a read).
- **Body:** `object_set` (required; §A.5), `select` (property names; default all visible), `order_by`, `aggregations`, `page_size`, `page_token`, `include` {`security` (default true), `provenance` (default false)}.

```json
{
  "object_set": {
    "search_around": {
      "object_set": { "filter": { "object_set": { "base": "Award" }, "where": { "and": [
        { "gte": { "property": "date", "value": "2026-09-01" } },
        { "eq":  { "property": "source", "value": "COCT_RFQ" } },
        { "gte": { "property": "value_amount", "value": "675000.00" } },
        { "lt":  { "property": "value_amount", "value": "750000.00" } } ] } } },
      "link": "awarded_to"
    }
  },
  "select": ["name_raw", "registration_number", "organisation_id", "review_status"],
  "order_by": [{ "property": "name_raw", "direction": "ASC" }],
  "aggregations": { "by_party_kind": { "terms": { "property": "party_kind", "size": 10 } } },
  "page_size": 100
}
```

**200:**

```json
{
  "object_type": "Supplier",
  "data": [ { "object_type": "Supplier", "pk": "coct:7Q2K…", "rid": "ri.objects.demo.object.Supplier.9a1b…", "version": "2",
              "title": "Lwazi Example Traders CC", "properties": { "name_raw": "Lwazi Example Traders CC",
              "registration_number": "2009/004512/99", "organisation_id": "01926e5c-4a5b-7c6d-8e7f-9a0b1c2d3e4f",
              "review_status": "NONE" }, "security": { "classification": "PUBLIC", "markings": [], "releasability": [],
              "purpose_tags": ["PROCUREMENT_INTEGRITY", "DUE_DILIGENCE"] }, "updated_at": "2026-09-24T03:12:44.120Z" } ],
  "aggregations": { "by_party_kind": { "buckets": [ { "key": "COMPANY", "count": "41" }, { "key": "CLOSE_CORPORATION", "count": "7" } ] } },
  "next_page_token": "b3BhcXVl…",
  "total_estimate": "48",
  "watermark": "wm1_0000000001f4a2_0000000000016018"
}
```

Errors: `VALIDATION_FAILED`, `PURPOSE_REQUIRED`, `CASE_REQUIRED`, `POLICY_DENIED`, `JUSTIFICATION_REQUIRED`, `NOT_FOUND` (unknown or invisible type or link), `LIMIT_EXCEEDED`, `WATERMARK_PENDING`, `RATE_LIMITED`. Audit: `objects.query` (query hash, row count, marking IDs).

#### OBJ-02 · `GET /api/v1/objects/{objectType}/{pk}` — P7
- **Path:** `pk` is URL-encoded.
- **Query:** `select` (comma list), `include` (`security`, `provenance`, `links_summary`), `reveal` (comma list of `cipher_text` properties; needs `X-Vigil-Justification` with checkpoint `REVEAL`).
- **Headers:** `X-Vigil-Purpose`, `X-Vigil-Watermark`.
- **200:** the object representation (§A.3), with `ETag: "v<version>"`.
- `links_summary` adds `{"links": [{"link": "awards", "count": "3"}]}`, counting only visible links.
- Errors: `NOT_FOUND` (missing or invisible), `JUSTIFICATION_REQUIRED`, `POLICY_DENIED`, `WATERMARK_PENDING`.

#### OBJ-03 · `GET /api/v1/objects/{objectType}/{pk}/links/{linkType}` — P7
- `linkType` is the link's API name *from this object's side* (`awards` from `Supplier`; `awarded_to` from `Award`).
- **Query:** `select`, `include=link_metadata`, `page_size`, `page_token`. For filtering, use OBJ-01 with a `search_around`.

```json
{
  "data": [
    { "object": { "object_type": "Award", "pk": "ocds-xxxxxx-2026-000123/award-1", "rid": "ri.objects.demo.object.Award.6f1c…", "version": "1",
                  "title": "Security services, depot 7", "properties": { "value_amount": "742500.00", "currency": "ZAR", "date": "2026-09-18" } },
      "link": { "link_type": "awarded_to", "cardinality": "MANY_TO_MANY", "via": null,
                "provenance": { "source_id": "coct-awards", "dataset_txn_id": "ri.forge.demo.transaction.01926e5d…", "fetched_at": "2026-09-20T02:00:03.000Z" },
                "security": { "classification": "PUBLIC", "markings": [] } } }
  ],
  "next_page_token": null,
  "total_estimate": "1"
}
```

For object-backed links, `via` holds the intermediary object (for example the `Directorship`). Errors: `NOT_FOUND`, `POLICY_DENIED`, `LIMIT_EXCEEDED`.

### B.3 Actions (P8) · `vigil.actions.v1.ActionService`

#### ACT-01 · `POST /api/v1/actions/{actionType}/validate` — P8
Stateless: no run is stored, no webhook is called, nothing is edited. Headers: `X-Vigil-Purpose`, `X-Vigil-Case-Id`.

```json
{ "parameters": { "supplier": { "object_type": "Supplier", "pk": "coct:7Q2K…" }, "indicator": { "object_type": "RiskIndicator", "pk": "rin_4XK2…" },
                  "summary": "Too short", "notify_cfo": false } }
```

**200:**

```json
{
  "valid": false,
  "parameters": [ { "name": "summary", "valid": false, "reason": "MIN_LENGTH", "message": "At least 50 characters." } ],
  "submission_criteria": [ { "name": "indicator_matches_supplier", "passed": true },
                           { "name": "not_already_referred", "passed": false, "message": "This supplier already has an open referral." } ],
  "requires_approval": true,
  "approval": { "reason": "REQUIRES_HUMAN_APPROVAL", "approver_role": "integrity-supervisor", "required_approvals": 1, "separation_of_duties": true },
  "justification_required": true,
  "would_touch": { "objects": 3, "object_types": ["Case", "WatchlistEntry", "Supplier"] }
}
```

Errors: `NOT_FOUND`, `PURPOSE_REQUIRED`, `POLICY_DENIED`, `LIMIT_EXCEEDED`.

#### ACT-02 · `POST /api/v1/actions/{actionType}/apply` — P8
- **Headers:** `Idempotency-Key` (**required**), `X-Vigil-Purpose` (in `allowed_purposes`), `X-Vigil-Case-Id` (required for case-scoped actions and case-predicated purposes), `X-Vigil-Justification` (when `justification_required`).
- **Body:**
  - `parameters` (required);
  - `options.staging`: `ALLOW` (default; stage when approval is needed) or `FORBID` (fail with `APPROVAL_REQUIRED` instead);
  - `options.return_edits` (default true);
  - `client_context.submitted_via` (COCKPIT, FIELD, API, AUTOMATION). `AGENT` is derived from the token's `act` claim and can never be claimed.

```json
{
  "parameters": { "supplier": { "object_type": "Supplier", "pk": "coct:7Q2K…" }, "indicator": { "object_type": "RiskIndicator", "pk": "rin_4XK2…" },
                  "summary": "Award A-1 is within 1% of the R750,000 RFQ threshold, made two days after close by limited bidding …",
                  "notify_cfo": true },
  "options": { "staging": "ALLOW", "return_edits": true },
  "client_context": { "submitted_via": "COCKPIT" }
}
```

**200 APPLIED** (for an action without approval, for example `record-review-status`):

```json
{
  "action_run_id": "01926e61-7b1d-7e2f-8a3b-4c5d6e7f8091",
  "action_type": "record-review-status",
  "status": "APPLIED",
  "edits": { "created": 0, "modified": 1, "deleted": 0, "links_added": 0, "links_removed": 0,
             "objects": [ { "object_type": "Supplier", "pk": "coct:7Q2K…", "op": "MODIFY", "changed_properties": ["review_status"] } ],
             "truncated": false },
  "side_effects": [],
  "watermark": "wm1_0000000001f4a3_0000000000016019",
  "applied_at": "2026-09-25T10:15:31.402Z"
}
```

**202 STAGED_FOR_APPROVAL:**

```json
{
  "action_run_id": "01926e61-8c2e-7f3a-9b4c-5d6e7f809102",
  "action_type": "refer-supplier",
  "status": "STAGED_FOR_APPROVAL",
  "approval": { "approval_request_id": "01926e61-8c2f-7a4b-8c5d-6e7f8091a2b3", "reason": "REQUIRES_HUMAN_APPROVAL",
                "approver_role": "integrity-supervisor", "required_approvals": 1, "expires_at": "2026-10-02T10:15:31.402Z" },
  "edits": null,
  "watermark": null
}
```

**422 REJECTED** (standard envelope; the run is stored with status `REJECTED`):

```json
{ "error": "VALIDATION_FAILED", "message": "The action was rejected by its submission criteria.", "status_code": 422,
  "timestamp": "2026-09-25T10:15:31.120Z", "request_id": "01926e61-9d3f-7b5c-8d6e-7f8091a2b3c4",
  "details": [ { "type": "CRITERION", "reason": "not_already_referred", "message": "This supplier already has an open referral." },
               { "type": "ACTION_RUN", "metadata": { "action_run_id": "01926e61-9d40-7c6d-9e7f-8091a2b3c4d5", "status": "REJECTED" } } ] }
```

Other errors:
- `APPROVAL_REQUIRED` (with `staging: FORBID`);
- `WRITEBACK_FAILED` (details `{"type": "RETRY", "metadata": {"http_status": 503, "duration_ms": 10000}}`; nothing committed);
- `CONFLICT` (a version conflict after one re-validation, or idempotency mismatch);
- `INTERNAL` with details `{"type": "ACTION_RUN", "metadata": {"status": "FAILED", "error_code": "RECONCILIATION_REQUIRED"}}` when the commit fails *after* a successful webhook. Compensation or human reconciliation follows (Blueprint §1.7, step 6);
- `LIMIT_EXCEEDED`, `DEADLINE_EXCEEDED`, `POLICY_DENIED`, `JUSTIFICATION_REQUIRED`, `CASE_REQUIRED`, `NOT_FOUND`.

Emits `vigil.ontology.edits.applied.v1` on commit, or `vigil.actions.run.staged.v1` when staged.

#### ACT-03 · `GET /api/v1/action-runs/{actionRunId}` — P8
Visible to the submitter, the approvers, and users who can see every touched object; everyone else gets `NOT_FOUND`. Parameters are redacted per §A.8.

```json
{
  "action_run_id": "01926e61-8c2e-7f3a-9b4c-5d6e7f809102", "action_type": "refer-supplier", "action_type_version": 2,
  "status": "APPLIED", "submitted_by": "user:3f2a…", "submitted_via": "AGENT",
  "agent": { "session_id": "ms_01926e5c7b1d7e2f", "model_id": "gpt-oss-120b" },
  "purpose": "PROCUREMENT_INTEGRITY", "case_id": null,
  "parameters": { "supplier": { "object_type": "Supplier", "pk": "coct:7Q2K…" }, "summary": "Award A-1 is within 1% …", "notify_cfo": true },
  "approval": { "approval_request_id": "01926e61-8c2f-7a4b-8c5d-6e7f8091a2b3", "status": "APPROVED",
                "decisions": [ { "approver": "user:9b7c…", "decision": "APPROVE", "decided_at": "2026-09-25T11:02:09.551Z", "justification_present": true } ] },
  "webhook": null,
  "edits": { "created": 2, "modified": 1, "deleted": 0, "links_added": 0, "links_removed": 0, "truncated": false },
  "edit_seq_range": ["90211", "90213"],
  "side_effects": [ { "index": 0, "kind": "NOTIFICATION", "status": "SUCCEEDED", "attempts": 1 } ],
  "watermark": "wm1_0000000001f4b0_0000000000016022",
  "submitted_at": "2026-09-25T10:15:31.402Z", "decided_at": "2026-09-25T11:02:09.551Z", "applied_at": "2026-09-25T11:02:10.004Z"
}
```

#### ACT-04 · `POST /api/v1/approvals/{approvalRequestId}/decision` — P8
- **Headers:** `Idempotency-Key`, `X-Vigil-Purpose`, `X-Vigil-Case-Id` where required.
- **Body:** `decision` (`APPROVE` or `REJECT`) and `justification` (20–2,000 characters).
- **Validation:**
  - the caller holds the approver role (OpenFGA) and passes Warden on every touched object;
  - `user:` principals only;
  - separation of duties;
  - the request is `PENDING` and not expired.
- On the n-th approval, the engine re-runs authorisation, validation and criteria against current state, then continues with webhook → commit → side effects (Blueprint §1.7).

```json
{ "decision": "APPROVE", "justification": "Checked indicator components 1–5 against the cited source records; referral is warranted." }
```

**200:**

```json
{ "approval_request_id": "01926e61-8c2f-7a4b-8c5d-6e7f8091a2b3", "status": "APPROVED",
  "action_run": { "action_run_id": "01926e61-8c2e-7f3a-9b4c-5d6e7f809102", "status": "APPLIED", "error_code": null,
                  "watermark": "wm1_0000000001f4b0_0000000000016022" } }
```

If re-validation fails, the approval stands and `action_run.status` is `REJECTED` with `error_code: "VALIDATION_FAILED"`. Errors:
- `POLICY_DENIED` (not an approver; `details.reason = "SEPARATION_OF_DUTIES"` for self-approval);
- `CONFLICT` (already decided);
- `EXPIRED`;
- `NOT_FOUND`.

#### ACT-05 · `GET /api/v1/approvals` — P8
Query: `status` (default `PENDING`), `assigned` (`me` or a role), `action_type`, `page_size`, `page_token`. Returns approval requests the caller may decide, each with its action-run summary (as in ACT-03, redacted).

#### ACT-06 · Outbound writeback webhook contract (Vigil → system of record) — P8
- Request: `POST <registered target URL>` over mTLS, with headers `X-Vigil-Action-Run-Id`, `X-Vigil-Idempotency-Key` and `X-Vigil-Signature` (HMAC-SHA-256 of the body under the target's registered secret).
- Body: `{"action_type", "action_run_id", "parameters" (minimised to the target's declared fields), "requested_by" (pseudonymous principal)}`.
- The target answers 2xx within **10 s**, optionally with `{"outputs": {…}}` for later rules. Anything else means `WRITEBACK_FAILED`, and nothing commits. Targets MUST deduplicate on `X-Vigil-Action-Run-Id`, which stays stable across retries of one Idempotency-Key (the run is persisted before the webhook; ARCHITECTURE §6.3 step 0).
- SAP PM specifics are appended by P24.

### B.4 Functions (P9) · `vigil.functions.v1.FunctionService`

#### FN-01 · `POST /api/v1/functions/{functionApiName}/invoke` — P9
Headers: `X-Vigil-Purpose`. Body: `{"version": 3, "inputs": {…}}` (typed per `input_schema`). The function runs as the caller (Blueprint §1.8).

```json
{ "function": "threshold-proximity", "version": 3, "kind": "QUERY",
  "output": { "sub_score": 0.9, "threshold_zar": "750000.00", "register_entry": "coct-rfq@2023-12-14" },
  "execution": { "duration_ms": 184, "memory_peak_mb": 61, "objects_loaded": 12, "search_arounds": 1 } }
```

An `EDITS` function returns `proposed_edits` as a **dry run**. It commits only through its function-backed action. Errors: `VALIDATION_FAILED`, `POLICY_DENIED`, `LIMIT_EXCEEDED` (objects, search-arounds, memory), `DEADLINE_EXCEEDED` (60 s), `UNAVAILABLE`.

### B.5 Datasets (P4) · `vigil.forge.datasets.v1.DatasetService`

#### DS-01 · `POST /api/v1/datasets/{datasetRid}/transactions` — P4
For service principals (connectors, transforms) holding the dataset-writer role. Headers:
- `Idempotency-Key` = SHA-256(connector id ‖ cursor from ‖ cursor to) (Blueprint §9.2);
- `X-Vigil-Purpose: DATA_INTEGRATION` (a registered system purpose).

```json
{
  "branch": "main",
  "type": "APPEND",
  "source_run": { "connector_id": "ocds-etenders", "connector_version": "1.2.0", "run_id": "01926e62-0a1b-7c2d-8e3f-4a5b6c7d8e9f",
                  "cursor_from": "2026-09-24T00:00:00Z", "cursor_to": "2026-09-25T00:00:00Z" },
  "declared_inputs": [],
  "provenance": { "terms_ref": "terms/ocds-etenders@1", "lawful_basis_ref": "lb/ocds-etenders@1", "responsible_party": "NT_OCPO", "data_origin": "REAL" }
}
```

- **Validation:** `type` ∈ SNAPSHOT, APPEND, UPDATE, DELETE. Complete provenance is mandatory (Blueprint §9.5). At most one OPEN `SNAPSHOT` transaction per branch. `declared_inputs` feeds Warden `propagate()` for derived datasets.
- **201:**

```json
{ "transaction_rid": "ri.forge.demo.transaction.01926e62-1b2c-7d3e-9f4a-5b6c7d8e9fa0", "dataset_rid": "ri.forge.demo.dataset.01926e5c…",
  "branch": "main", "type": "APPEND", "status": "OPEN", "opened_at": "2026-09-25T02:00:00.010Z", "expires_at": "2026-09-26T02:00:00.010Z",
  "upload": { "protocol": "S3", "endpoint": "https://s3.za-gp-1.vigil.internal", "bucket": "vigil-za-gp-1-lake",
              "prefix": "demo/_staging/01926e62-1b2c-7d3e-9f4a-5b6c7d8e9fa0/",
              "credentials": { "access_key_id": "…", "secret_access_key": "…", "session_token": "…", "expires_at": "2026-09-25T02:15:00.010Z" } } }
```

The credentials are scoped to the staging prefix, live for 15 minutes, and are never logged. Errors: `VALIDATION_FAILED`, `CONFLICT`, `POLICY_DENIED`, `NOT_FOUND`.

#### DS-02 · `POST /api/v1/datasets/{datasetRid}/transactions/{transactionRid}/commit` — P4
Body: `{"files": [{"path": "…/part-000.parquet", "sha256": "…", "row_count": 18234}], "schema_version": 4, "quality_contract": "dq/ocds-releases@3"}`.

On commit the server:
1. verifies the file hashes;
2. runs the `BLOCK` checks;
3. calls Warden `propagate()`;
4. makes the Iceberg commit;
5. emits OpenLineage and `vigil.forge.dataset.transaction.committed.v1`.

**200:**

```json
{ "transaction_rid": "ri.forge.demo.transaction.01926e62-1b2c-7d3e-9f4a-5b6c7d8e9fa0", "status": "COMMITTED",
  "snapshot_id": "7309245893202912345", "committed_at": "2026-09-25T02:03:41.228Z", "rows": { "added": "18234", "deleted": "0" },
  "label": { "markings": [], "classification": "PUBLIC", "releasability": [], "purpose_tags": ["PROCUREMENT_INTEGRITY", "DUE_DILIGENCE"] },
  "lineage_run_id": "01926e62-2c3d-7e4f-8a5b-6c7d8e9fa0b1" }
```

Errors: `QUALITY_CHECK_FAILED` (the transaction becomes `ABORTED`; details list the failed checks), `VALIDATION_FAILED` (schema or hash mismatch), `CONFLICT` (not OPEN, or a branch commit conflict after retries), `EXPIRED`. Replaying with the same key returns the committed result.

#### DS-03 · `POST /api/v1/datasets/{datasetRid}/transactions/{transactionRid}/abort` — P4
Body: `{"reason": "Source returned a partial page"}`. **200:** `{"transaction_rid": "…", "status": "ABORTED"}`. Errors: `CONFLICT` (already committed), `NOT_FOUND`.

#### DS-04 · `GET /api/v1/datasets/{datasetRid}/branches` — P4

```json
{ "data": [ { "name": "main", "head_transaction_rid": "ri.forge.demo.transaction.01926e62…", "head_snapshot_id": "7309245893202912345",
              "is_default": true, "created_at": "2026-09-01T08:00:00.000Z" },
            { "name": "vb-integrity-v2", "head_transaction_rid": "ri.forge.demo.transaction.01926e63…", "head_snapshot_id": "7309245893202999001",
              "is_default": false, "created_at": "2026-09-20T09:30:00.000Z" } ],
  "next_page_token": null, "total_estimate": "2" }
```

### B.6 Warden (P3) · `vigil.warden.v1.PolicyService`

#### POL-01 · `POST /api/v1/policy/evaluate` — P3 (administration and debugging)
- Roles: `warden-admin` or `security-auditor`. It **never returns data**.
- Evaluating a real principal needs `X-Vigil-Justification`, and the call itself is audited.
- The subject is either `{"principal": "user:…"}` or `{"synthetic": {…}}`; the resource is either `{"rid": "…"}` or `{"label": {…}}`.

```json
{ "subject": { "synthetic": { "markings": ["PII"], "clearance": 1, "releasability": ["REL-ZA"] } },
  "purpose": "PROCUREMENT_INTEGRITY", "case_id": null, "operation": "READ",
  "resource": { "label": { "markings": ["PII"], "classification": 2, "releasability": [], "purpose_tags": ["PROCUREMENT_INTEGRITY"] } } }
```

**200:**

```json
{ "decision": "DENY", "reasons": [ { "rule": "W1", "component": "CLASSIFICATION", "detail": "clearance 1 < classification 2" } ],
  "redactions": [], "obligations": ["AUDIT_READ"], "policy_version": "warden-policy@1.0.0+sha256:9c1d…", "evaluated_at": "2026-09-25T09:00:00.000Z" }
```

#### POL-02 · `POST /api/v1/policy/justifications` — P3 (justification capture, W7)
Headers: `Idempotency-Key`, `X-Vigil-Purpose`, `X-Vigil-Case-Id`. `checkpoint` ∈ `SPECIAL_CATEGORY_ACCESS`, `PERSON_LINKAGE`, `EXPORT`, `DECLASSIFICATION`, `REVEAL`.

```json
{ "checkpoint": "SPECIAL_CATEGORY_ACCESS", "text": "Reviewing evidence items for case 42 to prepare the referral bundle for the supervisor.",
  "scope": { "object_types": ["EvidenceItem"], "rids": [] } }
```

**201:**

```json
{ "justification_id": "01926e63-3d4e-7f5a-9b6c-7d8e9fa0b1c2", "checkpoint": "SPECIAL_CATEGORY_ACCESS", "expires_at": "2026-09-25T17:00:00.000Z",
  "bound_to": { "principal": "user:3f2a…", "purpose": "CASE_INVESTIGATION", "case_id": "case-2026-000042" } }
```

Errors: `VALIDATION_FAILED` (length or checkpoint), `PURPOSE_REQUIRED`, `CASE_REQUIRED`.

#### POL-03 · `GET /api/v1/policy/me` — P3
Returns the caller's effective attributes: *held* markings only, clearance, releasability, the purposes they may declare, and roles. For example `{"principal": "user:3f2a…", "tenant": "demo", "markings": ["PII"], "clearance": 2, "releasability": ["REL-ZA"], "purposes": ["PROCUREMENT_INTEGRITY"], "roles": ["integrity-analyst"]}`.

### B.7 Audit (P3) · `vigil.warden.v1.AuditService`

#### AUD-01 · `GET /api/v1/audit/events` — P3
- Role `audit-reader`; the purpose must be the registered `AUDIT_OVERSIGHT`. Audit reads are themselves audited.
- **Query:** `from` and `to` (required; at most 31 days apart), `actor`, `resource_rid`, `event_type` (repeatable), `purpose`, `case_id`, `decision`, `page_size`, `page_token`.

```json
{ "data": [ { "seq": "1048576", "event_id": "01926e64-4e5f-7a6b-8c7d-8e9fa0b1c2d3", "event_type": "objects.query",
              "occurred_at": "2026-09-25T10:14:02.117Z", "recorded_at": "2026-09-25T10:14:02.301Z", "actor": "user:3f2a…",
              "on_behalf_of": null, "purpose": "PROCUREMENT_INTEGRITY", "case_id": null, "resource_rid": "ri.ontology.demo.object-type.01926e5c…",
              "marking_ids": [], "decision": "ALLOW", "details": { "query_hash": "c4f1…", "row_count": 48 },
              "chain": { "prev_hash": "q1Zb…", "hash": "7yTn…", "signature": "Mz0x…", "signing_key_id": "audit-k1" } } ],
  "next_page_token": "b3BhcXVl…", "total_estimate": null }
```

### B.8 Mind (P14, P15) · `vigil.mind.agents.v1.AgentService`

#### MIND-01 · `POST /api/v1/mind/sessions` — P15
Headers: `Idempotency-Key`, `X-Vigil-Purpose`, `X-Vigil-Case-Id`. Body: `{"application": "integrity-analyst-assistant"}`. The application's tool allow-list is registered at P15.

```json
{ "session_id": "ms_01926e5c7b1d7e2f", "application": "integrity-analyst-assistant", "expires_at": "2026-09-25T18:00:00.000Z",
  "tools": [ { "name": "query_object_set", "kind": "OBJECT_SET_QUERY" },
             { "name": "function:threshold-proximity", "kind": "FUNCTION" },
             { "name": "action:refer-supplier", "kind": "ACTION", "mode": "STAGE_ONLY" } ],
  "route": { "tier": "T2", "model_id": "qwen3.8-27b", "endpoint_enclave": "za-gp-1/confidential" },
  "no_ai_fallback_available": true }
```

#### MIND-02 · `POST /api/v1/mind/sessions/{sessionId}/turns` — P15 (routing by P14)
- **Headers:** `Idempotency-Key`, `X-Vigil-Purpose` (must equal the session's), and `Accept: application/json` or `text/event-stream`.
- **Body:** `message`, `attachments[]` (RIDs), `options` {`max_tool_calls` (≤ 20), `task`}.
- Every tool call runs as the user. Writes are staged unless the action is `agent_executable` and needs no approval. Attachments and retrieved content are wrapped as `<untrusted_content>` (Blueprint §9.6).

```json
{ "message": "Which awards this month went to suppliers sharing a director with a restricted supplier? Draft a referral for the strongest.",
  "attachments": [], "options": { "max_tool_calls": 12, "task": "AGENT" } }
```

**200** (with `text/event-stream`: events `token`, `tool_call`, `staged_action`, then `done`, whose payload is this JSON):

```json
{
  "turn_id": "mt_01926e65a1b2c3d4",
  "answer": { "text": "Two awards match the pattern [1][2]. The strongest is A-1: …",
              "citations": [ { "marker": "[1]", "rid": "ri.objects.demo.object.Award.6f1c…", "properties": ["value_amount", "date"],
                               "dataset_txn_id": "ri.forge.demo.transaction.01926e5d…" } ],
              "label": "AI-generated summary of risk indicators — not a finding" },
  "tool_calls": [
    { "call_id": "tc_1", "tool": "query_object_set", "arguments": { "object_set": { "base": "Award" } },
      "policy": { "decision": "ALLOW", "redactions": 1 }, "result": { "object_count": 2 }, "duration_ms": 212 },
    { "call_id": "tc_2", "tool": "action:refer-supplier", "arguments": { "supplier": { "object_type": "Supplier", "pk": "coct:7Q2K…" } },
      "policy": { "decision": "ALLOW", "redactions": 0 }, "result": { "action_run_id": "01926e61-8c2e-7f3a-9b4c-5d6e7f809102", "status": "STAGED_FOR_APPROVAL" },
      "duration_ms": 95 } ],
  "staged_actions": [ { "action_run_id": "01926e61-8c2e-7f3a-9b4c-5d6e7f809102", "action_type": "refer-supplier",
                        "status": "STAGED_FOR_APPROVAL", "approval_request_id": "01926e61-8c2f-7a4b-8c5d-6e7f8091a2b3" } ],
  "model": { "model_id": "gpt-oss-120b", "tier": "T3", "endpoint_enclave": "za-gp-1/confidential", "weights_sha256": "a41f…" },
  "usage": { "input_tokens": 18342, "output_tokens": 912, "gpu_ms": 7410 },
  "safety": { "untrusted_segments": 3, "injection_signals": [], "refusals": [] },
  "no_ai_fallback": false
}
```

Errors:
- `POLICY_DENIED` (`details.reason = "NO_CLEARED_MODEL"`);
- `UNAVAILABLE` (model serving down: the body also carries `"no_ai_fallback": true` and the equivalent Cockpit route);
- `LIMIT_EXCEEDED` (tool calls or context);
- `EXPIRED` (session), `RATE_LIMITED`.

#### MIND-03 · `GET /api/v1/mind/sessions/{sessionId}/turns/{turnId}/trace` — P15
The full trace for audit and evaluation: model calls (model id, tier, prompt SHA-256, token counts, latency), tool calls (arguments redacted to the reader's view), policy decisions and staged actions. Visible to the session owner and to `audit-reader`.

### B.9 Entity resolution (P16) · `vigil.er.v1.ResolutionService`

#### ER-01 · `GET /api/v1/er/review-queue` — P16
- **Query:** `entity_class` (ORGANISATION or PERSON), `status` (default PENDING_REVIEW), `min_probability`, `max_probability`, `page_size`, `page_token`.
- **Headers:** `X-Vigil-Purpose`; for PERSON also `X-Vigil-Justification` (checkpoint `PERSON_LINKAGE`).
- `PERSON_CROSS_PARTY` items exist only under an active s57 authorisation, because they are never computed otherwise.

```json
{ "data": [ {
    "resolution_link": { "object_type": "ResolutionLink", "pk": "3b9f0c…", "rid": "ri.objects.demo.object.ResolutionLink.1d2e…" },
    "member": { "object_type": "Supplier", "pk": "ocds:ocds-xxxxxx-2026-000123:party-7", "title": "Zenzele Example Civils (Pty) Ltd" },
    "candidate_entity": { "object_type": "Organisation", "pk": "01926e5c-5b6c-7d7e-8f80-91a2b3c4d5e6", "title": "ZENZELE EXAMPLE CIVILS", "member_count": 4 },
    "match_probability": 0.9203, "match_weight": 3.530,
    "comparisons": [ { "comparison": "organisation_name", "level": "EXACT_NORMALISED_KEY", "bayes_factor": 7000.0 },
                     { "comparison": "location", "level": "SAME_MUNICIPALITY", "bayes_factor": 27.5 },
                     { "comparison": "shared_identifiers", "level": "NONE_SHARED", "bayes_factor": 0.6001 },
                     { "comparison": "cipc_registration", "level": "NULL", "bayes_factor": 1.0 } ],
    "linkage_class": "ORG_CROSS_PARTY", "model_version": "er-org@1.0.0", "created_at": "2026-09-25T04:10:00.000Z" } ],
  "next_page_token": null, "total_estimate": "37" }
```

#### ER-02 · `POST /api/v1/er/review-queue/{resolutionLinkPk}/decision` — P16
- **Headers:** `Idempotency-Key`, `X-Vigil-Purpose` (plus a justification for PERSON).
- **Body:** `decision` (CONFIRM, REJECT or DEFER), `note`, `expected_status`.
- Implemented as the `confirm-resolution-link` or `reject-resolution-link` action, so it becomes a hard constraint on every later run.

```json
{ "decision": "CONFIRM", "note": "Same director set on the CIPC extract; addresses match.", "expected_status": "PENDING_REVIEW" }
```

**200:** `{"status": "CONFIRMED", "action_run_id": "01926e66-…", "effective": "NEXT_ER_RUN", "watermark": "wm1_…"}`. Errors: `CONFLICT` (already decided, or `expected_status` mismatch), `LINKAGE_LOCKED`, `POLICY_DENIED`, `NOT_FOUND`.

#### ER-03 · `POST /api/v1/er/jobs` — P16
Headers: `Idempotency-Key`, `X-Vigil-Purpose`, and for PERSON a justification.

```json
{ "entity_class": "PERSON", "inputs": ["ri.forge.demo.dataset.01926e5c-cipc-directorships", "ri.forge.demo.dataset.01926e5c-restrictions"],
  "mode": "INCREMENTAL", "model_version": "er-person@1.0.0" }
```

- **202:** `{"job_id": "erj_01926e67…", "status": "QUEUED", "linkage_classes": ["PERSON_SAME_PARTY"]}`.
- **423:** `LINKAGE_LOCKED` (the error example in §A.6) when the inputs span responsible parties for persons and no active `S57_1A_LINKAGE` authorisation covers the scope. The job **does not run**; the refusal is audited as `er.linkage.locked`.

### B.10 Cases and evidence (P19) · `vigil.cases.v1.CaseService`

#### CASE-01 · `POST /api/v1/cases/{caseId}/evidence-pack/exports` — P19
- **Headers:** `Idempotency-Key`, `X-Vigil-Purpose`, `X-Vigil-Case-Id` (= `caseId`), and `X-Vigil-Justification` (checkpoint `EXPORT`).
- Runs the action `export-evidence-pack`: `OPERATIONAL` for internal recipients, and **`LEGAL_EFFECT`, with approval, for external recipients**.
- Content outside the recipient's releasability is excluded (W5), and indicator pages carry "RISK INDICATOR — NOT A FINDING".

```json
{ "format": "ZIP", "include": { "all": false, "evidence_ids": ["ev_01926e68a1", "ev_01926e68a2"] },
  "recipient": { "organisation": "Example Provincial Treasury (fictional)", "releasability": ["REL-ZA"] },
  "redaction_profile": "EXTERNAL_RECIPIENT", "include_indicator_explanations": true }
```

**202:** `{"export_id": "ex_01926e69", "status": "STAGED_FOR_APPROVAL", "action_run_id": "01926e69-…"}`. The status is `PENDING` for internal recipients. Errors: `JUSTIFICATION_REQUIRED`, `CASE_REQUIRED`, `POLICY_DENIED`, `LIMIT_EXCEEDED`.

#### CASE-02 · `GET /api/v1/cases/{caseId}/evidence-pack/exports/{exportId}` — P19

```json
{ "export_id": "ex_01926e69", "status": "READY",
  "manifest": { "files": [ { "name": "summary.pdf", "sha256": "e3b0…" }, { "name": "evidence/ev_01926e68a1.json", "sha256": "9f86…" },
                           { "name": "lineage.json", "sha256": "2c26…" } ],
                "manifest_sha256": "b94d…", "signature": { "alg": "Ed25519", "key_id": "evidence-k1", "value": "MEUCIQ…" } },
  "download": { "url": "https://za-gp-1.vigil.example/api/v1/downloads/ex_01926e69?t=…", "expires_at": "2026-09-25T12:15:00.000Z", "single_use": true },
  "labels": ["RISK INDICATOR — NOT A FINDING"] }
```

The pack's hashes and signature verify independently of Vigil (P19 "Done when"). Errors: `NOT_FOUND`, `EXPIRED`.

### B.11 Endpoints reserved for later phases (to be specified by the owner — append below)

| ID | Endpoint | Owner |
|---|---|---|
| OBJ-04 | `GET /api/v1/objects/{objectType}/{pk}/history` (edit and source history, decision lineage) | P7 |
| TRV-01 | `POST /api/v1/traversal/paths` (k-hop and path queries over Arrow snapshots) | P7 |
| CON-* | `/api/v1/connectors` (register, configure, run, terms register) | P11 |
| PIPE-* | `/api/v1/pipelines`, `/api/v1/quality/contracts` | P12 |
| DOC-* | `/api/v1/docai/extractions` (field provenance, review queue) | P17 |
| GEO-* | `/api/v1/geo/tiles/{layer}/{z}/{x}/{y}`, offline bundle manifests | P18 |
| CASE-* | `/api/v1/cases` (open, attach, refer, close as actions; custody log) | P19 |
| RELAY-* | Hub and spoke plan API (`vigil.relay.v1`, gRPC) | P20, P21 |
| SAP-* | SAP PM writeback target profile (extends ACT-06) | P24 |
| EDGE-* | NATS subjects `vigil.edge.<site>.*` (not REST) | P25 |
| FIELD-* | `/api/v1/field/sync`, queued signed action submission | P27 |
| COMMS-* | `/api/v1/comms/alerts`, delivery receipts, acknowledgements | P28 |
| EO-* | `/api/v1/eo/scenes`, `/api/v1/eo/detections` | P29 |
| SIM-* | `/api/v1/scenarios` (create, run, compare, promote) | P30 |
| APP-* | `/api/v1/apps`, `/api/v1/sdks` | P31 |
| PEER-* | `/api/v1/peering/shares` | P38 |
| AUD-02 | `GET /api/v1/audit/chain/verify` | P40 |
| KEY-* | `/api/v1/keys` (KEK registration, crypto-shredding) | P42 |

<!-- Append new Part B entries above this line. -->

---

## Part C — Event catalogue (CloudEvents 1.0)

### C.1 Envelope

| Attribute | Value |
|---|---|
| `specversion` | `1.0` |
| `id` | UUIDv7 (the outbox `event_id`) |
| `source` | `//vigil/<pod>/<tenant>/<service>`, for example `//vigil/za-gp-1/demo/core-actions` |
| `type` | `vigil.<domain>.<entity>.<verb>.v1` |
| `subject` | The resource RID |
| `time` | RFC 3339 UTC |
| `datacontenttype` | `application/protobuf` (Kafka binary mode); `application/json` only for debugging taps |
| `dataschema` | `urn:vigil:proto:<fully.qualified.Message>` |
| Extensions | `vigiltenant`, `vigilclass` (classification 0–4), `vigilmarkings` (comma-separated IDs), `vigilwatermark`, `partitionkey` (Partitioning extension), `traceparent` (Distributed Tracing extension) |

**Rules:**
- Payloads are **thin**: RIDs, PKs, counts and property *names*, never property values. Kafka therefore holds no personal information, and erasure never needs to touch it.
- **Topics** are `<tenant>.<domain>.<stream>.v1`, for example `demo.ontology.edits.v1` (the tenant-prefixed `ontology.edits.v1` stream of ARCHITECTURE §6.3). Retention is 7 days with no compaction; ACLs are per tenant and service.
- The envelope Protobuf lives in `proto/vigil/events/` (P2). Payload messages live in their domain packages.
- **AsyncAPI 3** channel documents live in `docs/asyncapi/` (P2); each owning phase appends its channels there, and they must match this catalogue.

### C.2 Events

| Type | Producer | Topic stream | Payload fields | Consumers |
|---|---|---|---|---|
| `vigil.forge.dataset.transaction.committed.v1` | forge-datasets (P4) | `forge.datasets` | `dataset_rid`, `transaction_rid`, `branch`, `type`, `snapshot_id`, `rows_added`, `rows_deleted`, `schema_version`, `label` {markings, classification, releasability, purpose_tags}, `lineage_run_id`, `committed_by`, `committed_at` | indexer (P6), transforms (P12), lineage |
| `vigil.forge.dataset.transaction.aborted.v1` | forge-datasets (P4) | `forge.datasets` | `dataset_rid`, `transaction_rid`, `reason_code`, `failed_checks[]` | connectors, alerting |
| `vigil.ontology.type.published.v1` | OMS (P5) | `ontology.types` | `type_rid`, `type_kind`, `api_name`, `version`, `compat`, `branch`, `definition_sha256`, `published_by`, `published_at` | indexer (mappings), OSS caches, SDK generation (P31), Cockpit |
| `vigil.indexer.objects.indexed.v1` | indexer (P6) | `indexer.objects` | `object_type`, `branch`, `watermark`, `source_transactions[]`, `rows_upserted`, `rows_deleted`, `full_reindex`, `alias_swapped`, `indexed_at` | OSS (read-your-writes), Valkey invalidation, Mind |
| `vigil.ontology.edits.applied.v1` | Action engine (P8, through the outbox) | `ontology.edits` | `action_run_id`, `action_type`, `edit_seq_from`, `edit_seq_to`, `objects[]` {object_type, pk, rid, op, changed_properties[]}, `label` (union), `applied_by`, `on_behalf_of`, `purpose`, `applied_at` | indexer (OpenSearch), lineage, audit exporters, automations (P15) |
| `vigil.actions.run.staged.v1` | Action engine (P8) | `actions.runs` | `action_run_id`, `action_type`, `submitted_by`, `submitted_via`, `agent_session_id`, `approval_request_id`, `approver_role`, `expires_at` | Cockpit inbox, notifications (P28) |
| `vigil.actions.run.decided.v1` | Action engine (P8) | `actions.runs` | `action_run_id`, `approval_request_id`, `outcome` (APPROVED, REJECTED, EXPIRED), `decided_by[]`, `final_status` | Cockpit, Mind (session follow-up) |
| `vigil.actions.sideeffect.failed.v1` | Temporal workers (P8) | `actions.runs` | `action_run_id`, `effect_index`, `kind`, `attempts`, `error_code`, `compensated` | alerting, Cockpit |
| `vigil.audit.event.recorded.v1` | audit chainer (P3) | `audit.events` | `seq`, `event_id`, `event_type`, `actor_type`, `decision`, `resource_rid`, `hash` | SIEM export, anchoring (P40) |
| `vigil.warden.gate.changed.v1` | Warden admin (P3) | `warden.gates` | `gate`, `authorisation_id`, `status` (ACTIVE, SUSPENDED, EXPIRED, REVOKED), `valid_to` | ER (suspend links), OSS caches |
| `vigil.er.run.completed.v1` | ER (P16) | `er.runs` | `run_id`, `entity_class`, `model_version`, `links_auto`, `links_pending`, `clusters_changed`, `linkage_classes[]`, `gate_refusals` | indexer, Cockpit review queue |

Structured-mode example (debugging tap only):

```json
{ "specversion": "1.0", "id": "01926e61-9e41-7d6e-8f70-81a2b3c4d5e6", "source": "//vigil/za-gp-1/demo/core-actions",
  "type": "vigil.ontology.edits.applied.v1", "subject": "ri.actions.demo.action-run.01926e61-7b1d-7e2f-8a3b-4c5d6e7f8091",
  "time": "2026-09-25T10:15:31.402Z", "datacontenttype": "application/json",
  "dataschema": "urn:vigil:proto:vigil.actions.v1.EditsApplied", "vigiltenant": "demo", "vigilclass": 0, "vigilmarkings": "",
  "vigilwatermark": "wm1_0000000001f4a3_0000000000016019", "partitionkey": "ri.objects.demo.object.Supplier.9a1b…",
  "data": { "action_run_id": "01926e61-7b1d-7e2f-8a3b-4c5d6e7f8091", "action_type": "record-review-status",
            "edit_seq_from": "90210", "edit_seq_to": "90210",
            "objects": [ { "object_type": "Supplier", "pk": "coct:7Q2K…", "rid": "ri.objects.demo.object.Supplier.9a1b…", "op": "MODIFY",
                           "changed_properties": ["review_status"] } ],
            "label": { "markings": [], "classification": 0, "releasability": [], "purpose_tags": ["PROCUREMENT_INTEGRITY", "DUE_DILIGENCE"] },
            "applied_by": "user:3f2a…", "on_behalf_of": null, "purpose": "PROCUREMENT_INTEGRITY", "applied_at": "2026-09-25T10:15:31.402Z" } }
```

<!-- Append new Part C events above this line. -->

---

## Part D — Append log

> One row per change, oldest first. **Never edit a row.** This section is always last.

| Date | Phase | Engineer (AI) | Endpoints added/deprecated |
|---|---|---|---|
| 2026-09-25 | P0 | Kone Tshivhinda (Claude Opus 5.5) | Added Part A conventions (headers, pagination, filter DSL, error format and catalogue, rate limits, redaction, idempotency, watermarks, versioning); OMS-01–05; OBJ-01–03; ACT-01–06; FN-01; DS-01–04; POL-01–03; AUD-01; MIND-01–03; ER-01–03; CASE-01–02; reserved B.11 IDs; events C.2 (11 types). Deprecated: none |
