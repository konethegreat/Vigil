<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Security and regulatory specification: POPIA, GDPR, PAIA, cyber, surveillance and sovereignty law; encryption, identity, audit, classification, SDLC, vulnerability SLAs, certification and the compliance-by-design matrix (P0) -->

# Vigil — Security & Compliance Specification

> **Status:** P0 v1.0 (2026-09-25). Changes need review by the lead architect **and** counsel (File Ownership Master Index).
> **Legal basis:** [`research/appendix/04-legal-regulatory-ethics.md`](research/appendix/04-legal-regulatory-ethics.md) (cited below as "A04 §x") and [dossier §9–§10](research/VIGIL_RESEARCH_DOSSIER.md#9-constitutional-by-design-the-legal-and-ethical-architecture).
> **Not legal advice.** This specification turns research into engineering controls. **Counsel must confirm every legal point before anyone relies on it.** Points the research could not verify are marked **[counsel to confirm]**, and §23 lists them together.
> **Companion documents:** [`SYSTEM_COMPLIANCE_GUARDRAILS.md`](SYSTEM_COMPLIANCE_GUARDRAILS.md) (red lines, gated modules, AI and scraping rules), [`INCIDENT_RESPONSE.md`](INCIDENT_RESPONSE.md), [`LAUNCH_CHECKLIST.md`](LAUNCH_CHECKLIST.md), [`ARCHITECTURE.md`](ARCHITECTURE.md) 🧊.

## 1. Roles and accountability

| Role | Who | Duty |
|---|---|---|
| **Operator** (POPIA s1, s20–21) | Vigil, in the normal case: it processes for a client that decides the purposes and means | Process only on the client's authority, in confidence, under a written s21 contract with s19-level security; notify the client of any compromise **immediately** (A04 §1.3) |
| **Responsible party** | The client; **and Vigil itself** if it decides purposes or means, for example through a cross-tenant identity graph, pooled model training or derived data products (A04 §1.3) | Every POPIA duty, including prior authorisation (s57). **Design rule: Vigil stays an operator.** Strict tenant isolation, no cross-tenant identity graph, and no pooled training on personal information (A04 "Implications" 1). |
| **Information Officer** | To be named by the founders (dossier §18 Q5; P0 task 0.7) and registered with the Information Regulator | POPIA compliance programme, PAIA manual, breach decisions, Regulator liaison. The Regulator has issued enforcement notices to organisations with no registered Information Officer (A04 §1.8). **No go-live without one** (LAUNCH_CHECKLIST). |
| **Security lead** | DevSecOps/SRE lead (dossier §14) until a security architect is hired | Controls in this document; vulnerability management; incident command for security incidents |
| **Counsel of record** | To be named (dossier §18 Q5) | s57 packs, s6(1)(c) opinions, export classification, foreign legal demands |

## 2. POPIA (Protection of Personal Information Act 4 of 2013)

### 2.1 The eight conditions for lawful processing, and Vigil's controls

The section ranges below follow the Act's standard structure **[counsel to confirm the mapping]**.

| Condition | Sections | Vigil control |
|---|---|---|
| 1 Accountability | s8 | Information Officer; this specification; the compliance evidence programme (P36) |
| 2 Processing limitation | s9–s12 | Lawful-basis tag on every dataset and record (§2.3); minimality in retention policies (P4.5); objection handling |
| 3 Purpose specification | s13–s14 | Declared purpose on every token and query (Warden, P3.5); retention schedules and deletion (§16) |
| 4 Further processing limitation | s15 | A compatibility gate at every join across sources; the s15(3) ground is logged (§2.3) |
| 5 Information quality | s16 | Data-quality contracts (P12.4); provenance on every property; human review of low-confidence entity-resolution links (P16.5) |
| 6 Openness | s17–s18 | Records of processing per deployment; notification text supplied to clients; publishable deployment configuration (GUARDRAILS §6) |
| 7 Security safeguards | s19–s22 | §10–§20 of this document; operator agreements; breach runbook (INCIDENT_RESPONSE R1) |
| 8 Data subject participation | s23–s25 | Access, correction and deletion tooling (§2.11) |

### 2.2 The s6(1)(c) exclusion: default posture

- The exclusion covers processing "by or on behalf of a public body" for national security, defence, public safety or crime-related purposes, **and only** "to the extent that adequate safeguards have been established in legislation" (A04 §1.1). The Regulator found SAPS in breach in 2023 and restated in September 2026 that SAPS "is not excluded" (A04 §1.1).
- **Vigil ships in full-POPIA mode by default.** An exclusion claim (s6(1)(c) or a Regulator exemption under s37) is a **per-dataset exception**, signed off by the *client's* counsel, recorded as a Warden dataset attribute and shown in the deployment's published configuration. Private buyers never get this exclusion (A04 §1.1).
- Where an exclusion removes the Regulator's oversight, Vigil's own obligations increase: the gated-module, audit and oversight controls in GUARDRAILS §3–§8 apply in full (A04 §7 analysis).

### 2.3 Lawful basis, collection and further processing (s11, s12, s15)

- **s11:** every connector (P11) and every dataset (P4) declares a lawful-basis tag (consent, contract, legal obligation, the data subject's legitimate interest, a public-law duty, or the legitimate interests of the responsible party or a third party) and a purpose tag. **A record without a lawful basis, purpose tags and a terms-register reference (`terms_ref`) is rejected at ingestion** (A04 §1.2; Blueprint §9.5). The last three grounds allow objections (s11(3)), so objection handling is part of §2.11.
- **s12:** collection is direct from the data subject unless an exception applies (for example public records, or law enforcement by a public body). The connector records which exception applies.
- **s15:** fusion is legally a chain of compatibility decisions, one per disclosing organisation (A04 §1.2). Tags follow records into joins. A join across sources whose purposes are not compatible is refused unless a recorded s15(3) ground applies (for example public-record data, or prevention of prejudice to the maintenance of the law *by a public body*). The law-enforcement limb does **not** cover a private customer's commercial risk scoring (A04 §1.2).

### 2.4 Security safeguards, operators and breach notification (s19–s22)

- **s19:** reasonable technical and organisational measures, based on risk identification and regular verification. Everything in §10–§20 serves this. The DoJ lapsed-licence fine (R5m), the TransUnion password findings and the Dis-Chem operator breach are the design lessons: licence-expiry alarms, enforced MFA and access reviews, and compliant operator contracts (A04 §1.3).
- **s20–s21:** Vigil signs the standard **s21 operator agreement** (P36.3) with every client. It flows s19-level security down to subprocessors (§17).
- **s22:** the responsible party notifies the Regulator and data subjects "as soon as reasonably possible". **Since 1 April 2025, notifications to the Regulator go through its eServices portal, not email** (A04 §1.3).
- **Vigil's internal notification SLA:**
  - staff to the security lead and Information Officer: **immediately, and within 1 hour** of suspecting a compromise;
  - Vigil (as operator) to the affected client: **immediately, and in any case within 24 hours** of becoming aware. The 24 hours are an outer limit, not a target, because s21 says "immediately";
  - Vigil acting as responsible party: the Regulator (eServices portal) and data subjects as soon as reasonably possible.
- The runbook is INCIDENT_RESPONSE R1. Incident packs are kept ready in the format the eServices portal needs (A04 §10).

### 2.5 Special personal information and children (s26–s35)

- **s26** prohibits, by default, processing information on religion, race, trade-union membership, political persuasion, health or sex life, **biometrics** and **criminal behaviour**. s27 provides general authorisations, and s33 lifts the prohibition on criminal and biometric data for bodies charged by law with applying criminal law. s34–s35 set a parallel regime for children (A04 §1.4).
- Ordinary features pull these in: face embeddings are biometric; "suspect" labels and scores implying criminality are arguably criminal-behaviour information; feeds from SASSA, schools and hospitals contain children's data (A04 §1.4).
- **Controls:**
  - a special-PI classifier runs at ingestion and applies the markings `SPECIAL-PI`, `CRIMINAL-CONDUCT` or `CHILD` (VIGIL_DOMAIN_BLUEPRINT §4.4);
  - their values are stored as `cipher_text` under **class-specific key channels** (Blueprint W9; §10 below);
  - a recorded s27, s33 or s35 ground is required before ingestion, and first access needs a justification (Blueprint W7);
  - **children's data is blocked by default** and needs the `CHILD_DATA` gate (GUARDRAILS §3);
  - risk scores and indicators about people are labelled "risk indicator, not finding", and are treated as potentially criminal-behaviour information.

### 2.6 Prior authorisation (s57–s59): the critical path

**Triggers** (A04 §1.5):

| Trigger | What it catches in Vigil |
|---|---|
| s57(1)(a): processing unique identifiers for a purpose other than the one intended at collection, **with the aim of linking** them with information held by other responsible parties | Multi-party entity resolution: ID, account, policy, employee and customer numbers, IP addresses; probably vehicle registrations and VINs **[counsel to confirm]** |
| s57(1)(b): information on criminal behaviour or unlawful or objectionable conduct, on behalf of third parties | Shared watch-lists, industry crime and fraud pools, LPR "hits" sent to third parties (A04 §3) |
| s57(1)(c): credit reporting | **Never built** (red line RL-7, GUARDRAILS §2) |
| s57(1)(d): transfer of special or children's PI to a country without adequate protection | Cross-border deployments and offshore support |

**Process** (the client, as responsible party, is the applicant; Vigil supplies the pack):

1. **Detect** the trigger during design review or the personal-information impact assessment (PIA) for the deployment.
2. **Prepare the pack** (below) with the client's Information Officer and counsel.
3. **File** it with the Information Regulator under its 2021 Guidance Note on prior-authorisation applications **[counsel to confirm the current form and submission channel]**.
4. **Wait.** Under s58 the Regulator has 4 weeks to say whether it will investigate further, and no more than 13 further weeks to finish. **Processing may not start until then.** Budget 4–17 weeks (A04 §1.5).
5. **Record** the authorisation in `warden.gate_authorisation` (gate `S57_1A_LINKAGE`, with dual approvers, counsel reference and validity dates; DATA_MODEL §3.6). Only then does the ER job's gate check pass, and only for the authorised scope. Without it the job refuses with `LINKAGE_LOCKED` (HTTP 423). When the authorisation expires or is revoked, the affected links become `SUSPENDED` (Blueprint §5.9). Until counsel rules otherwise, Vigil treats public-register linkage as engaging s57(1)(a).
6. **Re-apply** if the processing later departs from what was authorised (s57(4)).
7. **Strategic option:** a sector code of conduct under s57(3) (for example for mining crime intelligence) removes the need for repeated authorisations.

Processing without the required notice is an **offence** (s59: a fine or up to 12 months' imprisonment; A04 §1.5).

**Standard pack contents** (template owned by P36.3):
- the applicant and its Information Officer; the operator (Vigil) and its role;
- purposes, and the s57 trigger(s) relied on;
- a data map: sources, identifiers, flows, storage locations, retention;
- the **linkage rules** exported from the ER engine: blocking rules, comparators, thresholds and review policy (P16);
- the lawful basis per source and the s15 compatibility analysis;
- the PIA and the residual-risk register;
- safeguards: Warden markings, RLS pushdown, purpose binding, audit, encryption and key custody, human review (s71), gated-module controls;
- the cross-border analysis (s72), including the "no prompt leaves SA" test evidence (P14.4);
- operator and subprocessor agreements (s21); data-subject rights procedures (s23–s25); breach procedures (s22);
- security certification status (§21).

### 2.7 Direct marketing (s69)

- Vigil is not a marketing product. The comms gateway (P28) sends operational alerts over business-owned channels only, and records opt-in and consent (P28.4).
- The amended POPIA Regulations (in force 17 April 2025) require **written consent for electronic direct marketing, including recorded calls** (A04 §1.9), and the Regulator has fined a company for unlawful direct marketing (A04 §1.8).
- Rule: the gateway refuses any template or campaign classified as marketing unless a consent record exists for each recipient. Political messaging is refused outright (RL-4). **[counsel to confirm the s69 detail: the research did not analyse its text]**

### 2.8 Automated decisions (s71)

- s71(1) bars decisions with legal consequences, or that affect a person "to a substantial degree", based **solely** on automated processing intended to profile them. The exceptions (s71(2)–(3)) need appropriate measures, including a chance to make representations and "sufficient information about the underlying logic" (A04 §1.6).
- **Controls:**
  - Every Action type whose `outcome_class` is `ADVERSE` or `LEGAL_EFFECT` (referral for arrest, supplier blacklisting, access denial) must set `requires_human_approval`; OMS rejects a pack that breaks this invariant (Blueprint §1.7). Agents may only *stage* such Actions (P8.5, P15.3; INSTRUCTIONS §9.4).
  - Each decision keeps a **decision record**: the inputs read, the risk-indicator explanation, model versions, the agent's proposal against what the human applied, and the approver's justification (Blueprint §6.5, §10).
  - Deployments include a channel for data subjects to make representations.
  - For state clients, Constitution s33 and PAJA's duty to give reasons point the same way (A04 §1.6).

### 2.9 Cross-border transfers (s72)

- s72 allows a transfer where the recipient is bound by adequate law, binding corporate rules or a binding agreement; where the data subject consents; where it is necessary for a contract; or where it benefits the data subject. There is **no adequacy list** yet (A04 §1.6).
- **Controls:**
  - storage is geo-fenced to the tenant's residency pod. The only cross-pod flow is an `export-aggregate` Action producing non-personal `PUBLIC` aggregates, and `SOVEREIGN` tenants' data never leaves South Africa, not even as aggregates, without the data owner's written instruction (DATA_MODEL §13);
  - a **transfer broker** requires a legal-basis selection and writes an export log for every transfer;
  - **"No prompt leaves SA"** is a tested guarantee (P14.4). Hyperscaler LLM APIs are never called from the sovereign tier (Bedrock in Cape Town routes globally; AI_COMMUNICATION MSG-012);
  - **remote access from outside the country counts as a transfer** [counsel to confirm]. Support for sovereign tenants is given from inside the country, or under a recorded s72 basis the client has approved;
  - special PI or children's PI leaving the country also triggers s57(1)(d) (§2.6) and the gated module (GUARDRAILS §3, G7).

### 2.10 Liability and fines (s99, s109)

- s99(1) creates civil liability "whether or not there is intent or negligence". s109 allows administrative fines of up to R10 million, and some offences carry imprisonment **[counsel to confirm: A04 §1.7 marks these as not verified in session]**.
- After a fusion breach, aggregate strict liability would dwarf the fine cap. Contracts set liability caps and indemnities, and insurance is sized for aggregate exposure (A04 §1.7; dossier risk R5).

### 2.11 Data-subject participation (s23–s25)

- Tooling finds a data subject's records across a tenant's datasets, **within the tenant and under the request's own lawful basis**, so that access, correction and deletion requests can be answered. It never builds a cross-tenant profile.
- Corrections are Actions with audit. Deletions follow §16.

## 3. GDPR: only where EU data subjects are processed

- The research did not cover the GDPR (A04 addresses the EU AI Act and the Council of Europe convention). The GDPR matters only if a deployment processes personal data of people in the EU, for example an EU-based client or monitoring of people in the EU **[counsel to confirm territorial scope per deployment]**.
- In such a deployment Vigil adds: an Article 28 processor agreement, records of processing (Art. 30), Art. 32 security (this document), breach notification to the supervisory authority within 72 hours (Art. 33), a DPIA (Art. 35), and a Chapter V transfer mechanism, because South Africa is not known to have an EU adequacy decision **[counsel to confirm]**.

## 4. PAIA (Promotion of Access to Information Act)

- Vigil maintains a **PAIA manual** and a registered Information Officer. The Regulator has issued enforcement notices for missing Information Officers and PAIA failures (A04 §1.8) **[counsel to confirm the manual requirements for Vigil's entity type]**.
- **Contract as if every security contract will become public:** the Regulator ordered the State Security Agency to release records of payments to service providers (A04 §1.8).
- Tooling retrieves the records a PAIA request needs, with audit (A04 §10).

## 5. Cybercrimes Act 19 of 2020

- The offences (unlawful access, interception of data, interference) have been in force since 1 December 2021. **All security testing and OSINT collection happen only under written authorisation** from the system or data owner, following the authorised-testing protocol (§20; A04 §2).
- **Section 54 has not commenced** **[counsel to confirm for 2026]**. Once it does, electronic communications service providers and financial institutions must report prescribed offences to SAPS within 72 hours and preserve evidence. Vigil keeps a **ready-built s54 workflow** for bank and telecoms clients (INCIDENT_RESPONSE §3.1 and R1).

## 6. Critical Infrastructure Protection Act (CIPA) and MISS

- **CIPA:** the person in control of critical infrastructure implements prescribed security measures and appoints an SSA-vetted security manager (s24), and unlawfully disclosing information about security measures is a crime (s26). Some sections are in force; full commencement and the final regulations are **[counsel to confirm]** (A04 §2).
- **MISS** (Cabinet, 1996): the classifications Restricted, Confidential, Secret and Top Secret, and clearance requirements **[counsel to confirm: the MISS text was not fetched]**.
- **Controls:** a `SECURITY-MEASURE` marking (Blueprint §4.4) for sensor, camera, patrol and access-control configurations at critical-infrastructure sites (never on the commercial cloud tier); vetted staff for those sites; clearance sponsorship through the first security client; the classified enclave profile (P41).

## 7. Interception: RICA, *amaBhungane* and GILAB

- Interception, and access to real-time or archived communication-related information, need a direction from the designated judge under RICA. The Constitutional Court held that the NCC's **bulk interception "is not authorised by law"** (*amaBhungane* [2021] ZACC 3). The RICA Amendment Bill was referred back in November 2024; GILAB (assented 28 March 2025) allows judge-authorised bulk interception, but its regulations are pending **[counsel to confirm the 2026 status of both]** (A04 §3).
- **Stance: Vigil has no interception features, ever, and never ingests bulk-interception products** (red line RL-1). The communications-metadata module is gated and needs a direction or subpoena reference (RICA; Criminal Procedure Act s205), plus a post-surveillance notification tracker (GUARDRAILS §3, G1).

## 8. ECT Act, Chapter V: cryptography providers

Suppliers of cryptography products or services must be on the DCDT register before supplying. Enforcement is dormant (A04 §2). Vigil ships encryption, so **it registers defensively** before its first commercial deployment (LAUNCH_CHECKLIST).

## 9. Sovereignty: the National Data and Cloud Policy and the US CLOUD Act

- **NDCP ¶15.4.2:** government data "pertaining to the protection and preservation of national security and sovereignty… shall be stored only in digital infrastructure located within the borders of South Africa". ¶15.1.3 makes SITA the authority that sources government cloud, and ¶15.2.5 defines four data tiers (A04 §4).
- **CLOUD Act (18 U.S.C. §2713):** US providers must disclose data in their "possession, custody, or control", wherever it is stored. South Africa has no executive agreement with the US. A local hyperscaler region fixes *where* data sits, not *whose law* reaches it (A04 §4). Microsoft France told the French Senate it "cannot guarantee" non-disclosure (appendix 06 §2).
- **Controls:** the sovereign tier runs in South African colocation, on-premises or air-gapped, with no foreign control plane, customer-held keys (P42) and a South African operating entity (ADR-0004). The commercial cloud tier is labelled "hyperscaler-connected" and carries a CLOUD Act disclosure in its contract (ARCHITECTURE §7).
- **Challenge-and-disclose policy** for any foreign legal demand (runbook: INCIDENT_RESPONSE R4):
  1. **Redirect** the requester to the customer, who controls the data.
  2. **Notify** the customer promptly, unless a court order lawfully prohibits it, and challenge any such gag order.
  3. **Challenge** the demand where it is overbroad, conflicts with South African law (including POPIA s72) or lacks jurisdiction.
  4. **Disclose the minimum** only when legally compelled after challenges are exhausted, and only what the order strictly requires.
  5. **Report** every demand, challenge and disclosure in the annual transparency report (GUARDRAILS §8).

## 10. Encryption

### 10.1 At rest: AES-256-GCM tenant data encryption with envelope keys

**Key hierarchy:** an HSM- or KMS-held root → a **key-encryption key (KEK) per tenant** → **data-encryption keys (DEKs) per dataset** (and per special-PI store), wrapped by the tenant KEK. Plaintext DEKs exist only in process memory, and only while in use.

| Location | Protection | Key |
|---|---|---|
| Object storage (SeaweedFS / S3 API): Iceberg data and manifests, documents, media, embeddings | **AES-256-GCM**, envelope-encrypted per dataset (Iceberg v3 table encryption keys, or server-side encryption with Vigil-supplied keys) | Dataset DEK wrapped by the tenant KEK |
| Sensitive properties (special PI, children's PI, identifiers such as ID numbers) | **Application-level AES-256-GCM** through the `cipher_text` base type (envelope `vgc1:<channel>:<key_version>:<nonce>:<ciphertext>`; Blueprint §1.4, W9), so a database compromise yields ciphertext. Joins use paired pseudonyms (Blueprint §4.6). | Class-specific key channel; decryption needs a channel grant |
| PostgreSQL, OpenSearch, ClickHouse and Kafka volumes | Block-device encryption (LUKS2, **AES-256-XTS**), on per-tenant volumes where the tier requires it | Volume key wrapped by the tenant KEK (dedicated volumes) or by the platform KEK |
| Backups | AES-256-GCM before the data leaves the host; stored in-country only | Backup DEK wrapped by the tenant KEK |
| Field-app cache, edge spool | AES-256-GCM | Device-bound key (Android Keystore / TPM), revocable (INCIDENT_RESPONSE R7) |

- *Why block devices use XTS:* community PostgreSQL has no built-in transparent data encryption, and GCM is not suited to sector-level disk encryption. So Vigil combines volume encryption with application-level GCM for the fields that matter most. Every cipher Vigil itself controls is AES-256-GCM.
- **GCM rules:** 96-bit random nonces; rotate each DEK before it reaches 2³² encryptions (the NIST SP 800-38D limit for random nonces) or after 90 days for new writes, whichever comes first; **additional authenticated data** binds each ciphertext to its tenant, dataset, object RID and property, so ciphertexts cannot be swapped between records.
- Use vetted libraries only; never hand-rolled cryptography. Use FIPS 140-validated modules where a customer requires them (RKE2 FIPS profile, P35.1).

### 10.2 In transit

- **TLS 1.3** on every external endpoint. TLS 1.2 is allowed only by documented exception for a legacy system of record (a writeback target), with ECDHE and AEAD cipher suites.
- **mTLS between all services**, with short-lived workload certificates issued inside the boundary (P35.2). NATS leaf links use mTLS (P25.3). Edge agents and the field app pin the deployment's CA.
- HSTS on the Cockpit; no plaintext listeners except health probes on the management network.

### 10.3 Rotation, custody and HYOK

| Key | Rotation | Custody |
|---|---|---|
| Tenant KEK | Yearly, on customer request, or immediately on suspected compromise. Rotation re-wraps DEKs; no bulk re-encryption. | KMS/HSM. **Customer-held** where contracted (sovereign tier) |
| Dataset DEK | ≤ 90 days or the GCM limit, for new writes | Wrapped only |
| TLS / workload certificates | External ≤ 90 days; workload ≤ 24 hours | In-boundary CA |
| Signing keys (cosign) | Yearly; offline root | HSM, dual control |
| Connector and service secrets | Per source policy, with rotation hooks (P11.2) | KMS references only (BACKEND_GUIDE §9.3) |

- **HYOK (P42):** keys never leave the customer's HSM (PKCS#11). **Crypto-shredding** enforces retention expiry and erasure by destroying keys. The P42 Done-when: revoking a customer KEK makes the tenant's data unreadable within the documented window.

## 11. Identity and privileged access

- **Keycloak** (OIDC) is the only identity provider inside the boundary (P3.4). Federation with customer IdPs arrives in P38.
- **MFA is mandatory for privileged roles** (P3.4): administrators, marking administrators, approvers of gated modules and of `requires_human_approval` Actions, and Relay operators. These roles use phishing-resistant authenticators (WebAuthn/FIDO2). **The sovereign tier requires MFA for every user.**
- **Privileged access management:** no standing production administrator access. Elevation is just-in-time, approved by a second person, limited to 8 hours at most, recorded and audited. Production data access by Vigil staff needs a customer-approved ticket.
- **Break-glass:** two sealed accounts per site. Their credentials are split between two custodians (dual control) and stored offline. Any use pages the security lead, is treated as a Sev-2 incident until reviewed, is fully audited, and is followed by credential rotation and a review within 24 hours.
- **Lifecycle:** leavers lose access within 24 hours; access is reviewed quarterly (the TransUnion lesson, A04 §1.3). Service accounts use workload identity, never shared passwords.
- **Passwords**, where used at all: at least 14 characters, checked against an offline breached-password list, with login rate limiting. No forced periodic rotation.

## 12. Authorisation

- **Mandatory controls come first.** Warden markings are **conjunctive** (the user must hold every marking), classification is hierarchical, and releasability is **disjunctive**. Purpose and case checks apply to every read. **Deny wins** (ADR-0005; appendix 05 §4k). The formal rules are Blueprint §4.3 (W1–W12), including **no existence oracle**: an invisible resource is indistinguishable from an absent one (HTTP 404, W11).
- **Discretionary access** (organisations, spaces, projects, roles, sharing) lives in **OpenFGA**. A role grant can never override a missing marking.
- **Enforcement is pushed down** into Postgres RLS, OpenSearch DLS/FLS, Polaris-vended scoped storage credentials and ClickHouse row policies, all compiled from the same policy core. Traversal filters in-process (ARCHITECTURE §5).
- **Labels propagate** through lineage: the union of markings, the maximum classification, and the intersection of releasability and purposes; a derivation that nobody could lawfully use is blocked (Blueprint W5). **Making a label less restrictive is a `declassify-resource` Action**, requested by one marking administrator and approved by a second (Blueprint W8; dossier §6.2).
- **Agents are bound by the same policies as the humans they act for** (ARCHITECTURE §6.4; GUARDRAILS §4).

## 13. Audit

- **Hash-chained and append-only** from day one (P3.7; DATA_MODEL §3.5). One chain per tenant, written by a single chainer: `hash = SHA-256(prev_hash ‖ JCS(event))`, signed with Ed25519 (key in KMS/OpenBao in v1, the HSM from P42). Update, delete and truncate are blocked by triggers. External anchoring and an independent verifier arrive in P40.
- **Every Warden decision emits an audit event** (P3 Done-when). Every read is audited with the query hash, purpose, case, justification reference, row count and marking set (ARCHITECTURE §6.2).
- **Audit events never contain property values**, so erasure never has to touch the ledger (§16).
- Events are exported to the customer's SIEM by pull API and by stream (P3.7). Access to the audit log is itself audited.
- **Retention:** 7 years by default (DATA_MODEL §6.4, to be confirmed at P36), exported monthly to Iceberg and anchored before partitions are detached **[counsel to confirm per sector; POPIA s14 limits retention to what is necessary]**.
- If the ledger cannot be written, data-bearing requests fail closed (BACKEND_GUIDE B4).

## 14. Data classification

The normative classification ladder, marking vocabulary and label rules live in **[`VIGIL_DOMAIN_BLUEPRINT.md`](VIGIL_DOMAIN_BLUEPRINT.md) §4**. This section repeats the ladder only to connect it to hosting. **If the two ever differ, the blueprint wins**, and the difference is filed as a 🐛 BUG.

| Level | Code | NDCP ¶15.2.5 tier | MISS | Examples (Blueprint §4.2) | Permitted topologies (ARCHITECTURE §7) |
|---|---|---|---|---|---|
| 0 | `PUBLIC` | public / open | unclassified | OCDS releases, AIS positions, gazettes | All, including the commercial cloud tier |
| 1 | `RESTRICTED` | confidential / sensitive | Restricted | Internal operational data; commercial terminal parameters | All; the commercial tier for commercial tenants only |
| 2 | `CONFIDENTIAL` | confidential / sensitive | Confidential | Personal information, case files, restricted-supplier person entries | Sovereign DC, customer on-prem, appliance, edge. The commercial tier only without the markings listed below |
| 3 | `SECRET` | secret | Secret | Classified-enclave data only | **Classified-enclave profile only (P41)**, air-gapped |
| 4 | `TOP_SECRET` | top secret | Top Secret | Classified-enclave data only | **Classified-enclave profile only (P41)**, air-gapped |

- Each deployment has a ceiling, `max_classification`, which defaults to 2. Levels 3 and 4 are refused outside a classified-enclave profile (Blueprint §4.2; DATA_MODEL §3.1). The MISS mapping is to be confirmed at P3 and P41 **[counsel to confirm]**.
- **Markings apply alongside any level** (Blueprint §4.4): `PII`, `SPECIAL-PI`, `CRIMINAL-CONDUCT`, `CHILD`, `SECURITY-MEASURE`, `CASE-<n>`, and organisation, project and customer-defined markings. Releasability principals (`REL-ZA`, `REL-SADC`, …) and purpose tags complete the label.
- **Commercial "hyperscaler-connected" tenants cannot register** `SPECIAL-PI`, `CHILD`, `CRIMINAL-CONDUCT` or `SECURITY-MEASURE` markings; Warden refuses (DATA_MODEL §13; ADR-0004).
- **NDCP ¶15.4.2 data** (national security and sovereignty) is a hosting rule, not a level. It is held by tenants flagged `SOVEREIGN`, whose data never leaves South African infrastructure (DATA_MODEL §13), whatever its classification.

## 15. Sovereignty controls

- **Egress denied by default:** default-deny NetworkPolicies, an egress allow-list, and a Mind gateway that denies egress unless a route is explicitly configured (INSTRUCTIONS §9.8).
- **"No prompt leaves SA"** is an automated test in every release (P14.4) and a go-live gate (LAUNCH_CHECKLIST).
- **Nothing required from outside the boundary:** the control plane, identity, model registry, map tiles, time (NTP), licence checks, telemetry and updates are all in-boundary or bundled (ADR-0004; appendix 06 §4.2).
- **In-country operations:** support, disaster-recovery sites (Teraco CT2 or Equinix JN1) and backups stay in South Africa. Per-country pods follow the same model (P39).
- **Key custody** follows §10.3. Relay delivers signed bundles into air-gapped sites one way (P20).

## 16. Retention and erasure

- Every dataset has a retention policy (P4.5), set by its purpose's `retention` in `warden.purpose` to the shortest period the purpose allows (POPIA s14; DATA_MODEL §6.4). Expiry runs the erasure path.
- **Erasure (s24)** is an `erase-data-subject` Action (`LEGAL_EFFECT`, two-person approval) whose saga audits counts and hashes only, never values (DATA_MODEL §6.3). It hard-deletes the typed rows; tombstones the values in the edit log through `actions.erase_values()`, the only permitted `UPDATE` of `edit_log`, which keeps the structural record; deletes the matching OpenSearch, ClickHouse and LanceDB data; and removes rows from Iceberg with v3 deletion vectors, with proof hashes on pre- and post-erasure tags (P4 Done-when). The audit ledger holds no values, so it is untouched (§13).
- **Backups age out:** erasure is complete when the last backup containing the data expires, **at most 35 days**, and the response to the data subject says so (DATA_MODEL §6.3–§6.4). A restore within that window must re-apply completed erasures before the system goes back into service.
- After P42, crypto-shredding of per-subject keys replaces value tombstoning.

## 17. Subprocessor management

- A subprocessor register records the name, service, location, data categories, transfer basis (s72), agreement date and review date.
- **Sovereign-tier subprocessors are located in South Africa**, with no foreign-controlled access path.
- Customers get 30 days' notice of a new subprocessor and a right to object. s21 obligations flow down in writing (the Dis-Chem lesson, A04 §1.3).
- Annual review. **Expiry alarms at 60, 30 and 7 days** for every security tool licence and subscription (the DoJ lesson, A04 §1.3).

## 18. Secure software development lifecycle

- **Design:** every phase plan includes a threat model (STRIDE for security, LINDDUN for privacy) and names its Warden touchpoints. A new data source needs a lawful-basis tag and a terms-register entry before code is written (GUARDRAILS §5).
- **Review:** at least one human approval on `develop` and two on `main` (BRANCHING_STRATEGY §5). Changes to Warden, audit, crypto or the red-line suite also need the security lead.

| Control | Tool | Where | Blocks merge |
|---|---|---|---|
| SAST | **Semgrep** (all languages), **Bandit** (Python), plus language linters (clippy, gosec, Error Prone) | `security-scan.yml`, weekly and on PRs | Semgrep WARNING and ERROR findings; Bandit medium-or-higher severity and confidence |
| Secrets | **Gitleaks** | pre-commit + `security-scan.yml` | Any finding |
| Vulnerabilities (dependencies, images, IaC) | **Trivy** (+ `cargo-audit` and `govulncheck` now; `cargo deny` from P1.7) | `security-scan.yml` | Criticals and highs with a fix available; unfixed criticals are tracked with a 7-day review |
| SBOM | **Syft**, one SBOM per artefact | `sbom-license-scan.yml` | A missing SBOM |
| Licences | ADR-0003 policy over the SBOM | `sbom-license-scan.yml` | Red licences; amber without an allow-list entry |
| Provenance | **SLSA** provenance (target: level 3 by P34.3) | `deploy.yml` (from P34) | Missing provenance on a release artefact, once P34 wires it |
| Signing | **cosign** signatures, verified at admission by **Kyverno** | `deploy.yml`, clusters (P20.3) | Unsigned images are rejected |

- **Supply chain:** lockfiles committed (INSTRUCTIONS §6 rule 5), every artefact mirrored, base images owned (`infra/docker/`, no Bitnami) and rebuilt weekly (P1.6), a minimum release age for new npm packages, and a review of the top 50 dependencies every six months (ADR-0003).
- **Never disable a scan to get a green build** (INSTRUCTIONS §10 #14).

## 19. Vulnerability SLAs

Measured from triage, which happens within 1 working day of a report or scanner finding.

| Severity | Examples | Fix or mitigate within |
|---|---|---|
| **Critical** | CVSS ≥ 9.0; a known-exploited vulnerability reachable in Vigil; any Warden or marking bypass | **7 calendar days** (mitigation within 48 hours when exploitation is active) |
| High | CVSS 7.0–8.9 | 30 days |
| Medium | CVSS 4.0–6.9 | 90 days |
| Low | CVSS < 4.0 | 180 days, or the next minor release |

- **Air-gapped and on-prem customers:** a signed patch bundle is published through Relay within the same SLA, and the customer's installation date is tracked to closure.
- **Exceptions** need a written risk acceptance signed by the security lead and the CTO, with compensating controls and an expiry of at most 90 days.
- A marking or policy bypass is also an incident (INCIDENT_RESPONSE R2).

## 20. Penetration testing and authorised testing

- **External penetration test** every year, before the general availability of each major version, and after any significant architecture change. The scope covers the gateway, core, Mind, Relay and the field app (P34.1).
- **AI red team** (prompt injection, exfiltration through tools, marking bypass; P34.2) before every release that changes models, prompts or tools.
- **Warden differential fuzzing** across all bindings: 72 hours without divergence (P34 Done-when).
- **Authorised-testing protocol** (Cybercrimes Act, §5): a written scope and authorisation from the system owner, named testers, time windows, data handling rules and a stop procedure. Customers may commission their own tests under the same protocol.
- Critical and high findings are retested within their SLA. Results go into `docs/reviews/SECURITY_AUDIT.md` (P34).

## 21. Certification roadmap

Aligned with the dossier's 36-month plan (dossier §15):

| Target | Milestone | Evidence owner |
|---|---|---|
| POPIA programme (Information Officer, PAIA manual) | Q4 2026 | Information Officer |
| **ISO/IEC 27001** | Gap analysis Q1 2027 → certified H2 2027 | P36.1 |
| SITA certification engagement | From Phase Zero | GTM lead |
| ECT Act Chapter V registration | Before the first commercial deployment | Counsel |
| **SOC 2** | Type I H1 2028 → Type II H2 2028 | P36.2 |
| **ISO/IEC 42001** (AI management system) | Readiness H2 2028 → certified H1 2029 | P36.4 |
| Per-customer accreditation (classified enclave) | 2029 | P41 |

## 22. Compliance-by-design matrix (adapted from A04 §10)

| Law / obligation | Requirement | Vigil control (technical + process) | Phase(s) | Priority |
|---|---|---|---|---|
| POPIA s6(1)(c) | Exclusion only where statutory safeguards exist | Full-POPIA default; per-dataset exclusion claim signed by client counsel (§2.2) | P3, P36 | P0 |
| POPIA s11, s12, s15 | Lawful basis; compatible further processing | Lawful-basis and purpose tags on every record; compatibility gate at each join; s15(3) ground logged (§2.3) | P4, P11, P12 | P0 |
| POPIA s57(1)(a), s58 | Prior authorisation before linking identifiers | Linkage lock until an authorisation ID is recorded; linkage-rule export for the pack (§2.6) | P16, P3.5, P36.3 | P0 |
| POPIA s57(1)(b) | Criminal-conduct data for third parties | Shared watch-lists off by default; tenant isolation; authorisation gate (GUARDRAILS G5) | P3.5, P22 | P0 |
| POPIA s57(1)(d), s72 | Cross-border transfer; special PI | Geo-fenced storage; transfer broker with legal-basis selector; export logs; "no prompt leaves SA" (§2.9) | P14, P39 | P0 |
| POPIA s19 | Security safeguards | ISO 27001 / SOC 2 path; MFA and PAM; licence-expiry alarms; this document | P3, P34–P36 | P0 |
| POPIA s20–s22 | Operator contract; breach notification | s21 agreement; 24-hour client notice SLA; subprocessor register; eServices-ready incident packs | P36.3, INCIDENT_RESPONSE | P0 |
| POPIA s26–s35 | Special PI; children | Special-PI classifier; separately encrypted stores; s27/s33/s35 ground; children's data excluded by default (§2.5) | P3.1, P5, P42 | P0 |
| POPIA s71; PAJA | No solely automated adverse decisions | `requires_human_approval`; explanation record; channel for representations (§2.8) | P8.5, P15.3 | P0 |
| POPIA s99; PAIA | Strict liability; access to records | Liability caps and insurance; PAIA manual and record-retrieval tooling (§4) | Contracts, P36 | P1 |
| Cybercrimes Act s2–s3, s54 | Lawful access; 72-hour reporting once in force | Authorised-testing protocol; ready s54 workflow (§5) | P34, INCIDENT_RESPONSE | P1 |
| CIPA s24–s26; MISS | Vetting; secrecy of security measures | Cleared enclave team; `SECURITY-MEASURE` marking; accredited air-gapped build (§6) | P37, P41 | P1 |
| ECT Act Ch. V | Cryptography-provider register | Register with DCDT (§8) | Corporate | P2 |
| RICA; *amaBhungane*; GILAB | No bulk interception; judicial directions; notification | No interception features; direction reference for comms metadata; notification tracker (§7) | P43, GUARDRAILS G1 | P0 |
| NDCP ¶15.4.2; CLOUD Act | Onshore sovereign data; foreign compelled access | SA or on-prem hosting; customer-held keys; SA operator; challenge-and-disclose (§9) | P20, P37, P42 | P0 |
| SITA Act s7(3), s7(6); PPPFA / PPA | Procurement channel; certification | SITA certification roadmap; SI partnerships; CSD, tax and B-BBEE plan | GTM | P1 |
| *hessenDATA* safeguards | Thresholds; limits on data and methods | Case-predicated mode; methods registry; intrusive-source exclusion; justification per query; publishable configuration (GUARDRAILS §6) | P3.5, P15, P43 | P0 |
| EU AI Act Art. 5 / Annex III | Prohibited and high-risk AI (Annex III from 2 Dec 2027) | Prohibited features hard-blocked (GUARDRAILS §2); technical file; fundamental-rights impact assessment template | P36.4, P43 | P1 |
| NCACC / NPC / EAR | Export permits | Classification opinions and permits before marketing; party screening (GUARDRAILS §7) | Corporate | P1 |
| Kenya reg 26; Rwanda art. 50; Nigeria GAID | Localisation; registration | Sovereign pod per country; registration with each data-protection authority | P39 | P1 |

## 23. Items counsel must confirm

1. The mapping of the eight POPIA conditions to sections (§2.1), and the s22(5) contents of data-subject notices (INCIDENT_RESPONSE R1).
2. The s6(1)(c) and s37 position for each client and dataset (§2.2).
3. Whether vehicle registrations and VINs are "unique identifiers" under s57(1)(a); the Regulator's current prior-authorisation form and filing channel (§2.6).
4. The s69 requirements for the comms gateway (§2.7).
5. The text of s99 and s109: strict liability and the R10m cap (§2.10).
6. GDPR territorial scope per deployment, and the EU adequacy position for South Africa (§3).
7. PAIA manual requirements for Vigil's entity (§4).
8. Whether Cybercrimes Act s54 has commenced (§5).
9. CIPA commencement and final regulations; the MISS text (§6).
10. The 2026 status of the RICA Amendment Bill and the GILAB regulations (§7).
11. ECT Act Chapter V registration mechanics (§8).
12. NCACC and dual-use classification of the civil and defence editions (GUARDRAILS §7).
13. The current law in each African country before a pod launches (P39).
14. The default audit-log retention period per sector: 7 years pending P36 (§13).
