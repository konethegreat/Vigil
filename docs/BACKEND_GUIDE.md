<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Polyglot backend engineering guide: Python, Java, Rust, Go, Kotlin, Elixir, SQL, cross-cutting rules, local Docker CI and container monitoring (P0) -->

# Vigil — Backend Engineering Guide

> **Status:** P0 v1.0 (2026-09-25). Owned by P0; material changes need an ADR (File Ownership Master Index).
> **Read with:** [`INSTRUCTIONS.md`](../INSTRUCTIONS.md) (§5 Karpathy, §6 language charter, §8 attribution, §9 security), [`ARCHITECTURE.md`](ARCHITECTURE.md) 🧊, [`DATA_MODEL.md`](DATA_MODEL.md) 🧊, [`API_DESIGN.md`](API_DESIGN.md), [`SECURITY_AND_COMPLIANCE.md`](SECURITY_AND_COMPLIANCE.md), [`SYSTEM_COMPLIANCE_GUARDRAILS.md`](SYSTEM_COMPLIANCE_GUARDRAILS.md).
> **Precedence:** ARCHITECTURE, VIGIL_DOMAIN_BLUEPRINT, DATA_MODEL and accepted ADRs win over this guide. If this guide contradicts them, the guide is wrong: file a 🐛 BUG in `docs/AI_COMMUNICATION.md`.

## 0. How to use this guide

- §1 binds every language. §2–§8 are per language. §9 is cross-cutting. §10 is the local CI engine and the container-monitoring protocol that INSTRUCTIONS §11 item 9 points to.
- **MUST** means CI or review blocks the PR. **SHOULD** means any deviation is explained in the PR description.
- Tool versions are pinned by P1 (`rust-toolchain.toml`, Gradle toolchains, `.python-version`, `go.mod`, `packageManager`). This guide names tools, not versions.
- Code snippets are illustrative. The owning phase writes the real code, inside its own territory.

## 1. Rules for every backend language

| # | Rule | Enforced by |
|---|---|---|
| B1 | **Warden is the only door.** Every read or write of tenant data carries a `PolicyContext` evaluated by the Warden policy core ([ADR-0005](adr/ADR-0005-security-kernel-first.md)). There is no bypass flag and no "system" context on request paths. | Review, ArchUnit/lint rules, P3 conformance suite, P34 red team |
| B2 | **Own your database; use everyone else's contract.** No service connects to another service's database, schema or tables (ARCHITECTURE §3.2). | One DB role per service; NetworkPolicies |
| B3 | **Contracts first.** Cross-service calls use generated Protobuf/Connect clients or CloudEvents (§9.6). No hand-rolled JSON between services. | `buf lint`, `buf breaking` |
| B4 | **Fail closed.** If Warden, the audit ledger or KMS is unreachable, data-bearing requests fail with `503`, never with an unfiltered or unaudited answer. | Chaos tests (P8, P33) |
| B5 | **No property values, prompts or personal information in logs, traces or metrics** (ARCHITECTURE §11). | Log-scrubbing tests; review |
| B6 | **No required egress.** No code path may need a network call outside the deployment boundary (INSTRUCTIONS §9.8). | Egress-deny tests (P14.4); default-deny NetworkPolicies |
| B7 | **Idempotent writes.** Every mutating endpoint accepts an `Idempotency-Key` (§9.5). | Contract tests |
| B8 | **Coverage floors:** at least 85% line coverage per service, and **100%** for connectors, Warden bindings and policy-adjacent code: anything that builds a `PolicyContext`, applies redactions, compiles RLS/DLS policies, writes audit events or gates a module. | CI coverage gates |
| B9 | **Synthetic data only** outside production (INSTRUCTIONS §9.2). | Review; fixture scanners |
| B10 | **Attribution headers** on every file you create or modify (INSTRUCTIONS §8; §9.7 below). | `validate-compliance.py`; review |

## 2. Python services (FastAPI)

**Scope:** `services/mind/{gateway,agents,er,docai}/` (P14–P17), `services/forge-transforms/` (P12, Dagster jobs rather than HTTP services), `services/eo/` (P29), `services/sim/` (P30). Python 3.13+. The free-threaded build is not relied on (appendix 05 §1.4). Python services never read the ontology's databases directly: they call the Object Set, Action and Functions APIs *as the user* (ARCHITECTURE §6.4). Each service's own database (review queues, job state, metering) is private to it.

### 2.1 Layout (per service)

```
services/mind/er/
├── pyproject.toml            # uv project; ruff, mypy, pytest and coverage config
├── uv.lock                   # committed (INSTRUCTIONS §6 rule 5; AI_COMMUNICATION MSG-005)
├── src/vigil_mind_er/
│   ├── main.py               # create_app(): middleware stack (§2.5), routers, lifespan
│   ├── deps.py               # the Depends chain (§2.4)
│   ├── api/                  # one router module per resource + Pydantic In/Out schemas
│   ├── services/             # use cases; receive PolicyContext + session explicitly
│   ├── domain/               # pure logic; imports nothing from api/ or db/
│   ├── db/                   # SQLAlchemy models, repositories, engine factory
│   └── settings.py           # pydantic-settings; secret *references* only (§9.3)
└── tests/{unit,component,contract}/
```

Package names follow `vigil_<service>` (ARCHITECTURE §13). The `domain → services → api` import direction is checked by the P1 golden-path import contracts.

### 2.2 Async patterns

- Use `async def` for I/O-bound endpoints. **Never block the event loop:** no `requests`, `time.sleep`, synchronous DB drivers or heavy CPU inside `async def`. Ruff's `ASYNC` rules catch the common cases.
- CPU-bound work (Splink runs, OCR, embeddings) goes to worker processes or GPU workers. A short blocking call may use `anyio.to_thread.run_sync`. Long jobs return `202 Accepted` with a job resource.
- Create one `httpx.AsyncClient` and one `AsyncEngine` per process in the `lifespan` handler, with explicit timeouts. Never create them per request.
- Put a deadline on every outbound call (`asyncio.timeout()`); gateways and models get their own budgets.
- Use `asyncio.TaskGroup` for fan-out. Never call `asyncio.create_task()` without keeping and awaiting the task (the same "never fire-and-forget" rule as §10.3). Never swallow `asyncio.CancelledError`.
- `contextvars` carry the request ID and `policy_ctx_hash` for logging only. Authorisation data travels as explicit parameters, never through ambient context.

### 2.3 SQLAlchemy 2.x async session lifecycle

