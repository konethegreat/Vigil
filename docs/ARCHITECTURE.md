<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Initial frozen system architecture for Vigil (P0) -->

# Vigil — System Architecture

> **Status:** 🧊 FROZEN at P0 (v1.0, 2026-09-25). Changes require an ADR in [`docs/adr/`](adr/) approved by the lead architect (INSTRUCTIONS.md §4.1).
> **Read with:** [`VIGIL_DOMAIN_BLUEPRINT.md`](VIGIL_DOMAIN_BLUEPRINT.md) (ontology, entity resolution and risk logic) · [`DATA_MODEL.md`](DATA_MODEL.md) · [`API_DESIGN.md`](API_DESIGN.md) · [`SECURITY_AND_COMPLIANCE.md`](SECURITY_AND_COMPLIANCE.md) · the rationale in [`research/VIGIL_RESEARCH_DOSSIER.md`](research/VIGIL_RESEARCH_DOSSIER.md) §6–§10.

---

## 1. What Vigil is, in one paragraph

Vigil is a sovereign operational intelligence platform. **Forge** ingests data from any source (ERPs, sensors, documents, public registers, messages) into versioned datasets. **Core** maps those datasets onto an **Ontology**: typed objects, links and events that stand for real-world assets, people, places and operations. Analysts, operators, field crews and AI agents act on the Ontology through governed **Actions**, and those Actions **write back** into the systems of record. Every read, write and inference passes through the **Warden** security kernel, which enforces mandatory markings, purpose and lineage. **Relay** ships the same signed software to cloud, sovereign data centres, air-gapped appliances and edge nodes.

## 2. Architectural principles (non-negotiable)

| # | Principle | Consequence |
|---|---|---|
| A1 | **Security kernel first** | Nothing reads or writes data except through Warden-evaluated policy. Markings propagate through lineage. No component may bypass the policy core. |
| A2 | **Ontology ≠ graph database** | The ontology is a metadata-driven layer over polyglot persistence ([ADR-0002](adr/ADR-0002-ontology-not-a-graph-database.md)). |
| A3 | **Physical schema ≠ logical schema** | OMS generates typed physical tables and indexes. Object types are views over them. There are no EAV tables on hot paths. |
| A4 | **Edits are events** | An append-only edit log is the source of truth for all ontology edits. Current state, history, audit and decision lineage are projections of it. |
| A5 | **An Action = one transaction + outbox** | Validation, then the pre-commit writeback webhook, then an atomic commit of edits + edit log + outbox, then idempotent side effects run as Temporal activities. |
| A6 | **Branch-ready from v1** | Every dataset, schema and index record carries a branch key from v1, so branching never needs a retrofit. Branch creation, merge and index branches are deepening, first needed by P30. |
| A7 | **Contracts at every language boundary** | Protobuf/Buf, OpenAPI 3.1, CloudEvents/AsyncAPI, Arrow. No operational database is shared across a service or language boundary. The single sanctioned exception is inside the Core ontology plane: the Indexer (Rust) and the Action engine (Java) both write typed object tables, under the per-schema and per-column write-ownership matrix in DATA_MODEL §2 ([ADR-0001](adr/ADR-0001-language-charter.md), [ADR-0007](adr/ADR-0007-typed-object-tables-sanctioned-writers.md)). |
| A8 | **Sovereign by default** | Everything runs in-country or air-gapped. No required call leaves the deployment boundary. No prompt leaves South Africa ([ADR-0004](adr/ADR-0004-sovereign-first-deployment.md)). |
| A9 | **Designed for disconnection** | Every edge and field component works offline, using store-and-forward, queued Actions and offline map tiles. |
| A10 | **Humans decide consequential outcomes** | Adverse or legal-effect decisions need human approval (POPIA s71). AI proposes, humans dispose. |
| A11 | **Permissive, foundation-governed dependencies** | [ADR-0003](adr/ADR-0003-open-source-licence-policy.md). Every single-vendor dependency sits behind an abstraction boundary. |
| A12 | **Simplicity first** | Build the minimum slice that meets the phase's success criteria (Karpathy doctrine, INSTRUCTIONS.md §5). |

## 3. System topology

