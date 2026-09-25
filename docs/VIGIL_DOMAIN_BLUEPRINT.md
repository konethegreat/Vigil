<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Frozen domain truth: ontology meta-model, packs, markings, entity resolution, risk indicators, corridor analytics, AI routing, ingestion and decision lineage (P0) -->

# Vigil — Domain Blueprint (the Domain Truth)

> **Status:** 🧊 FROZEN at P0 (v1.0, 2026-09-25). Changes only through an ADR in [`adr/`](adr/) approved by the lead architect (INSTRUCTIONS.md §4.1, PHASES Rule 10).
> **What this is:** the Vigil equivalent of the Project Setup framework's `AI_DUE_DILIGENCE_ARCHITECTURE_PLAN.md` (Layer 4, item 8). It fixes the domain logic that no phase may reinterpret: the ontology meta-model, the shared packs, the marking algebra, entity resolution, risk indicators, corridor analytics, AI routing, ingestion and decision lineage.
> **Read with:** [`ARCHITECTURE.md`](ARCHITECTURE.md) 🧊 (topology, flows §6, limits §6.3, scale §9) · [`DATA_MODEL.md`](DATA_MODEL.md) 🧊 (physical design) · [`API_DESIGN.md`](API_DESIGN.md) (contracts) · research [dossier](research/VIGIL_RESEARCH_DOSSIER.md) §2.3, §9, §11.
> **Normative words:** MUST, MUST NOT, SHOULD, MAY (RFC 2119). **v1** = the slice Phase Zero (P22/P23) needs. **Deepening (P<ID>)** = later work that MUST NOT be built early (INSTRUCTIONS.md §5.2). **Confirm at P<ID>** = a third-party fact, legal point or parameter the named phase verifies before relying on it (register in §11).
> **Examples:** every name, identifier and number in this document is fictional or synthetic. Nothing here is legal advice; counsel confirms every legal point (dossier §9).

## 0. Map of this document

| § | Topic | Built by | Consumed by |
|---|---|---|---|
| 1 | Ontology meta-model | P5 (OMS), P6, P7, P8, P9 | every phase |
| 2 | Core pack `packs/_core` | P5; `case/` P19; `scenario/` P30 | P22, P23 |
| 3 | Integrity pack (P22) and Corridor pack (P23) | P22, P23 | Phase Zero demos |
| 4 | Markings and classification | P3 (Warden) | every data path |
| 5 | Entity resolution | P16 | P22, P23 |
| 6 | Risk indicator scoring | P22 | P19 cases |
| 7 | Corridor analytics | P23 (v0), P29, P30 | P23 demo |
| 8 | Multi-model AI routing | P14, P15 | P17, P19, P22 |
| 9 | Ingestion pipelines | P4, P11, P12, P17 | P6 |
| 10 | Decision capture and lineage | P8, P3 (audit), P15 | P19, P36, P40 |
| 11 | Confirm-later register and change control | all | all |

---

## 1. Ontology meta-model

### 1.1 Semantic, kinetic and dynamic

Palantir's documentation defines the Ontology as "a digital twin of the organization, containing both the **semantic elements (objects, properties, links)** and **kinetic elements (actions, functions, dynamic security)**", and calls data elements "the nouns" and actions "the verbs" (research appendix 02 §1.1). Vigil adopts that vocabulary exactly:

| Layer | Meaning at Vigil | Vigil primitives |
|---|---|---|
| **Semantic** (the nouns) | The typed model of real assets, people, places, events and operations | Object types, properties and value types, link types, interfaces |
| **Kinetic** (the verbs) | Governed write paths that change state and write back to systems of record | Action types (parameters, rules, submission criteria, writeback webhook, side effects) and function types (logic that reads objects and proposes edits) |
| **Dynamic** (evaluated at run time) | Security, logic, models and simulation computed against live state on every interaction, plus decision capture | Warden evaluation on every read and write (§4), function execution, risk indicators (§6), corridor models and scenarios (§7), decision lineage (§10) |

- **"Kinetic" never means simulation.** Simulation, forecasting and optimisation are dynamic logic (P30). The Gemini brief's "Kinetic Simulation" label is wrong (dossier §3, claim 3).
- Palantir lists "dynamic security" among the kinetic elements. At Vigil security is *evaluated* dynamically and *enforced* on every kinetic path; both statements hold.
- No verbatim Palantir definition of a three-way split was found (appendix 02 §1.1, **[UNVERIFIED]**). The table above is Vigil's normative reading, frozen here.

### 1.2 Resource identifiers (RIDs)

- **Grammar:** `ri.<service>.<tenant>.<kind>.<locator>`. `service` ∈ {`ontology`, `objects`, `actions`, `functions`, `forge`, `warden`, `audit`, `mind`, `er`, `cases`}; `tenant` is the tenant slug or `platform`; `kind` is kebab-case; `locator` is a lowercase UUIDv7.
- **Objects** are the exception: `ri.objects.<tenant>.object.<ObjectType>.<uuid>`, where the UUID is **UUIDv5(object-type UUID, primary key)**. The same object therefore always has the same RID, and no registry is needed to dereference it (DATA_MODEL §4.3).
- RIDs never contain property values or personal information. Clients treat them as opaque except for the `<ObjectType>` segment.

### 1.3 Object types

| Field | Rule |
|---|---|
| `api_name` | PascalCase, unique per ontology, immutable after first publish |
| `primary_key` | Exactly one `string` or `long` property. Immutable per object. **MUST NOT contain personal information**: use a surrogate or a pseudonym (§4.6), because PKs appear in URLs, logs and caches |
| `title_property` | The display title |
| `datasources` | Zero or more datasets (branch-aware). Zero means an action-only type such as `Case` |
| `implements` | Interfaces (§1.6) |
| `security` | Default classification and markings, row-marking rules, purpose tags, column markings (§4) |
| `existence_policy` | `SOURCE_WINS` (default for source-backed types) or `EDIT_WINS` (§1.9) |
| `status` | `EXPERIMENTAL`, `ACTIVE` or `DEPRECATED` |

Each object type becomes one typed physical table, one OpenSearch index alias and one RLS policy set (A3; DATA_MODEL §4). There are no EAV tables on hot paths.

### 1.4 Property base types

| Base type | Array? | Physical form (Postgres) | Notes |
|---|---|---|---|
| `boolean`, `integer`, `long`, `double` | yes | native | — |
| `decimal` | yes | `numeric(38,s)` | Serialised as a string in APIs (no float drift for rand amounts) |
| `string` | yes | `text` | Optional `enum`, `max_length`, `regex`, value type |
| `date`, `timestamp` | yes | `date`, `timestamptz` | Timestamps are UTC, millisecond precision |
| `struct` | yes | `jsonb`, schema-validated | v1: scalar fields only, at most 32 fields, no nesting |
| `geopoint` | yes | `geometry(Point,4326)` + H3 cells | H3 resolutions 5, 7 and 9 plus an optional Plus Code (DATA_MODEL §8) |
| `geoshape` | yes | `geometry(Geometry,4326)` | OGC-valid; at most 1 MB as WKB |
| `time_series` | **no** | reference `{series_id, unit}` | Points live in ClickHouse `ts_points` |
| `geotemporal_series` | **no** | reference `{series_id}` | Tracks live in ClickHouse `geo_track_points`. The indexer mirrors the latest point into a declared `geopoint` property (for example `Vessel.last_position`) |
| `vector` | **no** | `vector(n)`, n ≤ 2,000 | Bound to one embedding model; cosine distance (§8.5; DATA_MODEL §7) |
| `media_reference` | yes | `jsonb` `{media_set_rid, item_rid, view_rid, sha256}` | Bytes in object storage; the object's markings apply to the media |
| `cipher_text` | yes | `text` envelope `vgc1:<channel>:<key_version>:<nonce>:<ciphertext>` | AES-256-GCM; decryption needs a channel grant; searchable only through a paired pseudonym property |
| `marking` | yes (always) | `text[]` of marking IDs | Values join the row's markings (dynamic row marking). Writing one needs "apply" rights on that marking |

**Value types** are named constraints over a base type, validated on ingest and edit: `vigil.object_rid`, `vigil.record_key`, `za.sa_id_number`, `za.cipc_registration_number`, `za.vat_number`, `za.csd_supplier_number`, `iso.country_alpha2`, `iso.currency`, `itu.mmsi`, `imo.ship_number`, `olc.plus_code`, `h3.cell`. Definitions live in `packs/_schema/value-types/` (P5); SA identifier rules are in §5.5.

### 1.5 Link types

| Cardinality | Physical form | Example |
|---|---|---|
| `ONE_TO_ONE` | FK property on side A with a unique index | `WatchlistEntry.case_id` → `Case` |
| `MANY_TO_ONE` | FK property on the many side | `Award.tender_ocid` → `Tender` |
| `MANY_TO_MANY` | Join table of both primary keys plus system columns (markings, provenance) | `awarded_to`: `Award` ↔ `Supplier` |
| `OBJECT_BACKED` | An intermediary object type carrying link properties, with two N:1 links | `director_of`: `Person` ↔ `Organisation` through `Directorship` |

- Links are bidirectional and have two API names (`awarded_to` / `awards`).
- One datasource backs at most one link type.
- References are **logical**: no physical FK constraints between object tables, and deletes never cascade (DATA_MODEL §6). A link to a missing or invisible object resolves as "not found".
- A property with value type `vigil.object_rid` MAY back an N:1 link to an interface (a polymorphic reference), for example `ResolutionLink.member_rid` (§5.8).
- Visibility of a link needs visibility of both ends and of the link itself (rule W3, §4.3).

### 1.6 Interfaces

An interface is an abstract shape: shared properties plus link constraints. Multiple inheritance is allowed, and each implementing type maps its own properties onto the interface's. **v1:** OSS queries over an interface return the union of implementing types. Action constraints on interfaces are deepening (P31). Core interfaces:

| Interface | Shared properties | Implemented by |
|---|---|---|
| `Party` | `display_name`, `country` | `Organisation`, `Person` |
| `PartyRecord` | `name_raw`, `source_id`, `responsible_party`, `resolved_entity_rid` | `Supplier`, `OrganOfState`, `Directorship`, `Restriction` |
| `Locatable` | `location` (geopoint) | `Place`, `Asset`, `Event`, `Incident`, `Vessel`, `SatelliteDetection` |
| `Occurrence` | `occurred_at`, `event_type` | `Event`, `Incident`, `PortCall` |

### 1.7 Action types

An action type is one governed transaction over one or more objects.

| Element | Definition |
|---|---|
| Parameters | Typed (`object_ref`, `object_set`, scalars, arrays, media). Constraints: required, min/max, length, regex, enum, value type, object-set membership |
| Rules | Ordered: `create_object`, `modify_object`, `delete_object`, `create_link`, `delete_link`, `function` (edits returned by an `EDITS` function) |
| Submission criteria | Boolean conditions over parameters, object state and user attributes. All must pass; each carries a failure message |
| Writeback webhook | At most **one**, synchronous, **before** commit, timeout ≤ 10 s. Its outputs may feed later rules |
| Side effects | After commit: notifications and secondary-system calls, run as Temporal activities (idempotent, retried, compensable), in no guaranteed order |
| Flags | `outcome_class`, `requires_human_approval`, `agent_executable`, `justification_required`, `allowed_purposes`, `case_scoped` |
| Approval policy | Approver role, n-of-m (1–5), separation of duties, expiry (default 7 days) |

