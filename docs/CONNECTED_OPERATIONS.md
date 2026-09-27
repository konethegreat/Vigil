<!-- Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Founder-directed agency and observation architecture -->

# Vigil: connected operations across institutions

**Design direction, not a claim of deployed agency access.** This document carries the founder's latest instruction into the existing Forge, Core, Mind, Warden, Relay and Cockpit architecture. The local application now implements source preservation, quoted model proposals, reviewed publication, investigations and mission receipts. Camera ingestion, inter-agency sharing and live dispatch remain subsequent implementation work.

## The problem we have to solve

After a serious incident, useful information can exist in several places while no operational team has a current, defensible picture. A witness statement, vehicle observation, toll transaction, incident report and dispatch receipt are different records with different owners and clocks. The product succeeds when it shortens the path from an observation to an accountable response and preserves the evidence needed to assess what happened afterwards.

The founder's tavern-to-Limpopo example is a motivating scenario. Its date, casualty count, suspect route, camera coverage and cause of any delay have not been established here. Treat those as scenario assumptions, never seed them as confirmed intelligence. In particular, an apparent road route does not prove that a vehicle passed a particular tollgate or that footage exists.

Existing systems must be assessed rather than presumed absent. The Department of Justice describes the IJS Transversal Hub and electronic inter-department exchanges, including docket requests, charge sheets and case outcomes. Its July 2026 account also describes person-management integration. These official descriptions do not establish availability or response speed for our specific scenario. Vigil's discovery work must identify the remaining operational gaps and actual accessible interfaces. [Justice: IJS exchanges](https://www.justice.gov.za/m_speeches/2023/20231010-Political-Overview-Min.html), [Justice: July 2026 account](https://www.justice.gov.za/m_speeches/2026/20260730-Lex-Informatica2026-DMin.html).

## One platform, distinct responsibilities

Palantir's current architecture documentation describes Foundry as the data operations foundation, AIP as the AI layer and Apollo as the delivery platform, with Gotham applications integrated into that architecture. The useful lesson is the connection between layers and operational workflows. [Palantir platform architecture](https://www.palantir.com/docs/foundry/architecture-center/platforms).

Vigil's existing names already express the necessary separation. Do not rename the platform every time a new capability is proposed.

| Vigil component | Responsibility | Concrete operating result |
| --- | --- | --- |
| **Forge** | Connect systems, preserve originals, validate schemas, normalise observations and track lineage | A toll observation and a case document can be used together without losing their separate origins |
| **Core** | Shared object definitions, claims, timelines, cases, missions and transactional actions | A team can trace a conclusion to its sources and record a response against the same incident |
| **Mind** | Model routing, quoted extraction, retrieval, hypothesis review, translation and evaluations | Specialists help interpret authorised records; every output names its inputs and limitations |
| **Warden** | Identity, agency membership, case participation, release markings and purpose | Each institution can collaborate without handing every participant its entire database |
| **Relay** | Signed releases, controlled updates, recovery and operation at disconnected sites | Provincial nodes and edge equipment keep working through network and power interruptions |
| **Cockpit / Field** | Investigation, incident coordination, operator queues and field receipts | The person responsible can see what is known, what is requested and what requires action |

**Agency exchange is a cross-cutting Core/Forge/Warden capability.** It is not a separate chat product. Its key records are shared case participation, requests, release grants, evidence packets, receiving acknowledgments and results. Case correlation should also connect infrastructure incidents, procurement records and other relevant missions without equating association with wrongdoing.

## Walk the scenario through the system

