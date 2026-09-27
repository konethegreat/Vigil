<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — ADR-0003 open-source licence policy (P0) -->

# ADR-0003 — Open-Source Licence Policy (a Board-Level Control)

**Status:** Proposed (P0), to be accepted at founder sign-off (P0 task 0.7) · **Date:** 2026-09-25

## Context
Vigil ships commercial software into sovereign, on-prem and air-gapped environments. Infrastructure software keeps changing its licence or being abandoned:
- HashiCorp moved to BSL, and OpenTofu forked it.
- Redis moved to RSAL/SSPL, then added AGPL, and Valkey forked it.
- Elastic added AGPL.
- CockroachDB retired its free Core edition.
- EMQX moved to BSL.
- MinIO's community repository was archived in April 2026.
- Kùzu was archived in October 2025, and JedAI in May 2026.
- Langfuse joined ClickHouse, and OpenAI announced it is buying Astral (uv/Ruff).

Foundation-governed projects (ASF, CNCF, LF, Eclipse, PyTorch Foundation) have held their licences. Research: [appendix 05 §5](../research/appendix/05-polyglot-architecture.md).

## Options
1. **Case-by-case review, no policy.**
   - Against: relicensing and archival events arrive faster than ad-hoc reviews catch them.
2. **Permissive-only: ban all copyleft.**
   - Against: excludes PostGIS, Grafana and common LGPL libraries with no permissive equivalent.
3. **A tiered green/amber/red policy with an audited allow-list and a CI gate (chosen).**

## Decision

| Class | Licences | Rule |
|---|---|---|
| 🟢 Green | Apache-2.0, MIT, BSD-2/3, ISC, PostgreSQL, Zlib, Unicode; MPL-2.0 / EPL-2.0 used unmodified or with patches upstreamed; CC-BY-4.0 data | Allowed anywhere |
| 🟡 Amber | LGPL, GPL, AGPL, CC-BY-SA/ODbL data, OpenRAIL and community model licences (e.g. Llama) | Needs an architecture review and an ADR note. Allowed only as an **unmodified separate service or process**. **Never** in the edge agent, SDKs, mobile app or Warden. ODbL data is kept in separate databases |
| 🔴 Red | BSL/BUSL, SSPL, ELv2, FSL, Commons Clause, RSAL, PolyForm Noncommercial, Confluent Community, CC-BY-NC / "research-only", revenue-threshold licences | Banned in anything shipped unless a commercial licence has been bought and recorded |

**Standing amber decisions** (the ADR notes the allow-list refers to):
- **LGPL** libraries may be linked dynamically and used unmodified, with notices (e.g. GEOS, Pango). They are still listed in the allow-list for traceability.
- **PostGIS** (GPL-2.0-or-later) runs unmodified inside the PostgreSQL server as an extension. Database images that include it ship the licence notice and a source offer.
- **Grafana** (AGPL-3.0) runs unmodified as a separate service (ARCHITECTURE §11).
- CI-only tooling that is never shipped is outside the shipped-artefact scope, but must still not be 🔴.
- **Unknown or unrecognised licences are treated as amber** and fail the gate until classified.

**Governance score:** foundation-hosted > multi-vendor > single-vendor. Every single-vendor dependency needs a documented exit plan and an abstraction boundary (S3 API, Iceberg REST, Protobuf, OpenAI-compatible model API).

**Controls:**
- `sbom-license-scan.yml` (Syft SBOM + licence policy) blocks red licences, and amber ones without an allow-list entry, on every PR.
- Every artefact is mirrored (air-gap needs this anyway).
- The top 50 dependencies are re-reviewed every six months.
- Models are included: production uses Apache/MIT-licensed weights only.

## Consequences
Some convenient tools are excluded (Redpanda, Materialize, EMQX, PowerSync, Neo4j CE in-process, NC-licensed African-language models). This is deliberate. Customers get a clean, sovereign licence posture.
