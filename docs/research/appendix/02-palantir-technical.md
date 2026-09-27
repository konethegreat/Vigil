<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Raw research appendix (P0). Sourced analyst report; verify [UNVERIFIED] items before external use. -->

> **Appendix to** [`VIGIL_RESEARCH_DOSSIER.md`](../VIGIL_RESEARCH_DOSSIER.md). A raw research report produced on 24–25 Sep 2026 by a parallel research agent. Kept verbatim for traceability. Where it conflicts with the dossier, the dossier wins (the dossier applies the founders' greenfield decision).
>
> **Internal working paper; not for external distribution.**

# How Palantir Works Under the Hood — and What Vigil Should Build

*Technical due-diligence report for the Vigil founding team · 25 September 2026 · Topic 02*

**Method.** Primary sources: palantir.com/docs (Foundry, Gotham, Apollo), blog.palantir.com, Palantir job postings, github.com/palantir, and vendor docs for the comparable platforms. The session's search budget ran out after about 20 queries, so most evidence comes from fetching primary pages directly. Anything I could not confirm is marked **[UNVERIFIED]**; my own analysis is labelled **Inference** or **Recommendation**.

## Executive summary

- **The Ontology is not a graph database.** It is five services over versioned datasets ([object backend](https://www.palantir.com/docs/foundry/object-backend/overview/)): a metadata service (OMS), an indexing pipeline (Object Data Funnel), purpose-built object databases (OSv1 "Phonograph", now OSv2), a read service (OSS), and a transactional Actions service. Vigil should copy this shape.
- **The "kinetic" half is Vigil's core IP to build.** Actions with submission criteria, a pre-commit writeback webhook and post-commit side effects are what make the platform operational. Open-source analogues mostly lack them.
- **Security is mandatory-first.** Markings are conjunctive, propagate through lineage, and can only be removed with a central "Expand Access" permission. Agents are held to the same policies as humans.
- **Palantir is polyglot.** Java runs the services, Go the infrastructure, TypeScript/React the UIs and OSDK, and Python the transforms; Scala appears via the Spark fork, and Rust is rising (DataFusion-backed pipelines in 2026). The main back-end systems are Cassandra, Elasticsearch, Kafka, Spark, Flink and Kubernetes.
- **Apollo is a hub/spoke delivery system that solves deployment constraints and is built for disconnected sites.** Zarf/UDS + Flux/Fleet + Kairos cover most of it; the constraint engine and release-channel promotion are the missing pieces [Inference].

---

## 1. The Ontology in depth

### 1.1 "Semantic", "kinetic", "dynamic"

- **Palantir's own definition:** the Ontology is an "operational layer" over "datasets, virtual tables, and models": "a digital twin of the organization, containing both the **semantic elements (objects, properties, links)** and **kinetic elements (actions, functions, dynamic security)**" ([overview](https://www.palantir.com/docs/foundry/ontology/overview/)).
- **Nouns and verbs:** data elements are "the nouns" ("the semantic, real-world objects and links"); actions are "the verbs" ("the kinetic, real-world execution") ([why-ontology](https://www.palantir.com/docs/foundry/ontology/why-ontology); [blog, Jan 2024](https://blog.palantir.com/connecting-ai-to-decisions-with-the-palantir-ontology-c73f7b0a1a72)).
- **"Dynamic":** the docs frame each decision as **data + logic + action + security**.
  - Logic (rules, ML, optimisation, simulation) is attached through "logic binding".
  - Security policies "are dynamically computed at runtime for every interaction" ([why-ontology](https://www.palantir.com/docs/foundry/ontology/why-ontology)).
  - A 2026 post adds continuous capture of "decision data" and "decision lineage" ([blog, Apr 2026](https://blog.palantir.com/connecting-agents-to-decisions-277dee8ddb40)).
  - I found no verbatim primary definition of the triad **[UNVERIFIED]**. Secondary explainers ([Medium](https://pythonebasta.medium.com/understanding-palantirs-ontology-semantic-kinetic-and-dynamic-layers-explained-c1c25b39ea3c)) redefine "kinetic" as data connectivity, which contradicts Palantir.
- **Inference:** read *semantic* as the typed model, *kinetic* as governed write paths to reality, and *dynamic* as logic, simulation and security evaluated at runtime over live state plus decision capture. Palantir's architecture centre calls these **Language / Engine / Toolchain (OSDK)** ([ontology system](https://www.palantir.com/docs/foundry/architecture-center/ontology-system)).

### 1.2 Semantic primitives

- **Object types.** An object type is "the schema definition of a real-world entity or event" ([object types](https://www.palantir.com/docs/foundry/object-link-types/object-types-overview/)).
  - Backing sources: datasets, restricted views, streams ([backend](https://www.palantir.com/docs/foundry/object-backend/overview/)), and since Sept 2025 virtual tables and Iceberg tables ([Sep 2025](https://www.palantir.com/docs/foundry/announcements/2025-09)).
- **Property base types** ([base types](https://www.palantir.com/docs/foundry/object-link-types/base-types/)):
  - scalars, plus Array, Struct, **Vector**, **Geopoint**, **Geoshape**, Attachment, **Time series**, **Geotemporal series**, **Media reference**, **Cipher text** and **Marking**;
  - every type except Vector and Time series can be an array;
  - a media reference is media-set RID + view RID + item RID.
- **Link types** ([create link type](https://www.palantir.com/docs/foundry/object-link-types/create-link-type/)):
  - 1:1 and N:1 links use a foreign-key property;
  - **M:N uses a join-table dataset** of both primary keys, which can be auto-generated;
  - **object-backed links** put an intermediary object type in between to carry link metadata;
  - links are bidirectional, and one datasource cannot back two link types ([link types](https://www.palantir.com/docs/foundry/object-link-types/link-types-overview/)).
- **Interfaces** ([interfaces](https://www.palantir.com/docs/foundry/interfaces/interface-overview/)):
  - abstract shapes with shared properties, link constraints and action constraints (beta); multiple inheritance is allowed;
  - support is uneven: full in Ontology Manager and TS v2 functions, partial in OSS and OSDK, none yet in Workshop.
- **Object sets** are what everything queries: filter, search-around (link traversal), set algebra and aggregation, all served by OSS. In TypeScript v1, `.all()` fails above **100,000 objects** or **3 search-arounds** ([function limits](https://www.palantir.com/docs/foundry/functions/enforced-limits)).

### 1.3 Kinetic primitives

- **Action type:** "a single transaction that changes the properties of one or more objects" ([action types](https://www.palantir.com/docs/foundry/action-types/overview/)).
  - **Rules:** create/modify/delete objects or links, function, webhook, notification.
  - Typed **parameters**.
  - **Submission criteria:** who may run it and under what conditions.
  - Edits commit atomically to edit history.
- **Write-back to external systems** ([webhooks](https://www.palantir.com/docs/foundry/action-types/webhooks/)):
  - A **writeback webhook** runs "before any other rules are evaluated; if the webhook execution fails, no other changes will be made".
  - Only one is allowed per action, and its outputs can feed later rules.
  - **Side-effect webhooks** run after the commit, in no guaranteed order.
  - Targets include Salesforce, SAP or any HTTP server, with OAuth 2.0.
- **Functions** ([functions](https://www.palantir.com/docs/foundry/functions/overview/)):
  - TypeScript v1/v2 and Python, run server-side "in an isolated environment";
  - they read and traverse objects, return "Ontology edits" (function-backed actions) and call external systems.
  - Limits ([limits](https://www.palantir.com/docs/foundry/functions/enforced-limits)): 60 s default, 280 s in preview, 4 h async in Automate; 128 MB memory in TS v1, 2 GB for deployed Python, 512–5,120 MB serverless.

### 1.4 Backend services and storage

| Service | Documented role ([backend](https://www.palantir.com/docs/foundry/object-backend/overview/)) |
|---|---|
| Ontology Metadata Service | "defines the set of ontological entities that exist" |
| Object Set Service | Serves all reads: search, filter, aggregate, load |
| Object Data Funnel | Orchestrates OSv2 writes: indexes datasources + user edits into the object DBs |
| Actions service | Applies governed edits (permissions, conditions, history) |
| OSv1 "Phonograph" | Legacy DB; "planned deprecation after June 30, 2026" |
| OSv2 | Indexing separated from querying; incremental; streaming; ≤2,000 properties; 10,000+ edits per action |

- **Funnel batch pipeline**, four jobs ([batch](https://www.palantir.com/docs/foundry/object-indexing/funnel-batch-pipelines/)):
  1. **Changelog** — diff each datasource.
  2. **Merge** — combine changes with Action edits by primary key.
  3. **Index** — write index files.
  4. **Hydrate** — load indexes onto search nodes.
  - If more than 80% of rows change, it does a full reindex.
  - Edits reach indexes "immediately" and become durable via live pipelines every six hours.
  - Conflicts: "most recent transaction wins".
- **Funnel streaming** ([streaming](https://www.palantir.com/docs/foundry/object-indexing/funnel-streaming-pipelines)):
  - latency of seconds to minutes; input ordered by key and event time;
  - 1 MB per record, ≤250 properties, no user edits;
  - exactly-once by default.
  - **Direct datasources** are a third, low-latency path ([indexing](https://www.palantir.com/docs/foundry/object-indexing/overview)).
- **Writeback and edit history** ([breaking changes](https://www.palantir.com/docs/foundry/object-backend/object-storage-v2-breaking-changes)):
  - OSv2 forces every edit through Actions; OSv1's writeback datasets become optional **materializations**;
  - edit history must be switched on;
  - OSv1 history is lost on migration.
- **Storage engine:** job ads name Elasticsearch and Cassandra as core systems ([infra posting](https://jobs.lever.co/palantir/6fe5515f-f677-4d98-8ac2-1775a425f5e7)). That OSv1/OSv2 indexes are Elasticsearch-based is **[UNVERIFIED]**.
- **2026 additions:**
  - security-policy testing (GA June) ([Jun](https://www.palantir.com/docs/foundry/announcements/2026-06));
  - classification-based access controls (CBAC) on ontology resources (May) ([May](https://www.palantir.com/docs/foundry/announcements/2026-05));
  - SQL Studio over ontology data (GA June);
  - **SuperRepo**, "a single monorepo that holds your Ontology definitions, your functions, and your React application together" ([Aug](https://www.palantir.com/docs/foundry/announcements/2026-08)).

### 1.5 Documented limits (Vigil design targets)

| Limit | Value | Source |
|---|---|---|
| Objects / object types per action | 10,000 / 50 | [action limits](https://www.palantir.com/docs/foundry/action-types/scale-property-limits) |
| Single object edit | 32 KB (OSv1) / 3 MB (OSv2) | same |
| List parameters | 10,000 primitives / 1,000 object refs | same |
| Batch calls | 10,000; 20 if function-backed and unbatched | same |
| Properties per type | 2,000 (OSv2); 250 (streaming) | [backend](https://www.palantir.com/docs/foundry/object-backend/overview/) |
| Objects loaded (TS v1) | 100,000; ≤3 search-arounds | [limits](https://www.palantir.com/docs/foundry/functions/enforced-limits) |

---

## 2. Data integration layer

- **Datasets** are "the most essential representation of data from when it lands in Foundry through when it is mapped into the Ontology" ([datasets](https://www.palantir.com/docs/foundry/data-integration/datasets)).
  - Transaction types: **SNAPSHOT, APPEND, UPDATE, DELETE**; states: OPEN / COMMITTED / ABORTED; one history per branch ([API](https://www.palantir.com/docs/foundry/api/datasets-resources/transactions/create-transaction)).
  - Semantics **[UNVERIFIED; the page did not render]**: SNAPSHOT replaces the view, APPEND only adds files, UPDATE adds or overwrites, DELETE removes.
- **Backing store** ([G-Cloud 14 service definition](https://assets.applytosupply.digitalmarketplace.service.gov.uk/g-cloud-14/documents/92736/804537709233305-service-definition-document-2024-05-01-1410.pdf)):
  - data sits on "Distributed File Systems (e.g., Hadoop, Amazon S3)";
  - "Cassandra … as its primary metadata and configuration data store";
  - each file has its own AES-256 key, wrapped with RSA-2048 (compare [hadoop-crypto](https://github.com/palantir/hadoop-crypto)).
  - The Spark fork carries "a patched version of Parquet" ([palantir/spark](https://github.com/palantir/spark)). Parquet as the default format is **[UNVERIFIED]**.
- **Transforms:**
  - Python: Spark, or **lightweight** single-node (e.g., `transform_polars`: [API ref](https://www.palantir.com/docs/foundry/api-reference/transforms-python-library/api-transform-polars));
  - Java ([docs](https://www.palantir.com/docs/foundry/transforms-java/overview/)) and SQL.
  - Lightweight transforms went incremental in Sept 2025 and handle about 200M rows / 50 GB ([Sep 2025](https://www.palantir.com/docs/foundry/announcements/2025-09)).
  - Python editing in Code Repositories went "legacy" in June 2026 in favour of VS Code ([Jun 2026](https://www.palantir.com/docs/foundry/announcements/2026-06)).
- **Pipeline Builder**, "Foundry's primary application for data integration" ([overview](https://www.palantir.com/docs/foundry/pipeline-builder/overview)):
  - engines: Spark batch, Flink streaming, and "faster pipelines" "backed by DataFusion" (Rust; [Aug 2026](https://www.palantir.com/docs/foundry/announcements/2026-08));
  - outputs: datasets, media sets, virtual tables, objects, streams.
- **Streaming = Apache Flink** ([white paper](https://www.palantir.com/assets/xrfr7uokpv1b/4X2DdkGvINVPnbkkqwFmAN/191da2f23c31f242468a92a3237c8f3d/Foundry_Streaming_White_Paper_vF_updated_2023.03.30.pdf); [Flink fundamentals](https://www.palantir.com/docs/foundry/data-integration/flink-streaming)). Connectors: Kafka, Pub/Sub, OSI PI, Event Hubs, Kinesis, plus REST push.
- **Data Connection:**
  - "Foundry worker is the recommended way to run all data connection sources"; the on-prem agent worker is legacy ([docs](https://www.palantir.com/docs/foundry/data-connection/foundry-worker-vs-agent-worker));
  - agents now act as **agent proxies** under egress policies (GA Sept 2025) ([Sep 2025](https://www.palantir.com/docs/foundry/announcements/2025-09));
  - agents connect outbound only (secondary: [Vanyar](https://vanyar.com/articles/what-is-palantir-part-2)).
- **HyperAuto** (Software-Defined Data Integration) connects to SAP ERP, Salesforce, NetSuite and others, and auto-generates pipelines, ontology outputs and virtual tables; V2 adds streaming ([docs](https://www.palantir.com/docs/foundry/hyperauto/overview/)).
- **Virtual tables and Iceberg:**
  - virtual tables query external platforms "without first storing the data in a Foundry dataset" ([docs](https://www.palantir.com/docs/foundry/data-integration/virtual-tables));
  - compute can be pushed down to **BigQuery, Databricks, Snowflake** ([pushdown](https://www.palantir.com/docs/foundry/transforms-python/tables-compute-pushdown));
  - **Iceberg tables** are "an alternative resource type", with a managed catalog, REST/external catalogs and branching ([Iceberg](https://www.palantir.com/docs/foundry/data-integration/iceberg-tables)).
- **Global Branching** (GA May 2026) lets teams "branch, test, and merge across applications" ([May 2026](https://www.palantir.com/docs/foundry/announcements/2026-05); [overview](https://www.palantir.com/docs/foundry/global-branching/overview)).
  - Covers: Workshop, Logic, Automate, TS functions, Ontology, Actions, Pipeline Builder, restricted views.
  - Not yet: Python functions, and "The Ontology SDK is not currently branchable" ([details](https://www.palantir.com/docs/foundry/global-branching/best-practices-and-technical-details)).
- **Lineage, health, compute:**
  - Data Lineage covers datasets → transforms → objects ([lineage](https://www.palantir.com/docs/foundry/data-lineage/overview/)); health checks are a dataset concept.
  - **Compute modules** "run serverless Docker images" with horizontal autoscaling ([docs](https://www.palantir.com/docs/foundry/compute-modules/overview/)): SDKs for TypeScript, Python and Java plus an open client spec; they run as *functions* or *pipelines*.

---

## 3. Applications layer

| App | Purpose | Primitives |
|---|---|---|
| Workshop ([docs](https://www.palantir.com/docs/foundry/workshop/overview/)) | Low-code operational apps | object sets, variables, actions, functions, events |
| Slate ([docs](https://www.palantir.com/docs/foundry/slate/overview/)) | Custom HTML/CSS/JS apps | queries, objects, functions, actions |
| Quiver ([docs](https://www.palantir.com/docs/foundry/quiver/overview/)) | Object + time-series analysis | object sets, time-series properties, links |
| Contour ([docs](https://www.palantir.com/docs/foundry/contour/overview/)) | Analysis of "tables at scale" | datasets |
| Object Explorer ([docs](https://www.palantir.com/docs/foundry/object-explorer/overview/)) | Search "anything in the Ontology" | OSS search, object views, link pivots |
| Vertex ([docs](https://www.palantir.com/docs/foundry/vertex/overview/)) | "quantify cause and effect" on system graphs; scenarios | objects, links, time series, models |
| Map ([docs](https://www.palantir.com/docs/foundry/map/overview/)) | Geospatial-temporal analysis | geopoint/geoshape, geotemporal series |
| Notepad ([docs](https://www.palantir.com/docs/foundry/notepad/overview/)) | "object-aware collaborative rich-text editor" | embedded widgets, object refs |
| Machinery ([docs](https://www.palantir.com/docs/foundry/machinery/overview/)) | Process mining/modelling of state changes | state properties, event logs, automations |
| Gotham Gaia / Graph / Dossier | Common operating picture (COP) map, link analysis, briefings **[UNVERIFIED — docs returned 404]** | Gotham objects, links, layers |

**Inference:** apart from Contour, every app is a view over OSS + Actions + Functions and owns no store of its own.

---

## 4. AIP architecture

- **Grounding:** LLMs reach the Ontology through "an extensible tools paradigm", meant to "go beyond the data-centric limitations of retrieval-augmented generation" ([blog, Jan 2024](https://blog.palantir.com/connecting-ai-to-decisions-with-the-palantir-ontology-c73f7b0a1a72)).
  - "Actions can be automatically surfaced as tools for all types of agents".
  - "all agentic activity is controlled with the same security policies that govern human usage" ([blog, Apr 2026](https://blog.palantir.com/connecting-agents-to-decisions-277dee8ddb40)).
  - That Palantir uses the term "ontology-augmented generation" is **[UNVERIFIED]**.
- **AIP Logic**, "a no-code development environment for building, testing, and releasing functions powered by LLMs". It has object-query tools, can emit Ontology edits, and only reads what the user can read ([Logic](https://www.palantir.com/docs/foundry/logic/overview/)).
- **Agent Studio, now "AIP Chatbot Studio"** ([docs](https://www.palantir.com/docs/foundry/agent-studio/overview/)):
  - context from the ontology, documents and functions; deployed via Workshop, OSDK or the API;
  - **pro-code agents** with scoped permissions arrived July 2026 ([Jul 2026](https://www.palantir.com/docs/foundry/announcements/2026-07));
  - **AIP Evolve** (Aug 2026) has AI FDE agents tune AI systems ([Aug 2026](https://www.palantir.com/docs/foundry/announcements/2026-08)).
- **Automate** is "the single entry point for all business automation" ([docs](https://www.palantir.com/docs/foundry/automate/overview/)): time, object-data and streaming triggers; actions, Logic, functions or notifications as effects.
- **AIP Evals:** test cases, evaluators and suites; test cases can be auto-generated ([docs](https://www.palantir.com/docs/foundry/aip-evals/overview/)).
- **Model catalog** ([supported LLMs](https://www.palantir.com/docs/foundry/aip/supported-llms/)):
  - providers: OpenAI, Anthropic, Google, Meta Llama, Mistral, xAI, NVIDIA Nemotron, DeepSeek, Kimi, GLM, Gemma;
  - hosting: direct, Azure, Bedrock, Vertex, or "Palantir-hosted";
  - availability is gated by accreditation level (commercial, IL2/IL4/IL5) ([May](https://www.palantir.com/docs/foundry/announcements/2026-05), [Jun](https://www.palantir.com/docs/foundry/announcements/2026-06));
  - **bring-your-own-model** can be REST-, compute-module- or self-hosted ([AIP overview](https://www.palantir.com/docs/foundry/aip/overview/)).
- **LLM security:** providers "do not store or retain customer prompts or completions"; "strict technical and contractual guarantees" rule out training on customer data ([AIP security](https://www.palantir.com/docs/foundry/aip/aip-security/)). Per-user rate limits were added July 2026.
- **MCP (both GA June 2026, [Jun 2026](https://www.palantir.com/docs/foundry/announcements/2026-06)):**
  - **Palantir MCP** lets AI IDEs "design, build, edit, and review" Foundry applications ([docs](https://www.palantir.com/docs/foundry/palantir-mcp/overview/));
  - **Ontology MCP** "exposes your application's ontology resources as Model Context Protocol (MCP) tools" (objects, actions, queries), bounded by user permissions and "application restrictions" ([docs](https://www.palantir.com/docs/foundry/ontology-mcp/overview/)).

---

## 5. Security and governance

- **Projects and roles** ([docs](https://www.palantir.com/docs/foundry/security/projects-and-roles/)):
  - a project is "the primary boundary for discretionary role grants";
  - roles run Owner > Editor > Viewer > Discoverer, and grants inherit downward;
  - **mandatory controls (Markings, CBAC, Organizations) "will *always* prevent an ineligible user from accessing a resource, regardless of the user's role"**.
- **Markings** ([docs](https://www.palantir.com/docs/foundry/security/markings/)):
  - conjunctive: "a user must be a member of all the Markings";
  - they propagate by folder hierarchy **and by data dependency** ("All resources derived from a marked file … will assume a Marking unless the Marking is explicitly removed");
  - removal requires a central "Expand Access" permission.
- **Classification-based access control (CBAC)** ([docs](https://www.palantir.com/docs/foundry/security/classification-based-access-controls/)):
  - hierarchical levels plus **disjunctive** releasability (Country A *or* B);
  - data classifications propagate along dependencies.
  - Gotham applies mandatory controls "through provenance and lineage", plus "granular row or column-level controls based on a user's attributes" ([Gotham](https://www.palantir.com/docs/gotham/security/overview/)).
- **Restricted views** ([docs](https://www.palantir.com/docs/foundry/security/restricted-views/)):
  - row-level policies compare user attributes (UUIDs) with column values or marking columns, and can back object types;
  - they cannot be transform inputs;
  - the docs list exclusions such as "writeback operations, or exports" (context to verify).
- **Checkpoints** require a justification across 60+ interactions ([docs](https://www.palantir.com/docs/foundry/checkpoints/overview/)).
- **Cipher** manages encryption, hashing and pseudonymisation: "channels" hold keys, "licenses" grant permissions, and there is a CipherText property type ([docs](https://www.palantir.com/docs/foundry/cipher/overview/)).
- **Sensitive Data Scanner** matches patterns and triggers automated actions, including OCR and audio scanning of media ([docs](https://www.palantir.com/docs/foundry/sensitive-data-scanner/overview/)).
- **Audit and retention** ([audit](https://www.palantir.com/docs/foundry/security/audit-logs-overview/); [retention](https://www.palantir.com/docs/foundry/retention/overview/)):
  - the audit.3 schema has ~15 min latency, with categories such as `dataExport`, `dataLoad`, `userLogin`, `dataDelete`;
  - SIEM pull uses the `list-log-files` API; exports are retained ≤730 days;
  - retention policies delete dataset transactions.
- **Peering:** "Peer Manager" (beta, Sept 2025) shares ontology resources "across distinct enrollments" ([Sep 2025](https://www.palantir.com/docs/foundry/announcements/2025-09)). The internals of Gotham **Nexus Peering** (replication with ACL/classification mapping) are **[UNVERIFIED]**.
- **Accreditations:**
  - postings cite "IL-5 and FedRAMP" ([Apollo posting](https://jobs.lever.co/palantir/afea07a8-2721-45e6-a9ca-6580f3f9783c));
  - Palantir's SaaS is one of five DoD IL5-authorised offerings ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies));
  - the docs mention IL2/IL4/IL5 enrollments;
  - FedRAMP High, IL6 and Top Secret authorisations are **[UNVERIFIED]** here.

---

## 6. Apollo

- **Architecture** ([overview](https://www.palantir.com/docs/apollo/core/overview/); [how it works](https://www.palantir.com/docs/apollo/core/how-apollo-works/)):
  - a **Hub**'s Orchestration Engine issues **Plans**; **Spoke** agents report state and telemetry and "continuously poll the Orchestration Engine for new Plans";
  - hubs can manage other hubs;
  - an environment is "a grouping of Entities" (Helm charts, assets) in one Kubernetes cluster, and it need "not … be managed by a Hub at all times" (the example is vehicles) ([environments](https://www.palantir.com/docs/apollo/core/environments/)).
- **Releases** ([products](https://www.palantir.com/docs/apollo/core/products-releases-versions/)):
  - products are identified by Maven coordinates and have orderable versions (release, snapshot, RC);
  - **release channels** are fed by promotion pipelines;
  - a release that fails health checks is **recalled**.
- **Constraints** ([plans & constraints](https://www.palantir.com/docs/apollo/core/plans-and-constraints/)):
  - types: maintenance windows, dependency ranges, suppression windows (created automatically after failures), missing artifacts;
  - failed plans trigger automatic **rollback Plans**.
- **Air-gap:** environments marked "No Connection Expected" are exported as **Bundles** for one-way import; multi-hub chains (A → B → C) are supported ([export](https://www.palantir.com/docs/apollo/export-import/exporting-environments/)).
- **Rubix** is Palantir's "effort to rebuild our cloud architecture around Kubernetes" (blog, May 2019) ([Rubix](https://blog.palantir.com/introducing-rubix-kubernetes-at-palantir-ab0ce16ea42e)).
  - It brought autoscaling clusters and Spark-on-Kubernetes via a Go gang scheduler ([k8s-spark-scheduler](https://github.com/palantir/k8s-spark-scheduler)).
  - Postings cite "100s of K8s clusters", air-gapped sites and "edge devices" ([Substrate](https://jobs.lever.co/palantir/c38bb622-6f8e-43a1-8ae7-32291da328a8)).
  - **Inference:** Rubix is the substrate; Apollo is the delivery brain on top of it.

| Open-source analogue | Covers | Gap vs Apollo |
|---|---|---|
| [Zarf](https://github.com/zarf-dev/zarf) (Go, Apache-2.0) | Air-gap packages, built-in registry + Gitea, SBOMs, cosign signing ([docs](https://docs.zarf.dev/)) | No fleet orchestration |
| [UDS Core](https://github.com/defenseunicorns/uds-core) (AGPL-3.0) | Zarf + Istio, Keycloak, Pepr, Prometheus/Loki, Falco, Velero | AGPL |
| [Fleet](https://fleet.rancher.io/) / [Flux](https://fluxcd.io/flux/installation/configuration/air-gapped/) (CNCF graduated) | GitOps across clusters; mirrored images for air-gap | No constraint solver |
| [argocd-agent](https://argocd-agent.readthedocs.io/latest/) | Hub/spoke "principal/agent" | "not yet feature complete" |
| [Kairos](https://github.com/kairos-io/kairos) (CNCF sandbox; v4.1.2, June 2026) | Immutable edge OS, A/B upgrades, secure boot | OS layer only |
| [Replicated](https://docs.replicated.com/intro-replicated) (commercial) | k0s Embedded Cluster, licence entitlements, channels, air-gap bundles | Vendor-hosted hub |

---

## 7. Languages and infrastructure

| Repo | Language | Note |
|---|---|---|
| [conjure](https://github.com/palantir/conjure) | Java | Interface-definition language (IDL) → IR → Java/TS/Python/[Rust](https://github.com/palantir/conjure-rust)/[Go](https://github.com/palantir/conjure-go) generators |
| [blueprint](https://github.com/palantir/blueprint) | TypeScript | ~22k stars, Apache-2.0 |
| [osdk-ts](https://github.com/palantir/osdk-ts), [foundry-platform-typescript](https://github.com/palantir/foundry-platform-typescript), [pack](https://github.com/palantir/pack) | TypeScript | OSDK, platform SDK, "PACK" app kit (alpha) |
| [foundry-platform-python](https://github.com/palantir/foundry-platform-python) | Python | Platform SDK |
| [atlasdb](https://github.com/palantir/atlasdb) | Java | Transactions over Cassandra/Postgres; archived Nov 2024 |
| [tritium](https://github.com/palantir/tritium) | Java | Instrumentation; active (v0.134.0, July 2026) |
| [witchcraft-go-server](https://github.com/palantir/witchcraft-go-server) | Go | Server framework; archived Apr 2026 |
| [gradle-baseline](https://github.com/palantir/gradle-baseline), [palantir-java-format](https://github.com/palantir/palantir-java-format) | Java | Build/quality baseline |
| [policy-bot](https://github.com/palantir/policy-bot), [bulldozer](https://github.com/palantir/bulldozer) | Go | Pull-request governance bots |
| [spark](https://github.com/palantir/spark) | Scala | Fork; Parquet predicate-pushdown patches |
| [hadoop-crypto](https://github.com/palantir/hadoop-crypto) | Java | Per-file encryption; archived May 2026 |
| rust-zipkin, dialogue, distgo ([listing](https://api.github.com/search/repositories?q=org:palantir+archived:false&sort=stars&order=desc&per_page=40&page=2)) | Rust, Java, Go | Tracing, HTTP client, Go builds |

- **Job postings:**
  - "backend languages, including Java, Rust, and Go"; "Cassandra, ElasticSearch, Spark, Kafka, Kubernetes, Flink"; "Gradle and GitHub" ([infra](https://jobs.lever.co/palantir/6fe5515f-f677-4d98-8ac2-1775a425f5e7));
  - Apollo: "Java, Golang, C++".
- **Conclusion (inference):**
  - **Java** — backbone services, Conjure RPC, the data plane;
  - **Go** — infrastructure, agents, tooling;
  - **TypeScript** — all UIs plus the OSDK;
  - **Python** — transforms, ML, SDKs;
  - **Scala** — Spark only;
  - **Rust** — rising on performance paths (DataFusion);
  - Cassandra and Elasticsearch dominate storage, while Postgres is optional (AtlasDB).

---

## 8. Comparable platforms

| Platform | Documented model | Lesson for Vigil |
|---|---|---|
| **Anduril Lattice** | An entity is "a bag of components". Required: `entity_id`, `is_live`, `expiry_time` (<30 days), provenance. Templates: ASSET/TRACK/GEO ([entities](https://developer.anduril.com/guides/entities/overview)). Tasks go `STATUS_SENT`→`DONE_OK/NOT_OK` ([tasks](https://developer.anduril.com/guides/tasks/overview.md)). Objects API. SDKs: Python, TS, Java, Go. gRPC uses 1.3 Mbps vs 2 Mbps for REST ([protocol](https://developer.anduril.com/guides/best-practices/choose-a-protocol.md)). Lattice Mesh **[UNVERIFIED]** | Composition + TTL for edge data; tasks = actions for machines |
| **Cognite** | **Containers** (physical storage, ≤10 indexes) vs **views** (logical, versioned, `implements`); nodes/edges; auto-generated GraphQL ([views](https://docs.cognite.com/cdf/dm/dm_concepts/dm_containers_views_datamodels), [instances](https://docs.cognite.com/cdf/dm/dm_concepts/dm_spaces_instances), [GraphQL](https://docs.cognite.com/cdf/dm/dm_graphql/)) | Keep physical and logical schema separate |
| **C3 AI** | "C3 AI Types" over NoSQL, key-value, RDBMS, HDFS and vector stores ([platform](https://c3.ai/c3-agentic-ai-platform/)); details **[UNVERIFIED]** | Same polyglot thesis as Palantir |
| **Azure Digital Twins** | DTDL v2/v3: Property, Relationship, Component; `extends` depth ≤10; multiplicity not enforced ([ADT](https://learn.microsoft.com/en-us/azure/digital-twins/concepts-models)) | Use DTDL for import/export only |
| **Eclipse Ditto** | Thing = attributes + features (properties / desiredProperties) + Policy; MQTT/Kafka connectivity; MongoDB ([overview](https://eclipse.dev/ditto/basic-overview.html)) | Desired-vs-reported state for devices |
| **Bentley iTwin** | "distributed relational database, based on SQLite"; briefcases + ChangeSets; ECSQL ([iModels](https://www.itwinjs.org/learning/imodels/)) | Changesets for offline replicas |
| **Quantexa** | "Dynamic Entity Resolution re-generates the entity in real-time from the underlying raw data" ([ER](https://www.quantexa.com/entity-resolution/)) | Resolved entities are a *view*, not a destructive merge |
| **Senzing** | Principle-based, no training, real-time, embeddable ([Senzing](https://senzing.com/what-is-entity-resolution/)); gRPC SDKs ([garage](https://github.com/senzing-garage)) | Buy or embed entity resolution |

---

## 9. Implementation options for the ontology

| Option | Facts | Fit |
|---|---|---|
| Neo4j | Community GPLv3, Enterprise commercial ([licensing](https://neo4j.com/licensing/)); "most mandatory GQL features" ([conformance](https://neo4j.com/docs/cypher-manual/current/appendix/gql-conformance/)) | Analytics projection |
| JanusGraph | Java, Apache-2.0, v1.1.0 (2024); Cassandra/HBase + ES; Gremlin ([repo](https://github.com/JanusGraph/janusgraph)) | Heavy to operate |
| Apache AGE | Extension for PostgreSQL 11–18; openCypher; v1.8.0 ([repo](https://github.com/apache/age)) | Best graph-in-Postgres; keeps RLS |
| Memgraph | In-memory C++; BSL community / MEL enterprise ([repo](https://github.com/memgraph/memgraph)) | Licence limits |
| NebulaGraph | C++, Apache-2.0, Raft, nGQL ([repo](https://github.com/vesoft-inc/nebula)) | Scale-out analytics |
| Kùzu | **Archived 10 Oct 2025** ("working on something new!"); MIT, last 0.11.3 ([repo](https://github.com/kuzudb/kuzu)); fork LadybugDB ([repo](https://github.com/LadybugDB/ladybug)) | Vendor-viability warning |
| TypeDB 3 | Rust rewrite, MPL-2.0, TypeQL, functions ([repo](https://github.com/typedb/typedb)) | Small ecosystem |
| RDF/OWL/SHACL | Palantir's 2006 "dynamic ontology" patent uses proprietary type metadata, not RDF/OWL ([US7962495B2](https://patents.google.com/patent/US7962495B2/en)) | Interoperability only |
| GQL / SQL/PGQ | ISO/IEC 39075:2024 ([ISO](https://www.iso.org/standard/76120.html)); ISO/IEC 9075-16:2023 ([ISO](https://www.iso.org/standard/79473.html)). Implemented by Oracle 23ai `GRAPH_TABLE` ([Oracle](https://docs.oracle.com/en/database/oracle/property-graph/23.4/spgdg/sql-property-graphs.html)), Spanner Graph ([Google](https://docs.cloud.google.com/spanner/docs/graph/overview)), DuckPGQ research ([site](https://duckpgq.org/)); PostgreSQL core **[UNVERIFIED]** | Standards emerging |
| PuppyGraph | Graph queries over Iceberg/Delta/Postgres/Snowflake/Databricks; Gremlin + openCypher; free single-node edition ([site](https://www.puppygraph.com/)) | Zero-ETL projection |

- **Why not RDF (inference; no Palantir statement found):**
  - operational use needs **closed-world, primary-keyed, typed** records with **transactional, permissioned writes**;
  - it needs marking- and row-level security on every read, plus search, geo and time-series performance;
  - triple stores are optimised for open-world inference and federation instead.
- **Recommendation: build the ontology as a metadata-driven layer over polyglot persistence, not "a graph database".**
  1. It is Palantir's proven design.
  2. Operational traversals are shallow (Palantir caps them at 3 search-arounds), so indexed foreign keys and join tables suffice.
  3. Security pushdown is mature in Postgres row-level security (RLS) and search engines.
  4. Graph-DB viability and licensing risk is real (see Kùzu).
  5. Deep analytics can use a replaceable projection (AGE, PuppyGraph, DuckPGQ) behind the same Object Set API.

---

## Implications for Vigil

### (a) Minimum viable primitives, in build order

1. **Security kernel:** resource IDs, orgs/spaces/projects, roles, conjunctive markings, classification with disjunctive releasability, append-only audit.
2. **Versioned, branchable datasets** on an Iceberg lakehouse.
3. **Build service:** batch, lightweight and streaming, with lineage and marking propagation.
4. **Connectors** plus an outbound-only on-prem/edge agent.
5. **Ontology Metadata Service:** object, property, link, interface, action and function types; versioned and branchable.
6. **Indexing funnel:** changelog → merge edits by primary key → index → hydrate.
7. **Object Set Service:** filter, search-around, set algebra, aggregation, security pushdown.
8. **Actions service:** rules, parameters, submission criteria, one pre-commit writeback webhook, post-commit side effects, edit history.
9. **Functions runtime** (TS/Python/Java), sandboxed and with enforced limits.
10. **OSDK code generation** plus an Ontology MCP server.
11. **Core apps:** object explorer/views, map, time series, graph pivot; low-code builder later.
12. **AI layer:** model gateway; tools = object sets/actions/functions executed as the user; evals; automations.
13. **Delivery:** air-gap packaging → GitOps fleet → constraint-aware hub/spoke orchestrator.

### (b) Build vs adopt (†: not verified in this session)

| Primitive | Adopt | Build (Vigil IP) |
|---|---|---|
| Identity / authorization | Keycloak†; SpiceDB/OpenFGA† for discretionary roles | Marking/CBAC evaluator + lineage propagation |
| Row security | PostgreSQL RLS; search-engine DLS† | Compiler from ontology policies to filters |
| Lake | S3-compatible store†, Apache Iceberg, REST catalog (Polaris/Nessie†) | Transaction semantics, retention |
| Compute | Spark, Flink, DataFusion/Polars/DuckDB, Kafka | Transform SDK, scheduler, lineage |
| Connectors | Debezium†, Kafka Connect†, NiFi/MiNiFi† | SAP/ERP accelerators later |
| OMS / Funnel / OSS / Actions | — | **Build** (Java 21/Spring Boot) |
| Object stores | Postgres + PostGIS† + pgvector†; OpenSearch†; time-series DB | Store adapters behind OSS |
| Functions | Node/Deno, CPython in gVisor/Firecracker† | Runtime, limits, SDK injection |
| IDL / SDKs | Conjure (Apache-2.0) or Protobuf + Buf | Ontology-schema generators |
| Graph / entity resolution | AGE or PuppyGraph; Senzing | Integration |
| AI | vLLM†, OpenAI-compatible gateway† | Tool layer, evals, automations, MCP |
| Delivery / UI | Zarf, Flux/Fleet, Kairos, k3s/RKE2†; Blueprint, MapLibre† | Channels + constraints ("Apollo-lite"); app builder |

### (c) Design recommendations

1. **Physical ≠ logical schema.** Generate a typed Postgres table per object type from OMS metadata and index it into search. Implement links as foreign keys or join tables. Avoid entity-attribute-value (EAV) tables on hot paths.
2. **Markings are data.**
   - Conjunctive semantics become array containment, with a custom predicate for releasability.
   - Derived markings are the union of input markings.
   - Unmarking requires an audited grant.
   ```sql
   -- illustrative only
   ALTER TABLE obj_pump ENABLE ROW LEVEL SECURITY;
   CREATE POLICY p_markings ON obj_pump
     USING (markings <@ current_setting('vigil.user_markings')::text[]);
   ```
3. **An Action = one DB transaction + an outbox.**
   - Call the writeback webhook synchronously, before commit.
   - Commit the edits, edit log and outbox atomically.
   - Make side effects idempotent.
   - Adopt Palantir-like caps (10k objects, 3 MB per edit).
4. **Edits are events.** Derive current state, history, audit and decision lineage from an append-only edit log, and materialise it to Iceberg.
5. **Make conflict precedence explicit, per property:** source-wins, edit-wins, or latest-timestamp. Palantir documents "most recent transaction wins".
6. **Branch everything from v1** (datasets, schema, indexes). Palantir only reached GA in May 2026, and retrofitting is costly.
7. **Design edge-first.**
   - Lattice-style components with expiry, plus Ditto-style desired-vs-reported state.
   - gRPC on constrained links.
   - Changeset sync for disconnected sites, which matters for African low-bandwidth deployments [Inference].
8. **Languages:**
   - Java 21/Spring Boot for the control plane;
   - Go for agents and delivery tooling;
   - Python for transforms and ML;
   - TypeScript/Next.js for apps and functions;
   - Rust later, for DataFusion query paths and edge agents.
9. **AI runs as the user.**
   - Allow-list the actions each application may call.
   - Stage writes for human approval.
   - Expose the same tools via MCP.
   - Self-host open-weight models for sovereign and air-gapped sites.
10. **Plan for backend swaps.** Palantir's OSv1→OSv2 migration broke APIs and lost edit history. Keep object stores behind adapters, with the event log as the source of truth.

---

## Research limitations

- The WebSearch quota was exhausted after about 20 queries.
- WebFetch returned HTTP 429 for three pages:
  - palantir.com/docs/foundry/object-edits/overview/
  - palantir.com/docs/foundry/functions/api-object-sets
  - patents.google.com/patent/US11914566B2/en
- These pages returned 404 or navigation only: Gotham app docs, Foundry peering docs, dataset-transaction semantics, accreditation pages, Anduril Lattice Mesh, and C3 AI Type System docs.
- PostgreSQL devel docs are blocked by robots.txt.

## Sources

**Palantir docs — Ontology and backend**
- Ontology: https://www.palantir.com/docs/foundry/ontology/overview/ · https://www.palantir.com/docs/foundry/ontology/why-ontology · https://www.palantir.com/docs/foundry/architecture-center/ontology-system
- Object and link types: https://www.palantir.com/docs/foundry/object-link-types/object-types-overview/ · https://www.palantir.com/docs/foundry/object-link-types/base-types/ · https://www.palantir.com/docs/foundry/object-link-types/link-types-overview/ · https://www.palantir.com/docs/foundry/object-link-types/create-link-type/ · https://www.palantir.com/docs/foundry/interfaces/interface-overview/
- Actions: https://www.palantir.com/docs/foundry/action-types/overview/ · https://www.palantir.com/docs/foundry/action-types/webhooks/ · https://www.palantir.com/docs/foundry/action-types/scale-property-limits
- Functions: https://www.palantir.com/docs/foundry/functions/overview/ · https://www.palantir.com/docs/foundry/functions/enforced-limits
- Backend and indexing: https://www.palantir.com/docs/foundry/object-backend/overview/ · https://www.palantir.com/docs/foundry/object-backend/object-storage-v2-breaking-changes · https://www.palantir.com/docs/foundry/object-indexing/overview · https://www.palantir.com/docs/foundry/object-indexing/funnel-batch-pipelines/ · https://www.palantir.com/docs/foundry/object-indexing/funnel-streaming-pipelines

**Palantir docs — data integration**
- Datasets and transforms: https://www.palantir.com/docs/foundry/data-integration/datasets · https://www.palantir.com/docs/foundry/api/datasets-resources/transactions/create-transaction · https://www.palantir.com/docs/foundry/transforms-java/overview/ · https://www.palantir.com/docs/foundry/api-reference/transforms-python-library/api-transform-polars
- Pipelines and streaming: https://www.palantir.com/docs/foundry/pipeline-builder/overview · https://www.palantir.com/docs/foundry/data-integration/flink-streaming
- Connectivity: https://www.palantir.com/docs/foundry/data-connection/foundry-worker-vs-agent-worker · https://www.palantir.com/docs/foundry/hyperauto/overview/ · https://www.palantir.com/docs/foundry/data-integration/virtual-tables · https://www.palantir.com/docs/foundry/transforms-python/tables-compute-pushdown · https://www.palantir.com/docs/foundry/data-integration/iceberg-tables
- Branching, lineage, compute: https://www.palantir.com/docs/foundry/global-branching/overview · https://www.palantir.com/docs/foundry/global-branching/best-practices-and-technical-details · https://www.palantir.com/docs/foundry/data-lineage/overview/ · https://www.palantir.com/docs/foundry/compute-modules/overview/

**Palantir docs — applications**
- https://www.palantir.com/docs/foundry/workshop/overview/ · https://www.palantir.com/docs/foundry/slate/overview/ · https://www.palantir.com/docs/foundry/quiver/overview/ · https://www.palantir.com/docs/foundry/contour/overview/ · https://www.palantir.com/docs/foundry/object-explorer/overview/ · https://www.palantir.com/docs/foundry/vertex/overview/ · https://www.palantir.com/docs/foundry/map/overview/ · https://www.palantir.com/docs/foundry/notepad/overview/ · https://www.palantir.com/docs/foundry/machinery/overview/

**Palantir docs — AIP**
- https://www.palantir.com/docs/foundry/aip/overview/ · https://www.palantir.com/docs/foundry/aip/aip-security/ · https://www.palantir.com/docs/foundry/aip/supported-llms/ · https://www.palantir.com/docs/foundry/logic/overview/ · https://www.palantir.com/docs/foundry/agent-studio/overview/ · https://www.palantir.com/docs/foundry/automate/overview/ · https://www.palantir.com/docs/foundry/aip-evals/overview/ · https://www.palantir.com/docs/foundry/palantir-mcp/overview/ · https://www.palantir.com/docs/foundry/ontology-mcp/overview/

**Palantir docs — security**
- https://www.palantir.com/docs/foundry/security/projects-and-roles/ · https://www.palantir.com/docs/foundry/security/markings/ · https://www.palantir.com/docs/foundry/security/classification-based-access-controls/ · https://www.palantir.com/docs/foundry/security/restricted-views/ · https://www.palantir.com/docs/foundry/checkpoints/overview/ · https://www.palantir.com/docs/foundry/cipher/overview/ · https://www.palantir.com/docs/foundry/sensitive-data-scanner/overview/ · https://www.palantir.com/docs/foundry/security/audit-logs-overview/ · https://www.palantir.com/docs/foundry/retention/overview/ · https://www.palantir.com/docs/gotham/security/overview/

**Palantir release announcements**
- https://www.palantir.com/docs/foundry/announcements/2025-09 · https://www.palantir.com/docs/foundry/announcements/2026-05 · https://www.palantir.com/docs/foundry/announcements/2026-06 · https://www.palantir.com/docs/foundry/announcements/2026-07 · https://www.palantir.com/docs/foundry/announcements/2026-08

**Apollo docs**
- https://www.palantir.com/docs/apollo/core/overview/ · https://www.palantir.com/docs/apollo/core/how-apollo-works/ · https://www.palantir.com/docs/apollo/core/environments/ · https://www.palantir.com/docs/apollo/core/products-releases-versions/ · https://www.palantir.com/docs/apollo/core/plans-and-constraints/ · https://www.palantir.com/docs/apollo/export-import/exporting-environments/

**Palantir blog, public-sector filing, patent, job postings**
- Blog: https://blog.palantir.com/connecting-ai-to-decisions-with-the-palantir-ontology-c73f7b0a1a72 · https://blog.palantir.com/connecting-agents-to-decisions-277dee8ddb40 · https://blog.palantir.com/ontology-oriented-software-development-68d7353fdb12 · https://blog.palantir.com/introducing-rubix-kubernetes-at-palantir-ab0ce16ea42e
- Other: https://www.palantir.com/assets/xrfr7uokpv1b/4X2DdkGvINVPnbkkqwFmAN/191da2f23c31f242468a92a3237c8f3d/Foundry_Streaming_White_Paper_vF_updated_2023.03.30.pdf · https://assets.applytosupply.digitalmarketplace.service.gov.uk/g-cloud-14/documents/92736/804537709233305-service-definition-document-2024-05-01-1410.pdf · https://patents.google.com/patent/US7962495B2/en
- Jobs: https://jobs.lever.co/palantir/6fe5515f-f677-4d98-8ac2-1775a425f5e7 · https://jobs.lever.co/palantir/c38bb622-6f8e-43a1-8ae7-32291da328a8 · https://jobs.lever.co/palantir/afea07a8-2721-45e6-a9ca-6580f3f9783c

**GitHub (Palantir)**
- https://github.com/palantir · https://api.github.com/search/repositories?q=org:palantir+archived:false&sort=stars&order=desc&per_page=40&page=2 · https://github.com/palantir/conjure · https://github.com/palantir/conjure-rust · https://github.com/palantir/conjure-go · https://github.com/palantir/atlasdb · https://github.com/palantir/tritium · https://github.com/palantir/witchcraft-go-server · https://github.com/palantir/gradle-baseline · https://github.com/palantir/osdk-ts · https://github.com/palantir/foundry-platform-python · https://github.com/palantir/pack · https://github.com/palantir/k8s-spark-scheduler · https://github.com/palantir/spark · https://github.com/palantir/hadoop-crypto

**Comparable platforms**
- Anduril Lattice: https://developer.anduril.com/ · https://developer.anduril.com/llms.txt · https://developer.anduril.com/guides/entities/overview · https://developer.anduril.com/guides/tasks/overview.md · https://developer.anduril.com/guides/best-practices/choose-a-protocol.md
- Cognite: https://docs.cognite.com/cdf/dm/dm_concepts/dm_containers_views_datamodels · https://docs.cognite.com/cdf/dm/dm_concepts/dm_spaces_instances · https://docs.cognite.com/cdf/dm/dm_graphql/
- Others: https://c3.ai/c3-agentic-ai-platform/ · https://learn.microsoft.com/en-us/azure/digital-twins/concepts-models · https://eclipse.dev/ditto/basic-overview.html · https://www.itwinjs.org/learning/imodels/ · https://www.itwinjs.org/bis/ · https://www.quantexa.com/entity-resolution/ · https://senzing.com/what-is-entity-resolution/ · https://github.com/senzing-garage

**Graph databases and standards**
- https://neo4j.com/licensing/ · https://neo4j.com/docs/cypher-manual/current/appendix/gql-conformance/ · https://github.com/JanusGraph/janusgraph · https://github.com/apache/age · https://github.com/memgraph/memgraph · https://github.com/vesoft-inc/nebula · https://github.com/kuzudb/kuzu · https://github.com/LadybugDB/ladybug · https://github.com/typedb/typedb · https://www.iso.org/standard/76120.html · https://www.iso.org/standard/79473.html · https://docs.oracle.com/en/database/oracle/property-graph/23.4/spgdg/sql-property-graphs.html · https://docs.cloud.google.com/spanner/docs/graph/overview · https://duckpgq.org/ · https://www.puppygraph.com/

**Deployment tooling**
- https://docs.zarf.dev/ · https://github.com/zarf-dev/zarf · https://github.com/defenseunicorns/uds-core · https://fleet.rancher.io/ · https://fluxcd.io/flux/installation/configuration/air-gapped/ · https://argocd-agent.readthedocs.io/latest/ · https://github.com/kairos-io/kairos · https://docs.replicated.com/intro-replicated

**Secondary sources**
- https://www.puppygraph.com/blog/palantir-ontology · https://pythonebasta.medium.com/understanding-palantirs-ontology-semantic-kinetic-and-dynamic-layers-explained-c1c25b39ea3c · https://vanyar.com/articles/what-is-palantir-part-2 · https://en.wikipedia.org/wiki/Palantir_Technologies