**Execution pipeline** (normative; ARCHITECTURE §6.3):
1. **Authorise.** Warden checks the markings of every object the action reads or touches, OpenFGA checks the role, the purpose must be in `allowed_purposes`, a case must be supplied when the purpose is case-predicated, and a justification when required.
2. **Validate** the parameters.
3. **Evaluate** the submission criteria.
4. **Approval gate.** If approval is needed (below), the run becomes `STAGED_FOR_APPROVAL` and stops. On approval, steps 1–3 run again against current state (a time-of-check/time-of-use guard); a failure makes the run `REJECTED`.
5. **Writeback webhook** (optional). A non-2xx answer or a timeout aborts the run: nothing commits (`WRITEBACK_FAILED`).
6. **Commit** one Postgres transaction: edits to typed tables + `edit_log` + `outbox`. If the commit fails *after* a successful webhook, the run becomes `FAILED` with `error_code = RECONCILIATION_REQUIRED`. A saga then calls the target's registered compensating operation, or opens a human reconciliation task if the target has none (P8; INCIDENT_RESPONSE).
7. **Side effects** as Temporal sagas. A failed side effect never rolls back the commit; it is reported on the action run.

**Invariants** (OMS rejects a pack that breaks them):
- `outcome_class` ∈ {`INFORMATIONAL`, `OPERATIONAL`, `ADVERSE`, `LEGAL_EFFECT`}. **`ADVERSE` or `LEGAL_EFFECT` ⇒ `requires_human_approval = true`** (POPIA s71).
- `agent_executable = false` (the default): an agent's call is always staged, even where a human could apply directly. `agent_executable = true` and no approval required: the agent may apply, as the user. `agent_executable = true` and approval required: the agent may **only stage** (ARCHITECTURE §6.4).
- Approvers are always human (`user:` principals). With separation of duties, the approver differs from the submitter; for an agent run the human sponsor counts as the submitter.
- Criteria and rule expressions use a side-effect-free, non-Turing-complete expression language. v1 candidate: CEL (Common Expression Language, Apache-2.0). **Confirm at P8** (§4.1 evaluation plus ADR).

### 1.8 Function types

- **Languages:** TypeScript and Python in sandboxed containers; WASM components on Wasmtime LTS (P9).
- **Kinds:** `QUERY` reads object sets and returns values. `EDITS` returns ontology edits and commits **only** through a function-backed action rule; invoking it directly is a dry run.
- Functions run **as the calling user** with the same PolicyContext. No ambient I/O and no network egress in v1. Default timeout 60 s, memory caps per type, at most 100,000 objects loaded and at most 3 search-arounds. The artefact's SHA-256 is part of the type version.

### 1.9 Conflict policies (source values versus edits)

