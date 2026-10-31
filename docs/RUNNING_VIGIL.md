<!-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 — Mission environment runbook -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Investigation workflow and forward migration -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 - Source intake, extraction and review -->
<!-- Modified by Kone Tshivhinda (Qoder) on 2026-10-31 - P1 platform services and dev-up -->
<!-- Modified by Kone Tshivhinda (Qoder) on 2026-10-31 - Document the mypy and coverage gates -->
<!-- Modified by Kone Tshivhinda (Qoder) on 2026-10-31 - Document the golden-path template gate -->


# Running Vigil locally

## Start the complete system

Run Docker Desktop with Linux containers. From the repository root:

```powershell
just dev-up          # mission services plus the platform profile
just dev-up-full     # the above plus the Cockpit demo UI
```

`just dev-up-full` is equivalent to:

```powershell
docker compose -f docker-compose.dev.yml --profile full --profile platform up -d --build
```

Open http://127.0.0.1:3100. Ports 3100, 8087, 8017 and 5547 must be free. Every published port binds to `127.0.0.1`. Other projects' Docker services are unaffected: this environment is named `vigil-mission`.

| Component     | Purpose                                                          | Local address                       |
| ------------- | ---------------------------------------------------------------- | ----------------------------------- |
| Cockpit       | Production Next.js server and browser gateway                    | http://127.0.0.1:3100               |
| Core          | Java 25 / Spring Boot mission transactions                       | http://127.0.0.1:8087/api/v1/health |
| Mind          | Python / FastAPI bounded analysis                                | http://127.0.0.1:8017/health        |
| PostgreSQL 18 | Missions, original evidence, commands, audit and access receipts | 127.0.0.1:5547                      |

The PostgreSQL image is built from `infra/docker/postgres` and carries PostGIS and pgvector, so local development matches the CI database.

The browser holds a signed demonstration session in an HttpOnly, SameSite Strict cookie. It never receives the internal service token. Core derives tenant and purpose from the signed identity, checks operation policy and scopes transactions through PostgreSQL row-level security. All seven seed missions and all personas are fictional. The public local credentials in Compose are deliberately throwaway; this profile is for synthetic records.

Use the workspace selector to explore District operations, Protection services, and Independent review. Independent review sees operations records but cannot mutate them. The protection workspace contains only its own missions.

## Platform services

The `platform` profile starts the supporting components that later phases consume. It is synthetic-only and uses throwaway credentials; never point it at real data.

| Component       | Purpose                                    | Local address                                                       | Credentials                                |
| --------------- | ------------------------------------------ | ------------------------------------------------------------------- | ------------------------------------------ |
| Valkey 9        | Sessions, rate limits, object-set cache    | `127.0.0.1:6381`                                                    | none                                       |
| Kafka 4 (KRaft) | Core durable event log                     | `127.0.0.1:9094` (external listener)                                | none                                       |
| NATS 2          | Edge/control messaging with JetStream      | `127.0.0.1:4222`, monitoring `8222`                                 | none                                       |
| OpenSearch 3    | Full-text search and filters               | http://127.0.0.1:9200                                              | security plugin disabled (synthetic only)  |
| SeaweedFS 4     | S3-compatible object store                 | http://127.0.0.1:8333 (S3), `9333` (master), `8888` (filer)        | none                                       |
| Polaris 1       | Iceberg REST catalog                       | http://127.0.0.1:8181 (API), http://127.0.0.1:8182 (health)        | `root` / `local-synthetic-root-secret`     |
| Keycloak 26     | OIDC identity                              | http://127.0.0.1:8081                                              | `vigil-admin` / `local-synthetic-admin-only` |
| OpenFGA 1       | ReBAC authorisation                        | http://127.0.0.1:8082 (HTTP), `8083` (gRPC)                        | none; in-memory store                      |
| Temporal 1      | Action sagas and approvals                 | `127.0.0.1:7233`, UI http://127.0.0.1:8233                         | none                                       |
| OpenBao 2       | Dev KMS                                    | http://127.0.0.1:8200                                              | root token `local-synthetic-root-token`    |
| ClickHouse 26   | Telemetry and audit analytics              | http://127.0.0.1:8123, native `9000`                                | `default`, no password                     |

`docker compose -f docker-compose.dev.yml --profile platform ps` shows the state and `just dev-down` stops the project. Data lives in named volumes, except Keycloak, OpenFGA, Polaris and OpenBao, which restart empty on purpose. Images use exact patch tags; digest pinning and the Harbor mirror arrive with P1 task 1.6 and P20.

## First mission journey

