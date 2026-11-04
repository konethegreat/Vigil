#!/usr/bin/env bash
# Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Verify Buf contract codegen and compile checks across all six languages (P2 task 2.1, task 2.5)
# Modified by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Add WIT world validation and Java -> Kafka -> Python -> Rust EventEnvelope round-trip check (P2 task 2.4, Done-when)
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

BUF_IMAGE="bufbuild/buf:1.73.0"
GO_IMAGE="golang:1.27.1-alpine"
KAFKA_IMAGE="apache/kafka:4.4.0"
GEN_DIR="sdks/_codegen/gen"

echo "==> [codegen] cleaning $GEN_DIR"
rm -rf "$GEN_DIR" "sdks/_codegen/typescript/src/gen"
mkdir -p "$GEN_DIR/roundtrip"

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

echo "==> [codegen] Java & Kotlin compile check + sample EventEnvelope emission (JDK 25 + Gradle 9.8.2)"
(
  if [ -z "${JAVA_HOME:-}" ] && [ -d "C:/Users/Konet/.jdks/jdk-25.0.5+8" ]; then
    export JAVA_HOME="C:/Users/Konet/.jdks/jdk-25.0.5+8"
  fi
  cd sdks/_codegen/java
  ./gradlew --no-daemon --console=plain classes
  ./gradlew --no-daemon -q emitSampleEvent > "../gen/roundtrip/java_out.b64"
)

echo "==> [codegen] Kafka EventEnvelope broker round-trip ($KAFKA_IMAGE)"
(
  KAFKA_CONTAINER="vigil-p2-kafka-$$"
  trap 'docker rm -f "$KAFKA_CONTAINER" >/dev/null 2>&1 || true' EXIT
  MSYS_NO_PATHCONV=1 docker run -d --name "$KAFKA_CONTAINER" "$KAFKA_IMAGE" >/dev/null
  ready=0
  for _ in $(seq 1 30); do
    if MSYS_NO_PATHCONV=1 docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-topics.sh \
      --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
      ready=1
      break
    fi
    sleep 1
  done
  if [ "$ready" -ne 1 ]; then
    echo "error: Kafka container failed to become ready within 30s" >&2
    exit 1
  fi
  MSYS_NO_PATHCONV=1 docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 --create --if-not-exists \
    --topic vigil.demo.ontology.types --partitions 1 --replication-factor 1 >/dev/null
  MSYS_NO_PATHCONV=1 docker exec -i "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 --topic vigil.demo.ontology.types \
    < "$GEN_DIR/roundtrip/java_out.b64"
  MSYS_NO_PATHCONV=1 docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 --topic vigil.demo.ontology.types \
    --from-beginning --max-messages 1 --timeout-ms 10000 \
    > "$GEN_DIR/roundtrip/kafka_out.b64"
)

echo "==> [codegen] Python WIT world check, stub syntax/runtime check, and Kafka consumer verification"
python sdks/_codegen/python/check_generated.py

echo "==> [codegen] Rust compile check and EventEnvelope round-trip decode (prost 0.14)"
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

echo "==> [codegen] PASS: all six contract languages, WIT worlds, and Java -> Kafka -> Python -> Rust round-trip verified."
