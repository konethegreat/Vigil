#!/usr/bin/env bash
# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Verify every golden-path template (P1 task 1.4)
#
# Usage: [MODE=all|lint|test] scripts/ci/run-templates.sh
# Each Python template is a standalone uv project. The script runs the same gates
# the template READMEs document, so a template cannot rot unnoticed.
# Exits 1 if any gate fails. CI runs it from ci-backend.yml; run-backend.sh calls
# it for `just lint` and `just test`.
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

if [ "$FAILED" -eq 1 ]; then
  echo "RESULT: FAIL"
  exit 1
fi
echo "RESULT: PASS"
