<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Incident response: severities, roles, timelines, templates, nine runbooks, admin-bypass rules and the blameless post-mortem template (P0) -->

# Vigil — Incident Response

> **Status:** P0 v1.0 (2026-09-25). Runbooks for new failure modes are added here as phases ship (LAUNCH_CHECKLIST A7).
> **Read with:** [`SECURITY_AND_COMPLIANCE.md`](SECURITY_AND_COMPLIANCE.md) (§2.4 breach duties, §9 challenge-and-disclose), [`SYSTEM_COMPLIANCE_GUARDRAILS.md`](SYSTEM_COMPLIANCE_GUARDRAILS.md), [`BRANCHING_STRATEGY.md`](BRANCHING_STRATEGY.md) §6, [`ARCHITECTURE.md`](ARCHITECTURE.md) §11–§12.
> Legal steps are summaries, not legal advice. The Information Officer and counsel decide every notification.

## 1. Severity levels

| Severity | Definition | Examples | Response |
|---|---|---|---|
| **Sev-1: Critical** | Confirmed or likely harm to people, data or operations; legal notification likely | A personal-information breach; a marking or policy bypass exposing data in production; ransomware; a compromised signing key; a red-line violation; write-back harm to a safety-critical operation; a full outage of a sovereign deployment | Page 24/7 immediately. IC within **15 minutes**. Updates every **30 minutes**. Executives and the Information Officer informed within **1 hour**. |
| **Sev-2: High** | A serious weakness or degradation without confirmed exposure | An unapplied Warden obligation caught before exposure; a single-tenant outage; injection causing unauthorised *staged* Actions; loss of an encrypted edge node; a foreign legal demand; any break-glass use | Page 24/7. IC within **30 minutes**. Updates every **60 minutes**. |
| **Sev-3: Moderate** | Partial impact with a workaround | A failing non-critical pipeline; an injection attempt that was blocked; one user unable to work | Business hours. IC by the next working hour. Daily updates. |
| **Sev-4: Low** | Minor or cosmetic; a near miss with no impact | A UI defect; a noisy alert | Normal backlog; no IC |

When in doubt, declare the **higher** severity; downgrading later is cheap. Any suspected compromise of personal information starts at Sev-2 or above until the Information Officer has assessed it.

## 2. Roles

| Role | Responsibilities |
|---|---|
| **Incident Commander (IC)** | Owns the incident from declaration to resolution: sets severity, assigns roles, makes decisions, approves recovery and declares it resolved. On a Sev-1 the IC does not work hands-on. |
| **Comms lead** | The single voice for customers, staff and status pages, using the templates in §4. Coordinates regulatory communications with the Information Officer. |
| **Scribe** | Keeps a UTC timeline of events, decisions, actions and evidence references. It becomes the post-mortem's timeline. |
| **Security lead** | Forensics, containment, evidence preservation and chain of custody (§3.2). |
| **Legal / Information Officer** | Assesses POPIA s21/s22 duties and files with the Information Regulator's eServices portal where Vigil is the responsible party; handles Cybercrimes Act, GDPR and foreign-demand questions and legal privilege; approves statements with legal effect. |
| **Responders** | Service owners who carry out technical tasks for the IC. |

**AI agents** may help as scribes or analysts (drafting timelines, querying logs) under human direction. They **never** change production, contact customers or regulators, merge, or bypass protections (INSTRUCTIONS §3).

## 3. Lifecycle, timelines and evidence

### 3.1 Timelines

| Step | Target |
|---|---|
| Acknowledge the page | Sev-1: 5 min · Sev-2: 15 min |
| Declare, set severity, name the IC | Sev-1: ≤ 15 min from detection · Sev-2: ≤ 30 min |
| Tell the security lead and Information Officer about any suspected PI compromise | **Immediately; at most 1 hour** |
| Notify the affected customer (Vigil as operator, POPIA s21) | **Immediately; at most 24 hours** after becoming aware (SECURITY_AND_COMPLIANCE §2.4) |
| Notify the Information Regulator (eServices portal) and data subjects | As soon as reasonably possible (s22), by the responsible party |
| Report prescribed offences to SAPS (bank and telecoms clients, once Cybercrimes Act s54 commences) | Within 72 hours **[counsel to confirm commencement]** |
| Notify an EU supervisory authority (only where EU data subjects are affected) | Within 72 hours (GDPR Art. 33) **[counsel to confirm]** |
| Draft the post-mortem | Sev-1/Sev-2: within 5 working days |

