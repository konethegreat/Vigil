<!-- Written by Kone Tshivhinda (Qoder) on 2026-11-01 — TypeScript golden-path index (P1 task 1.4) -->

# TypeScript golden paths

Two shapes, each a standalone pnpm project:

| Shape | Use it for | Read |
|---|---|---|
| `library` | Shared code imported by services and jobs | [`library/README.md`](library/README.md) |
| `service` | Request-serving HTTP APIs | [`service/README.md`](service/README.md) |

A job shape is not provided: no Node runtime job is planned before P10. If one
appears, it copies the service's configuration and telemetry modules and drops
the HTTP surface, the same way the Python and Java job templates differ from
their services.

## Version pins

| Tool | Version | Why |
|---|---|---|
| Node | 24 LTS (`engines` `>=24`) | INSTRUCTIONS §6; CI runs 24 |
| pnpm | 11.17.0 | The repository's `packageManager` |
| TypeScript | 6.0.3 | Held below 7: `typescript-eslint` 8.72.1 declares `typescript <6.1.0` |
| ESLint | 10.14.0 | `typescript-eslint` 8.72.1 supports `^10`; the cockpit stays on 9 because its Next.js lint plugins declare `<=9` |
| Vitest | 5.0.5 | The cockpit's test runner |
| Fastify (service) | 5.12.7 | MIT, no mandatory network access |
| OpenTelemetry JS | API 1.10.0, SDK 3.0.0, OTLP HTTP exporter 0.300.0 | Matches the JS SDK's current stable line |

When a hold is lifted, raise it here, in both `package.json` files and in the
root Dependabot ignore rules in the same change.

## Verify

From this folder, for each shape:

```bash
cd library   # or service
pnpm install --frozen-lockfile
pnpm run check
```

`check` formats, lints, type-checks, tests with coverage and builds. The
shared gate, `scripts/ci/run-templates.sh`, runs the same commands for both
shapes and is called by `just templates` and CI.

## Copying

Copy one shape directory and drop its `node_modules`, `dist` and `coverage`
folders. The shape's `pnpm-workspace.yaml` lists `.`, so the copy is its own
pnpm root and does not need this repository's workspace.

The full contract, including the shared rules and the language rollout, is in
[`../README.md`](../README.md).
