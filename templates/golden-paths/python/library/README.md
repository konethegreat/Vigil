<!-- Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Python library golden path README (P1 task 1.4) -->

# Python library golden path

A shared Python package that receives configuration and a `PolicyContext` from
its caller. It has no HTTP server, no health probes and no environment reads:
the importing service or job owns those concerns.

## What is here

| File | Purpose |
|---|---|
| `src/vigil_library_template/config.py` | `LibrarySettings`, validated at construction and injected by the caller |
| `src/vigil_library_template/policy.py` | Structural `PolicyContext` and `require_policy_context`, which fails closed |
| `src/vigil_library_template/summarise.py` | Example operation: reads records only after the context check |

## Rules for libraries

- **Receive, never build.** A library takes a `PolicyContext` as an argument. It
  never constructs one, never reads tenant or purpose from the environment and
  never asks Warden on its own (BACKEND_GUIDE §2.4, §9.1).
- **No global telemetry setup.** Use the OpenTelemetry API only. The service
  owns the provider and exporter, so importing a library never opens a network
  connection (BACKEND_GUIDE §9.2, INSTRUCTIONS §9.8).
- **No environment reads.** Configuration arrives as an explicit object.
- **Fail closed.** A data-bearing function refuses to run without a usable
  context, as `summarise` does.
- **No content in telemetry.** Only `tenant`, `purpose`, counts and error codes
  are recorded (B5).

Health probes do not apply to a library. The service or job that hosts it
exposes `/healthz/live` and `/healthz/ready` (BACKEND_GUIDE §9.4).

## Verify

```bash
uv sync --locked
uv run ruff check .
uv run ruff format --check .
uv run mypy --strict src
uv run pytest --cov=vigil_library_template --cov-branch --cov-fail-under=85
```

## When copying

1. Rename `vigil_library_template` to `vigil_<domain>` and update
   `[project].name`, the hatch wheel path, the mypy files list and the coverage
   target.
2. Keep `require_policy_context` on every data-bearing entry point.
3. Keep `py.typed` so callers get the annotations.
4. Do not add a Warden binding, an HTTP server or environment parsing to a
   library; those belong in the host.

The full contract is in [`../README.md`](../README.md).