Every property that any action can edit MUST declare exactly one policy; OMS rejects an editable property without one. Non-editable properties are source-only by construction. For property *p* of object *o*: *s* is the latest source value, with timestamp *tₛ* (the source's own update time when it provides one, otherwise the dataset commit time); *e* is the latest edit, with timestamp *tₑ*.

| Policy | Merged value | Typical use |
|---|---|---|
| `SOURCE_WINS` | *s* whenever the source row for *o* appears in a changelog; the edit stays in history, marked superseded | Operational overrides until the source catches up |
| `EDIT_WINS` | *e* if any edit exists (until a `reset` edit), otherwise *s* | Analyst judgements: `review_status`, case notes |
| `LATEST_TIMESTAMP` | *s* if *tₛ* > *tₑ*, otherwise *e* (a tie goes to the edit) | Fields both sides maintain: `Vessel.eta_reported` |

- **Existence:** an object deleted by an action stays deleted even if the source still has it, until a `RESTORE` edit. For `SOURCE_WINS` existence, a source deletion removes the object (its edit history stays in `edit_log`). Action-created objects without a source follow edits only.
- **Mechanics:** each typed row carries `edited_props` (property → last edit sequence and timestamp). The indexer merges by primary key and writes with an optimistic version check, retrying any key an action changed concurrently (DATA_MODEL §4.4).
- **Worked example.** At 10:05 AIS reports `eta_reported = 14:00`. At 10:10 an operator edits it to 15:00 (pilot information). At 10:20 AIS reports 14:30. The policy is `LATEST_TIMESTAMP`, so the merged value is 14:30 (10:20 > 10:10). `Supplier.review_status` is `EDIT_WINS`: an analyst's `UNDER_REVIEW` survives every daily OCDS refresh that still says `NONE`.

### 1.10 Limits (v1)

| Limit | Value | Source |
|---|---|---|
| Objects / object types per action | 10,000 / 50 | ARCHITECTURE §6.3; Palantir envelope |
| Size of one object edit | 3 MB | same |
| List parameters | 10,000 primitives / 1,000 object references | Palantir envelope |
| Synchronous action budget / writeback webhooks | 60 s / 1 per action, ≤ 10 s | ARCHITECTURE §6.3 |
| Search-around hops | ≤ 3 in OSS and functions; deeper through traversal (≤ 6 at Tier 4) | ARCHITECTURE §9 |
| Intermediate set per search-around hop | 100,000 objects, then `LIMIT_EXCEEDED` (use traversal) | Palantir TS v1 envelope |
| Properties per object type | 1,000 (batch); 250 (streaming-backed) | PostgreSQL's 1,600-column table limit counts dropped columns too |
| Aggregation buckets per query | 10,000 | Vigil |
| API page size | ≤ 500 | API_DESIGN |

### 1.11 Versioning and schema branches

- **Type versions are immutable.** Each publish creates version *n*+1 of a type on a branch, with a compatibility class computed by OMS:

| Class | Changes |
|---|---|
| `COMPATIBLE` | Add an optional property, link, action or interface implementation; widen a constraint; add an enum value; change display metadata; deprecate |
| `BREAKING` | Remove or rename a property; change a base type, primary key or link cardinality; narrow a constraint; change a conflict policy |
| Security change | Adding markings or raising classification is immediate. **Removing** them is a declassification (W8, §4.3), never a schema change |

- **A `BREAKING` version needs a migration plan** (property mapping, backfill transform, reindex). OMS builds a shadow table, backfills, and swaps at cut-over (P5 "Done when").
- **Schema branches** pair with dataset branches: schema branch `x` ↔ Iceberg branch `vb-x` (DATA_MODEL §9). A branch holds type changes and, for scenarios (P30), branch-local edits.
- **Branch edits never merge into `main` directly.** A scenario is *promoted* by re-applying its decisions as real actions on `main`, with normal authorisation (P30 "Done when").
- **Merge:** fast-forward if `main` has not moved since the fork; otherwise a three-way diff, where any conflict rejects the merge for manual resolution.

### 1.12 Pack format (YAML)

A pack is a directory validated against the JSON Schema in `packs/_schema/` (P5):

```
packs/<pack>/
  pack.yaml            # name, version, depends_on, owner phase
  object-types/*.yaml  link-types/*.yaml  interfaces/*.yaml  action-types/*.yaml
  value-types/*.yaml   functions/<name>/   datasets/*.yaml   interop/ (DTDL, AAS, OCDS mappings)
```

```yaml
# packs/integrity/object-types/supplier.yaml
api_version: vigil.ontology/v1
kind: ObjectType
metadata:
  api_name: Supplier
  display_name: Supplier
  description: A supplier as named in one procurement record. ER resolves it to an Organisation.
  status: ACTIVE
  implements: [PartyRecord]
spec:
  primary_key: supplier_key
  title_property: name_raw
  existence_policy: SOURCE_WINS
  datasources:
    - dataset: integrity.suppliers            # Iceberg <tenant>.integrity.suppliers
  security:
    classification: PUBLIC
    purpose_tags: [PROCUREMENT_INTEGRITY, DUE_DILIGENCE]
    row_markings:
      - when: "party_kind == 'SOLE_PROPRIETOR'"   # a natural person trading in own name
        add: [PII]
  properties:
    supplier_key:        { base_type: string, value_type: vigil.record_key, required: true }
    name_raw:            { base_type: string, required: true, searchable: true, analyzer: vigil_org_name }
    registration_number: { base_type: string, value_type: za.cipc_registration_number }
    csd_number:          { base_type: string, value_type: za.csd_supplier_number }
    party_kind:          { base_type: string, enum: [COMPANY, CLOSE_CORPORATION, SOLE_PROPRIETOR, JOINT_VENTURE, OTHER] }
    organisation_id:     { base_type: string, description: "ER-assigned Organisation (FK of resolves_to)" }
    contact_phone:       { base_type: cipher_text, column_markings: [PII] }
    review_status:       { base_type: string, enum: [NONE, UNDER_REVIEW, CLEARED, REFERRED],
                           default: NONE, editable: true, conflict_policy: EDIT_WINS }
---
# packs/integrity/link-types/awarded-to.yaml
api_version: vigil.ontology/v1
kind: LinkType
metadata: { api_name: awarded_to, inverse_api_name: awards }
spec:
  cardinality: MANY_TO_MANY
  a: { object_type: Award }
  b: { object_type: Supplier }
  datasource: { dataset: integrity.award_suppliers, a_key: award_id, b_key: supplier_key }
---
# packs/integrity/action-types/refer-supplier.yaml
api_version: vigil.ontology/v1
kind: ActionType
metadata: { api_name: refer-supplier, display_name: Refer supplier for investigation, status: ACTIVE }
spec:
  outcome_class: ADVERSE
  requires_human_approval: true
  agent_executable: true              # agents may stage it; they can never apply it
  justification_required: true
  allowed_purposes: [PROCUREMENT_INTEGRITY]
  approval: { approver_role: integrity-supervisor, required_approvals: 1, separation_of_duties: true, expires_after: P7D }
  parameters:
    supplier:   { type: object_ref, object_type: Supplier, required: true }
    indicator:  { type: object_ref, object_type: RiskIndicator, required: true }
    summary:    { type: string, required: true, min_length: 50, max_length: 5000 }
    notify_cfo: { type: boolean, default: false }
  submission_criteria:
    - name: indicator_matches_supplier
      condition: "params.indicator.subject_rid == params.supplier.rid"
      failure_message: The risk indicator must relate to the selected supplier.
    - name: not_already_referred
      condition: "params.supplier.review_status != 'REFERRED'"
      failure_message: This supplier already has an open referral.
    - name: caller_is_analyst
      condition: "'integrity-analyst' in user.roles"
      failure_message: Only integrity analysts may refer suppliers.
  rules:
    - create_object: { object_type: Case, as: case,
        properties: { title: "'Referral: ' + params.supplier.name_raw", purpose: "'PROCUREMENT_INTEGRITY'", status: "'OPEN'" } }
    - create_object: { object_type: WatchlistEntry,
        properties: { subject_rid: params.supplier.rid, case_id: "case.pk", reason: params.summary,
                      expires_at: "request.time + duration('8760h')" } }   # request.time is host-supplied
    - modify_object: { object: params.supplier, properties: { review_status: "'REFERRED'" } }
  side_effects:
    - notification: { when: params.notify_cfo, channel: WHATSAPP, template: integrity.referral_opened,
                      recipients: "role:organ-cfo" }       # mocked until P28
  limits: { max_objects: 3 }
```

---

## 2. Core pack (`packs/_core`)

Both Phase Zero packs reuse these types (dossier §11: "one ontology, two lenses"). Owner: P5, except `case/` (P19) and `scenario/` (P30). `Organisation` and `Person` are **resolved entities**: their data comes from ER output datasets (§5), not from any single source.

| Object type | PK | Key properties | Security default | Notes |
|---|---|---|---|---|
| `Organisation` | `org_id` (ER cluster id, UUIDv7) | `display_name`, `legal_names[]`, `registration_numbers[]`, `org_kind` (COMPANY, CLOSE_CORPORATION, TRUST, NPO, ORGAN_OF_STATE, SOE, FOREIGN, OTHER), `country`, `member_count`, `resolution_version` | PUBLIC | Cluster view (§5.8). Implements `Party` |
| `Person` | `person_id` (ER cluster id) | `display_name`, `name_variants[]`, `id_psn` (pseudonym), `id_cipher` (cipher_text), `member_count` | CONFIDENTIAL + `PII` | Natural persons only. No gender, citizenship or population-group field, ever (§5.5) |
| `Place` | `place_id` (`mdb:ward:<code>`, `iso:ZA`, `unlocode:ZADUR`) | `name`, `place_kind` (COUNTRY, PROVINCE, DISTRICT, MUNICIPALITY, WARD, PORT, SITE), `boundary`, `centroid`, `plus_code` | PUBLIC | Link `within` (N:1 to Place). MDB ward and municipal polygons |
| `Asset` | `asset_id` | `asset_class`, `name`, `operator_org_id`, `location`, `status`, `criticality` (1–5) | RESTRICTED | Security-measure details carry `SECURITY-MEASURE` (§4.4) |
| `Event` | `event_id` | `event_type`, `occurred_at`, `location`, `severity` (1–5), `confidence` (0–1), `source_id` | PUBLIC | Links `involves` (M:N to parties), `at_place`, `documented_by` |
| `Document` | `document_id` = `doc:<sha256>` | `title`, `media`, `sha256`, `document_kind`, `source_url`, `fetched_at`, `language`, `extraction_status` | from source | Content-addressed, so re-fetches deduplicate naturally |
| `Identifier` | `idf_<base32(HMAC)>` (§4.6) | `kind` (CIPC_REG, SA_ID, VAT, CSD, PHONE, EMAIL, BANK_REF, ADDRESS, MMSI, IMO), `value_psn`, `value_cipher`, `value_masked`, `validity` | per kind; SA_ID, PHONE, EMAIL, BANK_REF carry `PII` | The same value always yields the same Identifier, so shared identifiers link records without exposing values |
| `Case` (P19) | `case_id` (`case-2026-000042`) | `title`, `status` (OPEN, REFERRED, CLOSED, ARCHIVED), `purpose`, `threshold_statement`, `legal_basis_ref`, `opened_by`, `opened_at`, `closed_at` | CONFIDENTIAL + `CASE-<n>` | Each case mints its own marking (W6) |
| `CaseParty` (P19, object-backed link) | `case_party_id` | `case_id`, `party_rid` (`vigil.object_rid`: Person or Organisation), `role` (SUBJECT, VICTIM, WITNESS, BYSTANDER, UNKNOWN), `added_by`, `justification_id` | inherits the case | Separates subjects from third parties (hessenDATA; GUARDRAILS §6, H4). Rule W6 applies to bystanders |
| `EvidenceItem` (P19) | `evidence_id` | `case_id`, `subject_rids[]` (`vigil.object_rid`), `document_id`, `content_sha256`, `source_citation` (dataset RID, transaction, PK), `captured_by`, `captured_at` | inherits the case marking and the cited sources' labels | Custody entries are edit-log projections (§10) |
| `Finding` (P19) | `finding_id` | `case_id`, `statement`, `finding_type`, `basis_evidence_ids[]`, `decided_by`, `decided_at` | inherits the case | **Human conclusions only.** Created by an `ADVERSE` action with approval; never by AI |
| `Scenario` (P30) | `scenario_id` | `name`, `schema_branch`, `dataset_branch`, `parameters` (struct), `model_versions`, `status` (DRAFT, RUNNING, COMPLETED, PROMOTED, DISCARDED), `promoted_action_run_id` | inherits its inputs | Results live on the branch (§1.11) |

The ER types `ResolutionLink` and the review actions live in the P16 pack `packs/_shared/er-rules/` (§5.8), which depends on `_core`.

---

## 3. Domain packs for Phase Zero

### 3.1 Integrity pack (`packs/integrity`, P22) — PZ-1

Object types (dossier §11; `Person`, `Identifier` and `Place` come from `_core`):

| Object type | PK | Key properties | Security default |
|---|---|---|---|
| `OrganOfState` | `organ_id` (OCDS buyer id, normalised) | `name`, `sphere` (NATIONAL, PROVINCIAL, LOCAL, SOE, PUBLIC_ENTITY), `regime` (PFMA, MFMA), `mdb_code`, `organisation_id` | PUBLIC |
| `Tender` | `ocid` (as published) | `title`, `procurement_method` (OCDS: open, selective, limited, direct), `procurement_method_details`, `main_procurement_category`, `value_amount`, `currency`, `tender_period_end`, `status`, `buyer_id` | PUBLIC |
| `Award` | `award_id` = `<ocid>/<award.id>` | `date`, `value_amount`, `currency`, `status`, `tender_ocid`, `source` (OCDS, COCT_TENDER, COCT_RFQ) | PUBLIC |
| `Contract` | `<ocid>/<contract.id>` | `award_id`, `value_amount`, `date_signed`, `period_start`, `period_end`, `status` | PUBLIC |
| `Supplier` | `supplier_key` (`ocds:<ocid>:<party.id>`, `coct:<hash>`) | `name_raw`, `registration_number`, `csd_number`, `vat_number`, `party_kind`, `organisation_id`, `review_status` | PUBLIC; `PII` if a sole proprietor |
| `Directorship` (object-backed link) | `directorship_key` | `person_id`, `organisation_id`, `role`, `appointed_on`, `resigned_on`, `source_id` (CIPC extract) | CONFIDENTIAL + `PII` |
| `Restriction` | `restriction_id` (hash of name key, identifier pseudonym, start date, authoriser) | `party_name_raw`, `party_kind` (PERSON, ORGANISATION), `identifier_psn`, `reason`, `period_start`, `period_end`, `authorising_body`, `document_id`, `extraction_confidence` | PUBLIC for organisations; CONFIDENTIAL + `PII` for persons |
| `CourtMatter` | `court_matter_id` (neutral citation or court + case number) | `court`, `case_number`, `judgment_date`, `matter_type`, `outcome`, `source_agreement_ref` | `CRIMINAL-CONDUCT` if criminal. **No source until a written agreement exists** (MSG-006) |
| `AuditFinding` | `<auditee>:<financial_year>` | `financial_year`, `audit_opinion` (CLEAN, UNQUALIFIED_WITH_FINDINGS, QUALIFIED, ADVERSE, DISCLAIMER, OUTSTANDING), `irregular_expenditure_zar`, `source` | PUBLIC |
| `RiskIndicator` | hash(subject RID, model version, as-of date) | `subject_rid`, `score`, `score_raw`, `band`, `override_code`, `data_sufficiency`, `explanation` (struct, §6.5), `review_status`, `label` | inherits every input's label |
| `WatchlistEntry` | `watch_id` | `subject_rid`, `case_id`, `reason`, `added_by`, `expires_at` (mandatory, at most 12 months), `status` | CONFIDENTIAL + case marking |

Links:

| Link (dossier name) | Cardinality | Ends |
|---|---|---|
| `issues` | 1:N (FK `Tender.buyer_id`) | `OrganOfState` → `Tender` |
| `awarded_under` | N:1 | `Award` → `Tender` |
| `awarded_to` | M:N (join table) | `Award` ↔ `Supplier` |
| `contract_for` | N:1 | `Contract` → `Award` |
| `resolves_to` | N:1 (FK `organisation_id`, ER-assigned) | `Supplier`, `OrganOfState` → `Organisation` |
| `director_of` | object-backed (`Directorship`) | `Person` ↔ `Organisation` |
| `has_identifier` | M:N | party records ↔ `Identifier` |
| `shares_identifier_with` | M:N, **derived** by a pack transform from `has_identifier` (provenance = the shared Identifier) | `Organisation` ↔ `Organisation` |
| `restricted_by` | 1:N (ER-assigned FK on `Restriction`) | `Organisation` or `Person` → `Restriction` |
| `party_to` | M:N | `Organisation` or `Person` ↔ `CourtMatter` (party roles are deepening) |
| `audited_in` | 1:N | `OrganOfState` → `AuditFinding` |
| `located_in` | N:1 | `OrganOfState`, `Supplier` → `Place` |

### 3.2 Corridor pack (`packs/corridors`, P23) — PZ-2 (Durban port and corridor twin)

| Object type | PK | Key properties | Security default |
|---|---|---|---|
| `Vessel` | `mmsi` (value type `itu.mmsi`) | `imo_number` (`imo.ship_number`), `name`, `call_sign`, `ship_type` (AIS code), `length_m`, `last_position`, `track` (geotemporal series), `sog_kn`, `cog_deg`, `nav_status`, `last_seen_at`, `ais_source` | PUBLIC |
| `PortCall` | `<mmsi>:<unlocode>:<anchorage_arrival_utc>` | `vessel_mmsi`, `port` (`ZADUR`), `anchorage_arrival_at`, `berth_arrival_at`, `departure_at`, `anchorage_wait_h`, `berth_id`, `terminal_id`, `eta_reported`, `eta_predicted`, `eta_shift_h`, `status` | PUBLIC |
| `Anchorage` | `anchorage_id` | `name`, `area` (geoshape), `current_vessel_count`, `wait_p50_h`, `wait_p90_h`, `wait_model` (struct, §7.1) | PUBLIC |
| `Berth` | `berth_id` | `name`, `terminal_id`, `area`, `status` | PUBLIC |
| `Terminal` | `terminal_id` | `name`, `operator_org_id`, `cargo_type`, `area`, `gust_stop_kn` (§7.2 parameter) | RESTRICTED |
| `SatelliteDetection` | `<product_id>:<n>` | `observed_at`, `location`, `length_est_m`, `confidence`, `matched_mmsi`, `match_score`, `dark` | PUBLIC |
| `WeatherCell` | `h3:<res7 cell>` | `forecast_issued_at`, `gust_series` (time series), `max_gust_next_48h_kn`, `source` | PUBLIC |
| `CorridorSegment` | `<route>:<km_from>-<km_to>` | `mode` (ROAD, RAIL), `geometry`, `length_km`, `risk_rate`, `risk_ratio`, `risk_band` (§7.3), `incident_count_30d` | PUBLIC |
| `BorderPost` | `border_post_code` (confirm at P23) | `name`, `countries`, `location`, `wait_estimate_h` (only with a data partnership) | PUBLIC |
| `GridArea` | EskomSePush area id | `name`, `load_reduction_active`, `next_event_start`, `next_event_end` | PUBLIC |
| `Incident` | hash(source, source event id) | `occurred_at`, `location`, `incident_type` (PROTEST, THEFT, VANDALISM, ACCIDENT, WEATHER, OTHER), `severity` (1–5), `extraction_confidence`, `document_id`, `segment_id` | PUBLIC; `PII` if extraction names a person |
| `SlotAdvisory` | `advisory_id` | `terminal_id`, `window_start`, `window_end`, `reason` (WIND, CONGESTION, INCIDENT, GRID), `recommended_shift_h`, `channel` (CSV, WHATSAPP), `issued_by`, `status` | RESTRICTED |

Links (appendix 06 §6 names in brackets): `calls_at` (PortCall → Berth, N:1) · `made_by` (PortCall → Vessel, N:1) · `at_anchorage` (Vessel → Anchorage, N:1, current) · `part_of` (Berth → Terminal, N:1) · `affected_by` (Terminal and Anchorage → WeatherCell, N:1 by spatial containment) · `matches_vessel` (SatelliteDetection → Vessel, N:1, nullable) · `detected_without_ais` (dark SatelliteDetection → Anchorage, N:1, derived) · `serves_corridor` (Terminal and BorderPost ↔ CorridorSegment, M:N) · `affects` (Incident → CorridorSegment, N:1) · `supplied_by` (Terminal → GridArea, N:1) · `advises` (SlotAdvisory → Terminal, N:1). The action `issue-slot-advisory` is `OPERATIONAL`, `agent_executable: false`, and needs no approval (P23.4).

---

## 4. Marking and classification model (Warden, P3)

### 4.1 Security labels

Every resource *R* (dataset, column, row/object, property, link, file, derived artefact, model context) carries a label **λ(R) = ⟨M, C, REL, P⟩**:

| Component | Meaning | Semantics |
|---|---|---|
| **M**: markings | Set of marking IDs (`^[A-Z][A-Z0-9-]{1,63}$`) | **Conjunctive**: a reader must hold *all* of them |
| **C**: classification | Level on the canonical ladder (§4.2) | Hierarchical: reader clearance ≥ C |
| **REL**: releasability | Set of releasability principals (`REL-ZA`, `REL-SADC`, `REL-<ORG>`), or ⊤ (unrestricted) | **Disjunctive**: the reader must match *at least one* principal. Groups expand to members first |
| **P**: purpose tags | Registered purpose codes, or ⊤ | The declared request purpose must be one of them |

A request carries a **policy context σ = ⟨Mᵤ, cᵤ, Rᵤ, p, k, J⟩**: the user's held markings, clearance, releasability attributes, declared purpose, optional case, and optional justification. Warden builds σ from the OIDC token, group-to-marking mappings, vetting records and the request headers (API_DESIGN).

### 4.2 Canonical classification ladder

| Level | Vigil code | National Data and Cloud Policy ¶15.2.5 tier | MISS (1996) | Examples |
|---|---|---|---|---|
| 0 | `PUBLIC` | public / open | unclassified | OCDS releases, AIS positions, gazettes |
| 1 | `RESTRICTED` | confidential / sensitive | Restricted | Internal operational data; commercial terminal parameters |
| 2 | `CONFIDENTIAL` | confidential / sensitive | Confidential | Personal information, case files, restricted-supplier person entries |
| 3 | `SECRET` | secret | Secret | Classified-enclave data only (P41) |
| 4 | `TOP_SECRET` | top secret | Top Secret | Classified-enclave data only (P41) |

- Mapping **out** to the NDCP tiers merges levels 1 and 2. Mapping **in** from "confidential/sensitive" takes level 2, the conservative end of the band.
- A deployment has a ceiling `max_classification` (default 2). Levels 3–4 are refused outside a classified-enclave profile (P41).
- The MISS text was not fetched during research (appendix 04 §2, **[UNVERIFIED]**). **Confirm at P3, and again at P41,** with the MISS text and counsel.

### 4.3 Formal rules

- **W1 Read.** allow(σ, R) ⇔ M(R) ⊆ Mᵤ ∧ C(R) ≤ cᵤ ∧ (REL(R) = ⊤ ∨ ⟦REL(R)⟧ ∩ Rᵤ ≠ ∅) ∧ (P(R) = ⊤ ∨ p ∈ P(R)) ∧ discretionary(σ, R) ∧ gates(σ, R). `discretionary` is the OpenFGA role check; `gates` are the gated-module checks (§5.9).
- **W2 Properties.** A property value is returned only if allow(σ, row) and allow(σ, λ(column)). Otherwise the property is redacted with a reason (API_DESIGN §A.8), and the row itself stays visible.
- **W3 Links.** A link between A and B is visible ⇔ allow(σ, A) ∧ allow(σ, B) ∧ allow(σ, λ(link)). A foreign key pointing at an invisible target is redacted.
- **W4 Write.** A user may create or modify R′ only if allow(σ, R), M(R′) ⊆ Mᵤ and C(R′) ≤ cᵤ. No write-up beyond the user's own label, and λ(R′) must be at least as restrictive as λ(R) unless W8 applies.
- **W5 Propagation** (Warden `propagate()`), for D = f(I₁ … Iₙ) with transform label λ_f:
  - M(D) = M(I₁) ∪ … ∪ M(Iₙ) ∪ M_f (**union**);
  - C(D) = max(C(I₁), …, C(Iₙ), C_f) (**max**);
  - REL(D) = ⟦REL(I₁)⟧ ∩ … ∩ ⟦REL(Iₙ)⟧ (**intersection**; ⊤ is neutral);
  - P(D) = P(I₁) ∩ … ∩ P(Iₙ) ∩ P_f (**intersection**; ⊤ is neutral).
  - If REL(D) = ∅ or P(D) = ∅, the derivation is **blocked**: nobody could lawfully use the output. The purpose case is the POPIA s15 compatibility gate. It reopens only through an approved purpose-compatibility record naming the s15(3) ground.
- **W6 Purpose and case binding.** A purpose with `case_required` needs a case k with status OPEN and a recorded threshold statement (the hessenDATA "identifiable danger" safeguard, dossier §9.2). Each case mints the marking `CASE-<n>`. Objects created by `case_scoped` actions get it, and only case members hold it. Every person named in a case has a `CaseParty` role. For a `BYSTANDER`, Warden redacts properties by default (reason `CASE`), blocks search-around expansion without a fresh justification, and applies shorter retention.
- **W7 Justification (Checkpoints).** These checkpoint classes need a justification J (20–2,000 characters) bound to user, purpose, case, checkpoint and scope, valid for at most 8 hours:
  - `SPECIAL_CATEGORY_ACCESS`: first access to `SPECIAL-PI`, `CRIMINAL-CONDUCT` or `CHILD` data;
  - `PERSON_LINKAGE`: person-level linkage;
  - `EXPORT`: exports;
  - `DECLASSIFICATION`: declassification;
  - `REVEAL`: revealing `cipher_text` in clear.

  Only J's ID and SHA-256 go into the audit event. The text lives in `vigil_sec.justification` (DATA_MODEL §4.2a), encrypted and erasable.
- **W8 Declassification** (Palantir's "Expand Access"). Any change that makes λ less restrictive (removing a marking, lowering C, widening REL or P) needs an **applied `declassify-resource` action**: requested by one marking administrator, approved by a *second*, each holding every affected marking and clearance ≥ C(R), with justifications. It is a Temporal workflow and is audited. It takes effect prospectively and is recorded as a lineage edge; derived resources re-propagate on their next build.
- **W9 Special classes.** Data in the classes of §4.4 needs its recorded ground before ingestion. `CHILD` ingestion is blocked unless the `CHILD_DATA` gate is active. Values in these classes are stored as `cipher_text` under class-specific key channels.
- **W10 AI.** Content goes to a model endpoint E only if allow(σ, content) **and** allow(σ_E, content), where σ_E is the endpoint's cleared label (§8.3). The obligation `NO_AI` blocks it outright.
- **W11 Deny by default; no existence oracle.** A missing or invalid context denies. Invisible resources are indistinguishable from absent ones (HTTP 404).
- **W12 Audit.** Every decision, allow and deny, emits a hash-chained audit event with the context hash, purpose, case, resource RID, marking IDs and decision.

### 4.4 Marking classes

| Marking | Class | Law | Required ground / control |
|---|---|---|---|
| `PII` | Personal information of a natural person | POPIA s1, s11 | Lawful basis recorded on the dataset. Juristic-person data is not marked by default; **counsel to confirm** (MSG-007) |
| `SPECIAL-PI` | Religion or philosophical beliefs, race or ethnic origin, trade-union membership, political persuasion, health, sex life, biometrics | POPIA s26(a) | s27 ground; separate key channel; checkpoint W7. Vigil never *infers* these traits (red line; §5.1 principle 5) |
| `CRIMINAL-CONDUCT` | Alleged offences and proceedings, including criminality-implying labels | POPIA s26(b), s33 | s33 authorisation; shared watch lists need the `S57_1B_WATCHLIST` gate |
| `CHILD` | Personal information of children | POPIA s34–35 | Blocked by default; `CHILD_DATA` gate |
| `SECURITY-MEASURE` | Security arrangements at critical infrastructure (patrols, camera positions) | CIPA s26 | Only in deployments with a cleared team; never sent to shared models |
| `CASE-<n>` | Case compartment | hessenDATA safeguards | Case membership |
| `ORG-<x>`, `PRJ-<x>`, `<TENANT>-<NAME>` | Organisation, project and customer-defined markings | — | Marking administrators only; owners cannot grant them |

### 4.5 Worked examples

**Example 1: a join in PZ-1.** "Award risk candidates" joins three datasets:

| Input | M | C | REL | P |
|---|---|---|---|---|
| OCDS awards | ∅ | 0 PUBLIC | ⊤ | ⊤ |
| Restricted-supplier entries | {PII} | 2 CONFIDENTIAL | ⊤ | {PROCUREMENT_INTEGRITY, DUE_DILIGENCE} |
| CIPC directorships | {PII} | 2 CONFIDENTIAL | ⊤ | {PROCUREMENT_INTEGRITY} |
| **Derived** (W5) | **{PII}** | **2** | **⊤** | **{PROCUREMENT_INTEGRITY}** |

- Analyst A holds {PII}, has clearance 2 and declares `PROCUREMENT_INTEGRITY`, so she is allowed.
- Viewer B holds ∅ and has clearance 1, so the rows are invisible to him (404 on direct fetch).
- A declares `DUE_DILIGENCE` instead: denied, because that purpose was lost in the intersection.
- This join links person-level identifiers across responsible parties, so it also needs the s57 gate (§5.9).

**Example 2: releasability and classification in PZ-2.** A corridor dataset shared with SADC partners has REL = {REL-SADC}, which expands to the 16 member states {AO, BW, KM, CD, SZ, LS, MG, MW, MU, MZ, NA, SC, ZA, TZ, ZM, ZW}, and C = 1 (terminal parameters). It is joined with a customs extract with REL = {REL-ZA} and C = 0. The result has REL = {REL-ZA} and C = max(1, 0) = 1. A Botswana liaison officer (Rᵤ = {REL-BW}, cᵤ = 2) is denied; a South African analyst (Rᵤ = {REL-ZA}, cᵤ = 1) is allowed. Joining with a dataset releasable only to {REL-KE} would give REL = ∅, and Warden blocks that derivation.

**Example 3: case binding, property redaction and declassification.**
- An `EvidenceItem` in case 42 has M = {CASE-2026-000042, CRIMINAL-CONDUCT} and C = 2, and its `id_cipher` property has column markings {PII}.
- Case member C holds {CASE-2026-000042, CRIMINAL-CONDUCT} but not PII. C sees the row, sees `id_cipher` as `{"redacted": true, "reason": "MARKING"}`, and can still filter on the pseudonym `id_psn`. Her first access needs a W7 justification, because the item is criminal-conduct data.
- A non-member receives 404.
- At closure, the transparency report needs "incidents per corridor segment": an aggregate with every cell at least 10 and no identifiers, derived at C = 2. Lowering it to PUBLIC is a W8 declassification: marking administrator X requests, a *different* administrator Y approves, both justify, and the audit chain records the lineage edge. The case data itself stays at C = 2.

### 4.6 Pseudonyms and cipher text

- **Pseudonym:** `psn_` + the first 26 characters of base32(HMAC-SHA-256(K_psn(tenant), kind ‖ ":" ‖ normalised value)). The key is per tenant and per purpose family, held in KMS (HSM from P42). Pseudonyms are deterministic, so they support joins, blocking and "shares identifier" detection without exposing values.
- **Pseudonyms are still personal information** under POPIA while the tenant can re-identify them. They reduce exposure; they do not remove obligations.
- **Masked display** (`******4521`) is a presentation of the cipher text for users with the `PII` marking. Users without it get the pseudonym or a redaction, never the mask.

### 4.7 Decision output: redactions and obligations

`evaluate(σ, λ) → Decision{allow, redactions, obligations}` (P3.2). Every enforcement point (Postgres, OpenSearch, OSS, Mind gateway, exports) MUST honour every obligation it receives, and refuses the request if it cannot.

| Output | Meaning |
|---|---|
| `REDACT(property, reason)` | Return `{"redacted": true, "reason": …}` in place of the value (W2, W3) |
| `MASK(property)` | Return the masked display of cipher text (§4.6) |
| `REQUIRE_JUSTIFICATION(checkpoint)` | Refuse until a J for this checkpoint is bound (W7) |
| `AUDIT_READ` | Emit a read audit event with row count and marking IDs (W12) |
| `NO_AI` | Never place the content in a model context (W10) |
| `NO_EXPORT` | Exclude it from exports and evidence packs |
| `SUPPRESS_SMALL_CELLS(k)` | Suppress aggregate cells below k in exports (default k = 10; DATA_MODEL §13) |
| `ATTRIBUTE(terms_ref)` | Carry the source's attribution on exports (terms register, P11.5) |

---

## 5. Entity resolution (P16)

### 5.1 Principles

1. **Fellegi–Sunter probabilistic linkage through Splink** (MIT): DuckDB backend in v1, Spark for large jobs.
2. **Non-destructive clusters.** Source records are never merged or edited. Resolved entities are regenerated views with per-link provenance (the Quantexa pattern), and every change is reversible.
3. **Humans decide the grey zone.** Review queue in the Cockpit; decisions are actions.
4. **The law is a gate in code.** Person-level cross-party linkage is locked until an authorisation reference is recorded (§5.9).
5. **No ethnicity inference.** Name rules run uniformly on every name. Vigil never classifies a person's language, ethnicity or origin from a name, and never stores such an attribute (POPIA s26; red line).

### 5.2 Pipeline

`normalise (§5.3) → block (§5.6) → compare (§5.4) → score (Fellegi–Sunter) → decide (§5.7) → cluster and survivorship (§5.8) → emit datasets → review → evaluate (§5.10)`

Every run is one Iceberg transaction on `<tenant>._er.*` datasets. That makes it versioned, attributable, and reversible to any earlier snapshot.

### 5.3 Name normalisation (replaces English phonetic codes)

| Step | Rule |
|---|---|
| N0 | Keep the raw string. All derived keys are *additional* columns |
| N1 | Unicode NFKC, case-fold, collapse whitespace. Strip diacritics for comparison keys only (Afrikaans ê/ë, Tshivenda ḓ/ṱ, Sepedi š, Portuguese ã/ç) |
| N2 | Remove titles from a curated list (Mr, Mrs, Ms, Dr, Prof, Adv, Hon, Rev). Tokens that are also common surnames (Nkosi, Kgosi) are **marked optional, never stripped** |
| N3 | Organisations: strip and store the legal form, whatever its spelling: (Pty) Ltd, Proprietary Limited, Ltd, CC, NPC, SOC Ltd, (RF), Inc, Trust, Lda, Limitada, SA, SARL. "t/a" and "trading as" split off an alias |
| N4 | Tokenise on spaces and punctuation. Hyphenated double surnames yield both parts and the joined form |
| N5 | Particles (Afrikaans/Dutch: van, van der, van den, de, du, de la, le, la, von, ter; Portuguese: da, das, de, do, dos, e) fold into the surname key: "Jan van der Merwe" → surname key `vandermerwe`, given `{jan}` |
| N6 | Initials: "J.P.", "JP" and "J P" become initial tokens that can match full given names (§5.4) |
| N7 | Name order: comma and all-caps-surname patterns ("MERWE, VAN DER JAN"; "NKOSI JABULANI PETER") produce both orders. Comparators are order-insensitive token sets |
| N8 | Multiple given names: people use one of several given names, or a church or English name alongside an African name. Handled by the subset-tolerant token-set score |
| N9 | Clan names and isibongo: a curated, reviewed equivalence table (`packs/_shared/er-rules/clan-equivalences.yaml`) maps a clan name to family surnames. Entries need native-speaker review and evidence; they are never auto-generated. **Table content: confirm at P16** |
| N10 | Patronymic and matronymic forms: markers bin, ibn, bint, ben, ould, wa (Kikuyu "Kamau wa Ngengi") and ka (isiZulu "Cetshwayo kaMpande") are tagged. In patronymic systems (Ethiopia, Eritrea, Somalia, Sudan) the second name is the father's *given* name, so it is compared by position, not as a stable family name |
| N11 | Portuguese naming (Mozambique, Angola): several surnames, maternal then paternal. The last surname is primary, and any-surname-subset matches are allowed at a lower level |
| N12 | Arabic transliteration (North and East Africa): drop the article al-/el- into an optional token; map variant clusters from a curated table (Mohamed / Mohammed / Muhammad / Mohamad; Abdel / Abdul / Abd al-); add a consonant-skeleton key (first letter kept, vowels removed, doubles collapsed) as a secondary signal |
| N13 | Orthographic variants, as reversible rewrite rules that *generate* keys and never replace them: Sesotho South Africa ↔ Lesotho `oe↔we` (Moshoeshoe / Moshweshwe; Mokoena / Mokwena) and `d↔l` before i and u (Modimo / Molimo); clerk spellings `dhl↔dl` (Dhlamini / Dlamini). **Click letters c, q and x are never collapsed** into k, g or s |

**Why Soundex and Metaphone are banned.** They encode English letter-to-sound rules:
- **Soundex** maps c, g, j, k, q, s, x and z to the same digit and ignores vowels. So "Mokoena" and "Makhene" are both **M250**, "Mkhize" and "Maseko" are both **M220**, and "Nxumalo" and "Nkumalo" are both **N254**. Click consonants (distinct phonemes in isiZulu and isiXhosa) collapse into k/g/s, and common noun-class prefixes (Ma-, Mo-, Nku-) dominate the four-character code. The result is false merges and missed matches.
- **Metaphone** applies English rules: th becomes θ (in Nguni and Sotho-Tswana orthography "th" is an aspirated t), initial X becomes S, Q becomes K, and C before e/i becomes S. So "Xaba", "Qwabe" and "Cele" all acquire English sounds they do not have.
- Vigil uses the normalisation above plus edit-distance similarity and curated tables instead. Per-language rule sets are deepening at P16 (task 16.2), and all rule sets apply to all names (principle 5).

### 5.4 Comparators

- **Jaro:** J(a, b) = ⅓ · (m/|a| + m/|b| + (m − t)/m), where m is the number of matching characters within the window ⌊max(|a|, |b|)/2⌋ − 1, and t is half the number of transposed matches. J = 0 when m = 0.
- **Jaro–Winkler:** JW = J + ℓ · p · (1 − J), with ℓ the common prefix length (at most 4) and p = 0.1. Common Bantu prefixes (Mo-, Ma-) make the prefix bonus less informative, so P16 MAY lower p to 0.05 **only** on evaluation evidence.
- **Normalised Levenshtein similarity:** L(a, b) = 1 − Lev(a, b) / max(|a|, |b|).
- **Composite token similarity:** **S(a, b) = 0.6 · JW(a, b) + 0.4 · L(a, b).**
- **Initials:** S(initial, token) = 0.85 if the initial equals the token's first letter, otherwise 0; S(initial, initial) = 1 if equal.
- **Token-set name score** for token sets A and B after N1–N13: take the one-to-one alignment π that maximises Σ S (Hungarian algorithm). Then
  - S_subset = Σ_π S / min(|A|, |B|) and S_full = Σ_π S / max(|A|, |B|);
  - **S_name = 0.75 · S_subset + 0.25 · S_full.** This tolerates omitted given names but still prefers complete agreement.
- **Variant keys** from N9–N13 do not raise S. They create a separate comparison level ("orthographic-variant exact"), so Splink learns their m and u probabilities on their own.

**Worked example.** "MOKOENA" versus "MOKWENA": m = 6, t = 0, so J = ⅓(6/7 + 6/7 + 1) = 0.904762. ℓ = 3, so JW = 0.904762 + 0.3 × 0.095238 = 0.933333. Lev = 1, so L = 1 − 1/7 = 0.857143. **S = 0.6 × 0.933333 + 0.4 × 0.857143 = 0.902857.** The N13 rule `oe↔we` also makes them an exact orthographic-variant match, a stronger comparison level. By contrast "MOKOENA" versus "MAKHENE" gives S = 0.674, although Soundex calls them identical. "J.P. van der Merwe" versus "Jan Pieter van der Merwe": the surname keys are exact; each given-name pair is initial against token, 0.85, so S_subset = S_full = 0.85 and S_name = 0.85.

**Comparisons and levels (Splink).** Exact levels carry term-frequency adjustment, because a match on a very common surname or company word is weak evidence.

| Comparison | Levels, strongest first |
|---|---|
| Person surname | exact key → orthographic-variant exact → S ≥ 0.92 → S ≥ 0.80 → else |
| Person given names | all tokens exact → S_name ≥ 0.90 → initials-compatible → S_name ≥ 0.75 → else |
| SA ID number | exact pseudonym → partial (first 10 digits' pseudonym) → different → null |
| Date of birth | only if the purpose's lawful basis covers it: exact → year and month → year → else |
| Organisation name | exact normalised key → S_name ≥ 0.92 → S_name ≥ 0.80 → else |
| CIPC registration | exact pseudonym → partial (same year and sequence, different suffix) → different → null |
| VAT / CSD number | exact → different → null |
| Location | same municipality → same province → different → null |
| Shared identifiers (phone, email, address, bank reference pseudonyms) | at least one shared → none shared → null |

### 5.5 South African identifier validation

| Identifier | Format | Validation | Used as an ER feature |
|---|---|---|---|
| SA ID number | 13 digits `YYMMDD SSSS C A Z` | Valid calendar date (century inferred for validation only); **Luhn** check digit Z over the first 12 digits | Full-value and first-10-digit pseudonyms (opaque equality keys: no digit is ever interpreted), plus a validity flag. **Never** gender (SSSS), citizenship (C) or the 12th digit (A, historically reported to encode population group). These are never derived, stored or used, because the lawful basis does not cover them and C and A touch special-PI and residence status. **Confirm posture with counsel at P16** |
| CIPC registration number | `YYYY/NNNNNN/NN` (regex `^(18\|19\|20)\d{2}/\d{6}/\d{2}$`; legacy "CK" prefix stripped) | Suffix is the entity type: 06 public company, 07 private company, 08 NPC, 21 incorporated, 23 close corporation. **Other codes: confirm at P22** | Exact and partial (same year and sequence, different suffix: a conversion or capture error) |
| VAT number | 10 digits beginning with 4 | Format only; no public checksum is relied on (**confirm at P22**) | Exact |
| CSD supplier number | observed as `MAAA` + 7 digits (**confirm at P22**) | Format only | Exact (join key only; the CSD itself is closed, MSG-006) |

**Luhn example** (the widely published synthetic test number 800101 5009 0 8 7). From the right of the first 12 digits, double every second digit starting with the rightmost: 8→16→7, 9→18→9, 0→0, 1→2, 1→2, 0→0, which sums to 20. The undoubled digits 0 + 0 + 5 + 0 + 0 + 8 sum to 13. Total 33, so Z = (10 − 33 mod 10) mod 10 = **7**: valid. An invalid check digit lowers the level to "unvalidated", never to "different".

### 5.6 Blocking rules

A candidate pair must satisfy at least one rule. Rules are the union of:
- **Organisations:** `reg_psn`; `name_key`; `substr(name_key, 1, 4)` + `municipality`; `vat_psn`; `csd_number`; `phone_psn`.
- **Persons:** `id_psn`; `surname_key` + `given_initial_1`; `surname_variant_key` + `given_initial_1`; `given_key_1` + `substr(surname_key, 1, 3)`.

Targets: at most 50 million comparisons per run, and blocking recall ≥ 0.99 on the evaluation set.

### 5.7 Scoring and decision thresholds

**Match weight:** w = log₂(λ/(1 − λ)) + Σₖ log₂(m_kℓ / u_kℓ), and probability p = 2ʷ / (1 + 2ʷ). λ is the prior probability that two random records match. m comes from EM training on blocked pairs; u comes from random sampling.

| Band | Organisations | Persons | Outcome |
|---|---|---|---|
| Auto-link | p ≥ **0.95** | p ≥ **0.98** | `AUTO_LINKED` |
| Review | 0.70 ≤ p < 0.95 | 0.80 ≤ p < 0.98 | `PENDING_REVIEW` (human queue) |
| Reject | p < **0.70** | p < 0.80 | no link |

**Rationale.** A false merge in this domain can tie an innocent party to a restricted supplier, which means defamation risk and POPIA harm. So auto-linking needs near-certainty, and persons, whose harm is higher, get stricter bands. The review band is sized for reviewer capacity. Below 0.70 a review queue is mostly non-matches and wastes reviewer time. P16 calibrates p on the evaluation set and MAY tighten these bands; loosening them needs an ADR.

**Hard rules,** applied after scoring:
- two validated, *different* CIPC numbers (or SA IDs) produce a **cannot-link**, whatever the names say;
- human `CONFIRMED` and `REJECTED` decisions are hard must-link and cannot-link constraints on every later run;
- an exact validated identifier with S_name ≥ 0.80 is auto-linked.

**Worked example** (illustrative m and u; λ = 10⁻⁴, so the prior term is −13.288). Compare an OCDS supplier and a CIPC company record:
- exact normalised name: m = 0.70, u = 0.0001, weight +12.773;
- same municipality: m = 0.55, u = 0.02, weight +4.781;
- both have phone pseudonyms, none shared: m = 0.60, u = 0.9998, weight −0.737;
- the OCDS record has no registration number: null, neutral.

w = 3.530, so **p = 0.920: review**. If the same pair also has the same CIPC number (m = 0.95, u = 10⁻⁷, weight +23.179), then w = 26.709 and **p > 0.999 99: auto-link**. With only a name at S ≥ 0.92 (weight +8.644) instead of an exact name, w = −0.599 and p = 0.398: rejected.

### 5.8 Clusters as views, provenance and reversibility

- **`ResolutionLink`** (pack `_shared/er-rules`, P16) is the object-backed link from a party record to an entity. PK = SHA-256(member RID ‖ "→" ‖ entity RID), so it is stable across runs. Properties: `member_rid` (`vigil.object_rid`), `entity_rid`, `entity_class` (ORGANISATION, PERSON), `match_probability`, `match_weight`, `comparison_levels` (struct), `model_version`, `blocking_rule`, `linkage_class` (ORG_SAME_PARTY, ORG_CROSS_PARTY, PERSON_SAME_PARTY, PERSON_CROSS_PARTY), `authorisation_ref`, `status` (AUTO_LINKED, PENDING_REVIEW, CONFIRMED, REJECTED, REVERTED, SUSPENDED), `decided_by`, `decided_at`, `valid_from`, `valid_to`. `status` is `EDIT_WINS`, so re-runs never overwrite human decisions.
- **Clustering:** connected components over `AUTO_LINKED` and `CONFIRMED` links. A component containing a cannot-link pair is split by removing its weakest edges, and the removed edges go to review.
- **Cluster identity is stable.** A new cluster keeps the ID of the predecessor with which it shares the largest membership (Jaccard ≥ 0.5; ties go to the oldest). A merge keeps the oldest ID and records `merged_from`; a split keeps the ID on the largest part.
- **Survivorship** for entity properties: the display name is the most frequent normalised name (ties go to the most recent source); identifiers are the union of validated identifiers. Entities are **regenerated each run** from membership. No source record is ever changed.
- **Materialisation:** a pack transform copies each record's active link into its FK (for example `Supplier.organisation_id`), so graph pivots cost one hop.
- **Reversal:** revert to an earlier Iceberg snapshot of `_er`, or split and unlink through review actions (`confirm-resolution-link`, `reject-resolution-link`, `split-entity`). Both paths are audited.

### 5.9 The s57(1)(a) linkage lock

- Every record carries `responsible_party`: the organisation that originated its processing, such as `CIPC`, `NT_OCPO` or the tenant itself (§9.5).
- A comparison is **person cross-party** if both records describe natural persons and their responsible parties differ.
- Before comparing, the ER job calls Warden `check_gate(S57_1A_LINKAGE, scope)`. Scope = the datasets, identifier kinds, purposes and responsible parties involved. The gate passes only with an active `warden.gate_authorisation` (DATA_MODEL §3.6), which holds an Information Regulator reference (or a sector-code reference under s57(3)), dual approvers, counsel reference and validity dates, and whose scope covers the job.
- **Without it, the job refuses to run** those comparisons (`LINKAGE_LOCKED`, HTTP 423) and emits an audit event. The lock is **on by default** in code (INSTRUCTIONS.md §9.3).
- When an authorisation expires or is revoked, its links become `SUSPENDED` at the next run, and the OSS hides them immediately through a gate obligation.
- **Synthetic data:** a `SYNTHETIC_DATA_ONLY` authorisation is valid only if every input dataset has `data_origin = SYNTHETIC`. This lets demos and tests exercise the code path.
- **Same-party** person linkage (for example directors within one CIPC extract) and organisation linkage are not locked in v1. Counsel must confirm the juristic-person posture (MSG-007); the gate supports switching organisation linkage on by tenant policy, without code changes. Whether public-register linkage engages s57(1)(a) at all is for counsel. Until counsel rules, **Vigil treats it as engaged.**

### 5.10 Evaluation

- **Labelled SA evaluation set:** synthetic persons plus public organisation records, built by a generator with explicit name patterns (particles, patronymics, Portuguese multi-surnames, Arabic transliteration, click consonants, orthographic variants, initials, order swaps).
- **Metrics:** pairwise precision, recall and F1 at the thresholds; cluster-level B³; blocking recall; reviewer throughput; precision of the review queue.
- **Targets:**
  - organisations: **precision ≥ 0.95 and recall ≥ 0.90** (P16 "Done when");
  - persons: precision ≥ 0.98 on same-party linkage;
  - blocking recall ≥ 0.99;
  - **fairness gate:** recall in every name-pattern bucket ≥ 0.85, and no bucket more than 0.05 below the overall. Buckets are properties of the *synthetic generator*, never inferred from real people.

---

## 6. Risk indicator scoring (Integrity pack, P22)

### 6.1 Principle

Outputs are **risk indicators, not findings**. Every `RiskIndicator` carries the label "RISK INDICATOR — NOT A FINDING". An indicator never triggers a consequence on its own. The only path to an adverse outcome is the `refer-supplier` action, which is `ADVERSE` and needs human approval (POPIA s71; defamation risk, dossier §11). The unit scored in v1 is an **Award**, with its supplier(s) and buyer as context.

### 6.2 Components

Score: R_raw = min(100, Σᵢ wᵢ · sᵢ · mᵢ), with sub-scores sᵢ ∈ [0, 1], weights wᵢ summing to 100, and multipliers mᵢ ≥ 1.

| # | Component | wᵢ | Sub-score sᵢ |
|---|---|---|---|
| 1 | Restriction linkage | 30 | 1.0 if the supplier's Organisation has a Restriction active on the award date. 0.7 if a director (Person) is linked to an active restriction (requires the s57 gate; otherwise "not computed"). 0.4 if a restriction ended within 5 years before the award. Pending (review-band) links contribute 0 and are only flagged |
| 2 | Director overlap | 15 | min(1, n/3), where n is the number of other suppliers sharing a director that the same buyer awarded within 365 days (same-party CIPC linkage) |
| 3 | Threshold proximity | 15 | max(s_prox, s_split). s_prox = 1 − (T − v)/(0.1·T) for 0.9T ≤ v < T, otherwise 0. s_split = 1 if awards from the same buyer to the same Organisation (or to director-sharing suppliers) within 30 days are each below T but sum to T or more. T comes from a versioned **threshold register** (for example the City of Cape Town RFQ threshold of R750,000; PFMA and MFMA values are [UNVERIFIED], **confirm at P22**) |
| 4 | Award velocity | 10 | max(s_speed, s_burst). s_speed = 1 − d/P10 if d < P10, where d is the days from tender close to award and P10 is the buyer's 10th percentile for the same method and category (at least 30 awards in 3 years, otherwise the national reference). s_burst = 1 if P(N ≥ n₉₀ \| λ) < 0.001, 0.5 if < 0.01, otherwise 0, with n₉₀ the supplier's awards from this buyer in 90 days and λ the buyer's mean per active supplier |
| 5 | Deviation / limited bidding | 15 | OCDS `procurementMethod`: direct 1.0, limited 0.8, selective 0.3, open 0. Also 1.0 if `procurementMethodDetails` cites a deviation, emergency or sole source (TR 16A6.4, MFMA SCM Regulation 32/36 wording) |
| 6 | Adverse court matters | 10 | 1.0 for a criminal fraud or corruption judgment against the Organisation within 10 years; 0.6 for a civil judgment in a procurement dispute; 0.3 for a pending matter. **Not computed without a written source agreement** |
| 7 | Audit findings (buyer context) | 5 | Latest opinion: DISCLAIMER or ADVERSE 1.0; QUALIFIED 0.6; UNQUALIFIED_WITH_FINDINGS 0.3; CLEAN 0 |

Multipliers:
- m₂ = 1.2 when the director-sharing supplier also shares a non-director identifier (phone, email, address or bank-reference pseudonym): corroboration.
- m₄ = m₅ = 1.1 for values of R10 million or more, and 1.25 for R50 million or more.
- All other mᵢ = 1.

### 6.3 Overrides, bands and sufficiency

- **O1 (sanctions-style override):** a *verified* match to an **active Treasury restriction** forces **HIGH**, whatever the other scores. "Verified" means a `ResolutionLink` that is `CONFIRMED`, or `AUTO_LINKED` through an exact validated identifier. The final score is R = max(R_raw, 60), and `override_code = O1_ACTIVE_RESTRICTION`.
- **O2:** an authorised person-level link from a director to an active restriction sets a floor of MEDIUM (R ≥ 30).
- **Bands:** LOW [0, 30), MEDIUM [30, 60), HIGH [60, 100].
- **Data sufficiency:** `SUFFICIENT`, `PARTIAL` (a component could not be computed, or restriction data is more than 14 days old) or `INSUFFICIENT` (the supplier is unresolved). An indicator with incomplete data is labelled as such, never displayed as "LOW", so absence of data never reads as reassurance.

### 6.4 Worked example

Award A-1 (fictional): R742,500 from Organ of State Y to Supplier X by limited bidding, two days after tender close.

| # | Evidence | sᵢ | mᵢ | wᵢ·sᵢ·mᵢ |
|---|---|---|---|---|
| 1 | X's Organisation had a restriction that ended 2 years ago | 0.4 | 1.0 | 12.0 |
| 2 | Two other suppliers awarded by Y within 365 days share a director with X (n = 2), and one also shares a phone pseudonym | 0.667 | 1.2 | 12.0 |
| 3 | T = R750,000: (750,000 − 742,500)/75,000 = 0.1, so s_prox = 0.9 | 0.9 | 1.0 | 13.5 |
| 4 | d = 2 days, P10 = 10 days, so s_speed = 0.8. n₉₀ = 3, λ = 0.4: P(N ≥ 3) = 0.0079, so s_burst = 0.5 | 0.8 | 1.0 | 8.0 |
| 5 | `procurementMethod = limited` | 0.8 | 1.0 | 12.0 |
| 6 | No court source agreement: not computed | — | — | 0.0 |
| 7 | Y's latest opinion is QUALIFIED | 0.6 | 1.0 | 3.0 |
| | **R_raw = min(100, 60.5)** | | | **60.5** |

Result: **HIGH** (≥ 60), `data_sufficiency = PARTIAL` (component 6), no override. **Variant B:** the restriction is still active and confirmed, so s₁ = 1.0, R_raw = 78.5, and O1 applies. **Variant C:** an otherwise clean award with R_raw = 12.0 whose supplier matches an active restriction by exact CIPC number: O1 gives R = 60, band **HIGH**, `score_raw = 12.0`, and the explanation says why.

### 6.5 Explainability record

Each `RiskIndicator.explanation` is a struct, and its JSON form ships with the evidence pack (P19). It satisfies s71's "sufficient information about the underlying logic":

```json
{
  "model_version": "integrity-risk@1.0.0+sha256:5b0e…", "as_of": "2026-09-25",
  "subject_rid": "ri.objects.demo.object.Award.6f1c2a9e-4b7d-5e3a-9c21-7d8e9f0a1b2c",
  "components": [
    { "id": "threshold_proximity", "weight": 15, "sub_score": 0.9, "multiplier": 1.0, "contribution": 13.5,
      "reason": "Award value R742,500 is within 1% of the R750,000 RFQ threshold (register entry coct-rfq@2023-12-14).",
      "evidence": ["ri.objects.demo.object.Award.6f1c…", "ri.forge.demo.dataset-transaction.01926e5c…"] }
  ],
  "overrides": [], "score_raw": 60.5, "score": 60.5, "band": "HIGH", "data_sufficiency": "PARTIAL",
  "not_computed": [{ "id": "adverse_court_matters", "reason": "NO_SOURCE_AGREEMENT" }],
  "label": "RISK INDICATOR — NOT A FINDING", "human_review_required": true
}
```

---

## 7. Corridor analytics (P23 v0; P29, P30 deepening)

### 7.1 Anchorage wait-time distribution

- **Definition:** wait W = `berth_arrival_at` − `anchorage_arrival_at`, in hours.
  - Anchorage arrival is the first AIS position inside the anchorage polygon with speed over ground below 1 knot for at least 30 minutes.
  - Berth arrival is the first position inside a berth polygon below 0.5 knots for at least 15 minutes.
- **Estimator:** Kaplan–Meier survival Ŝ(t) = P(W > t), per segment g (terminal × vessel class), over a trailing 30-day window.
  - Vessels still waiting, or that left without berthing, are **right-censored**: excluding them would bias waits downwards.
  - If fewer than 20 completed calls exist, pool to port level.
- **Per vessel at anchorage** with elapsed wait e:
  - rank = 1 − Ŝ(e) ("waited longer than x% of comparable vessels");
  - conditional survival Ŝ(e + x)/Ŝ(e);
  - predicted berthing time = the smallest x at which the conditional survival reaches 0.5, linearly interpolated.
- **Worked example.** Ŝ(24) = 0.55, Ŝ(36) = 0.40, Ŝ(48) = 0.22, Ŝ(60) = 0.12. A vessel waiting 36 h ranks above 60% of its peers. P(W > 48 \| W > 36) = 0.55 and P(W > 60 \| W > 36) = 0.30. The median total is 48 + 12 × (0.55 − 0.5)/(0.55 − 0.30) = **50.4 h**, so about 14.4 h remain.
- **Calibration:** compared monthly against TNPA statistics. A difference above 15% raises a data-quality alert.

### 7.2 ETA shift under a wind forecast — **v0 heuristic, not validated**

- **Stoppage hours:** for a terminal with gust stop threshold G* (a per-terminal parameter, **confirm with the operator at P23**), H_stop is the set of forecast hours in the terminal's WeatherCell with gust ≥ G* within 48 h. Gaps shorter than 1 h are merged.
- **Shift:** a queued vessel k with predicted berthing time tₖ shifts by Δₖ = κ · |H_stop ∩ [now, tₖ]|, the stoppage hours before its turn.
  - κ = 1.0 for container terminals (cranes stop) and 0.5 for bulk; configurable.
  - Every PortCall shows `eta_shift_h`, labelled "v0 heuristic — not for contractual decisions".
- **Example.** Gusts ≥ G* from 14:00 to 19:00 tomorrow (5 h). A container vessel due at 16:00 shifts 2 h; a vessel due at 20:00 shifts 5 h, to 01:00.
- **Deepening (P30):** queue simulation and a mean-absolute-error backtest.

### 7.3 Corridor-segment incident risk (gamma–Poisson, exponential decay)

- **Decayed incident count** for segment j at time t: Nⱼ = Σᵢ σᵢ · cᵢ · exp(−(t − tᵢ)/τ).
  - Severity weight σ = 0.2 × severity (1–5 → 0.2–1.0); extraction confidence cᵢ; τ = 14 days (half-life ≈ 9.7 days).
  - Duplicates (same type within 2 km and 6 h) are merged first.
- **Exposure:** Eⱼ = Lⱼ · τ, in km-days.
- **Prior:** Gamma(α₀ = 2, β₀ = α₀/r̄), where r̄ is the corridor-wide rate per km-day, so the prior mean is r̄.
- **Posterior mean:** λ̂ⱼ = (α₀ + Nⱼ)/(β₀ + Eⱼ). Risk ratio RRⱼ = λ̂ⱼ / r̄.
- **Bands:** LOW if RR < 1.5; ELEVATED if 1.5 ≤ RR < 3; HIGH if RR ≥ 3. P(λⱼ > 2r̄) comes from the posterior gamma and is reported as confidence.
- **Worked example.** Segment N3 km 20–40: L = 20 km, r̄ = 0.002, so β₀ = 1,000 and E = 280. Three incidents:
  - 1 day old: σ = 1.0, c = 0.9, contribution 0.838;
  - 5 days old: σ = 0.6, c = 0.8, contribution 0.336;
  - 20 days old: σ = 1.0, c = 1.0, contribution 0.240.

  N = 1.413, λ̂ = 3.413/1,280 = 0.00267, **RR = 1.33: LOW** (P(λ > 2r̄) = 0.16). A protest extracted from the news today (σ = 1.0, c = 0.9) raises N to 2.313, so λ̂ = 0.00337 and **RR = 1.68: ELEVATED** (P = 0.30). This is demo step 4 in appendix 06 §6.

### 7.4 Dark-vessel matching (Sentinel-1 against AIS; P29)

1. For each radar detection d (time t_d, position x_d, estimated length ℓ_d), take AIS vessels with reports within ±30 minutes. Interpolate each position at t_d linearly, or dead-reckon from SOG/COG when only one side lies within 10 minutes.
2. **Gate:** the distance r between x_d and the interpolated position is at most 2 km (**confirm the SAR geolocation error at P29**).
3. **Score:** S = exp(−½(r/σᵣ)²) · exp(−½(Δt/σₜ)²) · exp(−½((ℓ_d − ℓᵥ)/(0.3ℓᵥ))²), with σᵣ = 500 m, σₜ = 10 min, and Δt the gap to the nearest AIS report. The length term is 1 if either length is unknown.
4. **Assignment:** Hungarian one-to-one assignment within the scene, keeping pairs with S ≥ 0.1.
5. **Dark:** an unassigned detection with ℓ_d ≥ 15 m, outside the exclusion masks (platforms, buoys, breakwaters), gets `dark = true`. It is labelled "unmatched radar detection — not an accusation", because vessels not obliged to carry AIS and shore-receiver gaps are both innocent explanations.

**Example:** r = 380 m, Δt = 4 min, ℓ_d = 182 m, ℓᵥ = 190 m gives S = 0.749 × 0.923 × 0.990 = **0.685**, a match.

---

## 8. Multi-model AI routing (P14, P15)

### 8.1 Tiers

Models are Apache-2.0 or MIT open weights only (ADR-0003; dossier §8), and there are **no external model APIs in the sovereign tier**.

| Tier | Use | Candidate families (Sep 2026) | Serving |
|---|---|---|---|
| T0 no-AI | Deterministic fallback for every workflow | — | — |
| T1 small/fast | Extraction, classification, NER, PII detection, routing | Ministral 3 (3B/8B/14B), gpt-oss-20b, Gemma 4 and Qwen3.x small variants (sizes: **confirm at P14**) | vLLM; llama.cpp at the edge |
| T2 medium | Agents, tool use, query planning | Qwen3.x (for example Qwen3.8-27B), gpt-oss-120b | vLLM |
| T3 large reasoning | Case summaries, referral drafting, long-document reasoning | gpt-oss-120b-class on the first 4× RTX PRO 6000 server (70–120B quantised, dossier §10). Mistral Large 3 (675B MoE) and DeepSeek-V4 (MIT) only on larger in-country clusters | vLLM |
| E embeddings | Semantic search, RAG, ER candidate generation (deepening) | BGE-M3 (dense output 1,024 per its model card), Qwen3-Embedding (dimension: **read from the model card at P14**) | vLLM or a dedicated embedding server |

### 8.2 Registry

- **Models:** id, family, licence (must be Apache-2.0 or MIT), weights SHA-256, quantisation, context window, tiers, eval results, and channel `dev → canary → stable → retired`.
- **Endpoints:** id, pod, enclave label σ_E (maximum classification, cleared markings, residency), hardware and concurrency.
- The registry **rejects any endpoint outside the deployment boundary**. In v1 it accepts only self-hosted endpoints.

### 8.3 Routing rules

`route(task, context, σ)` runs in order:
1. **Eligibility.** Endpoints E with C(ctx) ≤ C_max(E), M(ctx) ⊆ M_cleared(E), residency = the tenant pod, and the purpose's `permitted_methods` including the task's method (for example `LLM_SUMMARISE`) (W10). If none qualifies, refuse with `POLICY_DENIED` (reason `NO_CLEARED_MODEL`) and fall back to T0.
2. **Capability.** A task maps to a minimum tier: EXTRACT, CLASSIFY, NER and PII_DETECT → T1; AGENT and PLAN → T2; SUMMARISE_CASE, DRAFT_REFERRAL and LONG_REASONING → T3; EMBED → E.
3. **Evaluation gate.** Only models promoted to `stable` for that task.
4. **Selection** by latency budget, then cost (GPU-seconds per 1k tokens), then load. Escalating to a higher tier is allowed; falling to a lower tier is not, for quality-critical tasks.
5. **Context fit.** If the context exceeds the window: map-reduce with per-chunk citations, or a larger-window model within the same eligibility set.
6. **Assembly through Warden.** Invisible fields are never included. Content the *endpoint* is not cleared for is redacted or pseudonymised (`⟦PERSON_1⟧`). Third-party content is wrapped in `<untrusted_content>` (§9.6).

**Latency targets (p95):** T1 ≤ 1.5 s for prompts up to 2k tokens; T2 turn ≤ 8 s excluding tool execution; T3 draft ≤ 60 s (asynchronous allowed). Per-tenant quotas and metering: P14.

### 8.4 Evaluation gates (before `canary → stable`)

| Task | Gate |
|---|---|
| Document field extraction | F1 ≥ 0.90 on the SA evaluation set (P17 "Done when"); 100% of fields carry provenance |
| Classification / PII detection | Macro-F1 ≥ 0.85; recall ≥ 0.95 for `PII` and special classes |
| Agent tool use | ≥ 95% correct tool calls on scripted tasks; **zero unauthorised tool calls** on the injection suite (P15 "Done when") |
| Referral and summary drafting | 100% of sentences cited; ≥ 95% of citations supported (human-graded sample); **zero fabricated citations** |
| All | Redaction and refusal tests 100%; latency within budget; licence verified; weights hash and model card recorded; no regression of more than 1 point against the current stable model. Promotion is a two-person action |

### 8.5 Embeddings

- **Default v1:** BGE-M3 dense, 1,024 dimensions (re-read the model card at P14), cosine distance.
- pgvector indexes `vector` columns up to 2,000 dimensions. A larger native model dimension needs a documented reduced output dimension or a lake-only index (LanceDB).
- A vector property is bound to one model id and dimension. Changing the model means re-embedding into a new column; models are never mixed (DATA_MODEL §7).

### 8.6 No-AI mode (ARCHITECTURE §12)

| AI step | Deterministic fallback |
|---|---|
| Document extraction | Manual-entry queue with the page image and field templates |
| Referral drafting | Template filled with cited facts and indicator explanations |
| Agent | The Cockpit forms and object-set builder |
| Entity resolution | Rules-only exact-identifier linkage |
| Summaries | Not produced; the source records are shown instead |

---

## 9. Ingestion pipelines (Forge: P4, P11, P12, P17)

### 9.1 Modes

| Mode | Mechanism | v1 freshness |
|---|---|---|
| Batch | Dataset transactions: `SNAPSHOT` replaces the dataset view; `APPEND` adds rows; `UPDATE` upserts by PK; `DELETE` removes by PK (Iceberg deletion vectors). States OPEN → COMMITTED or ABORTED | Per source cadence (OCDS daily, restricted list weekly) |
| Micro-batch | Scheduled pulls → `APPEND` or `UPDATE` every N minutes | ≤ 60 s (ARCHITECTURE §9) |
| Streaming | Kafka topics → streaming funnel (P26) | ≤ 5 s target (P26) |

Vigil defines these transaction semantics itself, because Palantir's could not be verified (appendix 02 §2).

### 9.2 Keys and deduplication

- **Primary keys.** Every dataset destined for the ontology declares a stable PK: a surrogate, `<source>:<natural key>`, or a pseudonym. It never contains personal information (§1.3). Natural keys are kept as properties.
- **Content hash:** `content_sha256` = SHA-256 of the RFC 8785 (JCS) canonical JSON of the payload fields, excluding provenance fields. A re-fetched record with the same (PK, content hash) is a no-op. Changed content becomes an `UPDATE`.
- **File deduplication:** by file SHA-256 (P11.4). Documents are content-addressed (`doc:<sha256>`).
- **Idempotency.** Opening a transaction uses the key SHA-256(connector id ‖ cursor from ‖ cursor to). A retried window returns the same transaction, and commit is idempotent. Action calls use the client's `Idempotency-Key` (API_DESIGN).

### 9.3 Late data and watermarks

- **Event-time watermark** (data completeness): W = max event time seen − `allowed_lateness` (declared per source, for example OCDS 30 days and AIS 10 minutes). Records older than W go to a `late` partition and merge through the next `UPDATE`. **They are never dropped silently.**
- **Index watermark** (read-your-writes): a monotonic per-tenant token that the indexer emits in `objects.indexed` (ARCHITECTURE §6.1). The two watermarks are different things and are never conflated in APIs.

### 9.4 Data-quality contracts

- One YAML contract per dataset (Soda or GX, P12), checking:
  - schema;
  - PK not null and unique;
  - freshness;
  - volume band (for example ±30% against the 7-day median);
  - validity (for example ≥ 99% of SA IDs pass Luhn; CIPC formats);
  - referential integrity (for example `Award.tender_ocid` resolves).
- `BLOCK` severity aborts the transaction or halts downstream builds (P12 "Done when"). `WARN` only alerts. Results are emitted as OpenLineage facets.

### 9.5 Provenance on every record

Every row and object carries:

| Field | Meaning |
|---|---|
| `source_id` | Connector registry id |
| `source_record_id` | Natural id in the source |
| `connector_version` | — |
| `fetched_at` | — |
| `terms_ref` | Terms-register entry and version (P11.5) |
| `lawful_basis` | — |
| `responsible_party` | Originating organisation (§5.9) |
| `dataset_rid`, `dataset_txn_id` | — |
| `content_sha256` | — |

Also the propagated label ⟨M, C, REL, P⟩ and `data_origin` (REAL, PSEUDONYMISED, SYNTHETIC). **A record without a lawful basis, an explicit purpose-tag declaration (an empty set must be declared as `UNRESTRICTED`, never defaulted) and a terms reference is rejected at ingestion** (appendix 04 §1.2).

### 9.6 Untrusted content (documents, web, messages)

- All third-party text reaching a model is wrapped in `<untrusted_content id="…" source="…" retrieved_at="…" sha256="…">…</untrusted_content>`, with extraction-only system instructions (INSTRUCTIONS.md §9.5). The exact format and the system-prompt template are in SYSTEM_COMPLIANCE_GUARDRAILS §4.2–§4.3.
- Output is constrained to a JSON schema. Every field carries page, bounding box, method, model version and confidence (P17.2), and fields with confidence below 0.80 go to human validation (P17.4).
- Extracted text is never executed as instructions or tool calls, and extracted URLs are never fetched automatically.
- Model output cannot lower the label of its source (W5).

**Worked flow: the restricted-supplier PDF, weekly.**
1. Fetch; file SHA-256 dedupe.
2. `Document` object.
3. docai extraction with per-field provenance.
4. Validation (Luhn, CIPC format).
5. `SNAPSHOT` (the report is a full list) into `integrity.restrictions`, with rows for persons marked {PII}, C = 2.
6. Indexer.
7. ER, where person entries face the s57 gate.

---

## 10. Decision capture and lineage

A **decision record** is a projection that lets any outcome be explained later. Nothing in it is a separate source of truth.

| Element | Captured in | Content |
|---|---|---|
| The act | `action_run` | Action type and version, parameters, actor, `submitted_via`, purpose, case, justification, idempotency key |
| Human control | `approval_request`, `approval_decision` | Approver, decision, justification, time; separation of duties enforced |
| AI contribution | Agent turn trace (P15) + `action_run.agent_session_id` | Model id and version, tier, endpoint enclave, prompt hash, tool-call trace, citations, the staged parameters, and the **diff between what the agent proposed and what the human applied** (a measure of automation bias) |
| Evidence at decision time | `action_run.validation` + read audit events | Object versions read (RID, version), dataset transactions, index watermark, the risk-indicator explanation cited |
| Policy | Audit ledger | Warden decisions, policy version hash, context hash, obligations applied |
| Effects | `edit_log`, side-effect runs, webhook hashes | Edit sequence range, per-property before and after values, writeback request and response hashes, side-effect status |

- **Explain query.** Walk from a property value to its `edit_log` entry, to the `action_run`, then to the approvals and AI suggestion, the inputs read, the source dataset transactions, the connector runs, and finally the source documents (SHA-256). The Cockpit shows this chain (P13, P19), and evidence packs embed it (P19.4).
- **Invariants:** every edit has an `action_run_id`; every action run has a human or service principal and a purpose; every agent-originated run has `agent_session_id` and a model id; every approval has a justification; every decision event is in the hash-chained audit ledger.
- **Retention:** decision records live at least as long as the objects they changed, plus the tenant's statutory period (**confirm at P36**). Personal information inside them is erasable (DATA_MODEL §6.3), while the structural record (who, when, which action) survives erasure.

---

## 11. Confirm-later register and change control

| Item | Owner |
|---|---|
| CEL as the action expression language (§4.1 evaluation + ADR) | P8 |
| MISS text and the ladder mapping (§4.2) | P3, P41 |
| h3-pg extension licence and PostgreSQL 18/19 support (else H3 computed by writers) | P5 |
| CIPC suffix codes beyond 06/07/08/21/23; VAT checksum; CSD format; threshold-register values (PFMA/MFMA) | P22 |
| SA ID feature posture (C/A digits, date of birth) and juristic-person linkage posture | P16 with counsel |
| Clan-name equivalence table content; per-language rule sets; Jaro–Winkler p | P16 |
| Terminal gust thresholds G*; border-post codes | P23 |
| Sentinel-1 geolocation error for the 2 km gate | P29 |
| Model sizes (Gemma 4, Qwen3.x); Qwen3-Embedding dimension; BGE-M3 dimension re-check | P14 |
| Decision-record retention periods | P36 |

**Change control.** This document is frozen. A phase that finds it wrong stops and writes an ADR, and does not reinterpret it (PHASES Rule 10). A parameter marked "confirm at P<ID>" may be *set* by that phase and recorded in its PR, but its rule may not change without an ADR.
