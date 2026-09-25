<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Branching, merge and release strategy (P0) -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 — Codex branch compatibility -->

# Vigil — Branching Strategy

> Enforced by `scripts/validate-compliance.py` (locally and in `pr-checks.yml`) and by GitHub branch protection. See INSTRUCTIONS.md §3.

## 1. Branch taxonomy

| Branch | Pattern | Created from | Merges into | Purpose |
|---|---|---|---|---|
| Production | `main` | — | — | Released, deployable code only. Tagged `vX.Y.Z`. **Protected.** |
| Integration | `develop` | `main` (once) | `main`, via `release/*` | Integration of completed phases. **Protected.** |
| Feature | `feature/P<ID>-<slug>` | `develop` | `develop` | Phase work, e.g. `feature/P8-outbox-relay` |
| Bug fix | `bugfix/P<ID>-<slug>` | `develop` | `develop` | Fix inside a phase's territory |
| Fix | `fix/<slug>` | `develop` | `develop` | Small cross-cutting fix approved in AI_COMMUNICATION |
| Chore | `chore/<slug>` | `develop` | `develop` | Tooling and docs only (no product code) |
| Codex workspace | `codex/<slug>` | Current authorised checkout | `develop` | App-managed implementation branch; record scope in an ADR and the work log. No merge or protection bypass. |
| Release | `release/v<X.Y.Z>` | `develop` | `main`, then back into `develop` through a back-merge PR from `main` | Stabilisation, version bump, release notes |
| Hotfix | `hotfix/<slug>` | `main` | `main`, then back into `develop` through a back-merge PR from `main` | Urgent production fix |

Slugs are lowercase kebab-case, 2–6 words. One phase per branch (Rule 5).

## 2. The P0 bootstrap exception (the only direct commit ever made to `main`)

A new repository needs a root commit. At initialisation:

1. `git init -b main`, then commit the **root commit** on `main`: `chore(repo): initialise repository`. It contains only `README.md`, `.gitignore` and `.gitattributes`. Make it **before** running `pre-commit install`, because the validator hook blocks commits on `main`.
2. `git branch develop`, so `develop` starts from the root commit.
3. `git switch -c feature/P0-governance-bootstrap develop`, then commit the full P0 scaffold there.
4. Once the remote exists (P0 task 0.8):
   - push `main`, `develop` and the feature branch;
   - enable branch protection (§5);
   - open the PR `docs(P0): governance bootstrap and founding research` into `develop`.
5. **From then on, direct commits to `main`/`develop` are prohibited forever.**

## 3. Flows

```
feature/P5-oms ──PR──▶ develop ──(release/v0.1.0)──PR──▶ main ──tag v0.1.0
                          ▲                               │
                          └──────── back-merge PR ◀───────┘
hotfix/cve-2027-xxxx ──PR──▶ main ──tag v0.1.1 ──back-merge PR──▶ develop
```

## 4. Merge policy: merge commits only

- **Allowed:** "Create a merge commit" only. **Squash and rebase merges are banned.**
- **Why:**
  - forensic and compliance auditability (who did what, when, with which AI);
  - atomic phase histories that can be reverted as a unit;
  - attribution preserved in history.
- In GitHub settings, disable "Allow squash merging" and "Allow rebase merging".
- **Commit messages** use Conventional Commits with the phase as scope:
  - `feat(P8): …`, `fix(P7): …`, `docs(P0): …`, `chore(repo): …`, `security(P3): …`
  - AI-assisted commits add trailers, for example:
    ```
    Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
    ```

## 5. Branch protection (GitHub settings for `main` and `develop`)

| Setting | Value |
|---|---|
| Require a pull request before merging | ✅, with at least 1 approval on `develop` and at least 2 on `main` |
| Dismiss stale approvals when new commits are pushed | ✅ |
| Require review from code owners | ✅ (once `CODEOWNERS` is added in P1) |
| Require status checks | `pr-checks`, `ci-backend`, `ci-frontend`, `security-scan`, `sbom-license-scan` (and `e2e-live` on `main`) |
| Require branches to be up to date | ✅ |
| Require conversation resolution | ✅ |
| Require signed commits | Recommended (✅ from P1) |
| Allow force pushes / deletions | ❌ / ❌ |
| Restrict who can push | Maintainers only (merges happen through the PR UI) |
| Require linear history | ❌ (incompatible with merge commits) |

> Required checks can be selected only after each workflow has run once, so P0 task 0.8 enables protection after the P0 PR's first run.

## 6. Admin bypass protocol

- Repository **admins** (humans) may bypass protection **themselves**, only in a declared emergency (a production incident per INCIDENT_RESPONSE.md, Sev-1/Sev-2).
- Within 24 hours they must record the bypass in `docs/AI_COMMUNICATION.md` (⚠️ WARNING, with the commit SHA) and open a follow-up PR with tests.
- **AI agents are never authorised to bypass, to merge, or to ask an admin to bypass on their behalf.** An agent that believes a bypass is needed writes the case in AI_COMMUNICATION and stops.

## 7. Versioning, tags and products

- **Platform releases** follow SemVer `vMAJOR.MINOR.PATCH`, tagged on `main` by a human.
- **Relay products** (P20–P21) carry their own versions (`core`, `mind`, `cockpit`, `edge-agent`, …) and move through the channels `dev → canary → stable → lts`.
- **Protobuf packages** are versioned by directory (`v1`, `v2`). Breaking changes need a new major package (Rule 6).

## 8. Housekeeping

- Delete merged feature branches (the merge commit keeps the history).
- **Stale claims:** a branch with no commits for 14 days, whose phase is still claimed, may be taken over through a 🔄 HANDOFF (Rule 9).
- Never rewrite history on any shared branch.
