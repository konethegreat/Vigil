<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Mandatory post-merge audit artefacts: schedule, scope, naming and template (P0) -->

# `docs/reviews/`: Post-Merge Audit Artefacts

> **Status:** P0 v1.0 (2026-09-25). This folder holds the audits that the Project Setup framework requires after major milestones (Layer 5 #13), plus incident post-mortems. The File Ownership Master Index gives it to the lead architect (tier audits), P32 (`SCALE_AUDIT.md`), P34 (`SECURITY_AUDIT.md`) and incident commanders (post-mortems), **one file per audit**.

## 1. Which audits, and when

| Artefact | Due | Owner | File name |
|---|---|---|---|
| **Full codebase audit** | At the end of **each Tier**: Tier 1 after P10, Tier 2 after P21, Tier 3 after P31, Tier 4 after P37, Tier 5 after P43 | Lead architect, or a delegated reviewer who did not write most of that tier's code | `FULL_CODEBASE_AUDIT_TIER<N>_<YYYY-MM-DD>.md` |
| **Scale audit** | **P32** (task 32.3), then after any change to the ARCHITECTURE §9 targets | P32 | `SCALE_AUDIT.md` (P32), then `SCALE_AUDIT_<YYYY-MM-DD>.md` |
| **Security audit** | **P34**, then yearly with the external penetration test (SECURITY_AND_COMPLIANCE §20) | P34, then the security lead | `SECURITY_AUDIT.md` (P34), then `SECURITY_AUDIT_<YYYY-MM-DD>.md` |
| **Incident post-mortem** | Within 5 working days of every Sev-1 and Sev-2 (INCIDENT_RESPONSE §7) | The incident's IC | `POSTMORTEM_<YYYY-MM-DD>_<slug>.md` |

- **A tier audit is due within 10 working days** after the tier's last phase merges to `develop`. No `release/v*` branch may be cut after that date until the audit is merged.
- Audits land through a PR like everything else: `chore/audit-tier<N>`, `chore/postmortem-<slug>`, or the owning phase's feature branch for P32 and P34. A human merges it (INSTRUCTIONS §3).

## 2. What every audit must analyse

| Dimension | Minimum evidence |
|---|---|
| **Code quality** | Lint and static-analysis debt per language; complexity and duplication hot spots; dead code (reported, not deleted: Rule 8); attribution headers present and never overwritten (INSTRUCTIONS §8) |
| **Test coverage** | Coverage per service against the floors (at least 85%; **100%** for connectors, Warden bindings and policy-adjacent code: BACKEND_GUIDE B8); the flaky-test list and its age; Done-when criteria actually verified by tests |
| **Memory leaks** | A **24-hour soak test** per long-running service, with RSS and heap growth plotted, using JFR/heap dumps (Java), memray or tracemalloc (Python), heaptrack (Rust), pprof (Go) and browser heap snapshots (Cockpit). Unexplained growth above 5% an hour after warm-up is a finding. |
| **Architectural fidelity** | Conformance to the frozen blueprints (ARCHITECTURE, VIGIL_DOMAIN_BLUEPRINT, DATA_MODEL) and accepted ADRs: Modulith verification, no cross-service database access, contract-first boundaries, the language charter, file ownership. Every deviation cites its ADR or becomes a finding. |
| **Licence posture** | The SBOM against ADR-0003 (red, amber and allow-list); exit plans for single-vendor dependencies; the date of the last top-50 dependency review |
| **Red-line conformance** | The P43 suite, or until then the manual GUARDRAILS §2 review; blocked combinations; gated-module grants all carry instruments and expiry dates |

**Additional scope per audit:**
- *Scale:* every ARCHITECTURE §9 target, met or with an owner-assigned plan (P32 Done-when); capacity headroom; cost per tenant.
- *Security:* penetration-test and AI red-team results; vulnerability-SLA compliance; the Warden differential-fuzzing record; privacy controls (lawful-basis tag coverage, the s57 linkage lock, egress tests); supply chain (signing, SLSA, SBOM diffs).
- *Post-mortem:* the INCIDENT_RESPONSE §7 template.

## 3. Rules

1. **Audits are immutable once merged.** Corrections go into a dated addendum or the next audit, never into the merged text.
2. **Auditors report; they don't patch.** Every finding becomes a 🐛 BUG or ⚠️ WARNING entry in `docs/AI_COMMUNICATION.md` for the owning phase (Rule 8), and the audit lists those entry IDs.
3. **No personal information, secrets or exploit details** in any file here. Details of unfixed vulnerabilities stay in the restricted evidence store until the fix ships. The public audit records the finding ID and its severity.
4. Findings are measurable and link to evidence: a CI run, a dashboard export or a commit SHA.
5. Every file carries the Markdown attribution header (INSTRUCTIONS §8).

## 4. Template (full codebase, scale and security audits)

```markdown
<!-- Written by <Name> (<AI>) on YYYY-MM-DD — <Audit type> for <Tier N / phase> -->
# <FULL_CODEBASE | SCALE | SECURITY> AUDIT: <scope> (<YYYY-MM-DD>)
**Auditor(s):** <names and AI models> · **Commit audited:** `<develop SHA>` · **Phases covered:** <P..–P..>
**Verdict:** Pass / Pass with findings / Fail (release blocked)

## 1. Summary
<Five lines at most: overall health, the top three risks, and the verdict's rationale.>

## 2. Findings
| ID | Dimension | Severity | Finding | Evidence | Owner phase | AI_COMMUNICATION ID | Due |
|---|---|---|---|---|---|---|---|

## 3. Dimension results
### 3.1 Code quality          ### 3.2 Test coverage (table per service vs floors)
### 3.3 Memory (soak charts)  ### 3.4 Architectural fidelity (deviations and ADRs)
### 3.5 Licence posture       ### 3.6 Red-line conformance
### 3.7 Audit-specific scope (scale targets / security results)

## 4. Follow-up from the previous audit
| Previous finding | Status | Evidence |
|---|---|---|

## 5. Sign-off
| Role | Name | Date |
|---|---|---|
| Auditor | | |
| Lead architect | | |
| Security lead (security audits) | | |
```
