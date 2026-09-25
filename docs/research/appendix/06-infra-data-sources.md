<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Raw research appendix (P0). Sourced analyst report; verify [UNVERIFIED] items before external use. -->

> **Appendix to** [`VIGIL_RESEARCH_DOSSIER.md`](../VIGIL_RESEARCH_DOSSIER.md). A raw research report produced on 24–25 Sep 2026 by a parallel research agent. Kept verbatim for traceability. Where it conflicts with the dossier, the dossier wins (the dossier applies the founders' greenfield decision).
>
> **Internal working paper; not for external distribution.**

# Vigil Research Pack 06: Infrastructure, Compute, Connectivity and Phase-Zero Public Data

*For the Vigil founding team (MB Tech, Johannesburg). Research date: 24 September 2026. Every figure carries a date. [UNVERIFIED] means I could not confirm it from a fetched primary or reputable source. Sections are split into **Facts** (sourced) and **Analysis** (my interpretation).*

---

## Executive summary

1. **Host sovereign workloads in SA carrier-neutral colocation, not in US hyperscaler regions.** South Africa holds more than 60% of Africa's 500+ MW of operational data-centre capacity ([Nairametrics/Fortren, 27 Jul 2026](https://nairametrics.com/2026/07/27/africas-data-centre-capacity-exceeds-500-mw-with-890-mw-in-pipeline-report/)). Teraco alone reports 189 MW ([Teraco, 10 Nov 2025](https://www.teraco.co.za/news/teraco-completes-ct2-data-centre-expansion/)).
2. **Hyperscaler AI inside SA is thin.** AWS af-south-1 has only G4dn (T4) and Inf1 accelerators ([AWS docs](https://docs.aws.amazon.com/ec2/latest/instancetypes/ec2-instance-regions.html)). Claude 4.5 on Bedrock in Cape Town uses *global* cross-region inference, so requests may be processed outside SA ([AWS, 30 Jan 2026](https://aws.amazon.com/blogs/machine-learning/scale-ai-in-south-africa-using-amazon-bedrock-global-cross-region-inference-with-anthropic-claude-4-5-models/)). Azure South Africa North is the only hyperscaler region listing 8×H100 VMs ([Northflank](https://northflank.com/cloud/azure/instances/Standard_ND96isr_H100_v5)). GCP africa-south1 has no GPUs ([gcloud-compute](https://gcloud-compute.com/africa-south1.html)).
3. **The US CLOUD Act risk is on the record.** Microsoft France told the French Senate on 18 Jun 2025 that it "cannot guarantee" customer data would not be handed to US authorities ([The Register, 25 Jul 2025](https://www.theregister.com/off-prem/2025/07/25/microsoft-exec-admits-it-cannot-guarantee-data-sovereignty/458553)).
4. **Start compute with RTX PRO 6000 Blackwell (96 GB) servers, not H100/B200.** A DGX B200 draws about 14.3 kW ([NVIDIA](https://www.nvidia.com/en-us/data-center/dgx-b200/)), roughly 3.5 times a standard 4 kW retail rack ([xneelo](https://xneelo.co.za/colocation/)).
5. **Power has improved a lot, but not everywhere.** By 4 Sep 2026 SA had gone 476 consecutive days without load-shedding, and Eskom's year-to-date EAF was 67.79% ([Eskom, 4 Sep 2026](https://www.eskom.co.za/eskom-fully-meets-2026-winter-demand-eaf-remains-at-its-highest-level-since-2020-67-79-and-diesel-expenditure-declines-by-r4-84-billion-or-81-64-to-deliver-energy-security/)). Localised "load reduction" still affected 3.4–6.5% of customers in September 2026.
6. **Connectivity is diverse but fragile.** Fourteen cable systems land in or are planned for SA ([TeleGeography](https://www.submarinecablemap.com/api/v3/country/south-africa.json)), and in 2024 cables on both coasts failed within 60 days of each other. Starlink is still unlicensed ([TechCentral, 20 Aug 2026](https://techcentral.co.za/spacex-breaks-cover-at-icasa-satellite-spectrum-hearings/285094/)). Amazon Leo is due in 2027 through Herotel ([TechCentral, 23 Aug 2026](https://techcentral.co.za/cell-c-in-talks-with-starlink-and-amazon-leo/285187/)).
7. **Several data sources carry licensing traps:** SAFLII bans automated access; the CSD is closed to the public; ACLED needs a corporate licence for commercial users; Global Fishing Watch is non-commercial only; Open-Meteo requires a paid plan for commercial use. Details are in §5.
8. **Prototype ranking:** (1) procurement-integrity graph, (2) Durban port and corridor twin, (3) Cape Town municipal service-delivery twin. The grid and infrastructure-crime twin works better as a layer inside these than as a standalone prototype.

**Method and limitations.** I ran 30 WebSearch queries and about 100 WebFetch calls on 24 Sep 2026. The session's shared search budget ran out after 30 queries, so after that I could only fetch pages directly. The fetch proxy then returned HTTP 429 (rate-limited), so I could **not read** FEWS NET, UN Comtrade, the World Bank API docs, Meta's WhatsApp pricing pages or Africa's Talking pricing. Several government portals also timed out or returned errors. The full list is at the end of the Sources, and every row that depends on those pages is flagged.

---

## 1. SA and African data centres

### 1.1 Facts: operators and capacity

| Operator | SA footprint | Capacity (date) | 2025–26 developments |
|---|---|---|---|
| **Teraco** (Digital Realty subsidiary) | Isando (JB1, JB3, JB5), Bredell (JB2, JB4), Cape Town (CT1, CT2), Durban (DB1) | **189 MW** in total: Isando 70, Bredell 64, Cape Town 53, Durban 2 ([10 Nov 2025](https://www.teraco.co.za/news/teraco-completes-ct2-data-centre-expansion/)) | CT2 added 32 MW, reaching 50 MW, on 90 MW of utility supply with zero-water cooling. JB4 expansion completed 12 Aug 2025. CT1 expansion announced 12 Nov 2025. The NAPAfrica exchange passed 6 Tbps on 11 Nov 2025 ([Teraco news](https://www.teraco.co.za/news/)). **JB7**, 40 MW at Isando, was announced 13 Nov 2024 with an R8bn loan ([ITWeb](https://www.itweb.co.za/article/teraco-announces-jb7-a-40mw-data-centre-expansion-to-its-isando-campus-and-new-r8bn-syndicated-loan/8OKdWqDXdXxqbznQ)). BusinessTech Africa reports US$877m for four new SA facilities in 2025–26 ([1 Apr 2026](https://www.businesstechafrica.co.za/technology/2026/04/01/equinix-is-accelerating-its-expansion-plans-in-johannesburg-less-than-two-years-after-its-south-african-debut/)). |
| **Equinix** | JN1 in Germiston | Opened Oct 2024 with 4 MW and 700 cabinets; full build 20 MW ([BTA, 1 Apr 2026](https://www.businesstechafrica.co.za/technology/2026/04/01/equinix-is-accelerating-its-expansion-plans-in-johannesburg-less-than-two-years-after-its-south-african-debut/)) or 24 MW ([iAfrica, 14 Aug 2026](https://iafrica.com/equinix-scales-johannesburg-data-centre-to-24mw-but-its-r7-5bn-expansion-land-sits-undeveloped/)) | Initial investment US$160m. R7.5bn expansion budget. Land bought in JHB and Cape Town in 2026 but not yet developed. Build cost US$10.06 per watt. Designed for liquid cooling ([iAfrica](https://iafrica.com/equinix-scales-johannesburg-data-centre-to-24mw-but-its-r7-5bn-expansion-land-sits-undeveloped/)). A proposed 160 MVA Cape Town hyperscale site was approved on 14 Jul 2026, then suspended by an appeal filed 6 Aug 2026 (Housing Assembly and Foxglove), citing missing load-flow and environmental assessments ([Daily Maverick, 20 Sep 2026](https://www.dailymaverick.co.za/article/2026-09-20-data-centre-deal-cape-town-forced-back-to-the-drawing-board/)). Africa-wide commitment: US$390m over five years ([BTA](https://www.businesstechafrica.co.za/technology/2026/04/01/equinix-is-accelerating-its-expansion-plans-in-johannesburg-less-than-two-years-after-its-south-african-debut/)). |
| **Vantage** | JNB1 at Waterfall City, Midrand; a second campus announced Dec 2022 ([BusinessWire](https://www.businesswire.com/news/home/20221213005272/en/Vantage-Data-Centers-Announces-Second-Campus-in-Johannesburg)) | **120 MW** critical load at full build across three buildings, with 180 MVA supplied by Eskom ([Vantage](https://vantage-dc.com/data-center-locations/emea/johannesburg-i-south-africa/)). BTA cites R15bn for 80 MW. | Relies on the Eskom grid through multiple diverse feeds; the site mentions no on-site renewables. |
| **Africa Data Centres** (Cassava) | JHB1, JHB2, CPT1; CPT2 under construction | **30 MW live plus 20 MW under construction** ([Capacity, 6 Feb 2026](https://capacityglobal.com/news/cassava-africa-data-centres-acquisition/)) | The Competition Tribunal approved STANLIB Infrastructure Fund II taking a substantial stake with joint control (Feb 2026). |
| **NTT DATA** | Johannesburg 1 | 12 MW ([DataCenterMap](https://www.datacentermap.com/south-africa/johannesburg/ntt-johannesburg-1-data-center/)) | Integrated with the JINX exchange ([NTT](https://services.global.ntt/en-US/newsroom/ntt-data-enhances-johannesburg-1-data-center-with-jinx-integration)). |
| **OADC** (WIOCC) | Isando, plus edge sites JNB2/3, DUR1/2, CPT1–3, BFN1, ELS1, PLZ1 | Isando: 12 MW, Tier III design, 3,000 m² ([OADC](https://openaccessdc.net/isando)) | The 2Africa cable lands into OADC Durban ([SubmarineNetworks](https://www.submarinenetworks.com/en/systems/asia-europe-africa/2africa/wiocc-lands-2africa-cable-into-oadc-durban-data-center)). |
| **Digital Parks Africa** | Samrand | Plan to grow to 22 MVA ([DCD headline](https://www.datacenterdynamics.com/en/news/digital-parks-africa-plans-to-grow-samrand-campus-capacity-to-22mva/)); R500m investment ([Baxtel headline](https://baxtel.com/news/digital-parks-africa-investing-r500-million-us-33-7-million-in-its-samrand-campus-in-south-africa)) | Details not fetched [UNVERIFIED]. |

**Market context.** Planned SA builds of about 1,000 MW could more than double current data-centre power demand ([Daily Maverick, 9 Feb 2026](https://www.dailymaverick.co.za/article/2026-02-09-major-sa-data-centre-expansion-plans-emes-emerge-set-to-more-than-double-current/)). In July 2026, Cape Town approved two hyperscale facilities totalling about 174 MW, and Durban approved exploratory agreements for a roughly 400 MW project ([iAfrica, 14 Aug 2026](https://iafrica.com/equinix-scales-johannesburg-data-centre-to-24mw-but-its-r7-5bn-expansion-land-sits-undeveloped/)).

### 1.2 Facts: pricing indications
- **Retail colocation (Johannesburg, xneelo, accessed 24 Sep 2026):** a full rack costs **R12,675 per month** (47U, 4 kW, 2×32A); a half rack costs R7,470 (2 kW). Both prices include VAT. Metered power is **R4.03/kWh including cooling** ([xneelo](https://xneelo.co.za/colocation/)).
- Wholesale and hyperscale pricing is not published; it is sold through paid market reports ([ResearchAndMarkets via BusinessWire, Oct 2025](https://www.businesswire.com/news/home/20251016838714/en/South-Africa-Colocation-Existing-Upcoming-Data-Center-Portfolio-Analysis-Report-2025-Locations-Existing-and-Upcoming-Floor-Space-IT-Load-Capacity-Future-Additions-Retail-and-Wholesale-Pricing---ResearchAndMarkets.com)).

### 1.3 Facts: power resilience
- **Teraco renewables:**
  - A 120 MW solar PV plant in the Free State, under construction since late 2024, will supply the data centres by wheeling power over the grid ([Teraco, 26 Feb 2025](https://www.teraco.co.za/news/teraco-signs-wind-ppa-to-power-data-centre-facilities-sustainably/); [Teraco, 10 Nov 2025](https://www.teraco.co.za/news/teraco-completes-ct2-data-centre-expansion/)).
  - A wind power purchase agreement with NOA was signed 26 Feb 2025, with first delivery in 2026, also wheeled over existing transmission and distribution networks ([Teraco](https://www.teraco.co.za/news/teraco-signs-wind-ppa-to-power-data-centre-facilities-sustainably/)).
  - Teraco has also announced a grid-capacity allocation for the solar plant ([Teraco headline](https://www.teraco.co.za/news/teraco-secures-grid-capacity-allocation-for-120mw-utility-scale-solar-development-in-south-africa/)).
- **National Policy on Data and Cloud** (Gazette 50741, 31 May 2024), as summarised by [ENS](https://www.ensafrica.com/news/detail/8715/south-africas-new-national-cloud-and-data-pol):
  - data-centre infrastructure used by government must be located in SA;
  - government data centres need at least 99.995% uptime;
  - self-provision of electricity and water is prioritised;
  - the draft's single state-owned data centre was dropped.

### 1.4 Analysis
- **Grid connections, not floor space, are the binding constraint.** The Cape Town appeal turned on missing load-flow studies. Vantage depends on a single utility.
- **The retail premium is large.** A fully drawn 4 kW xneelo rack uses about 2,920 kWh a month, which is about R11.8k of power. Total cost is about **R24k a month**, or roughly R6k per kW-month including VAT. Hyperscale wholesale should be well below this [UNVERIFIED].
- **Build cost.** Equinix's US$10.06/W works out to about **R165m per MW** at R16.4/US$.
- **Wheeled renewables are a real selling point** for ESG-minded buyers. Teraco has them; most competitors do not.

---

## 2. Hyperscalers and sovereign cloud

### 2.1 Facts

| Provider | SA region(s) | Accelerators in SA | Generative AI in SA | Investment |
|---|---|---|---|---|
| **AWS** | af-south-1, Cape Town (2020, 3 availability zones) | Only **G4dn (T4) and Inf1** ([AWS docs, Sep 2026](https://docs.aws.amazon.com/ec2/latest/instancetypes/ec2-instance-regions.html)) | Bedrock launched in Cape Town on 19 Nov 2025 ([AWS](https://aws.amazon.com/about-aws/whats-new/2025/11/amazon-bedrock-available-in-additional-regions/)). Claude Sonnet, Haiku and Opus 4.5 are served by *global* cross-region inference: requests can be routed to any commercial region worldwide, while logs and knowledge bases stay in af-south-1. AWS itself tells customers to check POPIA with their lawyers ([AWS, 30 Jan 2026](https://aws.amazon.com/blogs/machine-learning/scale-ai-in-south-africa-using-amazon-bedrock-global-cross-region-inference-with-anthropic-claude-4-5-models/)). | R15.6bn invested 2018–22, plus up to R30.4bn by 2029: **R46bn in total** ([ITWeb, 14 Apr 2023](https://www.itweb.co.za/article/aws-plans-to-invest-up-to-r304bn-into-its-cloud-infrastructure-in-sa-by-2029/4r1ly7R9yD6vpmda)) |
| **Microsoft Azure** | South Africa North (JHB), South Africa West (CT); generally available since 2019 [year UNVERIFIED] ([Azure blog](https://azure.microsoft.com/en-us/blog/microsoft-opens-first-datacenters-in-africa-with-general-availability-of-microsoft-azure/)) | **ND96isr H100 v5 (8×H100)** listed in SA North ([Northflank](https://northflank.com/cloud/azure/instances/Standard_ND96isr_H100_v5)), about **US$98/hour** list ([NPU Labs, 24 Jun 2026](https://npu-labs.com/articles/the-south-african-gpu-map)); NVads A10 v5 | Microsoft Learn (updated Sep 2026) lists SA North only for *Global* fine-tuning, which offers no data residency ([Learn](https://learn.microsoft.com/en-us/azure/ai-foundry/openai/concepts/models)). I found no regional Standard deployment in SA North ([Learn region tables](https://learn.microsoft.com/en-us/azure/foundry/foundry-models/concepts/models-sold-directly-by-azure-region-availability)). Treat in-region Azure OpenAI as unavailable until the portal confirms otherwise [UNVERIFIED]. | **ZAR 5.4bn by end-2027**, announced 6 Mar 2025, on top of ZAR 20.4bn over the previous three years ([Microsoft](https://news.microsoft.com/source/emea/features/microsoft-invests-zar-5-4bn-in-south-africa/)) |
| **Google Cloud** | africa-south1, Johannesburg (opened Jan 2024 [UNVERIFIED]) | **No deployable GPUs or TPUs** ([gcloud-compute](https://gcloud-compute.com/africa-south1.html); [NPU Labs](https://npu-labs.com/articles/the-south-african-gpu-map)) | Vertex AI generative models in-region [UNVERIFIED] | — |
| **Oracle** | Johannesburg region ([Oracle](https://www.oracle.com/za/cloud/cloud-regions/johannesburg/)); Nairobi region "in execution" with iXAfrica ([Intelligent CISO, 25 Feb 2026](https://www.intelligentciso.com/2026/02/25/ixafrica-data-centres-collaborates-with-oracle-to-deliver-kenyas-first-public-cloud-region/)) | [UNVERIFIED] | [UNVERIFIED] | — |
| **Huawei Cloud** | Johannesburg; three SA data centres ([Connecting Africa, 21 Jun 2024](https://www.connectingafrica.com/cloud-networking/huawei-using-sa-data-centers-to-extend-ai-cloud-adoption)) | Ascend chips [UNVERIFIED in SA] | "AI-native" strategy launched for SA in Jul 2025 ([Huawei](https://www.huaweicloud.com/intl/en-us/news/20250704192254304.html)) | — |

**Sovereign and government cloud**
- **Cassava National Sovereign Cloud** (5 Mar 2026): built with Microsoft Azure and hosted in ADC facilities, aimed at African governments ([Cassava](https://www.cassavatechnologies.com/cassava-technologies-announces-national-sovereign-cloud-to-support-secure-digital-infrastructure-for-african-governments/)).
- **SITA's government private cloud** dates from a 2018 ten-year contract with Gijima, IBM and Huawei ([ITWeb, 16 Nov 2018](https://www.itweb.co.za/article/sita-engages-private-sector-to-power-first-govt-cloud/KWEBb7yaRko7mRjO)).
- A 2026 blog claims SITA's Cloud Framework Agreement 2.0 became mandatory on 1 Apr 2026 under "NT Instruction 2025-11" ([tenders-sa.org](https://www.tenders-sa.org/blog/security-procurement-guide-2026-03-12)). **[UNVERIFIED]** The same blog misstates POPIA s72, which *permits* cross-border transfers under conditions (adequate protection, consent, contract), so treat the source with caution.

**US CLOUD Act exposure.** Asked on 18 Jun 2025 whether he could guarantee that data would not be transmitted to US authorities, Microsoft France's Anton Carniaux said: "No, I cannot guarantee that" ([The Register](https://www.theregister.com/off-prem/2025/07/25/microsoft-exec-admits-it-cannot-guarantee-data-sovereignty/458553)).

### 2.2 Analysis
- **Resident is not sovereign.** Data held in US-owned regions in SA remains compellable under US law. Bedrock's global inference also moves prompts offshore, which triggers POPIA s72 analysis every time.
- **The only in-country hyperscaler GPUs are Azure's H100s in Johannesburg.** Use them for commercial or non-sensitive bursts only.
- **Vigil's differentiator is clear:** in-country, operator-neutral, air-gappable, with no foreign control plane.

---

## 3. AI compute in Africa

### 3.1 Facts
- **Cassava / NVIDIA:**
  - Cassava calls itself the first NVIDIA Cloud Partner in Africa ([Cassava, 8 Sep 2026](https://www.cassava.ai/2026/09/08/launch-of-egypt-first-national-ai-factory/)).
  - Its SA "AI factory" is running; the CSIR and Zindi are named users. Expansion is planned to Nigeria, Kenya, Egypt and Morocco ([Cassava, 18 Mar 2026](https://www.cassava.ai/2026/03/18/cassava-scales-african-ai-infrastructure-with-nvidia-powered-ai-factories-to-accelerate-sovereign-data-capabilities/)).
  - GPU-as-a-Service progress was announced 11 Nov 2025 ([Cassava](https://www.cassava.ai/2025/11/11/forges-ahead-with-gpu-as-a-service/)).
  - Services on offer: bare metal, GPU-as-a-Service, AI-factory-as-a-service, LLM training and model-as-a-service. **No GPU counts or prices are published** (checked 24 Sep 2026, [cassava.ai/ai-compute](https://www.cassava.ai/ai-compute/)).
  - Earlier-reported plan figures (US$720m; about 3,000 GPUs in SA; up to 12,000 across Africa) are **[UNVERIFIED]**.
  - Egypt's national AI factory with Vodafone Business was announced 8 Sep 2026 (link above). Cassava and the Rockefeller Foundation provide compute to NGOs in eight countries ([Cassava](https://www.cassavatechnologies.com/)).
- **Other SA options:** Azure's H100s in SA North (§2). NPU Labs runs clusters of AMD Ryzen AI Max+ 395 mini-PCs (four nodes with 128 GB each for about R260k) ([24 Jun 2026](https://npu-labs.com/articles/the-south-african-gpu-map)).
- **Kenya:** Oracle's Nairobi region is being built with iXAfrica (Feb 2026, link above).
- **Nigeria:** 21st Century Technologies' Ikeja campus (36 MW) is the fourth-largest in Africa; Airtel has a data centre planned ([Nairametrics, Jul 2026](https://nairametrics.com/2026/07/27/africas-data-centre-capacity-exceeds-500-mw-with-890-mw-in-pipeline-report/)). Equinix LG3 (US$22m) was due to open Q1 2026 ([BTA](https://www.businesstechafrica.co.za/technology/2026/04/01/equinix-is-accelerating-its-expansion-plans-in-johannesburg-less-than-two-years-after-its-south-african-debut/)).
- **Egypt:** Khazna is in the pipeline (Nairametrics), alongside the Cassava/Vodafone AI factory.
- **Morocco and Rwanda:** I retrieved no primary 2025–26 sources **[UNVERIFIED]**.
- **Export controls:**
  - The AI Diffusion Rule (15 Jan 2025) did not list SA in Tier 1, which would have placed it under Tier 2 country caps ([Federal Register](https://www.federalregister.gov/documents/2025/01/15/2025-00636/framework-for-artificial-intelligence-diffusion)).
  - BIS rescinded the rule on **13 May 2025**. It issued guidance that using Huawei Ascend chips carries risk, and promised a replacement rule ([BIS](https://www.bis.gov/press-release/department-commerce-rescinds-biden-era-artificial-intelligence-diffusion-rule-strengthens-chip-related)). The replacement's status in Sep 2026 is [UNVERIFIED].

### 3.2 Indicative on-prem GPU costs and power
All ZAR figures use **R16.4/US$** (the x-rates page was timestamped 1 Jan 2026; [x-rates](https://www.x-rates.com/calculator/?from=USD&to=ZAR&amount=1)). Re-price at the live rate and add 15% VAT and distributor margin.

| Configuration | GPU / system power | Indicative price | ZAR |
|---|---|---|---|
| 1× RTX PRO 6000 Blackwell Server Edition (96 GB GDDR7) | Up to 600 W ([NVIDIA](https://www.nvidia.com/en-us/data-center/rtx-pro-6000-blackwell-server-edition/)) | About US$8–10k per card [UNVERIFIED] | R130–165k |
| 4× RTX PRO 6000 server | About 3.2–3.6 kW (analysis) | About US$50–65k [UNVERIFIED] | R0.8–1.1m |
| 8× RTX PRO 6000 server | About 6–7 kW (analysis) | About US$90–130k [UNVERIFIED] | R1.5–2.1m |
| 4× L40S (48 GB) server | About 350 W per GPU [UNVERIFIED] | About US$45–60k [UNVERIFIED] | R0.75–1.0m |
| 8× H100 SXM (HGX/DGX) | 700 W per GPU ([NVIDIA](https://www.nvidia.com/en-us/data-center/h100/)); system about 10 kW [UNVERIFIED] | About US$25k per GPU; up to about US$400k per system ([Modal, Aug 2024](https://modal.com/blog/nvidia-h100-price-article)) | R4–6.5m |
| 8× H200 HGX | About 10–11 kW [UNVERIFIED] | About US$300–450k [UNVERIFIED] | R4.9–7.4m |
| DGX B200 (8× Blackwell, 1,440 GB) | **About 14.3 kW maximum** ([NVIDIA](https://www.nvidia.com/en-us/data-center/dgx-b200/)) | About US$450–600k [UNVERIFIED] | R7.4–9.8m |

**Cloud reference prices** (AWS US regions, 18 Sep 2026, per GPU-hour): H100 (p5) US$6.88; H200 (p5en) US$7.91–9.89; B200 (p6) about US$14.24; L40S (g6e) about US$3.77; RTX PRO 6000 (g7e) about US$4.14 ([Thunder Compute](https://www.thundercompute.com/blog/ec2-gpu-instances)). Azure SA North's H100s work out to about US$12.25 per GPU-hour (NPU Labs).

### 3.3 Analysis
- **Payback is fast if the GPUs stay busy.** Running an Azure ND H100 v5 in SA around the clock costs about US$71.5k (≈R1.17m) a month. An owned 8×H100 at R4–6.5m pays for itself in 4–6 months of continuous use, before operations costs.
- **Power adds up.** A DGX B200 at R4.03/kWh costs about **R42k a month in electricity** (about 10,400 kWh).
- **For Phase Zero, the RTX PRO 6000 is the right tool.** A 4-GPU box is the largest AI node that fits a standard 4 kW retail rack. It is air-cooled, can be shipped for air-gapped deployments, and 96 GB per card is enough for quantised 70–120B open-weight models.
- **Cassava is both a supplier and a competitor.** It could provide GPU capacity, but it is also going after "sovereign AI for government". Partner with Cassava on compute; compete with it on the ontology and applications.

---

## 4. Connectivity and resilience

### 4.1 Facts: subsea cables landing in SA
TeleGeography lists 14 systems for SA (fetched 24 Sep 2026; [country index](https://www.submarinecablemap.com/api/v3/country/south-africa.json)).

| Cable | Ready for service | SA landing(s) | Notes |
|---|---|---|---|
| **2Africa** | 2024 | Yzerfontein, Duynefontein, Gqeberha, Amanzimtoti | 45,000 km. Owners: Meta, Vodafone, Bayobab, Orange, WIOCC, China Mobile, Telecom Egypt, center3 ([TG](https://www.submarinecablemap.com/api/v3/cable/2africa.json)). Design capacity up to 180 Tbps [UNVERIFIED]. |
| **Equiano** | 2023 | Melkbosstrand | Google, 15,000 km ([TG](https://www.submarinecablemap.com/api/v3/cable/equiano.json)). About 144 Tbps [UNVERIFIED]. |
| **T3** | 2023 | Amanzimtoti to Mauritius | Liquid and Mauritius Telecom, 3,200 km ([TG](https://www.submarinecablemap.com/api/v3/cable/t3.json)) |
| **DARE1** | 2021 | Mtunzini | 4,854 km ([TG](https://www.submarinecablemap.com/api/v3/cable/djibouti-africa-regional-express-1-dare-1.json)) |
| **Umoja** | 2028 (planned) | Amanzimtoti to Mandurah, Australia | Google ([TG](https://www.submarinecablemap.com/api/v3/cable/umoja.json)) |
| **Project Waterworth** | Not stated | Amanzimtoti and Cape Town | Meta, 50,000 km ([TG](https://www.submarinecablemap.com/api/v3/cable/project-waterworth.json)) |
| Legacy systems | — | Various | WACS, SAT-3/WASC, SAFE, ACE, EASSy, SEACOM/Tata TGN-Eurasia, METISS, ReuNION (details not fetched) |

### 4.2 Facts: the 2024 cable failures
- **West Africa, 14 Mar 2024:** WACS, MainOne, SAT-3/WASC and ACE were all damaged. Thirteen countries were affected, including SA and Namibia; Vodacom SA saw about six hours of impact ([Cloudflare](https://blog.cloudflare.com/undersea-cable-failures-cause-internet-disruptions-across-africa-march-14-2024/)). The widely reported cause, an undersea landslide in the Trou Sans Fond canyon, is [UNVERIFIED] in the source I read.
- **East Africa, 12 May 2024:** EASSy and Seacom were damaged, hitting Kenya, Uganda, Madagascar, Mozambique, Rwanda, Malawi and Tanzania. Repairs were completed **31 May 2024**. Red Sea cables damaged in February were still unrepaired in July because they lie in a war zone ([Cloudflare Q2 2024](https://blog.cloudflare.com/q2-2024-internet-disruption-summary/)). The location off KwaZulu-Natal is [UNVERIFIED].

**Analysis (lessons):**
- Both coasts failed within 60 days of each other, and repairs took about 3 weeks.
- SA's landings cluster at Amanzimtoti/Mtunzini on the east coast and Melkbosstrand/Yzerfontein/Duynefontein on the west, so a single seabed event can knock out several systems at once.
- For Vigil: keep identity, licensing, model weights and updates **in country**. Peer at NAPAfrica/JINX so domestic traffic stays domestic. Treat offshore APIs as optional extras, never as dependencies.

### 4.3 Facts: terrestrial and mobile
- **Terrestrial backbones** (Openserve, Liquid, DFA/Maziv, Broadband Infraco, FibreCo, SANReN): not researched in this pass **[UNVERIFIED]**.
- **3G and 2G sunset:**
  - The original deadlines (June 2024 for 2G, March 2025 for 3G) were scrapped in July 2025 in favour of a collaborative approach with operators.
  - MTN is retiring 3G area by area; Vodacom had not started as of 13 Jun 2026 ([MyBroadband](https://mybroadband.co.za/news/cellular/653722-mtn-switching-off-3g-in-parts-of-south-africa.html)).
  - Rising memory prices are pushing up handset costs, although 4G phones still sell for R399–R449 ([MyBroadband, 13 Feb 2026](https://mybroadband.co.za/news/cellular/628151-blow-to-south-africas-2g-and-3g-shutdown.html)).
- **Population coverage:** 4G is in the high-90s %, and 5G covers roughly half the population **[UNVERIFIED; the ICASA report could not be retrieved]**.

### 4.4 Facts: satellite
- **Starlink in SA:**
  - The Electronic Communications Act requires individual licensees to be **30% owned by historically disadvantaged groups (HDGs)**.
  - Minister Malatsi issued a **final policy direction on 12 Dec 2025** recognising equity-equivalent investment programmes (EEIPs) as an alternative.
  - On **13 May 2026 ICASA said it cannot give full effect to the direction without amending the ECA**. The ECA Amendment Bill is silent on ownership. Starlink remains unlicensed and has not applied ([TechCentral, 20 Aug 2026](https://techcentral.co.za/spacex-breaks-cover-at-icasa-satellite-spectrum-hearings/285094/)).
  - SpaceX has a local subsidiary and offers free internet for 5,000 rural schools as part of its equity-equivalent proposal. SA is excluded from Starlink's direct-to-cell plans ([MyBroadband, 23 Sep 2026](https://mybroadband.co.za/news/broadband/668575-south-africa-misses-out-on-starlink-mobile.html)).
  - Starlink now enforces a 7-day roaming limit in SA, and the Global Roam unlimited plan ended 17 Aug 2026 ([MyBroadband, 20 Jul 2026](https://mybroadband.co.za/news/broadband/658488-starlink-roaming-crackdown-in-south-africa.html)).
- **Starlink in SADC:** licensed in Botswana, Eswatini, Lesotho, Mozambique and Zimbabwe. Namibia has rejected it twice (MyBroadband, 23 Sep 2026).
- **Amazon Leo** (formerly Kuiper): distributed through Herotel, which holds the licences, under the "Evry" brand from 2027. Vodacom has a backhaul partnership, and Cell C is talking to both Starlink and Amazon ([TechCentral, 23 Aug 2026](https://techcentral.co.za/cell-c-in-talks-with-starlink-and-amazon-leo/285187/)).
- **OneWeb in SA:** [UNVERIFIED].

### 4.5 Facts: load-shedding record, 2024–2026
- Load-shedding was suspended on 26 Mar 2024. It returned at Stage 3 from 31 Jan to 2 Feb 2025, and reached Stage 6 without warning on 22 Feb 2025 ([Wikipedia](https://en.wikipedia.org/wiki/South_African_energy_crisis)).
- By 4 Sep 2026 there had been **476 consecutive days without load-shedding since 16 May 2025** ([Eskom](https://www.eskom.co.za/eskom-fully-meets-2026-winter-demand-eaf-remains-at-its-highest-level-since-2020-67-79-and-diesel-expenditure-declines-by-r4-84-billion-or-81-64-to-deliver-energy-security/)). The same statement reports:
  - EAF (1 Apr to 3 Sep 2026) of 67.79%, up from 61.3% a year earlier;
  - unplanned outages (UCLF) at 19.47%;
  - diesel spend of R1.08bn, down from R5.93bn;
  - load reduction still affecting 6.5% of customers, with a target of eliminating it by March 2027.
- A later headline reports EAF at 67.96% and load reduction affecting **3.4% of customers** ([Eskom, 18 Sep 2026](https://www.eskom.co.za/eaf-reaches-67-96-diesel-expenditure-falls-by-r4-8-billion-emissions-outperform-target-and-load-reduction-declines-to-just-3-4-of-customers/)).

**Analysis (edge deployments):**
- The national problem is now localised "load reduction" in high-loss areas, plus theft and vandalism, which Eskom itself names as continuing challenges.
- Edge kits for municipal depots or townships should carry **8–12 hours of battery plus solar**, and should use EskomSePush's load-reduction schedules as a planning input.
- LEO backhaul is legal in Mozambique, Botswana and Zimbabwe but not yet in SA. Design SA edge nodes around fibre and 4G.

---

## 5. Phase-Zero public data catalogue

Columns: name | publisher | URL/API endpoint | format | update frequency | licence/terms | auth needed | usefulness for Vigil.

**A. Procurement, corporate and legal**

| Name | Publisher | URL / endpoint | Format | Update | Licence / terms | Auth | Usefulness |
|---|---|---|---|---|---|---|---|
| eTenders OCDS API | National Treasury (OCPO) | [ocds-api.etenders.gov.za/swagger](https://ocds-api.etenders.gov.za/swagger/index.html); bulk copy at [OCP registry #143](https://data.open-contracting.org/en/publication/143) | OCDS JSON; bulk JSONL/CSV/XLSX | Continuous; registry snapshot covers Feb 2017–Sep 2026 | **PDDL** (per the OCP registry) | None for reading [UNVERIFIED] | **Core.** Known gaps: duplicate IDs; the `parties` field is missing for most processes; 35,309 tenders but only 1,691 contracts; municipalities and SOEs excluded unless they volunteer |
| Central Supplier Database | National Treasury | [secure.csd.gov.za](https://secure.csd.gov.za/) | Web application | Live | [Terms](https://secure.csd.gov.za/Home/Terms): only organs of state and suppliers may access it; data may be used for procurement only | Registered users | No public bulk access. 1,448,293 verified suppliers (site counter, 24 Sep 2026). Use CSD numbers only as join keys, or seek an MoU |
| Restricted Suppliers and Tender Defaulters | National Treasury (OCPO) | [RestrictedSuppliersReport.pdf](https://ocpo.treasury.gov.za/RestrictedSupplier/RestrictedSuppliersReport.pdf) | PDF (35 pages, about 409 entries; generated 15 Sep 2026) | Report regenerated periodically | Not stated | None | **High-value watch list.** Fields: name, registration/ID number, reason (fraud, collusion, non-delivery), period, authorising body (Eskom, Transnet, City of Cape Town, SARS…). Contains natural-person ID numbers, so it needs POPIA controls |
| CIPC company and beneficial-ownership data | CIPC | [cipc.co.za](https://www.cipc.co.za/) | Search, certificates, electronic data extracts | On demand | Electronic disclosure certificate R30; data extracts **R10 plus R0.04 per record** | CIPC account [UNVERIFIED] | Director links. Access to the beneficial-ownership register is limited to competent authorities [UNVERIFIED] |
| SAFLII | SAFLII | [saflii.org](https://www.saflii.org/) | HTML | Daily | **Robots rules block all automated agents from case law; no scraping, bulk querying or mirroring without a signed written agreement** ([terms](https://www.saflii.org/content/terms.html)) | — | Do not crawl it. Negotiate a written agreement |
| LawLibrary (Laws.Africa) | Laws.Africa | [lawlibrary.org.za](https://lawlibrary.org.za/) | HTML; APIs | Daily | CC BY-NC 4.0; scraping and bulk download prohibited; some collections (case law, gazettes) more permissive ([terms](https://lawlibrary.org.za/terms-of-use/)) | API [UNVERIFIED] | Best route to judgments and gazettes; commercial use needs permission |
| Government Gazette | Government Printing Works | www.gpwonline.gov.za (as cited by the [MDB](https://www.demarcation.org.za/)) | PDF | Weekly or more often | [UNVERIFIED] | None | Regulations, ward delimitations, notices |
| AGSA PFMA/MFMA reports | Auditor-General | agsa.co.za [UNVERIFIED path] | PDF | Annual | [UNVERIFIED] | None | Irregular-expenditure context by auditee |
| City of Cape Town tender and RFQ awards | City of Cape Town | [Tender Awards 2021–2026](https://services6.arcgis.com/nyYfO9SxHU2ChQd9/arcgis/rest/services/Tender_Awards_2021_to_2026/FeatureServer); [RFQ Awards](https://services6.arcgis.com/nyYfO9SxHU2ChQd9/arcgis/rest/services/Requests_for_Quotations_Awarded_2020_to_April_2026/FeatureServer) ([catalogue](https://odp-cctegis.opendata.arcgis.com/api/search/v1/collections/dataset/items?q=service%20requests)) | ArcGIS REST, CSV | About monthly (latest Apr–Aug 2026) | City's own terms, data "as is" | None | Municipal awards that are missing from OCDS. RFQ threshold raised to R750k on 14 Dec 2023, which allows threshold-splitting tests |

**B. Public finance and statistics**

| Name | Publisher | URL / endpoint | Format | Update | Licence / terms | Auth | Usefulness |
|---|---|---|---|---|---|---|---|
| Municipal Money API | National Treasury + OpenUp | [municipaldata.treasury.gov.za/api](https://municipaldata.treasury.gov.za/docs) | JSON; CSV export | **Quarterly** (2026 Q2 loaded, still being verified) | Commercial reuse allowed with attribution to National Treasury and the publication date ([terms](https://municipalmoney.gov.za/terms)) | None [UNVERIFIED] | Datasets: income and expenditure, capital, cash flow, aged debtors, repairs and maintenance, unauthorised/irregular/fruitless expenditure, audit opinions, grants, officials |
| Vulekamali | National Treasury + Imali Yethu | [vulekamali.gov.za](https://vulekamali.gov.za/); data portal at data.vulekamali.gov.za | CSV/XLSX | Budget cycle (2026-27 loaded) | Not stated | None | Budgets and infrastructure projects |
| Stats SA Census 2022 | Stats SA | [census.statssa.gov.za](https://census.statssa.gov.za/) | JavaScript web app | Static | [UNVERIFIED] | None | Ward and municipal denominators [UNVERIFIED] |
| Wazimap | OpenUp | [wazimap.com](https://wazimap.com/) | Open-source web GIS | Maintained; "Census Wazi" prototype | Free [UNVERIFIED per dataset] | None | Profiles and reusable code |
| SAPS crime statistics | SAPS | [saps.gov.za/services/crimestats.php](https://www.saps.gov.za/services/crimestats.php) | PDF/Excel [UNVERIFIED] | Quarterly (2025–26 release calendar) | [UNVERIFIED] | None | Station-level baselines [UNVERIFIED] |
| SARB Web API | SARB | [custom.resbank.co.za/SarbWebApi/Help](https://custom.resbank.co.za/SarbWebApi/Help) | REST | Daily/monthly | Not stated | Not stated | FX and macro series (`GetTimeseriesObservations/{code}`) |
| SARS trade statistics | SARS | tools.sars.gov.za/tradestatsportal (timed out) | [UNVERIFIED] | Monthly [UNVERIFIED] | [UNVERIFIED] | — | Corridor commodity flows |

**C. Energy and water**

| Name | Publisher | URL / endpoint | Format | Update | Licence / terms | Auth | Usefulness |
|---|---|---|---|---|---|---|---|
| Eskom Data Portal | Eskom | [eskom.co.za/dataportal](https://www.eskom.co.za/dataportal/) | CSV, emailed via a request form | Hourly/weekly; up to 5 years of history | Terms-of-use and disclaimer | Request form | Demand, generation by source, gas turbine use, renewables, unplanned outages, emissions |
| EskomSePush API | EskomSePush | [gumroad listing](https://eskomsepush.gumroad.com/l/api); [v2 docs](https://documenter.getpostman.com/view/1296288/UzQuNk3E) | JSON (v3, v3.1 beta) | Near-real-time | API licence agreement; quota per token; one IP per token | `Token` header | Area schedules for load-shedding **and load reduction**. Listed free on Gumroad; paid tiers [UNVERIFIED] |
| NERSA | NERSA | nersa.org.za | PDF | Ad hoc | [UNVERIFIED] | None | Tariffs and wheeling decisions [not fetched] |
| DWS hydrology and dams | Dept of Water and Sanitation | [Verified data](https://www.dws.gov.za/Hydrology/Verified/hymain.aspx); weekly reservoirs page ([Province.aspx](https://www.dws.gov.za/Hydrology/Weekly/Province.aspx), returned 403) | HTML tables [UNVERIFIED] | Weekly for reservoirs [UNVERIFIED] | [UNVERIFIED] | None | River, reservoir, canal, weather and tidal stations, by drainage region |
| Blue / Green / No Drop | Dept of Water and Sanitation | IRIS at ws.dws.gov.za/iris (unreachable) | PDF [UNVERIFIED] | Multi-year cycle | [UNVERIFIED] | — | Municipal water compliance scores |

**D. Municipal**

| Name | Publisher | URL / endpoint | Format | Update | Licence / terms | Auth | Usefulness |
|---|---|---|---|---|---|---|---|
| City of Cape Town service requests | City of Cape Town | [Service Requests FeatureServer](https://services6.arcgis.com/nyYfO9SxHU2ChQd9/arcgis/rest/services/Service_Requests_2023_until_20_May_2026/FeatureServer) (records from 2023 to 23 Sep 2026; historic data 2011–2024) | ArcGIS REST, CSV | Current to 23 Sep 2026 (cadence [UNVERIFIED]) | City's own terms, "as is"; commercial use not addressed ([terms](https://www.capetown.gov.za/General/Terms-of-use-open-data)) | None | **The best open municipal operations feed in SA** (from the City's SAP C3 notifications system) |
| Johannesburg, eThekwini, Tshwane open data | Metros | eThekwini's portal returned 503; Tshwane had a TLS error; no Johannesburg portal found | — | — | — | — | **Gap**, which limits replicating a Cape Town prototype elsewhere |
| Municipal Demarcation Board boundaries | MDB | [demarcation.org.za](https://www.demarcation.org.za/) (Spatial Knowledge Hub) | Shapefile, web apps | Final wards gazetted from 15 Dec 2025 for the 2026/27 local elections | Not stated | None | The ward and municipal polygons behind the `Place` object type |

**E. Maritime, aviation and logistics**

| Name | Publisher | URL / endpoint | Format | Update | Licence / terms | Auth | Usefulness |
|---|---|---|---|---|---|---|---|
| TNPA port statistics | Transnet National Ports Authority | transnetnationalportsauthority.net (timed out) | Excel/PDF [UNVERIFIED] | Monthly [UNVERIFIED] | [UNVERIFIED] | — | Calibrating AIS-derived estimates |
| aisstream.io | aisstream | `wss://stream.aisstream.io/v0/stream` ([docs](https://aisstream.io/documentation)) | WebSocket JSON | Real-time | Free; no SLA; no replay; commercial terms unstated ([home](https://aisstream.io/)) | API key, used server-side | Live vessel positions at Durban; up to 3 connections; coverage from shore-based receivers only |
| Global Fishing Watch API | GFW | [docs](https://globalfishingwatch.org/our-apis/documentation) | JSON | [UNVERIFIED] | **Non-commercial only** | Token | Port visits, encounters, satellite-radar vessel detections; demos only |
| AISHub | AISHub | [join-us](https://www.aishub.net/join-us) | [UNVERIFIED] | Real-time | Reciprocal: members must contribute a raw NMEA feed (at least 10 vessels, 90% uptime) | Contributor account | Put up a Durban receiver and join |
| MarineTraffic | Kpler | [marinetraffic.com/en/ais-api-services](https://www.marinetraffic.com/en/ais-api-services) (now redirects to Kpler) | API | Real-time | Commercial | Paid | Satellite AIS and port calls, later |
| OpenSky | OpenSky Network | opensky-network.org (timed out) | REST | Real-time | Non-commercial [UNVERIFIED] | Account | Aviation context |
| adsb.lol | adsb.lol | [adsb.lol](https://www.adsb.lol/) | REST plus daily archive | Real-time | Contributed data is CC0 ([licence](https://www.adsb.lol/privacy-license/)); commercial terms not stated | None [UNVERIFIED] | Air-freight movements |
| Border delays | FESARTA; TradeMark Africa | fesarta.org; trademarkafrica.com | [UNVERIFIED] | [UNVERIFIED] | [UNVERIFIED] | — | Beitbridge and Lebombo waiting times; probably needs a partnership |

**F. Weather, climate and earth observation**

| Name | Publisher | URL / endpoint | Format | Update | Licence / terms | Auth | Usefulness |
|---|---|---|---|---|---|---|---|
| Open-Meteo | Open-Meteo | [open-meteo.com](https://open-meteo.com/en/terms) | JSON | Hourly | Free tier (under 10k calls a day) is **non-commercial only**. Paid plans cover 1M, 5M or 50M+ calls a month ([pricing](https://open-meteo.com/en/pricing)). Data is CC BY 4.0; the server is AGPLv3 and can be self-hosted | Key on paid plans | Forecasts and history |
| SAWS | SA Weather Service | weathersa.co.za | [UNVERIFIED] | [UNVERIFIED] | Commercial data products [UNVERIFIED] | — | Official warnings |
| ERA5 | Copernicus C3S | [cds.climate.copernicus.eu](https://cds.climate.copernicus.eu/licences) | GRIB/NetCDF | Daily, a few days behind [UNVERIFIED] | CC-BY or the Copernicus products licence | Free account | Climate baselines |
| NASA FIRMS | NASA | [FIRMS API](https://firms.modaps.eosdis.nasa.gov/api/) (v3.7.14) | CSV/KML | Near-real-time, about 3 hours [UNVERIFIED] | Open [UNVERIFIED] | `MAP_KEY` | Fires near power lines and plantations |
| Digital Earth Africa | Digital Earth Africa | [services](https://www.digitalearthafrica.org/platform-resources/services) | OGC services, sandbox | Varies by product | [UNVERIFIED] | Free sandbox account | Surface water, 700k+ water bodies, water quality, cropland, coastlines, vegetation anomalies, analysis-ready Landsat and Sentinel |
| Copernicus Data Space Ecosystem | ESA / EU | [dataspace.copernicus.eu](https://dataspace.copernicus.eu/) | OData, STAC, Sentinel Hub, openEO | Per satellite pass | Sentinel data is free, full and open. Free quotas: 10k Sentinel Hub processing units and 10k requests a month; downloads of 4×20 MB/s up to 12 TB per 30 days; 10k openEO credits a month ([quotas](https://documentation.dataspace.copernicus.eu/Quotas.html)) | Free account | **Sentinel-1 radar** for detecting ships without AIS ("dark" vessels) at Durban anchorage |
| Global Forest Watch | WRI | globalforestwatch.org | API | [UNVERIFIED] | [UNVERIFIED] | Key [UNVERIFIED] | Low priority in SA |

**G. Geospatial base layers**

| Name | Publisher | URL / endpoint | Format | Update | Licence / terms | Auth | Usefulness |
|---|---|---|---|---|---|---|---|
| Google Open Buildings v3 | Google | [open-buildings](https://sites.research.google/gr/open-buildings/) | CSV, Earth Engine, BigQuery | Static (2023 model); temporal layer covers 2016–2023 | **Choose CC BY 4.0 or ODbL** | None | 1.8bn building footprints, including informal settlements |
| Overture Maps | Overture Maps Foundation | overturemaps.org | GeoParquet | Regular releases [UNVERIFIED] | Buildings, base, divisions and transportation are ODbL; places are CDLA-Permissive 2.0 ([attribution](https://docs.overturemaps.org/attribution/)) | None | Points of interest (places) under a permissive licence |
| OpenStreetMap | OSM Foundation | openstreetmap.org | PBF | Continuous | ODbL 1.0: attribution, plus share-alike for derivative databases ([OSMF](https://osmfoundation.org/wiki/Licence)) | None | Roads, rail, power lines |

**H. Events, conflict, humanitarian and air quality**

| Name | Publisher | URL / endpoint | Format | Update | Licence / terms | Auth | Usefulness |
|---|---|---|---|---|---|---|---|
| GDELT | GDELT Project | gdeltproject.org | CSV/JSON | About every 15 minutes [UNVERIFIED] | Free for academic, commercial and government use; citation plus link required ([about](https://www.gdeltproject.org/about.html)) | None | Seed for extracting news events about infrastructure crime |
| ACLED | ACLED | [EULA](https://acleddata.com/eula/) | API | Weekly [UNVERIFIED] | EULA of 8 Jul 2025: **commercial entities need a corporate licence**; no competing products; no scraping; no redistribution | Account | Budget for a licence |
| HDX / HAPI | UN OCHA | data.humdata.org (HAPI docs returned 403) | CKAN API | Varies | Per dataset [UNVERIFIED] | [UNVERIFIED] | Humanitarian context for SADC neighbours |
| FEWS NET | FEWS NET | fews.net (rate-limited, not read) | [UNVERIFIED] | [UNVERIFIED] | [UNVERIFIED] | — | Food-security outlooks |
| OpenAQ | OpenAQ | [docs](https://docs.openaq.org/about/about) | REST v3 | Hourly [UNVERIFIED] | Public data, subject to third-party terms | API key | Air-quality layer |
| World Bank, AfDB, UN Comtrade | WB, AfDB, UN | api.worldbank.org; comtradeplus.un.org (docs rate-limited) | JSON/CSV | Annual/monthly | World Bank CC BY 4.0 [UNVERIFIED]; Comtrade has free and premium tiers [UNVERIFIED] | Comtrade key | Macro context |

**I. Field-operations channels (write-back)**

| Name | Publisher | URL / endpoint | Format | Update | Licence / terms | Auth | Usefulness |
|---|---|---|---|---|---|---|---|
| Africa's Talking | Africa's Talking | africastalking.com (pricing page rate-limited) | REST | Real-time | SA SMS/USSD/voice pricing [UNVERIFIED] | API key | USSD and SMS still matter while the 3G sunset is slow |
| WhatsApp Business Cloud API | Meta | developers.facebook.com/docs/whatsapp (pricing page rate-limited) | REST, webhooks | Real-time | Per-message pricing since 1 Jul 2025 [UNVERIFIED]. SA indicative prices: about US$0.038 per marketing message and about US$0.008 per utility or authentication message; service replies free within the 24-hour window [all UNVERIFIED] | Meta app plus a business solution provider | Alerts and task hand-offs to crews, councillors and drivers |

**Analysis: licensing traps to design around**
1. **ODbL share-alike.** Keep databases derived from OSM or Overture separate from Vigil's proprietary data, and take Open Buildings under CC BY 4.0 rather than ODbL.
2. **Non-commercial sources.** Global Fishing Watch, OpenSky and Open-Meteo's free tier cannot be used in a product without a paid or negotiated licence.
3. **Sources closed to automated or public access.** SAFLII blocks automated access and the CSD is closed. Both need written agreements.
4. **POPIA exposure.** Director data from CIPC and ID numbers in the restricted-supplier list are personal information.

---

## 6. Phase-Zero "live digital twin" prototypes (ranked)

*The demo narratives below are **illustrative scripts**. The entities and patterns in them are hypothetical placeholders, not findings about real suppliers or officials.*

### #1 Procurement-integrity graph ("Tender Sentinel")
- **Ontology:**
  - Object types: `OrganOfState`, `Tender`, `Award`, `Contract`, `Supplier` (CSD and CIPC numbers), `Person` (director), `Identifier` (address, phone, email, bank reference), `Restriction`, `CourtMatter`, `AuditFinding`, `Place` (MDB boundaries).
  - Links: `issues`, `awarded_to`, `director_of`, `shares_identifier_with`, `restricted_by`, `party_to`, `audited_in`, `located_in`.
  - Derived flags: threshold proximity, deviation or limited bidding, speed of award, overlap of directors between suppliers.
- **Data and cadence:**
  - eTenders OCDS: daily delta.
  - Restricted-supplier PDF: re-pulled weekly.
  - City of Cape Town tender and RFQ awards: monthly.
  - CIPC extracts: bought on demand. At R0.04 per record, 2 million records cost about **R80k** (analysis).
  - LawLibrary or SAFLII: only under an agreement.
  - Municipal Money irregular-expenditure data: quarterly. AGSA reports: annual.
- **Decision improved:** which awards get a second look before contract signature or payment, and ongoing supplier due diligence.
- **Three-minute demo:**
  1. *(0:00)* Yesterday's awards stream onto a map of organs of state.
  2. *(0:30)* Click an award. The graph expands to its supplier, then its CIPC directors. One director also sits on a supplier on the restricted list (restricted by Transnet for fraud), so the award turns red.
  3. *(1:15)* A pattern view shows Cape Town RFQ awards just under the R750k threshold going to three suppliers that share a director, all within 30 days.
  4. *(2:00)* A locally hosted LLM drafts a referral pack in which every sentence is footnoted to a source record.
  5. *(2:40)* Write-back: the analyst clicks "Refer". A case is opened, the supplier is put on a watch list, and the CFO gets a WhatsApp alert.
- **Who it would impress:** National Treasury OCPO, AGSA, SIU, IDAC, provincial treasuries, forensic units in the metros, and financial-crime teams in banks.
- **Risks:** Defamation, so present results as risk indicators rather than findings. POPIA. Gaps in the OCDS `parties` data. No access to the CSD.

### #2 Durban port and corridor twin
- **Ontology:**
  - Object types: `Vessel` (MMSI/IMO), `PortCall`, `Anchorage/Berth` (geofences), `Terminal`, `SatelliteDetection` (Sentinel-1), `WeatherCell`, `Corridor` segment (N3 road, rail), `BorderPost`, `GridArea` (EskomSePush), `Incident` (from news).
  - Links: `at_anchorage`, `calls_at`, `affected_by`, `detected_without_AIS`, `serves_corridor`.
- **Data and cadence:**
  - aisstream: seconds.
  - Sentinel-1 via the Copernicus Data Space: each satellite pass.
  - Open-Meteo on a commercial plan: hourly.
  - TNPA statistics: monthly (for calibration).
  - EskomSePush load reduction: live.
  - GDELT incidents: about every 15 minutes.
  - Border delays: only if there is a partnership.
  - SARB exchange rates: daily.
  - Also install a Vigil AIS receiver in Durban and join AISHub.
- **Decision improved:** forecasting anchorage waiting times, anticipating wind-related stoppages, rebooking truck slots, and flagging vessels that are not broadcasting AIS.
- **Three-minute demo:**
  1. *(0:00)* A live anchorage view shows vessels waiting against their 30-day wait-time distribution.
  2. *(0:45)* A high-wind forecast for tomorrow afternoon projects crane stoppages, and the predicted estimated arrival times shift.
  3. *(1:30)* A Sentinel-1 pass shows radar detections with no matching AIS signal, flagged as "dark".
  4. *(2:00)* A protest on the N3, extracted from news, raises the risk rating for that corridor segment.
  5. *(2:30)* Write-back: revised slot advisories go out by WhatsApp to transport operators, and a CSV goes to terminal planners.
- **Who it would impress:** Transnet (TNPA and the terminal operator TPT), shipping lines, freight forwarders, SARS Customs, major exporters, and KZN provincial government.
- **Risks:** aisstream's commercial terms are unclear; AIS coverage offshore relies on shore receivers; TNPA publishes only monthly.

### #3 Cape Town municipal service-delivery twin
- **Ontology:**
  - Object types: `ServiceRequest` (category, status, timestamps), `Ward`/`Suburb`, `InferredAsset` (pipe, feeder, streetlight clusters), `OutageSchedule`, `Dam`, `WeatherEvent`, `BudgetLine` (repairs and maintenance, capital), `AuditOpinion`, `Building` (Open Buildings, for population estimates).
  - Links: `located_in`, `caused_by?` (a hypothesis link), `funded_by`, `affects_population`.
- **Data and cadence:**
  - City of Cape Town service requests: current to 23 Sep 2026.
  - DWS dam levels: weekly.
  - Weather: hourly.
  - EskomSePush: live.
  - Municipal Money: quarterly.
  - MDB wards and Census/Wazimap denominators: static.
- **Decision improved:** triaging the backlog, positioning crews before storms, and targeting maintenance spend where requests keep recurring.
- **Three-minute demo:**
  1. *(0:00)* A ward heatmap of open requests by category, with how long each has been open.
  2. *(0:40)* A rainfall forecast is overlaid on the history of sewer and stormwater requests after past heavy rain. Compute this lift from the data; do not assert it in advance.
  3. *(1:20)* Load-reduction schedules are shown against electricity fault tickets.
  4. *(1:50)* Repairs-and-maintenance spend from Municipal Money is set against the backlog.
  5. *(2:30)* Write-back: a prioritised list of work orders is generated, and ward councillors are notified by WhatsApp.
- **Who it would impress:** City of Cape Town directorates, National Treasury's Cities Support Programme, SALGA, COGTA/MISA, and insurers.
- **Risks:** Only Cape Town publishes this kind of data. Request statuses are point-in-time snapshots. No crew data is available.

### Evaluated but not recommended as a standalone: grid and infrastructure-crime twin
Eskom's portal data is national and delivered by emailed CSV. Signals of infrastructure crime would have to be extracted from news: GDELT is free, ACLED needs a licence, and SAPS publishes only quarterly. Load-shedding has been absent since 16 May 2025, although Eskom still names illegal connections, meter tampering and infrastructure damage as challenges ([4 Sep 2026](https://www.eskom.co.za/eskom-fully-meets-2026-winter-demand-eaf-remains-at-its-highest-level-since-2020-67-79-and-diesel-expenditure-declines-by-r4-84-billion-or-81-64-to-deliver-energy-security/)). **Build it as a layer inside #2 and #3** (corridor incidents, feeder faults, FIRMS fires near power lines).

### Ranking (analysis)

| Rank | Prototype | Data openness | How live | Visual impact | Buyer urgency | Weeks to demo |
|---|---|---|---|---|---|---|
| 1 | Procurement integrity | High (OCDS is PDDL) | Daily | Medium | **Very high** | 3–4 (founders already use OCDS) |
| 2 | Durban port and corridor | Medium (AIS terms) | **Seconds** | **Very high** | High | 5–6 |
| 3 | Cape Town municipal | High, but Cape Town only | Daily to weekly | High | Medium | 4–5 |
| 4 | Grid and infrastructure crime | Low | Hourly to quarterly | Medium | Medium | 6–8 |

---

## Implications for Vigil

1. **Host sovereign deployments in two SA colocation operators across two metros.**
   - Primary: Teraco Isando or Bredell (189 MW portfolio, NAPAfrica peering, wheeled renewables).
   - Disaster recovery: Teraco CT2 or Equinix JN1 (built for liquid cooling).
   - Durban edge node: OADC DUR1 (where 2Africa lands) or Teraco DB1, for the port twin.
   - Do not put the sovereign tier in AWS, Azure or GCP (CLOUD Act exposure plus global inference routing). Offer a separate "hyperscaler-connected" tier for commercial clients.
2. **First compute:**
   - One 4× RTX PRO 6000 Blackwell server (about R0.8–1.1m [UNVERIFIED]) in a standard 4 kW rack.
   - Plus a portable 1–2 GPU kit for air-gapped demonstrations.
   - Burst fine-tuning to Azure SA North H100s (about US$98 an hour) or Cassava GPU-as-a-Service, under in-country data-processing agreements.
   - **Buy no H100 or B200 until a contract justifies it.** At that point, lease high-density space at JN1 or Teraco rather than retrofitting a retail rack.
3. **Make "no prompt leaves SA" a product guarantee:** local open-weight models, cross-region inference disabled, and a per-object ledger of licences and POPIA lawful basis in the ontology.
4. **Design for disconnection.** The 2024 cable failures, Starlink's absence and ongoing load reduction all point the same way:
   - in-country control plane and model registry;
   - offline map tiles, with ODbL attribution and data kept separate;
   - store-and-forward synchronisation;
   - SMS, USSD and WhatsApp fallbacks.
   - Edge kits should carry 8–12 hours of autonomy. Use LEO satellite links only outside SA (Botswana, Mozambique, Zimbabwe…) until ICASA and the ECA change; Amazon Leo via Herotel arrives in 2027.
5. **Line up data rights early.**
   - Seek an MoU with National Treasury's OCPO for CSD access, and feed OCDS data-quality problems back to them.
   - Negotiate written agreements with SAFLII and LawLibrary.
   - Buy an ACLED corporate licence only when a buyer needs it.
   - Use a commercial Open-Meteo plan, or ECMWF open data [UNVERIFIED], for weather.
   - Put up a Durban AIS receiver and join AISHub.
6. **Lead the go-to-market with procurement integrity.** It is the fastest demo and the most urgent problem for buyers. Use the port twin as the showpiece for Transnet and the private sector, and the Cape Town twin as the template for the metros.
7. **Government procurement path:** SITA approval for IT procurement by organs of state [UNVERIFIED details], plus the 2024 data policy's requirement for in-country data centres. Vigil's in-country design is an advantage here, but start SITA engagement in Phase Zero.
8. **Treat Cassava as a partner on compute and a rival in government deals.** It has NVIDIA partner status, ADC facilities and an Azure-based sovereign cloud. Vigil's advantage has to be the ontology, write-back and workflow applications, not the infrastructure.
9. **Package deployments for pan-African colocation:** Oracle Nairobi, Equinix LG3 in Lagos, and Cassava's AI factories in Egypt, Kenya and Nigeria. Plan for export-control scrutiny of large GPU orders, and avoid Huawei Ascend for any client with US exposure, given BIS's May 2025 guidance.

---

## Sources

**Data centres:** https://www.teraco.co.za/news/teraco-completes-ct2-data-centre-expansion/ · https://www.teraco.co.za/news/ · https://www.teraco.co.za/news/teraco-signs-wind-ppa-to-power-data-centre-facilities-sustainably/ · https://www.teraco.co.za/news/teraco-secures-grid-capacity-allocation-for-120mw-utility-scale-solar-development-in-south-africa/ · https://www.itweb.co.za/article/teraco-announces-jb7-a-40mw-data-centre-expansion-to-its-isando-campus-and-new-r8bn-syndicated-loan/8OKdWqDXdXxqbznQ · https://www.businesstechafrica.co.za/technology/2026/04/01/equinix-is-accelerating-its-expansion-plans-in-johannesburg-less-than-two-years-after-its-south-african-debut/ · https://iafrica.com/equinix-scales-johannesburg-data-centre-to-24mw-but-its-r7-5bn-expansion-land-sits-undeveloped/ · https://www.dailymaverick.co.za/article/2026-09-20-data-centre-deal-cape-town-forced-back-to-the-drawing-board/ · https://www.dailymaverick.co.za/article/2026-02-09-major-sa-data-centre-expansion-plans-emes-emerge-set-to-more-than-double-current/ · https://vantage-dc.com/data-center-locations/emea/johannesburg-i-south-africa/ · https://www.businesswire.com/news/home/20221213005272/en/Vantage-Data-Centers-Announces-Second-Campus-in-Johannesburg · https://capacityglobal.com/news/cassava-africa-data-centres-acquisition/ · https://www.datacentermap.com/south-africa/johannesburg/ntt-johannesburg-1-data-center/ · https://services.global.ntt/en-US/newsroom/ntt-data-enhances-johannesburg-1-data-center-with-jinx-integration · https://openaccessdc.net/isando · https://www.submarinenetworks.com/en/systems/asia-europe-africa/2africa/wiocc-lands-2africa-cable-into-oadc-durban-data-center · https://www.datacenterdynamics.com/en/news/digital-parks-africa-plans-to-grow-samrand-campus-capacity-to-22mva/ · https://baxtel.com/news/digital-parks-africa-investing-r500-million-us-33-7-million-in-its-samrand-campus-in-south-africa · https://nairametrics.com/2026/07/27/africas-data-centre-capacity-exceeds-500-mw-with-890-mw-in-pipeline-report/ · https://xneelo.co.za/colocation/ · https://www.businesswire.com/news/home/20251016838714/en/South-Africa-Colocation-Existing-Upcoming-Data-Center-Portfolio-Analysis-Report-2025-Locations-Existing-and-Upcoming-Floor-Space-IT-Load-Capacity-Future-Additions-Retail-and-Wholesale-Pricing---ResearchAndMarkets.com · https://www.ensafrica.com/news/detail/8715/south-africas-new-national-cloud-and-data-pol

**Cloud and sovereignty:** https://www.itweb.co.za/article/aws-plans-to-invest-up-to-r304bn-into-its-cloud-infrastructure-in-sa-by-2029/4r1ly7R9yD6vpmda · https://docs.aws.amazon.com/ec2/latest/instancetypes/ec2-instance-regions.html · https://aws.amazon.com/about-aws/whats-new/2025/11/amazon-bedrock-available-in-additional-regions/ · https://aws.amazon.com/blogs/machine-learning/scale-ai-in-south-africa-using-amazon-bedrock-global-cross-region-inference-with-anthropic-claude-4-5-models/ · https://news.microsoft.com/source/emea/features/microsoft-invests-zar-5-4bn-in-south-africa/ · https://azure.microsoft.com/en-us/blog/microsoft-opens-first-datacenters-in-africa-with-general-availability-of-microsoft-azure/ · https://northflank.com/cloud/azure/instances/Standard_ND96isr_H100_v5 · https://npu-labs.com/articles/the-south-african-gpu-map · https://learn.microsoft.com/en-us/azure/ai-foundry/openai/concepts/models · https://learn.microsoft.com/en-us/azure/foundry/foundry-models/concepts/models-sold-directly-by-azure-region-availability · https://gcloud-compute.com/africa-south1.html · https://www.oracle.com/za/cloud/cloud-regions/johannesburg/ · https://www.intelligentciso.com/2026/02/25/ixafrica-data-centres-collaborates-with-oracle-to-deliver-kenyas-first-public-cloud-region/ · https://www.connectingafrica.com/cloud-networking/huawei-using-sa-data-centers-to-extend-ai-cloud-adoption · https://www.huaweicloud.com/intl/en-us/news/20250704192254304.html · https://www.cassavatechnologies.com/cassava-technologies-announces-national-sovereign-cloud-to-support-secure-digital-infrastructure-for-african-governments/ · https://www.itweb.co.za/article/sita-engages-private-sector-to-power-first-govt-cloud/KWEBb7yaRko7mRjO · https://www.tenders-sa.org/blog/security-procurement-guide-2026-03-12 · https://www.theregister.com/off-prem/2025/07/25/microsoft-exec-admits-it-cannot-guarantee-data-sovereignty/458553

**AI compute:** https://www.cassava.ai/ai-compute/ · https://www.cassava.ai/news/ · https://www.cassava.ai/2026/03/18/cassava-scales-african-ai-infrastructure-with-nvidia-powered-ai-factories-to-accelerate-sovereign-data-capabilities/ · https://www.cassava.ai/2025/11/11/forges-ahead-with-gpu-as-a-service/ · https://www.cassava.ai/2026/09/08/launch-of-egypt-first-national-ai-factory/ · https://www.cassavatechnologies.com/ · https://www.bis.gov/press-release/department-commerce-rescinds-biden-era-artificial-intelligence-diffusion-rule-strengthens-chip-related · https://www.federalregister.gov/documents/2025/01/15/2025-00636/framework-for-artificial-intelligence-diffusion · https://www.nvidia.com/en-us/data-center/dgx-b200/ · https://www.nvidia.com/en-us/data-center/h100/ · https://www.nvidia.com/en-us/data-center/rtx-pro-6000-blackwell-server-edition/ · https://modal.com/blog/nvidia-h100-price-article · https://www.thundercompute.com/blog/ec2-gpu-instances · https://www.x-rates.com/calculator/?from=USD&to=ZAR&amount=1

**Connectivity and power:** https://www.submarinecablemap.com/api/v3/country/south-africa.json · https://www.submarinecablemap.com/api/v3/cable/2africa.json · https://www.submarinecablemap.com/api/v3/cable/equiano.json · https://www.submarinecablemap.com/api/v3/cable/t3.json · https://www.submarinecablemap.com/api/v3/cable/umoja.json · https://www.submarinecablemap.com/api/v3/cable/project-waterworth.json · https://www.submarinecablemap.com/api/v3/cable/djibouti-africa-regional-express-1-dare-1.json · https://blog.cloudflare.com/undersea-cable-failures-cause-internet-disruptions-across-africa-march-14-2024/ · https://blog.cloudflare.com/q2-2024-internet-disruption-summary/ · https://mybroadband.co.za/news/cellular/653722-mtn-switching-off-3g-in-parts-of-south-africa.html · https://mybroadband.co.za/news/cellular/628151-blow-to-south-africas-2g-and-3g-shutdown.html · https://techcentral.co.za/spacex-breaks-cover-at-icasa-satellite-spectrum-hearings/285094/ · https://mybroadband.co.za/news/broadband/668575-south-africa-misses-out-on-starlink-mobile.html · https://techcentral.co.za/cell-c-in-talks-with-starlink-and-amazon-leo/285187/ · https://mybroadband.co.za/news/broadband/658488-starlink-roaming-crackdown-in-south-africa.html · https://en.wikipedia.org/wiki/South_African_energy_crisis · https://www.eskom.co.za/eskom-fully-meets-2026-winter-demand-eaf-remains-at-its-highest-level-since-2020-67-79-and-diesel-expenditure-declines-by-r4-84-billion-or-81-64-to-deliver-energy-security/ · https://www.eskom.co.za/eaf-reaches-67-96-diesel-expenditure-falls-by-r4-8-billion-emissions-outperform-target-and-load-reduction-declines-to-just-3-4-of-customers/ · https://www.icasa.org.za/news

**Data catalogue:** https://ocds-api.etenders.gov.za/swagger/index.html · https://data.open-contracting.org/en/publication/143 · https://secure.csd.gov.za/ · https://secure.csd.gov.za/Home/Terms · https://ocpo.treasury.gov.za/RestrictedSupplier/RestrictedSuppliersReport.pdf · https://ocpo.treasury.gov.za/Suppliers_Area/Pages/Database-of-Restricted-Suppliers.aspx · https://www.cipc.co.za/ · https://www.saflii.org/content/terms.html · https://lawlibrary.org.za/terms-of-use/ · https://municipaldata.treasury.gov.za/docs · https://municipalmoney.gov.za/terms · https://vulekamali.gov.za/ · https://census.statssa.gov.za/ · https://wazimap.com/ · https://www.saps.gov.za/services/crimestats.php · https://custom.resbank.co.za/SarbWebApi/Help · https://www.eskom.co.za/dataportal/ · https://eskomsepush.gumroad.com/l/api · https://documenter.getpostman.com/view/1296288/UzQuNk3E · https://www.dws.gov.za/Hydrology/Verified/hymain.aspx · https://odp-cctegis.opendata.arcgis.com/api/search/v1/collections/dataset/items?q=service%20requests · https://www.capetown.gov.za/General/Terms-of-use-open-data · https://aisstream.io/documentation · https://aisstream.io/ · https://globalfishingwatch.org/our-apis/documentation · https://www.aishub.net/join-us · https://www.marinetraffic.com/en/ais-api-services · https://www.adsb.lol/ · https://www.adsb.lol/privacy-license/ · https://open-meteo.com/en/terms · https://open-meteo.com/en/pricing · https://cds.climate.copernicus.eu/licences · https://firms.modaps.eosdis.nasa.gov/api/ · https://www.digitalearthafrica.org/platform-resources/services · https://documentation.dataspace.copernicus.eu/Quotas.html · https://sites.research.google/gr/open-buildings/ · https://docs.overturemaps.org/attribution/ · https://osmfoundation.org/wiki/Licence · https://www.demarcation.org.za/ · https://www.gdeltproject.org/about.html · https://acleddata.com/eula/ · https://acleddata.com/terms-of-use/ · https://docs.openaq.org/about/about

**Attempted but not read (HTTP 429, timeout, 403 or 503):** fews.net · comtradedeveloper.un.org · datahelpdesk.worldbank.org · developers.facebook.com/docs/whatsapp/pricing · business.whatsapp.com/products/platform-pricing · africastalking.com/pricing · hapi.humdata.org · opensky-network.org · transnetnationalportsauthority.net · tools.sars.gov.za/tradestatsportal · dws.gov.za/Hydrology/Weekly · ws.dws.gov.za/iris · opendata.durban · tshwane.gov.za