### 3.2 Evidence preservation (every security runbook)

- **Preserve before you fix.** Snapshot affected volumes; export the relevant audit-chain segment and **verify its hashes and Ed25519 signatures** (DATA_MODEL §3.5; P40); keep logs, traces and Langfuse sessions; record image digests and Relay plan history.
- **Never wipe or rebuild a compromised system before the security lead has captured it**, unless doing so is needed to stop ongoing harm, and then record why.
- Keep a chain-of-custody log (who took what, when, with which hash), store evidence in a restricted, in-country evidence store, and place it under legal hold where counsel directs.
- Record every command run in production in the incident timeline.

## 4. Communication templates

**T1: Internal declaration** (incident channel)
```
[SEV-<n>] <short title> — declared <UTC time> by <name>
IC: <name> · Comms: <name> · Scribe: <name> · Security: <name> · IO/Legal: <name>
Impact (known): <tenants / services / data classes>  ·  Not yet known: <…>
Current action: <containment step>  ·  Next update: <UTC time>
```

**T2: Customer notification** (Vigil as operator, POPIA s21)
```
Subject: Security incident notification — <deployment> — <reference>
We are notifying you, as the responsible party, of a security compromise that may affect personal information
processed on your behalf. Became aware: <UTC>. What happened (known so far): <…>. Information possibly affected:
<categories, approximate numbers, tenants>. Contained: <yes/no — measures>. What we need from you: <decisions,
contacts>. Support for your s22 notification: we will provide an incident pack for the Information Regulator's
eServices portal by <time>. Next update: <UTC>. Contact: <IC and Information Officer>.
```

