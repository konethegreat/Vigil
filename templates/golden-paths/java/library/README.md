<!-- Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Java library golden path README (P1 task 1.4) -->

# Java library golden path

A shared Java library that receives configuration and a `PolicyContext` from
its caller. It has no HTTP server, no health probes and no environment reads:
the importing service or job owns those concerns.

## What is here

| File | Purpose |
|---|---|
| `LibrarySettings.java` | Immutable settings validated at construction and injected by the caller |
| `PolicyContext.java` | Verified tenant and purpose, created by the caller and not by the library |
| `Summaries.java` | Example operation: reads records only after a context exists, and spans only tenant and purpose |
| `Summary.java` | The operation's result record |

## Rules for libraries

- **Receive, never build.** A library takes a `PolicyContext` as an argument. It
  never constructs one, never reads tenant or purpose from the environment and
  never asks Warden on its own (BACKEND_GUIDE §2.4, §9.1).
- **No global telemetry setup.** Use the OpenTelemetry API only. The caller owns
  the provider and exporter, so importing the library never opens a network
  connection (BACKEND_GUIDE §9.2, INSTRUCTIONS §9.8).
- **No environment reads.** Configuration arrives as an explicit object.
- **Fail closed.** A data-bearing function refuses to run without a usable
  context, as `Summaries.summarise` does.
- **No content in telemetry.** Only tenant, purpose, counts and error codes are
  recorded (B5).

Health probes do not apply to a library. The service or job that hosts it
exposes `/healthz/live` and `/healthz/ready` (BACKEND_GUIDE §9.4).

## Verify

```bash
./gradlew :library:check
```

The module is small enough to hold a 100 % line-coverage gate.

## When copying

1. Copy the sources into the shared package and replace
   `africa.vigil.templates.library`.
2. Keep the `PolicyContext` parameter on every data-bearing entry point.
3. Keep the OpenTelemetry API dependency only; do not add the SDK or an exporter
   to a library.
4. Do not add a Warden binding, an HTTP server or environment parsing to a
   library; those belong in the host.

The full contract is in [`../README.md`](../README.md).
