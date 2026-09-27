<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Raw research appendix (P0). Sourced analyst report; verify [UNVERIFIED] items before external use. -->

> **Appendix to** [`VIGIL_RESEARCH_DOSSIER.md`](../VIGIL_RESEARCH_DOSSIER.md). A raw research report produced on 24–25 Sep 2026 by a parallel research agent. Kept verbatim for traceability. Where it conflicts with the dossier, the dossier wins (the dossier applies the founders' greenfield decision). **Superseded where it assumes an MB–Transnet relationship or the reuse of MB assets:** the founders chose a greenfield start (dossier §5), and Vigil wins SOE work only through open tenders, DFI-funded proofs of value or SI contracts.
>
> **Internal working paper; not for external distribution.**

# Palantir Technologies: business, products, operating model, controversies, competitors and lessons for Vigil

*Prepared for the Vigil founding team (MB Tech, Johannesburg). Research cut-off: 24–25 Sep 2026. Every fact carries an inline source. Paragraphs marked **Analysis** are my own judgement. [UNVERIFIED] means I could not confirm the claim from a page fetched in this session.*

## Key takeaways
1. **Palantir is now a hyper-growth, very high-margin business.**
   - Q2 2026 revenue was $1.935B (+93% y/y), with a 62% adjusted operating margin and a "Rule of 40" score of 155%.
   - FY2026 guidance is $8.15B (+82%) ([Q2-26 PR](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000039/a2026q2ex991pressrelease.htm)).
   - Market capitalisation was $458B on 23 Sep 2026 ([StockAnalysis](https://stockanalysis.com/stocks/pltr/market-cap/)).
2. **The growth is American.** Vigil would sit in Palantir's "international commercial" bucket, which grew only +2% in FY2025 and +26% in H1 2026. International's share of revenue fell from 26% to about 19% ([10-K](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm), [TIKR](https://www.tikr.com/blog/wall-street-just-gave-palantir-its-highest-ever-price-target-right-when-europe-started-saying-no)).
3. **Europe is choosing "sovereign" alternatives.**
   - France's DGSI and Germany's BfV both picked the French vendor ChapsVision in 2026.
   - The German armed forces (Bundeswehr) ruled out Maven "for now".
   - The Swiss Army advised against Palantir.
   - London's mayor blocked a £50M Metropolitan Police deal (§6).
4. **Palantir has no verified African customer or office.** The continent's incumbent vendor of government data and surveillance platforms is Huawei.
5. **What a challenger can and can't copy.**
   - Copyable: the loop that turns field engineering into product, the Ontology (Palantir's operational data model) with write-back, the security model, and bootcamps.
   - Not copyable: US accreditations, $9.2B of cash, and 20 years of relationships.

---

## 1. Timeline & financial trajectory

### 1.1 Timeline
| Date | Event |
|---|---|
| 2003 | Founded by Thiel, Cohen, Lonsdale, Karp and Gettings ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)) |
| 2004–08 | Venture capital firms passed. In-Q-Tel (the CIA's venture arm) put in "over $2 million" across two rounds; Thiel put in about $40M. **The CIA was the sole customer from 2005 to 2008**, and FBI, NSA and Marines in Afghanistan followed ([Forbes 2013](https://www.forbes.com/sites/andygreenberg/2013/08/14/agent-of-intelligence-how-a-deviant-philosopher-built-palantir-a-cia-funded-data-mining-juggernaut/)) |
| 2008 / 2013 | Gotham released ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)). By 2013: about $450M revenue, roughly 60% commercial ([Forbes](https://www.forbes.com/sites/andygreenberg/2013/08/14/agent-of-intelligence-how-a-deviant-philosopher-built-palantir-a-cia-funded-data-mining-juggernaut/)) |
| 2016 | Sued the US Army over the DCGS-A intelligence-system procurement and won [UNVERIFIED] |
| 2019 | Took over Project Maven ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)). Revenue $742.6M, net loss $579.6M, top-3 customers 28% of revenue ([S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm)) |
| 30 Sep 2020 | NYSE direct listing, after the HQ moved to Denver in Aug 2020 ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)). Reference price of $7.25 [UNVERIFIED] |
| Q4 2022 | First GAAP-profitable quarter: Q1 2024 was the "sixth consecutive" ([Q1-24 PR](https://www.nasdaq.com/press-release/palantir-reports-revenue-growth-of-21-year-over-year-and-sixth-consecutive-quarter-of)) |
| Apr 2023 | AIP launched ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)) |
| FY2023 | **First GAAP-profitable year**: net income $209.8M, about 20 years after founding ([StockAnalysis](https://stockanalysis.com/stocks/pltr/financials/)) |
| Sep 2024 | S&P 500 inclusion (announced 6 Sep) ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)) |
| 26 Nov 2024 | Listing moved to Nasdaq ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies), [CNBC](https://www.cnbc.com/2024/11/15/palantir-jumps-9percent-to-a-record-after-announcing-move-to-nasdaq.html)). Joined the Nasdaq-100 in Dec 2024 ([Nasdaq](https://www.nasdaq.com/articles/palantir-stock-joining-nasdaq-100-index-and-super-micro-computer-being-booted-what)) |
| Feb 2026 | Announced HQ move to Miami ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)) |

### 1.2 Annual financials and stock-based compensation (SBC)
| FY | Revenue | y/y | GAAP net income | SBC (% of revenue) |
|---|---|---|---|---|
| 2019 | $742.6M | +25% | –$579.6M | $242M (33%) |
| 2020 | ≈$1,093M (derived) | ≈+47% | [UNVERIFIED ≈–$1.17B] | [UNVERIFIED ≈$1.27B] |
| 2021 | $1,542M | +41% | –$520M | $778M (50%) |
| 2022 | $1,906M | +24% | –$374M | $565M (30%) |
| 2023 | $2,225M | +17% | +$210M | $476M (21%) |
| 2024 | $2,866M | +29% | +$462M | $692M (24%) |
| 2025 | $4,475M | +56% | +$1,625M | $684M (15%) |
| TTM to Jun-26 | $6,156M | +79% | +$3,017M | $836M (14%) |

