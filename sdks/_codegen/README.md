<!-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Contract codegen verification harness documentation (P2 task 2.1, task 2.5) -->
<!-- Modified by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Document WIT world checks and Java -> Kafka -> Python -> Rust EventEnvelope round-trip (P2 task 2.4, Done-when) -->
# Contract Codegen Harness (`sdks/_codegen`)

`sdks/_codegen/` is the P2 contract codegen and interop verification harness
(tasks 2.1, 2.4, and 2.5). It verifies that:

1. The Protobuf contracts in [`proto/vigil/`](../../proto/vigil/) generate and
   compile cleanly across all six contract-consuming languages in the Vigil
   language charter (ADR-0001): **Java**, **Kotlin**, **Python**, **Go**,
   **TypeScript**, and **Rust**.
2. The WebAssembly Component Model WIT worlds in
   [`proto/wit/common/`](../../proto/wit/common/) (`vigil:policy@1.0.0` and
   `vigil:udf@1.0.0`) satisfy their structural and capability-sandbox contracts
   (no ambient `wasi:*` imports).
3. A sample `EventEnvelope` (wrapping a `vigil.common.v1.ResourceId` payload in
   `google.protobuf.Any`) round-trips end-to-end across **Java -> Kafka ->
   Python -> Rust** using a disposable `apache/kafka:4.4.0` KRaft broker.

In P31, this harness extends into the ontology-scoped SDK generator driven by
Ontology Metadata Service (OMS) type definitions.

## Why generated stubs stay untracked

Generated Protobuf stubs are written to `sdks/_codegen/gen/<lang>/`, which is
gitignored. Keeping generated stubs out of version control avoids multi-thousand
line diffs on every schema change while `verify.sh` proves that every contract
change compiles across all six target languages from pinned Buf remote plugins
and locked language dependencies.

## Package and option conventions

Every `.proto` file under `proto/vigil/` declares explicit language options:

- **Go (`go_package`):** `github.com/konethegreat/Vigil/gen/go/<proto-dir>;<alias>`
  (for example `github.com/konethegreat/Vigil/gen/go/vigil/common/v1;commonv1`
  and `github.com/konethegreat/Vigil/gen/go/vigil/events/v1;eventsv1`).
- **Java & Kotlin (`java_package`, `java_multiple_files`):**
  `africa.vigil.contracts.<domain>.v1` with `java_multiple_files = true`
  (matching the repository's `africa.vigil` package and Gradle group convention).

## Pinned versions

### Buf CLI and remote plugins ([`proto/buf.gen.yaml`](../../proto/buf.gen.yaml))

| Component | Pinned reference | Output directory | Options |
|---|---|---|---|
| Buf CLI container | `bufbuild/buf:1.73.0` | none | none |
| Java plugin | `buf.build/protocolbuffers/java:v33.1` | `sdks/_codegen/gen/java` | none |
| Kotlin plugin | `buf.build/protocolbuffers/kotlin:v33.1` | `sdks/_codegen/gen/kotlin` | none |
| Python plugin | `buf.build/protocolbuffers/python:v33.1` | `sdks/_codegen/gen/python` | none |
| Python typing plugin | `buf.build/protocolbuffers/pyi:v33.1` | `sdks/_codegen/gen/python` | none |
| Go plugin | `buf.build/protocolbuffers/go:v1.36.10` | `sdks/_codegen/gen/go` | `paths=source_relative` |
| TypeScript plugin | `buf.build/bufbuild/es:v2.9.0` | `sdks/_codegen/gen/ts` | `target=ts` |
| Rust plugin | `buf.build/community/neoeinstein-prost:v0.4.0` | `sdks/_codegen/gen/rust` | none |

### Language compile harnesses

| Language | Harness location | Runtime / compiler pins | How it verifies |
|---|---|---|---|
| **Rust** | `sdks/_codegen/rust/` | `prost` 0.14.x, `prost-types` 0.14.x (`Cargo.lock`, `deny.toml`) | Includes `vigil.common.v1.rs` and `vigil.events.v1.rs`, runs `cargo check --locked` and `cargo test --locked` |
| **TypeScript** | `sdks/_codegen/typescript/` | `typescript` 6.0.3, `@bufbuild/protobuf` 2.9.0 (`pnpm-lock.yaml`) | Stages generated `.ts` into `src/gen/`, runs `tsc --noEmit` under strict flags |
| **Python** | `sdks/_codegen/python/check_generated.py` | Python 3.13+ (`py_compile`, `ast`), optional `google.protobuf` | Byte-compiles every `*_pb2.py`, parses every `*_pb2.pyi` AST, and imports modules if `google.protobuf` is installed |
| **Java & Kotlin** | `sdks/_codegen/java/` | JDK 25, Gradle 9.8.2, `kotlin("jvm")` 2.3.20, `protobuf-java` 4.33.1, `protobuf-kotlin` 4.33.1 (`gradle.lockfile`) | Compiles `../gen/java` and `../gen/kotlin` together via `./gradlew classes` |
| **Go** | `verify.sh` (container) | `golang:1.27.1-alpine`, `google.golang.org/protobuf` v1.36.10 | Initializes a temporary module in the container and runs `go build ./...` across all generated packages |

## Running the harness

From the repository root in Git Bash (with Docker Desktop running):

```sh
bash sdks/_codegen/verify.sh
```

`verify.sh` performs these steps in order:

1. Cleans `sdks/_codegen/gen/`.
2. Runs `buf lint proto` in `bufbuild/buf:1.73.0`.
3. Runs `buf breaking proto` against `origin/develop` in `bufbuild/buf:1.73.0`.
4. Runs `buf generate proto --template proto/buf.gen.yaml --path proto/vigil`.
5. Compiles Java & Kotlin and runs `RoundTripMain` (`./gradlew emitSampleEvent`)
   to serialize a canonical `EventEnvelope` wire payload.
6. Launches an ephemeral `apache/kafka:4.4.0` KRaft container, publishes the
   serialized envelope to `vigil.demo.ontology.types`, and consumes it back.
7. Validates `proto/wit/common/{policy,udf}.wit`, byte-compiles and AST-checks
   Python stubs, and verifies the Kafka-consumed `EventEnvelope` wire fields in
   Python, writing `sdks/_codegen/gen/roundtrip/envelope.bin`.
8. Compiles and runs `cargo test --locked` in `sdks/_codegen/rust`, decoding
   `envelope.bin` via `prost` 0.14 and asserting all `EventEnvelope` and inner
   `ResourceId` fields.
9. Compiles the generated TypeScript and Go outputs.