**T3: Information Regulator notification checklist** (filed on the eServices portal by the responsible party)
- The responsible party and its Information Officer; the operator(s) involved; the date and time the compromise occurred and was discovered.
- A description of the compromise, the categories of personal information and data subjects affected, and approximate numbers.
- Likely consequences; the measures taken and planned; the measures data subjects should take.
- Whether data subjects have been notified, how and when; whether any delay was requested (s22(3)) and why.
- Contact details for follow-up. **[counsel to confirm the portal's current fields]**

**T4: Data-subject notice** (s22(5) contents **[counsel to confirm]**): a description of the possible consequences; the measures taken or intended; what the recipient should do to protect themselves; and, if known, the identity of the unauthorised person who may have accessed or acquired the information. Written in plain language, in the recipient's language where possible, and sent through the channel the Regulator permits.

**T5: Status update**
```
[SEV-<n>] <title> — update <#> at <UTC>
Status: investigating / contained / recovering / resolved · Changed since last update: <…>
Customer impact now: <…> · Next steps: <…> · Next update: <UTC>
```

**T6: Reply to a foreign legal demand** (R4; sent only after counsel approves)
```
We have received your request dated <date>, reference <ref>. The data you describe is controlled by our customer,
to whom requests should be directed. We are not able to respond further at this stage. Please direct correspondence
to <counsel of record>.
```

## 5. Runbooks

### R1. Personal-information breach (POPIA s21–s22)
- **Trigger:** reasonable grounds to believe personal information was accessed or acquired by an unauthorised person (s22(1)), whether it was found by Vigil, a customer, a subprocessor or a researcher.
- **Severity:** Sev-1 for special PI, children's PI, large volumes or an ongoing compromise; otherwise Sev-2.
1. **Contain:** revoke the affected sessions, tokens and credentials; isolate the affected hosts or tenant; block the exfiltration path; preserve evidence (§3.2).
2. **Scope it from the audit chain:** which tenants, datasets, marking sets and data subjects, when, and through which path. Establish whether Vigil is the operator (the normal case) or the responsible party.
3. **Notify the customer** with T2 immediately, and within 24 hours at most (s21).
4. **Support the responsible party's s22 filing:** hand over the incident pack (T3) for the **eServices portal**, since email notifications have not been accepted since 1 April 2025, and help draft the data-subject notices (T4). Any delay in notifying data subjects is only on the grounds s22(3) allows **[counsel to confirm]**.
5. Where Vigil is the responsible party, its Information Officer files on the portal directly.
6. Check the other regimes: Cybercrimes Act s54 for bank and telecoms clients (once commenced), and the GDPR where EU data subjects are affected (§3.1).
7. Eradicate, recover, rotate every exposed secret and key (SECURITY_AND_COMPLIANCE §10.3), then write the post-mortem (§7).
- **Exit:** notifications done and logged; root cause fixed with a regression test.

### R2. Marking or policy bypass
- **Trigger:** a conformance or fuzzing failure, a red-team finding, a customer report, or audit events showing data returned above the caller's context. An unapplied `Decision` obligation also counts (BACKEND_GUIDE §9.1).
- **Severity:** Sev-1 if production data was exposed; Sev-2 if it was caught before exposure.
1. **Fail closed at once:** disable the affected endpoint or query path, or apply a Warden deny policy to the affected object types. Accept the outage rather than keep leaking.
2. **Enumerate every exposure from the audit ledger:** query hashes, principals, row counts and marking sets. Any personal information exposed also runs R1.
3. **Fix in the policy core** with a reproduction case added to the conformance suite (≥ 500 cases) and to the differential-fuzzing corpus (P34.4). Release the fix in **every** binding together.
4. Treat it as a critical vulnerability (7-day SLA; SECURITY_AND_COMPLIANCE §19).

### R3. Prompt injection or model misbehaviour
- **Trigger:** an injection-detector alert; unexpected tool calls or staged Actions; output containing exfiltration attempts (URLs, encoded data), defamatory statements, fabricated citations, or refusal failures above clearance.
1. **Suspend** the affected agent application or automation at the Mind gateway, and cancel staged Actions that no one has approved yet.
2. **Quarantine** the source document or record (flag it `injection_suspected`) so it is no longer retrieved.
3. **Reconstruct the session** from Langfuse and the audit ledger: every model call, tool call and result.
4. **Check containment:** egress logs should show no external call (ADR-0004). If data reached a user who should not see it, run R2. If outputs fed decisions, tell the case owners and correct the records through human review (s71).
5. **Learn:** add the case to the injection suite (P15.5), roll back the model or prompt through Relay if it regressed, and resume only once the suites pass.

### R4. Foreign legal demand for data (US CLOUD Act or similar)
- **Trigger:** any subpoena, warrant, order or request from a foreign authority for customer data. It is handled as a Sev-2 legal process, not an outage.
1. **Do not respond substantively. Route it to counsel the same day.** Record it in the legal-demands register.
2. Follow **challenge-and-disclose** (SECURITY_AND_COMPLIANCE §9): redirect the requester to the customer (T6); notify the customer unless lawfully prohibited, and challenge any gag order; challenge demands that are overbroad or conflict with South African law, including POPIA s72; disclose only the minimum, and only when legally compelled after challenges are exhausted.
3. **Be accurate about capability:** where keys are customer-held (sovereign tier, P42), Vigil may be technically unable to produce content. Never claim otherwise.
4. Record every step for the transparency report (GUARDRAILS §8).

### R5. Ransomware
1. **Isolate** at once: segment the network, cordon the affected nodes and suspend the credentials involved. Preserve evidence (§3.2).
2. **Assume exfiltration** (double extortion) until disproved, and run R1 in parallel.
3. **Rebuild from signed Relay bundles** onto clean hosts rather than cleaning compromised ones. Restore data from immutable, in-country backups; verify integrity and the audit chain; replay the erasure log (SECURITY_AND_COMPLIANCE §16).
4. Rotate all credentials and keys that could have been exposed. Report the offence to SAPS on counsel's advice.
5. There is a **presumption against paying a ransom**. Any decision is reserved to the board, with counsel, because of legal and sanctions risk.

### R6. Supply-chain compromise
- **Trigger:** a malicious or compromised dependency, a poisoned base image, a compromised CI runner or a leaked signing key.
1. **Freeze releases:** suppress and recall the affected versions on every Relay channel (P21).
2. **Find the blast radius** by searching the SBOMs for the affected components and digests.
3. **Revoke** the compromised signing keys and CI secrets. Block the affected digests in Kyverno at every site.
4. **Rebuild** from pinned, verified sources with new keys. Ship a signed advisory and patch bundle to air-gapped sites.
5. Notify the customers running affected versions, and review the dependency policy (ADR-0003) in the post-mortem.

### R7. Edge node or field device lost or stolen
1. **Revoke** the device's certificates, NATS credentials and device-bound keys on the server, which makes its encrypted cache and spool unreadable. Queue a remote wipe for its next contact (BACKEND_GUIDE §6; P25, P27).
2. Invalidate the sessions of every user of the device, and rotate the site credentials it held.
3. **Assess what was on it:** the markings-filtered cache and the spool contents. Encryption at rest lowers the risk, but if personal information may have been accessed, run R1.
4. Tell the customer, report the theft to SAPS, provision a replacement from a signed image, and review physical controls (CIPA sites: SECURITY_AND_COMPLIANCE §6).

### R8. Client misuse or red-line violation
- **Trigger:** audit anomalies (purpose mismatches, bulk person queries outside a case, repeated justification boilerplate), a whistle-blower report, an oversight-board referral or press enquiries.
1. **Preserve** the audit evidence and get counsel's review. Brief the customer's executive sponsor.
2. **Suspend** the affected capability or tenant under the contract's suspension rights: revoke the gated-module grants in Warden, and disable modules through Relay (GUARDRAILS §9). Unlawful processing of personal information also runs R1.
3. **Inform the oversight board within 5 working days**, and record the case for the transparency report.
4. Resume only after a remediation plan is approved by the lead architect and counsel (and by the board, for security-sector customers).

### R9. Write-back failure causing operational harm
- **Trigger:** wrong or duplicate records in a system of record (for example SAP work orders), wrong slot advisories sent to transporters, missing notifications, or a webhook that succeeded before a failed commit (BACKEND_GUIDE §3.3).
- **Severity:** Sev-1 if a safety-critical operation (grid, rail, port) is affected, in which case the customer's own safety procedures take precedence.
1. **Stop the damage:** disable submissions of the affected Action type and its automations; pause or terminate the related Temporal workflows.
2. **Reconcile** using the edit log, idempotency keys and the system of record's own logs. Record the discrepancy list.
3. **Correct only through compensating Actions**, which are audited. Never edit a database by hand.
4. Tell the system-of-record owners and every affected party (for example corrected advisories to transporters), then re-enable behind a chaos test that reproduces the failure (P8 Done-when).

## 6. Admin branch-protection bypass during incidents (BRANCHING_STRATEGY §6)

- Only **human repository admins** may bypass branch protection, only themselves, and only during a **declared Sev-1 or Sev-2** incident, when a `hotfix/<slug>` pull request cannot land in time.
- Within **24 hours** the admin records the bypass in `docs/AI_COMMUNICATION.md` as a ⚠️ WARNING with the commit SHA and the incident reference, and opens a follow-up PR with tests.
- **AI agents are never authorised to bypass, to merge, or to ask an admin to bypass on their behalf.** An agent that believes a bypass is needed writes the case in AI_COMMUNICATION and stops.
- Every bypass is listed in the incident's post-mortem.

## 7. Blameless post-mortem

Required for every Sev-1 and Sev-2 within **5 working days**, and optional for Sev-3. File it as **`docs/reviews/POSTMORTEM_<YYYY-MM-DD>_<slug>.md`** through a PR (docs/reviews/README.md). Blameless means we describe systems and decisions, not culprits. Names appear only in roles and in the action items people have agreed to own. No personal information about data subjects goes in the document.

```markdown
<!-- Written by <Name> (<AI>) on YYYY-MM-DD — Post-mortem for <incident reference> -->
# Post-mortem: <title> (<SEV-n>, <incident reference>)
**Status:** Draft / Final · **IC:** <name> · **Date of incident:** <UTC> · **Author(s):** <names>

## Summary
<Three sentences: what happened, the impact, how it was resolved.>

## Impact
- Tenants and deployments: <…> · Duration: <start–end UTC> · Data affected: <categories, counts; "none" if none>
- Notifications made: customer <time> · Information Regulator (eServices) <time / n.a.> · data subjects <time / n.a.> · other <…>

## Timeline (UTC)
| Time | Event / decision / action | Evidence |
|---|---|---|

## Root cause and contributing factors
<Technical and organisational causes. Ask "why" until the answers become actionable.>

## What went well · What went badly · Where we got lucky

## Action items
| # | Action | Owner | Phase / territory | Due | Tracking (AI_COMMUNICATION ID / PR) |
|---|---|---|---|---|---|

## Controls check
- Warden and audit worked as designed? · Red lines and gated modules unaffected? · Branch-protection bypasses (with SHAs): <…>
- Regression tests added: <links> · Runbook changes needed: <…>
```

## 8. Exercises

- **Quarterly tabletop exercises** that rotate through R1–R9, with at least one AI-related scenario (R3) each year.
- A **breach-runbook rehearsal with each customer** before go-live, and within the previous 90 days (LAUNCH_CHECKLIST B2).
- Restore drills every quarter and remote-wipe drills for edge fleets every six months, both with recorded results.
- Findings from exercises are tracked like post-mortem action items.
