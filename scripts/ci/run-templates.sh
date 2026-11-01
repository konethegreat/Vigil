#!/usr/bin/env bash
# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Verify every golden-path template (P1 task 1.4)
# Modified by Kone Tshivhinda (Qoder) on 2026-11-01 — Add the Java 25 templates (P1 task 1.4)
#
# Usage: [MODE=all|lint|test] scripts/ci/run-templates.sh
# Each Python template is a standalone uv project; the Java templates are one
# Gradle build with three modules. The script runs the same gates the template
# READMEs document, so a template cannot rot unnoticed.
# Exits 1 if any gate fails. CI runs it from ci-backend.yml; run-backend.sh calls
# it for `just lint` and `just test`. A language whose toolchain is absent here is
# skipped with a warning; GitHub Actions installs every toolchain and runs them all.
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

if [ "$SKIPPED" -eq 1 ]; then
  echo "WARNING: some templates were skipped because their toolchain is missing here."
fi
if [ "$FAILED" -eq 1 ]; then
  echo "RESULT: FAIL"
  exit 1
fi
echo "RESULT: PASS"
