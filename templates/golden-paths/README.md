<!-- Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Golden-path template contract and language rollout (P1 task 1.4) -->
<!-- Modified by Kone Tshivhinda (Qoder) on 2026-11-01 — Java 25 templates are available; state the coverage metric per stack -->
<!-- Modified by Kone Tshivhinda (Qoder) on 2026-11-01 — TypeScript templates are available; state the TypeScript version hold -->
<!-- Modified by Kone Tshivhinda (Qoder) on 2026-11-01 — Rust library and job templates are available (P1 task 1.4) -->

# Golden-path templates

A golden path is the smallest complete project that already satisfies Vigil's
non-negotiables: it can be copied, renamed and extended without re-deciding
security, observability or configuration. Every language in the charter
(INSTRUCTIONS §6, ADR-0001) gets one service, one library and one job template.

These templates are scaffolds, not services. They live in
`templates/golden-paths/` (P1 territory) and are copied into a service
directory when that service starts. Nothing imports them, and they are never
deployed from this folder.

## What every template carries

| Element | Requirement | Why |
|---|---|---|
| Attribution header | `Written by` on line 1, `Modified by` appended on later edits | INSTRUCTIONS §8; traceability |
| Configuration | Validated at startup from the environment; invalid configuration stops the process | BACKEND_GUIDE §9.3; fail fast |
| Secrets | KMS references (`kms://...`) only, never values in files, images or git | INSTRUCTIONS §9.10; Gitleaks |
| Health | `/healthz/live`, `/healthz/ready`, `/healthz/startup` on services; not applicable to libraries or jobs | BACKEND_GUIDE §9.4 |
| Warden | Binding stub that builds a `PolicyContext`; it fails closed until the P3 binding is wired | INSTRUCTIONS §9.1; ADR-0005 |
| OpenTelemetry | OTLP export only when an in-boundary collector endpoint is configured; no SaaS exporters and no required egress | BACKEND_GUIDE §9.2; INSTRUCTIONS §9.8 |
| SBOM | Committed lockfile and pinned dependency floors so Syft can inventory the project | INSTRUCTIONS §6.5; ADR-0003 |
| Tests | Runnable offline, at least 85 % coverage, 100 % for the policy module; the metric is the one the stack's coverage tool enforces (branch for Python, line for JaCoCo and cargo-llvm-cov, statements and branches for Vitest) | BACKEND_GUIDE §1 B8 |

## Shapes

| Shape | Has HTTP | Health endpoints | Warden | Use it for |
|---|---|---|---|---|
| `service` | Yes (FastAPI, Spring Boot or Fastify) | Yes | Binding stub in-process | Request-serving APIs |
| `library` | No | Not applicable | Receives a `PolicyContext`, never constructs one | Shared code imported by services and jobs |
| `job` | No | Not applicable | Binding stub in-process | One-shot batch work |

A library must never read the environment for tenant or purpose. Its caller
owns configuration and the policy context, and passes both explicitly
(BACKEND_GUIDE §2.4, §9.1).

## Language rollout

| Language | Status | Templates | Owner |
|---|---|---|---|
| Python 3.13 | Available | `python/service`, `python/library`, `python/job` | P1 |
| Java 25 | Available | `java/service`, `java/library`, `java/job` | P1 |
| TypeScript 6.0.3 | Available | `typescript/service`, `typescript/library` | P1, before P10 |
| Rust 1.99.1 | Available | `rust/library`, `rust/job`; a service shape is added with P3 if Warden needs a gRPC/REST listener | P1, with P3 |
| Go | Planned | Service, library and job | P1, with P20 |
| Kotlin | Planned | Field app module, not a service | P1, with P27 |
| Elixir | Deferred | Comms gateway only, if the P28 ADR selects it | P28 |
| SQL | Not a runtime template | Schema and RLS templates live in DATA_MODEL | P3 |

The rollout table is the source of truth for task 1.4. A shape that does not
exist yet is listed as planned, not silently omitted.

## Copying a Python template

1. Copy the shape into the target service directory, for example
   `templates/golden-paths/python/service` to `services/<name>/`.
2. Rename the package directory (`vigil_service_template` to
   `vigil_<service>`) and update `[project].name`, `[tool.hatch.build.targets.wheel]`,
   `[tool.mypy].files` and the coverage target in `pyproject.toml`.
3. Replace the `service_name` default and the example route with the real
   contract. Keep the health paths unchanged.
4. Replace `UnavailableWarden` with the P3 binding. Do not add an allow-all
   mode, and do not read tenant or purpose from anywhere except the verified
   request context.
5. Keep `OTEL_EXPORTER_OTLP_ENDPOINT` empty unless the deployment has an
   in-boundary collector. The default must stay no-egress.
6. Run `uv lock` once, commit `uv.lock`, then verify with the commands below.
7. Add the service to `docker-compose.dev.yml`, `ci-backend.yml` and
   `docs/RUNNING_VIGIL.md` in the same change that makes it deployable.

## Copying a Java template

1. Copy one shape directory (`service`, `library` or `job`) into the target
   service directory.
2. Replace the `africa.vigil.templates.<shape>` package and the `group` in
   `build.gradle.kts`; keep the JaCoCo policy-package rule aligned with the new
   package name.
3. Replace the example route or work unit with the real contract. Keep the
   health paths, the policy seam and the `kms://`-only checks unchanged.
4. Replace `UnavailableWarden` with the P3 binding. Never add an allow-all mode
   or a bypass flag.
