#!/usr/bin/env bash
# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Verify every golden-path template (P1 task 1.4)
# Modified by Kone Tshivhinda (Qoder) on 2026-11-01 — Add the Java 25 templates (P1 task 1.4)
# Modified by Kone Tshivhinda (Qoder) on 2026-11-01 — Add the TypeScript templates (P1 task 1.4)
# Modified by Kone Tshivhinda (Qoder) on 2026-11-01 — Add the Rust templates (P1 task 1.4)
#
# Usage: [MODE=all|lint|test] scripts/ci/run-templates.sh
# Each Python template is a standalone uv project, each TypeScript shape is a
# standalone pnpm root, the Java templates are one Gradle build with three
# modules, and the Rust templates are one cargo workspace with two members. The
# script runs the same gates the template READMEs document, so a template cannot
# rot unnoticed.
# Exits 1 if any gate fails, 3 when a language was skipped for a missing
# toolchain and nothing failed, 0 otherwise. GitHub Actions installs every
# toolchain, so it only ever sees 0 or 1; run-backend.sh maps 3 to a SKIP row.
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

# shape=package. Keep this list in step with templates/golden-paths/README.md.
TEMPLATES="service=vigil_service_template library=vigil_library_template job=vigil_job_template"

FAILED=0
SKIPPED=0

if command -v uv > /dev/null 2>&1; then
  for entry in $TEMPLATES; do
    shape="${entry%%=*}"
    package="${entry#*=}"
    project="templates/golden-paths/python/$shape"
    if [ ! -f "$project/pyproject.toml" ]; then
      echo "SKIP: $project is not present"
      continue
    fi
    echo
    echo "==> [templates] $shape ($project)"
    if ! (
      cd "$project"
      uv sync --locked
      if [ "$MODE" != "test" ]; then
        uv run ruff check .
        uv run ruff format --check .
        uv run mypy --strict src
      fi
      if [ "$MODE" != "lint" ]; then
        uv run pytest --cov="$package" --cov-branch \
          --cov-report=term-missing --cov-fail-under=85
      fi
    ); then
      FAILED=1
    fi
  done
else
  echo "SKIP: the Python templates need uv, which is not installed here (GitHub Actions still runs this gate)"
  SKIPPED=1
fi

# Java: one Gradle build with service, library and job modules.
JAVA_PROJECT="templates/golden-paths/java"

# The launcher JVM Gradle will use: JAVA_HOME wins over PATH, as it does for Gradle itself.
java_major() {
  local exe="${JAVA_HOME:+$JAVA_HOME/bin/java}"
  if [ -z "$exe" ] || [ ! -x "$exe" ]; then
    exe="$(command -v java || true)"
  fi
  [ -n "$exe" ] || return 1
  "$exe" -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n 1
}

if [ ! -f "$JAVA_PROJECT/settings.gradle.kts" ]; then
  echo "SKIP: $JAVA_PROJECT is not present"
elif [ ! -x "$JAVA_PROJECT/gradlew" ]; then
  echo "SKIP: $JAVA_PROJECT/gradlew is missing or not executable"
else
  major="$(java_major || true)"
  case "$major" in
    '' | *[!0-9]*) major=0 ;;
  esac
  if [ "$major" -lt 25 ]; then
    echo "SKIP: the Java templates need JDK 25 (found: ${major:-none}; GitHub Actions still runs this gate)"
    SKIPPED=1
  else
    echo
    echo "==> [templates] java ($JAVA_PROJECT)"
    # lint compiles both source sets: the template has no separate Java formatter yet.
    case "$MODE" in
      lint) tasks=(classes testClasses) ;;
      test) tasks=(test jacocoTestCoverageVerification) ;;
      all) tasks=(check) ;;
    esac
    if ! (cd "$JAVA_PROJECT" && ./gradlew --no-daemon --console=plain "${tasks[@]}"); then
      FAILED=1
    fi
  fi
fi

# TypeScript: two standalone pnpm roots (library and service).
TS_PROJECT="templates/golden-paths/typescript"

