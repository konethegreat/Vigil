<!-- Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Rust golden-path index (P1 task 1.4) -->

# Rust golden paths

Two shapes in one cargo workspace:

| Shape | Use it for | Crate |
|---|---|---|
| `library` | Shared code imported by Warden crates | `vigil-library-template` |
| `job` | One-shot batch work behind the Warden gate | `vigil-job-template` |

A service shape is not provided. ADR-0005 keeps Warden as the only data door,
and P3 decides whether it needs a gRPC or REST listener. If it does, the
service shape copies the job's configuration and telemetry modules and adds the
health probes the contract requires.

## Version pins

| Tool | Version | Why |
|---|---|---|
| Rust | 1.99.1, edition 2024, resolver 3 | Pinned in `rust-toolchain.toml`; CI installs the same toolchain |
| OpenTelemetry | 0.33.x (`opentelemetry`, `opentelemetry_sdk`, `opentelemetry-otlp`) | Current stable line, traces only |
| reqwest | 0.13.x with `native-tls` | The OTLP HTTP client. `native-tls` uses the platform store, so no crate pulls a bundled root list outside the ADR-0003 green set |
| cargo-deny | latest, installed by `taiki-e/install-action` | Licence, advisory, ban and source checks against `deny.toml` |
| cargo-llvm-cov | latest, installed by `taiki-e/install-action` | Line coverage from the pinned toolchain's LLVM tools |

`deny.toml` carries the ADR-0003 green set. Syft has no Rust remote-licence
lookup, so the SBOM job runs `cargo deny check licenses` for every committed
`Cargo.lock` and records the `pkg:cargo` packages as delegated to it.

## What each shape does

`library` is caller-owned. `LibrarySettings::new` validates a namespace and
batch size, and `PolicyContext::new` validates a verified tenant and purpose;
the library never reads the environment for either. `summarise` records only
tenant and purpose on its span, and the context's `Debug` prints the signed
token as `redacted`.

`job` reads `VIGIL_TENANT_ID`, `VIGIL_PURPOSE`, `VIGIL_WARDEN_URL` and
`VIGIL_POLICY_TOKEN_REF` (a `kms://` reference) from the environment, with
`VIGIL_BATCH_SIZE`, `VIGIL_LOG_LEVEL`, `VIGIL_JOB_NAME` and
`VIGIL_OTEL_ENDPOINT` optional. Every problem is reported at once and the
process exits `2`; a policy failure exits `3`. OTLP export is built only when
an endpoint is set, and no span starts before Warden authorizes the run.

## Verify

From this folder:

```bash
cargo fmt --all --check
cargo clippy --workspace --all-targets --locked -- -D warnings
cargo deny check
cargo llvm-cov --workspace --locked --fail-under-lines 85
```

`scripts/ci/run-templates.sh` runs the same gates, including the per-file rule
that every `src/policy.rs` stays at 100 % lines. The library holds 100 %
overall.

## Copying

Copy `library/` or `job/` into the target workspace and move the
`[workspace.dependencies]` and `[workspace.lints]` keys from `Cargo.toml` into
the target root manifest. Copy `rust-toolchain.toml` and `deny.toml` to the
workspace root, commit `Cargo.lock`, and keep the policy seam and exit codes
unchanged.

The full contract, including the shared rules and the language rollout, is in
[`../README.md`](../README.md).
