<!-- Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Implementation plan for first complete mission -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification -->

# Mission increment 01

**Owner:** Kone Tshivhinda / OpenAI Codex (GPT-6). **Branch:** `codex/mission-foundation`. **Status:** Local implementation and acceptance complete; production platform and remote review remain future work. **Authorisation:** founder instruction to begin implementation after the concept review. **Decision:** ADR-0009.

1. Establish reproducible Java, Python and TypeScript projects and a local Docker profile.
2. Implement durable, permission-scoped missions, evidence, plans, task transitions, outcomes and audit history.
3. Implement a typed intelligence gateway with honest deterministic fallback and optional local multi-model execution.
4. Build a distinctive, responsive mission workspace with source inspection, plan review, task acknowledgments, outcome recording, search and creation.
5. Exercise domain failure paths and permissions, then browser-test the complete journey and document how to run it.

## What works

An operator can create a mission, preserve source originals, assess evidence with a reason, prepare a cited plan, track explicit handoff receipts, verify each task, record an outcome, and export the mission with its audit history. District operations, protection services, and independent review have separate server-enforced permissions. Source assessment changes invalidate earlier plans. Database transactions combine mission changes, idempotency receipts, and append-only audit records.

Java owns durable operational state, Python owns bounded model execution, TypeScript owns the interface and browser gateway, and PostgreSQL enforces transaction-level tenant scope. The model never receives database credentials or action tools. A second model can review a valid analysis; an unavailable reviewer is reported explicitly.

## Acceptance evidence — 25 September 2026

| Check                                         | Observed result                                                                                                                                                                       |
| --------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Java/PostgreSQL integration and session tests | 14 passed, 0 failures/errors; fresh `--rerun-tasks` execution, separate temporary database and baseline Mind service                                                                  |
| Python model and contract tests               | 19 passed; 99% statement coverage; Ruff lint and formatting passed                                                                                                                    |
| Browser gateway tests                         | 8 passed, including origin/host checks, identity isolation, signed HttpOnly sessions, retries and upstream failures                                                                   |
| Static frontend checks                        | ESLint and TypeScript passed                                                                                                                                                          |
| Production packaging                          | Core, Mind and standalone Cockpit Docker images built successfully with lockfiles                                                                                                     |
| Full HTTP acceptance                          | 11 check groups passed after file recovery; completed mission `VGL-4E4787A4`, baseline plan and verified 21-event audit chain                                                         |
| Persistence after container loss/recreation   | Earlier missions `VGL-EAC0C929` and `VGL-97C4DC5A` remain completed with verified 21-event chains                                                                                     |
| Real local reasoning through the full app     | `VGL-0042`: gateway → Core → Mind → installed `llama3.1:latest`; LOCAL_MODEL, 13.2 seconds, no warnings; source-reference validation completed                                        |
| Two-model behavior                            | Mocked success/failure paths passed. Earlier live `qwen3-coder:30b` review was unavailable; successful live two-model review is not claimed                                           |
| Browser interactions                          | Mission creation and source entry; original-source drawer and revision; stale-plan state; scoped workspace switch; authorisation, send, and acceptance receipts on `VGL-620EA2CA`     |
| Responsive interface                          | Desktop visual inspection and 390×844 viewport; no page-width overflow; queue and mission tabs scroll within their containers                                                         |
| Browser errors                                | No console errors observed in the final interaction check                                                                                                                             |
| Recovery                                      | Missing index rebuilt, 38 committed files restored, ten authored files recovered from session patches; source/Git snapshot, commit bundle and database dump saved outside the project |

The local reasoner was enabled in ignored `.env`; the challenger remains unset. The installed Ollama runtime was restarted after it was found stopped. No model weights were downloaded and no hosted-model charges were incurred.

## Review boundaries

This increment does not mark the full P1, P3, P8, P10 or AI phases complete. It uses synthetic identities and records, a Java policy boundary, fixed task templates, hand-maintained HTTP contracts, and manually recorded acknowledgments. Real dispatch, production authentication, attachment ingestion, offline field work, federation, granular field policies, external audit anchoring, and multilingual model evaluation remain unimplemented.

The original full-platform CI harness was not run for this increment: its database environment assumes the future platform schema and its Python discovery assumes a root workspace. The checks above are the reproducible mission-profile gates. No GitHub remote or PR is configured; no remote CI, licence/SBOM/security scan suite, automated axe assessment, or production security assessment is claimed. FastAPI's test client also emits an upstream httpx deprecation warning; runtime tests pass.

The expanded concept is retained in `research/VIGIL_FUTURE_CONCEPT.md`. Run instructions and limitations are in `RUNNING_VIGIL.md`; the implemented API is described in `MISSION_API.md`.