5. After changing dependencies run `./gradlew --write-locks` and commit the
   updated `gradle.lockfile` with the change.
6. Add the service to `docker-compose.dev.yml`, `ci-backend.yml` and
   `docs/RUNNING_VIGIL.md` in the same change that makes it deployable.

## Copying a TypeScript template

1. Copy one shape directory (`service` or `library`) into the target service
   directory and remove its `node_modules`, `dist` and `coverage` folders. Its
   `pnpm-workspace.yaml` lists `.`, so the copy is its own pnpm root.
2. Rename the package in `package.json`; for a service, also set
   `VIGIL_SERVICE_NAME`.
3. Replace the example route or operation with the real contract. Keep the
   health paths, the `authorize(...)` call on data-bearing routes and the
   `kms://`-only checks unchanged.
4. Replace `UnavailableWarden` with the P3 binding. Never add an allow-all mode
   or a bypass flag.
5. Run `pnpm install` after changing dependencies and commit `pnpm-lock.yaml`
   with the change.
6. Add the service to `docker-compose.dev.yml`, `ci-backend.yml` and
   `docs/RUNNING_VIGIL.md` in the same change that makes it deployable.

## Copying a Rust template

1. Copy `library/` or `job/` into the target cargo workspace. The crates
   inherit `[workspace.dependencies]` and `[workspace.lints]` from
   `rust/Cargo.toml`, so move those keys into the target root manifest instead
   of keeping two copies.
2. Rename the package in the crate's `Cargo.toml`, keep `publish = false` unless
   the crate is released, and keep the crate listed as a workspace member.
3. Replace the example operation or work unit with the real contract. Keep the
   policy seam, the `kms://`-only checks and the job exit codes (`0` success,
   `2` configuration, `3` policy) unchanged.
4. Replace `UnavailableWarden` with the P3 binding. Never add an allow-all mode
   or a bypass flag.
5. Copy `rust-toolchain.toml` and `deny.toml` to the workspace root. After
   changing dependencies run `cargo update`, `cargo deny check` and commit
   `Cargo.lock` with the change.
6. Add the crate to `ci-backend.yml` and `docs/RUNNING_VIGIL.md` in the same
   change that makes it deployable.

## Verification

`scripts/ci/run-templates.sh` runs every template check that CI runs, in one
place; `just templates` calls it. A language whose toolchain is absent is
skipped with a warning, and GitHub Actions installs every toolchain.

Each Python template is a standalone uv project:

```bash
cd templates/golden-paths/python/<shape>
uv sync --locked
uv run ruff check .
uv run ruff format --check .
uv run mypy --strict src
uv run pytest --cov=vigil_<package> --cov-branch --cov-fail-under=85
```

The Java templates are one Gradle build with three modules:

```bash
cd templates/golden-paths/java
./gradlew --no-daemon check
```

`check` compiles, tests and enforces JaCoCo: 85 % line coverage per module and
100 % for each `policy` package. The library holds 100 % overall. Java gates on
line coverage, the same rule Core uses; Python gates on branch coverage.

The TypeScript shapes are two standalone pnpm roots:

```bash
cd templates/golden-paths/typescript/library   # or service
pnpm install --frozen-lockfile
pnpm run check
```

`check` runs Prettier, ESLint, `tsc --noEmit`, Vitest with coverage and the
build. Vitest gates on statements, branches, functions and lines at 85 %, with
100 % on `src/policy.ts`; the library holds 100 % overall. TypeScript 7 is held
until `typescript-eslint` widens its peer range past `<6.1.0`, so the templates
pin 6.0.3, the version the cockpit builds with.

The Rust shapes are one cargo workspace with `library` and `job` members:

```bash
cd templates/golden-paths/rust
cargo fmt --all --check
cargo clippy --workspace --all-targets --locked -- -D warnings
cargo deny check
cargo llvm-cov --workspace --locked --fail-under-lines 85
```

cargo-llvm-cov gates on line coverage at 85 % for the workspace, and the gate
also requires 100 % lines on every `src/policy.rs`; the library holds 100 %
overall. `cargo deny check` applies `deny.toml`, which mirrors the ADR-0003
allow list. Syft has no Rust remote-licence lookup, so the SBOM job delegates
`pkg:cargo` packages to the same cargo-deny licence check.

The service and job templates also assert that a protected path returns `503`
(or, for a job, exit code `3`) while the Warden binding is unavailable. That is
the expected state until P3 lands; it is not a test to delete.

## Rules that templates must not soften

- No bypass flag, no "system" context and no allow-all binding (INSTRUCTIONS
  §9.1, GUARDRAILS §3).
- No real personal information in tests, fixtures or examples (INSTRUCTIONS
  §9.2).
- No secret values in `.env.example`, images or git. Secrets are references
  only (INSTRUCTIONS §9.10).
- No required outbound network call. Telemetry, Warden and storage are all
  in-boundary (INSTRUCTIONS §9.8).
- No red-line capability, and no feature flag that enables one (GUARDRAILS §2).
- No deletion of another author's attribution line (INSTRUCTIONS §8).

## Maintenance

- Dependency floors move with the owning service. When a shared library
  version changes, update the template in the same `chore/` change so the next
  service starts from the current baseline.
- A template that cannot pass its own verification is a bug in the template,
  not a reason to weaken the gate.
- Changes to this folder need a WORKLOG row and normal PR review under MSG-042.
