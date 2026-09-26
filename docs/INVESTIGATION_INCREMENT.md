<!-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Investigation implementation and acceptance -->
# Investigation increment 02

Vigil is an intelligence and operations platform for Africa. Crime investigations, protection, infrastructure, procurement integrity and public-service response share evidence, objects, decisions and operational accountability. GBV is one sensitive crime domain within this wider scope.

Owner: Kone Tshivhinda / OpenAI Codex GPT-6. Branch: `codex/mission-foundation`. Authorisation: founder's 26 September instruction to move forward at platform scope. Decision: ADR-0010. Status: local implementation and acceptance complete; broader platform and production gates remain open.

1. Preserve the existing repository and database; introduce a forward migration for scoped objects and relationship assertions.
2. Implement typed and cited records, reusable objects across visible missions, assertion review, source-change detection and atomic audit/idempotency behavior.
3. Build a real investigations workspace with graph, object search, timeline, source inspection and operational mission navigation.
4. Seed connected synthetic crime/integrity scenarios and verify the complete path, including adverse permission/concurrency cases.
5. Record actual results and remaining platform limitations here; commit and preserve a recovery bundle.

## Delivered behavior

The default Cockpit workspace is Investigations. Six object types and six directional relationship types share one tenant-scoped registry. Objects are immutable source-backed records. Relationships carry their own cited mission, source, assessment, reasoning and append-only review history. Objects can be reused across visible missions, without copying them or linking across tenants.

Network, Timeline and Objects use the same records, with case filters, type filters, text search, focus on an object's neighbours, preserved-source inspection and navigation to the originating mission. The connected synthetic seed comprises seven objects and eight assertions across infrastructure-crime inquiry `VGL-I001` and procurement inquiry `VGL-I002`. The scenario leaves loss versus movement versus count-error explanations unresolved and preserves disputed receiving quantities.

The existing Java command boundary owns writes. Mission version, assertion revision, idempotency receipt and local hash-chain audit prevent silent overwrites or duplicate retries. PostgreSQL applies forced RLS on all three new tables under the non-owner application role. Existing evidence-to-outcome operations and optional local-model reasoning remain available. New investigation and integrity missions receive investigation-specific task labels.

## Acceptance evidence — 26 September 2026

| Check | Observed result |
| --- | --- |
| Java/PostgreSQL and session suite | 24 tests passed: 10 new investigation tests, 11 existing mission tests, 3 session tests; zero failures/errors. Spotless passed. |
| Frontend suite | 12 tests passed: 9 gateway tests and 3 projection/chronology tests. ESLint and TypeScript passed. |
| Investigation HTTP workflow | Six groups passed through the running Cockpit gateway, Core and PostgreSQL. Final check mission `VGL-94D915D6`; seven audited events; source revisions and relationship history verified. |
| Operational regression | All 11 existing HTTP acceptance groups passed. `VGL-C31C1643` completed using LOCAL_MODEL reasoning and retained a verified 21-event audit chain. |
| Local model status | Ollama endpoint reachable; configured `llama3.1:latest` reasoner READY. Challenger unset; successful live two-model execution is not claimed. |
| Browser writes | Created source-backed document and Event objects; created and reviewed a relationship in `VGL-CD3D98A4`. Original source/hash visible; assessment history retained both revisions. |
| Browser navigation | Case scope, Timeline, source inspector and originating-mission navigation worked; post-create view selects the new object's case and Objects view. Unknown event time remains explicit. |
| Browser isolation | Review identity sees records with write controls disabled. Protection identity returns zero operations objects/assertions; prior workspace state is cleared. A request sequence guard also discards stale mission-list responses across workspace changes. |
| Responsive/accessibility check | 1440-pixel and 390-pixel layout checks found no page-width overflow. Controls have names at narrow widths; network nodes and view buttons support keyboard use. No browser console errors observed. Automated axe and assistive-technology certification were not run. |
| Production packaging | Core, Mind and standalone Cockpit Docker images built and run locally. No remote production deployment. |
| Migration and persistence | Applied forward migration to the retained database; rerun migration succeeded. After container rebuild/recreation, exactly seven seed objects remained; browser-created records persisted. Final registry: 13 objects, 11 assertions, including acceptance fixtures. |

An initial Gradle test-worker connection timed out while Docker's clock showed jumps. The next run completed all tests, and the final run after the purpose-label change passed again. No tests were skipped or disabled to get a pass. Large full-page browser capture was unavailable; responsive checks used rendered DOM and viewport screenshots.

## Deliberate boundaries

- This is a bounded local registry, capped at 500 objects and 1,000 assertions per tenant. The network canvas displays at most 30 nodes and says when it is capped. It does not prove platform-scale traversal/search, streaming ingestion or multi-agency federation.
- Source freshness is conservative: the pinned revision belongs to the mission's evidence snapshot. Any evidence change in the cited mission flags earlier records. Reviewed links can pin the current snapshot; immutable objects need a future versioned correction workflow.
- Claims remain analyst-entered and source-cited. The model planner still receives mission evidence, not automatic graph retrieval. Graph-aware model proposals, extraction and approval are subsequent work.
- Mission export includes investigation command audit details, but shared-object bundles and combined multi-mission investigation exports are not yet implemented.
- Production OIDC, granular markings, Warden, external audit anchoring, attachments/OCR, document ingestion, geospatial analysis and offline sync remain open. Full P0–P43 completion and the original full-platform CI/security/licence gate suite are not claimed.
- There is no configured Git remote, so no PR or remote CI was created. Original attribution and append-only ledgers are preserved. Work was performed without sub-agents.

Recovery material is stored outside the working folder in `C:/Users/Kone/Vigil-recovery/20260926-investigations/`: pre-change Git bundle/database dump, final Git bundle/database dump, and an interface review image. These are local recovery copies, not off-device backups.