The application opens in **Investigations**. Follow **Trace the missing infrastructure components.** through its shared stock, location, event, supplier and delivery-document records. Select **Reconcile the procurement and receiving record.** to see two of those objects reused with a separate procurement source. Network, Timeline and Objects share the same records. The inspector opens preserved source material and the originating operational mission. Add object and Add relationship require existing mission evidence; Review adds a new assessment receipt.

To follow the original operational workflow, choose **Missions**:

1. Open **Restore the corridor. Preserve the evidence.** in District operations.
2. Inspect the Evidence register. The source clock and field report disagree; originals remain visible.
3. Prepare a plan in Options. Inspect hypotheses, citations, missing evidence, and the execution trace.
4. Revise a source assessment with a reason. The old plan becomes stale. Refresh it before authorising further work.
5. In Operations, record the progression: proposed → authorised → sent → accepted → in progress → confirmed. Each step requires a receipt. Declines and unknown outcomes remain explicit.
6. Once every task has a confirmed result and the plan is current, record the outcome and its verification.
7. Export the JSON mission record. Decision history verifies the local hash chain and makes its lack of external anchoring explicit.

This prototype records simulated acknowledgments. It does not dispatch a responder, send a message, or establish that a real-world service was delivered.

## Source review journey

1. Open **Sources** and choose an active mission. Use **Add source** to paste text or import a UTF-8 `.txt`/`.md` file. Give the source a custodian and keep its assessment separate from the original.
2. Inspect the preserved original, then select **Extract proposals**. An available local reasoner is required; absence or failure is reported explicitly.
3. Compare each proposal with its quoted source. Select the objects to publish, then any supported relationships between them. Removing an endpoint also clears its selected relationships.
4. Record the review note. **Publish selected records** writes the selected records and decision together; **Reject all proposals** records an empty selection. The decision is final and rejected proposals remain inspectable.
5. Use **Open investigations** to inspect the published records. Relationships begin as reported, and the original source assessment does not become verified simply because a model extracted it.

Source edits invalidate pending proposals against the older evidence revision. A stale proposal remains readable; request a fresh extraction against the current revision. Independent review can read originals and decisions but cannot extract or publish.

## Local AI

Ollama is optional. If installed but stopped, start the Ollama application or run `ollama serve` locally. Inspect its installed models with `ollama list`. Copy `.env.example` to `.env` if needed and choose exact names already installed, for example:

```dotenv
VIGIL_REASONER_MODEL=llama3.1:latest
VIGIL_CHALLENGER_MODEL=
```

Apply a model change with:

```powershell
docker compose -f docker-compose.dev.yml up -d mind
```

Mind reaches the host through `host.docker.internal:11434`. It accepts only local Ollama destinations, sends an authorised source snapshot, requests bounded JSON, checks referenced source IDs, and gives the model no action tools or database credentials. The optional challenger has its own model setting and output validation. A schema-valid citation is not proof that the source supports a statement; operator assessment remains necessary.

Each call has a 90-second provider timeout. Mission planning uses a 768-token output cap; source extraction uses 1,800 tokens. Core allows 210 seconds for both roles; the browser gateway allows 220 seconds. Cold starts vary considerably with available memory. A failed mission-planning reasoner produces a labelled baseline checklist. Source extraction instead reports unavailability and saves no fabricated proposal. A failed challenger leaves valid reasoner output visible with review marked unavailable. Without a reasoning model, no model inference is attempted.

Verified on the founding host: `llama3.1:latest` produced a validated local analysis in about 32 seconds in the direct adapter check and 13.2 seconds through the complete browser-gateway/Core/Mind path after recovery. The final local `.env` selects this reasoner. A two-model run retained the valid analysis but `qwen3-coder:30b` review was unavailable; successful live two-model review is not claimed. Mocked two-model success and failure paths are covered by tests. Provider timings are observations, not service guarantees.

To repeat the optional local model check, from `services/mind`:

```powershell
$env:VIGIL_REASONER_MODEL='llama3.1:latest'
uv run python scripts/check_local_models.py
```

## Develop the cockpit

Stop only the packaged cockpit, then use Node and pnpm locally:

```powershell
docker compose -f docker-compose.dev.yml stop cockpit
pnpm install --frozen-lockfile
pnpm dev
```

The browser gateway defaults to the loopback core. Its accepted local origins are `http://127.0.0.1:3100` and `http://localhost:3100`, and the request host must match. `VIGIL_COCKPIT_ORIGIN` configures one explicit alternative if the local development port is intentionally changed.

