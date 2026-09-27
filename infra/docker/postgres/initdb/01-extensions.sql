-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Enable PostGIS and pgvector in the test database on first start (P0)
-- Executed by the official image's entrypoint (/docker-entrypoint-initdb.d) against $POSTGRES_DB.
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS vector;
