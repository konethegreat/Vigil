#!/usr/bin/env bash
# Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Local frontend CI runner: the ci-frontend.yml gates for apps/cockpit, a no-op until the cockpit exists (P0)
# Modified by Kone Tshivhinda (Qoder) on 2026-10-31 — MODE=lint|test for the P1 `just lint` / `just test` surface
#
# Usage: [MODE=all|lint|test] scripts/ci/run-frontend.sh   (docker-compose.ci.yml ci-frontend, or any machine with Node 24)
# MODE defaults to all, which is what CI runs. lint selects Prettier, ESLint and tsc; test selects
# Vitest. The production build runs only in all mode. Exits 1 if any gate fails. Keep the commands
# identical to .github/workflows/ci-frontend.yml.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

MODE="${MODE:-all}"
case "$MODE" in
  all | lint | test) ;;
  *)
    echo "unknown MODE '$MODE' (expected: all, lint, test)" >&2
    exit 2
    ;;
esac
want_lint() { [ "$MODE" != "test" ]; }
want_test() { [ "$MODE" != "lint" ]; }

APP=apps/cockpit
if [ ! -f "$APP/package.json" ]; then
  echo "SKIP: $APP/package.json does not exist yet (the cockpit arrives in P10). Nothing to check."
  exit 0
fi

if ! command -v pnpm > /dev/null 2>&1; then
  # Bootstrap only: pnpm then switches to the `packageManager` version declared in package.json.
  npm install --global "pnpm@${PNPM_VERSION:-12.6.0}"
fi

SUMMARY=()
FAILED=0

record() { SUMMARY+=("$(printf '%-5s %s' "$1" "$2")"); }

# gate DESCRIPTION COMMAND... : run one check and record PASS or FAIL.
gate() {
  local desc="$1"
  shift
  printf '\n==> [cockpit] %s\n    $ %s\n' "$desc" "$*"
  if "$@"; then
    record PASS "$desc"
  else
    record FAIL "$desc"
    FAILED=1
    return 1
  fi
}

has_a11y_script() {
  node -e 'process.exit(require("./package.json").scripts?.["test:a11y"] ? 0 : 1)'
}

cd "$APP"
if gate "pnpm install --frozen-lockfile" pnpm install --frozen-lockfile; then
  if want_lint; then
    gate "prettier --check" pnpm exec prettier --check . || true
    gate "eslint (pnpm lint)" pnpm lint || true
    gate "tsc --noEmit" pnpm exec tsc --noEmit || true
  fi
  if want_test; then
    gate "vitest (pnpm test)" pnpm test || true
    if has_a11y_script; then
      gate "axe accessibility (pnpm run test:a11y)" pnpm run test:a11y || true
    else
      record SKIP "axe accessibility: no test:a11y script yet (P10 Done-when: 0 serious violations)"
    fi
  fi
  if [ "$MODE" = "all" ]; then
    gate "next build (pnpm build)" pnpm build || true
  fi
fi

LINE="============================================================"
printf '\n%s\nVigil — local frontend CI summary\n%s\n' "$LINE" "$LINE"
printf '%s\n' "${SUMMARY[@]}"
echo "$LINE"
if [ "$FAILED" -eq 1 ]; then
  echo "RESULT: FAIL"
  exit 1
fi
echo "RESULT: PASS"
