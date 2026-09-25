<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Raw research appendix (P0). Sourced analyst report; verify [UNVERIFIED] items before external use. -->

> **Appendix to** [`VIGIL_RESEARCH_DOSSIER.md`](../VIGIL_RESEARCH_DOSSIER.md). A raw research report produced on 24–25 Sep 2026 by a parallel research agent. Kept verbatim for traceability. Where it conflicts with the dossier, the dossier wins (the dossier applies the founders' greenfield decision).
>
> **Internal working paper; not for external distribution.**

# Vigil: Legal, Regulatory, Ethical and Procurement Landscape (South Africa and Africa)

**Status date:** 25 September 2026. **Prepared for:** the Vigil founding team (MB Law / MB Tech).

**What this is.** A research memo, not legal advice. Items marked **[UNVERIFIED]** could not be confirmed against a primary or reputable source in this session, and counsel must check them before anyone relies on them. Each topic separates **Law** (what the source says) from **Analysis** (what it means for Vigil).

**How it was researched.** Primary sources were fetched where possible: gov.za, inforegulator.org.za, saflii.org, pmg.org.za, treasury.gov.za, thepresidency.gov.za, au.int, federalregister.gov, bundesverfassungsgericht.de, and the EU and Council of Europe sites. Two limits applied. The session's web-search budget ran out partway through. The fetch proxy also refused several pages with HTTP 429 (rate-limited): WSJ, Foreign Policy, Citizen Lab, The Verge and NHS England. These gaps are listed at the end.

---

## Executive summary

1. **POPIA applies to almost all of Vigil's work, security work included.** The s6(1)(c) exclusion covers only processing "by or on behalf of a public body", and only "to the extent that adequate safeguards have been established in legislation". The Regulator has twice said that SAPS is not excluded: in a 2023 enforcement finding and in a 2026 statement.
2. **Cross-organisation entity resolution is exactly the processing that needs prior authorisation.** Section 57(1)(a) covers linking unique identifiers across responsible parties for a new purpose. Sections 57(1)(b) and (d) will often apply as well.
3. **The Regulator enforces, but unevenly.** It has issued R5m and R500k fines and security-failure notices against TransUnion, Dis-Chem and SAPS. It lost the R5m matric-results case in court. Breach reports must go through its eServices portal since 1 April 2025.
4. **Surveillance law is unsettled.**
   - Bulk interception under RICA is unlawful (*amaBhungane*, 2021).
   - The RICA Amendment Bill was referred back by the President in November 2024.
   - The General Intelligence Laws Amendment Act (GILAB), signed 28 March 2025, allows judge-authorised bulk interception, but its regulations are still pending.
   - The Protection of State Information Bill (POSIB) is stalled.
   - No statute specifically authorises facial recognition (FRT) or licence-plate recognition (LPR).
5. **Government sovereign data must stay onshore, and SITA sources government cloud.** This comes from the National Data and Cloud Policy, paras 15.4.2 and 15.1.3. US hosts remain exposed to the CLOUD Act regardless.
6. **There is no South African AI law or policy.** The Draft National AI Policy was withdrawn on 26 April 2026 over hallucinated citations. The practical benchmarks are the EU AI Act (Annex III obligations from 2 December 2027) and Germany's 2023 *hessenDATA* ruling.
7. **Procurement is in transition.** The Public Procurement Act 28 of 2024 was assented; draft regulations were published in April 2026; commencement is **[UNVERIFIED]**. SITA's hold on departmental IT procurement (s7(3)) still applies.

---

## 1. POPIA deep-dive for a data-fusion platform

### 1.1 s6(1)(c): the security exclusion
**Law.** Section 6(1)(c) excludes processing "by or on behalf of a public body" in two cases ([s6](https://popia.co.za/section-6-exclusions/)):

- (i) processing that "involves national security, including activities that are aimed at assisting in the identification of the financing of terrorist and related activities, defence or public safety"; or
- (ii) processing whose purpose is "the prevention, detection, including assistance in the identification of the proceeds of unlawful activities and the combating of money laundering activities, investigation or proof of offences, the prosecution of offenders or the execution of sentences or security measures".

Either way, the exclusion applies only "to the extent that adequate safeguards have been established in legislation for the protection of such personal information".

The Regulator has applied this narrowly:
- **April 2023.** It found that SAPS breached POPIA when WhatsApp messages leaked the details of the Krugersdorp rape survivors. SAPS lacked "safeguards established in legislation as intended by section 6(1)(c)(ii)". It was ordered to notify the victims, apologise publicly, discipline officers and adopt a privacy policy ([Werksmans](https://www.werksmans.com/legal-updates-and-opinions/information-regulator-bites-enforcement-notice-issued-against-the-saps/)).
- **September 2026.** The Regulator said again that "Saps is not excluded from the operations of the act" ([TechCentral](https://techcentral.co.za/police-bodycam-tender-popia-biometrics-rules/286116/)).

**Analysis.**
- Private buyers (banks, mines, logistics firms) never get this exclusion. They must rely on legal-obligation grounds, such as FICA for anti-money-laundering, and on s33.
- Vigil processing for SAPS, the intelligence services or the SANDF may be "on behalf of" a public body. Even then, the exclusion bites only where a sector statute contains real data safeguards, and most do not.
- **Recommendation:** ship in full-POPIA mode by default. Treat any s6(1)(c) or s37 (Regulator exemption) claim as a per-dataset, counsel-signed exception.

### 1.2 Lawful basis, collection and further processing (s11, s12, s15)
**Law.**
- **s11(1) lawful grounds:** consent; contract; legal obligation; the data subject's legitimate interest; a public-law duty; or the legitimate interests of the responsible party or a third party. The data subject may object on the last three grounds (s11(3)).
- **s12 collection:** information must be collected directly from the data subject. There are exceptions (s12(2)), including public records and where collection is necessary for law enforcement by a public body, revenue, court proceedings or national security.
- **s15 further processing:** must be compatible with the original purpose. Compatibility is judged on:
  - the relationship between the purposes;
  - the nature of the information;
  - the consequences for the data subject;
  - how the information was collected;
  - contractual rights.
- **s15(3) deemed-compatible cases:** consent; public-record data; avoiding "prejudice to the maintenance of the law by any public body including the prevention, detection, investigation, prosecution and punishment of offences"; revenue; court proceedings; national security; serious and imminent threats to life or health; and non-identifiable research.
- Source: POPIA Act 4 of 2013 ([popia.co.za](https://popia.co.za/)).
- **Enforcement example:** Blouberg Municipality was fined R500k for publishing a former employee's declaration-of-interest data beyond its original purpose ([Michalsons](https://www.michalsons.com/blog/blouberg-municipality-enforcement-action-further-processing/79490); [IR briefing, Nov 2025](https://inforegulator.org.za/wp-content/uploads/2025/11/MEDIA-STATEMENT_INFORMATION-REGULATORS-LATEST-UPDATE-Journalist-ver.pdf)).

**Analysis.** Fusion is, legally, a chain of s15 decisions, one for each disclosing organisation.
- A Transnet + SAPS + SARS use case needs three separate compatibility justifications.
- The law-enforcement limb in s15(3) helps only when a *public body's* law-enforcement function is served. It does not cover a logistics customer's commercial risk-scoring.
- Every connector should carry machine-readable lawful-basis and purpose tags that follow each record into joins. Ingestion should be blocked without them.

### 1.3 Security, operators and breach reporting (s19–22)
**Law.**
- **s19:** reasonable technical and organisational measures, based on risk identification and regular verification.
- **s20–21:**
  - an operator processes only with the responsible party's authorisation and in confidence;
  - a written contract must impose s19-level security;
  - the operator must notify the responsible party "immediately" of a compromise.
- **s22:** the Regulator and data subjects must be notified "as soon as reasonably possible". Since **1 April 2025**, notifications to the Regulator must go through its eServices portal, not email ([IR, 7 Apr 2025](https://inforegulator.org.za/wp-content/uploads/2025/04/MEDIA-STATEMENT-INVITATION-TO-REPORT-SECURITY-COMPROMISES-THROUGH-THE-eSERVICES-PORTAL-.pdf)).
- **Volumes:** 2,374 compromises were reported in 2024/25, and 1,947 from April to November 2025 ([IR, Nov 2025](https://inforegulator.org.za/wp-content/uploads/2025/11/MEDIA-STATEMENT_INFORMATION-REGULATORS-LATEST-UPDATE-Journalist-ver.pdf)).

**Enforcement lessons:**
- **Department of Justice (DoJ):** let its anti-virus, SIEM and intrusion-detection licences lapse. That led to an enforcement notice and then an R5m fine ([SAnews](https://www.sanews.gov.za/south-africa/information-regulator-fines-justice-department)).
- **TransUnion (26 March 2024):** weak passwords and access policies that were never enforced. It was ordered to commission an independent audit of all user accounts ([TechCentral](https://techcentral.co.za/info-regulator-transunion-enforcement/242017/)).
- **Dis-Chem (August 2023):** a breach at its operator, Grapevine, affected 3.6 million people, and there was no compliant s21 contract ([ITWeb](https://www.itweb.co.za/article/inforeg-issues-dis-chem-with-enforcement-notice/nWJad7bN1za7bjO1)).

**Analysis.**
- Vigil is normally an **operator**. It needs s21 contracts and a 24-hour notice SLA to clients.
- It becomes a **responsible party**, carrying every duty including prior authorisation, if it decides purposes or means. Examples: a cross-tenant identity graph, pooled model training, or derived data products.

### 1.4 Special personal information and children (s26–35)
**Law.**
- **s26** prohibits by default processing of data on religion, race, trade-union membership, politics, health or sex life, **biometrics**, and **criminal behaviour** (alleged offences and proceedings).
- **s27** sets out general authorisations: consent, legal right or obligation, public-interest research, and case-by-case Regulator authorisation.
- **s33(1)** lifts the prohibition on criminal and biometric data for "bodies charged by law with applying criminal law or by responsible parties who have obtained that information in accordance with the law" ([s33](https://popia.co.za/section-33-authorisation-concerning-data-subjects-criminal-behaviour-or-biometric-information/)).
- **s34–35** impose a parallel prohibition and set of authorisations for children's data.

**Analysis.** Several ordinary features pull in special personal information or children's data:
- Face embeddings used for matching are biometric data.
- "Suspect" labels, and risk scores that imply criminality, are arguably criminal-behaviour data.
- Feeds from SASSA, schools and hospitals will contain children's data.

Each needs separate storage and a recorded s27 or s33 ground.

### 1.5 Prior authorisation (s57–59): the critical path
**Law.** A responsible party must obtain the Regulator's authorisation before processing in four cases ([s57](https://popia.co.za/section-57-processing-subject-to-prior-authorisation/)):

| Trigger | Statutory text | Vigil exposure |
|---|---|---|
| s57(1)(a) | "process any unique identifiers of data subjects — for a purpose other than the one for which the identifier was specifically intended at collection; and with the aim of linking the information together with information processed by other responsible parties" | This is the definition of multi-party entity resolution. Unique identifiers include ID, account, policy, employee and customer numbers, and IP addresses ([Werksmans](https://werksmans.com/unique-identifiers-why-the-distinction/)). Vehicle-registration and VIN linkage is probably covered **[analysis]**. |
| s57(1)(b) | "information on criminal behaviour or on unlawful or objectionable conduct on behalf of third parties" | Shared watchlists and industry crime or fraud pools |
| s57(1)(c) | credit reporting | Avoid entirely; it would also bring National Credit Act exposure |
| s57(1)(d) | transfer of special or children's PI to a country without adequate protection | Cross-border deployments and offshore support |

**Further rules:**
- **s57(2):** the Regulator may add further categories.
- **s57(3):** s57–58 no longer apply once a sector code of conduct is in force.
- **s57(4):** authorisation is needed once, unless the processing later departs from what was authorised.
- **s58 timeline:** the Regulator has 4 weeks to say whether it will investigate further, and no more than 13 weeks to finish. Processing may not start until then.
- **s59:** processing without notifying the Regulator is an offence carrying a fine or up to 12 months' imprisonment ([Webber Wentzel on the 2021 Guidance Note](https://www.webberwentzel.com/News/Pages/information-regulator-publishes-guidance-note-on-applications-for-prior-authorisation.aspx)).

**Analysis.**
- Keep a standard application pack ready: data map, linkage rules, safeguards and a personal-information impact assessment. The client files it as the responsible party.
- Budget 4–17 weeks before live linkage goes into production.
- Any shared multi-client layer would make Vigil itself an applicant.
- A sector code under s57(3), for example for mining crime intelligence, is a strategic lever.

### 1.6 Automated decisions and cross-border transfers (s71, s72)
**Law.**
- **s71(1)** bars decisions with legal consequences, or that affect someone "to a substantial degree", where the decision is "based solely on the basis of the automated processing of personal information intended to provide a profile".
- **s71(2)–(3)** exceptions are a contract, or a law or code with appropriate measures. Those measures must include a chance to make representations and "sufficient information about the underlying logic" ([s71](https://popia.co.za/section-71-automated-decision-making/)).
- **s72** permits cross-border transfers where:
  - the recipient is bound by a law, binding corporate rules or a binding agreement giving adequate protection;
  - the data subject consents;
  - the transfer is necessary for a contract; or
  - it benefits the data subject ([s72](https://popia.co.za/section-72-transfers-of-personal-information-outside-republic/)).
- There is no adequacy list. A guidance note on cross-border transfers is planned ([Werksmans on the Regulator's 2025/26 plan](https://werksmans.com/south-africas-information-regulator-what-the-2025-26-annual-performance-plan-means-for-business-as-presented-to-the-portfolio-committee-on-5-may-2026/)).

**Analysis.**
- Make human review a hard control for any adverse outcome: arrest referrals, supplier blacklisting, access denials.
- Keep an explanation record for each decision.
- For state clients, s33 of the Constitution and PAJA's reasons duty push the same way.

### 1.7 Liability
**Law.** Section 99(1) creates civil liability "whether or not there is intent or negligence". Section 109 allows administrative fines of up to R10m. Some offences carry up to 10 years' imprisonment. **[UNVERIFIED in session; standard POPIA text, counsel to confirm.]**

**Analysis.** Aggregate strict liability after a fusion breach will dwarf the fine cap. Liability caps, indemnities and insurance should be sized accordingly.

### 1.8 Enforcement, 2023–2026

| Respondent | Action | Issue / outcome |
|---|---|---|
| DoJ (2023) | Enforcement notice, then R5m fine | Security licences lapsed (s19); appeal pending ([IR, Nov 2025](https://inforegulator.org.za/wp-content/uploads/2025/11/MEDIA-STATEMENT_INFORMATION-REGULATORS-LATEST-UPDATE-Journalist-ver.pdf)) |
| SAPS (2023) | Enforcement notice | Victims' data leaked; s6(1)(c)(ii) safeguards absent |
| Dis-Chem (2023); TransUnion (2024) | Enforcement notices | Operator breach; weak passwords |
| DBE, matric results (2024–26) | Enforcement notice plus R5m fine | High Court full bench set both aside on 12 Dec 2025. It held that publishing exam numbers alone was not identifiable processing. Leave to appeal was refused on 3 June 2026 ([Moonstone](https://www.moonstone.co.za/matric-results-row-rolls-into-2026-as-regulator-digs-in/); [ITWeb](https://www.itweb.co.za/article/information-regulator-flunks-matric-results-appeal/lLn147mQmOj7J6Aa)) |
| Lancet; FT Rams; Blouberg | R100k; R100k; R500k | Failure to notify breaches; direct marketing; further processing |
| WhatsApp (16 Apr 2025) | Enforcement notice, then settlement | Transparency to South African users ([IR list](https://inforegulator.org.za/enforcement-notices/)) |
| State Security Agency (SSA, 2025) | PAIA enforcement notice | Ordered to release records of payments to service providers |
| Central Johannesburg TVET; Sibanye-Stillwater (2026) | Enforcement notices | No registered Information Officer; PAIA |

- **Experian:** no enforcement notice was found ([tracker](https://mjkinc.co.za/popia/enforcement-tracker)).
- **What the SSA order means for Vigil:** intelligence-vendor payments can be forced into public view through PAIA. Contract as if every security contract will become public.

### 1.9 Guidance and pipeline
**In force:**
- Guidance Note on prior-authorisation applications (2021).
- Amended POPIA Regulations, effective 17 April 2025. They require written consent for electronic direct marketing (including recorded calls), widen the channels for objections and complaints, and allow fines to be paid in instalments ([CDH](https://www.cliffedekkerhofmeyr.com/en/news/publications/2025/Sectors/Technology-Communications/Technology-and-Communications-Alert-21-May-Important-POPIA-amendments-to-note)).

**Coming:**
- A guidance note on cross-border transfers.
- An own-initiative **Code of Conduct for Gated Access Areas** (draft May 2026). It would curb ID copying and facial recognition at entry points ([BusinessDay](https://www.businessday.co.za/news/2026-05-11-information-regulators-privacy-crackdown-targets-office-parks-and-gated-communities/)).
- Draft health-data regulations (26 September 2025) ([LexAfrica](https://lexafrica.com/2026/02/south-africas-cybersecurity-shift-2026/)).

---

## 2. Security and cyber laws

**Cybercrimes Act 19 of 2020**
- **Law.**
  - The offence provisions (unlawful access, interception of data, interference) have been in force since 1 December 2021 ([Lexology](https://www.lexology.com/library/detail.aspx?g=d1d7d561-0687-416c-b3a3-fb2f0bfe16cc)).
  - Section 54 would oblige electronic communications service providers and financial institutions to report prescribed offences to SAPS "not later than 72 hours" after becoming aware, and to preserve evidence. The fine is up to R50,000.
  - Section 54 **has not commenced** ("to be proclaimed") ([cybercrimesact.co.za](https://cybercrimesact.co.za/section-54-obligations-of-electronic-communications-service-providers-and-financial-institutions/); [Michalsons](https://www.michalsons.com/blog/the-impact-of-the-cybercrimes-act-on-financial-institutions/56437)). No later proclamation was found **[UNVERIFIED for 2026]**.
- **Analysis.** Keep OSINT collection and security testing within written authorisation. Pre-build a s54 workflow for bank and telecoms clients.

**Critical Infrastructure Protection Act 8 of 2019 (CIPA)**
- **Law: key sections** ([Act](https://static.pmg.org.za/Critical_Infra_Protection_Act8of2019.pdf)):
  - s24: the person in control implements prescribed security measures at its own cost and appoints an SSA-vetted security manager.
  - s25: access control.
  - s26: unlawfully disclosing information about security measures is a crime (up to 7 years for high-risk infrastructure).
  - s29: repeals the National Key Points Act 102 of 1980.
  - s30: existing National Key Points are deemed critical infrastructure pending review within 48 months.
- **Law: commencement.**
  - Proclamation 54 (GG 46024, 10 March 2022) brought "certain sections" into force ([acts.co.za](https://www.acts.co.za/critical_infrastructure_protec)). The Critical Infrastructure Council sections were targeted for operation from 30 April 2022 ([PMG](https://pmg.org.za/committee-meeting/34465/)).
  - Interim regulations: GG 48526, 5 May 2023.
  - Draft CIP Regulations for comment: GG 49045, 28 July 2023. They cover risk tiers, security managers, contractor competency and MISS ([gov.za](https://www.gov.za/sites/default/files/gcis_document/202307/49045gon3732.pdf)).
  - DefenceWeb (December 2024) says National Key Points have "migrated" to CIPA ([DefenceWeb](https://defenceweb.co.za/security/national-security/security-at-national-key-points-explained/)). Full commencement and final regulations remain **[UNVERIFIED]**.
  - CIPA covers physical protection ("fences and guards, but not firewalls"). There is no law on critical *information* infrastructure ([Daily Maverick, July 2025](https://www.dailymaverick.co.za/article/2025-07-17-command-line-to-control-room-sas-infrastructure-vulnerable-to-cyberattacks/)).
- **Analysis.** At Eskom, Transnet or port sites, expect vetted staff access and security-manager control. Sensor, camera and patrol configurations will be protected "security measures" information under s26. Contractor-competency requirements will be flowed down to Vigil.

**Minimum Information Security Standards (MISS) and vetting**
- **Law.**
  - MISS (Cabinet, 1996) sets the classifications Restricted, Confidential, Secret and Top Secret, and requires clearances for access **[UNVERIFIED: text not fetched]**. Both the Data and Cloud Policy and the draft CIP Regulations rely on MISS.
  - Vetting is done by the civilian intelligence service (SSA, now the Domestic Intelligence Agency) and, for defence work, by Defence Intelligence.
  - In 2022 PMG recorded a backlog of about 3,000 SSA vetting cases ([PMG](https://pmg.org.za/committee-meeting/34465/)).
- **Analysis.** Clearance lead times, probably 6–18 months **[estimate]**, will set the pace of defence and intelligence deals. Get key staff sponsored for clearance through the first client.

**ECT Act, Chapter V (ss28–32)**
- **Law.** Suppliers of cryptography products or services must be recorded on the Department of Communications and Digital Technologies (DCDT) register before supplying. Failing to register is an offence (up to 2 years). Enforcement is dormant and unclear ([Michalsons](https://www.michalsons.com/blog/cryptography-laws-in-south-africa/3266)).
- **Analysis.** Register defensively.

**Cyber policy**
- **Law.**
  - The National Cybersecurity Policy Framework dates from 2012 (gazetted 2015) **[UNVERIFIED]**.
  - Capability is split between the DCDT Cybersecurity Hub and the SSA, and a new strategy was "finalising consultation" in 2025 ([Daily Maverick](https://www.dailymaverick.co.za/article/2025-07-17-command-line-to-control-room-sas-infrastructure-vulnerable-to-cyberattacks/)).
  - The financial-sector Joint Standard on cybersecurity has applied since June 2025 ([LexAfrica](https://lexafrica.com/2026/02/south-africas-cybersecurity-shift-2026/)).
- **Analysis.** Bank buyers will flow its third-party requirements down to Vigil.

---

## 3. Interception and surveillance

**RICA 70 of 2002**
- **Law.** Interception, and access to real-time or archived communication-related information, needs a direction from the designated judge. Criminal Procedure Act s205 subpoenas are also used to obtain metadata. RICA separately restricts "listed equipment" (interception devices) unless the Minister grants an exemption **[UNVERIFIED section numbers]**.

***amaBhungane v Minister of Justice* [2021] ZACC 3 (4 February 2021)**
- **Law.** The Constitutional Court confirmed that RICA is invalid for failing to provide:
  - post-surveillance notification;
  - an adequately independent designated judge;
  - ex parte safeguards;
  - procedures for examining, storing, sharing and destroying intercepted data;
  - protection for practising lawyers and journalists.
- It also upheld the finding that the National Communications Centre's (NCC's) bulk interception "is not authorised by law".
- The declaration was suspended for 36 months. In the meantime the Court read in interim ss23A (disclosure when the subject is a journalist or lawyer) and 25A (notification within 90 days, extendable in 90-day steps up to 2 years) ([SAFLII](https://www.saflii.org/za/cases/ZACC/2021/3.html)).

**RICA Amendment Bill (B28-2023)**
- **Law.**
  - Passed by the National Assembly on 14 November 2023 ([Parliament](https://www.parliament.gov.za/press-releases/national-assembly-passes-four-bills-during-plenary-sitting)).
  - Referred back by the President on 21 November 2024. His concerns were that s25A(2)(b) could mean a subject is never notified, and that ex parte safeguards were weak ([Presidency](https://www.thepresidency.gov.za/president-ramaphosa-refers-rica-amendment-bill-national-assembly)).
  - On 11 February 2025 the Justice Committee committed to reprocess it urgently ([Parliament](https://www.parliament.gov.za/press-releases/media-statement-committee-justice-committed-remedying-unconstitutionality-rica-act)).
  - Status in 2026: **[UNVERIFIED]**.

**GILAB (assented 28 March 2025)**
- **Law.**
  - Splits the SSA into a Foreign Intelligence Service and a Domestic Intelligence Agency.
  - Subjects bulk interception to authorisation and "court reviews" ([Presidency](https://www.thepresidency.gov.za/president-ramaphosa-assents-general-intelligence-laws-amendment-bill)).
  - Mass interception needs approval from a judge appointed by the President in consultation with the Chief Justice. However, the "details… will only be established through additional regulations".
  - The broad power to vet any "person or institution of national security interest" (which could have reached NGOs and churches) was dropped. Vetting of "persons who have access to critical infrastructure" remains ([Press Council](https://presscouncilsa.org.za/2025/03/29/general-intelligence-law-amendment-act-adopted-still-deeply-flawed/); [Intelwatch](https://intelwatch.org.za/2024/04/08/despite-important-gains-the-new-general-intelligence-laws-amendment-bill-fails-to-safeguard-against-a-second-state-capture/)).
  - Commencement and regulations: **[UNVERIFIED]**.

**Protection of State Information Bill (POSIB)**
- **Law.** Not law. In May 2025 Parliament's Legal Services agreed with most of the President's reservations: overbroad "national security" definitions, media-freedom impacts and weak defences. The committee is choosing between reprocessing the Bill, an ad hoc committee, or rescinding it ([IOL](https://iol.co.za/news/politics/2025-05-10-justice-committee-debates-future-of-the-protection-of-state-information-bill/); [News24](https://www.news24.com/politics/parliaments-legal-services-agree-with-ramaphosa-on-secrecy-bill-20250508-0329)).

**CCTV, LPR and FRT**
- **Law.**
  - SAPS relies on its general mandate (Constitution s205(3); SAPS Act 68 of 1995). Metro police rely on SAPS Act Chapter 12 and municipal by-laws **[UNVERIFIED section cites]**.
  - No statute specifically authorises FRT or LPR networks.
  - SITA tender **RFB 3286-2026**, for SAPS body cameras and dashcams with FRT and ANPR, closes on 29 September 2026. It cites no s27 ground and states no purpose. The Regulator says it was not consulted and keeps its prior-authorisation powers ([TechCentral](https://techcentral.co.za/police-bodycam-tender-popia-biometrics-rules/286116/); [Biometric Update](https://www.biometricupdate.com/202609/south-africa-seeks-facial-recognition-for-police-body-dashboard-cameras)).
  - The draft Gated Access Code notes that CCTV at access points captures faces "without the consent" of visitors ([BusinessDay](https://www.businessday.co.za/news/2026-05-11-information-regulators-privacy-crackdown-targets-office-parks-and-gated-communities/)).
- **Analysis.** Private LPR/CCTV networks that send "hits" to SAPS or private security are processing criminal-conduct information for third parties. That engages s57(1)(b), s26 and s33. Vigil should ingest such feeds only case by case, under authorisation.

---

## 4. Data sovereignty and cloud

**National Data and Cloud Policy** (GG 50741, GN 2533, 31 May 2024) ([gazette](https://www.gov.za/sites/default/files/gcis_document/202406/50741gen2533.pdf))
- **Law.** Key paragraphs:

| Para | Provision |
|---|---|
| 15.4.2 | Government data "pertaining to the protection and preservation of national security and sovereignty… shall be stored only in digital infrastructure located within the borders of South Africa" |
| 15.4.1, 15.4.3 | Compliance with South African law; criteria for cross-border agreements, including AfCFTA |
| 15.1.3 | SITA is "the responsible authority… to source data infrastructure and cloud services for the government" |
| 15.1.10 | Proposal: cloud as the "primary option for new ICT procurement" |
| 15.1.6 | Data-centre standards: 99.995% uptime; self-provided power and water |
| 15.2.2 | Open Data Framework |
| 15.2.5 | Four data tiers: public/open, confidential/sensitive, secret, top secret |
| 15.3.1.6 | Cybersecurity by design |
| 15.8.1, 17.1 | Data Advisory Council |
| 17.2 | Technical Task Team: DCDT, SSA, Treasury, DPSA and Home Affairs |

- **Law: what is missing.**
  - The final text has no dedicated section on the High Performance Computing and Data Processing Centre (HPCDPC), and no separate state digital-infrastructure company. Both were features of the 2021 draft **[UNVERIFIED comparison]**.
  - Implementation in 2025–26 (Advisory Council, Open Data Framework) is **[UNVERIFIED]**.

**SITA Act 88 of 1998**
- **Law.**
  - **s7(1)(a) mandatory services:** a private network and value-added network, transversal information systems, and data processing for them.
  - **s7(3):** "every department must… procure all information technology goods or services through the Agency".
  - **s7(5):** other public bodies *may* use SITA.
  - **s7(6):** SITA sets interoperability and security standards and must certify every IT acquisition by a department ([SAFLII](https://www.saflii.org/za/legis/consol_act/sitaa88o1998386/)).
  - Reform of SITA in 2024–26: **[UNVERIFIED]**.
- **Analysis.** Departments are reached only through SITA. SOEs and metros can be approached directly.

**CLOUD Act exposure**
- **Law.** 18 U.S.C. §2713 obliges US providers to disclose data in their "possession, custody, or control, regardless of whether… located within or outside of the United States" ([LII](https://www.law.cornell.edu/uscode/text/18/2713)). The US has executive agreements with the UK and Australia; South Africa has none ([DOJ](https://www.justice.gov/criminal/cloud-act-resources)).
- **Analysis.**
  - Local hyperscaler regions solve *where* data sits, not *whose law* reaches it. (Local region launch dates are **[UNVERIFIED]**.)
  - For data covered by NDCP 15.4.2 or MISS, offer on-premises or air-gapped deployment, or a South African-owned operator.
  - Use customer-held keys (HSM/HYOK), commit contractually to challenge foreign legal process, and publish transparency reporting on it.

---

## 5. AI governance

**South Africa**
- **Law.**
  - The National AI Policy Framework (DCDT, August 2024) is non-binding.
  - The **Draft National AI Policy** was approved by Cabinet on 25 March 2026 and gazetted on 10 April 2026 (Notice 3880), with comments due by 10 June 2026. It took a risk-based approach "inspired" by the EU AI Act and proposed new institutions: a National AI Commission, an AI Ethics Board, an AI Regulatory Authority, an AI Ombudsperson, an AI Insurance Superfund and an AI Safety Institute.
  - It was **withdrawn on 26 April 2026**. Its reference list contained fictitious sources "likely" generated by AI, and officials were suspended ([SAnews, publication](https://www.sanews.gov.za/south-africa/draft-ai-policy-approved-publication-public-comment); [SAnews, withdrawal](https://www.sanews.gov.za/south-africa/minister-announces-withdrawal-draft-ai-policy); [overview](https://en.wikipedia.org/wiki/Draft_South_Africa_National_Artificial_Intelligence_(AI)_Policy_2026)).
  - AI-specific law is "not imminent in 2026" ([LexAfrica](https://lexafrica.com/2026/02/south-africas-cybersecurity-shift-2026/)).
- **Analysis.** In the meantime AI is governed by POPIA (s71, s57, s26), PAJA for decisions by the state, the equality (s9) and privacy (s14) rights, and sector regulators. Vigil can influence the re-draft by publishing its own standards.

**African Union**
- **Law.** The Continental AI Strategy was endorsed at the Executive Council's 45th Ordinary Session (Accra, 18–19 July 2024) ([AU](https://au.int/en/documents/20240809/continental-artificial-intelligence-strategy)).

**EU AI Act** (the benchmark South Africa borrowed from)
- **Law: prohibitions (Article 5), in force since 2 February 2025:**
  - individual predictive policing "based solely on the profiling of a natural person";
  - untargeted scraping of facial images from the internet or CCTV;
  - biometric categorisation that infers sensitive traits;
  - real-time remote biometric identification in public spaces for law enforcement, except in narrow cases with prior judicial or independent authorisation ([Art 5](https://artificialintelligenceact.eu/article/5/)).
- **Law: high-risk uses (Annex III):** biometrics; safety components of critical infrastructure; law enforcement (point 6(a)–(e), including profiling and assessing evidence reliability); and migration and border control (point 7) ([Annex III](https://artificialintelligenceact.eu/annex/3/)).
- **Law: AI Omnibus.** Proposed 19 November 2025; political agreement 7 May 2026; in force 27 July 2026. Annex III obligations now apply from **2 December 2027**, and Annex I obligations from 2 August 2028 ([European Commission](https://digital-strategy.ec.europa.eu/en/policies/regulatory-framework-ai)).

**Council of Europe Framework Convention (CETS 225)**
- **Law.** Opened for signature on 5 September 2024. It covers public and private actors, with a national-security carve-out. No African state has signed. The CoE page states that the EU has ratified ([CoE](https://www.coe.int/en/web/artificial-intelligence/the-framework-convention-on-artificial-intelligence)). Entry into force: **[UNVERIFIED]**.

**German Federal Constitutional Court, 16 February 2023 (1 BvR 1547/19; 1 BvR 2634/20)**
- **Law.**
  - The Court struck down Hesse's s25a(1) HSOG (hessenDATA, which ran on Palantir Gotham) and Hamburg's s49(1) PolDVG. Its reasoning: these tools let police "with just one click, … create comprehensive profiles".
  - **What the Court required:**
    1. For serious analyses, a threshold of an **identifiable danger to particularly weighty legal interests** (life, limb, liberty).
    2. The **legislature itself** must limit the **types and amount of data** and the **methods** of analysis. It singled out data mining, "self-learning systems (AI)" and predictive analysis.
    3. Data from home surveillance, remote IT searches, telecoms surveillance, long-term observation and undercover work must be excluded.
    4. Suspects must be distinguished from uninvolved third parties.
    5. The administrative rules that specify the system must be documented and published.
  - **Outcome.** Hesse's provision survived only until 30 September 2023, on interim conditions: "sufficiently specific facts" of particularly serious offences, and a written justification for each use. Hamburg's provision was declared void ([BVerfG](https://www.bundesverfassungsgericht.de/SharedDocs/Pressemitteilungen/EN/2023/bvg23-018.html)).
- **Analysis.** This is the most exact court-tested specification of safeguards for a Palantir-class system. Vigil should build it into the product.

---

## 6. Public procurement and market access

**Framework**
- **Law: governing rules.** Constitution s217 (fair, equitable, transparent, competitive, cost-effective); PFMA ss38(1)(a)(iii) and 51(1)(a)(iii) with Treasury Regulation 16A; MFMA ss111–112 with the Municipal SCM Regulations.
- **Law: preferential procurement.** PPPFA 5 of 2000 and the **Preferential Procurement Regulations 2022** (in force 16 January 2023) apply an 80/20 points split up to R50m and 90/10 above R50m. Each organ of state sets its own "specific goals" following *Afribusiness* **[UNVERIFIED in session; see [Treasury SCM](https://www.treasury.gov.za/legislation/pfma/supplychain/default.aspx)]**.
- **Law: Public Procurement Act 28 of 2024.** Assented on 23 July 2024. It creates a single framework for all organs of state, repeals the PPPFA and bars office-bearers from bidding ([Presidency](https://www.thepresidency.gov.za/president-ramaphosa-assents-public-procurement-bill)). Draft General Public Procurement Regulations were published on 16 April 2026, with the comment deadline extended to 15 July 2026 ([Treasury](https://www.treasury.gov.za/comm_media/press/2026/20260610%20Media%20Statement%20-%20Draft%20Public%20Procurement%20Regulations%20-%20Extension%20of%20Timeframe%20for%20Submission.pdf)). **Commencement: [UNVERIFIED].**

**Compliance stack**
- **Law.**
  - Registration on the [Central Supplier Database](https://secure.csd.gov.za), which also verifies SARS tax compliance.
  - The [eTender portal](http://www.etenders.gov.za).
  - SBD 1, 3.x, 4 and 6.1 forms (MBD equivalents for municipalities).
  - B-BBEE under the ICT Sector Code:
    - EME: turnover up to R10m. Level 4 by default, Level 2 if at least 51% black-owned, Level 1 if 100% black-owned.
    - QSE: turnover of R10m–R50m.
    - The ownership target is 30%.
  - All of the above is **[UNVERIFIED; confirm the current forms and code]**.
- **Law: thresholds [UNVERIFIED].**
  - PFMA: price quotations up to about R1m, competitive bids above.
  - MFMA: written quotations up to R200k, competitive bids above.
  - Minimum advertising periods: 21 days (PFMA); 14 or 30 days (MFMA).
  - Typical timelines: 3–9 months for open tenders, longer through SITA **[estimate]**.

**Channels**
- **Law.**
  - Treasury transversal contracts. Treasury opened an investigation into transversal awards on 8 July 2026 ([Treasury](https://www.treasury.gov.za/comm_media/press/2026/2026070801%20Media%20statement%20-%20National%20Treasury%20Investigation%20into%20Transversal%20Contracts.pdf)).
  - SITA contracts and certification (SITA Act s7(3) and s7(6)).
  - SOEs such as Transnet and Eskom run their own board-approved policies under PFMA s51 and PPR 2022.
  - Defence procurement runs through Armscor (Armscor Act 51 of 2003), with Defence Intelligence clearances and industrial-participation rules **[UNVERIFIED detail]**.
  - Unsolicited bids are allowed only for unique, innovative, sole-provider offerings (Treasury practice note; MFMA SCM Reg 37) **[UNVERIFIED]**. Treasury issued *Guidelines for Management of Unsolicited Proposals* on 31 October 2025 ([Treasury](https://www.treasury.gov.za/comm_media/press/2025/Default.aspx)).

**Fast, lawful routes in (analysis)**
1. Win private buyers first: bank fraud and AML teams, mine security, logistics firms, insurers.
2. Subcontract to systems integrators that already hold SITA or transversal contracts.
3. Run no-cost proofs of concept at SOEs and metros only under a written, non-exclusive letter kept separate from any future tender. This avoids "unfair advantage" challenges.
4. Partner with CSIR, universities or TIA as delivery vehicles **[counsel to confirm the organ-of-state route]**.
5. Bid on projects funded by DFIs or donors (World Bank, AfDB, DBSA), which follow the funder's procurement rules.
6. Do not build growth on deviations (TR 16A6.4) or piggy-backing on other organs' contracts (TR 16A6.6, MFMA Reg 32). Both draw Auditor-General irregular-expenditure findings and state-capture optics.

---

## 7. Pan-African data protection and localisation

**Regional instruments**
- **Law: Malabo Convention.** In force since 8 June 2023, when Mauritania became the 15th state to ratify. **South Africa is not a party** ([Data Protection Africa](https://dataprotection.africa/malabo-convention-set-to-enter-force/); [AU](https://au.int/en/treaties/african-union-convention-cyber-security-and-personal-data-protection)).
- **Law: other instruments [UNVERIFIED in session].** The AU Data Policy Framework (2022), SADC Model Law (2013) and AfCFTA Digital Trade Protocol (2024) all favour harmonised transfers, but allow localisation for public-policy reasons.

**Country positions**

| Country | Law and localisation |
|---|---|
| Kenya | DPA 2019; transfers under ss48–50. **Reg 26** of the General Regulations 2021: processing for "strategic interests of the state" must run on servers in Kenya, or keep a serving copy there. This covers civil registration and ID, elections, public finance, protected computer systems, basic education and healthcare ([LN 263/2021](http://kenyalaw.org/kl/fileadmin/pdfdownloads/LegalNotices/2021/LN263_2021.pdf)) |
| Nigeria | NDPA 2023, signed June 2023 ([DPA](https://dataprotection.africa/nigeria/)). The GAID (March 2025; effective September 2025) requires registration of "major importance" controllers, DPIAs and audit returns **[UNVERIFIED]**. NITDA rules favour local hosting of government data **[UNVERIFIED]** |
| Rwanda | Law 058/2021, enforced by the NCSA. **Art 50:** storing data outside Rwanda needs NCSA authorisation. Compliance required from 15 October 2023 ([DPA](https://dataprotection.africa/rwanda/)) |
| Ghana | Act 843 (2012), with DPC registration; a Malabo party ([DPA](https://dataprotection.africa/ghana/)). The Cybersecurity Act 2020 regime for critical information infrastructure is **[UNVERIFIED]** |
| Egypt | PDPL 151/2020: cross-border transfers need a permit from the Data Protection Centre; national-security exclusion ([DPA](https://dataprotection.africa/egypt/)). Executive regulations: **[UNVERIFIED]** |
| Zambia | DPA 3/2021: registration; sensitive data may leave Zambia only with consent; s70 localisation power ([DPA](https://dataprotection.africa/zambia/)). Cyber laws replaced in 2025 **[UNVERIFIED]** |
| Botswana | DPA 2018 commenced 15 October 2021; adequacy-based transfers ([DPA](https://dataprotection.africa/botswana/)). A 2024 replacement Act is **[UNVERIFIED]** |
| Namibia / Mozambique | Both are Malabo parties. Whether a comprehensive data-protection statute is in force: **[UNVERIFIED]** |

**Analysis.** Deploy **sovereign pods** in each country: in-country hosting and keys, a local entity or partner, registration with the data-protection authority, and a local DPO. Move only aggregates across borders. Where national-security exclusions remove the regulator's oversight, Vigil's own obligations increase.

---

## 8. Export controls and supply chain

**US controls on AI chips**
- **Law.**
  - The AI Diffusion interim final rule (90 FR 4544, 15 January 2025) imposed worldwide licensing for advanced ICs (ECCN 3A090, 4A090) and model weights (4E091), with country compute caps. South Africa sat in the capped middle group, neither a Tier-1 ally nor arms-embargoed **[tier reading: analysis]** ([Federal Register](https://www.federalregister.gov/documents/2025/01/15/2025-00636/framework-for-artificial-intelligence-diffusion)).
  - The Bureau of Industry and Security (BIS) **rescinded** it on 13 May 2025. It issued guidance on Huawei Ascend chips, on training Chinese models and on diversion, and promised a "replacement rule in the future" ([BIS](https://www.bis.gov/press-release/department-commerce-announces-rescission-biden-era-artificial-intelligence-diffusion-rule-strengthens)).
  - Successor rule as at September 2026: **[UNVERIFIED]**.
  - US–South Africa relations are strained: a 7 February 2025 Executive Order halted US aid to South Africa ([White House](https://www.whitehouse.gov/presidential-actions/2025/02/addressing-egregious-actions-of-the-republic-of-south-africa/)).
- **Analysis.** Diversify compute sourcing, document end-uses and screen counterparties.

**ITAR and EAR (US export regimes) [analysis; UNVERIFIED specifics]**
- If US defence articles or data are integrated, ITAR controls follow them. They restrict re-transfer and foreign-person access, and can "taint" a whole deployment.
- Under the EAR, encryption items (5A002, 5D002), the de minimis rules and crime-control items need classification before any re-export from South Africa.
- Keep Vigil's core free of US-controlled content and screen against the Entity List.

**South African controls [UNVERIFIED section cites]**
- The National Conventional Arms Control Act 41 of 2002 (amended 2008) covers military software, technology and command-and-control (C2) systems. The National Conventional Arms Control Committee (NCACC) requires registration and marketing, contracting and export permits, and applies human-rights and conflict criteria.
- The Non-Proliferation of Weapons of Mass Destruction Act 87 of 1993 controls Wassenaar-aligned dual-use items, including information-security and surveillance goods.
- **Analysis.** A defence edition for other African militaries will probably need NCACC permits *before marketing*. The civil edition needs a dual-use classification opinion.

---

## 9. Ethics and legitimacy

**Documented abuses in Africa**
- **Huawei:** technicians allegedly helped Uganda and Zambia spy on opponents and bloggers ([WSJ, 2019](https://www.wsj.com/articles/huawei-technicians-helped-african-governments-spy-on-political-opponents-11565793017); Huawei denied).
- **CloudWalk–Zimbabwe (2018):** a national facial-recognition deal ([Foreign Policy](https://foreignpolicy.com/2018/07/24/beijings-big-brother-tech-needs-african-faces/)).
- **Cambridge Analytica:** worked on Kenya's 2013 and 2017 elections and Nigeria's 2015 election ([BBC](https://www.bbc.com/news/world-africa-43471707); [Guardian](https://www.theguardian.com/uk-news/2018/apr/04/cambridge-analytica-used-violent-video-to-try-to-influence-nigerian-election)).
- **Spyware:** Pegasus and Circles deployments linked to African states ([Citizen Lab 2018](https://citizenlab.ca/2018/09/hide-and-seek-tracking-nso-groups-pegasus-spyware-to-operations-in-45-countries/); [Citizen Lab 2020](https://citizenlab.ca/2020/12/running-in-circles-uncovering-the-clients-of-cyberespionage-firm-circles/)).
- None of these pages could be fetched this session (proxy rate limit), so all of the above is **[UNVERIFIED]**.
- **South Africa:** the *amaBhungane* case itself arose from the surveillance of journalist Sam Sole ([SAFLII](https://www.saflii.org/za/cases/ZACC/2021/3.html)).

**Palantir controversies [UNVERIFIED in session]**
- US Immigration and Customs Enforcement (ICE) case-management work, and "ImmigrationOS" (2025).
- The NHS England Federated Data Platform (2023) ([NHS England](https://www.england.nhs.uk/digitaltechnology/nhs-federated-data-platform/)).
- Secret predictive policing in New Orleans, 2012–2018 ([The Verge](https://www.theverge.com/2018/2/27/17054740/palantir-predictive-policing-tool-new-orleans-nopd)).
- hessenDATA, struck down in Germany (see section 5).

**South African watchdogs**
[Right2Know](https://www.r2k.org.za/), [Intelwatch](https://intelwatch.org.za/), [amaBhungane](https://amabhungane.org/), [Media Monitoring Africa](https://mediamonitoringafrica.org/), [ALT Advisory](https://altadvisory.africa/), [Research ICT Africa](https://researchictafrica.net/), [Open Secrets](https://www.opensecrets.org.za/).

**How Vigil can earn legitimacy (analysis)**
- Published red lines, enforced in code and in contracts.
- An independent oversight board with audit access and the power to halt deployments.
- Modules gated behind warrants or authorisations.
- Queries bound to a stated purpose.
- An immutable audit trail, anchored externally.
- Annual transparency reports covering deployments, legal-process demands, refusals and incidents.
- UN Guiding Principles human-rights due diligence before every sale to a government.
- A protected-disclosure channel for whistleblowers.
- Contract rights to suspend or terminate for misuse.

---

## 10. Compliance-by-design matrix

| Law / obligation | Requirement | Vigil control (technical + process) | Priority |
|---|---|---|---|
| POPIA s6(1)(c) | Exclusion applies only with statutory safeguards | Full-POPIA default; per-dataset exclusion claim signed off by client counsel | P0 |
| POPIA s11, s12, s15 | Lawful basis; compatible further processing | Lawful-basis and purpose tags on every record; compatibility gate at each join; s15(3) ground logged | P0 |
| POPIA s57(1)(a), s58 | Prior authorisation before linking identifiers | Linkage engine locked until an authorisation ID is recorded; export of linkage rules for the application | P0 |
| POPIA s57(1)(b) | Criminal-conduct data for third parties | Shared watchlists off by default; tenant isolation; authorisation gate | P0 |
| POPIA s57(1)(d), s72 | Cross-border transfer of special PI | Geo-fenced storage; transfer broker with a legal-basis selector; export logs | P0 |
| POPIA s19 | Security safeguards | ISO 27001 / SOC 2; MFA and privileged-access management; password policy; licence-expiry alarms | P0 |
| POPIA s20–22 | Operator contract; breach notices | Standard s21 data-processing agreement; 24-hour notice SLA; subprocessor register; incident packs ready for the eServices portal | P0 |
| POPIA s26–35 | Special PI; children | Special-PI classifier; separately encrypted stores; s27/s33/s35 ground required; children's data excluded by default | P0 |
| POPIA s71; PAJA | No solely automated adverse decisions | Human-review workflow; explanation record; channel for representations | P0 |
| POPIA s99; PAIA | Strict liability; disclosure of records | Liability caps, insurance; tooling to retrieve records for PAIA requests | P1 |
| Cybercrimes Act s2–3, s54 | Lawful access; 72-hour reporting (once in force) | Authorised-testing protocol; s54 workflow for bank and telecoms clients | P1 |
| CIPA s24–26; MISS | Vetting; secrecy of security measures | Cleared enclave team; "security measure" data class; accredited air-gapped build | P1 |
| ECT Act ch V | Crypto-provider register | Register with DCDT | P2 |
| RICA; *amaBhungane*; GILAB | No bulk interception; judicial directions; notification | No interception features; comms-metadata module requires a direction reference ID; notification tracker; no bulk-intercept ingestion | P0 |
| NDCP 15.4.2; CLOUD Act | Sovereign data held onshore; exposure to foreign compelled access | Hosting in South Africa or on-premises only; customer-held keys; South African operator; challenge-and-disclose policy | P0 |
| SITA Act s7(3), s7(6); PPPFA / PPA | Procurement channel; certification; preference points | SITA certification roadmap; systems-integrator partnerships; CSD, tax and B-BBEE plan | P1 |
| *hessenDATA* safeguards | Thresholds; limits on data types and methods | Case-predicated mode; registry of permitted methods; exclusion of intrusive sources; written justification per query; publishable configuration | P0 |
| EU AI Act Art 5 / Annex III | Prohibited and high-risk AI | Prohibited features hard-blocked; technical file; fundamental-rights impact assessment template | P1 |
| NCACC / NPC / EAR | Export permits | Classification opinions; permits obtained before marketing; party screening | P1 |
| Kenya reg 26; Rwanda Art 50; Nigeria GAID | Localisation; registration | Sovereign pod per country; registrations with data-protection authorities | P1 |

---

## Implications for Vigil

1. **Treat POPIA as the product specification.**
   - Assume s6(1)(c) will not shield security clients.
   - Stay an operator: strict tenant isolation, no cross-tenant identity graph, and no pooled training on personal data.
2. **Industrialise prior authorisation.**
   - Keep a standard s57/s58 application pack.
   - Budget 4–17 weeks per multi-party deployment.
   - Sponsor a sector code of conduct (s57(3)) to avoid repeated authorisations.
3. **Red lines: never build or sell.**
   - Bulk or untargeted interception, or ingesting its products.
   - Live FRT for general surveillance of public spaces, or untargeted face-scraping databases.
   - Predictive policing of individuals based on profiling alone.
   - Social or political scoring, or electoral micro-targeting.
   - Spyware or intrusion tooling, or RICA "listed equipment".
   - Biometric inference of race, religion, politics or sexuality.
   - Credit-bureau functions.
   - Sales to embargoed buyers, or to buyers credibly linked to recent abuses.
4. **Warrant- or authorisation-gated modules.**
   - Controls on every gated module: dual control, a reference ID for the legal instrument, limits on scope and duration, and automatic expiry.
   - Modules to gate:
     - communications metadata (RICA or CPA s205);
     - retrospective biometric search on named suspects;
     - LPR hotlists and movement histories;
     - cross-agency linkage of unique identifiers (s57(1)(a));
     - shared criminal-conduct watchlists (s57(1)(b));
     - children's data;
     - cross-border transfers of special PI;
     - any output that feeds a decision with legal effect (s71).
5. **Build the *hessenDATA* safeguards in natively.** A threshold for each case, whitelisted data types and methods (with separate approval for self-learning methods), exclusion of intrusive sources, minimisation of third parties, a written justification per query, and a configuration that can be published.
6. **Immutable audit, independent oversight and annual transparency reports.** Use hash-chained, externally anchored logs. Give a board of retired judges, technologists and civil-society members audit rights and the power to halt deployments.
7. **Sovereign-first architecture.** On-premises and air-gapped editions, hosting in South Africa only, and customer-held keys. Use per-country pods elsewhere in Africa.
8. **Security hygiene is also a legal defence.** Apply the DoJ, TransUnion and Dis-Chem lessons: licence monitoring, MFA and privileged-access management, subprocessor contracts, and a 24-hour breach SLA.
9. **Sequence the go-to-market.** Private buyers first; then SOEs and metros through proofs of concept and systems-integrator subcontracts; then departments through SITA; defence last, once clearances and NCACC permits are in hand. Track the PPA's commencement and its final regulations.
10. **Build the trust stack early.** B-BBEE planning against the ICT Code's 30% ownership target, CSD registration, ISO 27001, crypto registration, and staff vetting sponsored by the first security client.
11. **Export discipline.** Get NCACC and dual-use classification opinions before marketing outside South Africa. Keep the core free of ITAR content. Diversify GPU sourcing.
12. **Engage civil society before the press does.** Brief Right2Know, Intelwatch, ALT Advisory and RIA on the red lines, offer them oversight seats, and help shape the re-drafted AI policy.
13. **Counsel must confirm:**
    - the s6(1)(c) and s37 position for each client;
    - whether CIPA has fully commenced;
    - the status of the RICA Bill and the GILAB regulations;
    - PPA commencement and current procurement thresholds;
    - NCACC and dual-use classification;
    - the current law in each African country.

---

## Verification gaps
- **Pages not fetched** (proxy HTTP 429): WSJ (Huawei), Foreign Policy (CloudWalk), Citizen Lab (Pegasus), The Verge (New Orleans), NHS England (Federated Data Platform), BBC (Cambridge Analytica, Kenya).
- **Not re-checked** (search budget spent): Guardian (Cambridge Analytica, Nigeria); Citizen Lab 2020 (Circles); 2026 status of the RICA Bill and SITA reform; PPA commencement; detail of Nigeria's GAID; updates for Botswana, Egypt, Namibia and Mozambique; any successor to the AI Diffusion Rule; the MISS text; procurement thresholds.

## Sources
**POPIA and Information Regulator**
- https://popia.co.za/section-6-exclusions/
- https://popia.co.za/section-33-authorisation-concerning-data-subjects-criminal-behaviour-or-biometric-information/
- https://popia.co.za/section-57-processing-subject-to-prior-authorisation/
- https://popia.co.za/section-71-automated-decision-making/
- https://popia.co.za/section-72-transfers-of-personal-information-outside-republic/
- https://inforegulator.org.za/enforcement-notices/
- https://inforegulator.org.za/wp-content/uploads/2025/04/MEDIA-STATEMENT-INVITATION-TO-REPORT-SECURITY-COMPROMISES-THROUGH-THE-eSERVICES-PORTAL-.pdf
- https://inforegulator.org.za/wp-content/uploads/2025/11/MEDIA-STATEMENT_INFORMATION-REGULATORS-LATEST-UPDATE-Journalist-ver.pdf
- https://mjkinc.co.za/popia/enforcement-tracker
- https://www.webberwentzel.com/News/Pages/information-regulator-publishes-guidance-note-on-applications-for-prior-authorisation.aspx
- https://werksmans.com/unique-identifiers-why-the-distinction/
- https://www.werksmans.com/legal-updates-and-opinions/information-regulator-bites-enforcement-notice-issued-against-the-saps/
- https://werksmans.com/south-africas-information-regulator-what-the-2025-26-annual-performance-plan-means-for-business-as-presented-to-the-portfolio-committee-on-5-may-2026/
- https://www.cliffedekkerhofmeyr.com/en/news/publications/2025/Sectors/Technology-Communications/Technology-and-Communications-Alert-21-May-Important-POPIA-amendments-to-note
- https://www.sanews.gov.za/south-africa/information-regulator-fines-justice-department
- https://techcentral.co.za/info-regulator-transunion-enforcement/242017/
- https://www.itweb.co.za/article/inforeg-issues-dis-chem-with-enforcement-notice/nWJad7bN1za7bjO1
- https://www.moonstone.co.za/matric-results-row-rolls-into-2026-as-regulator-digs-in/
- https://www.itweb.co.za/article/information-regulator-flunks-matric-results-appeal/lLn147mQmOj7J6Aa
- https://www.michalsons.com/blog/blouberg-municipality-enforcement-action-further-processing/79490
- https://www.businessday.co.za/news/2026-05-11-information-regulators-privacy-crackdown-targets-office-parks-and-gated-communities/

**Security, cyber and critical infrastructure**
- https://cybercrimesact.co.za/section-54-obligations-of-electronic-communications-service-providers-and-financial-institutions/
- https://www.michalsons.com/blog/the-impact-of-the-cybercrimes-act-on-financial-institutions/56437
- https://www.michalsons.com/blog/cryptography-laws-in-south-africa/3266
- https://static.pmg.org.za/Critical_Infra_Protection_Act8of2019.pdf
- https://www.acts.co.za/critical_infrastructure_protec
- https://pmg.org.za/committee-meeting/34465/
- https://www.gov.za/sites/default/files/gcis_document/202307/49045gon3732.pdf
- https://defenceweb.co.za/security/national-security/security-at-national-key-points-explained/
- https://www.dailymaverick.co.za/article/2025-07-17-command-line-to-control-room-sas-infrastructure-vulnerable-to-cyberattacks/
- https://lexafrica.com/2026/02/south-africas-cybersecurity-shift-2026/

**Interception and surveillance**
- https://www.saflii.org/za/cases/ZACC/2021/3.html
- https://www.thepresidency.gov.za/president-ramaphosa-refers-rica-amendment-bill-national-assembly
- https://www.parliament.gov.za/press-releases/national-assembly-passes-four-bills-during-plenary-sitting
- https://www.parliament.gov.za/press-releases/media-statement-committee-justice-committed-remedying-unconstitutionality-rica-act
- https://www.thepresidency.gov.za/president-ramaphosa-assents-general-intelligence-laws-amendment-bill
- https://presscouncilsa.org.za/2025/03/29/general-intelligence-law-amendment-act-adopted-still-deeply-flawed/
- https://intelwatch.org.za/2024/04/08/despite-important-gains-the-new-general-intelligence-laws-amendment-bill-fails-to-safeguard-against-a-second-state-capture/
- https://iol.co.za/news/politics/2025-05-10-justice-committee-debates-future-of-the-protection-of-state-information-bill/
- https://www.news24.com/politics/parliaments-legal-services-agree-with-ramaphosa-on-secrecy-bill-20250508-0329
- https://techcentral.co.za/police-bodycam-tender-popia-biometrics-rules/286116/
- https://www.biometricupdate.com/202609/south-africa-seeks-facial-recognition-for-police-body-dashboard-cameras

**Data, cloud and CLOUD Act**
- https://www.gov.za/sites/default/files/gcis_document/202406/50741gen2533.pdf
- https://www.saflii.org/za/legis/consol_act/sitaa88o1998386/
- https://www.law.cornell.edu/uscode/text/18/2713
- https://www.justice.gov/criminal/cloud-act-resources

**AI**
- https://www.sanews.gov.za/south-africa/draft-ai-policy-approved-publication-public-comment
- https://www.sanews.gov.za/south-africa/minister-announces-withdrawal-draft-ai-policy
- https://en.wikipedia.org/wiki/Draft_South_Africa_National_Artificial_Intelligence_(AI)_Policy_2026
- https://au.int/en/documents/20240809/continental-artificial-intelligence-strategy
- https://artificialintelligenceact.eu/article/5/
- https://artificialintelligenceact.eu/annex/3/
- https://digital-strategy.ec.europa.eu/en/policies/regulatory-framework-ai
- https://www.coe.int/en/web/artificial-intelligence/the-framework-convention-on-artificial-intelligence
- https://www.bundesverfassungsgericht.de/SharedDocs/Pressemitteilungen/EN/2023/bvg23-018.html

**Procurement**
- https://www.thepresidency.gov.za/president-ramaphosa-assents-public-procurement-bill
- https://www.treasury.gov.za/comm_media/press/2026/20260610%20Media%20Statement%20-%20Draft%20Public%20Procurement%20Regulations%20-%20Extension%20of%20Timeframe%20for%20Submission.pdf
- https://www.treasury.gov.za/comm_media/press/2026/2026070801%20Media%20statement%20-%20National%20Treasury%20Investigation%20into%20Transversal%20Contracts.pdf
- https://www.treasury.gov.za/comm_media/press/2025/Default.aspx
- https://secure.csd.gov.za

**Africa**
- https://dataprotection.africa/malabo-convention-set-to-enter-force/
- http://kenyalaw.org/kl/fileadmin/pdfdownloads/LegalNotices/2021/LN263_2021.pdf
- https://dataprotection.africa/kenya/
- https://dataprotection.africa/nigeria/
- https://dataprotection.africa/rwanda/
- https://dataprotection.africa/ghana/
- https://dataprotection.africa/egypt/
- https://dataprotection.africa/zambia/
- https://dataprotection.africa/botswana/

**Export controls**
- https://www.federalregister.gov/documents/2025/01/15/2025-00636/framework-for-artificial-intelligence-diffusion
- https://www.bis.gov/press-release/department-commerce-announces-rescission-biden-era-artificial-intelligence-diffusion-rule-strengthens
- https://www.whitehouse.gov/presidential-actions/2025/02/addressing-egregious-actions-of-the-republic-of-south-africa/

**Ethics (not fetched; unverified)**
- https://www.wsj.com/articles/huawei-technicians-helped-african-governments-spy-on-political-opponents-11565793017
- https://foreignpolicy.com/2018/07/24/beijings-big-brother-tech-needs-african-faces/
- https://citizenlab.ca/2018/09/hide-and-seek-tracking-nso-groups-pegasus-spyware-to-operations-in-45-countries/
- https://citizenlab.ca/2020/12/running-in-circles-uncovering-the-clients-of-cyberespionage-firm-circles/
- https://www.bbc.com/news/world-africa-43471707
- https://www.theguardian.com/uk-news/2018/apr/04/cambridge-analytica-used-violent-video-to-try-to-influence-nigerian-election
- https://www.theverge.com/2018/2/27/17054740/palantir-predictive-policing-tool-new-orleans-nopd
- https://www.england.nhs.uk/digitaltechnology/nhs-federated-data-platform/
