<!-- Written by Kone Tshivhinda (Qoder) on 2026-11-01 — TypeScript library golden path README (P1 task 1.4) -->

# TypeScript library golden path

A shared TypeScript package that receives configuration and a `PolicyContext`
from its caller. It has no HTTP server, no health probes and no environment
reads: the importing service or job owns those concerns.

## Prerequisites

- Node 24 LTS or newer (`engines` declares `>=24`; CI runs 24).
- pnpm 11.17.0, pinned in `packageManager`.
- TypeScript 6.0.3, the version this repository builds with. TypeScript 7 is
  held until `typescript-eslint` widens its peer range past `<6.1.0`.

Each shape is a standalone pnpm root: `pnpm-workspace.yaml` lists `.` so the
project installs on its own and survives being copied out of this folder.

## What is here

| File               | Purpose                                                                                |
| ------------------ | -------------------------------------------------------------------------------------- |
| `src/settings.ts`  | `librarySettings`, validated at construction and injected by the caller                |
| `src/policy.ts`    | `PolicyContext`, `MissingPolicyContext` and `requirePolicyContext`, which fails closed |
| `src/summarise.ts` | Example operation: reads records only after the context check                          |
| `src/index.ts`     | The public surface; import from the package root                                       |

## Rules for libraries

- **Receive, never build.** A library takes a `PolicyContext` as an argument. It
  never constructs one, never reads tenant or purpose from the environment and
  never asks Warden on its own (BACKEND_GUIDE §2.4, §9.1).
- **No global telemetry setup.** Use the OpenTelemetry API only. The service
  owns the provider and exporter, so importing a library never opens a network
  connection (BACKEND_GUIDE §9.2, INSTRUCTIONS §9.8). `summarise` uses the
  global tracer when the caller injects none, which is a no-op until the host
  installs a provider.
- **No environment reads.** Configuration arrives as an explicit object.
- **Fail closed.** A data-bearing function refuses to run without a usable
  context, as `summarise` does.
- **No content in telemetry.** Only `tenant` and `purpose` are recorded (B5).

Health probes do not apply to a library. The service or job that hosts it
exposes `/healthz/live` and `/healthz/ready` (BACKEND_GUIDE §9.4).

## Verify

```bash
pnpm install --frozen-lockfile
pnpm run format:check
pnpm run lint
pnpm run typecheck
pnpm run test:coverage
pnpm run build
```

`pnpm run check` runs the same six commands. Coverage is 100 % for this
library: it is small, and the Java and Python library templates hold the same
floor. The barrel `src/index.ts` is excluded from the coverage map because it
only re-exports; every module behind it is measured.

## When copying

1. Rename the package in `package.json` and the folder to the domain name.
2. Keep `requirePolicyContext` on every data-bearing entry point.
3. Keep the `exports` map pointing at `dist`, and commit `pnpm-lock.yaml`.
4. Do not add a Warden binding, an HTTP server or environment parsing to a
   library; those belong in the host.

The full contract is in [`../README.md`](../README.md).