1. **One `AsyncEngine` per process; one `AsyncSession` per request**, created by a dependency. No global or module-level sessions, no session stored on `app.state`, and no session shared between concurrent tasks (`AsyncSession` is not safe for concurrent use).
2. **The dependency owns the transaction.** It opens `session.begin()`, commits on success and rolls back on any exception. Services and repositories never call `commit()` or `rollback()`; they may call `flush()`.
3. **The first two statements of every transaction set and verify the policy context** (DATA_MODEL §4.2): `SET LOCAL vigil.policy_ctx = '<token>'`, issued as `SELECT set_config('vigil.policy_ctx', :token, true)` because plain `SET` cannot take bind parameters, and then `SELECT vigil_sec.apply_ctx()`. The token is the one `warden-pdp` signed for this request; services never build it themselves. Never use session-level `SET`: a pooled connection would carry one user's context into another user's request.
4. **No lazy loading across `await` boundaries.** Configure `expire_on_commit=False` and declare relationships with `lazy="raise"`, so an accidental lazy load fails loudly instead of issuing hidden I/O. Load what you need in the query (`selectinload()`, `joinedload()`).
5. **Never return ORM instances** from services; map them to Pydantic Out models (§2.6).
6. **Never hold a transaction across a remote call** (LLM, HTTP, Temporal). Long jobs use one short transaction per batch.
7. Pool settings: `pool_pre_ping=True`; `pool_size` fits the service role's connection budget; timeouts are set per role (§8).
8. The commit happens when the dependency exits, before the response is sent. P1 pins the FastAPI version, and a component test asserts that a failed commit produces an error envelope, never a `200`.

```python
# deps.py (illustrative)
async def get_session(
    ctx: Annotated[PolicyContext, Depends(get_policy_context)],
    request: Request,
) -> AsyncIterator[AsyncSession]:
    maker: async_sessionmaker[AsyncSession] = request.app.state.sessionmaker  # built once in lifespan
    async with maker() as session, session.begin():      # commit on success, rollback on exception
        await session.execute(
            text("SELECT set_config('vigil.policy_ctx', :token, true)"),  # = SET LOCAL, transaction-scoped
            {"token": ctx.signed_pg_token()},                            # signed by warden-pdp (DATA_MODEL §4.2)
        )
        await session.execute(text("SELECT vigil_sec.apply_ctx()"))    # verifies the signature or raises
        yield session
```

### 2.4 Dependency injection with `Depends`: auth → policy context → session → service

```python
Principal = Annotated[AuthenticatedPrincipal, Depends(get_principal)]        # 1 token verified by middleware
Ctx = Annotated[PolicyContext, Depends(get_policy_context)]                   # 2 Warden context: purpose, case
Session = Annotated[AsyncSession, Depends(get_session)]                       # 3 per-request transaction, ctx set
Reviews = Annotated[ReviewQueueService, Depends(get_review_queue_service)]    # 4 service, built from 1–3

@router.post("/v1/review-items/{item_id}/decision", response_model=ReviewDecisionOut)
async def decide(item_id: RID, body: ReviewDecisionIn, ctx: Ctx, reviews: Reviews) -> ReviewDecisionOut:
    return await reviews.decide(ctx, item_id, body)
```

- FastAPI caches each dependency per request, so the same `PolicyContext` instance reaches the session and the service.
- `get_policy_context` builds the context through the Warden binding (P3) from the verified principal, the declared purpose and case, and any justification reference. A purpose-bound route without a purpose returns `PURPOSE_REQUIRED`, and a case-predicated purpose without an open, visible case returns `CASE_REQUIRED` (API_DESIGN §A.6).
- Services take `PolicyContext` as an explicit argument. Domain code never uses `Depends`.
- Tests override dependencies with `app.dependency_overrides`, using the real Warden binding and synthetic markings. **Never** stub Warden to "allow all".
- Route paths, error codes and headers follow `docs/API_DESIGN.md`; the paths above are placeholders.

### 2.5 Middleware ordering

Request path, outermost first: **request ID → OpenTelemetry tracing → auth/OIDC → tenant & purpose context → rate limit → audit → error handler**.

| # | Layer | Why it sits here |
|---|---|---|
| 1 | Request ID | Assigned first, so every later log line, span, audit record and error envelope carries it, including rejected requests. An inbound `X-Request-Id` (API_DESIGN §A.2) is trusted only from the gateway; otherwise one is generated. It is echoed in every response. |
| 2 | OTel tracing | Starts the server span early enough to time and record auth failures and throttling. It extracts W3C `traceparent` and records route templates, status, tenant, purpose and `policy_ctx_hash`, never bodies. |
| 3 | Auth / OIDC | Verifies the Keycloak access token (signature against a cached, in-boundary JWKS; `iss`, `aud`, `exp`, `nbf`, `azp`) with no network call per request. Everything below needs an authenticated principal. It writes its own `401` envelope, because exception handlers do not cover middleware. |
| 4 | Tenant & purpose | Takes the tenant from the token's `vigil_tenant` claim and requires `X-Vigil-Tenant` to match (otherwise `POLICY_DENIED`). It reads `X-Vigil-Purpose`, `X-Vigil-Case-Id` and `X-Vigil-Justification` (API_DESIGN §A.2), and stores them on `request.state`. Rate limiting and audit both need them. |
| 5 | Rate limit | Token buckets per tenant + principal + route class, in Valkey (ARCHITECTURE §5). It sits after auth, so limits apply per principal rather than per IP (IP limits live at the Envoy gateway), and before audit, so floods don't swamp the ledger. Rejections become security metrics. If Valkey is unavailable it degrades to a conservative per-replica limiter; it neither fails open nor takes the service down. |
| 6 | Audit | Wraps everything that can touch data, so it records the final outcome: status, row count, marking set and obligations applied, taken from `request.state`. The event is written when the response starts, before the body is released. If the ledger write fails, data-bearing responses fail closed (B4: no audit, no access). |
| 7 | Error handler | Innermost: FastAPI exception handlers wrap routes and dependencies. They turn domain errors into the `VigilError` envelope (API_DESIGN) with the request ID and without stack traces or values, so the audit layer sees a real status code. Starlette's `ServerErrorMiddleware` stays outermost as a last-resort bare `500`. |

Starlette runs the **last-added** middleware **first**, so register in reverse. Write middleware as pure ASGI classes: `BaseHTTPMiddleware` has known problems with `contextvars` and streaming.

```python
def create_app(settings: Settings) -> FastAPI:
    app = FastAPI(lifespan=lifespan, openapi_url=None if settings.is_production else "/openapi.json")
    register_exception_handlers(app)          # 7: VigilError envelope; never echoes request input
    app.add_middleware(AuditMiddleware)       # 6
    app.add_middleware(RateLimitMiddleware)   # 5
    app.add_middleware(TenantPurposeMiddleware)  # 4
    app.add_middleware(OidcAuthMiddleware)    # 3
    app.add_middleware(OpenTelemetryMiddleware)  # 2 (opentelemetry-instrumentation-asgi)
    app.add_middleware(RequestIdMiddleware)   # 1: outermost
    return app
```

### 2.6 Pydantic v2 schemas

- **Separate models per direction:** `XCreateIn`, `XUpdateIn` (explicit PATCH semantics), `XOut`, `XPage`. One model never serves both request and response.
- **Strict request bodies:** `model_config = ConfigDict(strict=True, extra="forbid", frozen=True)`. Query and path parameters use `Annotated` constraints instead, because strict mode rejects the string forms FastAPI receives.
- **No ORM leakage:** Out models are built explicitly from domain objects (`XOut.from_domain(obj)`). `from_attributes=True` is allowed only inside the mapping layer, and every route declares `response_model`.
- **Redaction is explicit:** a withheld property uses the shared redacted-value schema from the contract (API_DESIGN, `proto/vigil/common/`), never `null`, which would be indistinguishable from "no value".
- **Validation errors never echo input.** Pydantic errors include an `input` field, which may be personal information. The `RequestValidationError` handler strips it before building the envelope.
- Typed identifiers (`RID`, with the pattern from P2), timezone-aware UTC datetimes (`AwareDatetime`), and `Decimal` plus currency code for money. No floats for money.

