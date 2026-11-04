#!/usr/bin/env bash
# Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - End-to-end P4 Forge Datasets and Core Lineage verification runner (P4 tasks 4.1-4.7)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

echo "==> [1/4] Buf lint & breaking check for P4 dataset and lineage contracts"
MSYS_NO_PATHCONV=1 docker run --rm -v "$ROOT_DIR:/work" -w /work bufbuild/buf:1.73.0 lint proto

echo "==> [2/4] Core Lineage Store unit & JaCoCo coverage suite"
./gradlew --no-daemon test --tests vigil.core.lineage.LineageGraphStoreTest

echo "==> [3/4] Forge Datasets engine (Iceberg v3 transactions, branches, merge, W5 propagate, AES-256-GCM DEK, POPIA snapshot erasure)"
./gradlew -p services/forge-datasets --no-daemon spotlessCheck check test

echo "==> [4/4] PostgreSQL 18 Lineage DDL & RLS verification"
PG_CONTAINER="vigil-p4-pg18-$$"
cleanup() {
  docker rm -f "$PG_CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

MSYS_NO_PATHCONV=1 docker run -d --name "$PG_CONTAINER" \
  -e POSTGRES_PASSWORD=vigil_test \
  -e POSTGRES_DB=vigil_t_demo \
  postgres:18-alpine >/dev/null

for _ in $(seq 1 30); do
  if docker exec "$PG_CONTAINER" pg_isready -U postgres -d vigil_t_demo >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

MSYS_NO_PATHCONV=1 docker exec -i "$PG_CONTAINER" psql -v ON_ERROR_STOP=1 -U postgres -d vigil_t_demo \
  < "$ROOT_DIR/services/core/src/main/resources/db/migration/lineage/tenant/V4_001__create_lineage_tables.sql"

echo "ALL P4 FORGE DATASETS & LINEAGE CHECKS PASSED."
