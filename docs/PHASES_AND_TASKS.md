<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Master project plan: tiers, phases P0–P43, file ownership, conflict rules (P0) -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - First mission increment and recovery record -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Investigation increment scope and acceptance -->
<!-- Modified by Kone Tshivhinda (Qoder) on 2026-10-31 - Close P0 after the governance bootstrap merge -->
<!-- Modified by Kone Tshivhinda (Qoder) on 2026-10-31 - Record the connected operations integration -->

# Vigil — Phases & Tasks (Master Project Plan)

> **The master plan.** Every engineer and AI agent claims work here before touching code (INSTRUCTIONS.md §2, §7).
> **Status legend:** ⬜ Not started · 🚀 In progress · 🟡 Partial / blocked · ✅ Complete · 🧊 Frozen (blueprint)
> **Claim format:** `🚀 In Progress — <Engineer> (<AI model>) | Started: YYYY-MM-DD`

## How this plan works

1. **Tiers run in order, and phases inside Tiers 2 and 3 run in parallel** once their dependencies are met. **Exception:** the Phase Zero packs P22/P23 start as soon as their hard dependencies are ✅, mocking soft ones under Rule 4.
2. **Slice first.** Every phase has a **v1 slice**, the minimum that Phase Zero (P22/P23) needs, and a **full scope**. The v1 slice lands first. Deepening waits until a consumer needs it (Karpathy: simplicity first).
3. **"Done when"** is each phase's success criterion. A phase is ✅ only when its criteria are verified by tests, the local Docker CI is green, and the PR is merged to `develop`.
4. **Hard vs soft dependencies.** A hard dependency must be ✅ (at least its v1 slice). A soft dependency may be mocked under Rule 4.
5. **Timeline mapping** (see [dossier §15](research/VIGIL_RESEARCH_DOSSIER.md#15-the-36-month-roadmap)):

   | Window | Phases |
   |---|---|
   | Q4 2026 | P0–P5 |
   | Q1 2027 | P6–P13 + P22 |
   | Q2 2027 | P14–P21 + P23 |
   | H2 2027 | P24–P26 |
   | H1 2028 | P27–P31 |
   | H2 2028 | P32–P37 |
   | 2029 | P38–P43 |

## Phase index

> Status lives only on each phase card; claimants edit their card's **Status** line and nothing else here.

| Tier | Phase | Title | Complexity |
|---|---|---|---|
| 1 | P0 | Governance Bootstrap & Founding Research | Medium |
| 1 | P1 | Monorepo, Toolchains & Local Dev Environment | Medium |
| 1 | P2 | Contracts & Interop Foundation | Medium |
| 1 | P3 | Warden Security Kernel | Very High |
| 1 | P4 | Forge Datasets & Lakehouse | High |
| 1 | P5 | Core — Ontology Metadata Service | High |
| 1 | P6 | Core — Indexer (Funnel) | High |
| 1 | P7 | Core — Object Set Service & Traversal | High |
| 1 | P8 | Core — Actions, Edit Log & Write-back | Very High |
| 1 | P9 | Core — Functions Runtime | High |
| 1 | P10 | Cockpit Shell & Design System | Medium |
| 2 | P11 | Forge Connectors Framework | High |
| 2 | P12 | Forge Transforms, Pipelines & Data Quality | High |
| 2 | P13 | Cockpit — Object Views, Explorer & Action Forms | Medium |
| 2 | P14 | Mind — Model Gateway & Sovereign Serving | High |
| 2 | P15 | Mind — Ontology Tools, Agents, Automations & MCP | High |
| 2 | P16 | Mind — Entity Resolution Engine | Very High |
| 2 | P17 | Mind — Document AI & Extraction | High |
| 2 | P18 | Cockpit — Map/COP & Geospatial | Medium |
| 2 | P19 | Cockpit — Link Graph & Case Workbench | High |
| 2 | P20 | Relay v1 — Signed Bundles, Air-gap Install & CLI | High |
| 2 | P21 | Relay v2 — Hub/Spoke, Channels & Constraints | High |
| 3 | P22 | Integrity Pack — Phase Zero PZ-1 | High |
| 3 | P23 | Corridor Pack — Phase Zero PZ-2 (Durban Twin) | High |
| 3 | P24 | Enterprise Connectors & Write-back Targets | High |
| 3 | P25 | Edge Ingest Agent | High |
| 3 | P26 | Streaming Funnel & Real-time Objects | High |
| 3 | P27 | Vigil Field — Android App | High |
| 3 | P28 | Vigil Field — Comms Gateway | Medium |
| 3 | P29 | Earth Observation & Geospatial Feeds | Medium |
| 3 | P30 | Simulation & Optimisation | High |
| 3 | P31 | App Builder & SDK Generation | High |
| 4 | P32 | Performance & Load Testing | Medium |
| 4 | P33 | End-to-End & Disconnection Testing | Medium |
| 4 | P34 | Security Audit, AI Red-Teaming & Supply Chain | High |
| 4 | P35 | Container & Cluster Hardening | Medium |
| 4 | P36 | Compliance Evidence & Assurance | High |
| 4 | P37 | Sovereign Appliance & Air-gap Accreditation Build | High |
| 5 | P38 | SSO Federation & Cross-Org Peering | High |
| 5 | P39 | Multi-Country Sovereign Pods | High |
| 5 | P40 | Tamper-Evident Audit Verification & Oversight Portal | Medium |
| 5 | P41 | Classified Enclave Profile | Very High |
| 5 | P42 | Customer-Managed Keys (BYOK/HYOK) & Crypto-Shredding | High |
| 5 | P43 | Red-Line Conformance & Transparency Automation | Medium |

---

# Tier 1: Foundation (Sequential)

## P0: Governance Bootstrap & Founding Research
**Status**: ✅ Complete — Kone Tshivhinda (Qoder) | Started: 2026-09-25 | Closed: 2026-10-31 — D1–D12 signed off (MSG-043); governance bootstrap merged in PR #1 (MSG-044)
**Depends On**: —
**Blocks**: P1–P43
**Complexity**: Medium
**File Ownership**: `INSTRUCTIONS.md`, `README.md`, `docs/` (all blueprint and governance files; research; ADR-0001–0008), `scripts/validate-compliance.py`, `.github/` and root tooling files **for bootstrap only**
**Exclusion Zone**: No application code of any kind.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 0.1 | Founding research dossier | `docs/research/VIGIL_RESEARCH_DOSSIER.md` + 7 sourced appendices |
| 0.2 | Governance layer | INSTRUCTIONS, WORKLOG, BRANCHING_STRATEGY, AI_COMMUNICATION, this plan |
| 0.3 | Frozen blueprints | ARCHITECTURE, DOMAIN_BLUEPRINT, DATA_MODEL, API_DESIGN, FRONTEND/BACKEND guides |
| 0.4 | Safety layer | SECURITY_AND_COMPLIANCE, SYSTEM_COMPLIANCE_GUARDRAILS, LAUNCH_CHECKLIST, INCIDENT_RESPONSE |
| 0.5 | CI/CD & compliance tooling | 7 workflows, `docker-compose.ci.yml`, `validate-compliance.py`, pre-commit |
| 0.6 | ADRs 0001–0008 | Language charter, ontology ≠ graph DB, licences, sovereignty, Warden-first, codename, typed-table writers, Project Setup adaptations |
| 0.7 | Founder sign-off | D1–D12 (dossier §0), ADR-0001–0008 accepted, trademark search commissioned, Information Officer named |
| 0.8 | Remote & protections | Create the remote (dossier §18 Q7); push `main`, `develop` and the P0 branch; open the P0 PR; let every workflow run once; then enable the BRANCHING_STRATEGY §5 protections and select the required checks (they can only be selected after a first run) |

**Done when:** the founders have signed off D1–D12, the repository has been initialised with `main`/`develop`, and `feature/P0-governance-bootstrap` has been merged to `develop` by PR.

---

## P1: Monorepo, Toolchains & Local Dev Environment
**Status**: ⬜ Not Started
**Depends On**: P0
**Blocks**: P2–P43
**Complexity**: Medium
**File Ownership**: `justfile`, `.github/` (from P1 onward), `docker-compose.dev.yml`, `docker-compose.ci.yml`, `.pre-commit-config.yaml`, `.editorconfig`, `.gitattributes`, `.gitignore`, `scripts/ci/`, `scripts/validate-compliance.py` (after P0), `.semgrep/` (except `untrusted-content.yml`, P15), `templates/golden-paths/`, `infra/docker/`, `tools/`, `deploy/opentofu/`, `deploy/helm/platform/`, `deploy/helm/observability/`, root build and tool configs (`rust-toolchain.toml`, `deny.toml`, `.golangci.yml`, `.python-version`, `.prettierignore`, `settings.gradle.kts`, `pnpm-workspace.yaml`, `Cargo.toml` workspace, `go.work`, `pyproject.toml` workspace)
**Exclusion Zone**: `services/*/src/**` (no service code, only templates).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 1.1 | Toolchain pins | Java 25 + Gradle (Kotlin DSL), Node 24 LTS + pnpm + TS 7, Python 3.13 + uv, Rust stable (pinned `rust-toolchain.toml`), Go (pinned); lockfiles committed |
| 1.2 | `just` command surface | `just bootstrap`, `just lint`, `just test`, `just ci`, `just dev-up/down`; identical in CI |
| 1.3 | Local dev stack | `docker-compose.dev.yml`: Postgres 18 (PostGIS + pgvector), Valkey, Kafka (KRaft), NATS, OpenSearch, SeaweedFS, Polaris, Keycloak, OpenFGA, Temporal, OpenBao (dev KMS), ClickHouse |
| 1.4 | Golden-path templates | Service, library and job templates per language with OTel, health, config, SBOM, attribution header, Warden binding stub |
| 1.5 | Harden protections | CODEOWNERS, signed commits, `release/*` rulesets, Renovate/Dependabot branch and title conventions (the remote and base protections are P0 task 0.8) |
| 1.6 | Owned base images | `infra/docker/` base images per language (no Bitnami), rebuilt weekly, signed, pinned by digest |
| 1.7 | Remaining MUST gates | Wire the gates BACKEND_GUIDE lists as MUST: JaCoCo (100% for `services/forge-connectors`), `mypy --strict`, `cargo deny`, `llvm-cov`, `go test -race` (MSG-017) |

**Done when:** a new engineer runs `just bootstrap && just dev-up && just ci` on a clean machine and gets green checks in under 30 minutes, with every BACKEND_GUIDE MUST gate running (task 1.7).

---

## P2: Contracts & Interop Foundation
**Status**: ⬜ Not Started
**Depends On**: P1
**Blocks**: P3–P31
**Complexity**: Medium
**File Ownership**: `proto/buf.yaml`, `proto/buf.gen.yaml`, `proto/vigil/common/`, `proto/vigil/events/`, `proto/wit/common/`, `docs/asyncapi/`, `sdks/_codegen/`
**Exclusion Zone**: Domain packages `proto/vigil/<domain>/`. Each is owned by its domain's phase.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 2.1 | Buf workspace | Lint rules, `buf breaking` against `develop`, local generation for Java/TS/Python/Rust/Go/Kotlin |
| 2.2 | Common types | `ResourceId` (RID), `MarkingSet`, `PolicyContext`, `Purpose`, `PageRequest/PageInfo`, `VigilError`, `Watermark` |
| 2.3 | Event envelope | CloudEvents 1.0 + Protobuf payloads; `vigil.<domain>.<entity>.<verb>.v1` naming; AsyncAPI 3 catalogue |
| 2.4 | WIT worlds | `vigil:udf` and `vigil:policy` WASM component interfaces |
| 2.5 | SDK codegen harness | Skeleton that will generate ontology-scoped SDKs from OMS metadata (completed in P31) |

**Done when:** generated stubs compile in all six languages, `buf breaking` gates CI, and one sample event round-trips Java → Kafka → Python → Rust.

---

## P3: Warden Security Kernel
**Status**: ⬜ Not Started
**Depends On**: P2
**Blocks**: P4–P43
**Complexity**: Very High
**File Ownership**: `services/warden/`, `deploy/identity/` (Keycloak realm, OpenFGA model), `services/core/src/main/java/vigil/core/audit/`, `proto/vigil/warden/`, `deploy/helm/openbao/` (in-boundary KMS)
**Exclusion Zone**: Everything else. Other services consume Warden only through its bindings and APIs.
**Languages**: Rust (policy core), Java (audit module + FFM binding), Python (PyO3 binding), TypeScript/WASM (browser binding). Listed per INSTRUCTIONS §6 rule 2.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 3.1 | Marking model | Conjunctive markings, hierarchical classification, disjunctive releasability, PII/special-PI classes, customer-defined markings (see DOMAIN_BLUEPRINT §4) |
| 3.2 | Policy core (Rust) | `evaluate(policy_ctx, resource_markings) → Decision{allow, redactions, obligations}`, `propagate(inputs) → markings`; property tests (`proptest`) |
| 3.3 | Bindings | Java (FFM/Panama), Python (PyO3), TypeScript/browser (WASM core module; Component Model only where the host supports it). **One shared conformance suite** that every binding must pass identically. The Go binding (wazero) arrives with Go in P20 (task 20.6) |
| 3.4 | Identity & ReBAC | Keycloak realm (OIDC, MFA mandatory for privileged roles), OpenFGA model (orgs, spaces, projects, roles) |
| 3.5 | Purpose & justification | Purpose/case claims on tokens; justification prompts ("Checkpoints"); gated-module guard (authorisation reference IDs, expiry) |
| 3.6 | Pushdown compilers | Postgres RLS policy generator; OpenSearch DLS/FLS templates; scoped storage credential rules |
| 3.7 | Audit ledger | Append-only, hash-chained audit events (`audit.v1` schema), SIEM export |
| 3.8 | Key management v1 | In-boundary OpenBao (MPL-2.0) behind a PKCS#11-ready interface; tenant KEKs and dataset DEKs; `cipher_text` key channels (AES-256-GCM, AAD = tenant/dataset/RID/property); key ceremony for the policy-context signing key and audit signing |
| 3.9 | Tenancy & justifications | `tenancy.tenant` (DATA_MODEL §3.1, `V3_000`, applied first; P5 references it) and the encrypted, erasable `vigil_sec.justification` store (DATA_MODEL §4.2a) |

**Done when:**
- the conformance suite (at least 500 cases, including adversarial ones) passes in all 3 bindings (the Go binding joins in P20);
- an RLS-enforced table denies a user missing one of three markings;
- every decision emits a chained audit event;
- the DATA_MODEL security suite (MSG-023) passes on PostgreSQL 18, with the real extensions, for P3's tables.

---

## P4: Forge Datasets & Lakehouse
**Status**: ⬜ Not Started
**Depends On**: P3
**Blocks**: P6, P11, P12, P22–P26
**Complexity**: High
**File Ownership**: `services/forge-datasets/`, `deploy/helm/lakehouse/` (SeaweedFS, Polaris), `proto/vigil/forge/datasets/`, `services/core/src/main/java/vigil/core/lineage/`, `proto/vigil/lineage/`
**Exclusion Zone**: `services/core/`, `services/indexer/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 4.1 | Dataset & transaction API | Iceberg v3 tables; `SNAPSHOT/APPEND/UPDATE/DELETE`; OPEN/COMMITTED/ABORTED |
| 4.2 | Branch-ready datasets | v1: every dataset and transaction carries a branch key (`main`), and Iceberg tags work. Branch creation and merge are deepening, first needed by P30 (ARCHITECTURE A6) |
| 4.3 | Marking propagation | Every write calls Warden `propagate()`; markings stored as table properties + OpenLineage facet |
| 4.4 | Lineage events | OpenLineage run/dataset events; `vigil.forge.dataset.transaction.committed.v1` CloudEvent |
| 4.5 | Retention & schema evolution | Retention policies (POPIA minimality), column add/rename rules, deletion vectors for erasure |
| 4.6 | Lineage store | Core `lineage` module: enforced lineage graph (datasets → transforms → objects → Actions), marking-propagation records, decision-lineage API (DOMAIN_BLUEPRINT §10) |
| 4.7 | Dataset encryption | Per-dataset DEKs (AES-256-GCM) for Iceberg/S3 objects, wrapped by tenant KEKs from P3.8 |

**Done when:** a dataset can be written, branched, merged and read with inherited markings. A POPIA erasure request removes a row across snapshots, with an audited proof.

---

## P5: Core — Ontology Metadata Service (OMS)
**Status**: ⬜ Not Started
**Depends On**: P3
**Blocks**: P6–P9, P13, P15, P16, P22, P23, P31
**Complexity**: High
**File Ownership**: `services/core/` Modulith application skeleton, `services/core/src/main/java/vigil/core/ontology/`, `services/core/src/main/java/vigil/core/shared/`, `services/core/src/main/resources/db/migration/ontology/`, `proto/vigil/ontology/`, `packs/_schema/` (pack JSON Schema), `packs/_core/` (except `case/` and `scenario/`)
**Exclusion Zone**: `vigil/core/objects/`, `vigil/core/actions/`, `vigil/core/functions/`, `vigil/core/audit/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 5.1 | Type system | Object, link, interface, action and function types; property base types (scalar, array, struct, geopoint, geoshape, time series, geotemporal series, vector, media ref, cipher text, marking) |
| 5.2 | Versioning & branch-ready schemas | Immutable type versions; every schema record carries a branch key. Schema branch creation and merge are deepening (first consumer P30) |
| 5.3 | Physical DDL generator | Typed Postgres table per object type, join tables for M:N, RLS policies from Warden, OpenSearch mappings |
| 5.4 | Pack loader | YAML ontology packs validated against `packs/_schema`; dependency resolution between packs |
| 5.5 | Metadata API | gRPC/Connect + REST per API_DESIGN; change events `vigil.ontology.type.published.v1` |

**Done when:** loading `packs/_core` produces typed tables, indexes and RLS policies. A breaking type change is rejected unless it goes through a new version plus a migration plan. The DATA_MODEL security suite (MSG-023) passes on PostgreSQL 18 for the generated tables.

---

## P6: Core — Indexer (Funnel)
**Status**: ⬜ Not Started
**Depends On**: P4, P5
**Blocks**: P7, P22, P23, P26
**Complexity**: High
**File Ownership**: `services/indexer/`, `proto/vigil/indexer/`
**Exclusion Zone**: Typed-table DDL (P5). It *writes* rows; it never alters schemas.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 6.1 | Changelog | Diff source dataset transactions by primary key |
| 6.2 | Merge | Combine source changes with ontology edits under each property's conflict policy (`SOURCE_WINS`/`EDIT_WINS`/`LATEST_TIMESTAMP`) |
| 6.3 | Index | Bulk upsert into typed Postgres tables + OpenSearch documents, carrying markings |
| 6.4 | Hydrate & watermarks | Alias swaps, shadow-table full reindex when more than 80% of rows change, `vigil.indexer.objects.indexed.v1` watermark events |
| 6.5 | Backfill & replay | Rebuild any object type from the lakehouse + edit log |

**Done when:** 10M rows index at ≥ 20k rows/s per worker, and replaying from Iceberg + edit log reproduces byte-identical object state.

---

## P7: Core — Object Set Service & Traversal
**Status**: ⬜ Not Started
**Depends On**: P5, P6
**Blocks**: P8, P13, P15, P18, P19, P22, P23
**Complexity**: High
**File Ownership**: `services/core/src/main/java/vigil/core/objects/`, `services/traversal/`, `proto/vigil/objects/`
**Exclusion Zone**: `vigil/core/ontology/`, `services/indexer/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 7.1 | Object set algebra | Filter, union/intersect/subtract, search-around (≤ 3 hops), aggregations, ordering, pagination |
| 7.2 | Query compiler | To SQL (typed tables, `SET LOCAL vigil.policy_ctx`), OpenSearch DSL or traversal requests |
| 7.3 | Security pushdown & redaction | RLS/DLS enforcement; property-level redaction for column markings; read audit |
| 7.4 | Traversal service (Rust) | Arrow snapshots of link tables; k-hop neighbourhoods; shortest/all paths with in-process policy filtering |
| 7.5 | Read-your-writes | Watermark tokens; edit overlay for fresh Action results |

**Done when:** a filtered 1-hop query returns p95 ≤ 300 ms on 50M objects, and a policy-conformance test proves no unauthorised row, property or path ever leaks.

---

## P8: Core — Actions, Edit Log & Write-back
**Status**: ⬜ Not Started
**Depends On**: P7
**Blocks**: P9, P13, P15, P19, P22–P24, P27, P28
**Complexity**: Very High
**File Ownership**: `services/core/src/main/java/vigil/core/actions/`, `services/core/src/main/resources/db/migration/actions/`, `deploy/helm/temporal/`, `proto/vigil/actions/`
**Exclusion Zone**: `vigil/core/objects/` (read through the OSS interface only).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 8.1 | Action type runtime | Parameters, rules (create/modify/delete objects and links), submission criteria, validation |
| 8.2 | Writeback webhook | At most one per Action; synchronous, pre-commit; failure ⇒ nothing commits; ≤ 10 s. The `action_run` row is persisted first in its own transaction, so a retry with the same Idempotency-Key reuses its ID (and `X-Vigil-Action-Run-Id`) |
| 8.3 | Edit log + outbox | Single transaction: edits + `edit_log` + `outbox`; relay to the Kafka topic `<tenant>.ontology.edits.v1` as `vigil.ontology.edits.applied.v1` CloudEvents |
| 8.4 | Side-effect sagas | Temporal workflows: idempotent, retried, compensable; notifications |
| 8.5 | Human approval | `requires_human_approval` flag (POPIA s71); approver roles; agents may only stage |
| 8.6 | Limits & metering | 10k objects/action, 3 MB/edit, 60 s budget; usage metering per tenant |

**Done when:**
- chaos tests (webhook timeout, Kafka down, worker crash) never produce a partial commit or duplicate side effect;
- every edit is reconstructable from `edit_log`;
- the DATA_MODEL security suite (MSG-023) passes on PostgreSQL 18 for the `actions` tables.

---

## P9: Core — Functions Runtime
**Status**: ⬜ Not Started
**Depends On**: P8
**Blocks**: P15, P22, P23, P30, P31
**Complexity**: High
**File Ownership**: `services/functions-host/`, `services/core/src/main/java/vigil/core/functions/`, `proto/wit/functions/`, `proto/vigil/functions/`
**Exclusion Zone**: `vigil/core/actions/` (function-backed actions integrate through the Action runtime SPI).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 9.1 | WASM host | Wasmtime LTS, Component Model, fuel + epoch limits, no ambient I/O |
| 9.2 | TS & Python sandboxes | gVisor/Firecracker-class isolation; 60 s default, memory caps; SDK injected |
| 9.3 | Function-backed actions | Functions return ontology edits, applied through P8 |
| 9.4 | Metering & logs | Per-invocation CPU/memory accounting; OTel traces |

**Done when:** a hostile function (fork bomb, network egress, infinite loop) is contained and killed within limits, and a TS function-backed Action passes its E2E test.

---

## P10: Cockpit Shell & Design System
**Status**: ⬜ Not Started
**Depends On**: P2, P3
**Blocks**: P13, P18, P19, P31
**Complexity**: Medium
**File Ownership**: `apps/cockpit/` (root config), `apps/cockpit/src/app/layout.tsx`, `apps/cockpit/src/app/(shell)/`, `apps/cockpit/src/design/`, `apps/cockpit/src/components/ui/`, `apps/cockpit/src/lib/auth/`, `apps/cockpit/src/lib/api/`
**Exclusion Zone**: `apps/cockpit/src/features/**` (each owned by its feature phase).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 10.1 | App Router shell | Navigation, tenant/space switcher, "data as of" indicator, error boundaries |
| 10.2 | Auth | Keycloak OIDC (PKCE); purpose/case selector feeding `PolicyContext` |
| 10.3 | Design tokens | Colour/typography/spacing tokens, light/dark, **WCAG 2.1 AA** contrast verified |
| 10.4 | Component library | Tables, forms, dialogs, marking badges, justification prompt, redaction placeholder |
| 10.5 | API client | Generated Connect/TS client wrapper with policy-context headers |

**Done when:** a signed-in user sees an empty, accessible shell (axe: 0 serious violations). Every component shows how it renders a redacted property.

---

# Tier 2: Core Features (Parallel)

## P11: Forge Connectors Framework
**Status**: ⬜ Not Started
**Depends On**: P4
**Blocks**: P22, P23, P24
**Complexity**: High
**File Ownership**: `services/forge-connectors/` (framework, `connectors/jdbc/`, `connectors/rest/`, `connectors/file/`), `proto/vigil/forge/connectors/`
**Exclusion Zone**: Pack-specific connector folders `services/forge-connectors/connectors/<pack-source>/` (owned by P22, P23 and P24).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 11.1 | Connector base & registry | Abstract connector interface with category tags, `is_configured`, rate limiting, health, backoff |
| 11.2 | Credentials | KMS-backed secret references; never in config files; rotation hooks |
| 11.3 | JDBC & CDC | Batch JDBC + Debezium CDC into dataset transactions |
| 11.4 | REST & file | Paginated REST/JSON, file/PDF drop zones, checksum dedupe |
| 11.5 | Terms-of-use register | Every connector declares source licence/terms; blocked sources (e.g. SAFLII without an agreement) cannot be enabled |

**Done when:** a new REST source can be added in under 100 lines with rate limits, retries, provenance and a declared terms-of-use record.

## P12: Forge Transforms, Pipelines & Data Quality
**Status**: ⬜ Not Started
**Depends On**: P4
**Blocks**: P16, P17, P22, P23, P29
**Complexity**: High
**File Ownership**: `services/forge-transforms/`
**Exclusion Zone**: `services/forge-datasets/` (use its API only).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 12.1 | Transform SDK | Python `@transform` API over Polars/DuckDB (default) and PySpark (large); dataset I/O with automatic marking propagation |
| 12.2 | Orchestration | Dagster assets, schedules and sensors on dataset events |
| 12.3 | Incremental builds | Process only new transactions; checkpointing |
| 12.4 | Data quality | Soda/GX checks as data contracts; failing checks block downstream builds |
| 12.5 | Lineage | OpenLineage emission for every run |

**Done when:** a 3-stage incremental pipeline rebuilds only changed partitions, and a failed quality check halts downstream builds with an alert.

## P13: Cockpit — Object Views, Explorer & Action Forms
**Status**: ⬜ Not Started
**Depends On**: P7, P8, P10
**Blocks**: P19, P22, P31
**Complexity**: Medium
**File Ownership**: `apps/cockpit/src/app/(explore)/`, `apps/cockpit/src/features/objects/`, `apps/cockpit/src/features/actions/`
**Exclusion Zone**: `apps/cockpit/src/components/ui/` (request changes from P10 via 🔌 INTERFACE).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 13.1 | Search & explorer | Search across types, facets, object-set builder |
| 13.2 | Object pages | Properties, links, timeline/history, markings, provenance |
| 13.3 | Action forms | Generated from Action types; submission criteria feedback; approval state |
| 13.4 | Freshness | Watermark display; edit overlay |

**Done when:** an analyst finds an object, pivots to linked objects and submits an Action, all under policy, in the E2E test.

## P14: Mind — Model Gateway & Sovereign Serving
**Status**: ⬜ Not Started
**Depends On**: P3
**Blocks**: P15, P17
**Complexity**: High
**File Ownership**: `services/mind/gateway/`, `deploy/helm/mind-serving/`, `proto/vigil/mind/gateway/`
**Exclusion Zone**: `services/mind/agents/`, `services/mind/er/`, `services/mind/docai/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 14.1 | Gateway | LiteLLM-based routing; model registry (Apache/MIT models only); per-tenant quotas and metering |
| 14.2 | Serving | vLLM on RTX PRO 6000 nodes; llama.cpp profile for edge; offline model registry |
| 14.3 | Marking-aware redaction | Prompts and contexts filtered by Warden; refusal above clearance |
| 14.4 | Sovereignty egress policy | Deny-by-default egress; "no prompt leaves SA" enforced and tested |
| 14.5 | Tracing & evals | Langfuse + OTel; Inspect/promptfoo harness; model-vs-model comparisons |

**Done when:** gpt-oss or Qwen serves at the target latency on-prem, an egress test proves zero external calls, and redaction tests pass.

## P15: Mind — Ontology Tools, Agents, Automations & MCP
**Status**: ⬜ Not Started
**Depends On**: P7, P8, P9, P14
**Blocks**: P22, P23, P28, P31
**Complexity**: High
**File Ownership**: `services/mind/agents/`, `proto/vigil/mind/agents/`, `.semgrep/untrusted-content.yml`
**Exclusion Zone**: `services/mind/gateway/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 15.1 | Tool layer | Object-set queries, Functions and Actions as tools, executed **as the user** with the same PolicyContext |
| 15.2 | Ontology MCP server | Exposes app-scoped ontology resources as MCP tools, bounded by user permissions |
| 15.3 | Approval staging | Agents stage writes; `agent_executable` Actions only; human sign-off otherwise |
| 15.4 | Automations | Time, data-condition and stream triggers → Functions/Actions/notifications |
| 15.5 | Injection defence | `<untrusted_content>` boundaries; tool-result sanitising; red-team eval suite |
| 15.6 | Boundary lint | Semgrep rule `.semgrep/untrusted-content.yml` flagging prompt assembly that concatenates third-party text without the wrapper; wired into `security-scan.yml` through 🔌 INTERFACE with P1 |

**Done when:** an agent answers an ontology question with record-level citations and stages a referral Action that a human approves. The injection suite shows no unauthorised tool call.

## P16: Mind — Entity Resolution Engine
**Status**: ⬜ Not Started
**Depends On**: P5, P12
**Blocks**: P22
**Complexity**: Very High
**File Ownership**: `services/mind/er/`, `packs/_shared/er-rules/`, `proto/vigil/er/`
**Exclusion Zone**: Pack ontologies (they consume ER output only).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 16.1 | Splink pipeline | Blocking rules, Fellegi-Sunter models, DuckDB/Spark backends |
| 16.2 | African-name comparators | Name order, clan/patronymic forms, transliteration, per-language phonetics, initials (DOMAIN_BLUEPRINT §5) |
| 16.3 | Clusters as views | Non-destructive resolved entities with per-link provenance and confidence; reversible |
| 16.4 | s57 linkage lock | Cross-party person linkage disabled until an Information Regulator authorisation reference is recorded |
| 16.5 | Review queue & metrics | Human review API; precision/recall on a labelled SA evaluation set |

**Done when:** org-level ER reaches ≥ 0.95 precision / ≥ 0.90 recall on the evaluation set, and person-level cross-party linkage demonstrably refuses to run without an authorisation ID.

## P17: Mind — Document AI & Extraction
**Status**: ⬜ Not Started
**Depends On**: P12, P14
**Blocks**: P22, P23
**Complexity**: High
**File Ownership**: `services/mind/docai/`, `proto/vigil/docai/`
**Exclusion Zone**: `services/mind/gateway/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 17.1 | Pipeline | Docling + PaddleOCR 3 (olmOCR 2 on GPU) for PDFs and scans |
| 17.2 | Field provenance | Every extracted field keeps page, bounding box, method and confidence |
| 17.3 | Untrusted content | Documents treated as untrusted input; boundary wrapping; no instruction following |
| 17.4 | Human validation | Low-confidence fields routed to review |
| 17.5 | SA evaluation set | Gazettes, tender documents, restricted-supplier PDF, manifests |

**Done when:** field-level F1 ≥ 0.9 on the SA evaluation set, and 100% of fields carry provenance.

## P18: Cockpit — Map/COP & Geospatial
**Status**: ⬜ Not Started
**Depends On**: P7, P10
**Blocks**: P23, P29
**Complexity**: Medium
**File Ownership**: `apps/cockpit/src/app/(map)/`, `apps/cockpit/src/features/map/`, `deploy/helm/geo/` (PMTiles, TiTiler)
**Exclusion Zone**: `apps/cockpit/src/features/objects/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 18.1 | Map engine | MapLibre + deck.gl; object-set layers; H3 aggregation |
| 18.2 | Offline basemaps | Protomaps PMTiles bundles; ODbL attribution; separate licensed layers |
| 18.3 | Geo filters | Draw-to-filter, geofences, proximity |
| 18.4 | Time | Timeline scrubber over time-series and geotemporal properties |

**Done when:** the map runs fully offline from a PMTiles bundle, and 100k objects render at ≥ 30 fps.

## P19: Cockpit — Link Graph & Case Workbench
**Status**: ⬜ Not Started
**Depends On**: P7, P8, P13
**Blocks**: P22
**Complexity**: High
**File Ownership**: `apps/cockpit/src/app/(cases)/`, `apps/cockpit/src/features/graph/`, `apps/cockpit/src/features/cases/`, `packs/_core/case/`, `proto/vigil/cases/`
**Exclusion Zone**: `services/traversal/` (call its API).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 19.1 | Link graph | Sigma.js (WebGL) expansion, filtering and path finding through traversal |
| 19.2 | Case objects | `Case`, `EvidenceItem`, `Finding` types + Actions (open, attach, refer, close) |
| 19.3 | Evidence chain | Hashing, custody log and justification per step |
| 19.4 | Evidence pack export | Signed PDF/ZIP with source citations and hashes; "risk indicator, not finding" labelling |

**Done when:** an analyst builds a case from graph pivots and exports a signed evidence pack whose hashes verify independently.

## P20: Relay v1 — Signed Bundles, Air-gap Install & CLI
**Status**: ⬜ Not Started
**Depends On**: P1, P3
**Blocks**: P21, P37
**Complexity**: High
**File Ownership**: `services/relay/bundler/`, `services/relay/cmd/vigil/`, `services/relay/internal/common/`, `services/relay/internal/warden/`, `deploy/zarf/`, `deploy/kyverno/verify-images/`
**Exclusion Zone**: `services/relay/hub/`, `services/relay/spoke/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 20.1 | Bundles | Zarf package per product; images + charts + SBOM (Syft) + cosign signatures |
| 20.2 | Registry sync | Harbor on each site; mirrored images; no public pulls |
| 20.3 | Admission | Kyverno verifies signatures; unsigned images rejected |
| 20.4 | `vigil` CLI | `vigil install/upgrade/status/bundle` |
| 20.5 | Air-gap runbook | One-way import procedure; tested on a disconnected lab |
| 20.6 | Warden Go binding | wazero-hosted Warden policy core for Go services; must pass the shared P3 conformance suite |

**Done when:** a full Vigil stack installs on a disconnected 3-node lab from one signed bundle in under 2 hours, and an unsigned image is rejected.

## P21: Relay v2 — Hub/Spoke, Channels & Constraints
**Status**: ⬜ Not Started
**Depends On**: P20
**Blocks**: P37, P39
**Complexity**: High
**File Ownership**: `services/relay/hub/`, `services/relay/spoke/`, `proto/vigil/relay/`
**Exclusion Zone**: `services/relay/bundler/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 21.1 | Plans | Hub orchestration engine issues plans; spokes poll and report state and telemetry |
| 21.2 | Channels | `dev → canary → stable → lts` promotion; recall on failed health checks |
| 21.3 | Constraints | Maintenance windows, dependency ranges, suppression after failures |
| 21.4 | Rollback | Automatic rollback plans; audit of every change |
| 21.5 | Multi-hub | Hub → hub chains (sovereign → customer → edge) |

**Done when:** a canary failure is automatically recalled and rolled back across 3 spokes inside a maintenance window, with a full audit trail.

---

# Tier 3: Integrations & External Feeds (Parallel)

## P22: Integrity Pack — Phase Zero PZ-1
**Status**: ⬜ Not Started
**Depends On**: P6, P7, P8, P11, P13 (hard); P12, P15, P16, P17, P19 (soft — mock per Rule 4)
**Blocks**: P32, P36
**Complexity**: High
**File Ownership**: `packs/integrity/`, `services/forge-connectors/connectors/{ocds-etenders,treasury-restricted,municipal-money,coct-awards,cipc-extract}/`
**Exclusion Zone**: Connector framework (`services/forge-connectors/` outside the listed folders), ER engine internals.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 22.1 | Ontology | `OrganOfState`, `Tender`, `Award`, `Contract`, `Supplier`, `Person`, `Identifier`, `Restriction`, `CourtMatter`, `AuditFinding`, `Place` + links (dossier §11) |
| 22.2 | Connectors | eTenders OCDS (daily delta), Treasury restricted-supplier PDF (weekly, via P17), Municipal Money (quarterly), CoCT awards (monthly), CIPC extract loader. `CourtMatter` stays empty until a written agreement with SAFLII/LawLibrary exists (MSG-006); the v1 demo uses synthetic court records |
| 22.3 | Indicators | Threshold proximity, director overlap, restricted linkage, award velocity: **risk indicators, not findings** |
| 22.4 | Referral workflow | `Refer` Action → case + watch-list + notification (WhatsApp mocked until P28) |
| 22.5 | Data-rights register | Terms per source; SAFLII/LawLibrary only under agreement; person identifiers pseudonymised in demos |
| 22.6 | Demo & evaluation | 3-minute scripted demo; precision review with counsel |

**Done when:** the live demo runs end to end on public data with every indicator cited to source records, and counsel signs off the POPIA posture of the demo dataset.

## P23: Corridor Pack — Phase Zero PZ-2 (Durban Port & Corridor Twin)
**Status**: ⬜ Not Started
**Depends On**: P6, P7, P8, P11, P18 (hard); P12, P15, P17 (soft)
**Blocks**: P26, P29, P30, P32
**Complexity**: High
**File Ownership**: `packs/corridors/`, `services/forge-connectors/connectors/{ais,eskomsepush,gdelt,open-meteo,sarb,tnpa}/`
**Exclusion Zone**: Map engine internals (P18).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 23.1 | Ontology | `Vessel`, `PortCall`, `Anchorage`, `Berth`, `Terminal`, `SatelliteDetection` (defined here, populated from P29), `WeatherCell`, `CorridorSegment`, `BorderPost`, `GridArea`, `Incident` |
| 23.2 | Feeds | AIS (aisstream + own Durban receiver → AISHub), Open-Meteo (commercial plan), EskomSePush, GDELT incidents (via P17), SARB FX, TNPA monthly calibration |
| 23.3 | Wait-time model v0 | Anchorage wait distribution + ETA shift under wind forecasts |
| 23.4 | Slot advisory Action | Revised advisories to transporters (CSV/WhatsApp, mocked until P28) |
| 23.5 | Demo | 3-minute scripted live demo |
| 23.6 | Track storage | ClickHouse `geo_track_points` (DATA_MODEL §11) in `packs/corridors/clickhouse/` for AIS tracks; P26 extends it for streaming |

**Done when:** a live map shows vessels at anchorage against 30-day wait distributions, and one advisory Action completes end to end.

## P24: Enterprise Connectors & Write-back Targets
**Status**: ⬜ Not Started
**Depends On**: P8, P11
**Blocks**: P36
**Complexity**: High
**File Ownership**: `services/forge-connectors/connectors/{sap,pi-web,opcua-historian,maximo}/`, `packs/erp-sap/`
**Exclusion Zone**: Connector framework core.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 24.1 | SAP | OData/RFC read; PM work-order write-back webhook (licence terms for SAP connectors checked under ADR-0003) |
| 24.2 | OSIsoft PI / AVEVA | PI Web API time-series ingestion |
| 24.3 | OPC UA historians | Eclipse Milo client |
| 24.4 | Maximo / EAM | Work orders read/write |
| 24.5 | ERP pack | Asset, FunctionalLocation, WorkOrder, Notification types + Actions |

**Done when:** a Cockpit Action creates a real SAP PM work order in a sandbox through the pre-commit webhook, with failure causing zero partial commits.

## P25: Edge Ingest Agent
**Status**: ⬜ Not Started
**Depends On**: P2, P3, P4
**Blocks**: P26, P37
**Complexity**: High
**File Ownership**: `edge/agent/`, `proto/vigil/edge/`
**Exclusion Zone**: `services/**`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 25.1 | Protocols | OPC UA (open62541 via FFI), Modbus, MQTT/Sparkplug B, serial, file tail |
| 25.2 | Store-and-forward | Disk-backed Parquet/SQLite spool; ordered replay; backpressure |
| 25.3 | NATS leaf | Leaf-node bridging; mTLS; intermittent links |
| 25.4 | Signed config & OTA | Configuration signed by Relay; atomic A/B upgrades |
| 25.5 | Footprint | ARM64 static binary ≤ 30 MB RSS idle |

**Done when:** 8 hours of disconnection (v1; 72 hours is the Tier 4 target, P33) replay with zero loss or duplication, and the agent survives power-cut tests.

## P26: Streaming Funnel & Real-time Objects
**Status**: ⬜ Not Started
**Depends On**: P6, P23, P25
**Blocks**: P32
**Complexity**: High
**File Ownership**: `services/stream-funnel/`, `proto/vigil/stream/`
**Exclusion Zone**: `services/indexer/` (the batch funnel stays P6's).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 26.1 | Flink job | Kafka → keyed by PK/event time → typed tables/OpenSearch; exactly-once |
| 26.2 | Freshness SLOs | ≤ 60 s v1 → ≤ 5 s target |
| 26.3 | Time-series sink | ClickHouse for high-rate telemetry, linked to objects |

**Done when:** AIS positions reach object state in ≤ 5 s p95 under replayed Durban peak load.

## P27: Vigil Field — Android App
**Status**: ⬜ Not Started
**Depends On**: P2, P8
**Blocks**: P33
**Complexity**: High
**File Ownership**: `edge/field-app/`
**Exclusion Zone**: `apps/cockpit/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 27.1 | Offline cache | Encrypted, markings-filtered object cache; read sync |
| 27.2 | Queued Actions | Signed Action requests queued offline; server-side conflict semantics |
| 27.3 | Maps & forms | MapLibre + PMTiles; dynamic forms from Action types; photo/evidence capture with hashes |
| 27.4 | Low-end devices | Runs on 3 GB RAM Android; data-saver mode |

**Done when:** a crew completes 20 Actions offline over 8 hours, syncs, and every Action is applied or rejected with a reason.

## P28: Vigil Field — Comms Gateway
**Status**: ⬜ Not Started
**Depends On**: P8, P15
**Blocks**: P33
**Complexity**: Medium
**File Ownership**: `services/comms/`, `proto/vigil/comms/`
**Exclusion Zone**: `services/mind/**`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 28.1 | ADR: Elixir vs Go | Decide per ADR-0001 criteria (talent, scale, ops) |
| 28.2 | Channels | WhatsApp Business Cloud API (business-owned channels only), SMPP SMS, USSD via local aggregator |
| 28.3 | Presence & escalation | On-call rotas, acknowledgement, escalation ladders |
| 28.4 | Consent & logging | Opt-in records; POPIA s69 direct-marketing rules respected; no content outside the lawful basis |

**Done when:** an alert escalates across WhatsApp → SMS → USSD with acknowledgements recorded as ontology events.

## P29: Earth Observation & Geospatial Feeds
**Status**: ⬜ Not Started
**Depends On**: P12, P18, P23
**Blocks**: P32
**Complexity**: Medium
**File Ownership**: `services/eo/`, `proto/vigil/eo/`
**Exclusion Zone**: `services/forge-transforms/` core.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 29.1 | Catalogues | STAC over Copernicus Data Space and Digital Earth Africa; TiTiler |
| 29.2 | Dark-vessel detection | Sentinel-1 SAR detections matched against AIS |
| 29.3 | Fires & water | NASA FIRMS near linear assets; DE Africa water observations |

**Done when:** Sentinel-1 detections near Durban anchorage appear as `SatelliteDetection` objects linked to AIS matches.

## P30: Simulation & Optimisation
**Status**: ⬜ Not Started
**Depends On**: P9, P23
**Blocks**: P32
**Complexity**: High
**File Ownership**: `services/sim/`, `packs/_core/scenario/`, `proto/vigil/sim/`
**Exclusion Zone**: Pack ontologies (write scenario objects only).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 30.1 | Scenario engine | What-if runs on ontology branches; results as `Scenario` objects |
| 30.2 | Optimisation | OR-Tools/HiGHS, VROOM (routing), Timefold where JVM scheduling fits |
| 30.3 | Models | Corridor wait-time/ETA, crew dispatch |

**Done when:** a dispatcher compares two scenarios side by side and promotes one into an Action.

## P31: App Builder & SDK Generation
**Status**: ⬜ Not Started
**Depends On**: P9, P13, P15
**Blocks**: P32
**Complexity**: High
**File Ownership**: `apps/cockpit/src/app/(builder)/`, `apps/cockpit/src/features/builder/`, `sdks/` (generators and outputs), `proto/vigil/builder/`
**Exclusion Zone**: `apps/cockpit/src/components/ui/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 31.1 | Builder | Low-code pages from widgets bound to object sets, Functions and Actions |
| 31.2 | SDK generation | TS/Python/Java/Kotlin SDKs scoped to an app's ontology subset (OSDK-equivalent) |
| 31.3 | Publishing | Internal package registry; versioned per ontology version |

**Done when:** a non-engineer builds a working triage app in under a day, and a generated TS SDK drives it.

---

# Tier 4: Enterprise Scale & Production Readiness (Sequential)

## P32: Performance & Load Testing
**Status**: ⬜ Not Started
**Depends On**: P22, P23, P26, P29, P30, P31
**Blocks**: P33
**Complexity**: Medium
**File Ownership**: `tests/load/`, `docs/reviews/SCALE_AUDIT.md`
**Exclusion Zone**: Service code (file 🐛 BUG entries; don't patch).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 32.1 | Load suites | k6/Gatling for OSS, Actions, indexer, Mind |
| 32.2 | Capacity model | Validate ARCHITECTURE §9 targets; publish headroom |
| 32.3 | Scale audit | `SCALE_AUDIT.md` with regressions and fixes |

**Done when:** every ARCHITECTURE §9 Tier 4 target is either met or has an owner-assigned plan to meet it.

## P33: End-to-End & Disconnection Testing
**Status**: ⬜ Not Started
**Depends On**: P27, P28, P32
**Blocks**: P34
**Complexity**: Medium
**File Ownership**: `tests/e2e/`, `tests/chaos/`
**Exclusion Zone**: Service code.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 33.1 | Playwright suites | Cockpit journeys for both packs |
| 33.2 | Chaos & disconnection | Network partitions, WAN cuts, power loss, Kafka/indexer outages |
| 33.3 | Field journeys | Android offline → sync scenarios |

**Done when:** all critical journeys pass nightly, including the 8-hour disconnected field scenario.

## P34: Security Audit, AI Red-Teaming & Supply Chain
**Status**: ⬜ Not Started
**Depends On**: P33
**Blocks**: P35, P36
**Complexity**: High
**File Ownership**: `tests/security/`, `docs/reviews/SECURITY_AUDIT.md`
**Exclusion Zone**: `.github/` (request SLSA additions from the P1 owner via 🔌 INTERFACE).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 34.1 | External pentest | Scope: gateway, core, Mind, Relay, field app |
| 34.2 | AI red team | Prompt injection, data exfiltration via tools, marking bypass attempts |
| 34.3 | Supply chain | SLSA L3 provenance, dependency pinning, SBOM diffing |
| 34.4 | Policy fuzzing | Differential fuzzing of Warden bindings |

**Done when:** no open critical or high findings remain, and the Warden differential fuzzing runs for 72 hours without divergence.

## P35: Container & Cluster Hardening
**Status**: ⬜ Not Started
**Depends On**: P34
**Blocks**: P37
**Complexity**: Medium
**File Ownership**: `deploy/hardening/`
**Exclusion Zone**: Product charts (propose values through 🔌 INTERFACE).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 35.1 | CIS & FIPS | RKE2 CIS profile, FIPS mode, Pod Security Admission restricted |
| 35.2 | Network | Default-deny NetworkPolicies, mTLS mesh |
| 35.3 | Runtime | Falco-class detection, read-only root filesystems, non-root users |

**Done when:** the CIS benchmark passes with documented exceptions only.

## P36: Compliance Evidence & Assurance
**Status**: ⬜ Not Started
**Depends On**: P22, P24, P34
**Blocks**: P37
**Complexity**: High
**File Ownership**: `compliance/`, `docs/compliance/`
**Exclusion Zone**: Service code.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 36.1 | ISO/IEC 27001 | ISMS, Statement of Applicability, automated evidence |
| 36.2 | SOC 2 | Type I then Type II control evidence |
| 36.3 | POPIA packs | s57 prior-authorisation pack template, PIA template, s21 operator DPA, breach runbooks |
| 36.4 | ISO/IEC 42001 | AI management system; model cards; impact assessments |

**Done when:** the external auditor's stage-1 readiness review reports no major non-conformities.

## P37: Sovereign Appliance & Air-gap Accreditation Build
**Status**: ⬜ Not Started
**Depends On**: P21, P25, P35, P36
**Blocks**: P38
**Complexity**: High
**File Ownership**: `deploy/appliance/`
**Exclusion Zone**: `services/relay/**`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 37.1 | Talos appliance | 1–3 node image with an AI node; secure boot; TPM-sealed keys |
| 37.2 | Offline registries | Images, models, map tiles, docs |
| 37.3 | DR | Backup/restore; site-to-site replication for sovereign DC pairs |

**Done when:** the appliance goes from factory-fresh to a working Vigil with Phase Zero packs, fully offline, in under 4 hours.

---

# Tier 5: Post-Launch Hardening & Gaps (Sequential)

## P38: SSO Federation & Cross-Org Peering
**Status**: ⬜ Not Started
**Depends On**: P37
**Blocks**: P39
**Complexity**: High
**File Ownership**: `services/core/src/main/java/vigil/core/peering/`, `deploy/identity-federation/`
**Exclusion Zone**: `deploy/identity/` (P3).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 38.1 | Federation | SAML/OIDC federation with customer IdPs (AD/Entra/LDAP) |
| 38.2 | Peering | Share ontology subsets across tenants with marking mapping and revocation |

**Done when:** two tenants share a scoped object set with mapped markings, and revocation takes effect in ≤ 60 s.

## P39: Multi-Country Sovereign Pods
**Status**: ⬜ Not Started
**Depends On**: P21, P38
**Blocks**: P40
**Complexity**: High
**File Ownership**: `deploy/pods/`, `docs/compliance/countries/`
**Exclusion Zone**: Product code.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 39.1 | Pod template | In-country hosting, keys, local operator, DPA registration |
| 39.2 | First pods | Botswana, Namibia or Mozambique (Maputo corridor), then Kenya (Reg 26) |
| 39.3 | Aggregates only | Cross-border flows limited to non-personal aggregates |

**Done when:** a second-country pod runs with zero cross-border personal data, verified by an egress audit.

## P40: Tamper-Evident Audit Verification & Oversight Portal
**Status**: ⬜ Not Started
**Depends On**: P39
**Blocks**: P41–P43
**Complexity**: Medium
**File Ownership**: `services/audit-verifier/`, `apps/oversight/`
**Exclusion Zone**: `services/core/**/audit/` (read only).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 40.1 | External anchoring | Periodic Merkle roots anchored outside the deployment |
| 40.2 | Verifier CLI | Independent verification of chain integrity |
| 40.3 | Oversight portal | Read-only access for the independent oversight board |

**Done when:** tampering with any audit record is detected by the verifier.

## P41: Classified Enclave Profile
**Status**: ⬜ Not Started
**Depends On**: P40
**Blocks**: —
**Complexity**: Very High
**File Ownership**: `deploy/enclave/`
**Exclusion Zone**: Product code (profile/config only).

### Tasks
| # | Task | Description |
|---|------|-------------|
| 41.1 | MISS profile | Classification handling; cleared-staff operations; physical controls |
| 41.2 | Gated modules | Warrant-reference enforcement for every gated capability |

**Done when:** an accreditation authority accepts the enclave profile for a named customer.

## P42: Customer-Managed Keys (BYOK/HYOK) & Crypto-Shredding
**Status**: ⬜ Not Started
**Depends On**: P40
**Blocks**: —
**Complexity**: High
**File Ownership**: `services/keys/`, `deploy/hsm/`
**Exclusion Zone**: `services/warden/`.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 42.1 | Customer KEKs | Tenant KEKs held in the customer's HSM (PKCS#11), replacing P3.8's Vigil-managed KEKs; DEKs unchanged |
| 42.2 | HYOK | Keys never leave the customer HSM |
| 42.3 | Crypto-shredding | Retention expiry by key destruction, with an audit |

**Done when:** revoking a customer KEK renders the tenant's data unreadable within the documented window.

## P43: Red-Line Conformance & Transparency Automation
**Status**: ⬜ Not Started
**Depends On**: P40
**Blocks**: —
**Complexity**: Medium
**File Ownership**: `tests/redlines/`, `tools/transparency-report/`
**Exclusion Zone**: Service code.

### Tasks
| # | Task | Description |
|---|------|-------------|
| 43.1 | Red-line tests | Automated tests proving prohibited capabilities are absent or blocked (GUARDRAILS §2) |
| 43.2 | Transparency report | Annual report generation: deployments, legal demands, refusals, incidents |

**Done when:** the red-line test suite is part of release gating, and the first transparency report is published.

---

# File Ownership Master Index

> One path, one owner (Rule 1). "Shared, append-only" files follow Rule 2. When a path isn't listed, the nearest listed parent decides. If you still can't tell, ask in `AI_COMMUNICATION.md` before editing.

| Path | Owner | Notes |
|---|---|---|
| `INSTRUCTIONS.md`, `README.md` | P0 | Changes by lead architect via `chore/` PR |
| `WORKLOG.md` | **Shared, append-only** | Updated before every commit |
| `docs/AI_COMMUNICATION.md` | **Shared, append-only** | Status field of existing rows may change |
| `docs/API_DESIGN.md` | **Shared, append-only** | New endpoints appended by the owning phase |
| `docs/PHASES_AND_TASKS.md` | P0 | Claimants may edit **only** their phase's Status line |
| `docs/ARCHITECTURE.md`, `docs/VIGIL_DOMAIN_BLUEPRINT.md`, `docs/DATA_MODEL.md` | P0 🧊 | Frozen; ADR required |
| `docs/FRONTEND_GUIDE.md`, `docs/BACKEND_GUIDE.md`, `docs/BRANCHING_STRATEGY.md` | P0 | ADR for material changes |
| `docs/SECURITY_AND_COMPLIANCE.md`, `docs/SYSTEM_COMPLIANCE_GUARDRAILS.md` | P0 | Changes need lead + counsel review |
| `docs/LAUNCH_CHECKLIST.md`, `docs/INCIDENT_RESPONSE.md` | P0 | — |
| `docs/adr/` | **Shared, append-only** | New ADR files only; superseding, never editing, accepted ADRs |
| `docs/research/` | P0 | — |
| `docs/reviews/` | Lead architect (tier audits), P32 (`SCALE_AUDIT.md`), P34 (`SECURITY_AUDIT.md`), incident commanders (post-mortems) | One file per audit (see `docs/reviews/README.md`) |
| `docs/engineers/` | Local only | Gitignored (except `.gitkeep`) |
| `docs/compliance/` | P36 (+ P39 `countries/`) | — |
| `docs/asyncapi/` | P2 | Domain channels appended by owning phase |
| `.github/`, `justfile`, `docker-compose.*.yml`, `.pre-commit-config.yaml`, `.editorconfig`, `.gitattributes`, `.gitignore`, `scripts/ci/`, `infra/docker/`, `tools/` (excluding `tools/transparency-report/`) | P0 → P1 | P0 bootstrap, then P1 (MSG-003, MSG-015) |
| `scripts/validate-compliance.py` | P0 → P1 | — |
| `templates/golden-paths/` | P1 | — |
| `deploy/opentofu/`, `deploy/helm/platform/` (Kafka, OpenSearch, Postgres operator, Valkey, NATS, ClickHouse), `deploy/helm/observability/`, `.semgrep/` (except `untrusted-content.yml`), root tool configs (`rust-toolchain.toml`, `deny.toml`, `.golangci.yml`, `.python-version`, `.prettierignore`) | P1 | — |
| `deploy/helm/openbao/` | P3 | In-boundary KMS (P3.8) |
| `deploy/helm/<service>/` | Owner of `services/<service>/` or `apps/<app>/` | Product charts follow code ownership |
| `deploy/gateway/` (Envoy; BFF tier per MSG-020) | P10 | — |
| `.semgrep/untrusted-content.yml` | P15 | — |
| `proto/` (parent fallback), `proto/buf*.yaml`, `proto/vigil/common/`, `proto/vigil/events/`, `proto/wit/common/` | P2 | A domain package is claimed by its owning phase through 🔌 INTERFACE before first use |
| `proto/vigil/lineage/` · `functions/` · `mind/gateway/` · `mind/agents/` · `er/` · `docai/` · `cases/` · `stream/` · `eo/` · `sim/` · `builder/` | P4 · P9 · P14 · P15 · P16 · P17 · P19 · P26 · P29 · P30 · P31 | — |
| `proto/vigil/warden/` | P3 | — |
| `proto/vigil/forge/datasets/` · `proto/vigil/forge/connectors/` | P4 · P11 | — |
| `proto/vigil/ontology/` · `indexer/` · `objects/` · `actions/` · `relay/` · `edge/` · `comms/` | P5 · P6 · P7 · P8 · P21 · P25 · P28 | — |
| `proto/wit/functions/` | P9 | — |
| `services/warden/`, `deploy/identity/` | P3 | — |
| `services/core/` (app skeleton, `vigil/core/shared/`, `vigil/core/ontology/`) | P5 | — |
| `services/core/**/audit/` | P3 | — |
| `services/core/**/lineage/` | P4 | — |
| `services/core/**/objects/` · `**/actions/` · `**/functions/` · `**/peering/` | P7 · P8 · P9 · P38 | — |
| `services/core/src/main/resources/db/migration/<module>/` | Owner of `<module>` | Rule 7 |
| `services/forge-datasets/`, `deploy/helm/lakehouse/` | P4 | — |
| `services/indexer/` | P6 | — |
| `services/traversal/` | P7 | — |
| `deploy/helm/temporal/` | P8 | — |
| `services/functions-host/` | P9 | — |
| `apps/cockpit/` (shell, `design/`, `components/ui/`, `lib/`) | P10 | — |
| `apps/cockpit/src/app/(explore)/`, `features/objects/`, `features/actions/` | P13 | — |
| `apps/cockpit/src/app/(map)/`, `features/map/`, `deploy/helm/geo/` | P18 | — |
| `apps/cockpit/src/app/(cases)/`, `features/graph/`, `features/cases/`, `packs/_core/case/` | P19 | — |
| `apps/cockpit/src/app/(builder)/`, `features/builder/`, `sdks/` (except `sdks/_codegen/`, P2) | P31 | — |
| Phase-local Playwright specs | Each feature phase, inside its own feature folder (FRONTEND_GUIDE §14) | P33 consolidates them into `tests/e2e/` |
| `services/forge-connectors/` (framework, `jdbc/`, `rest/`, `file/`) | P11 | — |
| `services/forge-connectors/connectors/{ocds-etenders,treasury-restricted,municipal-money,coct-awards,cipc-extract}/`, `packs/integrity/` | P22 | — |
| `services/forge-connectors/connectors/{ais,eskomsepush,gdelt,open-meteo,sarb,tnpa}/`, `packs/corridors/` | P23 | — |
| `services/forge-connectors/connectors/{sap,pi-web,opcua-historian,maximo}/`, `packs/erp-sap/` | P24 | — |
| `services/forge-transforms/` | P12 | — |
| `services/mind/gateway/`, `deploy/helm/mind-serving/` | P14 | — |
| `services/mind/agents/` | P15 | — |
| `services/mind/er/`, `packs/_shared/er-rules/` | P16 | — |
| `services/mind/docai/` | P17 | — |
| `services/relay/bundler/`, `services/relay/cmd/vigil/`, `services/relay/internal/common/`, `services/relay/internal/warden/`, `deploy/zarf/`, `deploy/kyverno/verify-images/` | P20 | — |
| `services/relay/hub/`, `services/relay/spoke/` | P21 | — |
| `edge/agent/` | P25 | — |
| `services/stream-funnel/` | P26 | — |
| `edge/field-app/` | P27 | — |
| `services/comms/` | P28 | — |
| `services/eo/` | P29 | — |
| `services/sim/`, `packs/_core/scenario/` | P30 | — |
| `packs/_schema/`, `packs/_core/` (except `case/`, `scenario/`) | P5 | — |
| `tests/load/` · `tests/e2e/`, `tests/chaos/` · `tests/security/` | P32 · P33 · P34 | — |
| `deploy/hardening/` · `compliance/` · `deploy/appliance/` | P35 · P36 · P37 | — |
| `deploy/identity-federation/` · `deploy/pods/` | P38 · P39 | — |
| `services/audit-verifier/`, `apps/oversight/` | P40 | — |
| `deploy/enclave/` · `services/keys/`, `deploy/hsm/` · `tests/redlines/`, `tools/transparency-report/` | P41 · P42 · P43 | — |

---

# The 10 Conflict Prevention Rules

1. **One Phase, One Territory.** A phase has exclusive ownership of the files in its File Ownership list. Nobody else edits them without a 🔄 HANDOFF entry in `AI_COMMUNICATION.md` acknowledged by the owner.
2. **Shared Files Are Append-Only.**
   - `WORKLOG.md`, `docs/API_DESIGN.md`, `docs/AI_COMMUNICATION.md` and `docs/adr/` only grow downwards.
   - Existing rows are never edited or deleted. The only exception is the Status field of an `AI_COMMUNICATION.md` message.
3. **Route Isolation.** Every phase gets its own route and page files:
   - Next.js route groups per feature phase;
   - Spring controllers per Modulith module;
   - Protobuf packages per domain.
   There are no monolithic shared router or registry files. Registries use discovery (classpath scanning, entry points, file globs).
4. **Dependency Mocking.** If phase B needs an interface phase A hasn't built yet, B codes against the **contract** in `proto/` (or API_DESIGN) using a mock or fake that lives in B's territory. B never implements A's backend.
5. **PR Scoping.** One phase per PR. Never combine phases. Ideally a PR stays under ~800 changed lines, excluding generated code.
6. **Contract-First Interfaces.** Any change to a cross-phase interface starts as a 🔌 INTERFACE message plus a `proto/` (or OpenAPI) change reviewed by the consumer. `buf breaking` must pass. There are no breaking changes inside a `v1` package.
7. **Owned, Forward-Only Migrations.** Each module owns its migration folder, and versions carry the phase prefix (e.g. `V8_003__add_outbox.sql`). Never edit a merged migration. Fix forward.
8. **No Cross-Territory Refactors.** If you find a problem in someone else's territory, file a 🐛 BUG entry. Don't "improve" it. This is Karpathy's surgical-changes rule applied across teams.
9. **Claim Before Work, Release After.**
   - Set your phase to 🚀 In Progress (name, AI, date) *before* your first commit, push at once, and open a **draft PR** containing the claim. The open draft PR is the lock others can see, and it later becomes the phase PR. Check open PRs before claiming.
   - A claim with no commits for 14 days may be taken over through a 🔄 HANDOFF that names the previous claimant.
10. **Frozen Blueprints Are Law.**
    - `ARCHITECTURE.md`, `VIGIL_DOMAIN_BLUEPRINT.md` and `DATA_MODEL.md` are 🧊 frozen.
    - A phase may not reinterpret them to fit its own needs. If a blueprint is wrong, stop, write an ADR, and get the lead architect's approval first.

## Founder-authorised mission increment 01 — 2026-09-25

**Owner:** Kone Tshivhinda (OpenAI Codex GPT-6), working alone on `codex/mission-foundation`. **Status:** Local implementation and acceptance complete; full platform phases remain governed by their individual cards.

The founder explicitly instructed implementation after the expanded concept review. [ADR-0009](adr/ADR-0009-first-mission-increment.md) records the bounded cross-phase scope, language and deployment choices, and differences from the future platform. This increment owns the new `apps/cockpit`, `services/core`, `services/mind`, `infra/docker/mission-db`, mission contracts, root build files, local Compose profile, acceptance script and supporting documentation. [MISSION_INCREMENT.md](MISSION_INCREMENT.md) records the tests and unresolved production/remote gates. A Git remote is not configured; no draft PR lock was published. No original phase is marked complete by this entry.

## Founder-authorised investigation increment 02 — 2026-09-26

**Status:** Local implementation and acceptance complete — Kone Tshivhinda (OpenAI Codex GPT-6). **Scope:** ADR-0010; new investigation core classes, SQL migration, Cockpit workspace, contracts, tests, fixtures and supporting documentation. Continues the authorised local increment on `codex/mission-foundation`; no full phase is marked complete. Results and remaining limits: [INVESTIGATION_INCREMENT.md](INVESTIGATION_INCREMENT.md).

## Founder-authorised source review increment 03 — 2026-09-27

**Status:** Local implementation and acceptance complete — Kone Tshivhinda (OpenAI Codex GPT-6). **Scope:** ADR-0011 and contracts/EXTRACTION.md; Mind extraction, Core proposal persistence/review, Sources workspace, direct interface cleanup and acceptance. Existing cross-phase local increment and draft PR #1 continue; no full platform phase is marked complete. Evidence and bounded capabilities: [SOURCE_REVIEW_INCREMENT.md](SOURCE_REVIEW_INCREMENT.md).

## Founder-directed observation and agency exchange — 2026-09-27

**Status:** Design direction recorded; implementation pending. **Owner:** Kone Tshivhinda (OpenAI Codex GPT-6). **Scope:** [ADR-0012](adr/ADR-0012-connected-agency-operations.md) and [CONNECTED_OPERATIONS.md](CONNECTED_OPERATIONS.md). First demonstrate source capabilities, timed synthetic observations, explicit joint-case evidence release, receiving agency requests and acknowledgment/result records. Keep source custody and tenant boundaries, test late/duplicate events and unavailable coverage, and measure time to useful evidence and accepted responsibility. Live camera, toll and agency connections are not implemented by the current increments.

## Founder-authorised connected operations increment 04 — 2026-09-27

**Status:** Complete — implemented by Kone Tshivhinda (OpenAI Codex GPT-6), integrated through `feature/P18-connected-operations` on 2026-10-31. **Scope:** `contracts/COORDINATION.md`, ADR-0012, new Core coordination services/SQL/tests, Cockpit Observations/Agency inbox, local fixtures, acceptance and runbook updates. Founder explicitly approved the proposed incident-coordination workflow. Work alone; preserve existing workspaces and tenant boundaries. Verification criteria are in the contract; no complete platform phase is claimed. Integration verification: 50 Java integration tests, 30 Python tests, 26 Cockpit tests, lint, TypeScript, Prettier and the production build pass; Tomcat remains on the patched 11.0.27 from Spring Boot 4.0.9 rather than the branch's 11.0.26 override.