TypeScript 6.0.3 and ESLint 9.39.5 are pinned to the peer ranges of the current Next.js lint plugins. ESLint 9 is marked deprecated upstream; moving to ESLint 10 requires compatible React/import/accessibility plugins. The runtime image contains the Next.js standalone output, not the development lint toolchain.

## Verify

```powershell
pnpm --dir apps/cockpit lint
pnpm --dir apps/cockpit test
pnpm typecheck
pnpm build
docker compose -f docker-compose.dev.yml --profile checks run --build --rm core-test gradle test spotlessCheck
python scripts/verify-mission.py
python scripts/verify-investigations.py
python scripts/verify-extractions.py
```

From `services/mind`:

```powershell
uv sync --locked
uv run ruff check .
uv run ruff format --check .
uv run mypy --strict vigil_mind
uv run pytest --cov=vigil_mind --cov-branch --cov-report=term-missing --cov-fail-under=85
```

`just lint` and `just test` run these gates together with Spotless and the JaCoCo line-coverage verification (85% minimum, enforced by `check`); `just validate` runs the compliance validator. The golden-path templates under `templates/golden-paths/` are standalone uv projects and run in the same gates; `bash scripts/ci/run-templates.sh` checks them on their own.

The Java tests use a separate temporary PostgreSQL database and a separate Mind service with baseline planning, regardless of the demonstration model settings. Add `--rerun-tasks` after `gradle` when you need to force execution against a recreated test database rather than accept Gradle's up-to-date result. The HTTP acceptance script creates a clearly named synthetic mission in the running operations workspace and leaves the completed record as evidence. It is restricted to localhost. The smoke run is easiest with the default baseline planner; configured local models make it slower. Actual browser coverage and acceptance results are recorded in `MISSION_INCREMENT.md`.

The extraction acceptance requires an available local reasoning model and creates a synthetic source/review mission. Its six groups verify source preservation, proposal replay, rejected invalid publication, atomic publication, read-only access, export and tenant isolation. See [SOURCE_REVIEW_INCREMENT.md](SOURCE_REVIEW_INCREMENT.md) for current results and browser evidence.

These checks cover the local increments. They do not establish the original full-platform CI, a production security assessment, legal admissibility of evidence, or all P0–P43 completion criteria.

## Persistence and stopping

```powershell
docker compose -f docker-compose.dev.yml --profile full stop
```

Stopping preserves the named database volume. Starting again reuses existing missions and seeds only missing demonstration records. Do not remove `vigil-mission_mission-db` if you want to retain work.

For an existing database created before the access-receipt migration was added, apply it once without deleting data:

```powershell
Get-Content infra/docker/mission-db/access.sql -Raw | docker compose -f docker-compose.dev.yml exec -T db psql -U vigil_admin -d vigil -v ON_ERROR_STOP=1
```

Starting the complete system now runs the one-shot `migrate` service before Core. It applies `access.sql`, `investigation.sql` and `extraction.sql` idempotently to the retained database; fresh databases also initialise these tables. No database reset is required. To inspect a migration failure, use `docker compose -f docker-compose.dev.yml logs migrate`. Core will not start through Compose until migration succeeds.

Verify the new investigation workflow with `python scripts/verify-investigations.py`. It exercises the running browser gateway, Core and PostgreSQL using synthetic objects and assertions, including retries, source changes, review history, read-only access and tenant isolation. It leaves an explicitly named acceptance mission and its records for inspection. The existing Java check command also discovers the investigation integration suite. New browser projection tests run alongside gateway tests with `pnpm --dir apps/cockpit test`.

## Implementation boundaries and next work

The first implementation has text-source intake, a fixed three-task mission template, manual acknowledgments, local models, and a compact PostgreSQL JSON read model. It has no production OIDC, granular field markings, Rust/WASM policy kernel, attachment ingestion/OCR, queue-based model jobs, external dispatch, offline field sync, federation, geospatial asset model, multilingual evaluation suite, or automatic resource allocation yet.

The investigation registry adds immutable source-backed objects and reviewed assertions. Its graph is limited to 30 visible nodes; the complete local registry is capped at 500 objects and 1,000 assertions per workspace. Source review publishes selected quoted proposals into that registry; each mission is capped at 100 extraction runs. It is not the full-scale ontology/search service. The next platform work includes typed observations, explicit joint-case evidence release, agency request receipts, controlled dataset ingestion, versioned object corrections, combined investigation export and production identity. See [CONNECTED_OPERATIONS.md](CONNECTED_OPERATIONS.md). Preserve the current source, transaction and outcome contracts as those capabilities arrive.
