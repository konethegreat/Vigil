#!/usr/bin/env bash
# Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Verify Buf contract codegen and compile checks across all six languages (P2 task 2.1, task 2.5)
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

BUF_IMAGE="bufbuild/buf:1.73.0"
GO_IMAGE="golang:1.27.1-alpine"
GEN_DIR="sdks/_codegen/gen"

echo "==> [codegen] cleaning $GEN_DIR"
rm -rf "$GEN_DIR" "sdks/_codegen/typescript/src/gen"

echo "==> [codegen] buf lint proto"
MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD:/work" -w /work "$BUF_IMAGE" lint proto

echo "==> [codegen] buf breaking proto (against origin/develop)"
if git cat-file -e origin/develop:proto/buf.yaml 2>/dev/null; then
  git branch --force vigil-breaking-base origin/develop
  MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD:/work" -w /work "$BUF_IMAGE" \
    breaking proto --against ".git#branch=vigil-breaking-base,subdir=proto"
else
  echo "SKIP: origin/develop has no proto/buf.yaml"
fi

echo "==> [codegen] buf generate proto"
MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD:/work" -w /work "$BUF_IMAGE" \
  generate proto --template proto/buf.gen.yaml --path proto/vigil

echo "==> [codegen] Rust compile check (prost 0.14)"
(
  cd sdks/_codegen/rust
  cargo check --locked
  cargo test --locked
)

echo "==> [codegen] TypeScript compile check (@bufbuild/protobuf 2.9.0)"
(
  mkdir -p sdks/_codegen/typescript/src/gen
  cp -R "$GEN_DIR/ts/." sdks/_codegen/typescript/src/gen/
  trap 'rm -rf sdks/_codegen/typescript/src/gen' EXIT
  cd sdks/_codegen/typescript
  pnpm install --frozen-lockfile
  pnpm run typecheck
)

echo "==> [codegen] Python syntax and runtime check"
python sdks/_codegen/python/check_generated.py

echo "==> [codegen] Java & Kotlin compile check (JDK 25 + Gradle 9.8.2)"
(
  if [ -z "${JAVA_HOME:-}" ] && [ -d "C:/Users/Konet/.jdks/jdk-25.0.5+8" ]; then
    export JAVA_HOME="C:/Users/Konet/.jdks/jdk-25.0.5+8"
  fi
  cd sdks/_codegen/java
  ./gradlew --no-daemon --console=plain classes
)

echo "==> [codegen] Go compile check ($GO_IMAGE)"
MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD:/work" -w /work "$GO_IMAGE" sh -c '
  set -eu
  tmp="$(mktemp -d)"
  cp -R sdks/_codegen/gen/go/. "$tmp/"
  cd "$tmp"
  go mod init github.com/konethegreat/Vigil/gen/go
  go mod edit -require=google.golang.org/protobuf@v1.36.10
  go mod tidy
  go build ./...
'

echo "==> [codegen] PASS: all six contract languages generated and verified."
