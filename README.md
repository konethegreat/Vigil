<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Repository README (P0) -->

# Vigil *(codename)*

**Sovereign operational intelligence for Africa, built by Africans.**

Vigil turns fragmented operational data into a living model of real assets, people, places and events: an **Ontology**. Operators, analysts and AI agents act on that model through governed **Actions**, which **write back** into systems of record. It is constitutional by design, sovereign by default, and built to work when the network doesn't.

> ⚠️ "Vigil" is an internal **codename** until trademark clearance ([ADR-0006](docs/adr/ADR-0006-codename-and-trademark-clearance.md)).

## Status
**P0: Governance Bootstrap & Founding Research** 🚀. There is no application code yet, by design (INSTRUCTIONS.md §2, step 5: plan and sign-off before code).

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