```python
class ReviewDecisionIn(BaseModel):
    model_config = ConfigDict(strict=True, extra="forbid", frozen=True)
    decision: Literal["CONFIRM", "REJECT", "DEFER"]
    justification: Annotated[str, StringConstraints(min_length=20, max_length=2000)]

class ReviewDecisionOut(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)
    item_id: RID
    decision: Literal["CONFIRM", "REJECT", "DEFER"]
    decided_at: AwareDatetime
    audit_event_id: str
```

### 2.7 uv, ruff, mypy and pytest conventions

```bash
uv sync --locked                                  # never re-resolve in CI; uv.lock is committed
uv run ruff check .                               # E,F,W,I,B,UP,S,ASYNC,PT,SIM,RUF,TID
uv run ruff format --check .
uv run mypy --strict src
uv run pytest -q --cov=src --cov-branch --cov-fail-under=85
uv run coverage report --include='src/vigil_mind_gateway/redaction/*' --fail-under=100   # policy-adjacent: 100%
```

- Markers: `unit` (no network; `pytest-socket --disable-socket`), `component` (Testcontainers), `contract` (schemas against API_DESIGN and `proto/`). Use `asyncio_mode = "auto"` and `--strict-markers`.
- Test containers come from the Vigil registry mirror, never directly from Docker Hub, and never Bitnami images (appendix 05 §4m).
- Code that must reach 100% coverage in Python: Mind gateway redaction and egress policy, the agent tool layer (tools run as the user), the ER s57 linkage lock, and doc-AI untrusted-content wrapping. **Keep it under a `policy/` sub-package, and connectors under `connectors/`,** because `scripts/ci/run-backend.sh` enforces the 100% gate on exactly those paths.
- `mypy --strict` is a MUST gate under this guide, but the P0 runner does not run it yet; P1 adds it (§10.2).
- uv and Ruff are single-vendor tools (the Astral acquisition, appendix 05 §1.4). Mirror their binaries and keep a pip-compatible export (`uv export --format requirements-txt`) as the air-gap fallback.

## 3. Java 25 / Spring Boot 4 / Spring Modulith 2

**Scope:** `services/core/` (the Modulith: P3 audit, P5 ontology and shared, P7 objects, P8 actions, P9 functions, P38 peering), `services/forge-datasets/` (P4), `services/forge-connectors/` (P11 framework; P22–P24 connectors).

### 3.1 Build, formatting and static analysis

- Gradle with the Kotlin DSL; `gradle.lockfile` and dependency-verification metadata are committed; the toolchain is Java 25.
- **Formatter: google-java-format, applied through Spotless** (`./gradlew spotlessCheck`). *Why:* it needs no configuration, has the widest IDE and tooling support, and there is no fork to track. palantir-java-format (a fork tuned for long lambda chains) and Eclipse formatter profiles were considered and rejected. There are no per-file overrides; generated code is excluded.
- Error Prone plus NullAway over JSpecify `@NullMarked` packages (the null-safety model Spring Boot 4 adopts), compiled with `-Werror`. P1's golden path confirms the exact plug-in set.
- **No preview features** (`--enable-preview`) in shipped code. Structured concurrency is still a preview API in JDK 25.

### 3.2 Module boundaries (`vigil.core.<module>`)

- Each application module is a direct sub-package of `vigil.core`: `ontology`, `objects`, `actions`, `functions`, `lineage`, `audit`, `peering` and `shared` (ARCHITECTURE §3.1, §4).
- A module's **public API** is the set of types in its root package, plus any sub-package exposed with `@NamedInterface`. Everything else lives under `vigil.core.<module>.internal..` and is invisible to other modules. Controllers go in `internal.web` (Rule 3: one controller set per module).
- `shared` holds value types only (`PolicyContext` adapters, RIDs, error types). It depends on no other module and stays small.
- Module dependencies are declared, never implied:

```java
// services/core/src/main/java/vigil/core/actions/package-info.java (illustrative)
@org.springframework.modulith.ApplicationModule(
    displayName = "Actions",
    allowedDependencies = {"shared", "ontology", "objects::api", "audit::api"})
@org.jspecify.annotations.NullMarked
package vigil.core.actions;
```

- Modulith verification runs on every build:

```java
class ModularityTests {
  @Test void moduleBoundariesHold() {   // fails on cycles, internal-package access and undeclared dependencies
    ApplicationModules.of(VigilCoreApplication.class).verify();
  }
}
```

- Use `@ApplicationModuleTest` to test one module in isolation, with its collaborators mocked (Rule 4). Cross-module calls go through the other module's public interfaces or through application events (§3.4). Cycles are not allowed.

### 3.3 Transactions: an Action is one transaction plus an outbox (A5)

```java
// vigil.core.actions.internal.SubmitActionService (illustrative)
ActionResult submit(PolicyContext ctx, ActionRequest req) {
  var run = runs.persistOrReuse(ctx, req);                    // 0: own transaction; same Idempotency-Key ⇒ same run ID
  var plan = validator.authoriseAndValidate(ctx, req);        // 1–2: Warden + OpenFGA + submission criteria; read-only
  plan.webhook().ifPresent(w ->                                // 3: synchronous, pre-commit, ≤ 10 s; no DB transaction open
      writeback.call(w, req.idempotencyKey(), Duration.ofSeconds(10)));  // failure throws: nothing is committed
  return tx.execute(status -> {                                // 4: exactly one transaction
    policyContexts.applyLocal(ctx);                            //    SET LOCAL vigil.policy_ctx + vigil_sec.apply_ctx()
    var applied = edits.apply(plan);                           //    typed tables (optimistic concurrency per DATA_MODEL)
    editLog.append(applied);                                   //    append-only edit log: the source of truth (A4)
    outbox.enqueue(OntologyEdited.of(applied));                //    relayed to Kafka ontology.edits.v1 after commit
    return ActionResult.committed(applied);
  });                                                          // 5–6 run after commit: outbox relay, Temporal sagas
}
```

- Transaction boundaries sit on application-service methods only (`TransactionTemplate` or `@Transactional`), never on controllers or repositories. Queries use read-only transactions.
- **No remote call inside the transaction.** The webhook runs before `BEGIN`; side effects run after commit, as idempotent Temporal activities fed by the outbox.
- No `REQUIRES_NEW` inside an Action: it would split the atomic unit.
- The webhook call carries `X-Vigil-Action-Run-Id`, `X-Vigil-Idempotency-Key` and an HMAC `X-Vigil-Signature` over mTLS, and **targets must deduplicate on `X-Vigil-Action-Run-Id`** (API_DESIGN §A.9). If the commit fails after a successful webhook, the engine records a reconciliation event and the P8 compensation path takes over (INCIDENT_RESPONSE runbook R9).
- The v1 limits (10,000 objects, 3 MB per edit, a 60 s budget, one webhook per Action; ARCHITECTURE §6.3) are checked before the webhook runs.

