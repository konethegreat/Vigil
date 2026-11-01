#!/usr/bin/env bash
# Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Local backend CI runner: the ci-backend.yml gates for every language whose project exists (P0)
# Modified by Kone Tshivhinda (Qoder) on 2026-10-31 — MODE=lint|test for the P1 `just lint` / `just test` surface
# Modified by Kone Tshivhinda (Qoder) on 2026-10-31 — Add the mypy --strict and branch-coverage MUST gates (P1 task 1.7)
# Modified by Kone Tshivhinda (Qoder) on 2026-10-31 — Verify golden-path templates (P1 task 1.4)
# Modified by Kone Tshivhinda (Qoder) on 2026-11-01 — Run the Java templates through the same gate (P1 task 1.4)
# Modified by Kone Tshivhinda (Qoder) on 2026-11-01 — Run the TypeScript templates through the same gate (P1 task 1.4)
# Modified by Kone Tshivhinda (Qoder) on 2026-11-01 — Run the Rust templates through the same gate (P1 task 1.4)
#
# Usage: [MODE=all|lint|test] scripts/ci/run-backend.sh [all | python | java | rust | go | kotlin | elixir | proto | templates ...]
# MODE defaults to all, which is what CI runs. lint selects formatters, linters and static checks;
# test selects the test suites. The commands are the same in either mode, so `just lint` and
# `just test` cannot drift from ci-backend.yml.
# Runs inside docker-compose.ci.yml (ci-backend, ci-<language>) or on any machine with the toolchains.
# A language is SKIPPED when its project does not exist yet, or (with a warning) when its toolchain
# is not installed here; GitHub Actions still runs that gate. Exits 1 if any gate fails.
# Keep the commands identical to .github/workflows/ci-backend.yml.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

: "${GOLANGCI_LINT_VERSION:=v2.14.0}" # keep in step with ci-backend.yml

MODE="${MODE:-all}"
case "$MODE" in
  all | lint | test) ;;
  *)
    echo "unknown MODE '$MODE' (expected: all, lint, test)" >&2
    exit 2
    ;;
esac
export MODE
want_lint() { [ "$MODE" != "test" ]; }
want_test() { [ "$MODE" != "lint" ]; }

SUMMARY=()
FAILED=0
TOOL_SKIPS=0

record() { SUMMARY+=("$(printf '%-7s %-5s %s' "$1" "$2" "$3")"); }

# gate LANG DESCRIPTION COMMAND... : run one check and record PASS or FAIL.
gate() {
  local lang="$1" desc="$2"
  shift 2
  printf '\n==> [%s] %s\n    $ %s\n' "$lang" "$desc" "$*"
  if "$@"; then
    record "$lang" PASS "$desc"
  else
    record "$lang" FAIL "$desc"
    FAILED=1
    return 1
  fi
}

# have LANG TOOL... : true when every tool is installed; otherwise records a SKIP for LANG.
have() {
  local lang="$1" tool
  shift
  for tool in "$@"; do
    if ! command -v "$tool" > /dev/null 2>&1; then
      record "$lang" SKIP "'$tool' is not installed here (GitHub Actions still runs this gate)"
      TOOL_SKIPS=1
      return 1
    fi
  done
}

in_dir() {
  local dir="$1"
  shift
  (cd "$dir" && "$@")
}

# Source files, excluding dependency and build directories.
find_sources() {
  find . \( -name .git -o -name node_modules -o -name vendor -o -name target -o -name .venv \) -prune \
    -o "$@" -print
}