Sources: [S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm), [income statement](https://stockanalysis.com/stocks/pltr/financials/), [cash flow](https://stockanalysis.com/stocks/pltr/financials/cash-flow-statement/).

**FY2025** (reported 2 Feb 2026) ([Q4-25 PR](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000004/a2025q4ex991earningsrelease.htm), [Q4-25 call](https://www.investing.com/news/transcripts/earnings-call-transcript-palantirs-q4-2025-revenue-surges-stock-rises-93CH-4480410)):
- US commercial $1.465B (+109%); US government $1.855B (+55%).
- International commercial $608M (+2%); international government $547M (+47%).
- GAAP net income $1.625B (36% margin). Adjusted operating margin 50%. Adjusted free cash flow $2.27B.
- Q4 2025 Rule of 40 was 127%.

The **10-K** adds ([10-K](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm)):
- Revenue split 54% government / 46% commercial.
- Top-3 customers = 16% of revenue; top-20 customers averaged $93.9M each.
- Total remaining deal value $11.2B.
- Sales cycles "often… six to nine months"; contract terms of 1–5 years.
- 4,429 staff, 28% of them outside the US.

### 1.3 2026 quarters
| Metric | Q1 2026 (4 May) | Q2 2026 (3 Aug) |
|---|---|---|
| Revenue | $1.633B (+85%) | $1.935B (+93%) |
| US commercial / US government | $595M (+133%) / $687M (+84%) | $764M (+149%) / $809M (+90%) |
| International commercial / government | $179M (+26%) / — | $182M (+26%) / $181M (+42%) |
| Total contract value (TCV) booked | $2.41B | $3.373B (+49%) |
| Remaining deal value | US commercial $4.92B | US commercial $6.24B (+124%); total $13.1B (+83%) |
| GAAP net income | $871M (53%) | $1.062B (55%) |
| Adjusted operating margin / Rule of 40 | 60% / 145% | 62% / 155%; adjusted gross margin 86% |
| Customers | 1,007 (+31%); 615 US commercial | 653 US commercial (+35%) |
| Top-20 average revenue per customer (TTM) | $108M (+55%) | $124M (+67%) |
| Net dollar retention / SBC / cash | 150% / $202M / $8.0B | 157% / $265M / $9.2B |

Sources: [Q1-26 PR](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000026/a2026q1ex991pressrelease.htm), [Q1-26 business update](https://investors.palantir.com/files/Palantir%20-%20Q1%202026%20Business%20Update.pdf), [Q1-26 call](https://www.fool.com/earnings/call-transcripts/2026/05/04/palantir-pltr-q1-2026-earnings-transcript/), [Q2-26 PR](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000039/a2026q2ex991pressrelease.htm), [Q2-26 business update](https://investors.palantir.com/files/Palantir%20-%20Q2%202026%20Business%20Update.pdf), [Q2-26 call](https://www.fool.com/earnings/call-transcripts/2026/08/10/palantir-pltr-q2-2026-earnings-call-transcript/), [Futurum](https://futurumgroup.com/insights/palantir-q2-fy-2026-earnings-surge-on-us-commercial-ai-demand/) (total remaining deal value).

**Guidance.** FY2026 revenue guidance rose from $7.18B (February) to $7.65B (May) and then to $8.150–8.158B (August). The August guidance also calls for US commercial revenue above $3.424B and adjusted free cash flow of $4.5–4.7B. ([Q4-25 PR](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000004/a2025q4ex991earningsrelease.htm), [Q1-26 PR](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000026/a2026q1ex991pressrelease.htm), [Q2-26 PR](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000039/a2026q2ex991pressrelease.htm))

**Concentration (derived).** The top-20 customers produced about 67% of revenue in 2019 (20 × $24.8M) and about 40% by TTM Q2 2026 (20 × $124M) ([S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm)).

**Market value** ([StockAnalysis](https://stockanalysis.com/stocks/pltr/market-cap/), [ad-hoc-news](https://www.ad-hoc-news.de/boerse/news/corporate-news/palantir-stock-weighs-93-percent-growth-against-valuation/70166350), [Nasdaq/Motley Fool](https://www.nasdaq.com/articles/down-27-2026-palantir-stock-buy)):
- 23 Sep 2026: $458.3B at $190.72 a share. Trailing P/E about 158; 52-week range $106–$208.
- End-2024: $172.3B. End-2025: $423.7B.
- About $306B on 12 Feb 2026, after a 27% fall.

**International: Vigil's bucket.**
- International commercial was flat in FY2025. Management said its Q4 2025 bookings of about $1.3B were driven by long-term renewals ([Q4-25 call](https://www.investing.com/news/transcripts/earnings-call-transcript-palantirs-q4-2025-revenue-surges-stock-rises-93CH-4480410)).
- It re-accelerated to +26% in H1 2026. Even so, international fell to 18.8% of revenue in Q2 2026, from about 27% a year earlier ([TIKR](https://www.tikr.com/blog/wall-street-just-gave-palantir-its-highest-ever-price-target-right-when-europe-started-saying-no)).
- Management cites weakness in "Canada, Northern Europe, and in Europe in general" ([Q4-25 call](https://www.investing.com/news/transcripts/earnings-call-transcript-palantirs-q4-2025-revenue-surges-stock-rises-93CH-4480410)). Karp dismissed the "witchcraft dance you have in some parts of continental Europe" ([Q1-26 call](https://www.fool.com/earnings/call-transcripts/2026/05/04/palantir-pltr-q1-2026-earnings-transcript/)).
- The UK is the largest non-US market: about $427M, or 9.5% of FY2025 revenue ([Wikipedia country table](https://en.wikipedia.org/wiki/Palantir_Technologies)).

**Analysis.**
- Outside the US, Palantir's growth rests on government work (the UK and NATO) and on commercial renewals.
- It is distrusted in continental Europe, which gives regional challengers an opening.
- The risk: with 62% US margins, Palantir can afford to subsidise market entry abroad through joint ventures (Aither in Dubai) and headline investment pledges (£1.5bn in the UK).

---

## 2. Product suite

| Product | Purpose and users | Core capabilities | Deployment |
|---|---|---|---|
| **Gotham** (2008) | Defence, intelligence and police, including three German state police forces ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies), [The Local](https://www.thelocal.de/20250805/what-is-palantirs-gotham-software-and-why-do-german-police-want-it)) | Link and graph analysis, geospatial views, alerts and targeting; fuses police databases. Mandatory encryption, MFA and audit logging ([Gotham docs](https://www.palantir.com/docs/gotham/)) | On-prem or classified. In Germany it runs on police servers ([Tagesspiegel](https://www.tagesspiegel.de/dobrindt-will-palantir-fur-bka-und-bundespolizei-das-darf-niemals-zugelassen-werden-sagt-der-koalitionspartner-14539292.html)) |
| **Foundry** | Commercial and civil-government organisations | "Data, logic, actions": multimodal connectors, virtual tables, batch and streaming pipelines on Kubernetes, low-code apps (Workshop, Quiver, Contour), model integration, and role-, marking- and purpose-based authorisation ([docs](https://www.palantir.com/docs/foundry/platform-overview/overview/)) | SaaS or customer-hosted |
| **Ontology** | The core layer of Foundry and AIP | "A digital twin of the organization": objects, properties and links, plus actions, functions and dynamic security. Writes back to external systems through webhooks and the Ontology SDK (OSDK) ([docs](https://www.palantir.com/docs/foundry/ontology/overview/)) | — |
| **Apollo** | Continuous delivery, for Palantir and third parties | Write once, deploy everywhere. Pull-based release channels, dependency constraints, and compliance-aware change management (FedRAMP/IL5/IL6). Manages "every instance of Palantir's product" ([docs](https://www.palantir.com/docs/apollo/core/introduction/)) | Cloud, on-prem, air-gapped, classified |
| **AIP** (Apr 2023) | Putting LLMs to work on an organisation's data and operations | **Logic**: no-code LLM functions that read and edit the Ontology. **Automate**: triggers them on schedules or data conditions, applying changes directly or staging them for human review. **Chatbot Studio** (formerly Agent Studio): agents with retrieval context and tools. **Evals**: test suites and model-vs-model comparisons. Also Assist, Document Intelligence, Model Catalog ([AIP](https://www.palantir.com/docs/foundry/aip/overview/), [Logic](https://www.palantir.com/docs/foundry/logic/overview/), [Automate](https://www.palantir.com/docs/foundry/automate/overview/), [Chatbot Studio](https://www.palantir.com/docs/foundry/agent-studio/overview/), [Evals](https://www.palantir.com/docs/foundry/aip-evals/overview/)) | Inside Foundry; works with multiple models |
| **2026 AIP launches** | Builders and forward-deployed engineers (FDEs) | **AI FDE**: an agent that builds pipelines, Ontology objects, functions, apps and evals under the user's own permissions ([docs](https://www.palantir.com/docs/foundry/ai-fde/overview/)). **MindKit** and **Ontology Foundations** ([Q1-26 BU](https://investors.palantir.com/files/Palantir%20-%20Q1%202026%20Business%20Update.pdf)). **Sovereign AI OS** with Nvidia: Nemotron models on Blackwell GPUs, air-gapped. It appears in the Q1-26 business update; TIKR dates the announcement to 29 Jun 2026 ([TIKR](https://www.tikr.com/blog/palantir-stock-what-karps-ai-sovereignty-bet-means-for-investors-in-2026)). **Nebius** named preferred sovereign compute provider (Sep 2026) ([TheStreet](https://www.thestreet.com/investing/palantir-nebius-sovereign-ai-partnership)). Also a self-serve **Developer Tier** ([10-K](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm)) | — |
| **AIP bootcamps** | Sales motion | 1–5 day builds on the customer's real data; 560+ bootcamps with 465 organisations by Feb 2024 ([Q4-23 call](https://www.fool.com/earnings/call-transcripts/2024/02/05/palantir-technologies-pltr-q4-2023-earnings-call-t/)) | — |
| **Maven Smart System** | US combatant commands and NATO | Fuses intelligence and operational data for target identification and strike workflows. >100k users ([DefenseScoop](https://defensescoop.com/2026/04/15/palantir-maven-smart-system-pentagon-program-transition-feinberg/)); >25,000 "builders" ([Q2-26 call](https://www.fool.com/earnings/call-transcripts/2026/08/10/palantir-pltr-q2-2026-earnings-call-transcript/)) | Becomes a program of record under the Pentagon's Chief Digital and AI Office (CDAO) by 30 Sep 2026 |
| **TITAN** | US Army AI ground station | A vehicle linking sensors to weapons; Palantir leads, with Anduril and Northrop; 10 units ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)). Value of about $178M (Mar 2024) [UNVERIFIED] | Vehicle / edge |
| **ShipOS** | US Navy shipbuilding | $448M contract ([Q4-25 call](https://www.investing.com/news/transcripts/earnings-call-transcript-palantirs-q4-2025-revenue-surges-stock-rises-93CH-4480410)). Approvals cut from "200 hours to 15 seconds" ([Q1-26 call](https://www.fool.com/earnings/call-transcripts/2026/05/04/palantir-pltr-q1-2026-earnings-transcript/)) | — |
| **Warp Speed** | Manufacturing operating system for defence and industrial producers [UNVERIFIED: page was rate-limited] | Fuses ERP and manufacturing-execution data for production planning [UNVERIFIED] | — |
| **FedStart** | Software vendors selling to the US government | "SaaS offering for eligible companies and startups looking to deploy software to the federal government – fast" ([FedStart](https://www.palantir.com/fedstart/)). Accreditation levels covered are [UNVERIFIED] | Hosted by Palantir |
| **MetaConstellation / Skykit** | Satellite tasking and edge AI (used by US Northern Command) / portable backpack and maritime kits (sent to Ukraine from 2023) ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)) | — | Space / edge |

**Analysis.**
- The moat is not any one app. It is three things together: the Ontology with actions and write-back, the fine-grained security model, and Apollo's ability to ship the same code into air-gapped estates.
- AIP monetises that base. "AI FDE" shows Palantir automating its own services layer.

---

## 3. Signature contracts & deployments, 2023–2026

| Date | Customer and value | Notes |
|---|---|---|
| Nov 2023 | **NHS England Federated Data Platform (FDP)**: £330M over 7 years ([resultsense](https://www.resultsense.com/news/2026-09-08-palantir-nhs-data-optouts/)) | By June 2026, 168 of 214 trusts had signed and 123 were live; Greater Manchester refused. A decision on extending beyond Feb 2027 is due ([Register](https://www.theregister.com/databases/2026/06/13/nhs-patients-cant-opt-out-of-palantirs-data-platform-but-their-hospital-can/5254766)) |
| Jan 2024 | **Israel Defense Forces**: strategic partnership ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)) | — |
| May 2024 → May 2025 | **Maven**: a $480M 5-year contract raised to about $1.3B through 2029 ([DefenseScoop](https://defensescoop.com/2026/04/15/palantir-maven-smart-system-pentagon-program-transition-feinberg/)) | A memo from Deputy Defense Secretary Feinberg (9 Mar 2026) makes it a program of record. FY2027 request: $2.3B over 5 years ([Motley Fool](https://www.fool.com/investing/2026/08/25/palantirs-maven-is-now-an-official-pentagon-progra/)) |
| 25 Mar 2025 | **NATO** Maven Smart System: value undisclosed | Six-month procurement, "one of the most expeditious in NATO's history" ([NCIA](https://www.ncia.nato.int/newsroom/news/nato-acquires-aienabled-warfighting-system)) |
| Apr 2025 | **ICE ImmigrationOS**: $30M, runs to Sep 2027 | Part of a sole-source case-management deal worth more than $145M; that system has been in use since 2014 ([ACLU](https://www.aclu.org/news/privacy-technology/palantir-deportation-roundup)) |
| 31 Jul 2025 | **US Army Enterprise Agreement**: up to $10B over 10 years | Consolidates 75 contracts. Volume discounts, no reseller "pass-through fees", open to the whole Department of Defense ([Army.mil](https://www.army.mil/article/287506/u_s_army_awards_enterprise_service_agreement_to_enhance_military_readiness_and_drive_operational_efficiency)) |
| 18 Sep 2025 | **UK MoD partnership**: Palantir to invest up to £1.5bn; up to £750M of opportunities over 5 years | 350 jobs; London becomes Palantir's European defence HQ ([MoD](https://www.wired-gov.net/wg/news.nsf/articles/New+strategic+partnership+to+unlock+billions+and+boost+military+AI+and+innovation+18092025161000?open=)) |
| 4 Nov 2025 | **Dubai Holding "Aither" joint venture** | Real estate, hospitality, finance, infrastructure ([Dubai Media Office](https://www.mediaoffice.ae/en/news/2025/november/04-11/dubai-holding-and-palantir-launch-aither)) |
| 15 Dec 2025 | **France's DGSI** (domestic intelligence): 3-year renewal ([Business Wire](https://www.businesswire.com/news/home/20251215083215/en/Palantir-Announces-Renewal-of-Multi-Year-Contract-with-the-DGSI)) | On 16 Jun 2026 the DGSI said it would switch to ChapsVision ([Sifted](https://sifted.eu/articles/chapsvision-to-replace-palantir-in-major-contract-with-french-intelligence-agency)) |
| 30 Dec 2025 | **UK MoD**: £240.6M direct award, Apr 2026 to Mar 2029 ([UKAuthority](https://www.ukauthority.com/articles/mod-continues-data-analytics-contract-with-palantir)) | Palantir hired four former MoD officials in 2025 ([openDemocracy](https://www.opendemocracy.net/en/palantir-ministry-defence-hire-four-officials-2025-record-defence-contract-240-million/)) |
| Q4 2025 / Q1 2026 | **US Navy ShipOS** $448M; **USDA** $300M ([Q4-25 call](https://www.investing.com/news/transcripts/earnings-call-transcript-palantirs-q4-2025-revenue-surges-stock-rises-93CH-4480410), [Q1-26 call](https://www.fool.com/earnings/call-transcripts/2026/05/04/palantir-pltr-q1-2026-earnings-transcript/)) | — |
| 20 Jan 2026 | **Ukraine MoD**: Brave1 "Dataroom" for training AI on battlefield data ([Ukraine gov](https://digitalstate.gov.ua/news/tech/ukraine-launches-brave1-dataroom-with-palantir-to-train-ai-models-using-battlefield-data)) | Palantir is used in more than six Ukrainian agencies ([Defense News](https://www.defensenews.com/global/europe/2026/09/01/ousted-ukrainian-defense-chief-lands-palantir-ceo-as-first-investor-in-new-venture/)) |
| Mar 2026 | **UK Financial Conduct Authority** ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)) | — |
| Q2 2026 | **Multinational tech company**: about $370M over 3 years ([Q2-26 call](https://www.fool.com/earnings/call-transcripts/2026/08/10/palantir-pltr-q2-2026-earnings-call-transcript/)) | — |
| 22 May 2026 | **Metropolitan Police**: £50M deal **blocked** ([Computing](https://www.computing.co.uk/news/2026/government/sadiq-khan-blocks-met-s-50m-ai-deal-with-palantir)) | See §6 |

---

## 4. Operating model

- **Roles.**
  - **Forward-deployed engineers (FDEs, "Deltas")** worked on-site 3–4 days a week.
  - **Product Development ("Dev") engineers** turned the repetitive field work into platform tools. That is how Foundry's ingestion (Magritte), analysis (Contour) and app-building (Workshop) tools were created ([Nabeel Qureshi, "Reflections on Palantir", 15 Oct 2024](https://nabeelqu.co/reflections-on-palantir)).
  - Palantir once had more Deltas than conventional software engineers ([Paraform, Apr 2026](https://www.paraform.com/insights/forward-deployed-engineer-palantir-runway-greptile)).
  - The business-facing role is advertised as "Deployment Strategist" ([Palantir jobs](https://jobs.lever.co/palantir)). That this is the internal "Echo" role is [UNVERIFIED].
- **How deployments start.**
  - **Pilots (the old way).** Customers bought "8–12 week pilot[s]" that Palantir spent "just getting data access" ([Qureshi](https://nabeelqu.co/reflections-on-palantir)). Pilots were offered "at no or low cost". Installation time fell from more than 5 months (Q2 2019) to 14 days (Q2 2020) ([S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm)).
  - **Bootcamps replaced pilots.** Karp: "Two years ago, we did 92 pilots… last year… over 500 bootcamps"; "your 10 hours, your 10 months." In one case, cold outreach led to a five-day bootcamp and then a roughly $3M enterprise agreement, all within a quarter ([Q4-23 call](https://www.fool.com/earnings/call-transcripts/2024/02/05/palantir-technologies-pltr-q4-2023-earnings-call-t/)).
  - **"Sovereign" bootcamps (2026).** CEOs attend to ask "how do you work with open weight models?" ([Q2-26 call](https://www.fool.com/earnings/call-transcripts/2026/08/10/palantir-pltr-q2-2026-earnings-call-transcript/)).
- **Pricing.**
  - Terms of 1–5 years, cloud or on-prem, plus professional services ([10-K](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm)).
  - Consumption is metered in compute-seconds, GB-months and "Ontology volume"; rates are not published ([docs](https://www.palantir.com/docs/foundry/resource-management/usage-types/)).
  - Large buyers move to enterprise agreements.
  - Karp attacks per-token pricing as paying "to migrate your IP" to a model vendor ([Q2-26 call](https://www.fool.com/earnings/call-transcripts/2026/08/10/palantir-pltr-q2-2026-earnings-call-transcript/)).
- **Land-and-expand.**
  - Sales cycles run 6–9 months, sometimes more than a year ([10-K](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm)).
  - Average top-20 customer revenue went from $24.8M (2019) to $124M (Q2 2026), and net dollar retention is 157%.
- **Services turned into software.**
  - Gross margin was 80% in 2023, against 32% at Accenture ([Qureshi](https://nabeelqu.co/reflections-on-palantir)), and 86% (adjusted) in Q2 2026 ([Q2-26 call](https://www.fool.com/earnings/call-transcripts/2026/08/10/palantir-pltr-q2-2026-earnings-call-transcript/)).
- **Partners.**
  - Oracle cloud ([Oracle](https://www.oracle.com/africa/partner/palantir/)).
  - Nvidia and Nebius.
  - SAP, which is embedding AIP in its migration tools ([Q2-26 BU](https://investors.palantir.com/files/Palantir%20-%20Q2%202026%20Business%20Update.pdf)).
- **The FDE hiring boom.**
  - FDE job postings rose more than 800% between January and September 2025 ([Fast Company, citing the FT](https://www.fastcompany.com/91435680/postings-for-this-ai-job-are-up-800)).
  - In January–August 2026 they were up more than 1,000% y/y. The median advertised salary is above $188k, and OpenAI, Anthropic, Microsoft and Nvidia all hire FDEs ([Fortune, 3 Sep 2026](https://fortune.com/2026/09/03/forward-deployed-engineers-fast-growing-six-figure-silicon-valley-job-integrate-ai-with-customers-tech-careers-palantir/)).
  - a16z's "Trading margin for moat" argued that AI startups should copy this implementation-heavy model ([a16z, 4 Jun 2025](https://a16z.com/services-led-growth/)).
  - Palantir CTO Shyam Sankar: "Only Palantir has FDEs. Everyone else has sparkling sales engineers" ([Q2-26 call](https://www.fool.com/earnings/call-transcripts/2026/08/10/palantir-pltr-q2-2026-earnings-call-transcript/)).

**Analysis.**
- The model puts expensive people in front of cheap, reusable software. It only works if every engagement leaves reusable product behind.
- The metric that matters is time to the first workflow running in production.

---

## 5. Culture, hiring and talent
- **Culture.** Flat and without titles; "intellectual grandiosity" plus intense competitiveness. Recruiting leans on mission rather than salary (a "bat-signal"). Qureshi argues the unpopular 2016–2020 years filtered for independent thinkers ([Qureshi](https://nabeelqu.co/reflections-on-palantir)).
- **Headcount.** 4,100 in August 2025, when Karp reportedly wanted 3,600 ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)). By December 2025 it was 4,429 ([10-K](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm)). FDEs make up about half of staff ([Fast Company](https://www.fastcompany.com/91435680/postings-for-this-ai-job-are-up-800)).
- **Fellowships.**
  - The **Meritocracy Fellowship** (2025) hires high-school graduates who skip college and includes a "four-week seminar on Western civilization" ([Wikipedia, citing WSJ](https://en.wikipedia.org/wiki/Palantir_Technologies)). Cohort size and pay are [UNVERIFIED].
  - The American Tech Fellowship has more than 1,000 graduates ([Q2-26 call](https://www.fool.com/earnings/call-transcripts/2026/08/10/palantir-pltr-q2-2026-earnings-call-transcript/)).
- **Ideology as brand.** The listing letter said "We have chosen sides" ([S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm)). Karp and Zamiska's *The Technological Republic* (Feb 2025) extends the argument [UNVERIFIED: publisher page not fetched].
- **Costs of that stance.** Staff left over the Israel work and dissented internally over ICE, and Palantir paid a $1.7M hiring-discrimination settlement in 2017 ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)).

---

## 6. Controversies & failure modes
- **ICE.**
  - The ELITE app gives deportation officers target profiles with "confidence scores".
  - DHS published no privacy impact assessment for it.
  - Some surge hires got access before their background checks were finished ([Intercept, 3 Sep 2026](https://theintercept.com/2026/09/03/ice-palantir-privacy-personal-info-access/), [ACLU](https://www.aclu.org/news/privacy-technology/palantir-deportation-roundup)).
- **NHS FDP.**
  - Patients can't opt out of direct-care uses of the platform.
  - An extra 60,000 patients opted out of NHS data sharing between mid-May and mid-July 2026, which a minister linked to mistrust of Palantir.
  - A Commons committee has urged ending Palantir's role ([Register](https://www.theregister.com/databases/2026/06/13/nhs-patients-cant-opt-out-of-palantirs-data-platform-but-their-hospital-can/5254766), [resultsense](https://www.resultsense.com/news/2026-09-08-palantir-nhs-data-optouts/)).
- **Predictive policing.**
  - New Orleans ran a partnership from 2012 ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)); The Verge exposed it in 2018 ([Verge](https://www.theverge.com/2018/2/27/17054740/palantir-predictive-policing-tool-new-orleans-nopd), not fetched).
  - The LAPD's LASER programme shut in 2019 after an inspector-general audit ([Brennan](https://www.brennancenter.org/our-work/research-reports/predictive-policing-explained)). Palantir's role in it is [UNVERIFIED].
  - Denmark's POL-INTEL has run on Gotham since 2017 ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)).
- **Germany.**
  - **The ruling.** On **16 Feb 2023** the Federal Constitutional Court struck down the Hesse and Hamburg laws on automated police data analysis (Hesse §25a HSOG, Hamburg §49 PolDVG; cases 1 BvR 1547/19 and 1 BvR 2634/20). Hesse's law could stay, with restrictions, until 30 Sep 2023; Hamburg's was void at once. Any new law needs an "identifiable danger" threshold and statutory limits on data types, volumes and methods, including self-learning AI ([BVerfG](https://www.bundesverfassungsgericht.de/SharedDocs/Pressemitteilungen/EN/2023/bvg23-018.html)).
  - **Bavaria.** Its VeRA system (since 2024) faces a constitutional complaint from the civil-rights group GFF ([The Local](https://www.thelocal.de/20250805/what-is-palantirs-gotham-software-and-why-do-german-police-want-it)).
  - **Baden-Württemberg.** It signed a €25M, 5-year contract in March 2025, *before* it had a legal basis. It legislated on 12 Nov 2025 (113–22) and must switch to a European alternative "by 2030 at the latest" ([Posteo](https://posteo.de/en/news/germany-baden-w%C3%BCrttemberg-approves-use-of-palantir)).
  - **Federal level.** Interior Minister Dobrindt wants Palantir for the federal criminal police (BKA) and the federal police. Johannes Schätzl, of coalition partner SPD: "It must never be allowed" ([Tagesspiegel, 11 Oct 2025](https://www.tagesspiegel.de/dobrindt-will-palantir-fur-bka-und-bundespolizei-das-darf-niemals-zugelassen-werden-sagt-der-koalitionspartner-14539292.html)). Cabinet proposals on 30 Apr 2026 add AI data-analysis powers ([drweb](https://www.drweb.de/waehrend-sap-eine-milliarde-investiert-kaufen-fuenf-laender-palantir/)).
  - **Security services.** The BfV (domestic intelligence) chose ChapsVision in May 2026 ([Cybernews](https://cybernews.com/tech/germany-palantir-alternative/)). The Bundeswehr ruled out Maven "for now" while it evaluates 30 alternatives ([TIKR](https://www.tikr.com/blog/wall-street-just-gave-palantir-its-highest-ever-price-target-right-when-europe-started-saying-no)).
- **Switzerland.**
  - A 20-page Army Staff report (Dec 2024) called the software "impressive". It also warned that "sensitive data could be accessed by the US government and intelligence services", flagged dependency risks, and recommended alternatives.
  - No purchase followed, despite Palantir approaching at least seven federal bodies between 2018 and 2024. Palantir then sued the magazine Republik, which broke the story, for a right of reply ([Republik](https://www.republik.ch/2026/02/18/how-tenaciously-palantir-courted-switzerland)).
  - Palantir's rebuttal said the report was not a "rigorous technical… risk assessment" and cited a Fraunhofer review finding "no hidden backdoors" ([Palantir blog](https://blog.palantir.com/korrektur-wie-das-online-magazin-die-republik-einen-regierungsbericht-zu-palantir-verdrehte-9a1b399ae255)).
- **European sovereignty.** Thales pitched Hexaforce as a Maven alternative free of US export controls, framing the issue as "whether Washington could pull the plug" ([TIKR](https://www.tikr.com/blog/wall-street-just-gave-palantir-its-highest-ever-price-target-right-when-europe-started-saying-no)).
- **Lock-in and cost.**
  - The Met Police deal was blocked for a "clear and serious breach" of procurement rules. MP Chi Onwurah warned about "vendor lock-in" ([Computing](https://www.computing.co.uk/news/2026/government/sadiq-khan-blocks-met-s-50m-ai-deal-with-palantir)).
  - Older disputes (NYPD data export, 2016 commercial churn) are [UNVERIFIED].

**Analysis.** The failures cluster around three things:
- surveillance of citizens without a clear legal basis;
- sole-source or direct awards, which invite backlash;
- exposure to a foreign (US) jurisdiction.

None of them is a failure of the technology itself.

---

## 7. Africa & the Middle East
- **Africa: no verified customer, office or strategy.** This is a gap in what I could find, not proof that none exists.
  - Palantir's jobs board (25 Sep 2026) lists Abu Dhabi, Dubai and Tel Aviv, and no African city ([Lever](https://jobs.lever.co/palantir)).
  - **The indirect footprint is humanitarian.** The UN World Food Programme (WFP) signed a 5-year Foundry deal on 5 Feb 2019 ([WFP](https://www.wfp.org/news/palantir-and-wfp-partner-help-transform-global-humanitarian-delivery)). Palantir remains "the cornerstone of the WFP data ecosystem" across 120 countries ([Privacy International, Jun 2026](https://privacyinternational.org/news-analysis/5789/world-food-programme-expand-palantir-partnership)).
  - Oracle's Africa site markets Palantir with no African customers named ([Oracle](https://www.oracle.com/africa/partner/palantir/)).
  - Co-founder Joe Lonsdale's fund, 8VC, led an $11.8M round in Nigeria's Terra Industries (12 Jan 2026). Terra protects about $11B of infrastructure in Nigeria and Ghana ([DroneXL](https://dronexl.co/2026/01/12/palantir-investment-nigeria-drone-startup/)).
- **The African incumbent is Chinese.** Huawei had 73 "Safe City" agreements in 52 countries, 22% of them in sub-Saharan Africa. Its claimed 46% crime cut in Kenya's Safe City areas was contradicted by police data ([CSIS, Nov 2019](https://www.csis.org/analysis/watching-huaweis-safe-cities)).
- **Middle East.**
  - The IDF partnership, and Iran nuclear-deal (JCPOA) verification for the IAEA through Palantir's MOSAIC ([Wikipedia](https://en.wikipedia.org/wiki/Palantir_Technologies)).
  - The Aither joint venture with Dubai Holding, which Palantir executive Noam Perski called "part of an AI arms race" ([The National](https://www.thenationalnews.com/future/technology/2025/11/04/dubai-holding-and-us-based-palantir-in-joint-venture-to-lead-ai-transformation/)).
  - No Saudi or Qatari contract could be verified.

**Analysis.** For Palantir, Africa is a humanitarian use case, not a market. The model to copy or pre-empt is the Gulf template: a joint venture co-owned with a sovereign holding company.

---

## 8. Competitive landscape

| Player | What it does | How it differs from Palantir | Status (dated) |
|---|---|---|---|
| **Anduril** (Lattice) | Autonomy and command software | Sells its own hardware too | Valued at $61B (May 2026); about $2B revenue in 2025 ([Wikipedia](https://en.wikipedia.org/wiki/Anduril_Industries)) |
| **Scale AI** (Donovan, Thunderforge) | Data labelling, now also defence LLM apps | Model and data layer | Meta took a 49% non-voting stake (about $14.8B per Wikipedia; other reports differ) and founder Wang left (Jun 2025) ([Wikipedia](https://en.wikipedia.org/wiki/Scale_AI)) |
| **Shield AI** (Hivemind) | Autonomy for aircraft | Sells airframes plus autonomy | Raising $1.5B at $12.7B (Mar 2026) ([Wikipedia](https://en.wikipedia.org/wiki/Shield_AI)) |
| **Vannevar Labs** (Decrypt) | Foreign-language open-source intelligence | Narrow mission app | Profitable by Jan 2023; up to $99M production contract from the Defense Innovation Unit (Nov 2024) ([Wikipedia](https://en.wikipedia.org/wiki/Vannevar_Labs)) |
| **Rhombus Power** (Guardian) | AI for early warning [UNVERIFIED] | — | — |
| **Govini, now "Air"** | Analytics for defence acquisition, supply chain and readiness | Vertical data product | Rebranded in 2026 ([Air](https://air.ai/)) |
| **C3.ai** | Enterprise AI apps | App templates, sold mainly through partners | FY2026 revenue $250M (−36%), net loss $470M; CEO replaced (Sep 2025) ([StockAnalysis](https://stockanalysis.com/stocks/ai/financials/), [Wikipedia](https://en.wikipedia.org/wiki/C3.ai)) |
| **Databricks** | Data lakehouse plus agent tools (Agent Bricks) | Horizontal, open formats, adopted bottom-up | Valued at $190B (Aug 2026); $6.9B annualised revenue (Jun 2026) ([Wikipedia](https://en.wikipedia.org/wiki/Databricks)) |
| **Snowflake, Microsoft Fabric, Google** | Data clouds with bundled AI | Bundling and easy procurement | Not researched in depth [gap] |
| **Cognite** | Data operations for heavy industry | Industrial vertical; Saudi Aramco owns 7.4% | ([Wikipedia](https://en.wikipedia.org/wiki/Cognite)) |
| **Quantexa** | Entity resolution | Financial-crime wedge | Valued at $2.6B (Mar 2025); recurring revenue above $100M a year (Oct 2024) ([Wikipedia](https://en.wikipedia.org/wiki/Quantexa)) |
| **Dataminr** | Real-time event alerting | Alerting rather than a platform | Buying ThreatConnect for $290M (Oct 2025) ([Wikipedia](https://en.wikipedia.org/wiki/Dataminr)) |
| **Helsing** | Defence AI plus drones | Pitches itself as "a European sovereign alternative" | Valued at $18B (Jul 2026) ([Wikipedia](https://en.wikipedia.org/wiki/Helsing_(company))) |
| **Preligens, now Safran.ai** | AI for satellite and aerial imagery | Bought by a defence prime | Acquired 2024 for a reported €220M [UNVERIFIED] |
| **Faculty** | UK "decision intelligence" | Services-heavy | Accenture is buying it for more than £740M (Jan 2026) ([Wikipedia](https://en.wikipedia.org/wiki/Faculty_(company))) |
| **ChapsVision** | French sovereign analytics | Runs on an air-gapped sovereign cloud | Won the DGSI and the BfV (2026) ([Sifted](https://sifted.eu/articles/chapsvision-to-replace-palantir-in-major-contract-with-french-intelligence-agency)) |
| **Huawei Safe City / Alibaba City Brain** | State-backed city surveillance and traffic AI | Bundled with finance | City Brain: Hangzhou 2016, Kuala Lumpur 2018 ([Wikipedia](https://en.wikipedia.org/wiki/City_Brain), [CSIS](https://www.csis.org/analysis/watching-huaweis-safe-cities)) |

**Analysis: which "Palantir for X" plays worked.**
- **The winners do one of three things:**
  - own a narrow, painful workflow backed by proprietary data (Quantexa, Vannevar, Air);
  - own hardware and weapons (Anduril, Helsing);
  - sell sovereignty to a jurisdiction that wants it (ChapsVision).
- **The strugglers made one of three mistakes:**
  - went horizontal without deployment depth (C3.ai);
  - lost their neutrality (Scale, after Meta's stake);
  - stayed a services company and sold out (Faculty).
- **Databricks is the real horizontal threat.** Build on top of open lakehouse formats rather than fight them.

---

## 9. Lessons for a challenger

**What made Palantir work**
1. A patient anchor customer (the CIA) and patient capital ([Forbes](https://www.forbes.com/sites/andygreenberg/2013/08/14/agent-of-intelligence-how-a-deviant-philosopher-built-palantir-a-cia-funded-data-mining-juggernaut/)).
2. Data integration plus fine-grained security, which defused the internal politics over who controls data ([Qureshi](https://nabeelqu.co/reflections-on-palantir)).
3. The loop in which field engineering became product ([Qureshi](https://nabeelqu.co/reflections-on-palantir)).
4. Deployment tooling that cut installation from 5 months to 14 days ([S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm)).
5. Bootcamps instead of pilots ([Q4-23 call](https://www.fool.com/earnings/call-transcripts/2024/02/05/palantir-technologies-pltr-q4-2023-earnings-call-t/)).
6. Expansion within accounts: top-20 customer revenue grew 5× between 2019 and 2026.
7. Enterprise agreements that consolidate scattered spending ([Army.mil](https://www.army.mil/article/287506/u_s_army_awards_enterprise_service_agreement_to_enhance_military_readiness_and_drive_operational_efficiency)).

**What nearly killed it**
- Twenty years to a GAAP profit.
- Stock-based compensation equal to 50% of revenue in 2021.
- The top-3 customers were 28% of revenue in 2019 ([S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm)).
- Pilots swallowed by the work of getting data access.
- Pariah status in 2016–2020 ([Qureshi](https://nabeelqu.co/reflections-on-palantir)).
- Pushback on constitutional and sovereignty grounds.

**Replicable in 2026**
- Open-weight models: Nemotron "beat frontier models" on five production tasks within 24 hours, according to Palantir ([Q2-26 call](https://www.fool.com/earnings/call-transcripts/2026/08/10/palantir-pltr-q2-2026-earnings-call-transcript/)).
- Agent-assisted build work, as AI FDE shows.
- Open-source building blocks: Postgres with row-level security, Keycloak, Kubernetes, Iceberg (my analysis).
- The bootcamp sales motion.

**Not replicable (soon)**
- US accreditations and programs of record.
- $9.2B of cash.
- Classified relationships built over decades.
- A 4,400-person organisation.

---

## Implications for Vigil (opinionated)
1. **Sell sovereignty, not "an African Palantir".**
   - Put these in the headline: on-prem or South Africa-hosted deployment, no US-jurisdiction dependency, customer-held keys and a published independent code audit.
   - Europe's exits and refusals prove the demand.
   - South Africa's draft AI Policy (10 Apr 2026, since withdrawn over hallucinated citations) named "geopolitical dependencies on foreign AI infrastructure" a critical gap ([Wikipedia](https://en.wikipedia.org/wiki/Draft_South_Africa_National_Artificial_Intelligence_(AI)_Policy_2026), [CNBC Africa](https://www.cnbcafrica.com/2026/south-africa-pulls-ai-policy-after-hallucinated-citations-expose-drafting-scandal)).
2. **Make Transnet your "CIA".**
   - Propose one anchor programme, such as a rail and port corridor digital twin plus asset-security workflows.
   - Deliver it as 2–5 day bootcamps on live data, each with pre-agreed conversion into a multi-year enterprise agreement. Never do free 8–12-week pilots.
   - Land a second anchor within 12 months so no customer exceeds about 30% of revenue.
3. **Build the three moats first.**
   - An Ontology with actions and write-back to source systems.
   - Purpose- and marking-based access control on Postgres row-level security plus Keycloak, with lineage and audit.
   - An "Apollo-lite" deployer for air-gapped installations.
   - Chat and agents are a thin layer on top.
4. **Make "constitutional by design" a feature.**
   - Encode the German court's tests as policy objects: purpose binding, limits on data categories and methods, and a logged justification per query.
   - This lets buyers worried about POPIA and the constitutional right to privacy (s14) say yes.
5. **Enter through procurement integrity, not surveillance.**
   - Package your eTenders OCDS, entity-resolution and due-diligence work as the first "Ontology pack": tender risk, beneficial ownership and conflict-of-interest graphs.
   - Target Treasury, the SIU, the Auditor-General and SOE supply-chain units.
   - This is the Quantexa pattern.
6. **Run an AI-leveraged FDE model.**
   - Small pods of technical FDEs ("Deltas") and deployment strategists ("Echos"), with a Dev team that owns productisation.
   - Measure time to first production workflow and gross margin per deployment.
   - Build internal "AI FDE" agents early, because the FDE boom means you will compete with AI labs for talent.
7. **Price in rand, on capacity, not tokens.** Fixed-fee bootcamps, then term enterprise agreements tiered by compute and Ontology volume, with model costs passed through at cost.
8. **Stay model-agnostic, open-weight first.** Keep a way to swap out any single US lab. TIKR reports that Palantir customers "barely noticed" US restrictions on Anthropic's models ([TIKR](https://www.tikr.com/blog/palantir-stock-what-karps-ai-sovereignty-bet-means-for-investors-in-2026)).
9. **Turn local credentials into barriers.** Palantir can't match these: B-BBEE status, South African security clearances, local data residency, and relationships with CSIR, Armscor and SITA. Add ISO 27001 and SOC 2 early.
10. **Pre-empt the Gulf and Chinese playbooks.** Offer governments co-ownership or joint-venture vehicles (the Aither pattern), and compete with bundled "Safe City" deals on auditability.
11. **Raise patient, blended capital** (development-finance institutions, strategic and sovereign investors), and keep stock-based compensation well below Palantir's 2021 level of 50% of revenue.
12. **Publish a "won't do" list before the first controversy**, for example no mass profiling of citizens without a statutory basis. Treat Databricks and Fabric as layers to integrate with, not enemies.

---

## Research limitations
- WebSearch hit its session cap after about 45 searches; later checks used WebFetch.
- Some sites were blocked: CNBC, The Verge, Bloomberg, heise.
- Some Wikipedia pages were not reachable, and Palantir's Warp Speed, MetaConstellation and Skykit pages were rate-limited.
- Check the [UNVERIFIED] items before using them externally.

## Sources
**Palantir filings and results:**
- [Q4/FY2025 release](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000004/a2025q4ex991earningsrelease.htm)
- [FY2025 10-K](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000011/pltr-20251231.htm)
- [Q1-26 release](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000026/a2026q1ex991pressrelease.htm)
- [Q2-26 release](https://www.sec.gov/Archives/edgar/data/1321655/000132165526000039/a2026q2ex991pressrelease.htm)
- [Q2-26 release (Nasdaq)](https://www.nasdaq.com/press-release/palantir-reports-q2-2026-us-comm-revenue-growth-149-y-y-and-revenue-growth-93-y-y)
- [Q1-26 Business Update](https://investors.palantir.com/files/Palantir%20-%20Q1%202026%20Business%20Update.pdf)
- [Q2-26 Business Update](https://investors.palantir.com/files/Palantir%20-%20Q2%202026%20Business%20Update.pdf)
- [2020 S-1](https://www.sec.gov/Archives/edgar/data/1321655/000119312520230013/d904406ds1.htm)
- [Q1-24 release](https://www.nasdaq.com/press-release/palantir-reports-revenue-growth-of-21-year-over-year-and-sixth-consecutive-quarter-of)
- [Q2-23 release](https://investors.palantir.com/news-details/2023/Palantir-Reports-Its-Third-Consecutive-Quarter-of-GAAP-Profitability-GAAP-EPS-of-0.01-in-Q2-2023/)

**Earnings calls:**
- [Q4-23 call](https://www.fool.com/earnings/call-transcripts/2024/02/05/palantir-technologies-pltr-q4-2023-earnings-call-t/)
- [Q4-25 call](https://www.investing.com/news/transcripts/earnings-call-transcript-palantirs-q4-2025-revenue-surges-stock-rises-93CH-4480410)
- [Q1-26 call](https://www.fool.com/earnings/call-transcripts/2026/05/04/palantir-pltr-q1-2026-earnings-transcript/)
- [Q2-26 call](https://www.fool.com/earnings/call-transcripts/2026/08/10/palantir-pltr-q2-2026-earnings-call-transcript/)
- [Q2-26 call highlights](https://finance.yahoo.com/markets/stocks/articles/palantir-technologies-inc-pltr-q2-050456686.html)

**Palantir product documentation:**
- [AIP](https://www.palantir.com/docs/foundry/aip/overview/)
- [Logic](https://www.palantir.com/docs/foundry/logic/overview/)
- [Chatbot Studio](https://www.palantir.com/docs/foundry/agent-studio/overview/)
- [Automate](https://www.palantir.com/docs/foundry/automate/overview/)
- [Evals](https://www.palantir.com/docs/foundry/aip-evals/overview/)
- [AI FDE](https://www.palantir.com/docs/foundry/ai-fde/overview/)
- [Usage types](https://www.palantir.com/docs/foundry/resource-management/usage-types/)
- [Ontology](https://www.palantir.com/docs/foundry/ontology/overview/)
- [Foundry overview](https://www.palantir.com/docs/foundry/platform-overview/overview/)
- [Apollo](https://www.palantir.com/docs/apollo/core/introduction/)
- [Gotham](https://www.palantir.com/docs/gotham/)
- [FedStart](https://www.palantir.com/fedstart/)
- [Palantir jobs](https://jobs.lever.co/palantir)

**Markets and valuation:**
- [StockAnalysis income](https://stockanalysis.com/stocks/pltr/financials/)
- [StockAnalysis cash flow](https://stockanalysis.com/stocks/pltr/financials/cash-flow-statement/)
- [StockAnalysis market cap](https://stockanalysis.com/stocks/pltr/market-cap/)
- [ad-hoc-news](https://www.ad-hoc-news.de/boerse/news/corporate-news/palantir-stock-weighs-93-percent-growth-against-valuation/70166350)
- [Nasdaq/Motley Fool](https://www.nasdaq.com/articles/down-27-2026-palantir-stock-buy)
- [Futurum](https://futurumgroup.com/insights/palantir-q2-fy-2026-earnings-surge-on-us-commercial-ai-demand/)
- [TIKR on Europe](https://www.tikr.com/blog/wall-street-just-gave-palantir-its-highest-ever-price-target-right-when-europe-started-saying-no)
- [TIKR on sovereignty](https://www.tikr.com/blog/palantir-stock-what-karps-ai-sovereignty-bet-means-for-investors-in-2026)
- [TheStreet](https://www.thestreet.com/investing/palantir-nebius-sovereign-ai-partnership)
- [CNBC](https://www.cnbc.com/2024/11/15/palantir-jumps-9percent-to-a-record-after-announcing-move-to-nasdaq.html)
- [Nasdaq-100](https://www.nasdaq.com/articles/palantir-stock-joining-nasdaq-100-index-and-super-micro-computer-being-booted-what)

**History:**
- [Wikipedia: Palantir](https://en.wikipedia.org/wiki/Palantir_Technologies)
- [Forbes 2013](https://www.forbes.com/sites/andygreenberg/2013/08/14/agent-of-intelligence-how-a-deviant-philosopher-built-palantir-a-cia-funded-data-mining-juggernaut/)

**Contracts:**
- [Army.mil](https://www.army.mil/article/287506/u_s_army_awards_enterprise_service_agreement_to_enhance_military_readiness_and_drive_operational_efficiency)
- [DefenseScoop 2026](https://defensescoop.com/2026/04/15/palantir-maven-smart-system-pentagon-program-transition-feinberg/)
- [DefenseScoop 2025](https://defensescoop.com/2025/05/23/dod-palantir-maven-smart-system-contract-increase/)
- [Motley Fool on Maven](https://www.fool.com/investing/2026/08/25/palantirs-maven-is-now-an-official-pentagon-progra/)
- [NCIA](https://www.ncia.nato.int/newsroom/news/nato-acquires-aienabled-warfighting-system)
- [MoD](https://www.wired-gov.net/wg/news.nsf/articles/New+strategic+partnership+to+unlock+billions+and+boost+military+AI+and+innovation+18092025161000?open=)
- [The Register, UK deal](https://www.theregister.com/2025/09/20/uk_palantir_defense_pact/)
- [UKAuthority](https://www.ukauthority.com/articles/mod-continues-data-analytics-contract-with-palantir)
- [openDemocracy](https://www.opendemocracy.net/en/palantir-ministry-defence-hire-four-officials-2025-record-defence-contract-240-million/)
- [Computing](https://www.computing.co.uk/news/2026/government/sadiq-khan-blocks-met-s-50m-ai-deal-with-palantir)
- [The Register, NHS](https://www.theregister.com/databases/2026/06/13/nhs-patients-cant-opt-out-of-palantirs-data-platform-but-their-hospital-can/5254766)
- [resultsense](https://www.resultsense.com/news/2026-09-08-palantir-nhs-data-optouts/)
- [ACLU](https://www.aclu.org/news/privacy-technology/palantir-deportation-roundup)
- [The Intercept](https://theintercept.com/2026/09/03/ice-palantir-privacy-personal-info-access/)
- [Ukraine gov](https://digitalstate.gov.ua/news/tech/ukraine-launches-brave1-dataroom-with-palantir-to-train-ai-models-using-battlefield-data)
- [Defense News](https://www.defensenews.com/global/europe/2026/09/01/ousted-ukrainian-defense-chief-lands-palantir-ceo-as-first-investor-in-new-venture/)
- [Business Wire: DGSI](https://www.businesswire.com/news/home/20251215083215/en/Palantir-Announces-Renewal-of-Multi-Year-Contract-with-the-DGSI)
- [Sifted](https://sifted.eu/articles/chapsvision-to-replace-palantir-in-major-contract-with-french-intelligence-agency)
- [Cybernews](https://cybernews.com/tech/germany-palantir-alternative/)
- [Dubai Media Office](https://www.mediaoffice.ae/en/news/2025/november/04-11/dubai-holding-and-palantir-launch-aither)
- [The National](https://www.thenationalnews.com/future/technology/2025/11/04/dubai-holding-and-us-based-palantir-in-joint-venture-to-lead-ai-transformation/)

**Operating model and culture:**
- [Nabeel Qureshi](https://nabeelqu.co/reflections-on-palantir)
- [a16z](https://a16z.com/services-led-growth/)
- [Fast Company](https://www.fastcompany.com/91435680/postings-for-this-ai-job-are-up-800)
- [Fortune](https://fortune.com/2026/09/03/forward-deployed-engineers-fast-growing-six-figure-silicon-valley-job-integrate-ai-with-customers-tech-careers-palantir/)
- [Paraform](https://www.paraform.com/insights/forward-deployed-engineer-palantir-runway-greptile)
- [Oracle Africa](https://www.oracle.com/africa/partner/palantir/)

**Controversies:**
- [BVerfG](https://www.bundesverfassungsgericht.de/SharedDocs/Pressemitteilungen/EN/2023/bvg23-018.html)
- [The Local](https://www.thelocal.de/20250805/what-is-palantirs-gotham-software-and-why-do-german-police-want-it)
- [Posteo](https://posteo.de/en/news/germany-baden-w%C3%BCrttemberg-approves-use-of-palantir)
- [drweb](https://www.drweb.de/waehrend-sap-eine-milliarde-investiert-kaufen-fuenf-laender-palantir/)
- [Tagesspiegel](https://www.tagesspiegel.de/dobrindt-will-palantir-fur-bka-und-bundespolizei-das-darf-niemals-zugelassen-werden-sagt-der-koalitionspartner-14539292.html)
- [Bundestag hib](https://www.bundestag.de/presse/hib/kurzmeldungen-1155436)
- [Republik](https://www.republik.ch/2026/02/18/how-tenaciously-palantir-courted-switzerland)
- [Palantir rebuttal](https://blog.palantir.com/korrektur-wie-das-online-magazin-die-republik-einen-regierungsbericht-zu-palantir-verdrehte-9a1b399ae255)
- [Brennan Center](https://www.brennancenter.org/our-work/research-reports/predictive-policing-explained)
- [The Verge (not fetched)](https://www.theverge.com/2018/2/27/17054740/palantir-predictive-policing-tool-new-orleans-nopd)

**Africa and the Middle East:**
- [WFP](https://www.wfp.org/news/palantir-and-wfp-partner-help-transform-global-humanitarian-delivery)
- [Privacy International](https://privacyinternational.org/news-analysis/5789/world-food-programme-expand-palantir-partnership)
- [DroneXL](https://dronexl.co/2026/01/12/palantir-investment-nigeria-drone-startup/)
- [CSIS](https://www.csis.org/analysis/watching-huaweis-safe-cities)
- [Wikipedia: SA AI Policy](https://en.wikipedia.org/wiki/Draft_South_Africa_National_Artificial_Intelligence_(AI)_Policy_2026)
- [CNBC Africa](https://www.cnbcafrica.com/2026/south-africa-pulls-ai-policy-after-hallucinated-citations-expose-drafting-scandal)

**Competitors:**
- [Anduril](https://en.wikipedia.org/wiki/Anduril_Industries)
- [Scale AI](https://en.wikipedia.org/wiki/Scale_AI)
- [Shield AI](https://en.wikipedia.org/wiki/Shield_AI)
- [C3.ai](https://en.wikipedia.org/wiki/C3.ai)
- [C3.ai financials](https://stockanalysis.com/stocks/ai/financials/)
- [Databricks](https://en.wikipedia.org/wiki/Databricks)
- [Helsing](https://en.wikipedia.org/wiki/Helsing_(company))
- [Quantexa](https://en.wikipedia.org/wiki/Quantexa)
- [Dataminr](https://en.wikipedia.org/wiki/Dataminr)
- [Faculty](https://en.wikipedia.org/wiki/Faculty_(company))
- [Cognite](https://en.wikipedia.org/wiki/Cognite)
- [Vannevar Labs](https://en.wikipedia.org/wiki/Vannevar_Labs)
- [Air (formerly Govini)](https://air.ai/)
- [City Brain](https://en.wikipedia.org/wiki/City_Brain)