### 3.4 Application events

- **Between modules, in-process:** publish Spring application events inside the transaction and consume them with `@ApplicationModuleListener`, which runs after commit, asynchronously, in its own transaction. Enable the JDBC Event Publication Registry so incomplete publications are retried after a crash.
- **Between services:** **only the P8 outbox**, relayed to Kafka as CloudEvents named `vigil.<domain>.<entity>.<verb>.v1` with Protobuf payloads from `proto/vigil/events/` (ARCHITECTURE §8). Modulith's event externalisation is not used for ontology edits, so there is one path to Kafka and one ordering guarantee.
- Listeners are idempotent: they deduplicate on the event ID.

### 3.5 Flyway migrations (Rule 7)

DATA_MODEL §15 is normative. In summary:
- **Path:** `services/core/src/main/resources/db/migration/<module>/{control,tenant}/V<phase>_<seq>__<desc>.sql`, for example `actions/tenant/V8_001__create_action_tables.sql`. `control/` targets `vigil_control`; `tenant/` fans out to every `vigil_t_*` database under a per-tenant advisory lock.
- **Each module has its own history table** (`flyway_history_<module>`), so modules version independently without out-of-order failures. The `vigil_sec` SQL (P3) lives in `services/warden/sql/{control,tenant}/` and is applied first.
- **Forward-only.** Flyway checksums stop anyone editing a merged migration; fix forward. Destructive changes go expand → backfill → constrain → switch readers → drop, across releases. Never rename in place.
- Every migration that creates a table holding tenant data also runs `ENABLE` and `FORCE ROW LEVEL SECURITY` on it (§8). Migrations run as the owning role, never as an application role.
- **Not Flyway:** the typed object tables that OMS generates per object type (P5.3, A3). OMS applies them from type versions and journals them (DATA_MODEL §3.4).
- Test both paths with Testcontainers: all migrations from an empty database, and the upgrade from the previous release's schema.

### 3.6 Virtual threads

- Set `spring.threads.virtual.enabled=true` for request handling and `@Async` work.
- Virtual threads add no database capacity. The HikariCP pool size bounds DB concurrency; `Semaphore`s, not thread pools, bound outbound calls.
- JEP 491 (JDK 24+) removed pinning on `synchronized`. Still keep monitors away from I/O. A foreign-function downcall into Warden pins its carrier for the length of the call, which is acceptable only because Warden calls are short and CPU-only.
- Pass `PolicyContext` explicitly. If a request-scoped carrier is unavoidable, use `ScopedValue` rather than `ThreadLocal`, and never cache expensive objects in `ThreadLocal`s.

### 3.7 Testing: Testcontainers, JaCoCo, ArchUnit

- Use Testcontainers with Spring Boot's `@ServiceConnection` for Postgres (the PostGIS + pgvector image built from `infra/docker/postgres/`), Kafka (KRaft), OpenSearch, Keycloak, OpenFGA and the Temporal dev server. Pull images from the Vigil mirror by setting `TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX`. Container reuse is off in CI.
- JaCoCo gates: at least 85% line coverage per service, and 100% for `vigil.core.audit`, the Warden adapter, the RLS/DLS compilers, the connector framework and every `connectors/*` package.
- ArchUnit rules run alongside Modulith verification:

```java
@AnalyzeClasses(packages = "vigil")
class ArchitectureRules {
  @ArchTest static final ArchRule noFieldInjection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
  @ArchTest static final ArchRule noStandardStreams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
  @ArchTest static final ArchRule webNeverTouchesPersistence =
      noClasses().that().resideInAPackage("..web..")
          .should().dependOnClassesThat().resideInAPackage("..persistence..");
  @ArchTest static final ArchRule jdbcOnlyInPersistence =
      noClasses().that().resideOutsideOfPackage("..persistence..")
          .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.jdbc.core.JdbcTemplate");
  @ArchTest static final ArchRule repositoriesRequirePolicyContext =            // B1, enforced structurally
      methods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Repository").and().arePublic()
          .should(new ArchCondition<JavaMethod>("take a PolicyContext parameter") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
              boolean ok = method.getRawParameterTypes().stream().anyMatch(t -> t.isEquivalentTo(PolicyContext.class));
              if (!ok) events.add(SimpleConditionEvent.violated(method, method.getFullName() + " lacks a PolicyContext"));
            }
          });
}
```

### 3.8 Forge connectors (P11 framework; P22–P24 sources)

- Every connector implements the P11 SPI: category tags, `is_configured`, rate limits, health, backoff, provenance, and a **terms-of-use record** (P11.5). The framework refuses to enable a connector whose terms status is `BLOCKED`, such as SAFLII without a written agreement (GUARDRAILS §5).
- Credentials are KMS references (P11.2). They never appear in config files, logs or test fixtures.
- Tests replay recorded, **synthetic** fixtures. CI makes no live calls to sources, and connector code is held to 100% coverage.

## 4. Rust: Warden, indexer, traversal, functions host, edge agent

### 4.1 Workspace layout

```
Cargo.toml                    # root workspace (P1): members, [workspace.dependencies], [workspace.lints]
Cargo.lock                    # committed
rust-toolchain.toml           # pinned stable (P1)
deny.toml                     # cargo-deny: ADR-0003 licence policy, advisories, bans, allowed sources
services/warden/crates/       # illustrative split; P3 owns the final layout
  vigil-warden-core/          # pure semantics: evaluate(), propagate(), redaction obligations
  vigil-warden-ffi/           # C ABI cdylib for Java FFM                    (unsafe exception)
  vigil-warden-py/            # PyO3 abi3 wheel built with maturin            (unsafe exception)
  vigil-warden-wasm/          # wasm-bindgen (browser) + wasip1 module (wazero) (unsafe exception)
  vigil-warden-pdp/           # warden-pdp sidecar binary
  vigil-warden-conformance/   # ≥ 500 language-neutral cases run by every binding (P3 Done-when)
services/indexer/  services/traversal/  services/functions-host/  edge/agent/
```

Crates are named `vigil-*` (ARCHITECTURE §13). The policy core is **pure**: no I/O, no clock (the evaluation time arrives inside the `PolicyContext`), no randomness and no global mutable state. That is what lets every binding produce identical decisions and keeps the conformance suite deterministic.

### 4.2 `unsafe` code: forbidden by default

- Every crate root starts with `#![forbid(unsafe_code)]`.
- **Exceptions are FFI crates only:** the three Warden binding crates, and edge-agent adapters that bind C libraries (open62541 for OPC UA, P25.1). Any other crate needs the full process below.
- **Exception process:**
  1. Name the crate and the reason in the phase's §4.1 plan, and record it as an ADR note.
  2. Use `#![deny(unsafe_code)]` at the crate root, and `#[allow(unsafe_code)]` only on the smallest module that needs it.
  3. Every `unsafe` block carries a `// SAFETY:` comment (`clippy::undocumented_unsafe_blocks = "deny"`).
  4. The Rust lead **and** the security lead both review it.
  5. The crate's pure-Rust tests run under Miri nightly, and its exported surface is fuzzed in P34.