node_major() {
  command -v node > /dev/null 2>&1 || return 1
  node -p 'process.versions.node.split(".")[0]' 2>/dev/null
}

if [ ! -d "$TS_PROJECT" ]; then
  echo "SKIP: $TS_PROJECT is not present"
elif ! command -v pnpm > /dev/null 2>&1; then
  echo "SKIP: the TypeScript templates need pnpm, which is not installed here (GitHub Actions still runs this gate)"
  SKIPPED=1
else
  major="$(node_major || true)"
  case "$major" in
    '' | *[!0-9]*) major=0 ;;
  esac
  if [ "$major" -lt 24 ]; then
    echo "SKIP: the TypeScript templates need Node 24 LTS or newer (found: ${major:-none}; GitHub Actions still runs this gate)"
    SKIPPED=1
  else
    for shape in library service; do
      project="$TS_PROJECT/$shape"
      if [ ! -f "$project/package.json" ]; then
        echo "SKIP: $project is not present"
        continue
      fi
      echo
      echo "==> [templates] typescript/$shape ($project)"
      if ! (
        cd "$project"
        pnpm install --frozen-lockfile
        case "$MODE" in
          lint)
            pnpm run format:check
            pnpm run lint
            pnpm run typecheck
            ;;
          test) pnpm run test:coverage ;;
          all) pnpm run check ;;
        esac
      ); then
        FAILED=1
      fi
    done
  fi
fi

# Rust: one cargo workspace with library and job members.
RS_PROJECT="templates/golden-paths/rust"

if [ ! -f "$RS_PROJECT/Cargo.toml" ]; then
  echo "SKIP: $RS_PROJECT is not present"
elif ! command -v cargo > /dev/null 2>&1; then
  echo "SKIP: the Rust templates need cargo, which is not installed here (GitHub Actions still runs this gate)"
  SKIPPED=1
elif [ "$MODE" != "test" ] && ! command -v cargo-deny > /dev/null 2>&1; then
  echo "SKIP: the Rust templates need cargo-deny, which is not installed here (GitHub Actions still runs this gate)"
  SKIPPED=1
elif [ "$MODE" != "lint" ] && ! command -v cargo-llvm-cov > /dev/null 2>&1; then
  echo "SKIP: the Rust templates need cargo-llvm-cov, which is not installed here (GitHub Actions still runs this gate)"
  SKIPPED=1
else
  echo
  echo "==> [templates] rust ($RS_PROJECT)"
  if ! (
    cd "$RS_PROJECT"
    if [ "$MODE" != "test" ]; then
      cargo fmt --all --check
      cargo clippy --workspace --all-targets --locked -- -D warnings
      # deny.toml mirrors the ADR-0003 allow list; Syft cannot read crate licences,
      # so this is the licence gate for the Rust dependencies.
      cargo deny check
    fi
    if [ "$MODE" != "lint" ]; then
      cargo llvm-cov --workspace --locked --summary-only --fail-under-lines 85
      # The policy seam must stay at 100% lines even when the branch total dips. The
      # dot in the pattern matches the path separator on Windows and Linux.
      cargo llvm-cov report --summary-only | awk '
        $1 ~ /src.policy\.rs$/ {
          rows++
          if ($10 != "100.00%") {
            printf "policy file below 100%% line coverage: %s (%s)\n", $1, $10 > "/dev/stderr"
            failed = 1
          }
        }
        END {
          if (rows == 0) {
            print "no policy.rs coverage row found; the policy seam must be measured" > "/dev/stderr"
            failed = 1
          }
          exit failed ? 1 : 0
        }
      '
    fi
  ); then
    FAILED=1
  fi
fi

if [ "$FAILED" -eq 1 ]; then
  echo "RESULT: FAIL"
  exit 1
fi
if [ "$SKIPPED" -eq 1 ]; then
  echo "WARNING: some templates were skipped because their toolchain is missing here."
  echo "RESULT: SKIP (partial)"
  exit 3
fi
echo "RESULT: PASS"
