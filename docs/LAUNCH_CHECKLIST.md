<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Platform release and per-customer go-live checklists with go/no-go sign-off (P0) -->

# Vigil — Launch Checklist

> **Status:** P0 v1.0 (2026-09-25).
> **How to use it:**
> - **Part A** goes into every `release/vX.Y.Z` PR description. **Part B** is copied into the customer's go-live record for every new deployment, and again for any major upgrade.
> - Every ticked item links to its **evidence**: a CI run, a report in `docs/reviews/`, a signed document or a ticket. A tick without evidence counts as unticked.
> - Any unticked item means **no-go**, unless the sign-off table records a waiver naming who accepted the risk and until when.
> - **Items marked ⛔ can never be waived.** They include open critical vulnerabilities, a red licence in the SBOM, a failed prompt-injection or red-line test, a failed egress test, a missing s57 authorisation where linkage is used, and no named Information Officer; the ⛔ markers below are the complete list.
> - A bare `§` reference below points to SECURITY_AND_COMPLIANCE.md.
> **Read with:** [`SECURITY_AND_COMPLIANCE.md`](SECURITY_AND_COMPLIANCE.md), [`SYSTEM_COMPLIANCE_GUARDRAILS.md`](SYSTEM_COMPLIANCE_GUARDRAILS.md), [`INCIDENT_RESPONSE.md`](INCIDENT_RESPONSE.md), [`BRANCHING_STRATEGY.md`](BRANCHING_STRATEGY.md) §7.

## Part A — Platform release checklist (`release/vX.Y.Z`)

### A1. Build and supply chain
- [ ] All seven workflows are green on the release branch, and local Docker CI was run with exit codes verified (BACKEND_GUIDE §10).
- [ ] ⛔ `sbom-license-scan` is green: an SBOM exists for every artefact, with no red licences and no amber licence missing from the allow-list (ADR-0003).
- [ ] Every image and bundle is **signed** (cosign) and carries SLSA provenance, and a Kyverno policy test shows unsigned images are rejected (P20.3).
- [ ] Base images were rebuilt within the last 7 days (P1.6); lockfiles are committed and unchanged since the scans ran.