python_gates() {
  local dir=""
  local source_target="src"
  if [ -f pyproject.toml ]; then
    dir="."
  elif [ -f services/mind/pyproject.toml ]; then
    dir="services/mind" # the Mind service owns its uv project until a root workspace exists (P1)
    source_target="vigil_mind"
  else
    record python SKIP "no pyproject.toml workspace and no services/mind/pyproject.toml"
    return 0
  fi
  have python uv || return 0
  gate python "uv sync --locked ($dir)" in_dir "$dir" uv sync --locked --all-packages || return 0
  if want_lint; then
    gate python "ruff check ($dir)" in_dir "$dir" uv run ruff check . || true
    gate python "ruff format --check ($dir)" in_dir "$dir" uv run ruff format --check . || true
    gate python "mypy --strict $source_target ($dir)" \
      in_dir "$dir" uv run mypy --strict "$source_target" || true
  fi
  if want_test; then
    gate python "pytest (branch coverage >= 85%, $dir)" \
      in_dir "$dir" uv run pytest --cov="$source_target" --cov-branch \
      --cov-report=term-missing --cov-fail-under=85 || true
    if [ -n "$(find_sources \( -path '*/connectors/*.py' -o -path '*/policy/*.py' \) | head -n 1)" ]; then
      gate python "100% coverage: connectors/ and policy/ ($dir)" \
        in_dir "$dir" uv run coverage report --include='*/connectors/*,*/policy/*' --fail-under=100 || true
    fi
  fi
}

templates_gates() {
  local status=0
  if [ ! -d templates/golden-paths/python ] && [ ! -f templates/golden-paths/java/settings.gradle.kts ] \
    && [ ! -d templates/golden-paths/typescript ] && [ ! -f templates/golden-paths/rust/Cargo.toml ]; then
    record templates SKIP "no golden-path templates yet (P1 task 1.4)"
    return 0
  fi
  printf '\n==> [templates] golden-path templates (MODE=%s)\n    $ bash scripts/ci/run-templates.sh\n' "$MODE"
  bash scripts/ci/run-templates.sh || status=$?
  case "$status" in
    0) record templates PASS "golden-path templates (MODE=$MODE)" ;;
    3)
      record templates SKIP "a template toolchain is missing here (GitHub Actions still runs it)"
      TOOL_SKIPS=1
      ;;
    *)
      record templates FAIL "golden-path templates (MODE=$MODE)"
      FAILED=1
      ;;
  esac
}

java_gates() {
  [ -f gradlew ] || { record java SKIP "no Gradle build yet (P1)"; return 0; }
  have java java || return 0
  case "$MODE" in
    all) gate java "gradlew spotlessCheck check test" ./gradlew --no-daemon spotlessCheck check test || true ;;
    lint) gate java "gradlew spotlessCheck" ./gradlew --no-daemon spotlessCheck || true ;;
    test) gate java "gradlew check test" ./gradlew --no-daemon check test || true ;;
  esac
}

rust_gates() {
  [ -f Cargo.toml ] || { record rust SKIP "no Cargo workspace yet (P3)"; return 0; }
  have rust cargo || return 0
  if ! cargo fmt --version > /dev/null 2>&1 || ! cargo clippy --version > /dev/null 2>&1; then
    rustup component add rustfmt clippy || true # the official rust image ships a minimal profile
  fi
  if want_lint; then
    gate rust "cargo fmt --check" cargo fmt --all --check || true
    gate rust "cargo clippy -D warnings" \
      cargo clippy --workspace --all-targets --locked -- -D warnings || true
    cargo deny --version > /dev/null 2>&1 || cargo install cargo-deny --locked || true
    gate rust "cargo deny check" cargo deny check || true
  fi
  if want_test; then
    gate rust "cargo test" cargo test --workspace --locked || true
    rustup component add llvm-tools-preview || true
    cargo llvm-cov --version > /dev/null 2>&1 || cargo install cargo-llvm-cov --locked || true
    gate rust "cargo llvm-cov (lines >= 85%)" \
      cargo llvm-cov --workspace --fail-under-lines 85 || true
  fi
}

gofmt_clean() {
  local unformatted
  unformatted="$(find_sources -name '*.go' | tr '\n' '\0' | xargs -0 -r gofmt -l)"
  [ -z "$unformatted" ] || { printf 'gofmt -w needed for:\n%s\n' "$unformatted"; return 1; }
}

go_test_race_coverage() {
  local profile total
  profile="$(mktemp)"
  if ! go test ./... -race -covermode=atomic -coverprofile="$profile"; then
    rm -f "$profile"
    return 1
  fi
  total="$(go tool cover -func="$profile" | awk '/^total:/ {gsub(/%/, "", $3); print $3}')"
  rm -f "$profile"
  [ -n "$total" ] || { echo "coverage total was not reported" >&2; return 1; }
  echo "total coverage: ${total}%"
  awk -v total="$total" 'BEGIN { exit (total + 0 < 85) }'
}

