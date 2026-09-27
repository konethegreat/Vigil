# Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — PostgreSQL 18 test/dev image with PostGIS 3 and pgvector from the PGDG repository of the official image (P0)
#
# Used by docker-compose.ci.yml (test-postgres) and .github/workflows/e2e-live.yml.
# ARCHITECTURE §5: PostgreSQL 18→19 + PostGIS + pgvector per tenant. An owned base image, no Bitnami
# (ARCHITECTURE §13). The official image already configures apt.postgresql.org (PGDG) for $PG_MAJOR,
# so the extensions come from the same repository as the server itself.
ARG POSTGRES_VERSION=18.6
FROM postgres:${POSTGRES_VERSION}-trixie

ARG POSTGIS_MAJOR=3

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        "postgresql-${PG_MAJOR}-postgis-${POSTGIS_MAJOR}" \
        "postgresql-${PG_MAJOR}-postgis-${POSTGIS_MAJOR}-scripts" \
        "postgresql-${PG_MAJOR}-pgvector"; \
    rm -rf /var/lib/apt/lists/*

# Runs once, on first start, against $POSTGRES_DB: CREATE EXTENSION postgis, vector.
COPY initdb/ /docker-entrypoint-initdb.d/

# Least privilege: the official entrypoint supports starting as the image's postgres user.
# PG18 keeps data in /var/lib/postgresql/18/docker, so mount volumes/tmpfs at /var/lib/postgresql.
USER postgres

# TCP (not the socket) so the check stays red while the entrypoint's socket-only init server runs.
HEALTHCHECK --interval=5s --timeout=3s --retries=20 \
    CMD pg_isready -h 127.0.0.1 -U "${POSTGRES_USER:-postgres}" || exit 1