### 3.1 Planes

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ EXPERIENCE PLANE                                                                                    │
│  Vigil Cockpit (Next.js/TS)      Vigil Field app (Kotlin)      Comms gateway (Elixir → Go fallback)  │
│  Explorer·Map/COP·Graph·Timeline·Case·Builder   offline, queued Actions   WhatsApp·SMPP·USSD·presence│
└──────────────▲──────────────────────────────▲──────────────────────────────▲────────────────────────┘
               │ Connect/gRPC-Web, REST        │ Connect (sync), Actions queue │ NATS (Protobuf)
┌──────────────┴──────────────────────────────┴──────────────────────────────┴────────────────────────┐
│ API EDGE: vigil-gateway (Envoy) · BFF tier per MSG-020 (P10) · Keycloak OIDC · rate limits (Valkey)│
└──────────────▲──────────────────────────────────────────────────────────────────────────────────────┘
┌──────────────┴───────────────────────────── ONTOLOGY PLANE — VIGIL CORE ────────────────────────────┐
│  core (Java 25, Spring Boot 4, Spring Modulith 2):                                                  │
│    ontology (OMS) · objects (Object Set API) · actions (Action engine + outbox) · functions gateway │
│    lineage · audit                                                                                  │
│  indexer (Rust): changelog → merge → index → hydrate          traversal (Rust): multi-hop over Arrow│
│  functions runtime (Rust Wasmtime host + sandboxed TS/Python containers)                            │
│  Temporal (write-back sagas)                                                                        │
└──────────────▲───────────────────────────────▲──────────────────────────────────▲──────────────────┘
               │ datasets / change feeds       │ tools-as-user (MCP, gRPC)        │ Actions → systems of record
