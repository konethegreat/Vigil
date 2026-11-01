<!-- Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Java service golden path README (P1 task 1.4) -->

# Java service golden path

A Spring Boot web service with validated configuration, health probes, an
opt-in OTLP exporter and a Warden binding stub that fails closed. Copy it when
a Java service starts; do not import it.

## What is here

| File | Purpose |
|---|---|
| `config/Settings.java` | `@ConfigurationProperties` record, validated at startup; secrets are `kms://` references |
| `policy/` | `PolicyContext`, the `WardenBinding` seam, `UnavailableWarden` and the two failure types |
| `web/PolicyContextArgumentResolver.java` | Resolves `PolicyContext` from the three verified headers; Warden failures become 503 or 403 |
| `web/HealthController.java` | `/healthz/live`, `/healthz/startup` and `/healthz/ready` |
| `web/ExampleController.java` | One guarded example route; replace it with the real contract |
| `web/ApiExceptionHandler.java` | `ProblemDetail` responses that never echo header values |
| `telemetry/` | Provider and tracer beans; OTLP only when `VIGIL_OTEL_ENDPOINT` is set |

## Health probes

| Path | Meaning |
|---|---|
| `/healthz/live` | The process can serve. It never checks dependencies. |
| `/healthz/startup` | The web context is serving. No warm-up work is modelled yet. |
| `/healthz/ready` | The binding reports ready. Returns 503 while the stub is in place. |

The readiness 503 is intentional. A service must not report ready while a
fail-closed dependency is unavailable (BACKEND_GUIDE §9.4, B4).

## Configuration

| Variable | Required | Notes |
|---|---|---|
| `VIGIL_WARDEN_URL` | Yes | `http(s)` URL of the in-boundary Warden PDP |
| `VIGIL_INTERNAL_TOKEN_REF` | Yes | `kms://` reference, never a token value |
| `VIGIL_SERVICE_NAME` | No | Defaults to `vigil-service-template`; rename when copying |
| `VIGIL_ENVIRONMENT` | No | Defaults to `dev`; recorded as `deployment.environment.name` |
| `VIGIL_PORT` | No | Defaults to 8000 |
| `VIGIL_OTEL_ENDPOINT` | No | `http(s)` collector endpoint; empty means no export and no egress |

An unresolved required value, a non-HTTP Warden URL or a token reference that
is not `kms://` stops startup. `.env.example` shows the shape and contains
references only; Spring Boot does not read `.env` itself, so export the values
or pass them through the Compose `environment`/`env_file` block.

## Run

```bash
VIGIL_WARDEN_URL=http://warden.invalid:8080 \
VIGIL_INTERNAL_TOKEN_REF=kms://vigil/dev/service-auth#1 \
./gradlew :service:bootRun
```

The guarded route needs `X-Vigil-Tenant`, `X-Vigil-Purpose` and
`X-Vigil-Policy-Token`. Until the P3 binding replaces `UnavailableWarden`, it
returns 503. That is correct fail-closed behaviour, not a bug.

## Verify

```bash
./gradlew :service:check
```

The gate is 85 % line coverage overall and 100 % for
`africa.vigil.templates.service.policy`.

## When copying

1. Copy the module into `services/<name>/` and replace the
   `africa.vigil.templates.service` package.
2. Keep the health paths and their semantics.
3. Replace `UnavailableWarden` with the P3 binding. Never add an allow-all mode
   or a bypass flag.
4. Replace the example route with the real contract, and keep `PolicyContext`
   as a handler argument on every data-bearing route.
5. Add the service to Compose, CI and the runbook before deploying it.

The full contract, including the shared rules and the language rollout, is in
[`../README.md`](../README.md).