1. **Open and verify the incident.** Preserve the first report with time, place, custodian, uncertainty and the assigned incident owner. Separate occurrence time from report time. Incident duplication is resolved explicitly, with prior references retained.
2. **Establish observations worth following.** A witness may describe a vehicle or supply a partial registration. Preserve the wording and alternatives. A plate reading is an observation of a plate; it is not a verified vehicle identity, driver identity or proof of participation in an offence.
3. **Discover relevant coverage.** Show known cameras, toll operators and other participating sources for the incident's time and area, with actual capabilities and health. A video camera may not perform plate recognition. An unavailable feed is a gap, not evidence that nothing passed.
4. **Request a bounded search.** An authorised investigator sends an incident-linked query to participating custodians for a specific time window and candidate description. Source adapters return observations, coverage and limitations. They cannot silently search all unrelated records.
5. **Build a candidate chronology.** Core orders observations with clock uncertainty and late-arrival information. Mind can propose links and explain alternatives. Raw plate text, OCR candidates, image references, confidence calibration and contradictions remain inspectable. Cloned plates, misreads and impossible timing must remain plausible alternatives.
6. **Review and share the relevant packet.** The incident owner chooses an observation packet for a receiving provincial team. It contains the precise task, source references, uncertainty, allowed uses, deadline and contact owner. Receipt means delivered; acceptance means a named recipient has taken responsibility. Neither means a result was achieved.
7. **Coordinate the response.** The authorised agency decides its operational response. Vigil records its decision and feedback. It can prepare reminders and escalations to agreed duty roles; a model cannot infer that an arrest, interception or dispatch occurred from an alert being sent.
8. **Close the loop.** The receiving team records a result or an explicit unknown outcome, with supporting evidence. The case timeline and source assessments are updated. A later correction reaches recipients of the earlier claim and preserves the original history.

This must still work when one participant has only a scanned register or manually entered report. The bridge records who transcribed the source and which original it came from. It must not dress up manual entry as a live system integration.

## Observation contract

The next observation increment needs first-class records rather than a generic text blob pretending to be a live feed.

| Record | Required content |
| --- | --- |
| `SourceCapability` | Owning agency, source type, geographical coverage, available event types, retention window, query limits, contact/duty role, last successful observation and connection state |
| `Observation` | Unique producer/event ID, owner, occurrence time, received time, device clock uncertainty, location uncertainty, observed attributes, original artifact reference/hash, adapter version and quality flags |
| `CandidateLink` | Two observations or an observation and an object, claimed relation, supporting references, conflicting evidence, model/rule version, proposed/reviewed state and reviewer |
| `CoverageResult` | Requested time/area, searched time/area, sources attempted, gaps, response age and explicit outcome: matches, no match, partial, offline, denied or not connected |
| `EvidencePacket` | Case, selected sources and claims, source versions, intended recipients, release grant, expiry, fingerprint and later correction/revocation receipts |
| `AgencyRequest` | Sending and receiving agencies, incident, precise task, named owner or duty role, deadline, evidence packet and current lifecycle state |

Producer event ID and source owner form the deduplication key. Resent events must not create extra sightings. Updates are versioned corrections, never an overwritten observation. Received time cannot replace event time; a late event must not appear to have just happened.

