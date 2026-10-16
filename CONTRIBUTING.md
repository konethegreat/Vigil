<!-- Written by Kone Tshivhinda (Gemini 3.8 Flash) on 2026-10-16 — Contributor guidelines and workflow standards -->
# Contributing to Vigil

Thank you for your interest in contributing to **Vigil** — sovereign operational intelligence for Africa.

Vigil is governed by high standards of forensic auditability, cryptographic provenance, and code hygiene. Before starting work, please review this guide and the repository's governing contracts.

---

## 1. The Supreme Law: INSTRUCTIONS.md

The supreme rulebook for this repository is **[`INSTRUCTIONS.md`](INSTRUCTIONS.md)**.
Every human contributor and every AI assistant (Gemini, Claude, Codex, Copilot) must comply with its protocols.

Key requirements:
- **No Direct Commits to `main` or `develop`:** All changes must arrive via pull requests.
- **Merge Commits Only:** Squash and rebase merges are strictly disallowed to maintain full forensic traceability.
- **Attribution Headers:** Every modified or newly created code, configuration, or documentation file must retain all preceding attribution headers and append a new line detailing the author, AI model, and change summary.
- **Append-Only Ledgers:** `WORKLOG.md` must have an entry appended before every commit. Never edit or delete historical rows.

---

## 2. Engineering Doctrine

Vigil enforces the **Karpathy Engineering Doctrine** ([`INSTRUCTIONS.md §5`](INSTRUCTIONS.md#5-the-karpathy-engineering-doctrine)):
1. **Think Before Coding:** State assumptions explicitly. Surface architectural tradeoffs. Do not speculate.
2. **Simplicity First:** Write the minimum code required to solve the bounded task. No speculative abstractions or unrequested configurability.
3. **Surgical Changes:** Modify only files inside your claimed territory. Do not perform drive-by refactorings or style reformatting on adjacent code.
4. **Goal-Driven Execution:** Define clear, reproducible verification criteria before coding and verify them before submitting.

---

## 3. Branching Strategy

Our branching taxonomy follows [`docs/BRANCHING_STRATEGY.md`](docs/BRANCHING_STRATEGY.md):

| Branch Type | Name Pattern | Base Branch | Target Branch |
| ----------- | ------------ | ----------- | ------------- |
| Feature     | `feature/P<ID>-<slug>` | `develop`   | `develop`     |
| Bug Fix     | `bugfix/P<ID>-<slug>`  | `develop`   | `develop`     |
| Small Fix   | `fix/<slug>`           | `develop`   | `develop`     |
| Chore/Docs  | `chore/<slug>`         | `develop`   | `develop`     |
| Release     | `release/v<X.Y.Z>`     | `develop`   | `main`        |
| Hotfix      | `hotfix/<slug>`        | `main`      | `main`        |

---

## 4. Local Development & Verification

### Prerequisites
- Docker Desktop with Compose support
- Python 3.11+ (Python 3.13 recommended)
- Java 25 / OpenJDK
- Node.js 22+ & `pnpm` 10+
- `just` (command runner, optional but recommended)

### Running Local Validation
Before pushing any branch or opening a pull request, run the compliance validator:

```powershell
# Fast local check
python scripts/validate-compliance.py

# CI-parity check against develop
python scripts/validate-compliance.py --ci --base develop
```

### Running Local Tests
```powershell
# Cockpit frontend checks
pnpm --dir apps/cockpit typecheck
pnpm --dir apps/cockpit test

# Docker CI containers
docker compose -f docker-compose.ci.yml run --rm ci-backend
docker compose -f docker-compose.ci.yml run --rm ci-frontend
```

---

## 5. Commit & PR Guidelines

### Commit Messages
Follow the Conventional Commits format with an explicit scope matching the phase or `repo`:
```text
<type>(<scope>): <summary>

Co-Authored-By: <Name or AI Model> <email or noreply>
```
Valid types: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `perf`, `build`, `ci`, `security`.

### Pull Request Process
1. Base your branch off `develop`.
2. Fill out all sections of [`.github/pull_request_template.md`](.github/pull_request_template.md), including §4.1 solution evaluation and Karpathy verification steps.
3. Ensure CI checks (`pr-checks`, `security-scan`, `sbom-license-scan`) pass cleanly.
4. Only human maintainers may approve and merge pull requests. AI agents are prohibited from merging.

---

## 6. Code of Conduct

All participants in the Vigil community are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please report any violations to `erictshivhinda@gmail.com`.