### 4.3 Errors, panics and lints

- **Libraries use `thiserror`** (one error enum per crate boundary; no `Box<dyn Error>` in public APIs). **Binaries use `anyhow`** at the top level (`main`, CLI commands), with `.context("…")` on every fallible step.
- No `unwrap()`, `expect()`, `panic!`, `todo!` or `unimplemented!` outside tests. Set these as `deny` in `[workspace.lints.clippy]`, and allow them in tests with `allow-unwrap-in-tests` / `allow-expect-in-tests` in `clippy.toml`.
- `vigil-warden-core` also denies `clippy::arithmetic_side_effects`: policy arithmetic uses checked operations.
- Log with `tracing` exported through OpenTelemetry, never with values (B5).

```bash
cargo fmt --all --check
cargo clippy --workspace --all-targets --all-features --locked -- -D warnings
cargo deny check                                   # licences (ADR-0003), advisories, bans, sources
cargo test --workspace --locked
cargo llvm-cov --workspace --fail-under-lines 85   # vigil-warden-* crates: 100%
```

### 4.4 Warden property tests (`proptest`)

These properties MUST hold, and the P3 suite encodes each one:

| Property | Statement |
|---|---|
| Conjunctive markings | Access is allowed on the marking dimension if and only if the user holds **every** marking on the resource. |
| Monotonicity | Adding a marking to a resource never turns a deny into an allow. |
| Classification dominance | The resource's classification must be at or below the user's clearance. |
| Disjunctive releasability | Releasability is satisfied when *any* of the resource's release-to groups matches the context. |
| Propagation is a union | `propagate()` is commutative, associative and idempotent, and never drops an input marking. |
| Redaction completeness | Every property whose markings exceed the context appears in `Decision.redactions`. |
| Fail closed | Malformed or unknown input yields deny, never an error that callers might treat as allow. |
| Determinism | Identical inputs give identical decisions in every binding (differential fuzzing, P34.4). |

```rust
proptest! {
    #![proptest_config(ProptestConfig { cases: 1024, ..ProptestConfig::default() })] // nightly job: 100_000

    #[test]
    fn allow_iff_user_holds_all_markings(user in marking_set(), resource in marking_set()) {
        let decision = evaluate(&ctx_with_markings(&user), &resource);
        prop_assert_eq!(decision.is_allow(), resource.is_subset(&user));
    }

    #[test]
    fn adding_a_marking_never_grants(user in marking_set(), resource in marking_set(), extra in marking()) {
        let before = evaluate(&ctx_with_markings(&user), &resource).is_allow();
        let after = evaluate(&ctx_with_markings(&user), &resource.with(extra)).is_allow();
        prop_assert!(!after || before);
    }

    #[test]
    fn propagate_is_a_union(a in marking_set(), b in marking_set()) {
        prop_assert_eq!(propagate(&[a.clone(), b.clone()]), propagate(&[b.clone(), a.clone()]));
        prop_assert!(a.is_subset(&propagate(&[a.clone(), b.clone()])));
        prop_assert_eq!(propagate(&[a.clone(), a.clone()]), a);
    }
}
```

Commit the `proptest-regressions/` files, so every shrunk failure is replayed forever.

### 4.5 FFI and binding rules

| Binding | Used by | Mechanism | Rules |
|---|---|---|---|
| Java | core, Forge | `cdylib` with a C ABI, called through the FFM API (`java.lang.foreign`) | Requests and decisions cross as Protobuf bytes (`PolicyContext`, `MarkingSet`, `Decision`). Rust allocates the output and exports `vigil_warden_free(ptr, len)`. Java uses a confined `Arena` and never keeps a Rust pointer after the call. No callbacks into Java. |
| Python | Mind, transforms | PyO3 + maturin, abi3 wheels | Release the GIL during evaluation (`allow_threads`); raise a typed `WardenError`; store no Python objects in Rust statics. |
| TypeScript | Cockpit | wasm-bindgen (`wasm32-unknown-unknown`) | Display-side use only (badges, pre-flight hints). The server remains authoritative. |
| Go | Relay | wazero loading a core module (`wasm32-wasip1`) with a `(ptr, len)` Protobuf ABI | Grant no WASI filesystem or network imports; cap memory; pool instances. |
| Rust services | indexer, traversal | the `vigil-warden-core` crate, linked natively | No WASM; same conformance suite. |
| Wasmtime hosts | functions host | WASM component (`vigil:policy` WIT world, P2.4) | Fuel and epoch limits. |

**No panic may cross an FFI boundary.** Every exported function body runs inside `std::panic::catch_unwind`, and a caught panic returns a **deny** decision with code `WARDEN_INTERNAL`, never a partial result. `extern "C-unwind"` is not used. Hosts map WASM traps to deny. FFI crates keep `panic = "unwind"` (which `catch_unwind` needs); binaries such as the agent and the PDP may set `panic = "abort"`.

```rust
#[unsafe(no_mangle)] // Rust 2024 edition
pub extern "C" fn vigil_warden_evaluate(req_ptr: *const u8, req_len: usize, out: *mut OutBuffer) -> i32 {
    let outcome = std::panic::catch_unwind(|| {
        // SAFETY: the caller guarantees req_ptr points to req_len readable bytes for the duration of this call.
        let request = unsafe { std::slice::from_raw_parts(req_ptr, req_len) };
        evaluate_encoded(request) // Result<Vec<u8>, WardenError>; pure, no I/O
    });
    match outcome {
        Ok(Ok(decision)) => write_out(out, decision, STATUS_OK),
        Ok(Err(err)) => write_out(out, deny_encoded(err.code()), STATUS_DENY),
        Err(_panic) => write_out(out, deny_encoded(ErrorCode::WardenInternal), STATUS_DENY),
    }
}
```

## 5. Go: Relay (bundler, hub, spoke, `vigil` CLI)

### 5.1 Module layout

- One Go module, rooted at `services/relay/`, joined to the repository's `go.work` (P1). **The module path is fixed in P1** once the remote exists (ARCHITECTURE §13, AI_COMMUNICATION MSG-011). Until then, documents write `<module-root>` and nobody hard-codes a hosting path.

```
services/relay/
├── go.mod · go.sum            # committed
├── cmd/vigil/                 # CLI: vigil install|upgrade|status|bundle (P20)
├── bundler/                   # Zarf bundle build, sign and verify (P20)
├── internal/common/           # shared helpers (P20); importable only under services/relay/
├── hub/  hub/cmd/relay-hub/   # orchestration engine (P21)
└── spoke/ spoke/cmd/relay-spoke/  # spoke agent (P21)
```

### 5.2 Rules

