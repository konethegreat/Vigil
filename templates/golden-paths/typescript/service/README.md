<!-- Written by Kone Tshivhinda (Qoder) on 2026-11-01 — TypeScript service golden path README (P1 task 1.4) -->

# TypeScript service golden path

A request-serving Fastify service with validated configuration, health probes,
an opt-in OTLP exporter and a Warden binding stub that fails closed. Copy it
when a TypeScript service starts; do not import it.

## Prerequisites

- Node 24 LTS or newer (`engines` declares `>=24`; CI runs 24).
- pnpm 11.17.0, pinned in `packageManager`.
- TypeScript 6.0.3, the version this repository builds with. TypeScript 7 is
  held until `typescript-eslint` widens its peer range past `<6.1.0`; ESLint is
  on 10.14.0 here because this template has no Next.js lint plugins, unlike
  the cockpit which stays on 9.

Each shape is a standalone pnpm root: `pnpm-workspace.yaml` lists `.` so the
project installs on its own and survives being copied out of this folder.

## What is here

| File               | Purpose                                                                                       |
| ------------------ | --------------------------------------------------------------------------------------------- |
| `src/config.ts`    | Environment configuration, validated before the server binds; secrets are `kms://` references |
| `src/policy.ts`    | `PolicyContext`, the `WardenBinding` seam and `UnavailableWarden`                             |
| `src/telemetry.ts` | OTLP export only when an in-boundary collector endpoint is set                                |
| `src/app.ts`       | `buildApp` factory, health probes and one guarded example route                               |
| `src/main.ts`      | Process entry: loads settings, starts telemetry, serves, shuts down on a signal               |

## Health probes

| Path               | Meaning                                                              |
| ------------------ | -------------------------------------------------------------------- |
| `/healthz/live`    | The process can serve. It never checks dependencies.                 |
| `/healthz/startup` | The server has finished booting. Returns 503 before that.            |
| `/healthz/ready`   | Started and Warden is ready. Returns 503 while the stub is in place. |

The readiness 503 is intentional. A service must not report ready while a
fail-closed dependency is unavailable (BACKEND_GUIDE §9.4, B4).

## Configuration

| Variable                                              | Required | Notes                                                                  |
| ----------------------------------------------------- | -------- | ---------------------------------------------------------------------- |
| `VIGIL_WARDEN_URL`                                    | Yes      | `http(s)` URL of the in-boundary Warden PDP                            |
| `VIGIL_INTERNAL_TOKEN_REF`                            | Yes      | `kms://` reference, never a token value                                |
| `VIGIL_OTEL_ENDPOINT` / `OTEL_EXPORTER_OTLP_ENDPOINT` | No       | Leave unset for no egress; the `VIGIL_` name wins when both are set    |
| `VIGIL_ENVIRONMENT`                                   | No       | `dev` (default), `test`, `staging` or `production`                     |
| `VIGIL_SERVICE_NAME`                                  | No       | Defaults to `vigil-service-template`; rename when copying              |
| `VIGIL_HOST`                                          | No       | Defaults to `127.0.0.1`; set `0.0.0.0` only inside a container network |
| `VIGIL_PORT`                                          | No       | Defaults to 8080                                                       |
| `VIGIL_LOG_LEVEL`                                     | No       | `INFO` (default), `WARNING` or `ERROR`                                 |

Invalid configuration stops the process with exit code 2 and lists every
problem at once. `.env.example` shows the shape; it contains references only,
and the template reads the process environment, not the file.

## Run

```bash
pnpm install --frozen-lockfile
pnpm run build
VIGIL_WARDEN_URL=http://warden.invalid:8080 \
VIGIL_INTERNAL_TOKEN_REF=kms://vigil/dev/service-auth#1 \
pnpm start
```

The guarded route needs `X-Vigil-Tenant`, `X-Vigil-Purpose` and
`X-Vigil-Policy-Token`. Missing or blank headers are a 400. Until the P3
binding replaces `UnavailableWarden`, a complete request returns 503 and
readiness stays 503. That is correct fail-closed behaviour, not a bug. Denied
requests are a 403, and error bodies never echo the header values.

## Verify

```bash
pnpm install --frozen-lockfile
pnpm run format:check
pnpm run lint
pnpm run typecheck
pnpm run test:coverage
pnpm run build
```

`pnpm run check` runs the same six commands. The floors are 85 % on lines,
statements, functions and branches, with 100 % on `src/policy.ts`. `src/main.ts`
is outside the coverage map because it only wires the process together, the
same code the Java service leaves outside its floor.

## When copying

1. Rename the package in `package.json` and set `VIGIL_SERVICE_NAME`.
2. Keep the health paths and their semantics.
3. Replace `UnavailableWarden` with the P3 binding. Never add an allow-all mode
   or a bypass flag.
4. Replace the example route with the real contract, and keep `authorize(...)`
   on every data-bearing route.
5. Commit `pnpm-lock.yaml` whenever dependencies change.
6. Add the service to Compose, CI and the runbook before deploying it.

The full contract, including the shared rules and the language rollout, is in
[`../README.md`](../README.md).