For compatible video products, ONVIF Profile M defines analytics metadata and event interfaces, including geolocation, vehicles and licence plates. Its support is conditional on device capabilities; it does not make every camera an ANPR system. A connector must negotiate capabilities and be tested against the actual deployed product. [ONVIF Profile M](https://www.onvif.org/profiles/profile-m/).

Raw video normally remains with its custodian. The exchange carries searchable metadata and authorised references, and obtains selected clips when needed. As illustrative engineering arithmetic, 1,000 continuous 4-Mbit/s streams produce about **43.2 decimal TB/day**, before replicas. At one 2-KB metadata event per camera every ten seconds, metadata is about **17.28 decimal GB/day**. These are assumed workloads, not measured Vigil capacity or a vendor quote. Clip retrieval, indexing, replication and inference add their own costs.

## Collaboration without removing agency boundaries

The current demonstration's operations and protection identities are isolated tenants. They are not connected real agencies, and switching identities is not federation. Do not solve exchange by disabling their database isolation or granting a universal service account access to everything.

Choose **explicit release to a joint case** as the first exchange design. The source owner keeps the original. A policy-checked projection includes only the released fields and sources. A recipient's view is authorised against both agency membership and case participation. Export and model retrieval enforce the same release rules as the interface.

The originating agency can share a vehicle observation while withholding a witness's contact details. A protection practitioner can contribute a narrow incident fact while retaining confidential support records. Agencies can report that a query was denied or a source is unavailable without revealing the protected record's content.

Operational request states should be explicit:

`DRAFT → RELEASED → DELIVERED → ACCEPTED → IN_PROGRESS → RESULT_RECORDED → CLOSED`

`DECLINED`, `EXPIRED`, `CANCELLED` and `OUTCOME_UNKNOWN` are meaningful outcomes, not disappearing errors. Delivery retries use a stable message ID; receiving acknowledgment does not create duplicate work. An owner records and resolves conflicting acknowledgments. Revocation stops future access where controlled; it cannot claim to erase an already downloaded recipient copy. Record that limitation and any required recipient action.

A connector failing during a response cannot leave the operator staring at an optimistic green status. Show the last successful exchange, stale age, backlog, current owner and available fallback procedure. Store-and-forward queues retain signed messages locally, then replay with deduplication when the connection returns.

## Intelligence that earns its place

- **Perception specialists** process authorised imagery or audio and emit observations with measured quality. An LLM is not the plate-reading engine.
- **Extraction specialists** turn reports into quoted object and relationship proposals. The current local source workflow supplies this boundary.
- **Temporal and spatial engines** test chronology, distances, source coverage and incompatible observations. Deterministic checks precede narrative explanations.
- **Reasoners** retrieve only case-authorised records, propose alternative explanations and identify missing information.
- **Challengers** look for contradictions, source dependence, misleading associations and unsupported conclusions. Two models agreeing is not independent evidence.
- **Workflow assistants** prepare evidence requests, briefings and assignments within explicit tool permissions. Receiving staff confirm consequential operational decisions and results.

Model and connector evaluations must cover South African languages, local place ambiguity, partial/dirty plates, night footage, timestamp drift, delayed ingestion and adversarial source text. Track false associations as well as missed associations. A single good demonstration is not an accuracy estimate.

## How we prove we are solving the problem

The first field pilot should connect one defined operational corridor, two participating agency teams and one evidence custodian. They should replay historic, synthetic or otherwise authorised scenarios against their existing workflow, then run a supervised pilot. Document where delay occurs before proposing automation.

| Question | Measurement and acceptance evidence |
| --- | --- |
| Does information arrive sooner? | Time from source availability to searchable observation; median and p95, separated by connector and outage state |
| Does the right team take ownership? | Time from release to delivery and named acceptance; unacknowledged requests explicitly counted |
| Are candidates useful? | Analyst-confirmed useful associations, rejected associations and review time against a labelled evaluation set |
| Is coverage honestly represented? | Known feed outages, retention gaps and clock errors visible in every scenario that injects them |
| Does the evidence survive handoff? | Every published claim resolves to an accessible original or a clearly recorded access limitation, with receipt and source version |
| Can teams recover? | Replayed messages produce one request and one acknowledgment; restart and disconnection tests preserve records |
| Are boundaries real? | Tests deny nonparticipants, expired grants and unauthorised model retrieval, including exports and cached reads |
| Does the workflow reach a result? | Requests with recorded results or explicit unknown outcomes; do not substitute alert counts or arrests for evidence of effectiveness |

Candidate simulator targets can include p95 observation-to-query below five seconds after ingestion and p95 local request delivery below two seconds. These are design targets to test under a declared load, not current measurements or promises about agency response times.

## Delivery sequence

1. **Source review foundation:** preserve text, extract exact-quoted proposals, record analyst decisions, publish atomically, export receipts. Implemented locally in increment 03; see [SOURCE_REVIEW_INCREMENT.md](SOURCE_REVIEW_INCREMENT.md).
2. **Observation workbench:** typed synthetic sightings, event/receipt times, source capability registry, duplicate/late-event handling, time-window search and a map/timeline with visible coverage gaps. No real camera connection implied.
3. **Two-agency exchange:** joint-case membership, explicit release grants, evidence packets, request inboxes and durable acknowledgment/result records. Begin with separate synthetic tenants; pass isolation and replay tests before adding a real connector.
4. **One real custody chain:** integrate a consenting source custodian through its actual supported interface; test source preservation, time quality, withdrawal, outage and coverage semantics.
5. **Field pilot and edge operation:** responsible agency users validate usefulness, duty-role routing, offline receipts, corrections and operational outcomes. Expand domains only with measured evidence that the workflow works.

The same primitives support infrastructure sabotage, stolen assets, missing-person response, emergency coordination, procurement anomalies, port operations and protected support pathways. Surveillance is an important input to a larger system whose unit of success is resolved work backed by evidence.