- **`context` everywhere.** `ctx context.Context` is the first parameter of every function that does I/O or may block. Never store it in a struct. Use `context.Background()` only in `main` and tests. Every outbound call has a deadline, and loops honour `ctx.Done()`.
- **Error wrapping.** Return errors, don't panic. Wrap with the operation and `%w` (`fmt.Errorf("verify bundle %q: %w", name, err)`). Check sentinels with `errors.Is` and typed errors with `errors.As`; never compare error strings. Log once, at the boundary.
- **Concurrency.** Use `errgroup.WithContext` for fan-out. Every goroutine has an owner and an exit path. Tests run with `-race`.
- **Signatures first.** Verify signatures and SBOM before applying anything. Unsigned or foreign-signed artefacts are rejected (P20.3).
- Log with `log/slog` (JSON handler) and export through OpenTelemetry. Evaluate policy through the Warden wazero binding (§4.5).
- **Lint and scan:** `gofmt`/`goimports`, and `golangci-lint run` with at least `errcheck`, `govet`, `staticcheck`, `gosec`, `errorlint`, `contextcheck`, `noctx`, `bodyclose` and `revive`. Run `govulncheck ./...` in `security-scan.yml`.
- **Tests:** `go test ./... -race -covermode=atomic -coverprofile=cover.out`, with at least 85% coverage, and 100% for signature verification and plan-constraint evaluation.

```go
func TestVerifyBundle(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name    string
		bundle  string
		wantErr error
	}{
		{"signed, SBOM present", "testdata/ok.tar.zst", nil},
		{"unsigned image", "testdata/unsigned.tar.zst", ErrUnsigned},
		{"signed by an untrusted key", "testdata/foreign-key.tar.zst", ErrUntrustedKey},
	}
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			ctx, cancel := context.WithTimeout(t.Context(), 30*time.Second)
			defer cancel()
			if err := VerifyBundle(ctx, tc.bundle, testTrustRoot(t)); !errors.Is(err, tc.wantErr) {
				t.Fatalf("VerifyBundle() error = %v, want %v", err, tc.wantErr)
			}
		})
	}
}
```

## 6. Kotlin: the Vigil Field Android app (P27 only)

- Jetpack Compose, with Kotlin Multiplatform for shared domain and sync code. Gradle Kotlin DSL; `gradle.lockfile` committed.
- ktlint (through Spotless), detekt and Android Lint, all with warnings as errors.
- **The app never makes an access decision.** Data arrives already filtered by markings at sync time; the UI renders the markings and redactions it was given.
- The offline object store is encrypted at rest with AES-256-GCM, with keys in the Android Keystore (StrongBox where the device has it). Remote revocation wipes the store and the keys (INCIDENT_RESPONSE runbook R7).
- **Queued Actions** are signed with a device-bound key and carry an idempotency key plus the purpose and case. The server applies or rejects each one by Action semantics. The client never merges (ARCHITECTURE §6.5).
- Use structured coroutines (`viewModelScope`, `lifecycleScope`) and `Dispatchers.IO` for disk and network. `GlobalScope` is banned.
- The app must work on 3 GB RAM devices, offer a data-saver mode (P27.4), and work without Google Play Services (sovereign and air-gapped fleets).
- Local unit tests plus Compose UI tests; Kover coverage of at least 85%, and 100% for the sync, queue and crypto modules.

## 7. Elixir: the comms gateway (P28, only if its ADR selects Elixir)

- Elixir is adopted **only if** the P28 ADR selects it (ADR-0001). Otherwise the gateway is written in Go and §5 applies.
- One Phoenix service (Channels and Presence) under OTP supervision trees. It talks only Protobuf over NATS, plus the Action and Object APIs (appendix 05 §1.6). It never uses Ecto against any other service's database.
- Gates: `mix format --check-formatted`, `mix credo --strict`, `mix dialyzer`, `mix sobelow`, `mix deps.audit`, and `mix test --cover` (at least 85%; 100% for consent and escalation logic). `mix.lock` is committed.
- Business-owned WhatsApp channels only (P28.2). Consent and opt-in records are kept, and POPIA s69 direct-marketing rules apply (P28.4, SECURITY_AND_COMPLIANCE §2.7).

## 8. SQL and PostgreSQL rules

1. **RLS is always on and FORCED.** Every table that holds tenant data gets `ENABLE ROW LEVEL SECURITY` **and** `FORCE ROW LEVEL SECURITY`, so policies bind the table owner too. A CI check against `pg_class` (`relrowsecurity`, `relforcerowsecurity`) fails any migration that misses either.
2. **Object-table policies are generated, never hand-written** (Warden pushdown compiler P3.6, driven by OMS P5.3). Hand-written policies on control-plane tables need the security lead's review.
3. **`SET LOCAL vigil.policy_ctx`** (the signed token from `warden-pdp`) followed by `SELECT vigil_sec.apply_ctx()` opens every data-access transaction (DATA_MODEL §4.2). Every policy also calls `vigil_sec.ctx_ok()`, which re-verifies the signature, so hand-edited settings return zero rows. A missing or invalid context evaluates to NULL, which denies (Blueprint W11).
4. **No superuser application roles.** Service roles (`vigil_reader`, `vigil_actions`, `vigil_indexer`, `vigil_traversal`, …) have no `SUPERUSER`, `BYPASSRLS`, `CREATEROLE` or `CREATEDB`, and never own tables. `vigil_owner` owns tenant objects and is never used by a running service. The indexer and traversal get explicit, narrow system policies, never bypass. **No role has a `DELETE` policy**: deletes are soft, except through the erasure function (DATA_MODEL §4.5, §6.3).
5. **No cross-service database access:** no shared roles, no `dblink` or `postgres_fdw` into another service's database, and no reads of another Modulith module's schema. Cross-module reads use the module's Java API.
6. **Parameterised SQL only.** Identifiers are composed only by the OMS DDL generator, with `format('%I', …)` or `quote_ident`.
7. Timeouts are set per role: `statement_timeout`, `lock_timeout`, `idle_in_transaction_session_timeout`.
8. Transaction-mode connection pooling is compatible with rule 3. Session-level `SET`, advisory locks held across transactions and `LISTEN` are not.
9. Migrations are forward-only, with expand and contract steps (Rule 7).
10. Columns holding personal information carry their classification in the migration, in the form DATA_MODEL defines, so the special-PI classifier and the retention jobs can find them.

```sql
-- Excerpt of the generated template (DATA_MODEL §4.5; the P3.6 compiler emits the real policies).
ALTER TABLE objects.ot_supplier ENABLE ROW LEVEL SECURITY;
ALTER TABLE objects.ot_supplier FORCE ROW LEVEL SECURITY;      -- binds the owner too
CREATE POLICY p_read ON objects.ot_supplier FOR SELECT TO vigil_reader
  USING ((SELECT vigil_sec.ctx_ok()) AND deleted_at IS NULL
     AND markings <@ (SELECT vigil_sec.ctx_markings())                       -- conjunctive markings
     AND classification <= (SELECT vigil_sec.ctx_clearance())
     AND (releasability = '{}' OR releasability && (SELECT vigil_sec.ctx_releasability()))
     AND (purpose_tags = '{}' OR (SELECT vigil_sec.ctx_purpose()) = ANY (purpose_tags)));
```

The scalar sub-selects make the planner evaluate each context value once per query rather than once per row.

## 9. Cross-cutting conventions

### 9.1 Warden binding usage

