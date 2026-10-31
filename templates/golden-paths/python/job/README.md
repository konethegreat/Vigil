<!-- Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Python job golden path README (P1 task 1.4) -->

# Python job golden path

A one-shot batch entry point with validated configuration, an opt-in OTLP
exporter and a Warden binding stub that fails closed. Jobs have no HTTP server
and no health probes: their outcome is the exit code and the audit record.

## What is here

| File | Purpose |
|---|---|
| `src/vigil_job_template/config.py` | Environment configuration, validated before any work starts |
| `src/vigil_job_template/policy.py` | `PolicyContext`, the `WardenBinding` seam and `UnavailableWarden` |
| `src/vigil_job_template/telemetry.py` | OTLP export only when `OTEL_EXPORTER_OTLP_ENDPOINT` is set |
| `src/vigil_job_template/main.py` | `run` orchestration, `execute` placeholder and the exit-code contract |

## Exit codes

| Code | Meaning |
|---|---|
| 0 | The batch completed. |
| 2 | Configuration was missing or invalid. Nothing ran. |
| 3 | Policy evaluation was unavailable. Nothing ran, by design. |

The template returns 3 until the P3 binding replaces `UnavailableWarden`. A
data-bearing job must never run without a context (BACKEND_GUIDE §9.1, B4).

## Configuration

| Variable | Required | Notes |
|---|---|---|
| `VIGIL_TENANT_ID` | Yes | Tenant the run belongs to |
| `VIGIL_PURPOSE` | Yes | The single purpose this job is allowed to serve |
| `VIGIL_WARDEN_URL` | Yes | In-boundary Warden PDP |
| `VIGIL_POLICY_TOKEN_REF` | Yes | `kms://` reference for the service-principal token |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | No | Unset means no export and no egress |
| `VIGIL_JOB_NAME` | No | Defaults to `vigil-job-template` |
| `VIGIL_BATCH_SIZE` | No | Defaults to 100; must be positive |

`.env.example` shows the shape. It contains references only.

## Run

```bash
uv sync --locked
VIGIL_TENANT_ID=tenant-1 \
VIGIL_PURPOSE=DATA_INTEGRATION \
VIGIL_WARDEN_URL=http://warden.invalid:8080 \
VIGIL_POLICY_TOKEN_REF=kms://vigil/dev/job-token#1 \
uv run python -m vigil_job_template.main
echo $?
```

## Verify

```bash
uv sync --locked
uv run ruff check .
uv run ruff format --check .
uv run mypy --strict src
uv run pytest --cov=vigil_job_template --cov-branch --cov-fail-under=85
```

## When copying

1. Rename `vigil_job_template` to `vigil_<job>` and update `[project].name`,
   the hatch wheel path, the mypy files list and the coverage target.
2. Replace `execute` with the real batch. Keep the policy check in `run`, so no
   data is touched before a context exists.
3. Replace `UnavailableWarden` with the P3 binding. Never add an allow-all mode
   or a force flag.
4. Keep the exit codes; the scheduler and alerts depend on them.
5. Add the job to its scheduler and runbook before enabling it.

The full contract is in [`../README.md`](../README.md).