go_gates() {
  local modules module dir
  modules="$(find_sources -name go.mod | sort)"
  [ -n "$modules" ] || { record go SKIP "no Go modules yet (P20)"; return 0; }
  have go go || return 0
  if ! command -v golangci-lint > /dev/null 2>&1; then
    go install "github.com/golangci/golangci-lint/v2/cmd/golangci-lint@${GOLANGCI_LINT_VERSION}" || true
    PATH="$(go env GOPATH)/bin:$PATH"
  fi
  if want_lint; then
    gate go "gofmt" gofmt_clean || true
  fi
  while IFS= read -r module; do
    dir="$(dirname "$module")"
    if want_lint; then
      gate go "golangci-lint ($dir)" in_dir "$dir" golangci-lint run ./... || true
    fi
    if want_test; then
      gate go "go test -race ./... (coverage >= 85%, $dir)" \
        in_dir "$dir" go_test_race_coverage || true
    fi
  done <<< "$modules"
}

kotlin_gates() {
  [ -f edge/field-app/gradlew ] || { record kotlin SKIP "no edge/field-app yet (P27)"; return 0; }
  if [ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]; then
    record kotlin SKIP "Android SDK is not installed here (GitHub Actions still runs this gate)"
    TOOL_SKIPS=1
    return 0
  fi
  have kotlin java || return 0
  if want_test; then
    gate kotlin "gradlew check (edge/field-app)" in_dir edge/field-app ./gradlew --no-daemon check || true
  else
    record kotlin SKIP "no separate Kotlin lint gate; gradlew check runs in test mode"
  fi
}

elixir_gates() {
  [ -f services/comms/mix.exs ] || { record elixir SKIP "no services/comms mix project (P28)"; return 0; }
  have elixir mix || return 0
  gate elixir "mix deps.get" in_dir services/comms env MIX_ENV=test mix deps.get || return 0
  if want_lint; then
    gate elixir "mix format --check-formatted" \
      in_dir services/comms env MIX_ENV=test mix format --check-formatted || true
    gate elixir "mix credo --strict" in_dir services/comms env MIX_ENV=test mix credo --strict || true
  fi
  if want_test; then
    gate elixir "mix test" in_dir services/comms env MIX_ENV=test mix test || true
  fi
}

proto_gates() {
  [ -f proto/buf.yaml ] || { record proto SKIP "no proto/buf.yaml yet (P2)"; return 0; }
  if ! want_lint; then
    record proto SKIP "no proto tests; buf lint and buf breaking run in lint mode"
    return 0
  fi
  have proto buf git || return 0
  gate proto "buf lint" buf lint proto || true
  if git rev-parse --verify --quiet refs/heads/develop > /dev/null \
    && git cat-file -e develop:proto/buf.yaml 2> /dev/null; then
    gate proto "buf breaking (against develop)" \
      buf breaking proto --against ".git#branch=develop,subdir=proto" || true
  else
    record proto SKIP "buf breaking: no local develop branch with proto/buf.yaml to compare against"
  fi
}

[ "$#" -gt 0 ] || set -- all
for language in "$@"; do
  case "$language" in
    all) python_gates; templates_gates; java_gates; rust_gates; go_gates; kotlin_gates; elixir_gates; proto_gates ;;
    python) python_gates ;;
    templates) templates_gates ;;
    java) java_gates ;;
    rust) rust_gates ;;
    go) go_gates ;;
    kotlin) kotlin_gates ;;
    elixir) elixir_gates ;;
    proto) proto_gates ;;
    *) echo "unknown language '$language' (expected: all python java rust go kotlin elixir proto templates)" >&2; exit 2 ;;
  esac
done

LINE="============================================================"
printf '\n%s\nVigil — local backend CI summary\n%s\n' "$LINE" "$LINE"
printf '%s\n' "${SUMMARY[@]}"
echo "$LINE"
if [ "$TOOL_SKIPS" -eq 1 ]; then
  echo "WARNING: some present projects were skipped because their toolchain is missing here."
fi
if [ "$FAILED" -eq 1 ]; then
  echo "RESULT: FAIL"
  exit 1
fi
echo "RESULT: PASS"
