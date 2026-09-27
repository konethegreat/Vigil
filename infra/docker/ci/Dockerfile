# Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Multi-toolchain CI image (Temurin 25, Python 3.13 + uv, Rust stable, Go, Node 24 + pnpm, buf, just) for docker-compose.ci.yml ci-backend (P0)
#
# Mirrors the GitHub Actions toolchains so `just ci` gives the same verdict as CI (skill §4.2).
# Every version is pinned below; bump them together with the env blocks in
# .github/workflows/ci-backend.yml and security-scan.yml. JDK, Go, Node, uv and buf are copied from
# their official images; Debian 13's system Python is 3.13. Kotlin/Android and Elixir are not
# included (bounded languages, P27/P28): scripts/ci/run-backend.sh reports them as SKIP and GitHub
# Actions runs them. Runs as the non-root user `ci` (uid 1000), so files it writes into a
# bind-mounted checkout belong to a typical host user.
ARG DEBIAN_TAG=trixie-slim
ARG TEMURIN_TAG=25-jdk-noble
ARG GO_VERSION=1.27.1
ARG NODE_VERSION=24.21.0
ARG UV_VERSION=0.12.18
ARG BUF_VERSION=1.73.0

FROM eclipse-temurin:${TEMURIN_TAG} AS jdk
FROM golang:${GO_VERSION}-trixie AS go
FROM node:${NODE_VERSION}-trixie-slim AS node
FROM ghcr.io/astral-sh/uv:${UV_VERSION} AS uv
FROM bufbuild/buf:${BUF_VERSION} AS buf

FROM debian:${DEBIAN_TAG}

ARG PNPM_VERSION=12.6.0
ARG JUST_VERSION=1.58.0
ARG GOLANGCI_LINT_VERSION=v2.14.0
ARG RUST_TOOLCHAIN=stable

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

ENV JAVA_HOME=/opt/java/openjdk \
    GOPATH=/go \
    RUSTUP_HOME=/usr/local/rustup \
    CARGO_HOME=/usr/local/cargo \
    UV_PYTHON_DOWNLOADS=never \
    PATH=/opt/java/openjdk/bin:/usr/local/go/bin:/go/bin:/usr/local/cargo/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# Build tools, Python 3.13, and the geo/PDF libraries some Python services need (GDAL, PROJ, GEOS, Pango).
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
        bash build-essential ca-certificates curl fontconfig git jq pkg-config libssl-dev unzip xz-utils \
        python3 python3-dev python3-venv \
        gdal-bin libgdal-dev libproj-dev proj-bin libgeos-dev \
        libpango-1.0-0 libpangoft2-1.0-0 libharfbuzz-subset0 \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

COPY --from=jdk /opt/java/openjdk /opt/java/openjdk
COPY --from=go /usr/local/go /usr/local/go
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
COPY --from=uv /uv /uvx /usr/local/bin/
COPY --from=buf /usr/local/bin/buf /usr/local/bin/buf

# npm/npx entry points, then pnpm.
RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
 && ln -s ../lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx \
 && npm install --global "pnpm@${PNPM_VERSION}" \
 && rm -rf /root/.npm

# Downloads below are verified against their published SHA-256 sums in /tmp.
WORKDIR /tmp

# Rust stable through rustup (checksum-verified rustup-init), with rustfmt and clippy.
# World-writable like the official rust image, so the unprivileged user can use the cargo registry.
RUN url="https://static.rust-lang.org/rustup/dist/$(uname -m)-unknown-linux-gnu/rustup-init" \
 && curl --proto '=https' --tlsv1.2 -fsSLo rustup-init "${url}" \
 && echo "$(curl --proto '=https' --tlsv1.2 -fsSL "${url}.sha256" | cut -d' ' -f1)  rustup-init" | sha256sum -c - \
 && chmod +x rustup-init \
 && ./rustup-init -y --no-modify-path --profile minimal --default-toolchain "${RUST_TOOLCHAIN}" \
        --component rustfmt --component clippy \
 && rm rustup-init \
 && mkdir -p "${CARGO_HOME}/registry" \
 && chmod -R a+w "${RUSTUP_HOME}" "${CARGO_HOME}"

# just: release binary, checksum-verified.
RUN tarball="just-${JUST_VERSION}-$(uname -m)-unknown-linux-musl.tar.gz" \
 && curl -fsSLO "https://github.com/casey/just/releases/download/${JUST_VERSION}/${tarball}" \
 && curl -fsSL "https://github.com/casey/just/releases/download/${JUST_VERSION}/SHA256SUMS" \
        | grep " ${tarball}\$" | sha256sum -c - \
 && tar -xzf "${tarball}" -C /usr/local/bin just \
 && rm "${tarball}"

# golangci-lint compiled with the pinned Go, so it always understands the Go version it lints.
RUN go install "github.com/golangci/golangci-lint/v2/cmd/golangci-lint@${GOLANGCI_LINT_VERSION}" \
 && rm -rf /root/.cache/go-build \
 && mkdir -p /go/pkg/mod \
 && chmod -R 1777 /go

# Unprivileged runner user; cache directories exist so named volumes inherit its ownership.
RUN groupadd --gid 1000 ci \
 && useradd --uid 1000 --gid ci --create-home --shell /bin/bash ci \
 && mkdir -p /home/ci/.cache /home/ci/.gradle \
 && chown -R ci:ci /home/ci

USER ci
WORKDIR /workspace
CMD ["bash", "scripts/ci/run-backend.sh"]
