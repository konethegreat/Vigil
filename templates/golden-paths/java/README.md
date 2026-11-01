<!-- Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Java 25 golden-path README (P1 task 1.4) -->

# Java 25 golden paths

One Gradle build with the same three shapes as the Python templates: a Spring
Boot service, a shared library and a one-shot job. The shared contract, the
security rules and the language rollout live in
[`../README.md`](../README.md).

These modules are scaffolds. They live in `templates/golden-paths/` and are
copied into a service directory when that service starts; nothing here is
deployed.

## Prerequisites

- JDK 25. Each module pins a Java 25 toolchain, so Gradle refuses to build with
  an older JDK instead of silently compiling for the wrong target.
- The committed Gradle wrapper (9.8.2). Do not install Gradle globally; the
  wrapper jar is the same pinned binary as the repository root wrapper.
- The first build resolves Maven Central. After that, `gradle.lockfile` in each
  module and `settings-gradle.lockfile` fail the build if a dependency resolves
  to a version that is not committed.

## Modules

| Module | Shape | What it carries |
|---|---|---|
| `service` | Spring Boot web service | Health probes, validated configuration, policy argument resolver, opt-in OTLP |
| `library` | Plain Java library | Caller-supplied `PolicyContext` and settings, OpenTelemetry API only |
| `job` | `application` plugin, one-shot | Exit-code contract, validated environment, opt-in OTLP |

Versions are declared once in `gradle/libs.versions.toml`: Spring Boot 4.1.2,
OpenTelemetry 1.67.0, JUnit 6.1.3 and AssertJ 3.27.7.

## Verify

```bash
cd templates/golden-paths/java
./gradlew --no-daemon check
```

`check` compiles both source sets, runs the tests and enforces JaCoCo:

| Module | Gate |
|---|---|
| `service` | 85 % line coverage overall, 100 % for `africa.vigil.templates.service.policy` |
| `library` | 100 % line coverage |
| `job` | 85 % line coverage overall, 100 % for `africa.vigil.templates.job.policy` |

JaCoCo also records branch coverage in its report, but the gate is the line
ratio, the same rule Core uses. The Python templates gate on branch coverage
because that is what their coverage tool measures; both enforce 100 % on the
policy seam. Run one module with `./gradlew :service:check` (or `:library`,
`:job`).

## Expected failures until P3

The Warden binding is the fail-closed `UnavailableWarden` stub:

- `/healthz/ready` returns `503`.
- `/v1/example` returns `503`.
- The job exits `3` without touching data.

Those are asserted in the tests. They are the correct state before the P3
binding lands, not a bug and not a test to delete.

## Telemetry

The service and job create an `SdkTracerProvider` with only `service.name` and
`deployment.environment.name` as resource attributes. An OTLP span processor is
added only when `VIGIL_OTEL_ENDPOINT` is set, so the default configuration makes
no outbound call. Spans carry `vigil.tenant` and `vigil.purpose` and nothing
from the payload.

## When copying

1. Copy one shape directory into the target service, for example `service` into
   `services/<name>/`.
2. Replace the `africa.vigil.templates.<shape>` package with the service
   package, and update `group` in `build.gradle.kts`. Keep the `policy` package
   name in the JaCoCo rule aligned with the new package.
3. Keep the health paths and their semantics, the policy seam and the
   `kms://`-only configuration checks.
4. Replace `UnavailableWarden` with the P3 binding. Never add an allow-all mode
   or a bypass flag.
5. After changing dependencies run `./gradlew --write-locks` and commit the
   updated lockfiles with the change.
6. Add the service to Compose, CI and the runbook in the same change that makes
   it deployable.