┌──────────────┴───────────── DATA PLANE — VIGIL FORGE ──┐ ┌──────── INTELLIGENCE PLANE — VIGIL MIND ─┐
│ connectors (Java) · edge/ingest agent (Rust)           │ │ model gateway (LiteLLM) · vLLM/llama.cpp │
│ Kafka 4 (core log) · NATS JetStream (edge/control)     │ │ ontology tools + MCP · agents · evals    │
│ forge-datasets (Java): Iceberg v3 + Polaris + S3 API   │ │ entity resolution (Splink) · doc AI      │
│ transforms (Python/Dagster; Polars/DuckDB/Spark/Flink) │ │ speech (Omnilingual ASR) · geo-ML · sims │
│ data quality · OpenLineage events                      │ │ (Python 3.13+, FastAPI)                  │
└────────────────────────────────────────────────────────┘ └──────────────────────────────────────────┘
═══════ SECURITY PLANE — VIGIL WARDEN: Rust policy core (native + WASM) · Keycloak · OpenFGA · audit ledger ═══════
═══════ DELIVERY PLANE — VIGIL RELAY: Go hub/spoke controller · Zarf bundles · Harbor · Flux · Kyverno · RKE2/k3s/Talos ═══════
═══════ OBSERVABILITY: OpenTelemetry everywhere → VictoriaMetrics (metrics/logs) · ClickHouse (traces) · Grafana ═══════
```

### 3.2 Layer boundaries (the dependency rule)

`Presentation (Cockpit/Field)` ➔ `API edge (gateway/BFF)` ➔ `Service layer (Core modules, Mind, Forge services)` ➔ `Data access (repositories, object store adapters, Iceberg clients)` ➔ `Background workers (indexer, Temporal workers, Dagster jobs)` ➔ `External feeds (connectors, edge agents)`

- Dependencies point **inward and downward only**. The presentation layer never touches a database.
- Services never call another service's database. They use its contract (gRPC/Connect or events).
- Background workers own no business rules beyond what their phase defines. Business rules live in Core modules or Action definitions.

## 4. Services, languages and ownership

| Service / component | Path | Language | Runtime | Phase | Responsibility |
|---|---|---|---|---|---|
| Warden policy core | `services/warden/` | Rust | lib (native + WASM component), `warden-pdp` sidecar | P3 | Marking and classification evaluation, releasability, purpose checks, lineage marking propagation, redaction rules |
| Identity & ReBAC config | `deploy/identity/` | Keycloak realm JSON, OpenFGA DSL | Keycloak, OpenFGA | P3 | Users, orgs, groups, projects/spaces, roles, relationship tuples |
| Audit ledger | `services/core/.../audit/` | Java | core module | P3 | Hash-chained append-only audit events; external anchoring from P40 |
| Forge datasets | `services/forge-datasets/` | Java | Spring Boot | P4 | Iceberg datasets, transactions (SNAPSHOT/APPEND/UPDATE/DELETE), branches/tags, retention, lineage events |
| Ontology Metadata Service | `services/core/.../ontology/` | Java | core module | P5 | Object/link/interface/action/function types; versioning; schema branches; physical DDL generation |
| Indexer (Funnel) | `services/indexer/` | Rust | worker | P6 | Changelog → merge (source rows + edits by primary key) → index (Postgres typed tables + OpenSearch) → hydrate |
| Object Set Service | `services/core/.../objects/` | Java | core module | P7 | Filter, search, aggregate, search-around; security pushdown; delegates multi-hop to traversal |
| Traversal | `services/traversal/` | Rust | gRPC service | P7 | Multi-hop link analysis over Arrow snapshots; path queries; neighbourhood expansion with policy filters |
| Action engine | `services/core/.../actions/` | Java + Temporal | core module + workers | P8 | Action types, parameters, submission criteria, writeback webhook, edit log, outbox, sagas |
| Lineage store | `services/core/.../lineage/` | Java | core module | P4 | Enforced lineage graph (datasets → transforms → objects → Actions), marking-propagation records, decision lineage (DOMAIN_BLUEPRINT §10) |
| Functions runtime | `services/functions-host/` | Rust (Wasmtime LTS) + sandboxed containers | service | P9 | WASM UDFs; TS/Python functions in gVisor/Firecracker-class sandboxes; enforced limits |
| Cockpit | `apps/cockpit/` | TypeScript | Next.js | P10, P13, P18, P19, P31 | Operator and analyst UI |
| Connectors | `services/forge-connectors/` | Java | Spring Boot + Debezium/Kafka Connect runtime | P11, P24 | Source adapters, schedules, rate limits, credentials via OpenBao (MPL-2.0) or an HSM-backed KMS. HashiCorp Vault (BSL) is never shipped (ADR-0003) |
| Transforms & pipelines | `services/forge-transforms/` | Python | Dagster + uv | P12 | Transform SDK (Polars/DuckDB/PySpark), incremental builds, quality checks, lineage |
| Mind gateway | `services/mind/gateway/` | Python | FastAPI + LiteLLM | P14 | Model routing, marking-aware redaction, quotas, usage metering, Langfuse traces |
| Mind agents & MCP | `services/mind/agents/` | Python | FastAPI | P15 | Tools = object sets, Actions and Functions executed *as the user*; Ontology MCP server; approval staging; automations |
| Entity resolution | `services/mind/er/` | Python | Splink jobs + FastAPI | P16 | Probabilistic linkage, African-name comparators, clusters as views, review queue |
| Document AI | `services/mind/docai/` | Python | FastAPI + GPU workers | P17 | Docling/PaddleOCR/olmOCR extraction with field-level provenance and confidence |
| Relay | `services/relay/` | Go | controller + agent + `vigil` CLI | P20–P21 | Signed Zarf bundles, Harbor sync, Flux sources, plans, constraints, channels, rollback |
| Edge agent | `edge/agent/` | Rust | static binary (ARM/x86) | P25 | OPC UA/Modbus/MQTT-Sparkplug/serial adapters, disk-backed store-and-forward, NATS leaf |
| Field app | `edge/field-app/` | Kotlin | Android (Compose + KMP) | P27 | Offline object cache, queued Actions, maps (PMTiles), forms, evidence capture |
| Comms gateway | `services/comms/` | Elixir (or Go per ADR at P28) | Phoenix | P28 | WhatsApp Cloud API, SMPP SMS, USSD, presence, alert escalation |
| SDKs | `sdks/{ts,python,java,kotlin}/` | generated | libraries | P2 (skeleton), P31 | Ontology-scoped SDKs generated from OMS (OSDK-equivalent) |
| KMS (v1) | `deploy/helm/openbao/` | OpenBao (MPL-2.0) | service | P3 | Tenant KEKs, dataset DEKs, `cipher_text` key channels; PKCS#11-ready (P42 moves KEKs to customer HSMs) |
| Warden admin | `services/warden/admin/` | Rust | service | P3 | Marking, purpose and gate registries (`warden.*` schema) |
| Stream funnel | `services/stream-funnel/` | Java | Flink job | P26 | Kafka → typed tables, OpenSearch and ClickHouse; exactly-once |
| Earth observation | `services/eo/` | Python | workers | P29 | STAC/Copernicus/DE Africa ingestion; dark-vessel matching |
| Simulation | `services/sim/` | Python | service | P30 | Scenario engine; OR-Tools/HiGHS/VROOM optimisation |
| Audit verifier | `services/audit-verifier/` | Rust or Go (ADR at P40) | CLI + service | P40 | Independent audit-chain verification; external anchoring |
| Keys | `services/keys/` | Rust | service | P42 | Customer-managed keys (BYOK/HYOK); crypto-shredding |
| Ontology packs | `packs/<pack>/` | YAML/JSON ontology definitions + pack functions | — | P22+ | Integrity, Corridors, SAP/ERP, Grid, Municipal |
| Deployment | `deploy/` | Helm, Zarf, OpenTofu, Kustomize | — | P1, P20, P35, P37 | Charts, bundles, infrastructure |

**Rule:** a phase owns its directories exclusively. The master matrix is the File Ownership Master Index in [`PHASES_AND_TASKS.md`](PHASES_AND_TASKS.md).

## 5. Data stores and their jobs

| Store | Technology | Holds | Written by | Read by |
|---|---|---|---|---|
| Lakehouse | **Iceberg v3** on S3 API (**SeaweedFS** on-prem; native S3 in the commercial cloud tier) + **Apache Polaris** REST catalog | Raw and derived datasets, edit-log materialisations, embeddings (LanceDB), history | forge-datasets, transforms, indexer (materialise) | transforms, indexer, Mind, analytics |
| Control-plane DB | **PostgreSQL 18→19** (`vigil_control`) | Tenants, OMS metadata, schema versions, Action types, audit ledger, OpenFGA store, Relay state | core, Warden admin, Relay | core |
| Object store (hot) | **PostgreSQL 18→19** per tenant (`vigil_t_<tenant>`) + PostGIS + pgvector/pgvectorscale | Typed object tables, link/join tables, edit log, outbox, current state | indexer (hydrate), Action engine (edits) | Object Set Service, traversal snapshots |
| Search | **OpenSearch** (per-tenant index prefix, document- and field-level security) | Full-text + filters over objects; per-document markings field | indexer | Object Set Service |
| OLAP/telemetry | **ClickHouse** | Time-series properties, sensor telemetry, audit analytics copy, traces | Forge streaming, OTel collector | Cockpit charts, Mind |
| Edge store | **SQLite** + **DuckDB** + Parquet spool; **QuestDB** optional | Site time-series, queued Actions, cached objects | edge agent, field app | edge UI, sync |
| Cache | **Valkey** (BSD-3, Linux Foundation) | Sessions, rate limits, object-set result cache (keyed by policy context hash), idempotency keys | gateway, core | gateway, core |
| Streams | **Kafka 4.x** (core durable log), **NATS JetStream** (edge, control, comms) | Change feeds, CloudEvents, telemetry, commands | Forge, core outbox relay, edge agents | indexer, Mind, comms |
| Workflow state | **Temporal** (Postgres persistence) | Action sagas, approvals, retries, compensations | Action engine | Temporal workers |

**Security pushdown invariant.** Every store that serves reads enforces Warden decisions natively:
- Postgres RLS policies, compiled from ontology policy;
- OpenSearch DLS/FLS;
- Iceberg scoped credentials, vended by Polaris per policy context;
- ClickHouse row policies.

The policy core supplies identical semantics to all of them (P3). The one exception is traversal, which filters in-process by linking the Rust policy core natively.

## 6. Key flows

### 6.1 Ingest → dataset → objects

1. A connector or edge agent pulls or receives data. It writes an Iceberg transaction (APPEND or SNAPSHOT) through `forge-datasets`, which emits an OpenLineage run event and a `vigil.forge.dataset.transaction.committed.v1` CloudEvent.
2. **Markings propagate:** output dataset markings = the union of all input markings (Warden `propagate()`), recorded as an OpenLineage facet.
3. The indexer consumes the event and runs changelog (diff by primary key) → **merge** with ontology edits under the property's conflict policy (`SOURCE_WINS | EDIT_WINS | LATEST_TIMESTAMP`) → **index** (upsert typed Postgres rows + OpenSearch documents, carrying markings) → **hydrate** (swap index aliases, warm caches). If more than 80% of rows changed, it does a full reindex into a shadow table and swaps atomically.
4. It emits `vigil.indexer.objects.indexed.v1` with a watermark, so the Object Set Service can offer read-your-writes by watermark.

### 6.2 Read (object set query)

1. Cockpit (or an agent) → gateway (OIDC token + declared **purpose** or **case ID**) → Object Set API.
2. Warden builds a **policy context**: user markings, clearances, releasability, org memberships, purpose, case.
3. The OSS compiles the query into SQL (Postgres, with `SET LOCAL vigil.policy_ctx`), an OpenSearch query, or a traversal request. Each engine enforces the policy natively (§5).
4. Results are redacted at property level wherever column markings exceed the context. **Every read is audited** with query hash, purpose, row count and marking set.

### 6.3 Action with write-back (the kinetic path)

```
Cockpit / Field / Agent ──submit(action_type, params, purpose)──▶ Action engine (core)
  0. persist action_run (VALIDATING), keyed by (submitted_by, Idempotency-Key), in its own transaction;
     a retry with the same key reuses the same action_run_id (= X-Vigil-Action-Run-Id)
  1. authorise: Warden (markings on all touched objects) + OpenFGA (role) + submission criteria
  2. validate params (types, bounds, invariants)
  3. [optional] writeback webhook (sync, pre-commit, timeout ≤ 10s) ──▶ system of record (SAP/ERP/API)
        └─ failure ⇒ abort; nothing is committed
  4. BEGIN; apply edits to typed tables; append edit_log; insert outbox rows; COMMIT
  5. outbox relay ──▶ Kafka `<tenant>.ontology.edits.v1` (vigil.ontology.edits.applied.v1) ──▶ indexer · lineage · audit
  6. side effects (notifications, secondary systems) ──▶ Temporal saga (idempotent, retried, compensable)
  7. if the action is flagged `requires_human_approval` (s71 / adverse outcome): steps 3–6 run only
     after an approver with the right role signs; agents may only *stage* such actions