### A2. Security
- [ ] ⛔ **No open critical vulnerabilities**, and no high vulnerability past its SLA (SECURITY_AND_COMPLIANCE §19).
- [ ] Penetration-test status is current: an external test within the last 12 months, or a new one for a major version, with every critical and high finding retested (§20).
- [ ] Semgrep, Bandit, Gitleaks and Trivy are clean or triaged, with no disabled scans (INSTRUCTIONS §10 #14).
- [ ] The Warden conformance suite passes in all bindings, and differential fuzzing shows no divergence since the last release (P34.4).
- [ ] Every change to Warden, audit, crypto or identity had the security lead's review.

### A3. AI
- [ ] Model evaluation gates pass for every model, prompt or tool change; the model cards are updated (GUARDRAILS §4.7).
- [ ] ⛔ The **prompt-injection suite** (P15.5) passes, with no unauthorised tool call, and the system-prompt template matches GUARDRAILS §4.3 verbatim.
- [ ] The **AI red-team suite** (P34.2) passes: no exfiltration through tools, no marking bypass.
- [ ] The **"no-AI" fallback** works: every critical workflow completes with the model servers stopped (ARCHITECTURE §12).
- [ ] Production model weights are all Apache- or MIT-licensed (ADR-0003).

### A4. Red lines
- [ ] ⛔ The **red-line conformance suite** (P43) passes. Until P43 exists: a manual review of GUARDRAILS §2 (each red line, and each blocked combination, shown to be absent or refused), signed by the lead architect and counsel.
- [ ] No new connector class, purpose, model task or module combination bypasses the registries' deny-lists.

### A5. Reliability
- [ ] SLO dashboards and burn-rate alerts exist for every changed service (ARCHITECTURE §11); the load results for this release are within ARCHITECTURE §9 targets or have an owner-assigned plan (P32).
- [ ] Migrations tested from empty and from the previous release; rollback plan documented and tested on Relay's `canary` channel (P21).
- [ ] Chaos and disconnection suites pass (P33): Kafka down, indexer lag, webhook timeout, WAN cut.

### A6. Accessibility
- [ ] axe reports **0 serious or critical** violations across the Cockpit journeys; the keyboard-only and screen-reader passes are done (FRONTEND_GUIDE §9).

### A7. Documentation and runbooks
- [ ] Release notes, upgrade guide and air-gap bundle instructions are written; API and event changes are appended to `docs/API_DESIGN.md` and `docs/asyncapi/`.
- [ ] Runbooks for new failure modes are added to INCIDENT_RESPONSE; tier audits due in `docs/reviews/` are merged (reviews/README).
- [ ] WORKLOG and AI_COMMUNICATION are up to date; no open ⚠️ WARNING blocks the release.

## Part B — Per-customer go-live checklist

**Customer / deployment:** ______ · **Topology:** sovereign DC / on-prem / air-gapped / edge / commercial cloud tier · **Version:** ______ · **Target date:** ______

### B1. Contract and governance
- [ ] The **s21 operator agreement** is signed; the subprocessor list is approved by the customer (SECURITY_AND_COMPLIANCE §17).
- [ ] The acceptable-use policy, with suspension and termination rights, is in the contract (GUARDRAILS §9).
- [ ] Customer due diligence is complete: human-rights due diligence for government buyers, embargo and abuse screening, and export classification for any buyer outside South Africa (GUARDRAILS §7).
- [ ] ⛔ **Vigil's Information Officer is named and registered**, and the customer's Information Officer and counsel contacts are recorded (B10).
- [ ] ECT Act Chapter V registration is in place (SECURITY_AND_COMPLIANCE §8).

### B2. Privacy (POPIA)
- [ ] The **personal-information impact assessment (PIA)** is done and signed by the customer.
- [ ] A **lawful basis and a purpose** are recorded for every dataset and connector, and ingestion refuses untagged data (SECURITY_AND_COMPLIANCE §2.3).
- [ ] ⛔ **s57 prior authorisation has been obtained where cross-party linkage (or any other s57 trigger) is used**, and it is recorded in `warden.gate_authorisation` (gate `S57_1A_LINKAGE` or `S57_1B_WATCHLIST`, dual-approved, in date). Otherwise the linkage lock stays closed (§2.6; P16.4).
- [ ] Any s6(1)(c) exclusion claim is signed by the customer's counsel and recorded per dataset (§2.2).
- [ ] Special PI and children's data are gated and separately encrypted, or confirmed absent (§2.5).
- [ ] Retention policies are configured; the data-subject request (s23–s25) and PAIA processes are live.
- [ ] The **breach runbook has been rehearsed** with the customer in a tabletop exercise within the last 90 days (INCIDENT_RESPONSE R1).

### B3. Security
- [ ] Penetration-test status for this version is current, with no open criticals (Part A2) and any customer-commissioned test done under the authorised-testing protocol.
- [ ] The **SBOM and licence scan** are green for the exact bundle delivered, and the site's Kyverno verifies the signatures.
- [ ] MFA is enforced for privileged roles (for every user on the sovereign tier); break-glass accounts are sealed under dual control; the first access review is done (§11).
- [ ] Audit export to the customer's SIEM is working, and audit-chain verification passes.

### B4. Sovereignty
- [ ] ⛔ The **egress test "no prompt leaves SA" passes** on this deployment, and default-deny egress is verified (P14.4).
- [ ] **Keys are customer-held where contracted**, with evidence of the key ceremony (§10.3).
- [ ] The hosting location is verified (in South Africa for NDCP ¶15.4.2 data); support is given in-country or under a recorded s72 basis; backups and DR stay in-country.

### B5. Reliability
- [ ] SLOs are agreed and their dashboards are live (ARCHITECTURE §9 v1: object-set query p95 ≤ 300 ms; Action commit p95 ≤ 250 ms; streaming freshness ≤ 60 s).
- [ ] **Backups** are configured and a **restore test** has passed, including erasure replay (SECURITY_AND_COMPLIANCE §16).
- [ ] The DR plan is tested against the agreed RPO/RTO; the capacity model is sized for the customer's volumes.
- [ ] For edge sites: offline autonomy is verified (8 hours at v1) and a remote-wipe drill has passed (INCIDENT_RESPONSE R7).

### B6. AI
- [ ] Evaluation gates pass on an approved, non-production sample of the customer's content.
- [ ] Prompt-injection and red-team suites pass against the customer's configured applications and tool manifests.
- [ ] The customer has reviewed the list of `agent_executable` Action types and the `requires_human_approval` flags (s71).
- [ ] The "no-AI" fallback has been demonstrated to the customer's operators.

### B7. Red lines, gated modules and *hessenDATA* configuration
- [ ] ⛔ The red-line suite passes on the delivered build.
- [ ] Gated modules are enabled only with recorded instruments, dual control and expiry dates (GUARDRAILS §3).
- [ ] The permitted data types and methods registry, case thresholds and intrusive-source exclusions are configured, and the publishable configuration has been generated (GUARDRAILS §6).

### B8. Accessibility
- [ ] axe reports 0 serious violations on the customer's configured apps; a keyboard walkthrough of their critical journey is done.

### B9. Documentation and training
- [ ] Operator runbooks, administrator guides and the Relay upgrade procedure have been handed over.
- [ ] Users are trained on purpose and justification, redaction, and "risk indicator, not finding".
- [ ] Approvers are trained on human review under s71; administrators are trained on gated-module grants and break-glass.

### B10. Support and escalation contacts

| Role | Vigil | Customer |
|---|---|---|
| Incident Commander (24/7) | | |
| Security lead | | |
| Information Officer | | |
| Counsel | | |
| Account / deployment lead | | |
| Customer system-of-record owners (write-back targets) | — | |

### B11. Go / no-go sign-off

| Area | Name | Role | Decision (Go / No-go / Go with waiver) | Waiver reference and expiry | Date |
|---|---|---|---|---|---|
| Engineering | | Lead architect | | | |
| Security | | Security lead | | | |
| Privacy | | Information Officer | | | |
| Legal | | Counsel | | | |
| Operations | | SRE lead | | | |
| Customer | | Customer sponsor | | | |

A go decision needs every row. For government and security-sector deployments, the oversight board is informed before go-live (GUARDRAILS §8).
