<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Founding research dossier for Vigil (P0) -->

# Vigil — Founding Research Dossier

> **What this is.** The founding research and strategy for Vigil, an operational intelligence platform "for Africa, by Africans". It takes Palantir apart, checks the Gemini brief claim by claim, maps the African problem space, and turns all of it into an architecture, a wedge, a plan and a set of engineering rules.
>
> **Status:** P0 — for founder sign-off · **Date:** 25 September 2026 · **Classification:** Internal · **Owner:** Kone Tshivhinda
>
> **Evidence base:** seven parallel research streams run on 24–25 September 2026, reading roughly 1,500 web sources (company filings, official documentation, legislation, regulators, reputable press). The full sourced reports are in [`appendix/`](appendix/). Claims marked **[UNVERIFIED]** could not be confirmed from a primary source during research. Nothing in this dossier is legal advice. Counsel must confirm every legal point before anyone relies on it.
>
> **Doctrine:** Big vision, small steps. The engineering rules come from Karpathy: think before coding, simplicity first, surgical changes, goal-driven execution. They are what keep an ambitious platform from collapsing under its own weight. See [`INSTRUCTIONS.md`](../../INSTRUCTIONS.md) §5.

---

## 0. The answer on one page

**The thesis.** Palantir's advantage isn't "AI" and it isn't dashboards. It is three things working together:

1. an **Ontology** that can act on the world (typed objects and links, plus transactional *Actions* that write back into systems of record);
2. a **mandatory security model** (markings that follow data through its lineage, purpose checks, audit);
3. **Apollo**, a delivery system that ships the same software into cloud, on-prem, air-gapped and edge sites.

All three can be rebuilt in 2026 from permissively licensed open source, plus a substantial but bounded core of original engineering (Warden, OMS, Indexer, Object Set Service, Actions, Relay). What can't be copied is Palantir's US accreditations, its $9.2B of cash and 20 years of relationships. Vigil doesn't need them: Palantir has no verified African customer or office (§2.6).

**The opening.** Across the world, governments are choosing sovereign alternatives to Palantir:
- France's DGSI and Germany's BfV both chose the French vendor ChapsVision in 2026 (the DGSI announced its switch on 16 Jun 2026).
- The Swiss Army advised against Palantir.
- London's mayor blocked a £50M Met Police deal.
- Germany's Constitutional Court struck down the Hesse and Hamburg laws authorising automated police data analysis in 2023 (Hesse's hessenDATA ran on Palantir Gotham) ([appendix 01 §6](appendix/01-palantir-business.md)).

Palantir has **no verified African customer or office**. In Africa the incumbent is Huawei's "Safe City" product ([CSIS](https://www.csis.org/analysis/watching-huaweis-safe-cities)). The market wants three things, and neither incumbent offers all of them: **sovereign, lawful and auditable**.

**The decisions this dossier asks the founders to sign off:**

| # | Decision | Where argued |
|---|---|---|
| D1 | Position Vigil as **"constitutional by design"** sovereign operational intelligence, not "an African Palantir". The law is the product spec, not red tape. | §1, §9 |
| D2 | Build the **three moats first**: security kernel (Warden), ontology with write-back (Core), sovereign delivery (Relay). Chat and agents sit on top. | §6 |
| D3 | The ontology is **a metadata-driven layer over polyglot persistence** (Postgres + Iceberg + OpenSearch), **not a graph database**. | §6.3, [ADR-0002](../adr/ADR-0002-ontology-not-a-graph-database.md) |
| D4 | **Seven programming languages plus SQL, adopted in stages**, with hard boundaries between them: Java, TypeScript, Python, Rust and SQL from day one; Go when Relay starts; Kotlin, and Elixir if P28 chooses it over Go, when the field products start. | §7, [ADR-0001](../adr/ADR-0001-language-charter.md) |
| D5 | **Licence policy is a board-level control.** Apache/MIT/BSD/MPL go in the shipped core. BSL, SSPL, ELv2 and non-commercial licences stay out. AGPL is allowed only as an unmodified, separate service. | §8, [ADR-0003](../adr/ADR-0003-open-source-licence-policy.md) |
| D6 | **First wedge: Corridor Integrity.** Freight rail, ports and border corridors, starting with infrastructure crime, built on a shared **counterparty (procurement-integrity) graph**. | §5 |
| D7 | **Phase Zero on public data only:** (1) procurement-integrity graph, (2) Durban port and corridor twin. One ontology, two lenses. | §11 |
| D8 | **Sovereign hosting:** South African carrier-neutral colocation plus on-prem and air-gapped editions. US hyperscaler regions only for a separate commercial tier. No prompt leaves South Africa. | §10, [ADR-0004](../adr/ADR-0004-sovereign-first-deployment.md) |
| D9 | **Published red lines and warrant-gated modules** from day one. An external oversight board before the first security customer. | §9 |
| D10 | **Revenue first:** paid bootcamps, never free pilots. Services stay at or below 40% of revenue, and contribution margin at or above 40%, by month 24. | §12–13 |
| D11 | **Forward-deployed engineering is the defining role**, but every engagement must leave reusable product behind. | §14 |
| D12 | **"Vigil" stays a codename until the trademark is cleared.** Altron has held vigil.co.za since 2005, and 3xLOGIC sells VIGIL surveillance software. | §17, [ADR-0006](../adr/ADR-0006-codename-and-trademark-clearance.md) |

---

## 1. The vision, restated, with one pushback

**The vision:** a platform that turns Africa's fragmented operational data into a living model of real assets, people, places and events, and lets operators make decisions that change the real world. Palantir-class in capability. Built, owned and run in Africa.

**The pushback.** The brief says "no limit or red tape". No limit on ambition: agreed. But three reasons say the red tape is the product:

1. **Legality is the reason buyers switch.** Europe's 2025–2026 defections from Palantir were about jurisdiction, legality and lock-in, not features ([appendix 01 §6](appendix/01-palantir-business.md)). A South African buyer faces the same questions, sharpened by POPIA and the Constitution.
2. **Some rules are hard gates.**
   - POPIA s57(1)(a) needs **prior authorisation from the Information Regulator** before unique identifiers are linked across organisations. That is literally what entity resolution does. It adds 4–17 weeks per multi-party deployment ([appendix 04 §1.5](appendix/04-legal-regulatory-ethics.md)).
   - Bulk interception is unlawful (*amaBhungane*, ConCourt 2021), pending GILAB's judge-authorised regime, whose regulations are not yet in place.
   - POPIA applies even to SAPS: the Regulator said so in 2023 and again in 2026.
3. **Money doesn't speed up the real bottlenecks.** With unlimited capital you could hire faster, buy GPUs and run all tiers in parallel. You still could not shorten data-access agreements, prior authorisations, security clearances (6–18 months, an estimate), procurement cycles (3–9 months, an estimate) or earning trust. **The binding constraints are legitimacy and access, not money.** So they are designed in from the start.

---

## 2. Palantir, taken apart

### 2.1 Palantir in 2026: the numbers

