<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Raw research appendix (P0). Sourced analyst report; verify [UNVERIFIED] items before external use. -->

> **Appendix to** [`VIGIL_RESEARCH_DOSSIER.md`](../VIGIL_RESEARCH_DOSSIER.md). A raw research report produced on 24–25 Sep 2026 by a parallel research agent. Kept verbatim for traceability. Where it conflicts with the dossier, the dossier wins (the dossier applies the founders' greenfield decision).
>
> **Internal working paper; not for external distribution.**

# Vigil — Polyglot Platform Architecture & Building Blocks (Report 05)

*Principal-architect briefing for the Vigil founding team · status as of 24 September 2026*

**Method and confidence.** I ran about 35 searches before the shared session search quota ran out, then read about 190 primary sources directly: repos, LICENSE files, release notes and foundation pages. After that the fetch proxy refused requests (HTTP 429). Unread pages: Mesa and Eclipse Ditto repos, grafana.com/licensing, the Prometheus 3.0 post, redis.io AGPL post, valkey.io, openbao.org, HashiCorp's IBM post, the OpenJDK JDK 25 page and Wikipedia's Redis page. Anything depending on them, or not re-read this session, is marked **[UNVERIFIED]**. Sentences with a linked source are facts; **Rec:** marks my recommendations.

---

## 0. Executive summary

- **Five runtimes with hard boundaries:** Java 25/Kotlin (ontology, Actions, domain logic, Android app), TypeScript (cockpit, SDKs), Python (AI, data science, user transforms), Rust (data plane, edge agent, policy core, WASM host), Go (control plane, operators, Apollo-like delivery, CLIs).
- **Elixir is a bounded sixth language** for the real-time/messaging gateway only. C/C++ is consumed, never authored.
- **One of each foundation:** contracts (Protobuf + Buf, CloudEvents, Arrow), monorepo, telemetry (OpenTelemetry), and policy core (Rust, compiled to WASM for every runtime).
- **Postgres-centric core:** PG 18 → 19 (SQL/PGQ) over an Iceberg v3 lakehouse on self-hosted S3; NATS at the edge, Kafka in the core; Temporal for write-back sagas; OpenFGA plus built-in mandatory markings; Zarf/Harbor/Flux for air-gapped delivery.
- **Licences:** anything Vigil ships must be permissive and preferably foundation-governed. MinIO (archived Apr 2026), Kùzu (archived Oct 2025) and RapidPro (moved to BSL) show why.

---

## 1. Language charter

### 1.1 Rust — data plane, ingestion, edge agents, policy enforcement, WASM

- **Memory safety.** In 2025, memory-safety bugs fell below 20% of Android vulnerabilities for the first time. Rust code shows about 0.2 vulnerabilities per million lines, against about 1,000 for C/C++. Rust changes also have a roughly 4× lower rollback rate and spend 25% less time in review ([Google, Nov 2025](https://blog.google/security/rust-in-android-move-fast-fix-things/)).
- **Government guidance.**
  - CISA and NSA call memory-safe languages "the most comprehensive mitigation" ([CISA, Jun 2025](https://www.cisa.gov/news-events/alerts/2025/06/24/new-guidance-released-reducing-memory-related-vulnerabilities); [CSI PDF](https://media.defense.gov/2025/Jun/23/2003742198/-1/-1/0/CSI_MEMORY_SAFE_LANGUAGES_REDUCING_VULNERABILITIES_IN_MODERN_SOFTWARE_DEVELOPMENT.PDF)).
  - Vendors were pushed to publish memory-safety roadmaps by 2026 ([CISA roadmaps](https://www.cisa.gov/case-memory-safe-roadmaps); [TechRepublic](https://www.techrepublic.com/article/cisa-fbi-memory-safety-recommendations/)).
  - Microsoft's widely quoted "~70% of CVEs are memory safety" figure is [UNVERIFIED] this session ([MSRC 2019](https://msrc.microsoft.com/blog/2019/07/a-proactive-approach-to-more-secure-code/)).
- **Production cases.**
  - Cloudflare rewrote its core proxy (FL2) in Rust: 10 ms faster responses, 50% less CPU and less than half the memory ([InfoQ, Oct 2025](https://www.infoq.com/news/2025/10/cloudflare-rust-proxy)). Its Pingora framework is open source ([Cloudflare](https://blog.cloudflare.com/pingora-open-source/)).
  - Discord moved its Read States service from Go to Rust to eliminate GC latency spikes ([Discord](https://discord.com/blog/why-discord-is-switching-from-go-to-rust)).
  - AWS Firecracker is Rust [UNVERIFIED].
- **Data-engine ecosystem.**
  - DataFusion 55 (25 Aug 2026) had a record 175 contributors. It underpins Comet, delta-rs, ParadeDB and Ballista ([DataFusion](https://datafusion.apache.org/blog/output/2026/08/25/datafusion-55.0.0/)).
  - InfluxDB 3 is Rust on Arrow, DataFusion and Parquet, under MIT/Apache-2.0 ([InfluxData](https://www.influxdata.com/blog/influxdb3-open-source-public-alpha/)).
  - Qdrant, LanceDB, RisingWave, Lakekeeper and SedonaDB are all Rust (see §4). Polars and Vector.dev are Rust too [UNVERIFIED licences].

**Rec:** Rust owns the edge/ingest agent, the markings/policy core, the object-index and traversal services (DataFusion + Arrow), the Wasmtime host for user-defined functions (UDFs) and plugins, and industrial protocol adapters. Hire 2–3 senior Rust engineers early; nothing else goes into Rust.

### 1.2 Go — control plane, operators, delivery, CLIs, connectors

- **Cloud-native control-plane software is predominantly Go.** Examples: Temporal server (99.4% Go, MIT) ([repo](https://github.com/temporalio/temporal)), NATS ([repo](https://github.com/nats-io/nats-server)), SpiceDB ([repo](https://github.com/authzed/spicedb)), OpenFGA ([repo](https://github.com/openfga/openfga)), Zarf ([repo](https://github.com/zarf-dev/zarf)), Talos ([repo](https://github.com/siderolabs/talos)), SeaweedFS ([repo](https://github.com/seaweedfs/seaweedfs)) and lakeFS ([repo](https://github.com/treeverse/lakeFS)).
- **Uber** converged new backend services onto Java and Go ([Pragmatic Engineer](https://newsletter.pragmaticengineer.com/p/consolidating-technologies)). It builds its Go monorepo with Bazel ([Uber](https://www.uber.com/gb/en/blog/go-monorepo-bazel/)).
- **Microsoft** ported the TypeScript compiler to Go: TypeScript 7.0 builds 8–12× faster ([InfoQ, Aug 2026](https://www.infoq.com/news/2026/08/typescript-7-released/)).

**Rec:** Go is used for Kubernetes operators, the Apollo-like fleet controller, CLIs, NATS-side glue and simple connectors.

### 1.3 Java/Kotlin (JVM) — ontology, transactional domain logic, big-data ecosystem

- **Java 25 is the current LTS** (Sept 2025) ([JetBrains](https://blog.jetbrains.com/idea/2025/09/java-25-lts-and-intellij-idea/)). Since JDK 24, virtual threads no longer pin carrier threads on `synchronized` (JEP 491) ([OpenJDK](https://openjdk.org/jeps/491)).
- **Spring.** Spring Boot 4.0 (20 Nov 2025) runs on Spring Framework 7. It adds first-class Java 25 support, modularised jars, JSpecify null-safety and API versioning ([Spring](https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now/)). Spring Modulith 2.0 went GA the next day ([Spring](https://spring.io/blog/2025/11/21/spring-modulith-2-0-ga-1-4-5-and-1-3-11-released/)).
- **The data ecosystem is JVM:** Kafka 4.x, Flink 2.x, Spark 4.x, Iceberg, Polaris, OpenSearch and Keycloak (§4).
- **GraalVM caveat.** On 15 Sep 2025 Oracle said GraalVM for JDK 24 was the last GraalVM release shipped as part of Java SE. Work on startup time and footprint moves to OpenJDK's Project Leyden, and GraalVM now focuses on GraalPy and GraalJS ([Oracle](https://blogs.oracle.com/java/detaching-graalvm-from-the-java-ecosystem-train)). GraalVM 25 Community Edition still ships Native Image ([InfoWorld](https://www.infoworld.com/article/4061937/graalvm-25-arrives-backed-by-jdk-25.html)).
- **Kotlin and mobile.** In August 2026 Android held 81.4% of mobile OS share in Africa ([StatCounter](https://gs.statcounter.com/os-market-share/mobile/africa)) and 75.2% in South Africa ([StatCounter](https://gs.statcounter.com/os-market-share/mobile/south-africa)). That Compose Multiplatform for iOS is production-stable is [UNVERIFIED] ([JetBrains](https://kotlinlang.org/compose-multiplatform/)).

**Rec:** Build ontology metadata, Actions and domain services as a Spring Modulith modular monolith on Java 25 + Spring Boot 4. Prefer Leyden AOT caches to GraalVM native images (native only for CLIs). Build the Android-first field app in Kotlin, with KMP for shared domain and sync code.

### 1.4 Python — ML/AI, LLM orchestration, document AI, geo, entity resolution, user transforms

- **Free-threading.** Python 3.14's free-threaded build is officially supported (PEP 779). It costs about 5–10% single-thread performance, and C extensions that don't support it re-enable the GIL ([Red Hat, Sep 2026](https://developers.redhat.com/articles/2026/09/14/python-314-free-threaded-build-now-available-rhel)).
- **Tooling ownership.** On 19 Mar 2026 OpenAI announced it would acquire Astral (uv, Ruff, ty), promising continued open-source support. Closing was subject to regulatory approval ([OpenAI](https://openai.com/index/openai-to-acquire-astral/); [Willison](https://simonwillison.net/2026/mar/19/openai-acquiring-astral/)).
- **Ecosystem:** vLLM, Ray, Splink, Dagster, Docling and TiTiler are Python-first (§4).

**Rec:** Python for ML/LLM, document AI, entity resolution, geo analysis and user transforms (Polars/DuckDB/PySpark), with FastAPI for model services. Standardise on uv but mirror its binaries and keep pip-compatible lockfiles. Don't rely on free-threading until key C extensions certify it.

### 1.5 TypeScript — web cockpit, visualisation, generated SDKs

- **TypeScript 7.0** (3 Aug 2026) is the native Go compiler. Its stable programmatic API only arrives in 7.1, so some tooling (typescript-eslint, framework plugins) still needs `tsc6` ([InfoQ](https://www.infoq.com/news/2026/08/typescript-7-released/)).
- **Palantir's OSDK** generates TypeScript, Python and Java SDKs from the ontology. Each app gets "just the subset of the Ontology relevant to you", with scoped tokens ([Palantir](https://www.palantir.com/docs/foundry/ontology-sdk/overview); [osdk-ts](https://github.com/palantir/osdk-ts)).
- **Visualisation.** CesiumJS is Apache-2.0 and supports 3D Tiles ([repo](https://github.com/CesiumGS/cesium)). PMTiles is BSD-3 ([repo](https://github.com/protomaps/PMTiles)). deck.gl (MIT), MapLibre GL JS (BSD-3), Sigma.js, Cytoscape.js and G6 (MIT) are [UNVERIFIED] this session.

**Rec:** Next.js/React cockpit on TypeScript 7; a generated Vigil OSDK (TS/Python/Java/Kotlin) from ontology metadata; Sigma.js (WebGL) for large link graphs, Cytoscape.js for analyst layouts.

### 1.6 Elixir/Erlang (BEAM) — real-time collaboration, presence, alerting, messaging gateways

- **Scale evidence.** Discord scaled Elixir to 5 million concurrent users ([Discord](https://discord.com/blog/how-discord-scaled-elixir-to-5-000-000-concurrent-users)). WhatsApp's Erlang stack grew to nearly 500 million users on about 11,000 cores ([HighScalability](https://highscalability.com/how-whatsapp-grew-to-nearly-500-million-users-11000-cores-an/)).
- **Language maturity.** Elixir 1.19 (Oct 2025) and 1.20 (Jun 2026) gradually type-check every program ([Elixir](https://elixir-lang.org/blog/)).
- **Ecosystem example.** ElectricSQL's Postgres sync engine is written in Elixir ([repo](https://github.com/electric-sql/electric)).

**Rec:** Adopt Elixir in bounded form: one team owning one service (Phoenix Channels/Presence, alert escalation, WhatsApp/SMS/USSD adapters) that talks only Protobuf over NATS. Start around months 9–12. Until then, a Go gateway is acceptable.

### 1.7 C/C++/CUDA — consumed, not authored

These are all C/C++ libraries Vigil consumes:
- GDAL and PROJ [UNVERIFIED licences].
- H3: Apache-2.0 ([repo](https://github.com/uber/h3)).
- ClickHouse: Apache-2.0 ([repo](https://github.com/ClickHouse/ClickHouse)).
- OR-Tools: C++ with Python/Java/C# wrappers ([repo](https://github.com/google/or-tools)).
- OSRM: BSD-2 ([repo](https://github.com/Project-OSRM/osrm-backend)).
- Valhalla: MIT ([repo](https://github.com/valhalla/valhalla)).
- llama.cpp: MIT ([repo](https://github.com/ggml-org/llama.cpp)).
- TensorRT-LLM: Apache-2.0, runs on NVIDIA GPUs including Jetson ([repo](https://github.com/NVIDIA/TensorRT-LLM)).

**Rec:** Reach these only through bindings (Rust FFI, Python wheels, Java Panama). Build edge video analytics as TensorRT/ONNX pipelines driven by the Rust agent.

### 1.8 Verdicts on other languages

| Language | 2025–26 facts | Verdict |
|---|---|---|
| Julia | 1.12 added experimental `juliac --trim` ([Julia](https://julialang.org/blog/2025/10/julia-1.12-highlights/index.html)); 1.13 released Sep 2026 ([AlternativeTo](https://alternativeto.net/news/2026/9/julia-1-13-released-with-major-speed-gains-in-precompilation-startup-and-garbage-collection/)) | **No** — use OR-Tools/HiGHS from Python or Java |
| Zig | 0.16 introduces `std.Io`; 1.0 is still unscheduled ([Zig](https://ziglang.org/download/0.16.0/release-notes.html)) | **No** — revisit after 1.0 |
| Mojo | 1.0 released 12 Aug 2026 ([Register](https://www.theregister.com/ai-and-ml/2026/08/12/modulars-mojo-programming-language-hits-10-milestone/5286545)); compiler open-sourced 19 Aug 2026 (Apache-2.0 + LLVM exceptions), but it isn't accepting compiler contributions yet ([Linuxiac](https://linuxiac.com/mojo-programming-language-goes-fully-open-source/)) | **Watch** — no production use before 2027 |
| Scala | Flink 2.0 removed its Scala DataStream API ([Flink](https://flink.apache.org/2025/03/24/apache-flink-2.0.0-a-new-era-of-real-time-data-processing/)) | **No** new Scala code |
| C#/.NET | .NET 10 LTS, Nov 2025 ([Microsoft](https://devblogs.microsoft.com/dotnet/announcing-dotnet-10/)) | **No** — it duplicates the JVM's role |
| Dart/Flutter | Popular with African mobile developers [UNVERIFIED] | **Conditional** — only if one small team must ship iOS at parity; otherwise Kotlin |
| Gleam | v1.18, Jul 2026 ([Gleam](https://gleam.run/news/)) | **No** — Elixir already covers the BEAM |

### 1.9 The polyglot tax and guardrails

**Evidence:**
- **Uber:** engineers "needed to contribute to microservices written in Python, Node.JS, Java and Go", and Uber standardised on Java and Go ([Pragmatic Engineer](https://newsletter.pragmaticengineer.com/p/consolidating-technologies)).
- **Meta** formally supports Hack, C++, Rust and Python. Teams that choose anything else carry the maintenance burden themselves (same source).
- **Google** supports C++, Java/Kotlin, Python, Go and TypeScript/JavaScript, each with its own style guide and readability process ([Pragmatic Engineer](https://newsletter.pragmaticengineer.com/p/google-part-2)).
- **Spotify** introduced Golden Paths to end "rumour-driven development" ([Spotify](https://engineering.atspotify.com/2020/08/how-we-use-golden-paths-to-solve-fragmentation-in-our-software-ecosystem)).
- **Airbnb** documented its monolith-to-SOA "great migration" ([InfoQ](https://www.infoq.com/news/2019/02/airbnb-monolith-migration-soa/)).

**Rec — guardrails:**
1. **Tiered language registry:** Primary (Java, TypeScript, Python), Specialist (Rust, Go), Bounded (Elixir; Kotlin for mobile). Anything new needs an ADR and CTO sign-off.
2. **Golden-path templates** per language (service, library, job) with OTel, the policy-core binding, health, config and SBOM built in.
3. **At most two languages per team.**
4. **Cross languages only through contracts** (Protobuf, Arrow, CloudEvents); never share an operational database across a language boundary.
5. **Solve N×M once:** authorisation, telemetry and SDK generation are built once (Rust core, OTel, codegen) and exposed to every language.

---

## 2. Interop and contracts

| Concern | Choice | Evidence |
|---|---|---|
| Service RPC | **Protobuf + gRPC**, with **Connect** for browser and mobile clients | Buf checks for breaking changes at four levels (FILE, PACKAGE, WIRE_JSON, WIRE), locally, in CI and in the Buf Schema Registry ([Buf](https://buf.build/docs/breaking/)). Connect speaks gRPC, gRPC-Web and its own HTTP protocol; Go, TypeScript and Swift are stable, Kotlin and Python beta; it is a CNCF sandbox project ([Connect](https://connectrpc.com/docs/introduction/)) |
| Public REST | **OpenAPI 3.1**, moving to 3.2 | OpenAPI 3.2.0 was published 19 Sep 2025, adding streaming media types and the QUERY method ([OAI](https://spec.openapis.org/oas/v3.2.0.html)) |
| Events | **CloudEvents** envelope, documented with **AsyncAPI 3** | CloudEvents graduated from CNCF in Jan 2024, with Kafka, MQTT, AMQP and NATS bindings ([spec](https://github.com/cloudevents/spec)). AsyncAPI 3 adds request/reply; 3.1 is current ([AsyncAPI](https://www.asyncapi.com/docs/reference/specification/v3.0.0)) |
| Columnar exchange | **Arrow IPC/Flight** and **ADBC** | ADBC offers idiomatic APIs in C/C++, C#, Go, Java, JavaScript, Python, R, Ruby and Rust ([ADBC](https://arrow.apache.org/adbc/current/index.html)) |
| Query plans | **Substrait** — watch | "Format for describing compute operations on structured data" ([Substrait](https://substrait.io/)) |
| Alternatives | **Not adopted:** Conjure, Smithy | Conjure is Apache-2.0 and targets Java, TypeScript, Python, Rust and Go, but has a small community ([repo](https://github.com/palantir/conjure)). Smithy is maintained by AWS and is protocol-agnostic ([Smithy](https://smithy.io/2.0/index.html)) |

**WASM facts:**
- WASI 0.3.0 shipped on 11 Jun 2026 with native async (`stream<T>`, `future<T>`), followed by 0.3.1 in August. WASI 1.0 has no date ([WASI](https://wasi.dev/roadmap)).
- Wasmtime ships a major release monthly, with an LTS every 12 releases supported for 24 months ([Wasmtime](https://docs.wasmtime.dev/stability-release.html)).
- Extism (BSD-3) provides host SDKs for Rust, Go, Java, Python, Elixir and others, but it uses core WASM modules, not the Component Model ([Extism](https://github.com/extism/extism)).

**Rec:** Component Model on a **Wasmtime LTS** (with fuel/epoch limits) for sandboxed UDFs, validators, customer plugins and the policy core; Extism only for prototypes; heavy Python/ML UDFs in sandboxed containers. Keep protos in the monorepo and run `buf breaking` against `main`, so the build never needs the SaaS registry (air-gap-safe). Generate Vigil's OSDK-equivalent from ontology-scoped Protobuf/OpenAPI descriptions.

---

## 3. Monorepo and build

**Facts:**
- **Bazel 9.0 LTS** (20 Jan 2026) "completely removed" WORKSPACE, so Bzlmod is mandatory, and C++ rules moved to rules_cc ([Bazel](https://blog.bazel.build/2026/01/20/bazel-9.html)).
- **Buck2** is hermetic by default, with Starlark rules for C++, Python, Rust, Java, Kotlin, Go and Erlang ([Buck2](https://buck2.build/)).
- **Pants 2.33** covers Python, Go, Java, Scala, Kotlin, Shell and Docker, but not JavaScript ([Pants](https://www.pantsbuild.org/)).
- **Nx** supports Gradle, Maven and .NET, with remote caching via Nx Cloud ([Nx](https://nx.dev/docs/technologies)).
- **moon** (written in Rust, v2.5) supports remote caching ([moon](https://moonrepo.dev/moon)).

**Rec:** For months 0–18, one monorepo on native toolchains (Gradle, pnpm, uv, Cargo, Go modules), with `just` as the single command surface, Buf for protos and each ecosystem's own cache. Lay it out for Bazel (one package per directory, pinned toolchains, no network in tests). Move to Bazel 9 when CI exceeds 20 minutes, the team passes ~40 engineers, or cross-language codegen hurts. Skip Buck2 (thin external community) and Pants (no JavaScript).

---

## 4. Building blocks by layer

Unless a row says otherwise, every candidate below is self-hostable and air-gap-capable. The verdict column is my recommendation.

### a. Object storage

| Candidate | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| MinIO CE | AGPLv3 · Go | Repo archived 25 Apr 2026 ("no longer maintained"), source-only; users are steered to the commercial AIStor ([repo](https://github.com/minio/minio)) | **Exit** |
| SeaweedFS | Apache-2.0 · Go | S3 API, rack-aware 10.4 erasure coding, cloud tiering, built-in Iceberg REST catalog ([repo](https://github.com/seaweedfs/seaweedfs)) | **Default** in the appliance |
| Ceph RGW | LGPL · C++ [UNVERIFIED] | The large-scale incumbent [UNVERIFIED] | Petabyte-scale national DCs |
| Garage | AGPL · Rust [UNVERIFIED] | Single binary, 1 GB RAM minimum, 3-zone geo-replication ([site](https://garagehq.deuxfleurs.fr/)) | Small multi-site deployments |
| RustFS | Apache-2.0 · Rust | 1.0.0-beta.11; distributed mode is "under testing" ([repo](https://github.com/rustfs/rustfs)) | Watch |

**Rec:** Treat the S3 API as the contract, and use native S3 in the cloud.

### b. Lakehouse tables and catalogs

| Candidate | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| Iceberg v3 | Apache-2.0 · Java | v3 is "complete and adopted": deletion vectors, row lineage, variant, geometry/geography, default values, ns timestamps, table encryption keys. v4 is in development ([spec](https://raw.githubusercontent.com/apache/iceberg/main/format/spec.md)); latest release 1.11 ([releases](https://iceberg.apache.org/releases/)) | **Table format** |
| Apache Polaris | Apache-2.0 · Java | ASF top-level project; Iceberg REST API; federates Hive/Hadoop/BigQuery catalogs; OPA/Ranger authorisation ([repo](https://github.com/apache/polaris)) | **Catalog** |
| Lakekeeper | Apache-2.0 · Rust | OpenFGA authorisation, multi-tenancy, CloudEvents change events; single vendor (Vakamo) ([repo](https://github.com/lakekeeper/lakekeeper)) | Lightweight edge catalog |
| Nessie · lakeFS | Apache-2.0 · Java · Go | Nessie: git-like catalog semantics ([repo](https://github.com/projectnessie/nessie)). lakeFS: branches whole object stores, with write-audit-publish hooks ([repo](https://github.com/treeverse/lakeFS)) | Only for atomic multi-table merges |
| Unity Catalog OSS | Apache-2.0 · Scala/Java | LF AI & Data sandbox project ([repo](https://github.com/unitycatalog/unitycatalog)) | No |

Delta Lake and Hudi were not re-verified [UNVERIFIED].

**Rec:** Implement Foundry-style branching as Iceberg branches and tags, tied together by Vigil's own branch metadata across datasets, transforms and ontology edits.

### c. OLTP and multi-model Postgres; distributed SQL

- **PostgreSQL 18** (25 Sep 2025) added async I/O (up to 3× faster reads), `uuidv7()`, OAuth and skip scan ([PG](https://www.postgresql.org/about/news/postgresql-18-released-3142/)).
- **PostgreSQL 19** reached Beta 4 on 24 Sep 2026 ([PG](https://www.postgresql.org/)). It adds **SQL/PGQ property-graph queries**, `REPACK`, `FOR PORTION OF` and sequence replication ([notes](https://www.postgresql.org/docs/19/release-19.html)).

| Extension / system | Licence · lang | Status | Verdict |
|---|---|---|---|
| pgvector | OSS · C | 0.8.6; HNSW and IVFFlat indexes, iterative scans ([repo](https://github.com/pgvector/pgvector)) | **Yes** |
| pgvectorscale | PostgreSQL · Rust | StreamingDiskANN index, SBQ quantisation, label filtering ([repo](https://github.com/timescale/pgvectorscale)) | **Yes** |
| pg_duckdb | MIT | 1.1.1; reads Parquet, Iceberg and Delta; PG 14–18 ([repo](https://github.com/duckdb/pg_duckdb)) | **Yes** |
| Apache AGE | Apache-2.0 | 1.8.0; PG 11–18 ([repo](https://github.com/apache/age)) | Superseded by PG 19 SQL/PGQ |
| TimescaleDB | Apache-2.0 core + TSL | The company is now Tiger Data ([repo](https://github.com/timescale/timescaledb)) | Avoid TSL features |
| Citus · ParadeDB | AGPL-3.0 | ([repo](https://github.com/citusdata/citus)) · ([repo](https://github.com/paradedb/paradedb)) | Amber |
| YugabyteDB | Apache-2.0 core | v2025.2; PostgreSQL 15 fork ([repo](https://github.com/yugabyte/yugabyte-db)) | Only for multi-region active-active |
| CockroachDB | Proprietary | Core retired in 24.3; free only under $10M revenue ([Cockroach](https://www.cockroachlabs.com/blog/enterprise-license-announcement/)) | **No** |
| TiDB | Apache-2.0 · Go | MySQL-compatible ([repo](https://github.com/pingcap/tidb)) | No (wrong dialect) |

**Rec:** **PostgreSQL 18 now, moving to 19 at GA**, with PostGIS (licence [UNVERIFIED]), pgvector/pgvectorscale and pg_duckdb.

### d. OLAP

| Candidate | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| ClickHouse | Apache-2.0 · C++ | v26.7, Jul 2026 ([repo](https://github.com/ClickHouse/ClickHouse)) | **Hot analytics, telemetry, logs** |
| StarRocks | Apache-2.0 · Java/C++ | Linux Foundation project; 3.5.19; shared-data mode; reads Iceberg/Delta/Hudi ([repo](https://github.com/StarRocks/starrocks)) | **Federated lake SQL** |
| DuckDB | MIT [UNVERIFIED] · C++ | 1.4 LTS (Sep 2025), 1.5 (Mar 2026); 2.0 due 21 Oct 2026; every other release is LTS ([DuckDB](https://duckdb.org/release_calendar)) | **Embedded everywhere** |
| Trino · Doris · Pinot · Druid | [UNVERIFIED] | Not re-verified | Trino is an acceptable alternative to StarRocks |

### e. Streaming, messaging and stream processing

| Candidate | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| Kafka 4.x | Apache-2.0 · Java | 4.0 (18 Mar 2025): ZooKeeper-free (KRaft), new rebalance protocol (KIP-848) GA, queues (KIP-932) in early access, brokers need Java 17 ([Kafka](https://kafka.apache.org/blog/2025/03/18/apache-kafka-4.0.0-release-announcement/)); 4.3 shipped 22 May 2026 ([blog](https://kafka.apache.org/blog)) | **Core durable log** |
| NATS JetStream | Apache-2.0 · Go | Synadia–CNCF agreement of 1 May 2025: trademarks go to the Linux Foundation, repos and domain stay with CNCF ([CNCF](https://www.cncf.io/announcements/2025/05/01/cncf-and-synadia-align-on-securing-the-future-of-the-nats-io-project/); [repo](https://github.com/nats-io/nats-server)) | **Edge and control bus** |
| Redpanda | BSL 1.1 · C++ | No commercial streaming service allowed; converts to Apache-2.0 after 4 years ([BSL](https://raw.githubusercontent.com/redpanda-data/redpanda/dev/licenses/bsl.md)) | **No** |
| Flink 2.x | Apache-2.0 · Java | 2.0: ForSt disaggregated state, materialised tables, DataSet/Scala APIs removed ([Flink](https://flink.apache.org/2025/03/24/apache-flink-2.0.0-a-new-era-of-real-time-data-processing/)); 2.3 in Jun 2026 ([posts](https://flink.apache.org/posts/)) | **Heavy stateful processing** |
| RisingWave | Apache-2.0 · Rust | Postgres wire protocol, native Iceberg, state in S3 ([repo](https://github.com/risingwavelabs/risingwave)) | **SQL materialised views** |
| Arroyo | MIT/Apache-2.0 · Rust | Acquired by Cloudflare 10 Apr 2025 ([Cloudflare](https://blog.cloudflare.com/cloudflare-acquires-arroyo-pipelines-streaming-ingestion-beta/)); repo still dual-licensed ([repo](https://github.com/ArroyoSystems/arroyo)) | Watch |
| Materialize | BSL 1.1 | Free Community Edition limited to <24 GiB memory ([repo](https://github.com/MaterializeInc/materialize)) | **No** |

Pulsar and RabbitMQ Streams were not re-verified [UNVERIFIED].

### f. Batch and federated compute

| Candidate | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| Spark 4.x | Apache-2.0 · Scala | 4.0 (May 2025), 4.1 (Dec 2025), 4.2 (Jul 2026) ([Spark](https://spark.apache.org/news/index.html)) | **Large jobs** |
| DataFusion + Comet | Apache-2.0 · Rust | Comet gives ~2× on TPC-DS SF1000 for Spark 3.4–4.1 with no code changes ([repo](https://github.com/apache/datafusion-comet)) | **Embedded engine + Spark accelerator** |
| Polars · DuckDB | [UNVERIFIED] · Rust · C++ | Fast single-node engines | **Default for user transforms** |
| Daft | Apache-2.0 · Rust | Multimodal data processing on Ray/K8s ([repo](https://github.com/Eventual-Inc/Daft)) | Watch |
| Ray | Apache-2.0 | A PyTorch Foundation project, alongside vLLM ([PyTorch](https://pytorch.org/foundation/)) | **ML and batch inference** |

### g. Graph — can Vigil avoid a dedicated graph database?

| Candidate | Licence | 2026 status | Verdict |
|---|---|---|---|
| Neo4j | GPLv3 CE / commercial EE | ([Neo4j](https://neo4j.com/licensing/)) | Amber/red |
| Memgraph · SurrealDB | BSL | ([repo](https://github.com/memgraph/memgraph)) · ([repo](https://github.com/surrealdb/surrealdb)) | **No** |
| FalkorDB | SSPLv1 | ([repo](https://github.com/FalkorDB/FalkorDB)) | **No** |
| TypeDB 3 | MPL-2.0 · Rust | Rust rewrite ([repo](https://github.com/typedb/typedb)) | Niche |
| JanusGraph | Apache-2.0 | LF AI & Data; last cited release 1.1.0 (2024) ([repo](https://github.com/JanusGraph/janusgraph)) | Stagnant |
| Kùzu | MIT | **Archived 10 Oct 2025** ([repo](https://github.com/kuzudb/kuzu)) | Gone |
| PuppyGraph | Proprietary | Runs openCypher/Gremlin over Iceberg, Delta and Postgres; free single-node edition ([site](https://www.puppygraph.com/)) | Optional add-on |
| DuckPGQ | MIT | Research work-in-progress ([repo](https://github.com/cwida/duckpgq-extension)) | Watch |
| **PostgreSQL 19 SQL/PGQ** | PostgreSQL | In PG 19 ([notes](https://www.postgresql.org/docs/19/release-19.html)) | **Use** |

NebulaGraph and the ISO GQL standard (ISO/IEC 39075) are [UNVERIFIED].

**Rec — yes, avoid a dedicated graph database.** The ontology already is a typed property graph:
- Store links relationally.
- Query patterns with PG 19's `GRAPH_TABLE`.
- Run multi-hop link analysis in a Rust in-memory traversal service over Arrow.
- Expose a GQL/openCypher-style API so an engine can be swapped in later.

### h. Search and vector

| Candidate | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| OpenSearch | Apache-2.0 · Java | Trademark held by LF Projects ([repo](https://github.com/opensearch-project/OpenSearch)) | **Object/document search** |
| Elasticsearch | AGPL / ELv2 / SSPL | AGPL option added 29 Aug 2024 ([Elastic](https://www.elastic.co/blog/elasticsearch-is-open-source-again)) | Amber |
| Tantivy / Quickwit | Apache-2.0 · Rust | Quickwit joined Datadog in Jan 2025 and relicensed to Apache-2.0 ([Quickwit](https://quickwit.io/blog/quickwit-joins-datadog); [repo](https://github.com/quickwit-oss/quickwit)) | **Tantivy embedded at the edge** |
| Meilisearch · Vespa | MIT CE + BUSL EE · Apache-2.0 | Meilisearch's sharding is Enterprise-only ([repo](https://github.com/meilisearch/meilisearch)); Vespa ([repo](https://github.com/vespa-engine/vespa)) | No |
| pgvector (+scale) | see §c | — | **Vector default** |
| Qdrant | Apache-2.0 · Rust | Quantisation, GPU indexing, in-process **Qdrant Edge** ([repo](https://github.com/qdrant/qdrant)) | **>~100M vectors, or edge** |
| Milvus · LanceDB · Weaviate | Apache-2.0 · Apache-2.0 · BSD-3 | Milvus is under LF AI & Data ([repo](https://github.com/milvus-io/milvus)); LanceDB is embedded ([repo](https://github.com/lancedb/lancedb)); Weaviate ([repo](https://github.com/weaviate/weaviate)) | **LanceDB** for lake-resident multimodal embeddings |

### i. Time-series and OT/SCADA telemetry

| Candidate | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| ClickHouse | Apache-2.0 | see §d | **Central telemetry store** |
| QuestDB | Apache-2.0 · Java/C++/Rust | `SAMPLE BY`, `ASOF JOIN`, InfluxDB line-protocol ingest ([repo](https://github.com/questdb/questdb)) | **Edge/site time-series** |
| InfluxDB 3 Core | MIT/Apache-2.0 · Rust | Single-node; historical queries and HA are Enterprise-only ([docs](https://docs.influxdata.com/influxdb3/core/)) | No |
| VictoriaMetrics | Apache-2.0 · Go | Single-node and cluster ([repo](https://github.com/VictoriaMetrics/VictoriaMetrics)) | **Platform metrics** |
| Eclipse Milo | EPL-2.0 · Java | OPC UA 1.05 ([repo](https://github.com/eclipse-milo/milo)) | JVM-side OPC UA |
| open62541 | MPL-2.0 · C99 | A v1.4 server was OPC Foundation-certified ([repo](https://github.com/open62541/open62541)) | **OPC UA via Rust FFI** |
| Apache PLC4X | Apache-2.0 | "Universal protocol adapter" for IIoT ([site](https://plc4x.apache.org/)) | Multi-protocol PLC access |
| Sparkplug 3.0 | Eclipse spec | Birth/death certificates on top of MQTT ([Eclipse](https://sparkplug.eclipse.org/)) | **Use** |

DNP3 and IEC 61850 library licences are [UNVERIFIED]; dual GPL/commercial licensing is common.

**Rec:** The Rust agent speaks OPC UA (via open62541), Modbus and MQTT/Sparkplug B, and publishes to NATS. Budget for a commercial DNP3/IEC 61850 stack for utility customers.

### j. Durable execution (write-back sagas) vs data pipelines

| Candidate | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| Temporal | MIT · Go | Latest server release v1.31.2 ([repo](https://github.com/temporalio/temporal)) | **Actions/write-back sagas** (compensation, approvals, retries into flaky ERPs) |
| Restate | BSL 1.1 · Rust | No managed service for third parties ([LICENSE](https://raw.githubusercontent.com/restatedev/restate/main/LICENSE)) | **No** |
| DBOS Transact | MIT | Durable workflows on Postgres ([repo](https://github.com/dbos-inc/dbos-transact-py)) | Small edge workflows |
| Dagster | Apache-2.0 · Python | Asset-centric ([repo](https://github.com/dagster-io/dagster)) | **Data pipelines** |
| Airflow 3 | Apache-2.0 | 22 Apr 2025: Task SDK, DAG versioning, event-driven assets, Edge Executor ([Airflow](https://airflow.apache.org/blog/airflow-three-point-oh-is-here/)) | Only where customers mandate it |
| Kestra | Apache-2.0 · Java | YAML workflows ([repo](https://github.com/kestra-io/kestra)) | No |

### k. Identity and authorization — implementing Palantir-style controls

| Candidate | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| Keycloak | Apache-2.0 · Java | CNCF incubating since Apr 2023 ([CNCF](https://www.cncf.io/projects/keycloak/)) | **Identity** (federates AD/LDAP/SAML, runs offline) |
| Zitadel · Ory | AGPL-3.0 · Go / Apache-2.0 · Go | ([repo](https://github.com/zitadel/zitadel)) · ([repo](https://github.com/ory/kratos)) | No / alternative |
| OpenFGA | Apache-2.0 · Go | CNCF incubating since 28 Oct 2025 ([CNCF](https://www.cncf.io/projects/openfga/)) | **ReBAC + conditions** |
| SpiceDB | Apache-2.0 · Go | Caveats combine ABAC and ReBAC ([repo](https://github.com/authzed/spicedb)) | Alternative |
| Cedar | Apache-2.0 · Rust | CNCF sandbox since 8 Oct 2025; symbolic verification ([CNCF](https://www.cncf.io/projects/cedar/); [repo](https://github.com/cedar-policy/cedar)) | For customer-authored policies |
| OPA | Apache-2.0 · Go | Creators joined Apple in Aug 2025; still "CNCF graduated", no governance or licence change ([OPA](https://www.openpolicyagent.org/blog)) | Cluster policy |
| Topaz | Apache-2.0 | OPA plus a Zanzibar-style directory ([repo](https://github.com/aserto-dev/topaz)) | No |

**Rec — design:**
1. **Mandatory markings.** Every dataset, column, row group, object and link carries a set of markings (classification, compartments, releasability, PII, customer-defined). A user must hold *all* of them (deny wins). Owners can't grant markings; only marking administrators can. The logic lives in a small **Rust policy core**, compiled to native code and to a WASM component, so every runtime enforces identical semantics.
2. **Relationship and discretionary access** (projects, organisations, roles, sharing) goes in **OpenFGA** on Postgres.
3. **Purpose-based access.** Tokens carry a declared purpose or case ID, OpenFGA conditions check it, and every read is logged with its purpose.
4. **Row, column and object security.** The enforcement point compiles decisions into:
   - Postgres row-level security.
   - Engine row filters and column masks.
   - OpenSearch document/field-level security [UNVERIFIED].
   - Scoped storage credentials vended by the catalog.
5. **Propagation through lineage.**
   - Outputs inherit the union of their inputs' markings, recorded as OpenLineage facets.
   - Removing a marking is an approved "declassify" Action, run as a Temporal workflow under a two-person rule.

### l. Lineage, catalog and data quality

| Candidate | Licence | 2026 status | Verdict |
|---|---|---|---|
| OpenLineage (+ Marquez) | Apache-2.0 | LF AI & Data graduate; Marquez is the reference implementation ([repo](https://github.com/OpenLineage/OpenLineage)) | **Event standard** |
| DataHub · OpenMetadata | Apache-2.0 | DataHub: column-level lineage, MCP server ([repo](https://github.com/datahub-project/datahub)). OpenMetadata: 130+ connectors ([repo](https://github.com/open-metadata/OpenMetadata)) | Export targets |
| GX Core · Soda Core v4 | Apache-2.0 · [UNVERIFIED] | Soda defines checks as YAML data contracts ([repo](https://github.com/great-expectations/great_expectations); [repo](https://github.com/sodadata/soda-core)) | **Data checks** |
| dbt Fusion | Mostly ELv2 · Rust | Beta ([repo](https://github.com/dbt-labs/dbt-fusion)). After the Fivetran merger: "dbt Core and Fusion will both continue to be shipped under their current licenses" ([dbt](https://www.getdbt.com/blog/dbt-labs-and-fivetran-merge-announcement)) | Use dbt Core, not Fusion |

**Rec:** Vigil's own lineage graph must *enforce* marking propagation, so it can't be a bolt-on catalog.

### m. Deployment platform ("Apollo-like")

| Component | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| Zarf | Apache-2.0 · Go | Air-gap-native packages with SBOMs, a built-in registry and Gitea ([repo](https://github.com/zarf-dev/zarf)) | **Bundle format** |
| UDS Core | AGPL-3.0 | Bundles Istio, Keycloak, Pepr, Loki, Falco, Velero ([repo](https://github.com/defenseunicorns/uds-core)) | Borrow ideas; don't redistribute |
| RKE2 · k3s · Talos | Apache-2.0 · Apache-2.0 · MPL-2.0 | RKE2: FIPS 140-2, CIS ([docs](https://docs.rke2.io/)). k3s: <100 MB, SQLite default, ARM ([repo](https://github.com/k3s-io/k3s)). Talos: immutable, API-only ([repo](https://github.com/siderolabs/talos)) | **Sovereign DC · edge · appliance** |
| Flux · Argo CD | Apache-2.0 | Flux is CNCF graduated and pulls from OCI repositories ([repo](https://github.com/fluxcd/flux2)); Argo CD ([repo](https://github.com/argoproj/argo-cd)) | **Flux** (pull-based) |
| Harbor, in-toto, Crossplane, KubeEdge | Apache-2.0 | All CNCF graduated ([CNCF](https://www.cncf.io/projects/)) | Use |
| OpenTofu | MPL-2.0 | CNCF sandbox since 23 Apr 2025 ([CNCF](https://www.cncf.io/projects/opentofu/)) | **Infrastructure-as-code** |
| SLSA · Trivy | — · Apache-2.0 | SLSA 1.2 is current ([SLSA](https://slsa.dev/spec/v1.1/)); Trivy is at v0.72 ([repo](https://github.com/aquasecurity/trivy)) | Use; cosign/Syft [UNVERIFIED] |

**Cautionary tale — Bitnami.** Versioned Bitnami images moved to an unmaintained `bitnamilegacy` repository, and deletion of the public catalog was postponed to 29 Sep 2025 ([issue](https://github.com/bitnami/containers/issues/83267)).

**Rec:** A Go controller builds signed Zarf bundles and delivers them to a Harbor registry on each site. Flux pulls from there (tolerating intermittent links), and Kyverno verifies signatures at admission. Build your own base images.

### n. Edge and offline-first

| Candidate | Licence · lang | 2026 status | Verdict |
|---|---|---|---|
| KubeEdge | Apache-2.0 | CNCF graduated; edge autonomy while disconnected; v1.23 ([site](https://kubeedge.io/)) | For large node fleets |
| SQLite · Turso DB | public domain [UNVERIFIED] · MIT, Rust | Turso DB is pre-1.0 and "replaces libSQL" as the company's direction ([repo](https://github.com/tursodatabase/turso)) | **SQLite**; watch Turso |
| Automerge 3 · Yjs · Loro | MIT | Automerge 3 cut memory ~10× ([repo](https://github.com/automerge/automerge)); Yjs is used by JupyterLab and Linear ([repo](https://github.com/yjs/yjs)); Loro reached 1.0 (Rust) ([repo](https://github.com/loro-dev/loro)) | **Yjs** (web) / **Loro** (native), for collaborative notes only |
| ElectricSQL | Apache-2.0 · Elixir | 1.0 (Mar 2025); read-path Postgres sync ([repo](https://github.com/electric-sql/electric)) | **Read sync** |
| PowerSync · Zero | FSL-1.1-ALv2 (non-compete) · Apache-2.0 | ([LICENSE](https://raw.githubusercontent.com/powersync-ja/powersync-service/main/LICENSE)) · ([repo](https://github.com/rocicorp/mono)) | Red · watch |
| Mosquitto · NanoMQ | EPL-2.0/EDL · MIT, C | NanoMQ supports QUIC bridging ([repo](https://github.com/eclipse-mosquitto/mosquitto); [repo](https://github.com/nanomq/nanomq)) | **Edge MQTT brokers** |
| EMQX | BSL 1.1 · Erlang | BSL since v5.9; clusters need a licence file ([repo](https://github.com/emqx/emqx)) | **No** |
| ChirpStack | MIT · Rust | LoRaWAN network server ([repo](https://github.com/chirpstack/chirpstack)) | **Use** |
| WhatsApp · RapidPro | Meta Cloud API · BSL 1.1 | The On-Premises API expired 23 Oct 2025 ([Meta](https://developers.facebook.com/docs/whatsapp/on-premises/sunset)); RapidPro converts from BSL to AGPL-3.0 on 2028-07-22 ([LICENSE](https://raw.githubusercontent.com/nyaruka/rapidpro/main/LICENSE)) | WhatsApp can't be air-gapped; avoid RapidPro |

**Rec:**
- **Edge node:** k3s plus the Rust agent (disk-backed store-and-forward to a NATS leaf), SQLite, DuckDB and a Parquet spool.
- **Field app:** Electric-style read sync; writes are queued as Vigil Actions, so write-back stays server-side.
- **Sovereign messaging:** SMPP SMS and USSD via local aggregators (Africa's Talking docs were unreadable [UNVERIFIED]).

### o. Geospatial

| Area | Facts | Pick |
|---|---|---|
| Formats and engines | GeoParquet 2.0.0-rc.1, an OGC incubating standard ([site](https://geoparquet.org/)); SedonaDB (Rust/DataFusion, Apache-2.0) propagates CRS and does KNN joins ([repo](https://github.com/apache/sedona-db)); H3 Apache-2.0 ([repo](https://github.com/uber/h3)); Plus Codes Apache-2.0, built for places without street addresses ([repo](https://github.com/google/open-location-code)) | **PostGIS + H3**, GeoParquet/Iceberg geo types, SedonaDB/DuckDB |
| Maps and 3D | PMTiles (BSD-3) serves tiles by HTTP range requests straight from S3 ([repo](https://github.com/protomaps/PMTiles)); CesiumJS can use self-hosted terrain without ion ([repo](https://github.com/CesiumGS/cesium)) | **MapLibre + deck.gl + Protomaps** offline; Cesium for 3D |
| Open data | Overture Places is CDLA-Permissive-2.0; Base, Buildings, Divisions and Transportation are ODbL ([Overture](https://docs.overturemaps.org/attribution/)). OSMF: output "intended for the extraction of the original data" is a database, so share-alike applies ([OSMF](https://osmfoundation.org/wiki/Licence/Community_Guidelines/Produced_Work_-_Guideline)) | Keep ODbL layers in separate databases |
| Earth observation | TiTiler (MIT) serves COG/STAC/Zarr ([repo](https://github.com/developmentseed/titiler)); STAC ([STAC](https://stacspec.org/en)); Digital Earth Africa provides free Landsat/Sentinel ARD plus water and cropland products ([DEA](https://www.digitalearthafrica.org/)) | **TiTiler + STAC over DE Africa** |
| Routing | Valhalla: MIT, multimodal, isochrones, offline tiles ([repo](https://github.com/valhalla/valhalla)); OSRM: BSD-2 ([repo](https://github.com/Project-OSRM/osrm-backend)); GraphHopper: Apache-2.0 ([repo](https://github.com/graphhopper/graphhopper)) | **Valhalla** at the edge, OSRM for matrices |

### p. AI/LLM stack

| Area | Facts | Pick |
|---|---|---|
| Serving | vLLM (Apache-2.0; NVIDIA/AMD/Intel/CPU, TPU/Ascend plugins) ([repo](https://github.com/vllm-project/vllm)); SGLang (Apache-2.0; "over 400,000 GPUs") ([repo](https://github.com/sgl-project/sglang)); llama.cpp (MIT; CUDA/Metal/Vulkan/HIP/CPU) ([repo](https://github.com/ggml-org/llama.cpp)); NVIDIA Dynamo (Apache-2.0) adds disaggregated serving and KV-aware routing ([repo](https://github.com/ai-dynamo/dynamo)) | **vLLM** core, **llama.cpp** edge |
| Open weights (Sep 2026) | gpt-oss Apache-2.0; the 20b runs in 16 GB ([OpenAI](https://openai.com/index/introducing-gpt-oss/)). Qwen3.8 (Aug 2026) Apache-2.0 ([HF](https://huggingface.co/Qwen/Qwen3.8-27B)). DeepSeek-V4-Pro MIT, 1.7T ([HF](https://huggingface.co/deepseek-ai/DeepSeek-V4-Pro-0813)). Mistral 3 all Apache-2.0: Large 3 (675B MoE), Ministral 3/8/14B ([Mistral](https://mistral.ai/news/mistral-3)). Gemma 4 now Apache-2.0 ([Google](https://ai.google.dev/gemma/docs/core)). Llama 4: 700M-MAU clause and "Built with Llama" naming ([licence](https://dev.meta.ai/llama/llama4/license)); no Llama release since May 2025 ([HF](https://huggingface.co/meta-llama)) | **Apache/MIT models only** |
| Embeddings | Qwen3-Embedding (Apache-2.0, 100+ languages) ([HF](https://huggingface.co/Qwen/Qwen3-Embedding-8B)); BGE-M3 (MIT; dense + sparse + multi-vector) ([HF](https://huggingface.co/BAAI/bge-m3)) | Both |
| African languages and speech | Non-commercial: InkubaLM CC BY-NC ([HF](https://huggingface.co/lelapa/InkubaLM-0.4B)); NLLB-200 CC-BY-NC, "not released for production deployment" ([HF](https://huggingface.co/facebook/nllb-200-distilled-600M)); MMS CC-BY-NC, 1,162 languages ([HF](https://huggingface.co/facebook/mms-1b-all)). Usable: Omnilingual ASR, 1,600+ languages, code Apache-2.0, data CC-BY-4.0 ([repo](https://github.com/facebookresearch/omnilingual-asr)); AfroXLMR MIT, 17 languages ([HF](https://huggingface.co/Davlan/afro-xlmr-large)). Community: Masakhane ([site](https://www.masakhane.io/)). Gap: Parakeet v3 is European-only ([HF](https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3)) | **Omnilingual ASR, AfroXLMR**; NC models for research only |
| Document AI | Docling (MIT, LF AI & Data) ([repo](https://github.com/docling-project/docling)); PaddleOCR 3 (Apache-2.0) ([repo](https://github.com/PaddlePaddle/PaddleOCR)); olmOCR 2 (Apache-2.0, <$200 per million pages) ([repo](https://github.com/allenai/olmocr)); dots.ocr (MIT code, separate model licence) ([repo](https://github.com/rednote-hilab/dots.ocr)); Marker (Apache-2.0 code; weights free only under $5M revenue) ([repo](https://github.com/datalab-to/marker)) | **Docling + PaddleOCR**, olmOCR on GPUs |
| Agent tooling | MCP now sits in the LF Agentic AI Foundation (9 Dec 2025) ([LF](https://www.linuxfoundation.org/press/linux-foundation-announces-the-formation-of-the-agentic-ai-foundation)); LiteLLM MIT ([repo](https://github.com/BerriAI/litellm)); Langfuse part of ClickHouse since Jan 2026 ([repo](https://github.com/langfuse/langfuse)); promptfoo "now part of OpenAI", still MIT ([repo](https://github.com/promptfoo/promptfoo)); Inspect MIT ([repo](https://github.com/UKGovernmentBEIS/inspect_ai)); Llama Guard 4 under the Llama licence ([HF](https://huggingface.co/meta-llama/Llama-Guard-4-12B)); NeMo Guardrails Apache-2.0 ([repo](https://github.com/NVIDIA/NeMo-Guardrails)) | **MCP servers wrapping ontology Actions**; LiteLLM gateway; Langfuse; Inspect + promptfoo; NeMo Guardrails |

**Rec:** The gateway enforces markings, redacting anything above the caller's clearance.

### q. Entity resolution

| Candidate | Licence | 2026 status | Verdict |
|---|---|---|---|
| Splink | MIT | ~1M records in about a minute on a laptop, 100M+ on Spark; used for UK Census linkage ([repo](https://github.com/moj-analytical-services/splink)) | **Engine** |
| Zingg · dedupe | AGPL-3.0 · MIT | ([repo](https://github.com/zinggAI/zingg)) · ([repo](https://github.com/dedupeio/dedupe)) | No · small jobs |
| Senzing | Commercial | ~$58,560/yr for 10M records ([pricing](https://senzing.com/pricing/)) | Optional paid plug-in |
| JedAI | Apache-2.0 | Archived 26 May 2026 ([repo](https://github.com/scify/JedAIToolkit)) | No |
| libpostal | MIT · C | Trained on OSM and OpenAddresses ([repo](https://github.com/openvenues/libpostal)) | Plus Codes + H3 fallback |

No maintained open-source library for African-name matching was found [UNVERIFIED gap].

**Rec:** Build Splink comparators for name order, clan/patronymic forms, transliteration and per-language phonetics, with human review in the cockpit.

### r. Simulation and optimisation

| Candidate | Licence | Facts | Verdict |
|---|---|---|---|
| OR-Tools · HiGHS | Apache-2.0 · MIT | OR-Tools includes CP-SAT and PDLP ([repo](https://github.com/google/or-tools)); HiGHS solves LP/MIP/QP ([repo](https://github.com/ERGO-Code/HiGHS)) | **Default solvers** |
| Timefold CE | Apache-2.0 · Java | Forked from OptaPlanner in 2023 ([repo](https://github.com/TimefoldAI/timefold-solver)) | **JVM scheduling** |
| VROOM | BSD-2 · C++ | VRPTW/PDPTW over OSRM/Valhalla ([repo](https://github.com/VROOM-Project/vroom)) | **Fleets** |
| SimPy, Mesa, JuMP, Ditto, DTDL, BaSyx | [UNVERIFIED] | Not verified | SimPy/Mesa for what-ifs |

**Rec:** The ontology *is* the digital twin; import and export DTDL/AAS rather than running Ditto or BaSyx.

### s. Observability

VictoriaMetrics and ClickHouse are Apache-2.0 (above). Grafana, Loki, Tempo and Mimir are AGPL-3.0 [UNVERIFIED]. Prometheus 3 and SigNoz are [UNVERIFIED].

**Rec:** OpenTelemetry SDKs and Collector everywhere (the polyglot equaliser); VictoriaMetrics for metrics and logs, ClickHouse for traces; Grafana only unmodified, as a separate service (amber).

---

## 5. Open-source licence risk

| When | Event | Source |
|---|---|---|
| Aug 2023 | HashiCorp moved to BSL; the OpenTofu fork followed (MPL-2.0, CNCF sandbox Apr 2025) | [CNCF](https://www.cncf.io/projects/opentofu/) (HashiCorp dates [UNVERIFIED]) |
| 2024–25 | Redis moved to RSAL/SSPL; the Valkey fork followed; Redis 8 added AGPL | [UNVERIFIED] (pages unreadable) |
| Aug 2024 | Elastic added an AGPL option; CockroachDB retired Core | [Elastic](https://www.elastic.co/blog/elasticsearch-is-open-source-again), [Cockroach](https://www.cockroachlabs.com/blog/enterprise-license-announcement/) |
| 2025 | Quickwit joined Datadog (relicensed to Apache-2.0); Cloudflare acquired Arroyo; the NATS dispute was settled; EMQX (from v5.9) moved to BSL and Zitadel to AGPL (exact dates [UNVERIFIED]); Bitnami catalog changes; GraalVM detached from Java SE; Kùzu archived | sources in §1–4 |
| 2026 | Langfuse joined ClickHouse (Jan); Astral–OpenAI deal announced (Mar); MinIO archived (Apr); JedAI archived (May). Also recent, dates [UNVERIFIED]: promptfoo joined OpenAI; RapidPro moved to BSL | sources in §1–4 |
| Positive moves | Gemma 4, Quickwit and Marker code are now Apache-2.0 | sources above |

**Pattern:** single-vendor, VC-backed infrastructure relicenses under monetisation pressure. Foundation-governed projects (ASF, CNCF, LF, Eclipse, PyTorch Foundation) have held; even NATS's attempted exit was reversed.

**Rec — policy:**
- **Green:** Apache-2.0, MIT, BSD, ISC, PostgreSQL; MPL-2.0/EPL-2.0 unmodified or with patches upstreamed.
- **Amber** (architecture review; separate process only; never in the edge agent, SDKs or mobile app): LGPL, GPL, AGPL, CC-BY/OpenRAIL weights, community model licences.
- **Red** (banned unless a commercial licence is in place): BSL/BUSL, SSPL, ELv2, FSL, Commons Clause, CC-BY-NC/"research-only", revenue-threshold licences.
- **Governance scoring:** foundation-hosted > multi-vendor > single-vendor. Any single-vendor dependency needs a documented exit plan and an abstraction boundary (S3 API, Iceberg REST, Protobuf).
- **Controls:** SBOM and licence-scan gates in CI; mirror every artefact (air-gap requires it anyway); review the top 50 dependencies yearly.

---

## 6. Reference architecture

| Subsystem | Technology | Lang | Licence | Why |
|---|---|---|---|---|
| Edge/ingest agent | Custom agent (OPC UA via open62541, Modbus, MQTT/Sparkplug, store-and-forward) | Rust | Own + MPL-2.0 | Memory-safe static binary on ARM |
| Stream bus | NATS JetStream (edge/control) + Kafka 4.x (core) | Go / Java | Apache-2.0 | Foundation-governed; light edge plus a durable core |
| Lakehouse | Iceberg v3 on SeaweedFS/Ceph + Apache Polaris | Java/Go | Apache-2.0 | Open tables, multi-engine, ASF governance |
| Ontology metadata service | Spring Boot 4 + Spring Modulith on PostgreSQL 18→19 | Java 25 | Apache-2.0 / PostgreSQL | Team strength and transactional integrity |
| Object index/store | Postgres (hot objects) + Rust indexer (DataFusion over Iceberg) → OpenSearch | Rust/Java | Apache-2.0 | Low-latency reads over lake history |
| Graph traversal | PG 19 SQL/PGQ + Rust in-memory traversal | SQL/Rust | PostgreSQL / Apache-2.0 | No GPL, BSL or single-vendor graph engine |
| Search | OpenSearch | Java | Apache-2.0 | Linux Foundation governance |
| Vector | pgvector + pgvectorscale; Qdrant at scale or edge | C/Rust | PostgreSQL (pgvector's [UNVERIFIED]) / Apache-2.0 | Postgres first, with a Rust escape hatch |
| Time-series | ClickHouse (core), QuestDB or SQLite at edge, VictoriaMetrics | C++/Java/Go | Apache-2.0 | Permissive and fast |
| Actions/write-back | Java Action services + Temporal sagas + connectors | Java/Go | Apache-2.0 / MIT | Compensation, idempotency, approvals |
| Workflow/pipelines | Dagster (data) + Temporal (operations) | Python/Go | Apache-2.0 / MIT | Asset lineage vs sagas |
| AI gateway/agents | LiteLLM + vLLM / llama.cpp + MCP servers + Langfuse + Inspect | Python/C++ | MIT / Apache-2.0 | Sovereign, Apache-2.0-licensed models |
| Authz/markings | Keycloak + OpenFGA + Rust policy core (WASM) | Java/Go/Rust | Apache-2.0 | One set of semantics in every runtime |
| Lineage | OpenLineage → Vigil lineage store (+ DataHub export) | Java | Apache-2.0 | Lineage is enforced, not just visualised |
| Apollo-like deployment | Go controller + Zarf + Harbor + Flux + RKE2/k3s/Talos + OpenTofu | Go | Apache-2.0 / MPL-2.0 | Signed, air-gap-native delivery |
| Edge node | k3s + Rust agent + NATS leaf + SQLite/DuckDB + Valhalla + llama.cpp + PMTiles | Rust/Go/C++ | Permissive | Runs disconnected |
| Field mobile app | Kotlin/Compose + KMP; SQLite; read sync + queued Actions; MapLibre + PMTiles | Kotlin | Apache-2.0 / BSD | Android has 81% share in Africa |
| Web cockpit | Next.js/React, TypeScript 7, deck.gl, MapLibre, CesiumJS, Sigma.js, Yjs, generated SDK | TypeScript | MIT / BSD / Apache-2.0 (partly [UNVERIFIED]) | OSDK-style development speed |
| Real-time comms/messaging gateway | Phoenix Channels/Presence + WhatsApp Cloud, SMPP and USSD adapters over NATS | Elixir | Apache/MIT [UNVERIFIED] | BEAM fault tolerance on flaky networks |

---

## Implications for Vigil

1. **Stage language adoption.** Year 1: Java, TypeScript, Python and Kotlin (mobile), plus Rust for the edge agent and policy core as the first specialist hire. Go arrives with Vigil Apollo (months 6–12); Elixir only once messaging is a product.
2. **Build the policy core first,** in Rust, compiled to WASM. Markings and lineage propagation are Vigil's moat and its main liability, so they must behave identically in every runtime.
3. **Make contracts the product.** Protobuf plus Buf, CloudEvents and Arrow; generated SDKs are how customers, and Vigil's own teams, build.
4. **Bet on Postgres.** With PG 19's SQL/PGQ, pgvector and PostGIS, a separate graph database and vector database are deferrable.
5. **Iceberg v3 plus Polaris on SeaweedFS is the data spine.** Plan the MinIO exit now.
6. **Treat write-back as sagas, not pipelines:** Temporal for Actions, Dagster for data.
7. **Air-gap is a build-system property.** Mirror everything, sign everything, never use vendor image catalogs; Bitnami is the proof.
8. **The licence policy is a board-level control.** No BSL, SSPL, FSL or NC-licensed components in anything shipped. Prefer foundation-governed dependencies.
9. **Keep the AI sovereign by default.** Apache-2.0 open-weight models, vLLM and llama.cpp, and agents that act only through audited ontology Actions.
10. **African specifics are a differentiator.** That means African-name entity resolution, Plus Code and H3 addressing, SMS/USSD fallbacks, Digital Earth Africa, and Omnilingual ASR. Fund a small research function.
11. **Buy a commercial DNP3/IEC 61850 stack** rather than building one, and budget for Senzing and Cesium as optional paid add-ons.
12. **Revisit these choices every six months.** Six of the relicensing, acquisition and archival events in this report happened in the past 12 months: Bitnami, Kùzu, Langfuse, Astral, MinIO and JedAI.

---

## Sources

**Languages:**
- https://blog.google/security/rust-in-android-move-fast-fix-things/
- https://www.cisa.gov/news-events/alerts/2025/06/24/new-guidance-released-reducing-memory-related-vulnerabilities
- https://media.defense.gov/2025/Jun/23/2003742198/-1/-1/0/CSI_MEMORY_SAFE_LANGUAGES_REDUCING_VULNERABILITIES_IN_MODERN_SOFTWARE_DEVELOPMENT.PDF
- https://www.cisa.gov/case-memory-safe-roadmaps
- https://www.techrepublic.com/article/cisa-fbi-memory-safety-recommendations/
- https://www.infoq.com/news/2025/10/cloudflare-rust-proxy
- https://blog.cloudflare.com/pingora-open-source/
- https://discord.com/blog/why-discord-is-switching-from-go-to-rust
- https://datafusion.apache.org/blog/output/2026/08/25/datafusion-55.0.0/
- https://www.influxdata.com/blog/influxdb3-open-source-public-alpha/
- https://newsletter.pragmaticengineer.com/p/consolidating-technologies
- https://newsletter.pragmaticengineer.com/p/google-part-2
- https://www.uber.com/gb/en/blog/go-monorepo-bazel/
- https://www.infoq.com/news/2026/08/typescript-7-released/
- https://blog.jetbrains.com/idea/2025/09/java-25-lts-and-intellij-idea/
- https://openjdk.org/jeps/491
- https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now/
- https://spring.io/blog/2025/11/21/spring-modulith-2-0-ga-1-4-5-and-1-3-11-released/
- https://blogs.oracle.com/java/detaching-graalvm-from-the-java-ecosystem-train
- https://www.infoworld.com/article/4061937/graalvm-25-arrives-backed-by-jdk-25.html
- https://gs.statcounter.com/os-market-share/mobile/africa
- https://gs.statcounter.com/os-market-share/mobile/south-africa
- https://developers.redhat.com/articles/2026/09/14/python-314-free-threaded-build-now-available-rhel
- https://openai.com/index/openai-to-acquire-astral/
- https://simonwillison.net/2026/mar/19/openai-acquiring-astral/
- https://www.palantir.com/docs/foundry/ontology-sdk/overview
- https://github.com/palantir/osdk-ts
- https://discord.com/blog/how-discord-scaled-elixir-to-5-000-000-concurrent-users
- https://highscalability.com/how-whatsapp-grew-to-nearly-500-million-users-11000-cores-an/
- https://elixir-lang.org/blog/
- https://julialang.org/blog/2025/10/julia-1.12-highlights/index.html
- https://alternativeto.net/news/2026/9/julia-1-13-released-with-major-speed-gains-in-precompilation-startup-and-garbage-collection/
- https://ziglang.org/download/0.16.0/release-notes.html
- https://www.theregister.com/ai-and-ml/2026/08/12/modulars-mojo-programming-language-hits-10-milestone/5286545
- https://linuxiac.com/mojo-programming-language-goes-fully-open-source/
- https://devblogs.microsoft.com/dotnet/announcing-dotnet-10/
- https://gleam.run/news/
- https://engineering.atspotify.com/2020/08/how-we-use-golden-paths-to-solve-fragmentation-in-our-software-ecosystem
- https://www.infoq.com/news/2019/02/airbnb-monolith-migration-soa/

**Interop and build:**
- https://buf.build/docs/breaking/
- https://connectrpc.com/docs/introduction/
- https://spec.openapis.org/oas/v3.2.0.html
- https://github.com/cloudevents/spec
- https://www.asyncapi.com/docs/reference/specification/v3.0.0
- https://arrow.apache.org/adbc/current/index.html
- https://substrait.io/
- https://github.com/palantir/conjure
- https://smithy.io/2.0/index.html
- https://wasi.dev/roadmap
- https://docs.wasmtime.dev/stability-release.html
- https://github.com/extism/extism
- https://blog.bazel.build/2026/01/20/bazel-9.html
- https://buck2.build/
- https://www.pantsbuild.org/
- https://nx.dev/docs/technologies
- https://moonrepo.dev/moon

**Data layers:**
- https://github.com/minio/minio
- https://github.com/seaweedfs/seaweedfs
- https://garagehq.deuxfleurs.fr/
- https://github.com/rustfs/rustfs
- https://raw.githubusercontent.com/apache/iceberg/main/format/spec.md
- https://iceberg.apache.org/releases/
- https://github.com/apache/polaris
- https://github.com/lakekeeper/lakekeeper
- https://github.com/projectnessie/nessie
- https://github.com/unitycatalog/unitycatalog
- https://github.com/treeverse/lakeFS
- https://www.postgresql.org/about/news/postgresql-18-released-3142/
- https://www.postgresql.org/
- https://www.postgresql.org/docs/19/release-19.html
- https://github.com/pgvector/pgvector
- https://github.com/timescale/pgvectorscale
- https://github.com/apache/age
- https://github.com/timescale/timescaledb
- https://github.com/citusdata/citus
- https://github.com/duckdb/pg_duckdb
- https://github.com/paradedb/paradedb
- https://github.com/yugabyte/yugabyte-db
- https://www.cockroachlabs.com/blog/enterprise-license-announcement/
- https://github.com/pingcap/tidb
- https://github.com/ClickHouse/ClickHouse
- https://github.com/StarRocks/starrocks
- https://duckdb.org/release_calendar
- https://kafka.apache.org/blog/2025/03/18/apache-kafka-4.0.0-release-announcement/
- https://kafka.apache.org/blog
- https://raw.githubusercontent.com/redpanda-data/redpanda/dev/licenses/bsl.md
- https://www.cncf.io/announcements/2025/05/01/cncf-and-synadia-align-on-securing-the-future-of-the-nats-io-project/
- https://github.com/nats-io/nats-server
- https://flink.apache.org/2025/03/24/apache-flink-2.0.0-a-new-era-of-real-time-data-processing/
- https://flink.apache.org/posts/
- https://github.com/risingwavelabs/risingwave
- https://blog.cloudflare.com/cloudflare-acquires-arroyo-pipelines-streaming-ingestion-beta/
- https://github.com/ArroyoSystems/arroyo
- https://github.com/MaterializeInc/materialize
- https://spark.apache.org/news/index.html
- https://github.com/apache/datafusion-comet
- https://github.com/Eventual-Inc/Daft
- https://pytorch.org/foundation/
- https://github.com/ray-project/ray
- https://neo4j.com/licensing/
- https://github.com/memgraph/memgraph
- https://github.com/FalkorDB/FalkorDB
- https://github.com/surrealdb/surrealdb
- https://github.com/typedb/typedb
- https://github.com/JanusGraph/janusgraph
- https://github.com/kuzudb/kuzu
- https://www.puppygraph.com/
- https://github.com/cwida/duckpgq-extension
- https://github.com/opensearch-project/OpenSearch
- https://www.elastic.co/blog/elasticsearch-is-open-source-again
- https://quickwit.io/blog/quickwit-joins-datadog
- https://github.com/quickwit-oss/quickwit
- https://github.com/meilisearch/meilisearch
- https://github.com/vespa-engine/vespa
- https://github.com/qdrant/qdrant
- https://github.com/milvus-io/milvus
- https://github.com/lancedb/lancedb
- https://github.com/weaviate/weaviate
- https://github.com/questdb/questdb
- https://docs.influxdata.com/influxdb3/core/
- https://github.com/VictoriaMetrics/VictoriaMetrics
- https://github.com/eclipse-milo/milo
- https://github.com/open62541/open62541
- https://plc4x.apache.org/
- https://sparkplug.eclipse.org/

**Workflow, security, lineage and deployment:**
- https://github.com/temporalio/temporal
- https://raw.githubusercontent.com/restatedev/restate/main/LICENSE
- https://github.com/dbos-inc/dbos-transact-py
- https://airflow.apache.org/blog/airflow-three-point-oh-is-here/
- https://github.com/dagster-io/dagster
- https://github.com/kestra-io/kestra
- https://www.cncf.io/projects/keycloak/
- https://github.com/keycloak/keycloak
- https://github.com/zitadel/zitadel
- https://github.com/ory/kratos
- https://github.com/authzed/spicedb
- https://github.com/openfga/openfga
- https://www.cncf.io/projects/openfga/
- https://www.cncf.io/projects/cedar/
- https://github.com/cedar-policy/cedar
- https://www.openpolicyagent.org/blog
- https://github.com/aserto-dev/topaz
- https://github.com/OpenLineage/OpenLineage
- https://openlineage.io/
- https://github.com/datahub-project/datahub
- https://github.com/open-metadata/OpenMetadata
- https://github.com/great-expectations/great_expectations
- https://github.com/sodadata/soda-core
- https://github.com/dbt-labs/dbt-fusion
- https://www.getdbt.com/blog/dbt-labs-and-fivetran-merge-announcement
- https://github.com/zarf-dev/zarf
- https://github.com/defenseunicorns/uds-core
- https://github.com/siderolabs/talos
- https://docs.rke2.io/
- https://github.com/k3s-io/k3s
- https://github.com/fluxcd/flux2
- https://github.com/argoproj/argo-cd
- https://www.cncf.io/projects/
- https://www.cncf.io/projects/opentofu/
- https://github.com/opentofu/opentofu
- https://slsa.dev/spec/v1.1/
- https://github.com/aquasecurity/trivy
- https://goharbor.io/
- https://github.com/bitnami/containers/issues/83267

**Edge, geo, AI, entity resolution and optimisation:**
- https://kubeedge.io/
- https://github.com/tursodatabase/turso
- https://github.com/automerge/automerge
- https://github.com/yjs/yjs
- https://github.com/loro-dev/loro
- https://github.com/electric-sql/electric
- https://raw.githubusercontent.com/powersync-ja/powersync-service/main/LICENSE
- https://github.com/rocicorp/mono
- https://github.com/eclipse-mosquitto/mosquitto
- https://github.com/emqx/emqx
- https://github.com/nanomq/nanomq
- https://github.com/chirpstack/chirpstack
- https://developers.facebook.com/docs/whatsapp/on-premises/sunset
- https://raw.githubusercontent.com/nyaruka/rapidpro/main/LICENSE
- https://geoparquet.org/
- https://github.com/apache/sedona-db
- https://github.com/protomaps/PMTiles
- https://github.com/CesiumGS/cesium
- https://docs.overturemaps.org/attribution/
- https://osmfoundation.org/wiki/Licence/Community_Guidelines/Produced_Work_-_Guideline
- https://github.com/developmentseed/titiler
- https://stacspec.org/en
- https://www.digitalearthafrica.org/
- https://github.com/valhalla/valhalla
- https://github.com/Project-OSRM/osrm-backend
- https://github.com/graphhopper/graphhopper
- https://github.com/uber/h3
- https://github.com/google/open-location-code
- https://github.com/vllm-project/vllm
- https://github.com/sgl-project/sglang
- https://github.com/ggml-org/llama.cpp
- https://github.com/NVIDIA/TensorRT-LLM
- https://github.com/ai-dynamo/dynamo
- https://openai.com/index/introducing-gpt-oss/
- https://huggingface.co/Qwen/Qwen3.8-27B
- https://huggingface.co/deepseek-ai/DeepSeek-V4-Pro-0813
- https://mistral.ai/news/mistral-3
- https://ai.google.dev/gemma/docs/core
- https://dev.meta.ai/llama/llama4/license
- https://huggingface.co/meta-llama
- https://huggingface.co/Qwen/Qwen3-Embedding-8B
- https://huggingface.co/BAAI/bge-m3
- https://huggingface.co/lelapa/InkubaLM-0.4B
- https://huggingface.co/facebook/nllb-200-distilled-600M
- https://huggingface.co/facebook/mms-1b-all
- https://github.com/facebookresearch/omnilingual-asr
- https://huggingface.co/Davlan/afro-xlmr-large
- https://www.masakhane.io/
- https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3
- https://github.com/docling-project/docling
- https://github.com/PaddlePaddle/PaddleOCR
- https://github.com/allenai/olmocr
- https://github.com/rednote-hilab/dots.ocr
- https://github.com/datalab-to/marker
- https://www.linuxfoundation.org/press/linux-foundation-announces-the-formation-of-the-agentic-ai-foundation
- https://github.com/BerriAI/litellm
- https://github.com/langfuse/langfuse
- https://github.com/promptfoo/promptfoo
- https://github.com/UKGovernmentBEIS/inspect_ai
- https://huggingface.co/meta-llama/Llama-Guard-4-12B
- https://github.com/NVIDIA/NeMo-Guardrails
- https://github.com/moj-analytical-services/splink
- https://github.com/zinggAI/zingg
- https://github.com/dedupeio/dedupe
- https://senzing.com/pricing/
- https://github.com/scify/JedAIToolkit
- https://github.com/openvenues/libpostal
- https://github.com/google/or-tools
- https://github.com/ERGO-Code/HiGHS
- https://github.com/TimefoldAI/timefold-solver
- https://github.com/VROOM-Project/vroom

**Listed but not verified this session:** https://msrc.microsoft.com/blog/2019/07/a-proactive-approach-to-more-secure-code/ · https://kotlinlang.org/compose-multiplatform/
