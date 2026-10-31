<!-- Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Python service golden path README (P1 task 1.4) -->

# Python service golden path

A request-serving FastAPI service with validated configuration, health probes,
an opt-in OTLP exporter and a Warden binding stub that fails closed. Copy it
when a Python service starts; do not import it.

## What is here

| File | Purpose |
|---|---|
| `src/vigil_service_template/config.py` | Environment configuration, validated at startup; secrets are `kms://` references |
| `src/vigil_service_template/policy.py` | `PolicyContext`, the `WardenBinding` seam and `UnavailableWarden` |
| `src/vigil_service_template/deps.py` | Auth to policy-context dependency; maps Warden failures to 503 or 403 |
| `src/vigil_service_template/telemetry.py` | OTLP export only when `OTEL_EXPORTER_OTLP_ENDPOINT` is set |
| `src/vigil_service_template/main.py` | `create_app` factory, health probes and one guarded example route |

## Health probes

| Path | Meaning |
|---|---|
| `/healthz/live` | The process can serve. It never checks dependencies. |
| `/healthz/startup` | The lifespan has finished. Returns 503 before that. |
| `/healthz/ready` | Started and Warden is ready. Returns 503 while the stub is in place. |

The readiness 503 is intentional. A service must not report ready while a
fail-closed dependency is unavailable (BACKEND_GUIDE §9.4, B4).

## Configuration

| Variable | Required | Notes |
|---|---|---|
| `VIGIL_WARDEN_URL` | Yes | `http(s)` URL of the in-boundary Warden PDP |
| `VIGIL_INTERNAL_TOKEN_REF` | Yes | `kms://` reference, never a token value |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | No | Leave unset for no egress; point it at the in-boundary collector |
| `VIGIL_ENVIRONMENT` | No | `dev` (default), `test`, `staging` or `production` |
| `VIGIL_SERVICE_NAME` | No | Defaults to `vigil-service-template`; rename when copying |
| `VIGIL_HOST` | No | Defaults to `127.0.0.1`; set `0.0.0.0` only inside a container network |
| `VIGIL_PORT` | No | Defaults to 8000 |

Invalid configuration stops the process. `.env.example` shows the shape; it
contains references only.

## Run

```bash
uv sync --locked
VIGIL_WARDEN_URL=http://warden.invalid:8080 \
VIGIL_INTERNAL_TOKEN_REF=kms://vigil/dev/service-auth#1 \
uv run uvicorn vigil_service_template.main:create_app --factory
```

The guarded route needs `X-Vigil-Tenant`, `X-Vigil-Purpose` and
`X-Vigil-Policy-Token`. Until the P3 binding replaces `UnavailableWarden`, it
returns 503. That is correct fail-closed behaviour, not a bug.

## Verify

```bash
uv sync --locked
uv run ruff check .
uv run ruff format --check .
uv run mypy --strict src
uv run pytest --cov=vigil_service_template --cov-branch --cov-fail-under=85
```

## When copying

1. Rename `vigil_service_template` to `vigil_<service>` and update
   `[project].name`, the hatch wheel path, the mypy files list and the coverage
   target.
2. Keep the health paths and their semantics.
3. Replace `UnavailableWarden` with the P3 binding. Never add an allow-all mode
   or a bypass flag.
4. Replace the example route with the real contract, and keep
   `Depends(get_policy_context)` on every data-bearing route.
5. Add the service to Compose, CI and the runbook before deploying it.

The full contract, including the shared rules and the language rollout, is in
[`../README.md`](../README.md).
