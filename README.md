<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Repository README (P0) -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - First mission increment and recovery record -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 — Working local mission system -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Connected investigation workspace -->

# Vigil *(codename)*

**Sovereign operational intelligence for Africa, built by Africans.**

Vigil turns fragmented operational data into a living model of real assets, people, places and events: an **Ontology**. Operators, analysts and AI agents act on that model through governed **Actions**, which **write back** into systems of record. It is constitutional by design, sovereign by default, and built to work when the network doesn't.

> ⚠️ "Vigil" is an internal **codename** until trademark clearance ([ADR-0006](docs/adr/ADR-0006-codename-and-trademark-clearance.md)).

## Status
**Investigation increment 02: connected local implementation.** A Java/PostgreSQL core, Python intelligence service and TypeScript cockpit run together. The investigation workspace connects typed, source-backed objects across missions, with relationship assessments, review history, event chronology and direct navigation into operational work. Synthetic infrastructure-crime and procurement inquiries demonstrate shared records across domains; protection, infrastructure and response retain the evidence-to-outcome mission workflow. The larger P0–P43 platform remains in development.

## Run Vigil

With Docker Desktop running, from the repository root:

```powershell
docker compose -f docker-compose.dev.yml --profile full up -d --build
```

Open **http://127.0.0.1:3100**. The first build downloads dependencies; later starts reuse the images. The UI, core, intelligence service and database bind only to loopback. PostgreSQL retains mission records in the `vigil-mission_mission-db` volume.

The workspace selector exposes three explicitly fictional identities: District operations, Protection services, and an operations reviewer with read-only access. These are demonstration identities, not a production login system. All handoff receipts are manually recorded; no emergency service is contacted.

You can open a mission, preserve original source text, revise an assessment with its reason, generate an evidence-linked plan, record each handoff transition, confirm task results, record the outcome, and export the complete mission with its locally verifiable audit chain. Evidence revisions invalidate older plans. Concurrent changes and duplicate command retries are checked by the server.

Start in **Investigations**. Select the infrastructure-components inquiry to follow stock, depot, access-event and delivery-document claims; the procurement inquiry reuses its supplier and document objects. Switch between Network, Timeline and Objects, inspect original sources, create cited objects, and record or review relationships. Evidence changes flag affected mission assertions for review. See [investigation acceptance](docs/INVESTIGATION_INCREMENT.md) and [ADR-0010](docs/adr/ADR-0010-investigation-workspace.md) for the bounded implementation and next platform work.

The default planner uses a labelled deterministic checklist. Optional local Ollama reasoner and challenger models can be selected in `.env`; see [.env.example](.env.example). No hosted LLM or model download is invoked automatically. See the [runbook](docs/RUNNING_VIGIL.md), [acceptance record](docs/MISSION_INCREMENT.md), and [implementation decision](docs/adr/ADR-0009-first-mission-increment.md).

## Start here
1. **[`INSTRUCTIONS.md`](INSTRUCTIONS.md)**: the compliance contract. Mandatory for every human and AI agent.
2. **[`docs/research/VIGIL_RESEARCH_DOSSIER.md`](docs/research/VIGIL_RESEARCH_DOSSIER.md)**: why Vigil is built this way (decisions D1–D12).
3. **[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)**: the frozen system blueprint.
4. **[`docs/PHASES_AND_TASKS.md`](docs/PHASES_AND_TASKS.md)**: claim a phase before you touch code.

## The platform

| Product | Palantir analogue | Language |
|---|---|---|
| **Warden**: security kernel | Markings, CBAC, Checkpoints, audit | Rust |
| **Core**: Ontology (OMS, Indexer, Object Sets, Actions, Functions) | Ontology | Java 25 + Rust |
| **Forge**: data integration | Data Connection, Pipelines, datasets | Java, Rust, Python |
| **Mind**: AI (gateway, agents/MCP, ER, doc AI) | AIP | Python |
| **Cockpit**: operator applications | Workshop, Object Explorer, Map, Graph | TypeScript |
| **Field**: edge node, Android app, comms | Skykit / mobile | Rust, Kotlin, Elixir |
| **Relay**: sovereign delivery | Apollo | Go |

## Local CI (same gates as GitHub Actions)
```bash
python3 scripts/validate-compliance.py                         # governance & hygiene
docker compose -f docker-compose.ci.yml run --rm ci-backend    # per-language backend gates (path-aware)
docker compose -f docker-compose.ci.yml run --rm ci-frontend   # cockpit gates
docker compose -f docker-compose.ci.yml down -v                # clean up
# or, with act:  act pull_request -W .github/workflows/pr-checks.yml
```

## Documentation map
- **Governance:**
  - [INSTRUCTIONS](INSTRUCTIONS.md)
  - [PHASES_AND_TASKS](docs/PHASES_AND_TASKS.md)
  - [BRANCHING_STRATEGY](docs/BRANCHING_STRATEGY.md)
  - [AI_COMMUNICATION](docs/AI_COMMUNICATION.md)
  - [WORKLOG](WORKLOG.md)
- **Blueprints:**
  - [ARCHITECTURE](docs/ARCHITECTURE.md)
  - [DOMAIN_BLUEPRINT](docs/VIGIL_DOMAIN_BLUEPRINT.md)
  - [DATA_MODEL](docs/DATA_MODEL.md)
  - [API_DESIGN](docs/API_DESIGN.md)
  - [BACKEND_GUIDE](docs/BACKEND_GUIDE.md)
  - [FRONTEND_GUIDE](docs/FRONTEND_GUIDE.md)
  - [ADRs](docs/adr/)
- **Safety:**
  - [SECURITY_AND_COMPLIANCE](docs/SECURITY_AND_COMPLIANCE.md)
  - [SYSTEM_COMPLIANCE_GUARDRAILS](docs/SYSTEM_COMPLIANCE_GUARDRAILS.md)
  - [LAUNCH_CHECKLIST](docs/LAUNCH_CHECKLIST.md)
  - [INCIDENT_RESPONSE](docs/INCIDENT_RESPONSE.md)
  - [reviews](docs/reviews/)
- **Strategy:**
  - [Research dossier](docs/research/VIGIL_RESEARCH_DOSSIER.md)
  - [appendices](docs/research/appendix/)
