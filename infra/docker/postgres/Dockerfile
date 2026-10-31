# Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — PostgreSQL 18 test/dev image with PostGIS 3 and pgvector from the PGDG repository of the official image (P0)
# Modified by Kone Tshivhinda (Qoder) on 2026-10-31 — Build on the PostGIS project's PostgreSQL 18 image and add only pgvector from PGDG; the previous apt install pulled the whole GDAL/GEOS stack from Debian and took over an hour on a slow link
#
# Used by docker-compose.ci.yml (test-postgres), docker-compose.dev.yml (db) and .github/workflows/e2e-live.yml.
# ARCHITECTURE §5: PostgreSQL 18→19 + PostGIS + pgvector per tenant. An owned derived image, no Bitnami
# (ARCHITECTURE §13). postgis/postgis keeps PGDG's signed apt source for trixie-pgdg, so only its own
# index is fetched; Debian supplies libc and the extension depends on postgresql-18 alone.
ARG POSTGIS_IMAGE=postgis/postgis:18-3.6@sha256:4a43e3c3e5d55b9208240fc71db543b79c638766280c625a425bd0a2390989c4
FROM ${POSTGIS_IMAGE}

ARG PGVECTOR_VERSION=0.8.7-1.pgdg13+1

RUN set -eux; \
    apt-get -o Dir::Etc::sourcelist=/etc/apt/sources.list.d/pgdg.list \
        -o Dir::Etc::sourceparts=/dev/null \
        -o APT::Get::List-Cleanup=0 update; \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        "postgresql-${PG_MAJOR}-pgvector=${PGVECTOR_VERSION}"; \
    rm -rf /var/lib/apt/lists/*

# Runs once, on first start, against $POSTGRES_DB: CREATE EXTENSION postgis, vector.
COPY initdb/ /docker-entrypoint-initdb.d/

# Least privilege: the official entrypoint supports starting as the image's postgres user.
# PG18 keeps data in /var/lib/postgresql/18/docker, so mount volumes/tmpfs at /var/lib/postgresql.
USER postgres

# TCP (not the socket) so the check stays red while the entrypoint's socket-only init server runs.
HEALTHCHECK --interval=5s --timeout=3s --retries=20 \
    CMD pg_isready -h 127.0.0.1 -U "${POSTGRES_USER:-postgres}" || exit 1