```

Limits (v1): at most 10,000 objects per Action, at most 3 MB per object edit, 60 s synchronous budget, 1 writeback webhook per Action. These match Palantir's documented envelope.

### 6.4 AI agent turn (Mind)

1. A user asks, or an automation fires.
2. The agent receives only tools the application allow-lists: object-set queries, Functions and Actions.
3. Tools run **as the user**, with the same purpose context and markings.
4. The model gateway redacts or refuses content above the caller's context.
5. Every tool call and model call is traced (Langfuse + OTel) and audited.
6. Writes are staged for approval unless the Action type is marked `agent_executable` *and* not `requires_human_approval`.
7. Untrusted content (documents, web, messages) is wrapped in `<untrusted_content>` boundaries before it reaches a model ([`SYSTEM_COMPLIANCE_GUARDRAILS.md`](SYSTEM_COMPLIANCE_GUARDRAILS.md)).

### 6.5 Edge and field (disconnected)

The edge agent keeps a disk-backed spool (Parquet + SQLite). It forwards to a NATS leaf node, which bridges to the core when a link exists. The field app keeps an encrypted, markings-filtered object cache and a queue of signed Action requests. On reconnect it submits them in order, and the server applies normal authorisation. **Conflicts are resolved server-side by Action semantics, never by client merge.** Offline maps come from PMTiles bundles. Collaborative notes (not operational state) may use CRDTs (Yjs on web, Loro on native).

## 7. Deployment topologies

| Topology | Substrate | Use | Notes |
|---|---|---|---|
| **Sovereign DC** | RKE2 (FIPS/CIS) on bare metal in SA colocation (Teraco Isando/Bredell primary; Teraco CT2 or Equinix JN1 DR) | Default for government/SOE and sensitive data | Customer-held keys (HSM), SA operator, no foreign control plane |
| **Customer on-prem** | RKE2 on customer hardware | SOEs, miners, banks | Delivered by Relay bundles; customer ops with Vigil support |
| **Air-gapped appliance** | Talos Linux (built-in Kubernetes) on a 1–3 node appliance with a 4× RTX PRO 6000 AI node | Classified or disconnected sites; demos | One-way bundle import; offline model registry |
| **Edge node** | k3s (or bare static binaries) on ARM/x86, 8–12 h battery autonomy | Rail yards, border posts, substations, mines | Rust agent + NATS leaf + SQLite/DuckDB + llama.cpp + Valhalla + PMTiles |
| **Commercial cloud tier** | Managed Kubernetes in an SA hyperscaler region | Non-sensitive commercial tenants only | Labelled "hyperscaler-connected"; CLOUD Act disclosure in contract; never for sovereign data |

**Relay model** ([`research/appendix/02`](research/appendix/02-palantir-technical.md) §6):
- A hub issues **Plans** to spokes, which report state and **poll** for new plans.
- **Products** carry semver releases on **channels** (`dev → canary → stable → lts`). A release that fails health checks is **recalled**.
- **Constraints** include maintenance windows, dependency ranges and suppression after failures. A failed plan triggers an automatic rollback plan.
- Air-gapped spokes import signed **bundles** one way.
- Every artefact is signed (cosign), carries an SBOM (Syft) and is verified at admission (Kyverno).

## 8. Interoperability contracts

| Concern | Standard | Location | Rules |
|---|---|---|---|
| Service RPC | Protobuf + gRPC; **Connect** for browser/mobile | `proto/vigil/<domain>/v1/` | `buf lint` + `buf breaking` against `develop` in CI; no breaking changes within `v1` |
| Public REST | OpenAPI 3.1 | `docs/API_DESIGN.md` + `proto/openapi/` | Error envelope and pagination as in API_DESIGN |
| Events | CloudEvents 1.0, documented with AsyncAPI 3 | `proto/vigil/events/` + `docs/asyncapi/` | `type` = `vigil.<domain>.<entity>.<verb>.v1`; payload = Protobuf |
| Columnar | Apache Arrow IPC / Flight / ADBC | — | Used by traversal, Mind, transforms |
| UDFs/plugins | WASM Component Model (WIT) on Wasmtime LTS | `proto/wit/` | Fuel and epoch limits; no ambient I/O |
| Ontology export | DTDL v3 / AAS / OCDS mappings (import/export only) | `packs/*/interop/` | Never the internal model |

## 9. Scalability parameters (v1 targets → Tier 4 targets)

| Dimension | v1 (Phase Zero) | Tier 4 target | Lever |
|---|---|---|---|
| Objects per tenant | 50M | 5B | Partitioned typed tables (hash by PK), OpenSearch shards, Iceberg for history |
| Object-set query p95 (≤ 1 hop, filtered) | 300 ms | 150 ms | Covering indexes, Valkey result cache keyed by policy-context hash |
| Search-around | ≤ 3 hops in OSS | ≤ 6 hops via traversal | Rust traversal over Arrow snapshots |
| Indexer throughput | 20k rows/s per worker | 200k rows/s per cluster | Parallel partitions, bulk COPY, shadow-table swaps |
| Action commit p95 (no webhook) | 250 ms | 120 ms | Single-transaction design, outbox |
| Streaming freshness | 60 s | 5 s | Kafka → streaming funnel (P26) |
| Concurrent Cockpit users per tenant | 200 | 10,000 | Stateless BFF, CDN-less static assets for sovereign sites |
| Edge autonomy | 8 h | 72 h | Spool sizing, local inference |

**Horizontal scale.** Core modules are stateless behind the gateway. The Modulith deploys as one unit in v1, and modules split into separate deployments only when a Tier 4 load test proves the need (Karpathy: no speculative microservices). Indexer and traversal scale by partition. Mind scales GPU workers independently.

## 10. Caching strategy

- **Valkey** holds sessions, rate limits, idempotency keys, and object-set result caches.
- Cache keys **always include the policy-context hash** (markings, purpose, org), so cached results can never cross security contexts.
- Caches are invalidated by `objects.indexed` watermark events.
- **Nothing sensitive is cached at the edge except in the encrypted field-app store**, which is filtered by the user's markings at sync time.

## 11. Observability

- OpenTelemetry SDKs are used in every language. Traces, metrics and logs carry `tenant`, `purpose`, `case_id` (when present) and `policy_ctx_hash`. They never carry property values.
- Pipelines: OTel Collector → VictoriaMetrics (metrics/logs) and ClickHouse (traces). Grafana runs unmodified as a separate service.
- Each service ships SLO dashboards and burn-rate alerts. Runbooks are in [`INCIDENT_RESPONSE.md`](INCIDENT_RESPONSE.md).

## 12. Resilience and failure modes

| Failure | Behaviour |
|---|---|
| Upstream system of record down | Writeback-webhook Actions fail cleanly (no commit). Side-effect sagas retry with backoff, then compensate. |
| Kafka unavailable | The outbox holds events and the relay resumes from the last offset. Reads keep working at the last watermark. |
| Indexer lag | The OSS serves the last consistent watermark and the Cockpit shows "data as of". Actions still commit, and edits show immediately via an edit overlay. |
| WAN cut (edge) | The agent spools locally and the field app queues Actions. Local inference and maps keep working. |
| Model server down | Mind degrades to "no-AI" mode. Every workflow must be completable without AI. |
| Power loss (load reduction) | Edge kits run on battery for 8–12 h. DC sites use their colocation provider's resilience. |

## 13. Repository layout and naming

```
vigil/
├── INSTRUCTIONS.md · README.md · WORKLOG.md · justfile · docker-compose.ci.yml
├── .github/workflows/          # the 7 governed workflows
├── docs/                       # governance + blueprints + ADRs + research + reviews
├── scripts/                    # validate-compliance.py and helpers
├── proto/                      # Protobuf (buf), WIT, OpenAPI, AsyncAPI
├── services/
│   ├── warden/                 # Rust policy core (+ bindings)
│   ├── core/                   # Java Spring Modulith: ontology, objects, actions, functions, lineage, audit
│   ├── indexer/                # Rust
│   ├── traversal/              # Rust
│   ├── functions-host/         # Rust (Wasmtime) + sandbox images
│   ├── forge-datasets/         # Java
│   ├── forge-connectors/       # Java
│   ├── forge-transforms/       # Python (Dagster)
│   ├── mind/{gateway,agents,er,docai}/   # Python
│   ├── relay/                  # Go (controller, agent, cli)
│   └── comms/                  # Elixir (Tier 3, per ADR)
├── edge/{agent,field-app}/     # Rust, Kotlin
├── apps/cockpit/               # TypeScript / Next.js
├── sdks/{ts,python,java,kotlin}/  # generated
├── packs/{integrity,corridors,erp-sap,...}/
├── deploy/{helm,zarf,opentofu,identity,kyverno}/
├── infra/docker/               # base images (own them; no Bitnami)
└── tests/{e2e,load,chaos}/
```

**Naming conventions (all languages):**
- Code roots use the codename `vigil`: Java packages `vigil.core.*`, `vigil.forge.*`; Rust crates `vigil-*`; Python packages `vigil_*`; npm scope `@vigil/*`.
- The Go module root is fixed in P1, once the remote exists (see dossier §18 Q7).
- The codename stays in code even if the public brand changes ([ADR-0006](adr/ADR-0006-codename-and-trademark-clearance.md)).

## 14. Technology decisions index

| Decision | ADR |
|---|---|
| Language charter & staged adoption | [ADR-0001](adr/ADR-0001-language-charter.md) |
| Ontology as metadata-driven layer, not a graph DB | [ADR-0002](adr/ADR-0002-ontology-not-a-graph-database.md) |
| Open-source licence policy | [ADR-0003](adr/ADR-0003-open-source-licence-policy.md) |
| Sovereign-first deployment | [ADR-0004](adr/ADR-0004-sovereign-first-deployment.md) |
| Security kernel first (Warden) | [ADR-0005](adr/ADR-0005-security-kernel-first.md) |
| Codename & trademark clearance | [ADR-0006](adr/ADR-0006-codename-and-trademark-clearance.md) |
| Typed object tables: the two sanctioned writers inside Core | [ADR-0007](adr/ADR-0007-typed-object-tables-sanctioned-writers.md) |
