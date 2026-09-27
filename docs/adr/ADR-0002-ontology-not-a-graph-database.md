<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — ADR-0002 ontology implementation strategy (P0) -->

# ADR-0002 — The Ontology Is a Metadata-Driven Layer over Polyglot Persistence, Not a Graph Database

**Status:** Proposed (P0), to be accepted at founder sign-off (P0 task 0.7) · **Date:** 2026-09-25

## Context
The Gemini brief proposed "PostgreSQL with graph/relational modelling or dedicated graph backends paired with semantic vector search" as Vigil's core primitive. Palantir's public docs show something different: its Ontology is **five services over versioned datasets**, namely a metadata service, an indexing funnel, purpose-built object stores, a read service and a transactional Actions service. It is not built on RDF or a graph database. Operational traversals are shallow: Palantir's TypeScript v1 functions cap object loads at three search-arounds. Graph databases carry licence and viability risk: Neo4j Community is GPLv3, Memgraph is BSL, FalkorDB is SSPL, and Kùzu was archived on 10 Oct 2025. PostgreSQL 19 adds SQL/PGQ property-graph queries (in beta in Sep 2026, per the PG19 release notes; re-verify at GA). Research: [appendix 02 §1, §9](../research/appendix/02-palantir-technical.md), [appendix 05 §4g](../research/appendix/05-polyglot-architecture.md).

## Options
1. **A dedicated graph DB as the system of record** (Neo4j/JanusGraph/NebulaGraph).
   - For: native traversals.
   - Against: licence risk, weak row-level and marking security, a second transactional store, and poor analytics and search.
2. **RDF/OWL triple store.**
   - For: standards and inference.
   - Against: open-world semantics don't fit closed-world, permissioned, transactional operations, and performance for geo, time-series and search is weak.
3. **A metadata-driven ontology over Postgres typed tables + OpenSearch + Iceberg, with a Rust traversal service and PG19 SQL/PGQ (chosen).**

## Decision
Implement the ontology as follows:
- **OMS** holds the metadata.
- **Typed physical tables** are generated per object type, and links become foreign keys or join tables.
- An **indexer** writes those tables and OpenSearch.
- An **Object Set Service** compiles queries and pushes security down.
- A **Rust traversal service** runs multi-hop analysis over Arrow snapshots.
- **An append-only edit log** is the source of truth for edits.
- A graph engine (AGE, PuppyGraph, DuckPGQ) may be added later as a *replaceable projection* behind the Object Set API, never as the system of record.

## Consequences
- **For:**
  - one transactional store with mature RLS;
  - native search, geo (PostGIS) and vector (pgvector);
  - no graph licence exposure;
  - swappable engines.
- **Against:**
  - Vigil must build the OMS, Indexer, OSS and Actions itself (this is the core IP);
  - deep path analytics need the traversal service (P7).
