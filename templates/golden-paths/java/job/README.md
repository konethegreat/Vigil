<!-- Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Java job golden path README (P1 task 1.4) -->

# Java job golden path

A one-shot batch entry point with validated configuration, an opt-in OTLP
exporter and a Warden binding stub that fails closed. Jobs have no HTTP server
and no health probes: their outcome is the exit code and the audit record.

## What is here

| File | Purpose |
|---|---|
| `Settings.java` | Environment configuration, validated before any work starts |
| `policy/` | `PolicyContext`, the `WardenBinding` seam, `UnavailableWarden` and `PolicyUnavailableException` |
| `Telemetry.java` | Provider with an OTLP processor only when `VIGIL_OTEL_ENDPOINT` is set |
| `Execute.java` | The unit of work; records only tenant and purpose on its span |
| `JobMain.java` | Entry point, exit-code contract and guaranteed provider shutdown |

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
| `VIGIL_WARDEN_URL` | Yes | In-boundary Warden PDP; must be `http(s)` |
| `VIGIL_POLICY_TOKEN_REF` | Yes | `kms://` reference for the service-principal token |
| `VIGIL_BATCH_SIZE` | No | Defaults to 100; must be a positive integer |
| `VIGIL_OTEL_ENDPOINT` | No | `http(s)`; unset or empty means no export and no egress |

Errors name the variable and never print its value. `.env.example` shows the
shape and contains references only; pass the values as environment variables.

## Run

`./gradlew :job:run` is fine for a smoke test, but Gradle turns a non-zero
application exit into a failed task. To observe the exit-code contract, build
the distribution and invoke it directly:

```bash
./gradlew :job:installDist
VIGIL_TENANT_ID=tenant-1 \
VIGIL_PURPOSE=DATA_INTEGRATION \
VIGIL_WARDEN_URL=http://warden.invalid:8080 \
VIGIL_POLICY_TOKEN_REF=kms://vigil/dev/job-token#1 \
job/build/install/job/bin/job
echo $?
```

On Windows the launcher is `job\build\install\job\bin\job.bat`.

## Verify

```bash
./gradlew :job:check
```

The gate is 85 % line coverage overall and 100 % for
`africa.vigil.templates.job.policy`.

## When copying

1. Copy the module into the job's home and replace the
   `africa.vigil.templates.job` package and `application.mainClass`.
2. Replace `Execute.execute` with the real batch. Keep the policy check in
   `run`, so no data is touched before a context exists.
3. Replace `UnavailableWarden` with the P3 binding. Never add an allow-all mode
   or a force flag.
4. Keep the exit codes; the scheduler and alerts depend on them.
5. Add the job to its scheduler and runbook before enabling it.

The full contract is in [`../README.md`](../README.md).
