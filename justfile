# Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Command surface for governance checks, local Docker CI, act, the dev stack and the §4.3 container monitor (P0)
# Modified by Kone Tshivhinda (Qoder) on 2026-10-31 — P1 bootstrap, lint and test recipes
#
# Run `just` to list recipes. P1 extends this file (bootstrap, lint, test; AI_COMMUNICATION MSG-003).

set shell := ["bash", "-euo", "pipefail", "-c"]
set positional-arguments := true

compose_ci := "docker compose -f docker-compose.ci.yml"
act_platform := "-P ubuntu-latest=catthehacker/ubuntu:act-latest"

# List the recipes.
default:
    @just --list

# Verify the pinned toolchains and install dependencies from the lockfiles (P1 task 1.1).
bootstrap:
    bash scripts/bootstrap.sh

# Governance and hygiene checks (the same script as the pre-commit hook and pr-checks.yml).
validate:
    python3 scripts/validate-compliance.py

# Fast local linters: ruff, Spotless, Prettier, ESLint, tsc, buf, plus the compliance validator.
lint: validate
    MODE=lint bash scripts/ci/run-backend.sh
    MODE=lint bash scripts/ci/run-frontend.sh

# Fast local tests: pytest, Gradle tests and Vitest.
test:
    MODE=test bash scripts/ci/run-backend.sh
    MODE=test bash scripts/ci/run-frontend.sh

# Full local CI: compliance validator, then backend and frontend gates in Docker.
ci: validate ci-backend ci-frontend

# Backend gates for every language whose project exists (multi-toolchain image).
ci-backend:
    timeout 900 {{ compose_ci }} build ci-backend
    just monitor "$({{ compose_ci }} run -d ci-backend)" 600   # composite of ≤3-min gates (MSG-027)

# Cockpit gates (apps/cockpit); a no-op until P10.
ci-frontend:
    timeout 900 {{ compose_ci }} build ci-frontend
    just monitor "$({{ compose_ci }} run -d ci-frontend)" 600   # composite of ≤3-min gates (MSG-027)

# Remove the CI containers, network and cache volumes.
ci-clean:
    {{ compose_ci }} down -v --remove-orphans

# Run pr-checks.yml locally with act.
act-pr:
    act pull_request -W .github/workflows/pr-checks.yml

# Run ci-backend.yml locally with act.
act-backend:
    act pull_request -W .github/workflows/ci-backend.yml {{ act_platform }}

# Run ci-frontend.yml locally with act.
act-frontend:
    act pull_request -W .github/workflows/ci-frontend.yml {{ act_platform }}

# Start the local dev stack (docker-compose.dev.yml arrives in P1 task 1.3).
dev-up:
    #!/usr/bin/env bash
    set -euo pipefail
    if [ ! -f docker-compose.dev.yml ]; then
      echo "docker-compose.dev.yml does not exist yet: the local dev stack is P1 task 1.3." >&2
      exit 1
    fi
    docker compose -f docker-compose.dev.yml up -d

# Stop the local dev stack.
dev-down:
    #!/usr/bin/env bash
    set -euo pipefail
    if [ ! -f docker-compose.dev.yml ]; then
      echo "docker-compose.dev.yml does not exist yet (P1 task 1.3): nothing to stop."
      exit 0
    fi
    docker compose -f docker-compose.dev.yml down

# Supervise a detached container under skill §4.3 / BACKEND_GUIDE §10.3, e.g.
#   just monitor "$(docker compose -f docker-compose.ci.yml run -d ci-backend)" 180
# TIMEOUT in seconds: 180 tests/linters, 600 E2E, 900 image builds.
monitor CONTAINER TIMEOUT="180" POLL="30":
    #!/usr/bin/env bash
    set -euo pipefail
    container="$1" timeout="$2" poll="$3"
    docker inspect "$container" > /dev/null # rule 1: we must hold a real container ID or name
    start=$SECONDS last=""
    while [ "$(docker inspect --format '{{{{.State.Running}}' "$container")" = "true" ]; do
      tail="$(docker logs --tail 50 "$container" 2>&1)" # rule 2: inspect the logs
      if (( SECONDS - start > timeout )); then # rule 3: hard ceiling
        if [ "$tail" = "$last" ]; then state="hung, no output change"; else state="still producing output"; fi
        printf '%s\n' "$tail"
        echo "!! $container exceeded ${timeout}s ($state): reaping it (rule 4)" >&2
        docker kill "$container" > /dev/null 2>&1 || true
        docker rm -f "$container" > /dev/null 2>&1 || true
        exit 124
      fi
      echo "── $container running for $(( SECONDS - start ))s; last 50 log lines:"
      printf '%s\n' "$tail"
      last="$tail"
      sleep "$poll"
    done
    code="$(docker inspect --format '{{{{.State.ExitCode}}' "$container")" # rule 5: never assume success
    docker logs --tail 50 "$container"
    docker rm -f "$container" > /dev/null # leave nothing behind
    echo "── $container exited with code $code"
    exit "$code"