| Metric | Value | Source |
|---|---|---|
| FY2025 revenue | $4.475B (+56%); GAAP net income $1.625B | [Q4-25 release](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000004/a2025q4ex991earningsrelease.htm) |
| Q2 2026 revenue | $1.935B (+93% y/y); adjusted operating margin 62%; Rule of 40 = 155% | [Q2-26 release](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000039/a2026q2ex991pressrelease.htm) |
| FY2026 guidance | $8.15B (+82%) | same |
| Market cap | $458B (23 Sep 2026) | [StockAnalysis](https://stockanalysis.com/stocks/pltr/market-cap/) |
| Top-20 customer average | $124M TTM (was $24.8M in 2019); net dollar retention 157% | [Q2-26](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000039/a2026q2ex991pressrelease.htm), [S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm) |
| Staff | 4,429 (Dec 2025), about half of them FDEs | [10-K](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm) |
| International commercial (Vigil's bucket) | +2% in FY2025, +26% in H1 2026; international revenue fell to about 19% of total | [10-K](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm), [TIKR](https://www.tikr.com/blog/wall-street-just-gave-palantir-its-highest-ever-price-target-right-when-europe-started-saying-no) |
| Road to profit | About 20 years to its first GAAP-profitable year (2023). Accumulated deficit $3.96B by mid-2020. SBC was 50% of revenue in 2021 | [S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm), [appendix 01](appendix/01-palantir-business.md) |

**What this means for Vigil:** Palantir's growth is American. Outside the US it relies on UK and NATO government work and on renewals, and continental Europe distrusts it. It can also afford to subsidise entry into new markets through joint ventures (Aither with Dubai Holding) and big headline pledges (£1.5bn in the UK). Expect Palantir to show up in Africa **through a Gulf-style JV or a hyperscaler partner** once Africa looks attractive. Vigil's window is the next 24–36 months.

### 2.2 The product suite

| Palantir | What it is | Vigil equivalent |
|---|---|---|
| **Gotham** (2008) | Defence, intelligence and police: link analysis, geospatial, targeting | Later, gated **Vigil Command** profile on the same Core (not before 2028) |
| **Foundry** | Commercial and civil platform: data integration, ontology, apps | **Vigil Forge + Core + Cockpit** |
| **Ontology** | "A digital twin of the organization": semantic objects/links + kinetic actions/functions + dynamic security | **Vigil Core** (OMS, Indexer, Object Set Service, Actions, Functions) |
| **Apollo** | Continuous delivery everywhere, including air-gapped and classified | **Vigil Relay** |
| **AIP** (2023) | LLM functions, agents, automations and evals on the ontology; 2026: "AI FDE", Ontology MCP, sovereign AI OS with NVIDIA | **Vigil Mind** |
| **Maven / TITAN / Skykit** | Military fusion, edge ground stations, portable kits | **Vigil Field** (edge node, Android app, SMS/USSD/WhatsApp), civil first |
| **Bootcamps** | 1–5 day builds on real customer data, which replaced 8–12 week pilots | Paid **Vigil Bootcamps** |

Sources: [appendix 01 §2](appendix/01-palantir-business.md), [appendix 02](appendix/02-palantir-technical.md).

### 2.3 How it actually works

From Palantir's own public documentation ([appendix 02](appendix/02-palantir-technical.md)):

- **Semantic vs kinetic vs dynamic.** Palantir calls the Ontology "a digital twin of the organization, containing both the **semantic elements (objects, properties, links)** and **kinetic elements (actions, functions, dynamic security)**" ([docs](https://www.palantir.com/docs/foundry/ontology/overview/)).
  - Semantic elements are the nouns. Kinetic elements are the verbs, meaning governed write paths back into reality.
  - "Dynamic" means logic, simulation and security evaluated at runtime against live state.
  - **"Kinetic" does not mean simulation.** Several secondary explainers get this wrong, and so does the Gemini brief (§3).
- **The Ontology is five services over versioned datasets, not a graph database** ([object backend](https://www.palantir.com/docs/foundry/object-backend/overview/)):

  | Service | What it does |
  |---|---|
  | Ontology Metadata Service (OMS) | Holds the types |
  | Object Data Funnel | An indexing pipeline: changelog → merge edits by primary key → index → hydrate |
  | Object storage (OSv1 "Phonograph" → OSv2) | Purpose-built object databases |
  | Object Set Service (OSS) | Serves all reads: filter, search-around, aggregate |
  | Actions service | Makes transactional, permissioned edits |

- **An Action is one transaction.**
  - It has typed parameters, rules and submission criteria.
  - It may call **one writeback webhook, which runs before commit**. If the webhook fails, nothing changes.
  - After commit, side-effect webhooks run in no guaranteed order. Edits land in an edit history.
  - Documented limits: 10,000 objects per action, 3 MB per edit (OSv2), and at most 3 search-arounds in functions ([limits](https://www.palantir.com/docs/foundry/action-types/scale-property-limits)).
- **Data layer.**
  - Datasets use SNAPSHOT, APPEND, UPDATE and DELETE transactions over files in distributed storage, with Cassandra holding metadata. Each file is encrypted with its own AES-256 key.
  - Spark handles batch, Flink handles streaming, and since August 2026 DataFusion (Rust) powers the "faster pipelines". Iceberg tables and virtual tables are supported.
  - Global Branching reached general availability only in May 2026.
- **Security is mandatory-first.**
  - **Markings are conjunctive** (a user must hold all of them) and **propagate along data dependencies**. Removing one needs a central "Expand Access" permission.
  - Classification-based access controls add levels plus *disjunctive* releasability.
  - Restricted views give row-level policies. Checkpoints force a written justification. Cipher handles pseudonymisation. Audit logs follow the audit.3 schema.
  - **AI agents are bound by the same policies as humans** ([markings](https://www.palantir.com/docs/foundry/security/markings/), [blog, Apr 2026](https://blog.palantir.com/connecting-agents-to-decisions-277dee8ddb40)).
- **Apollo is hub-and-spoke.**
  - A hub issues *plans*; spokes poll for them. Releases move through release channels, and a release that fails health checks is recalled.
  - Constraints include maintenance windows and dependency ranges, and failures trigger automatic rollback plans.
  - Air-gapped sites import one-way *bundles* ([Apollo docs](https://www.palantir.com/docs/apollo/core/how-apollo-works/)).
- **Palantir is polyglot:**
  - Java for the backbone services and Conjure RPC;
  - Go for infrastructure and agents;
  - TypeScript for every UI and the OSDK;
  - Python for transforms and ML;
  - Scala only through its Spark fork;
  - Rust rising on performance paths.
  - Storage appears to be dominated by Cassandra and Elasticsearch (inferred from job postings; [appendix 02 §7](appendix/02-palantir-technical.md)).

### 2.4 The operating model

- **Deltas and Devs.** Forward-deployed engineers ("Deltas") work at the customer site. Product ("Dev") engineers turn repeated field work into platform tools. Foundry's ingestion (Magritte), analysis (Contour) and app-building (Workshop) tools were all born this way ([Qureshi, 2024](https://nabeelqu.co/reflections-on-palantir)).
- **Deployment got fast.** Installation time fell from more than 5 months (2019) to 14 days (2020) ([S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm)).
- **Bootcamps replaced pilots.** In 2023 Palantir ran more than 500 bootcamps, compared with 92 pilots two years earlier: "your 10 hours, your 10 months" ([Q4-23 call](https://www.fool.com/earnings/call-transcripts/2024/02/05/palantir-technologies-pltr-q4-2023-earnings-call-t/)).
- **Pricing and sales.** Contracts run 1–5 years; usage is metered in compute-seconds and "Ontology volume"; large buyers move to enterprise agreements. Sales cycles are typically 6–9 months ([10-K](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm)).
- **The FDE hiring boom.** FDE job postings rose more than 800% in 2025 and more than 1,000% year on year in 2026, with a median above $188k. OpenAI, Anthropic, Microsoft and NVIDIA all hire them ([Fortune, Sep 2026](https://fortune.com/2026/09/03/forward-deployed-engineers-fast-growing-six-figure-silicon-valley-job-integrate-ai-with-customers-tech-careers-palantir/)). Vigil will compete with AI labs for this talent.

### 2.5 Where Palantir fails

Across its controversies, the failures cluster in three places:
- **Surveillance of citizens without a clear legal basis:** ICE, New Orleans predictive policing, NHS opt-out mistrust.
- **Sole-source and direct awards:** the Met Police deal was blocked for procurement breaches.
- **Foreign-jurisdiction exposure:** the Swiss Army report warned that "sensitive data could be accessed by the US government".

**None of these were technology failures.** Germany's Federal Constitutional Court (16 Feb 2023, 1 BvR 1547/19) set out, in some detail, the safeguards a system like this needs:
- an "identifiable danger" threshold;
- statutory limits on the types of data and the methods, including self-learning AI;
- exclusion of intrusive sources;
- separating suspects from third parties;
- published administrative rules.

Vigil builds these in as product features (§9).

### 2.6 Africa: the empty chair

- **No verified African customer, office or strategy.**
  - Palantir's jobs board lists Abu Dhabi, Dubai and Tel Aviv, and nothing in Africa.
  - Its African footprint is humanitarian, through the WFP partnership ([appendix 01 §7](appendix/01-palantir-business.md)).
- **The incumbent is Chinese.**
  - Huawei had 73 Safe City agreements in 52 countries, 22% of them in sub-Saharan Africa.
  - Its claimed 46% cut in crime in Kenya's Safe City areas was contradicted by police data ([CSIS](https://www.csis.org/analysis/watching-huaweis-safe-cities)).
  - Reports of Huawei technicians helping Uganda and Zambia spy on opponents (WSJ, 2019) and CloudWalk's 2018 face-data deal with Zimbabwe **[UNVERIFIED in session]** define the reputational risk for everyone in this category.

### 2.7 What is and isn't replicable in 2026

| Replicable | Not replicable (yet) |
|---|---|
| Ontology + Actions + write-back on open source (Postgres, Iceberg, Temporal) | US accreditations (IL5/FedRAMP) and programs of record |
| Mandatory markings + lineage propagation (Rust policy core) | $9.2B cash and 157% net dollar retention |
| Apollo-lite on Zarf/Flux/Harbor/RKE2 | Two decades of classified relationships |
| Bootcamp sales motion; AI-assisted FDE work | A 4,400-person organisation |
| Open-weight models (gpt-oss, Qwen, Mistral 3, Gemma 4), which Palantir says rival frontier models on production tasks | — |
| **Vigil-only advantages:** B-BBEE status, SA clearances, in-country sovereignty, African-language AI, African-name entity resolution, cost | — |

---

## 3. The Gemini brief: verified, corrected, extended

Overall: the Gemini brief is **directionally right about the ontology and forward-deployed engineering, and wrong or silent on most of what decides success in Africa.**

| # | Gemini claim | Verdict | Evidence and correction |
|---|---|---|---|
| 1 | "Palantir's true moat… is an operational ontology" | **Partly right** | The moat is a trio: ontology with kinetic Actions, mandatory security that propagates through lineage, and Apollo delivery into air-gapped estates. Account expansion driven by forward-deployed engineers is on top of that (net dollar retention 157%). |
| 2 | Digital twin with real-time write-back | **Right, but imprecise** | Write-back is transactional: *one* pre-commit writeback webhook, then side effects after commit, with limits (10k objects per action). "Real-time" depends on the target system. The hard part is integrating with the customer's systems of record (SAP etc.), with idempotency and compensation. |
| 3 | Diagram: "Kinetic *Simulation* & Decision Layer" | **Wrong term** | In Palantir's usage, *kinetic* means Actions, Functions and write paths, not simulation. The diagram is missing the security and governance plane, versioned datasets, lineage and branching, the object indexing and storage services, and the delivery plane. |
| 4 | Ingest everything (SQL, scanned manifests, WhatsApp) and resolve it into single entities | **Right direction, legally naïve** | Cross-organisation linkage of unique identifiers needs **POPIA s57(1)(a) prior authorisation**. Resolved entities should be *views with provenance* rather than destructive merges (the Quantexa pattern), with human review. Ingesting messaging content raises RICA and POPIA questions; use only business-owned channels (WhatsApp Business API) with a lawful basis. |
| 5 | "Instead of tables and rows" | **Misleading** | The ontology is a semantic layer *over* tables and indexes. Vigil generates a typed physical table per object type from metadata. The physical and logical schemas are separate (the Cognite containers-vs-views model). |
| 6 | "Click Reroute → dispatch, notify, adjust capacity" | **Right; the hard part is missing** | This needs sagas (Temporal), approvals, idempotent connectors and **POPIA s71**, which forbids solely automated decisions with legal effect. Vigil keeps a human in the loop on every adverse decision. |
| 7 | Wedges: utilities, freight corridors, extraction | **Generic** | Utilities: municipal buyers are insolvent. They owe Eskom R120bn, and 45% adopted unfunded budgets, so utilities are an expansion market. Mining majors already run AVEVA PI, SAP and their own operations centres, so they are second customers. **Freight corridors are the strongest**: logistics failures cost about R353bn in 2023, a 2026–27 reform window creating new private buyers, and Transnet spending about R4bn a year on security while still losing 4.5 mt a year to cable theft. **Missing from Gemini:** procurement integrity (R145bn of municipal irregular expenditure), the fastest wedge to build on public data. |
| 8 | "Built Gotham for defence first, then abstracted into Foundry" | **Right, but not the model to copy** | Palantir needed a patient anchor customer (In-Q-Tel/CIA), about $2.3B of pre-IPO capital and 20 years to profit. South African startups raised **$83M in H1 2026** in deals of $100k+ (Africa: The Big Deal, via [Tech In Africa](https://www.techinafrica.com/egypt-tops-africas-h1-2026-startup-funding-but-nigeria-reclaims-the-equity-crown/)). Vigil's path is civil and commercial first, revenue-first, and defence last. |
| 9 | Forward-deployed engineering: embed, find the bottleneck, build in days | **Right, and out of date** | Palantir moved from free 8–12 week pilots to 1–5 day bootcamps, and in 2026 it sells an "AI FDE" agent that automates FDE work. The model only works if every engagement leaves reusable product behind (the Delta→Dev loop). Otherwise you become a systems integrator. |
| 10 | "First workflow in days, not months" | **True only once the platform exists** | Palantir got to 14-day installs after about 15 years of tooling. For Vigil, months 0–9 are for the platform, and days-to-workflow is a Tier 2+ target. |
| 11 | "Build the core graph primitive first (Postgres graph modelling or a graph DB + vector)" | **Half right** | Build the **security kernel** first, because markings that propagate through lineage cannot be retrofitted. Then build the ontology *services*, not a graph database. Postgres 18/19 (SQL/PGQ graph queries in PG19), Iceberg and OpenSearch avoid a dedicated graph database. (Kùzu, an embedded graph database, was archived in Oct 2025.) |
| 12 | "Agentic ingestion: you don't need 500 engineers to write parsers" | **Right, with guardrails** | Document AI works (Docling, PaddleOCR 3, olmOCR 2). But untrusted documents carry prompt-injection risk, extractions hallucinate, and sovereignty matters: Bedrock in Cape Town uses *global* cross-region inference. Agents should *generate* reviewed, deterministic connector code rather than run unsupervised, and every extracted field needs provenance and a confidence score. |
| 13 | Prototype on port AIS, load-shedding plus substations, fault reports | **Good idea, stale specifics** | There has been **no load-shedding since 16 May 2025** (490 days to 18 Sep 2026), so a load-shedding twin is stale. Licensing traps: Global Fishing Watch is non-commercial; the free tier of Open-Meteo is non-commercial; **SAFLII bans automated access**; the Central Supplier Database is closed. Research ranks the procurement-integrity graph first (3–4 weeks) and the Durban port twin second (5–6 weeks). |
| 14 | "Stripping away regulatory red tape" | **Wrong framing** | See §1 and §9. Europe is rewarding the lawful, sovereign alternative. |
| 15 | *(absent)* Languages and stack, delivery, sovereignty, licences, talent, capital, procurement law, B-BBEE, naming, ethics, connectivity, African-specific AI | **Missing** | Covered in §6–§17 of this dossier. |

---

## 4. The African problem landscape

The numbers below are dated and sourced in [appendix 03](appendix/03-africa-market.md).

| Domain | The pain (latest) | Who pays | Read |
|---|---|---|---|
| **Freight rail & ports** | Logistics failures cost about **R353bn in 2023**. Rail moved 167.9 mt in 2025/26, against a 250 mt target for 2030. Transnet spends **about R4bn a year on security** and still loses **4.5 mt a year to cable theft**. Durban ranked last in CPPI 2024 and was "most improved globally" in CPPI 2025. | Transnet/TRIM, **11 private rail operators with access agreements** (the network statement covers 12; mainline from 2027), ICTSI (Durban Pier 2, from Jan 2026), miners, forwarders | **Best-funded problem; reform window now** |
| **Borders** | Border delays cost up to **R16bn a year**. The worst 5% of Beitbridge crossings take 57 hours. The BMA is spending R12.5bn to rebuild six posts from late 2026. | BMA concessionaires, forwarders, SARS | Sell to private consortia |
| **Energy** | No load-shedding since May 2025, but municipal arrears are **R120.26bn** (projected R358bn by FY2031) and theft costs **about R17.5bn a year**. Copper theft costs the economy **more than R45bn a year**. | Eskom Distribution, NTCSA (R440bn transmission plan) | Expansion market |
| **Water** | **47.3% of water is lost or unbilled.** 47% of wastewater systems are critical. AGSA found R18.98bn lost in 2023/24. Johannesburg loses 49%. | Metros (cash-constrained), DWS | Expansion; buyer solvency is weak |
| **State integrity** | **R145.21bn of municipal irregular expenditure** since 2021-22, 87% from breaching procurement rules. Only 39 of 257 municipalities got clean audits. SA was delisted from the FATF grey list on 24 Oct 2025. | Treasury OCPO, AGSA, SIU, IDAC, banks, DFIs | **Fastest public-data wedge** |
| **Public safety** | Organised syndicates are behind 57% of carjackings. Vumacam runs 7,000+ cameras. The Madlanga Commission heard testimony alleging a police cartel. SITA tender RFB 3286-2026 (SAPS body cameras with FRT/ANPR) closes 29 Sep 2026 and cites no POPIA s27 ground, per TechCentral (Sep 2026). | SAPS, metros, private security | **Highest ethical risk; only infrastructure-crime cases for asset owners** |
| **Mining** | Illegal mining costs about R21bn a year (older estimate). Miners already run plant-data platforms. | Majors, exporters | Second customer (security, export logistics) |
| **Defence** | SANDF budget about US$3.6bn, 63% of it salaries. Lessons from the SAMIDRC deployment in the DRC. Buying cycles take 3–7 years. | DoD, Armscor, CSIR | **Last**, once clearances and NCACC permits are in place |
| **Pan-Africa** | Lobito corridor (US$700m AFC/AfDB), Mombasa and the Northern Corridor, the Maputo corridor, Nigeria's ports and customs. EUDR (the EU deforestation rule) applies to large operators from 30 Dec 2026. | LAR/Trafigura, KPA/KRA, CFM, revenue authorities | Corridor data model exports directly |

**The gap no one fills.** Nobody in South Africa offers a vendor-neutral model that joins **asset and incident data** with **procurement, company-ownership and court data**, and then **writes back into operational and legal workflows** ([appendix 03 §9](appendix/03-africa-market.md)).

---

## 5. Choosing the wedge (greenfield re-score)

The research scored 12 wedges assuming an existing Transnet relationship and reusable MB code. The founders have since said **Vigil is greenfield**, so "team edge" below counts only the founders' domain know-how (legal and evidentiary practice, procurement, entity resolution), with no relationships and no code carried over.

Weights: pain 20%, budget 20%, data access 15%, sales cycle 10%, team edge 15%, expansion 10%, ethics 10%. Every score is out of 5, and **5 is always best** (for sales cycle, 5 is shortest; for ethics, 5 is lowest risk).

| Wedge | Pain | Budget | Data | Cycle | Edge | Expansion | Ethics | **Score** |
|---|---|---|---|---|---|---|---|---|
| **A. Corridor Integrity** (rail, port, border; infrastructure crime) | 5 | 4 | 3 | 3 | 3 | 5 | 4 | **3.90** |
| **B. Counterparty & procurement-integrity graph** | 4 | 3 | 5 | 4 | 4 | 4 | 3 | **3.85** |
| **C. Control tower for private rail and port users** | 4 | 4 | 3 | 4 | 2 | 4 | 5 | **3.65** |
| D. Grid theft and crime (Eskom/NTCSA) | 4 | 4 | 3 | 2 | 2 | 4 | 4 | 3.35 |
| E. Bank AML after grey-listing | 3 | 4 | 3 | 3 | 3 | 3 | 4 | 3.30 |
| F. Mine security | 4 | 4 | 3 | 3 | 2 | 3 | 3 | 3.25 |
| G. Border-post data layer | 4 | 3 | 3 | 2 | 2 | 4 | 3 | 3.05 |
| H. Metro public-safety fusion | 5 | 3 | 4 | 3 | 1 | 3 | 1 | 3.05 |
| I–L. Livestock, water twin, municipal revenue, defence C2 | — | — | — | — | — | — | — | 2.25–2.95 |

**Decision (D6).**
- **Build B's graph first**, on public data, in weeks. It is the shared "party layer": organisations, people, contracts, restrictions, court matters.
- **Sell A as the flagship operational wedge.** One ontology joins rail and port assets, incidents, security contractors, procurement and court cases on 1–2 corridors. It writes back into maintenance, dispatch, contractor-penalty and prosecution workflows. Headline KPIs:
  - km of cable stolen;
  - train-hours lost;
  - tonnes lost;
  - repair time;
  - arrest-to-conviction ratio.
- **C (the private-operator control tower) is the first expansion.** It is solvent, private and time-bound: operators run on the main line from 2027.

The ranking holds without the Transnet relationship. A and B stay on top, 0.05 apart, and C follows.

**Greenfield changes the order of buyers, not the wedge.** Private buyers close fastest, so they come first:
1. private train operators and mining exporters;
2. ICTSI and freight forwarders;
3. security contractors;
4. banks and DFIs, who buy B on its own.

Transnet/TRIM stays the anchor target, but through an open tender, an NLCC/BUSA-funded proof of value, or an integrator's existing contract. It never comes through a sweetheart deal: a procurement-integrity company cannot survive the optics.

**Say no for 2026–27:** general policing, municipal revenue collection, defence C2, and facial recognition in version 1.

---

## 6. Product architecture: the Vigil suite

### 6.1 Seven products on one kernel

```
                    ┌──────────────────────────── VIGIL COCKPIT (TypeScript) ─────────────────────────────┐
  Operators,        │ Object Explorer · Map/COP · Link Graph · Timeline · Case Workbench · App Builder      │
  analysts,         └───────────────▲───────────────────────────────────────────────▲──────────────────────┘
  field crews                       │ generated Vigil SDK (TS/Py/Java/Kotlin) · MCP │
                    ┌───────────────┴──────── VIGIL CORE — the Ontology (Java + Rust) ┴─────────────────────┐
  VIGIL MIND ◀────▶ │ Ontology Metadata · Indexer/Funnel · Object Set Service · Actions (+Temporal sagas) · │ ◀──▶ VIGIL FIELD
  (Python)          │ Functions runtime (WASM/containers) · Edit log · Lineage                              │      (Rust edge node,
  models, agents,   └───────────────▲────────────────────────────────────────────────▲─────────────────────┘      Kotlin app,
  ER, doc AI,                       │ datasets, streams                              │ write-back                 Elixir comms)
  evals             ┌───────────────┴──────── VIGIL FORGE — data integration ─────────┴─────────────────────┐
                    │ Connectors (Java) · Edge/ingest agent (Rust) · Kafka/NATS · Iceberg lakehouse ·       │
                    │ Transforms (Python/DataFusion/Spark) · Data quality · Lineage events                  │
                    └───────────────────────────────────────────────────────────────────────────────────────┘
  ══════════ VIGIL WARDEN — security kernel (Rust policy core + Keycloak + OpenFGA + audit ledger) ══════════
  ══════════ VIGIL RELAY — sovereign delivery (Go hub/spoke controller + Zarf + Harbor + Flux + RKE2/k3s) ═════
```

| Product | Palantir analogue | Language(s) | Phases |
|---|---|---|---|
| **Warden**: markings, classification + releasability, purpose/justification, ReBAC, audit ledger, lineage propagation | Markings, CBAC, Checkpoints, audit | Rust core (native + WASM), Java adapters | P3 |
| **Core**: OMS, Indexer, OSS, Actions, Functions, edit log | Ontology (OMS/Funnel/OSv2/OSS/Actions) | Java 25 / Spring Boot 4 / Spring Modulith 2; Rust indexer and traversal | P5–P9 |
| **Forge**: connectors, edge agent, streams, lakehouse, transforms, lineage | Data Connection, Pipeline Builder, Code Repos, datasets | Java connectors; Rust agent; Python transforms (Dagster) | P4, P11, P12, P24–P26 |
| **Mind**: model gateway, ontology tools and MCP, agents, evals, entity resolution, document AI, speech | AIP Logic / Chatbot Studio / Automate / Evals / Ontology MCP | Python 3.13+ / FastAPI | P14–P17 |
| **Cockpit**: object views, map, graph, timeline, case workbench, app builder | Workshop, Object Explorer, Map, Gaia/Graph, Quiver, Notepad | TypeScript 7 / Next.js / React | P10, P13, P18, P19, P31 |
| **Field**: edge node, Android offline app, SMS/USSD/WhatsApp gateway | Skykit/TITAN/mobile (civil) | Rust, Kotlin, Elixir | P25, P27, P28 |
| **Relay**: signed bundles, hub/spoke plans, constraints, release channels, air-gap | Apollo + Rubix | Go | P20–P21 |

### 6.2 Five design laws, taken from Palantir's own history

1. **Physical ≠ logical schema.** OMS metadata generates a typed Postgres table per object type, indexed into OpenSearch. Links are foreign keys or join tables. No EAV tables on hot paths.
2. **Markings are data, and they propagate.** Every row, object, link, dataset and column carries markings. Anything derived from marked data takes the *union* of its inputs' markings. Removing a marking is an audited, two-person Action.
3. **An Action is one database transaction plus an outbox.** The writeback webhook runs synchronously before commit. Edits, the edit log and the outbox commit atomically. Side effects are idempotent Temporal activities.
4. **Edits are events.** An append-only edit log is the source of truth. Current state, history, audit and "decision lineage" are all derived from it. Palantir's OSv1→OSv2 migration lost edit history. Vigil doesn't take that risk.
5. **Branch everything from v1.** Datasets (Iceberg branches and tags), ontology schema and indexes can all be branched. Palantir shipped global branching only in May 2026, and retrofitting it is costly.

### 6.3 Why not a graph database

Operational queries only go a few hops (Palantir's TypeScript v1 functions cap object loads at three search-arounds). Security has to be pushed down into every read: Postgres RLS, OpenSearch document-level security, scoped storage credentials. Graph databases carry real risk for viability and licensing:
- Neo4j Community is GPLv3; Memgraph is BSL; FalkorDB is SSPL.
- Kùzu was archived on 10 Oct 2025.

**PostgreSQL 19 adds SQL/PGQ graph queries** (in beta in Sep 2026, per the PG19 release notes; verify at GA). Deep link analysis runs in a Rust in-memory traversal service over Arrow, behind the same Object Set API, so an engine can be swapped in later ([ADR-0002](../adr/ADR-0002-ontology-not-a-graph-database.md)).

---

## 7. The polyglot engineering strategy

The brief asks for "the most robust languages, each doing what it's best at". That is the right instinct, and it is also how Palantir, Google and Meta work. The evidence also shows the **polyglot tax**. Uber standardised on Java and Go after engineers had to work across Python, Node, Java and Go. Google supports five languages, each with its own style and readability process ([Pragmatic Engineer](https://newsletter.pragmaticengineer.com/p/consolidating-technologies)).

The answer is a **charter with hard boundaries and staged adoption.**

### 7.1 The language charter

| Language | Tier | What it owns in Vigil | Why this language: evidence |
|---|---|---|---|
| **Java 25 (LTS)** + Spring Boot 4 / Spring Modulith 2 | Primary | Core ontology services (OMS, OSS API, Actions, Functions gateway, lineage, audit), Forge connectors and datasets, domain packs | The data ecosystem is JVM: Kafka 4, Flink 2, Spark 4, Iceberg, Polaris, OpenSearch, Keycloak. Virtual threads no longer pin carrier threads (JEP 491). Palantir's backbone is Java. The team's strongest language. |
| **TypeScript 7** + Next.js/React | Primary | Cockpit, visualisation (MapLibre, deck.gl, CesiumJS, Sigma.js), generated Vigil SDK, TS Functions | TS 7 is the native Go compiler, 8–12× faster builds. Palantir's UIs and OSDK are TypeScript. |
| **Python 3.13/3.14** + FastAPI + uv | Primary | Mind (gateway, agents, evals), entity resolution (Splink), document AI (Docling, PaddleOCR), speech, geospatial analytics, user transforms (Polars/DuckDB/PySpark), Dagster pipelines | vLLM, Ray, Splink and Docling are Python-first. Free-threaded 3.14 is supported but not relied on. |
| **Rust** | Specialist | Warden policy core (compiled native and WASM, so every runtime enforces identical semantics), Forge edge and ingest agent, Core indexer and traversal (DataFusion/Arrow), Wasmtime UDF host, OT protocol adapters | Memory safety: about 0.2 vulnerabilities per MLOC vs about 1,000 for C/C++ in Android. Cloudflare's FL2 rewrite (−50% CPU). DataFusion 55. Palantir's new pipelines run on DataFusion. |
| **Go** | Specialist | Relay (hub/spoke controller, Kubernetes operators), `vigil` CLI, NATS-side glue | Kubernetes, Temporal, NATS, OpenFGA, SpiceDB, Zarf, Talos are all Go. Palantir's infrastructure is Go. |
| **Kotlin** (+ KMP) | Bounded | Vigil Field Android app (offline-first, queued Actions) | Android holds **81.4% of mobile OS share in Africa** and 75.2% in SA (StatCounter, Aug 2026). |
| **Elixir** (BEAM) | Bounded, deferred | Vigil Field comms gateway: presence, alert escalation, WhatsApp/SMPP/USSD adapters over flaky networks | Discord scaled Elixir to 5M concurrent users; WhatsApp ran on Erlang. **Adopt only when messaging becomes a product (Tier 3), through an ADR.** A Go gateway is the fallback, because the local talent pool is thin. |
| **SQL** | First-class | Postgres schemas, RLS policies, SQL/PGQ, analytics | The shared language of every engineer. |
| C/C++/CUDA | Consumed only | GDAL, PROJ, H3, DuckDB, ClickHouse, OR-Tools, Valhalla, llama.cpp, TensorRT, via bindings | Never authored in-house. |
| Not adopted | — | Scala (Flink 2 dropped its Scala API), C#/.NET (duplicates the JVM), Julia/Zig (use OR-Tools/HiGHS; Zig has no 1.0), Mojo (watch after its 1.0 in Aug 2026), Gleam; Dart/Flutter only if iOS parity is forced | — |

Sources: [appendix 05 §1](appendix/05-polyglot-architecture.md).

### 7.2 Guardrails against the polyglot tax (enforced in INSTRUCTIONS.md §6)

1. **A tiered registry.** Primary, Specialist and Bounded tiers. A new language needs an ADR and sign-off from the lead architect.
2. **At most two languages per team or phase.**
3. **Cross language boundaries only through contracts:** Protobuf + Buf (gRPC/Connect), OpenAPI 3.1, CloudEvents + AsyncAPI, and Arrow Flight/ADBC for columnar data. **No operational database is shared across a language boundary.**
4. **Solve the N×M problems once.**
   - Authorisation is the Rust policy core, compiled to WASM and native.
   - Telemetry is OpenTelemetry.
   - SDKs are generated from ontology metadata.
5. **Golden-path templates** per language, with OTel, the policy binding, health checks, config, SBOM and attribution headers built in.
6. **Staged adoption:**

   | When | Languages added |
   |---|---|
   | Year 1 | Java, TypeScript, Python, Rust, SQL |
   | Months 6–12 | Go (with Relay) |
   | Tier 3 | Kotlin (field app), Elixir (comms) |

### 7.3 Monorepo & build

One monorepo on native toolchains: Gradle, pnpm, uv, Cargo, Go modules. `just` is the single command surface, and Buf handles protos. The layout is Bazel-ready: one package per directory, pinned toolchains, no network access in tests. Move to **Bazel 9** (Bzlmod-only since Jan 2026) once CI takes more than 20 minutes or the team passes about 40 engineers ([appendix 05 §3](appendix/05-polyglot-architecture.md)).

---

## 8. Reference technology stack

Licence policy is set out in [ADR-0003](../adr/ADR-0003-open-source-licence-policy.md). Everything below is self-hostable and works air-gapped.

| Layer | Choice | Licence | Rejected / watch | Why |
|---|---|---|---|---|
| Object storage | **SeaweedFS** (on-prem), native S3 (cloud); the S3 API is the contract | Apache-2.0 | **MinIO: exit.** The community repo was archived 25 Apr 2026. Ceph for national-DC scale | Maintained and permissive; has a built-in Iceberg REST catalog |
| Lakehouse | **Apache Iceberg v3** + **Apache Polaris** | Apache-2.0 | Nessie/lakeFS only for atomic multi-table merges | Branches and tags give Foundry-style branching |
| OLTP / hot objects | **PostgreSQL 18 → 19** + PostGIS + pgvector/pgvectorscale + pg_duckdb | PostgreSQL / OSS | CockroachDB (proprietary since 2024), Citus/ParadeDB (AGPL) | RLS, SQL/PGQ, the team's strength |
| Search | **OpenSearch** | Apache-2.0 (LF) | Elasticsearch (AGPL/ELv2/SSPL) | Foundation-governed |
| Vector | **pgvector** first; Qdrant at scale or at the edge; LanceDB for lake embeddings | OSS / Apache-2.0 | — | Postgres first |
| OLAP & telemetry | **ClickHouse**; QuestDB/SQLite at the edge; VictoriaMetrics for platform metrics | Apache-2.0 | InfluxDB 3 Core (single-node limits) | — |
| Streams | **Kafka 4.x** (core log) + **NATS JetStream** (edge/control) | Apache-2.0 | Redpanda, Materialize (BSL) | Foundation-governed |
| Stream / batch compute | **Flink 2.x**, **DataFusion** (+Comet), **Polars/DuckDB**, **Spark 4** for large jobs, Ray for ML | Apache-2.0 / MIT | Arroyo (watch) | Mirrors Palantir's Spark/Flink/DataFusion |
| Durable execution | **Temporal** (Actions and write-back sagas) + **Dagster** (data pipelines) | MIT / Apache-2.0 | Restate (BSL) | Compensation, approvals, retries into unreliable ERPs |
| Identity & authz | **Keycloak** + **OpenFGA** + **Warden** (Rust policy core) | Apache-2.0 | Zitadel (AGPL), Cedar (for customer-authored policies later) | CNCF projects; mandatory markings built in-house |
| Lineage & quality | **OpenLineage** → Vigil lineage store (enforcing); Soda/GX checks; dbt Core | Apache-2.0 | dbt Fusion (ELv2) | Lineage must *enforce*, not just show |
| Delivery | **Zarf + Harbor + Flux + Kyverno + cosign/Syft/Trivy + OpenTofu** on **RKE2** (DC), **k3s** (edge), **Talos** (appliance) | Apache-2.0 / MPL-2.0 | UDS Core (AGPL: borrow ideas only); Bitnami images (don't use) | Signed, air-gap-native |
| Geospatial | **PostGIS + H3 + Plus Codes**, **MapLibre + deck.gl + PMTiles** (offline), **CesiumJS** (3D), **Valhalla** (routing), **TiTiler + STAC** over Digital Earth Africa | Permissive | Keep ODbL layers (OSM/Overture) in **separate databases** | Offline-first maps |
| AI serving | **vLLM** (core), **llama.cpp** (edge), **LiteLLM** gateway, **Langfuse**, **Inspect + promptfoo** evals, **NeMo Guardrails** | Apache-2.0 / MIT | Llama 4 (MAU clause), non-commercial models in production | Sovereign by default |
| Models (Sep 2026) | **gpt-oss, Qwen3.x, Mistral 3, Gemma 4 (Apache-2.0), DeepSeek-V4 (MIT)**; embeddings **Qwen3-Embedding, BGE-M3**; ASR **Omnilingual ASR** (code Apache-2.0; weights licence to confirm at P14); **AfroXLMR** | Apache/MIT | InkubaLM, NLLB and MMS are CC-BY-NC: research only | Commercially clean |
| Document AI | **Docling + PaddleOCR 3**; olmOCR 2 on GPUs | MIT / Apache-2.0 | Marker weights (revenue-capped) | — |
| Entity resolution | **Splink** + Vigil's African-name comparators; Senzing as an optional paid plug-in | MIT | Zingg (AGPL); JedAI (archived May 2026) | No maintained open-source African-name matcher exists: **Vigil IP** |
| Optimisation | **OR-Tools, HiGHS, Timefold, VROOM** | Apache/MIT/BSD | — | Dispatch, scheduling, routing |
| Observability | **OpenTelemetry** everywhere; VictoriaMetrics; ClickHouse for traces; Grafana unmodified (amber) | Apache-2.0 | — | The polyglot equaliser |
| Contracts | **Protobuf + Buf**, Connect, OpenAPI 3.1, CloudEvents, AsyncAPI 3, Arrow Flight/ADBC, **WASM Component Model on Wasmtime LTS** | Apache-2.0 | Conjure/Smithy (small communities) | Contracts are the product |

**Why the licence policy matters now.** In the last 12 months: MinIO archived (Apr 2026), Kùzu archived (Oct 2025), JedAI archived (May 2026), Langfuse acquired by ClickHouse, OpenAI announced it is acquiring Astral (uv/Ruff), and the Bitnami catalog changed. Single-vendor infrastructure changes its licence under monetisation pressure, and foundation-governed projects hold. **Re-review the top 50 dependencies every six months** ([appendix 05 §5](appendix/05-polyglot-architecture.md)).

---

## 9. Constitutional by design: the legal and ethical architecture

The full matrix and citations are in [appendix 04](appendix/04-legal-regulatory-ethics.md). Counsel must confirm everything below.

### 9.1 The laws that shape the product

| Law | What it means for Vigil | Product control |
|---|---|---|
| **POPIA s6(1)(c)** | The security exclusion applies only "to the extent that adequate safeguards have been established in legislation". The Regulator says SAPS is **not** excluded (2023, 2026). | Full POPIA by default. Per-dataset exclusion claims need client counsel sign-off. |
| **POPIA s57(1)(a)–(d), s58** | **Prior authorisation** before linking unique identifiers across responsible parties. Also applies to criminal-conduct information held for third parties, and to cross-border transfers of special PI. The Regulator has 4 + up to 13 weeks. | **The linkage engine stays locked until an authorisation ID is recorded.** Keep a standard application pack, and sponsor a sector code under s57(3). |
| **POPIA s71** | No solely automated decisions with legal effect based on profiles. | A human-review workflow for every adverse outcome, plus an explanation record. |
| **POPIA s72** | Cross-border transfers need adequacy, consent, a contract or a benefit to the data subject. | Geo-fenced storage and a transfer broker. "No prompt leaves SA" (Bedrock in Cape Town routes globally). |
| **POPIA s19–22** | Security safeguards; operator contracts; breach reports through the eServices portal since 1 Apr 2025. | ISO 27001 / SOC 2 path; 24-hour breach SLA; s21 operator DPA. |
| **RICA + *amaBhungane* (2021) + GILAB (2025)** | Bulk interception is unlawful. GILAB's judge-authorised regime waits on regulations. | **No interception features, ever.** The comms-metadata module needs a direction reference ID. |
| **National Data & Cloud Policy (2024) ¶15.4.2** | Sovereign government data must be stored in SA. SITA sources government cloud (¶15.1.3). | SA-hosted or on-prem only; customer-held keys; a challenge-and-disclose policy against foreign compelled access (US CLOUD Act). |
| **CIPA (2019), MISS** | Vetted security managers; security-measures information is protected (s26). | A cleared enclave team; a "security measure" data class. |
| **Cybercrimes Act s54** (not yet in force) | 72-hour reporting for ECSPs and financial institutions. | A pre-built s54 workflow for bank and telco clients. |
| **ECT Act Ch. V** | Cryptography providers must register (enforcement dormant). | Register defensively. |
| **AI policy** | **No SA AI law.** The draft National AI Policy was **withdrawn on 26 Apr 2026** over hallucinated citations. | Benchmark against the EU AI Act (Annex III from 2 Dec 2027) and *hessenDATA*; publish Vigil's own standard. |
| **NCACC / Non-Proliferation Act** | Military C2 software and dual-use surveillance goods need permits, before marketing. | Classification opinion before any export or defence sale. |

### 9.2 The hessenDATA safeguards, built into the product

1. **Case-predicated mode.** Sensitive analyses need a case with an identifiable danger or serious-offence threshold.
2. **A registry of permitted data types and methods per deployment.** Self-learning methods need separate approval.
3. **Intrusive sources are excluded by default.**
4. **Third-party minimisation.** Suspects are kept separate from bystanders.
5. **A written justification on every sensitive query** (Palantir's "Checkpoints").
6. **A publishable configuration.** The rules of each deployment can be disclosed.

### 9.3 Red lines: Vigil will never build or sell

- Bulk or untargeted interception, or ingesting its products.
- Live facial recognition for general surveillance of public spaces, or untargeted face-scraping databases.
- Predictive policing of individuals from profiling alone.
- Social or political scoring, or electoral micro-targeting. Cambridge Analytica's work on Kenya's 2013/2017 elections and Nigeria's 2015 election is the cautionary tale.
- Spyware or intrusion tooling, or RICA "listed equipment".
- Biometric inference of race, religion, politics or sexuality.
- Credit-bureau functions.
- Sales to embargoed buyers, or to buyers credibly linked to recent abuses.

These red lines are enforced **in code** (disabled capabilities, blocked module combinations) and **in contract** (suspension rights).

### 9.4 Warrant- or authorisation-gated modules

Every gated module has the same controls:
- dual control;
- a reference ID for the legal instrument;
- limits on scope and time;
- automatic expiry.

The gated modules are:
- communications metadata;
- retrospective biometric search on named suspects;
- LPR hotlists and movement histories;
- cross-agency identifier linkage (s57(1)(a));
- shared watchlists (s57(1)(b));
- children's data;
- cross-border transfer of special PI;
- any output feeding a decision with legal effect (s71).

### 9.5 Legitimacy infrastructure

- **Hash-chained, externally anchored audit logs.**
- **An independent oversight board** (retired judges, technologists, civil society) with audit rights and the power to halt deployments.
- **Annual transparency reports.**
- **UNGP human-rights due diligence** before every government sale.
- **A protected-disclosure channel.**
- **Early briefings** for Right2Know, Intelwatch, ALT Advisory and Research ICT Africa, before the press arrives.

---

## 10. Sovereign infrastructure and compute

Details are in [appendix 06](appendix/06-infra-data-sources.md).

- **Where to host.**
  - SA holds **more than 60% of Africa's 500+ MW** of data-centre capacity; Teraco alone reports 189 MW.
  - Primary: **Teraco Isando/Bredell** (Johannesburg). DR: **Teraco CT2 or Equinix JN1**. Durban edge (port twin): **OADC DUR1 or Teraco DB1**.
  - The sovereign tier never goes on US hyperscalers. A separate "hyperscaler-connected" commercial tier is allowed.
- **Hyperscaler AI in SA is thin.**
  - AWS af-south-1 offers only T4/Inf1 accelerators. Claude on Bedrock in Cape Town uses *global* cross-region inference.
  - Azure SA North is the only in-country region with 8×H100 VMs (about US$98 an hour). GCP africa-south1 has no GPUs.
  - Microsoft France told the French Senate it "cannot guarantee" data won't be handed to US authorities.
- **First compute.**
  - One **4× RTX PRO 6000 Blackwell (96 GB)** server (about R0.8–1.1m **[UNVERIFIED]**). It fits a standard 4 kW rack and runs quantised 70–120B open-weight models.
  - A portable 1–2 GPU air-gap demo kit.
  - Burst to Azure SA North H100s or Cassava GPU-as-a-Service, under in-country DPAs.
  - **Buy no H100 or B200 until a contract pays for it.** A DGX B200 draws about 14.3 kW.
- **Power.**
  - 476–490 days without load-shedding (Sep 2026).
  - Localised "load reduction" still hits 3.4–6.5% of customers.
  - Edge kits carry 8–12 hours of autonomy.
- **Connectivity.**
  - Fourteen cable systems land or are planned. In 2024 cables on both coasts failed within 60 days of each other.
  - Starlink is still unlicensed in SA. Amazon Leo is due in 2027 via Herotel.
  - Design for disconnection: store-and-forward, SMS/USSD fallbacks, offline tiles, an in-country control plane and model registry.
- **Cassava is both partner and competitor.** Partner on compute; compete on ontology and applications.

---

## 11. Phase Zero: two live twins on public data

**One ontology, two lenses.** Both prototypes share the same core object types (`Organisation`, `Person`, `Place`, `Asset`, `Event`, `Document`, `Case`). The point is to show that Vigil is a *platform*, not a single-purpose app. The catalogue of 45+ sources, with endpoints and terms, is in [appendix 06 §5](appendix/06-infra-data-sources.md).

### PZ-1 — Counterparty & procurement-integrity graph (weeks 1–6 of Phase Zero)

- **Sources.**
  - eTenders OCDS API (PDDL; daily delta).
  - Treasury Restricted Suppliers / Tender Defaulters report (PDF; about 409 entries; contains natural-person IDs, so POPIA controls apply).
  - City of Cape Town tender and RFQ awards (ArcGIS REST).
  - Municipal Money API (commercial reuse allowed with attribution).
  - CIPC director extracts, bought on demand (R10 + R0.04 per record).
  - Court judgments **only under a written agreement** (SAFLII bans automated access; LawLibrary is CC BY-NC).
- **Object types:** `OrganOfState`, `Tender`, `Award`, `Contract`, `Supplier`, `Person`, `Identifier`, `Restriction`, `CourtMatter`, `AuditFinding`, `Place`.
- **Links:** `issues`, `awarded_to`, `director_of`, `shares_identifier_with`, `restricted_by`, `party_to`, `audited_in`, `located_in`.
- **What the demo proves:**
  - ingestion into versioned datasets;
  - OMS types, indexing and object sets;
  - entity resolution as a *view* with provenance;
  - graph pivots;
  - AI drafting a referral pack where **every sentence is footnoted to a source record**;
  - a write-back Action: "Refer", which opens a case, adds the supplier to a watch list and sends a WhatsApp alert.
- **Risk controls.**
  - Outputs are **risk indicators, not findings**, to limit defamation risk.
  - Person-level identifiers are pseudonymised in demos.
  - A prior-authorisation pack must be ready before any commercial linkage (s57).

### PZ-2 — Durban port & corridor twin (weeks 6–12)

- **Sources.**
  - AIS: aisstream.io WebSocket (commercial terms unclear), plus a **Vigil-owned AIS receiver in Durban** feeding AISHub, which is reciprocal.
  - Sentinel-1 SAR via the Copernicus Data Space, for "dark vessel" detection (added in P29, after the v1 demo).
  - Commercial Open-Meteo plan.
  - TNPA monthly statistics, for calibration.
  - EskomSePush load-reduction API.
  - GDELT news events, for corridor incidents.
  - SARB exchange rates.
- **Object types:** `Vessel`, `PortCall`, `Anchorage`, `Berth`, `Terminal`, `SatelliteDetection`, `WeatherCell`, `CorridorSegment`, `BorderPost`, `GridArea`, `Incident`.
- **What the demo proves:**
  - streaming ingestion;
  - geospatial and time-series properties;
  - simulation (anchorage wait-time and ETA forecasts);
  - the edge node;
  - write-back of slot advisories to transporters by WhatsApp or CSV.

### Data-rights actions to start now

- MoU with Treasury OCPO for CSD access (and send them OCDS data-quality feedback).
- Written agreements with SAFLII and LawLibrary.
- Commercial Open-Meteo plan.
- Durban AIS receiver.
- ACLED corporate licence only when a buyer needs it.
- Take OSM and Overture layers under ODbL but keep them in **separate databases**. Use Open Buildings under **CC BY 4.0**.

---

## 12. Go-to-market

**Routes in, in order** ([appendix 04 §6](appendix/04-legal-regulatory-ethics.md), [appendix 07 §5](appendix/07-funding-talent-gtm.md)):
1. **Private buyers first.** Private train operators, mining exporters, ICTSI/terminal operators, forwarders, security contractors, and banks and DFIs (the integrity graph). They move quickly and are solvent.
2. **SOEs and metros:**
   - an open tender;
   - a proof of value funded by business or a DFI (NLCC/BUSA);
   - a non-exclusive written PoC letter kept separate from any future tender;
   - or **subcontracting to an SI that already holds a contract** (BCX, NTT Data, iOCO, Altron), keeping the IP and the customer.
3. **Metros, using TIA's Technology Acquisition and Deployment Fund** (up to R1m over 24 months for SA developers that already have government or municipal customers).
4. **National departments only through SITA** (SITA Act s7(3)). Start SITA certification engagement in Phase Zero.
5. **Defence last**, after clearances, NCACC classification and an accredited air-gapped build.
6. **Never** grow through deviations (TR 16A6.4) or by piggy-backing on another organ's contract (TR 16A6.6). Both attract AGSA irregular-expenditure findings.

**Bootcamps.** Paid, on-site and offline-capable:
- edge kit;
- pre-built SAP, OSIsoft PI and OPC UA connectors;
- two FDEs plus one domain expert.

Each bootcamp ends with a live ontology, one write-back workflow and a **business case in rand**, and its fee is credited against the subscription.

**Pricing, as a working hypothesis to test with the first 5 buyers:**

| Offer | Price (excl. VAT) |
|---|---|
| 3–5 day bootcamp | R150–400k (credited) |
| 8–16 week proof of value | R1.5–4m, or a platform fee plus a share of *verified* avoided losses |
| Platform for one SOE division or metro | R6–20m a year |
| Enterprise agreement | R25–60m a year |
| FDE retainer | R2–3m per FDE-year |
| Compute and LLM usage | Passed through, plus 20–30% |

Palantir's 2019 top-20 average was about R400m a year, which sets the ceiling.

**Partners, not enemies:**
- Vumacam (cameras and LPR);
- telematics firms (Cartrack/Karooooo, MiX/PowerFleet, Netstar, Tracker);
- drone suppliers;
- SAP, Esri, Genetec and Milestone (integrate with them rather than compete).

Treat Databricks and Microsoft Fabric as layers to integrate with.

**B-BBEE (facts to confirm with a verification agency):**
- EMEs (turnover under R10m) are Level 4 by default.
- At least 51% black-owned gives Level 2; 100% gives Level 1.
- The ICT Sector Code's ownership target is 30% **[UNVERIFIED specifics]**.

Design the cap table so strong B-BBEE status survives the fundraising rounds. It is a moat Palantir cannot copy.

---

## 13. Capital plan and unit economics

**The environment** ([appendix 07 §1](appendix/07-funding-talent-gtm.md)):
- SA led Africa with **$643M of equity in 2025** (Partech), then raised **$83M in H1 2026** (Africa: The Big Deal, deals of $100k+). The two trackers count differently, so the figures are indicative rather than directly comparable.
- A B2B seed round of $2–4M would be top-quartile in SA.
- Naspers Foundry closed in 2023.
- Hlayisani Fund II has invested in **Cogitait AI**, an operational-intelligence startup, which means both a conflict to check and a competitor to watch.

| Stage | When | Amount | Sources | Milestones needed to raise the next stage |
|---|---|---|---|---|
| Pre-seed / bootstrap | Months 0–9 | R8–15m | Founders, angels, **s11D (150% deduction, to 31 Dec 2033)**, **TIA**, paid bootcamps | One paid deployment live with write-back; 8–10 staff |
| Seed | Months 9–15 | $2–3.5M | Knife Capital, Hlayisani II, Grindstone II, 4Di, HAVAIC, angels | R10–20m contracted ARR; 2 sectors; ISO 27001 |
| Series A | Months 24–30 | $8–12M | Partech Africa, TLcom, Knife + DFI co-investors (IFC, BII, Proparco) | R60–100m ARR; net revenue retention above 120%; first non-SA customer; SOC 2 Type II |
| Series B | Month 40+ | $25–40M | Gulf, US or DFI growth capital | R200m+ ARR across 3+ countries |

**Unit-economics rules.**
- Services at or below 40% of revenue by month 24.
- Contribution margin at or above 40% (Palantir took until 2020 to reach 48%).
- No customer above 30% of revenue after month 18.
- Stock-based comp stays well below Palantir's 2021 level (50% of revenue).

> **If capital were unlimited, what would change?** You would hire the first 25 people in six months instead of eighteen. You would run Tiers 2 and 3 in parallel, buy sovereign GPU capacity early, and open Nairobi and Lagos pods in year two. **What would not change:**
> - data-access agreements;
> - prior authorisations (4–17 weeks);
> - security clearances (6–18 months, estimated);
> - procurement cycles (3–9 months, estimated);
> - earning trust.
>
> That is why the plan is revenue-first. It doesn't depend on a funding market that South Africa does not currently have.

---

## 14. Team, talent and the FDE model

**Salaries (base, ZAR per year, 2026):**
- **Market.** Software engineer about R410k (PayScale). **Senior about R830k.** SITA is among the top payers at about R818k (Indeed).
- **Competition.** Palantir's New York FDE base is $135–200k plus RSUs, so expect the top decile of SA engineers to be poached.
- **How to compete:** real equity, a USD-indexed bonus component, the mission, and pan-African and Gulf deployments ([appendix 07 §4](appendix/07-funding-talent-gtm.md)).

**First 25 hires, in waves:**

| Wave | Roles |
|---|---|
| **0–6 months** | Staff ontology/platform lead (Java); Rust lead (Warden + agent); Python/ML lead (Mind); TypeScript lead (Cockpit); 2 FDEs; DevSecOps/SRE (air-gap K8s, owns ISO 27001); deployment strategist (business side) |
| **6–18 months** | Go engineer (Relay); data engineer; product manager; designer; GRC lead (ISO 42001, SOC 2); security architect (SA citizen who can be cleared); 2 more FDEs (mining, metros); second ML engineer |
| **12–30 months** | **"Vigil Fellows"** graduate FDE cohort, recruited through Zindi and the Deep Learning Indaba (Jobs Fund co-funding possible); SI alliances manager; East Africa lead; Kotlin and Elixir engineers when Field starts |

**How the FDE model works at Vigil:**
- **Pods.** Each is 1–2 FDEs plus a deployment strategist, paired with a product ("Dev") owner.
- **The rule.** Every engagement files at least one reusable artefact per sprint: a connector, a pack type, a template, a function. Otherwise it is consulting, and it gets flagged.
- **Internal "AI FDE" agents** automate connector scaffolding and ontology mapping. This is Palantir's own 2026 direction.
- **Metrics:**
  - time to first production workflow;
  - gross margin per deployment;
  - reusable artefacts per engagement.

---

## 15. The 36-month roadmap

Phase IDs map to [`docs/PHASES_AND_TASKS.md`](../PHASES_AND_TASKS.md).

| Window | Platform (phases) | Market | Team | Assurance |
|---|---|---|---|---|
| **Q4 2026** (months 0–3) | P0–P5: governance, repo, contracts, **Warden kernel**, Forge datasets, OMS (v1 slices) | Trademark clearance; data-rights MoUs; SITA intro | 6–8 | POPIA programme (Information Officer, PAIA manual) |
| **Q1 2027** (months 3–6) | P6–P13: Indexer, Object Sets, Actions + Temporal, Functions, Cockpit shell, connectors, transforms, object views; **P22 PZ-1 demo** (integrity graph) | First 3 paid bootcamps (private rail operator, exporter, bank) | 8–10 | ISO 27001 gap analysis |
| **Q2 2027** (months 6–9) | P14–P21: Mind gateway, agents/MCP, ER, doc AI, map, graph + cases, Relay v1/v2; **P23 PZ-2 demo** (Durban twin) | First paid proof of value (Corridor Integrity) | 10–14 | Prior-authorisation pack filed with the first client |
| **H2 2027** (months 9–15) | P24–P26: SAP/PI/OPC UA connectors and write-back, edge agent, streaming funnel; packs hardened for production | 2–3 production customers; seed closes | 15–20 | ISO 27001 certified |
| **H1 2028** (months 15–21) | P27–P31: Field app, comms gateway, EO feeds, simulation, app builder + SDKs | Transnet/TRIM tender or SI route; first metro via TIA | 20–25 | SOC 2 Type I |
| **H2 2028** (months 21–27) | P32–P37 (production readiness): load, E2E and disconnection, security audit and AI red team, hardening, compliance evidence, sovereign appliance | R40–60m ARR; Series A | 25–35 | SOC 2 Type II; ISO 42001 readiness |
| **H1 2029** (months 27–33) | P38–P40 (post-launch hardening): SSO federation and peering, multi-country pods, tamper-evident audit + oversight portal | First Botswana, Namibia or Mozambique pod (Maputo corridor) | 35–50 | ISO 42001 |
| **H2 2029** (months 33–36) | P41–P43: classified enclave (gated), HYOK/crypto-shredding, red-line conformance + transparency; Kenya/Lobito pods | Pan-African corridor network; Series B prep | 50–60 | Per-customer accreditation |

---

## 16. Risk register (top 15)

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Palantir enters through a Gulf-style JV or hyperscaler partner | M | H | Move first; sovereignty and B-BBEE moats; SA-owned operator; price in rand |
| R2 | Cassava, Huawei or an SI bundles "sovereign AI" with infrastructure | H | M | Partner on compute; win on ontology, write-back and legitimacy |
| R3 | Prior authorisation (s57) delays go-lives | H | M | Standard pack; file early; sector code; phased linkage |
| R4 | Procurement-integrity optics (any hint of a sweetheart deal) | M | H | Open tenders, DFI-funded PoCs, SI contracts; a conflict barrier with MB's legal practice |
| R5 | A security breach under strict liability (s99) | M | H | Warden-first; ISO 27001; hash-chained audit; insurance; liability caps |
| R6 | FDE and senior talent poached by AI labs or Palantir | H | M | Equity, USD-indexed bonuses, mission, Fellows pipeline |
| R7 | SA funding drought continues | H | M | Revenue-first; non-dilutive funds; DFIs as payers |
| R8 | Trademark conflict ("Vigil") | H | M | Clear before public use; coined company name |
| R9 | OSS licence changes or archival | M | M | Licence policy, abstraction boundaries, 6-monthly review |
| R10 | US export controls or GPU supply | M | M | RTX PRO start; diversified sourcing; no Huawei Ascend for clients with US exposure |
| R11 | SOE finances (Transnet's underlying operations lost money) | M | M | Price against avoided losses; private buyers first |
| R12 | Scope creep and polyglot tax | H | M | Language charter, phase ownership, Karpathy doctrine |
| R13 | Misuse by a client (surveillance creep) | M | H | Red lines in code; gated modules; oversight board; termination rights |
| R14 | Customer concentration | M | M | Second anchor within 12 months; no customer above 30% |
| R15 | Data quality in public sources (OCDS `parties` gaps) | H | L | Data-quality checks; feedback to OCPO; pay for CIPC extracts |

---

## 17. Name and brand

**Facts** ([appendix 07 §6](appendix/07-funding-talent-gtm.md)):
- **vigil.co.za has been registered to ALTRON TMT (Pty) Ltd since 30 June 2005** (whois checked 24–25 Sep 2026).
- **3xLOGIC sells VIGIL**, a video-management system with face and plate recognition. That is the security market Vigil targets.
- The USPTO lists about 500 "VIGIL" marks, 340 of them in Class 9.
- vigil.ai is parked for sale; vigil.africa is registered.

**Decision D12.**
- "Vigil" stays the **internal codename**.
- MB's legal practice runs a CIPC and WIPO Global Brand Database search in Nice classes 9, 38, 42 and 45, and **talks to Altron** (which is also a potential SI partner).
- The company should have a **coined, distinctive name**, ideally from an African language and checked by native speakers, with a Madrid filing.
- "Vigil" could survive as a product-line or composite mark if cleared.
- Register .africa, .co.za, .ai and .com together.
- Don't spend on branding before clearance.

---

## 18. Decisions and open questions for the founders

1. Sign off D1–D12 (§0), or amend them.
2. **Company name**, after the trademark search.
3. **Conflict barrier** between MB's legal practice and Vigil, if MB acts for any prospective Vigil customer (for example Transnet).
4. **Cap-table and B-BBEE design** before the first external money.
5. **Who is the Information Officer** (POPIA), and who is counsel of record for the s57 pack?
6. **Oversight board:** the first three names to approach.
7. Remote GitHub repository: the `Motsoeneng-Bill-Tech` org, or a new Vigil org (relevant to the IP separation that greenfield implies)?
8. Phase Zero budget: the first server (about R1m), the AIS receiver, the CIPC extracts (about R80k), the Open-Meteo plan.

---

## 19. Key sources

All ~1,500 sources are in the appendices. These are the ones that carry the most weight:

- Palantir SEC filings: [S-1 (2020)](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm), [10-K FY2025](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm), [Q2-2026 release](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000039/a2026q2ex991pressrelease.htm)
- Palantir docs: [Ontology overview](https://www.palantir.com/docs/foundry/ontology/overview/), [object backend](https://www.palantir.com/docs/foundry/object-backend/overview/), [action webhooks](https://www.palantir.com/docs/foundry/action-types/webhooks/), [markings](https://www.palantir.com/docs/foundry/security/markings/), [Apollo](https://www.palantir.com/docs/apollo/core/how-apollo-works/), [Ontology MCP](https://www.palantir.com/docs/foundry/ontology-mcp/overview/)
- [Nabeel Qureshi, "Reflections on Palantir" (2024)](https://nabeelqu.co/reflections-on-palantir); [Pragmatic Engineer on FDEs (2025)](https://newsletter.pragmaticengineer.com/p/forward-deployed-engineers)
- [BVerfG press release on hessenDATA, 16 Feb 2023](https://www.bundesverfassungsgericht.de/SharedDocs/Pressemitteilungen/EN/2023/bvg23-018.html); [Sifted on DGSI → ChapsVision](https://sifted.eu/articles/chapsvision-to-replace-palantir-in-major-contract-with-french-intelligence-agency); [Republik on Switzerland](https://www.republik.ch/2026/02/18/how-tenaciously-palantir-courted-switzerland)
- [POPIA s57](https://popia.co.za/section-57-processing-subject-to-prior-authorisation/), [s71](https://popia.co.za/section-71-automated-decision-making/), [s72](https://popia.co.za/section-72-transfers-of-personal-information-outside-republic/); [amaBhungane [2021] ZACC 3](https://www.saflii.org/za/cases/ZACC/2021/3.html); [National Data and Cloud Policy (GG 50741)](https://www.gov.za/sites/default/files/gcis_document/202406/50741gen2533.pdf); [SITA Act](https://www.saflii.org/za/legis/consol_act/sitaa88o1998386/); [Draft AI Policy withdrawal](https://www.sanews.gov.za/south-africa/minister-announces-withdrawal-draft-ai-policy)
- [Transnet turnaround and security spend (Daily Maverick, Sep 2025)](https://www.dailymaverick.co.za/article/2025-09-07-transnet-turnaround-losses-narrow-but-we-have-a-long-way-to-go-says-ceo/); [logistics cost R353bn (Citizen)](https://www.citizen.co.za/business/transnet-costing-south-africa-r1-billion-day/); [border delays up to R16bn (Business Report, Jul 2026)](https://businessreport.co.za/economy/2026-07-28-border-delays-threaten-sas-trade-ambitions-as-congestion-costs-economy-up-to-r16-billion-a-year/); [rail network statement (Mining Weekly, Sep 2026)](https://www.miningweekly.com/article/new-version-of-rail-network-statement-officially-gazetted-2026-09-21); [AGSA local government 2024-25](https://www.sanews.gov.za/south-africa/ag-tables-local-government-outcome-reports-2024-2025); [municipal Eskom debt R120.26bn](https://www.timeslive.co.za/news/south-africa/2026-09-23-municipalities-owe-eskom-r120bn-as-debt-threatens-viability/)
- [CSIS on Huawei Safe Cities](https://www.csis.org/analysis/watching-huaweis-safe-cities); [Carnegie AI surveillance index](https://carnegieendowment.org/research/2019/09/the-global-expansion-of-ai-surveillance)
- [Google on Rust in Android (Nov 2025)](https://blog.google/security/rust-in-android-move-fast-fix-things/); [PostgreSQL 19 release notes](https://www.postgresql.org/docs/19/release-19.html); [MinIO repo (archived)](https://github.com/minio/minio); [Kùzu repo (archived)](https://github.com/kuzudb/kuzu); [StatCounter Africa mobile OS](https://gs.statcounter.com/os-market-share/mobile/africa)
- [Teraco CT2](https://www.teraco.co.za/news/teraco-completes-ct2-data-centre-expansion/); [AWS Bedrock global cross-region inference in SA](https://aws.amazon.com/blogs/machine-learning/scale-ai-in-south-africa-using-amazon-bedrock-global-cross-region-inference-with-anthropic-claude-4-5-models/); [Eskom 4 Sep 2026](https://www.eskom.co.za/eskom-fully-meets-2026-winter-demand-eaf-remains-at-its-highest-level-since-2020-67-79-and-diesel-expenditure-declines-by-r4-84-billion-or-81-64-to-deliver-energy-security/); [eTenders OCDS API](https://ocds-api.etenders.gov.za/swagger/index.html); [SAFLII terms](https://www.saflii.org/content/terms.html)
- [Partech 2025 Africa report](https://partechpartners.com/africa-reports/2025-africa-tech-venture-capital-report); [H1 2026 funding (Tech In Africa)](https://www.techinafrica.com/egypt-tops-africas-h1-2026-startup-funding-but-nigeria-reclaims-the-equity-crown/); [SARS s11D](https://www.sars.gov.za/businesses-and-employers/my-business-and-tax/research-and-development-incentive/); [TIA funding instruments](https://www.tia.org.za/funding-instruments/); [whois vigil.co.za](https://www.whois.com/whois/vigil.co.za); [3xLOGIC](https://www.3xlogic.com/)

### Appendices (raw research reports, fully sourced)

| # | Report | Topic |
|---|---|---|
| 01 | [Palantir business](appendix/01-palantir-business.md) | Financials, products, contracts, operating model, controversies, competitors |
| 02 | [Palantir technical](appendix/02-palantir-technical.md) | Ontology internals, data layer, AIP, security, Apollo, languages, comparables |
| 03 | [Africa market](appendix/03-africa-market.md) | Sector pain, buyers, players, wedge scoring |
| 04 | [Legal, regulatory, ethics](appendix/04-legal-regulatory-ethics.md) | POPIA, cyber, surveillance, sovereignty, AI, procurement, pan-African, export, compliance matrix |
| 05 | [Polyglot architecture](appendix/05-polyglot-architecture.md) | Language charter, interop, build, building blocks, licences, reference architecture |
| 06 | [Infrastructure & data](appendix/06-infra-data-sources.md) | Data centres, cloud, GPUs, connectivity, power, public-data catalogue, prototypes |
| 07 | [Funding, talent, GTM](appendix/07-funding-talent-gtm.md) | Capital, precedents, talent, GTM, naming, 36-month plan |

> Research limits: the shared web-search quota ran out partway through every stream, and some sites rate-limited or blocked fetches (WSJ, Bloomberg, Foreign Policy, Citizen Lab, parts of Wikipedia). Each appendix lists its gaps. Verify **[UNVERIFIED]** items before external use.
