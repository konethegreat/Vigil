#!/usr/bin/env bash
# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — P1 bootstrap: verify the pinned toolchains and install project dependencies
#
# Checks Docker, JDK 25, Node 24+, pnpm 11, Python 3.13 and uv, then installs the JavaScript
# and Python dependencies from their lockfiles. It never installs toolchains: missing tools are
# reported with the exact version required, so `just bootstrap` is safe to run on any machine.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

FAILED=0
fail() {
  printf 'FAIL: %s\n' "$1" >&2
  FAILED=1
}
ok() { printf '  ok: %s\n' "$1"; }

java_major() {
  "$1" -version 2>&1 | awk -F'"' '/version/ {print $2}' |
    awk -F. '{ if ($1 == "1") print $2; else print $1 }'
}

node_major() { node --version | sed 's/^v//' | cut -d. -f1; }
major_of() { "$1" --version | sed 's/^[^0-9]*//' | cut -d. -f1; }

echo "==> Toolchains"

if command -v docker > /dev/null 2>&1 && docker info > /dev/null 2>&1; then
  ok "docker $(docker --version | awk '{print $3}' | tr -d ',')"
else
  fail "Docker is not installed or the daemon is not running. Start Docker Desktop, then rerun."
fi

JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVA_BIN="${JAVA_BIN:-java}"
if command -v "$JAVA_BIN" > /dev/null 2>&1 && [ "$(java_major "$JAVA_BIN")" = "25" ]; then
  ok "java 25 ($JAVA_BIN)"
else
  fail "JDK 25 is required (the language charter pins Java 25). Install it and set JAVA_HOME to that JDK."
fi

if command -v node > /dev/null 2>&1 && [ "$(node_major)" -ge 24 ]; then
  major="$(node_major)"
  if [ "$major" = "24" ]; then
    ok "node $major"
  else
    ok "node $major (CI runs Node 24 LTS; results may differ)"
  fi
else
  fail "Node 24 LTS or newer is required. Install it from https://nodejs.org."
fi

if command -v pnpm > /dev/null 2>&1 && [ "$(major_of pnpm)" -ge 11 ]; then
  ok "pnpm $(pnpm --version)"
else
  fail "pnpm 11 is required. Run: corepack enable (the version is pinned in package.json)."
fi

if command -v uv > /dev/null 2>&1; then
  ok "uv $(uv --version | awk '{print $2}')"
  PYTHON_BIN=python3
  command -v python3 > /dev/null 2>&1 || PYTHON_BIN=python
  if "$PYTHON_BIN" -c "import sys; raise SystemExit(0 if sys.version_info[:2] == (3, 13) else 1)" 2> /dev/null; then
    ok "python 3.13"
  else
    echo "  note: uv will provision Python 3.13 from .python-version when it syncs."
  fi
else
  fail "uv is required for the Mind service. Install it from https://docs.astral.sh/uv/getting-started/installation/."
fi

if [ "$FAILED" -eq 1 ]; then
  echo "Bootstrap stopped: fix the FAIL lines above, then rerun 'just bootstrap'." >&2
  exit 1
fi

echo "==> JavaScript dependencies"
pnpm install --frozen-lockfile

echo "==> Python dependencies (services/mind)"
(cd services/mind && uv sync --locked)

echo
echo "Bootstrap complete."
echo "Next: 'just dev-up' starts the local stack, then 'just lint', 'just test' or 'just ci'."