- Build the `PolicyContext` **once per request**, through the binding, from the verified token, the declared purpose or case, and any justification reference. Never assemble one by hand.
- Pass it **explicitly** down the stack, and push it into every store: Postgres (`SET LOCAL`), OpenSearch (DLS/FLS compiled by Warden), Iceberg (credentials vended by Polaris for that context), ClickHouse (row policies) and traversal (in-process filtering) (ARCHITECTURE §5).
- Apply **every obligation** in the `Decision` (redactions, audit, justification) before any data leaves the service. An unapplied obligation is a Sev-2 incident (INCIDENT_RESPONSE runbook R2).
- Writes call `propagate()`, so outputs carry the union of their inputs' markings. Removing a marking is a two-person "declassify" Action (dossier §6.2).
- Background jobs run as service principals with a narrow, explicit purpose (for example `DATA_INTEGRATION`) and are audited like users. There is no "god mode" context.
- Cache keys always include the policy-context hash (`pch`, defined in DATA_MODEL §12; ARCHITECTURE §10), and key names never contain personal information.

### 9.2 OpenTelemetry conventions

| Allowed on spans, logs and metrics | Forbidden |
|---|---|
| `service.name` (`vigil-<service>`), `service.version`, `deployment.environment`, `vigil.site` | Property values, object titles, free text |
| `vigil.tenant`, `vigil.purpose`, `vigil.case_id` (when present), `vigil.policy_ctx_hash`, `vigil.request_id` | Prompts and completions (Langfuse keeps them in-boundary, under markings) |
| Object-type and Action-type RIDs, counts, durations, error codes | Tokens, keys, secrets |
| Parameterised statement text | Personal information: names, ID numbers, phone numbers, email addresses, data subjects' IP addresses |

- Export only to the in-boundary OTel Collector (ARCHITECTURE §11). No SaaS exporters.
- No unbounded metric labels (no object or user IDs). Sampling is parent-based, and errors are always kept.
- Logs are JSON at `INFO`. `DEBUG` is never enabled on production data paths.

### 9.3 Configuration and secrets

