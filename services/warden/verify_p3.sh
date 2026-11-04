#!/usr/bin/env bash
# Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - End-to-end P3 Warden verification runner across Rust, Java FFM, Python, TypeScript/WASM, and PostgreSQL 18 (P3 tasks 3.1-3.9)
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."
REPO_ROOT="$(pwd)"

echo "==> [1/5] Building Rust Warden kernel (native cdylib + wasm32-unknown-unknown) and running Rust unit/property/conformance tests"
(
  cd services/warden
  cargo fmt --check
  cargo clippy --all-targets -- -D warnings
  cargo test --locked
  cargo build --release
  rustup target add wasm32-unknown-unknown
  cargo build --release --target wasm32-unknown-unknown
)

echo "==> [2/5] Running Python binding over 560-case shared conformance suite + W12 audit chain"
python services/warden/bindings/python/warden_binding.py

echo "==> [3/5] Running TypeScript/WebAssembly binding over 560-case shared conformance suite + W12 audit chain"
node services/warden/bindings/typescript/verify_wasm.mjs

echo "==> [4/5] Running Java 25 Panama FFM binding over 560-case shared conformance suite + W12 audit chain"
if [ -z "${JAVA_HOME:-}" ] && [ -d "C:/Users/Konet/.jdks/jdk-25.0.5+8" ]; then
  export JAVA_HOME="C:/Users/Konet/.jdks/jdk-25.0.5+8"
fi
./gradlew --no-daemon compileJava
"${JAVA_HOME:-/usr}/bin/java" --enable-native-access=ALL-UNNAMED -cp build/classes/java/main vigil.core.audit.WardenJavaBinding "$REPO_ROOT"

echo "==> [5/5] Running PostgreSQL 18 DATA_MODEL security & RLS verification suite (MSG-023 / MSG-030)"
PG_CONTAINER="vigil-p3-pg18-verify-$$"
cleanup() {
  docker rm -f "$PG_CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

MSYS_NO_PATHCONV=1 docker run -d --name "$PG_CONTAINER" \
  -e POSTGRES_DB=vigil \
  -e POSTGRES_USER=vigil_admin \
  -e POSTGRES_PASSWORD=vigil_admin_local_only \
  -v "$REPO_ROOT:/work:ro" \
  postgres:18-alpine >/dev/null

for _ in $(seq 1 30); do
  if docker exec "$PG_CONTAINER" pg_isready -U vigil_admin -d vigil >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

MSYS_NO_PATHCONV=1 docker exec -e PGPASSWORD=vigil_admin_local_only "$PG_CONTAINER" \
  psql -U vigil_admin -d vigil -v ON_ERROR_STOP=1 -f /work/services/warden/sql/verify_p3_pg18.sql

echo "============================================================"
echo "PASS: All P3 Warden Security Kernel Done-when criteria verified!"
echo "============================================================"