- Non-secret configuration comes from files or the environment and is validated at startup (pydantic-settings, `@ConfigurationProperties` with validation, typed Go and Rust structs). Invalid configuration fails fast.
- **Secrets are KMS references only** (for example `kms://<scope>/<path>#<version>`; P1/P11.2 fix the scheme). They are resolved at runtime from the in-boundary OpenBao KMS (MPL-2.0; HashiCorp Vault is BSL and never shipped, ADR-0003) and held in memory only, with rotation hooks. Secret values never go in env files, Helm values, images or git (Gitleaks runs in CI).
- Local development uses the dev KMS from `docker-compose.dev.yml`. `.env` files are never committed (INSTRUCTIONS §10 #4–#5).
- **Red-line capabilities have no feature flags** (GUARDRAILS §2). Gated modules are unlocked by Warden grants, never by configuration flags (GUARDRAILS §3).

### 9.4 Health and readiness

| Probe | HTTP path (Actuator equivalent) | Checks | Must not |
|---|---|---|---|
| Liveness | `/healthz/live` (`/actuator/health/liveness`) | The process can serve; the event loop isn't blocked | Check dependencies (that cascades restarts) |
| Readiness | `/healthz/ready` (`/actuator/health/readiness`) | DB reachable, Warden binding loaded, KMS reachable, migrations at the expected version, JWKS cached | Report ready while a fail-closed dependency is down |
| Startup | `/healthz/startup` | Warm-up finished (models loaded, caches primed) | — |

gRPC services implement `grpc.health.v1`. Probes are served on a management port that the gateway doesn't expose, and they reveal no versions or dependency names. On shutdown: fail readiness, drain in-flight work for up to 30 s, then close pools.

### 9.5 Idempotency keys

API_DESIGN §A.2 and §A.9 are normative. In summary:
- Every mutating call (Action submissions, connector triggers, job creation) requires an `Idempotency-Key` of 8–128 characters `[A-Za-z0-9_-]` (UUIDv7 recommended).
- The request hash is SHA-256 over the method, the path and the RFC 8785 canonical body.
- Keys are kept for at least 24 hours in Valkey (`v1:idem:…`, DATA_MODEL §12), and durably in `actions.action_run` (`UNIQUE (submitted_by, idempotency_key)`, `request_sha256`; DATA_MODEL §4.8) for Actions. Valkey is only a cache.
- The same key with the same hash **replays** the original status and body, with `Idempotent-Replayed: true`. The same key with a different hash gets `CONFLICT` (409). A `FAILED` or `REJECTED` run is terminal, so a retry needs a new key.
- Side effects deduplicate end to end: writeback targets on `X-Vigil-Action-Run-Id` (§3.3), Temporal activities on IDs derived from the run, and Kafka consumers on the CloudEvent `id`.

### 9.6 Contract-first (Buf)

```bash
buf lint
buf breaking --against '.git#branch=develop'   # CI gate; no breaking change inside a v1 package (Rule 6)
buf generate                                   # Java, TypeScript, Python, Rust, Go and Kotlin stubs (P2)
```

- The order is fixed: 🔌 INTERFACE message → `proto/` PR reviewed by the consumers → generate → implement. Never edit generated code.
- REST endpoints are appended to `docs/API_DESIGN.md` (append-only) with OpenAPI 3.1 in `proto/openapi/`. Events are documented in AsyncAPI 3 under `docs/asyncapi/`.

### 9.7 Attribution headers (INSTRUCTIONS §8)

| Files | Header |
|---|---|
| Java, Kotlin, TypeScript, Go, Rust | `// Written by <Name> (<AI>) on YYYY-MM-DD — <summary>`, then `// Modified by …` lines |
| Python, YAML, shell, `justfile`, TOML, Dockerfile, Elixir | `# Written by …` / `# Modified by …` |
| SQL | `-- Written by …` |
| Markdown | `<!-- Written by … -->` |
| JSON | An entry in the folder's `ATTRIBUTION.md` |

Append a line for your change; never delete or rewrite earlier lines. Generated files carry the generator's "do not edit" banner; attribution goes on the generator or template instead.

### 9.8 Testing pyramid

| Layer | What it proves | Tools | Runs |
|---|---|---|---|
| Unit | Pure logic; no network, injected clock, fixed seeds | pytest, JUnit 5, `cargo test`, `go test`, Vitest | Every PR |
| Component | One service against real stores | Testcontainers | Every PR |
| Contract | Wire compatibility | `buf breaking`, OpenAPI/AsyncAPI schema tests | Every PR |
| Warden conformance | Identical decisions in every binding (≥ 500 cases) | P3 suite + proptest | Every PR touching Warden or a binding |
| Integration | Services together | `docker-compose.ci.yml` | Every PR (path-aware) |
| E2E | User journeys, including disconnection | Playwright, P33 suites | `run-e2e` label, nightly, `main` |
| Load / chaos | ARCHITECTURE §9 targets; failure modes | k6/Gatling (P32); chaos suites (P33) | Nightly, release |
| Security / AI red team | Pentest findings, injection, exfiltration, marking bypass | P34 suites | Release |
| Red lines | Prohibited capabilities absent or blocked | P43 suite (a manual checklist until then) | Release gate |

- **A bug fix starts with a failing reproduction test** (INSTRUCTIONS §5.4).
- **Never disable tests, linters or scans to get a green build** (INSTRUCTIONS §10 #14). Fix a flaky test within 5 working days, or delete it with a 🐛 BUG entry explaining what coverage was lost. Never skip it silently.
- Fixtures are synthetic. Scanners reject South African ID-number patterns and real phone numbers in test data.

## 10. Local Docker CI and the Quota-Saving Engine (Project Setup skill §4)

When GitHub Actions minutes are limited, or you are offline or air-gapped, run **the same workflows locally in Docker**. Local CI must be green, **with its exit code checked**, before you push (INSTRUCTIONS §2 step 7; §11 item 9).

### 10.1 `act`: run the 7 governed workflows locally

```bash
# 1. PR checks: validate-compliance.py, PR-title convention, WORKLOG updated, no forbidden files
act pull_request -W .github/workflows/pr-checks.yml
# 2. Backend CI: per-language gates, path-aware (§2.7, §3, §4.3, §5.2)
act pull_request -W .github/workflows/ci-backend.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
# 3. Frontend CI: Cockpit gates (FRONTEND_GUIDE §14)
act pull_request -W .github/workflows/ci-frontend.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
# 4. Security scan: Semgrep/Bandit SAST, Gitleaks, Trivy
act pull_request -W .github/workflows/security-scan.yml
# 5. SBOM and licence scan: Syft SBOM plus the ADR-0003 licence policy
act pull_request -W .github/workflows/sbom-license-scan.yml -P ubuntu-latest=catthehacker/ubuntu:act-latest
# 6. Live E2E: the workflow runs only for PRs labelled run-e2e, so supply that label in the event payload
printf '{"pull_request":{"labels":[{"name":"run-e2e"}]}}' > "${TMPDIR:-/tmp}/vigil-act-e2e.json"
act pull_request -W .github/workflows/e2e-live.yml -e "${TMPDIR:-/tmp}/vigil-act-e2e.json" \
    -P ubuntu-latest=catthehacker/ubuntu:act-latest
# 7. Deploy: DRY RUN ONLY (-n). Never deploy from a workstation.
act push -W .github/workflows/deploy.yml -n
```

- `act` never receives real registry, cluster or KMS credentials. If a workflow needs a secret to run at all, pass a dummy value with `--secret-file` pointing to a file **outside** the repository.
- On air-gapped machines, mirror the runner image in Harbor and pass `-P ubuntu-latest=<harbor>/mirror/catthehacker/ubuntu:act-latest`.
- `act` starts containers, so it falls under the monitoring protocol in §10.3.

### 10.2 The `docker-compose.ci.yml` harness

```bash
docker compose -f docker-compose.ci.yml run --rm ci-backend    # backend gates in Docker; the exit code is the job's
docker compose -f docker-compose.ci.yml run --rm ci-frontend   # Cockpit gates
docker compose -f docker-compose.ci.yml run --rm ci-python     # one language on its official image (also ci-java, ci-rust, ci-go)
docker compose -f docker-compose.ci.yml down -v                # remove containers AND volumes; leave nothing behind
just ci                                                        # validator + ci-backend + ci-frontend (justfile)
just ci-clean                                                  # down -v --remove-orphans
```

| Job | What `scripts/ci/run-*.sh` runs today (languages whose project doesn't exist yet are skipped) | Still to add in P1, to match this guide |
|---|---|---|
| `ci-backend` | Python: `ruff check .`, `ruff format --check .`, `pytest --cov-fail-under=85`, plus 100% on `*/connectors/*` and `*/policy/*` · Java: `./gradlew spotlessCheck check test` · Rust: `cargo fmt --check`, `clippy -D warnings`, `cargo test` · Go: `golangci-lint`, `go test` · Kotlin: `./gradlew check` | `mypy --strict` (§2.7); `cargo deny` and `cargo llvm-cov` (§4.3); `go test -race` with coverage (§5.2); JaCoCo gates (§3.7) |
| `ci-frontend` | The pnpm forms of the skill's `npx`/`npm` gates: `prettier --check`, `lint`, `tsc --noEmit`, `test`, `build` (Vigil commits `pnpm-lock.yaml`) | Bundle budgets and axe gates (FRONTEND_GUIDE §11, §14) |

The service containers are tmpfs-backed: PostgreSQL 18 with PostGIS and pgvector, built from `infra/docker/postgres/`, and Valkey (ARCHITECTURE §5), not the skill template's pg16 image. The P0 harness pulls upstream images. Once Harbor exists (P1, P20), every image comes from the Vigil mirror.

### 10.3 Asynchronous Container & Stale Task Monitoring Protocol (skill §4.3)

When running containerised builds or long-running sweeps in the background, the AI agent (and any human) **MUST actively prevent zombie processes and hanging tasks**:

```
[Launch Task / Docker Container]
             │
             ▼
[Poll Container State / Exit Code] ──(Running)──▶ [Check Execution Duration]
             │                                              │
      (Exited Code 0 / Error)                     (Duration > Timeout Max)
             │                                              │
             ▼                                              ▼
   [Parse Logs & Report]                         [Kill Stale Container & Log]
```

**The 5 monitoring rules for AI agents:**

1. **Never fire-and-forget heavy tasks.** Always retain the container ID or task ID when launching a background process.
2. **Periodic log inspection.** Periodically poll container logs (`docker logs --tail 50 <container_id>`) to detect infinite loops or deadlock.
3. **Strict timeout enforcement:**
   - Unit tests / linters: **3 minutes maximum**.
   - Playwright / E2E suites: **10 minutes maximum**.
   - Docker image builds: **15 minutes maximum**.
4. **Stale task reaping.** If a background process exceeds its maximum timeout without output changes, immediately issue:
   ```bash
   docker kill <container_id>
   docker rm -f <container_id>
   ```
5. **Exit code verification.** Never assume success; always inspect the exit code (`docker inspect <container_id> --format='{{.State.ExitCode}}'`).

**Vigil notes on the rules:**
- The limits are per job. A polyglot job that needs more than 3 minutes is split by language or path. Raising a limit needs the lead architect's approval, recorded in `docs/AI_COMMUNICATION.md`.
- A reaped task is a failed task. Record the container ID, elapsed time, last 50 log lines and whether output had stopped changing (hung) or was still moving (too slow) in the PR description, and file a 🐛 BUG for a hang.
- An AI agent that cannot finish a run within these rules stops and reports. It never pushes on an unverified result.

**The canonical implementation is the `just monitor` recipe** (justfile). It checks that it holds a real container (Rule 1), prints the last 50 log lines on every poll (Rule 2), reaps the container with `docker kill` + `docker rm -f` and exits `124` once the limit passes (Rules 3–4), and exits with the container's own exit code (Rule 5):

```bash
CID="$(docker compose -f docker-compose.ci.yml run -d ci-backend)"   # Rule 1: keep the container name/ID
just monitor "$CID" 180 || echo "ci-backend failed or was reaped (exit $?; 124 = reaped)"
# Other limits: just monitor "<e2e-container>" 600 (Playwright/E2E) · just monitor "<build-container>" 900 (image builds)
```

Finish every session with `just ci-clean`, then `docker ps -a --filter "name=ci-"` to confirm nothing is left running.
