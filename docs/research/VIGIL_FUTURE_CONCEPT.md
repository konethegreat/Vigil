<!-- Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Founder-requested concept review and expanded Vigil vision -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification -->

# Vigil: an operating system for coordinated action

**Date:** 25 September 2026

**Status:** Concept proposal for discussion; recommendations, scenarios and targets below are not implemented capabilities or accepted architectural decisions.

**Scope:** Review the founding approach and expand the project around the founder's ambitions for South Africa and Africa: crime, gender-based violence, public safety, functioning infrastructure and broader social problems. Administrative constraints are assumed solvable for this concept exercise. Physical capacity, incomplete information and the possibility of error remain engineering realities.

## 1. The thesis

Vigil should become the system through which people and institutions turn fragmented information into coordinated, measurable action.

It should connect six questions:

1. What is happening?
2. What do we actually know, and what remains uncertain?
3. What outcomes matter to the people affected?
4. Which interventions are feasible with the resources available?
5. Who has accepted responsibility for doing what, by when?
6. What happened after the intervention, and what should change next time?

The long-term ambition is an African institution with its own operational software, applied research, deployment capability, language technology and partner ecosystem. Its product is the infrastructure for solving problems across organisational boundaries.

The defensible asset is the combination of an evidence model, a mission execution system, local operational knowledge, and a growing body of evaluated interventions. The LLMs supplying reasoning can change. Vigil retains its identity and capability when they do.

**Product sentence:** Vigil brings evidence, intelligence and people together to protect lives and keep essential systems working.

**Technical sentence:** Vigil is a federated platform that maintains evidence-backed operational state, plans interventions, coordinates their execution and evaluates outcomes.

These are proposed positioning statements. Global uniqueness has not been established by this review.

## 2. Review of the current approach

The repository is at P0. The inspected checkout contains founding documents, CI scaffolding and infrastructure support, with no application services yet. This review used the founding dossier, architecture, domain blueprint, frontend guide, phase plan, relevant research appendices and communication ledger at commit `cc6f55b`.

### What deserves to survive

The existing approach correctly treats the ontology as a model of real objects, relationships and actions; distinguishes that model from a graph database; preserves provenance and reversible entity resolution; provides a transactional action path; supports several model families; anticipates unreliable connectivity; and treats in-country deployment as a product capability. These are substantial foundations.

Palantir's own documentation describes an operational ontology with both semantic elements and actions. Recreating that vocabulary alone will not differentiate Vigil. It establishes a useful baseline. [Palantir Ontology](https://www.palantir.com/docs/foundry/ontology/overview)

### What should change

| Finding                                                                                               | Evidence in the existing plan                                                                                                  | Proposed change                                                                                                                     |
| ----------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| The chosen commercial entry point has become the identity of the entire company                       | D6–D7 select Corridor Integrity and procurement; the initial domain packs cover those areas                                    | Keep infrastructure as a possible commercial entry point, while making protection and coordinated response core design requirements |
| GBV does not have a substantive product model                                                         | No dedicated protection pack, survivor workflow or outcome model in the inspected domain blueprint or phase index              | Introduce a first-class protection mission with safe contact, care referrals, restricted evidence and verified handoffs             |
| The plan specifies infrastructure more fully than the work an operator must accomplish                | P0–P10 build platform foundations; AI appears in P14–P17, field/comms in P27–P28                                               | Deliver a complete mission across interface, evidence, AI, actions and a field handoff from the first increment                     |
| Platform primitives are being frozen ahead of operational learning                                    | Architecture is marked frozen; several foundational ADRs remain Proposed                                                       | Protect invariants early, but treat component selection and service boundaries as hypotheses until a working mission exercises them |
| The AI design routes tasks to model tiers, but lacks a complete investigation and plan-revision model | Domain blueprint §8 contains routing, evaluation and fallback; §10 contains valuable decision lineage                          | Add explicit hypotheses, evidence gaps, contradictory claims, alternative plans, invalidation and outcome evaluation                |
| Cross-organisation operation arrives too late for the mission                                         | Peering is P38; multi-country pods P39                                                                                         | Exercise two separately controlled organisations in an early prototype, even if the first handoff uses a manual acknowledgment      |
| The visual specification is primarily an engineering specification                                    | Frontend guide defines libraries, route groups, tables and forms                                                               | Define signature interaction patterns around a mission, its evidence and the work still needed                                      |
| Several planning numbers look more settled than their evidence warrants                               | v1 targets include 50 million objects per tenant and several fixed latency limits; the commercial roadmap contains ARR targets | Tie capacity to measured workloads and distinguish targets from forecasts and demonstrated results                                  |
| Comparative claims overreach the evidence                                                             | The dossier states a national gap and relies partly on the absence of verified Palantir customers                              | Replace universal absence claims with a feature-level competitor and partner comparison, including customer validation              |

The appendix already acknowledges Cmore and Vumacam. The problem is not that competitors were entirely omitted; it is that the main thesis still makes stronger absence claims than that research supports.

One technical correction merits an early design decision. Architecture §6.3 allows an external writeback before the local transaction commits. Domain blueprint §1.7 already recognises compensation if the commit then fails. But a timeout can also occur after the external system has acted. The operational model must represent **unknown external outcome**, reconcile using a stable operation identifier, and avoid treating a timeout as proof that nothing happened. Default to a committed intent plus an outbox and acknowledged external execution. Use a synchronous external-first path only where the target's contract justifies it. Temporal retries require deliberate idempotency; they do not make an arbitrary external action occur exactly once. [Temporal activity execution](https://github.com/temporalio/documentation/blob/main/docs/encyclopedia/activities/activity-execution.mdx)

## 3. The actual landscape

The opportunity is substantial, but it is not an empty market.

| Existing capability                                                      | Primary evidence                                                                                                                                 | Implication for Vigil                                                                                                    |
| ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------ |
| General operational ontology and AI integration                          | [Palantir Ontology](https://www.palantir.com/docs/foundry/ontology/overview), [Palantir AIP](https://www.palantir.com/docs/foundry/aip/overview) | Ontology, multiple models and agents are baseline capabilities in this category                                          |
| South African shared situational awareness and multi-source coordination | [CSIR Cmore](https://www.csir.co.za/what-we-do/defence-and-security/command-and-control)                                                         | Local capabilities already exist; compare interoperability, deployment and mission completion in detail                  |
| Connected video, alerts and dispatch                                     | [Vumacam Proof](https://vumacam.co.za/)                                                                                                          | Treat existing networks as potential partners and competitors; validate integration access separately                    |
| Emergency response networks and integration                              | [AURA corporate platform](https://aura.services/za/partner-as-a-corporate/)                                                                      | Response capacity is a real operational network; an app cannot invent it                                                 |
| Justice-system information exchange                                      | [Department of Justice IJS newsletter, March 2025](https://www.justice.gov.za/newsletter/IJS/202503-IJS-Newsletter.pdf)                          | Build a practical integration proposition for existing institutional systems                                             |
| Protection and GBV case management                                       | [Primero / GBVIMS+](https://www.primero.org/)                                                                                                    | Referrals and digital case files already exist; learn from established practitioner workflows and integrate where useful |
| Integrated survivor services                                             | [NPA Thuthuzela Care Centres](https://www.npa.gov.za/thuthuzela-care-centres)                                                                    | Connect existing care, support and justice pathways; design with the people operating them                               |

Vendor pages establish advertised capabilities, not independently measured effectiveness. This is a targeted landscape review, not a procurement evaluation or proof of market exclusivity.

Two research findings should materially affect product design:

- The HSRC's 2022 national GBV study, published in 2024, reports lifetime physical violence among 33.1% of adult women and sexual violence among 9.8%. These are study estimates, not current daily incident counts. Protection belongs in the platform's founding mission. [HSRC full study](https://hsrc.ac.za/wp-content/uploads/2024/12/FULL-REPORT-THE-FIRST-SOUTH-AFRICAN-NATIONAL-GENDER-BASED-VIOLENCE-STUDY-2022-10.12.24-SPREAD.pdf)
- Stats SA's 2025/26 survey reports that 51.8% of households experiencing housebreaking reported none of those incidents to police. Recorded incidents therefore cannot be treated as a complete map of harm. Product analytics must expose reporting coverage and changes in reporting behaviour. [Stats SA GPSJS 2025/26, Figure 23](https://www.statssa.gov.za/publications/P0341/P03412026.pdf)

The proposed opening is the connection between evidence, decisions, services and outcomes across these existing systems, designed for African operating conditions. Whether buyers value that combination enough to switch or integrate is a hypothesis to test.

## 4. What the future could look like

Imagine a district in which an electrical fault, failed water pump, road closure, clinic service interruption and rising requests for assistance are visible as connected operational conditions. Each organisation keeps control of its own systems and sensitive records. Vigil can assemble the permitted parts into a mission, identify which resources are available, coordinate the people responding, and show which service was actually restored.

A protection worker sees a private care and support workflow. A utility manager sees repair dependencies. A dispatcher sees accepted assignments and routes. A public oversight view sees aggregate service performance. They share relevant operational facts, not unrestricted access to each other's records.

Over time, the district learns which combinations of interventions work under which conditions. Another district can reuse the tested workflow without importing private case histories. Local teams can contribute adapters, language resources and evaluated mission packages. National and regional deployments can exchange narrowly scoped findings and operational requests.

That is the scale of the ecosystem: independent organisations operating together, with software that improves their ability to understand and act.

## 5. Six mission families

These are coordinated applications over common primitives. They should not require six separate platforms. Each is a proposed direction, not an existing service.

| Mission         | Core work                                                                                               | Example success measure                                                                     | Necessary collaborators                                                                         |
| --------------- | ------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| **Protect**     | GBV support, child protection, safe contact, referrals, restricted evidence and follow-up               | Time to an accepted referral; completion of agreed support; survivor-reported experience    | Survivor organisations, care providers, social workers, legal support and designated responders |
| **Investigate** | Evidence assembly, incident linkage, competing explanations, document/media review and case preparation | Analyst time saved; independently verified evidence completeness; correction of false links | Investigators, forensic specialists and case reviewers                                          |
| **Integrity**   | Procurement and payment review, ownership records, delivery verification and irregularity investigation | Review yield; time to resolve an exception; verified loss avoided                           | Asset owners, finance teams, auditors and investigators                                         |
| **Lifelines**   | Water, energy, rail, telecoms and clinic infrastructure continuity                                      | Service restoration time; repeat failures; affected service-hours                           | Utilities, repair crews, municipalities and facility operators                                  |
| **Response**    | Flood, fire and other emergency coordination, resource allocation and recovery                          | Accepted dispatch time; completed assistance; unmet demand and coverage                     | Disaster managers, emergency services, community organisations and logistics providers          |
| **Opportunity** | Later expansion into access to services, agricultural support, training and local economic bottlenecks  | Completed service access; reduced administrative delay; verified delivery                   | Service providers, training institutions, agricultural organisations and local businesses       |

The last family tests whether Vigil's problem-solving core generalises beyond safety. It should follow demonstrated reuse, not a promise to become every kind of software at once.

## 6. The GBV flagship: a protected path from first contact to follow-through

**Illustrative scenario:** A person asks for help through a channel they have chosen. The scenario is synthetic; no institutional integrations or response capacity are implied.

1. **Establish a safe interaction.** Ask whether continuing on this channel is safe, which language is preferred and whether any follow-up contact is wanted. Offer a direct route to a trained person. Do not make completing an AI interview a prerequisite to assistance.
2. **Capture once, with control.** Preserve the original statement or recording where the person chooses to provide it. AI may draft a transcript or summary, with uncertainty visible and correction available. A translated summary remains distinct from the original account.
3. **Create a protected mission.** Record the requested help, designated case owner, agreed communication choices and relevant deadlines. The protection record lives in a compartment with narrowly scoped sharing.
4. **Find a feasible path to support.** Combine provider availability, accessibility, travel constraints and the person's preferences. Availability is only called current when a provider has supplied sufficiently recent confirmation.
5. **Execute a handoff.** Send the minimum necessary referral information through an agreed channel. Record whether a named provider accepted it. A sent message is not a completed referral.
6. **Preserve selected evidence.** Keep originals, hashes, upload history, provenance and access history. AI helps organise the material without rewriting it into a supposedly authoritative account.
7. **Support continuing work.** Track agreed care, support and justice-related appointments through the professionals responsible. Generate reminders only using the communication choices recorded for that person.
8. **Detect operational abandonment.** A referral that remains unaccepted or a case without an owner becomes visible to the appropriate supervisor. Escalation rules are designed with practitioners.
9. **Learn from outcomes.** Measure service access, broken handoffs, delays and the person's assessment of support. Ending contact voluntarily is not automatically classified as a failed case.

The NPA's Thuthuzela model already coordinates medical, psychosocial and legal support. Vigil's proposal is an interoperable operational layer around real service pathways, informed by existing systems such as GBVIMS+. It should be designed with those practitioners and survivors. [NPA model](https://www.npa.gov.za/thuthuzela-care-centres), [Primero](https://www.primero.org/)

The interface should assume some devices are shared or observed by another person. Notifications, application history, cloud backups and location sharing can expose an interaction. A discreet appearance or quick-exit control cannot guarantee that use leaves no trace. These are concrete product requirements for safe contact, not administrative additions. [NNEDV Safety Net guidance on apps](https://www.techsafety.org/choosingapps)

The system should prioritise access to assistance without turning a model's interpretation of an account into a judgement of credibility, guilt or entitlement to care.

## 7. The crime and infrastructure flagship

**Illustrative scenario:** A rail operator reports a damaged cable segment. A repair contractor records a different incident time. An authorised incident feed contains a nearby event. A procurement record identifies who maintains the affected asset.

Vigil builds a time-based picture with each source and uncertainty preserved. It distinguishes three possible explanations: theft, an equipment fault, and a record-entry error. It asks which available observation would distinguish them most effectively. It can then prepare parallel work: restore the asset, preserve evidence, check maintenance history and request a review of the conflicting timestamps.

For a human investigator, the useful output is an evidence-backed hypothesis with supporting and conflicting material, plus the next legitimate investigative steps. An association between a supplier and an asset is a relationship, not an accusation.

For an operator, the useful output is a repair mission with dependencies, stock availability, an accountable crew and confirmation that service resumed. The investigation can continue without leaving the repair unowned.

For the platform, the reusable structure is the same as Protect: intake, evidence, constraints, resources, handoff, acknowledgment, completion and follow-up. The record access and specialist workflows remain separate.

## 8. The missing core: evidence, missions and outcomes

The existing ontology remains useful. Extend it with three explicit models.

### Evidence model

Treat a claim as something a source asserts about the world. Do not flatten every assertion into a single property value that looks equally certain.

Suggested primitives:

`Source → Observation → Claim → Hypothesis → Decision → Action → Outcome`

An individual claim should preserve:

- the subject and asserted relationship or value;
- who or what asserted it;
- the original source reference and exact supporting span, image region or time interval;
- when the alleged event occurred and when Vigil learned about it;
- whether it is reported, directly observed, extracted, inferred, disputed, corrected or superseded;
- whether apparently independent sources derive from the same original report;
- the method and version that produced any machine inference;
- uncertainty, missing information and the basis for confidence;
- the rules governing who can use it and for what work.

A model's confidence statement is not a calibrated probability. Where probabilities are useful, measure their calibration against task-specific data. Elsewhere, explicit evidence states are more honest than decorative percentages.

**Belief revision is a product feature.** When an identifier is corrected or a source is retracted, find dependent hypotheses and plans, invalidate pending recommendations where necessary and notify their owners. Preserve the history of what was known at an earlier decision time.

Signed provenance can show origin and tampering history; it cannot establish that a depicted event or underlying allegation is true. C2PA explicitly makes that distinction. [C2PA explainer](https://c2pa.org/specifications/specifications/2.2/explainer/Explainer.html)

### Mission model

A mission is a structured attempt to reach an outcome. Its record contains an objective, participants, an evidence snapshot, constraints, a plan, dependencies, resource commitments, milestones, escalation rules and completion criteria.

Illustrative mission states:

`DRAFT → READY → ACTIVE → WAITING / BLOCKED → COMPLETED / CANCELLED`

An individual external task has a more precise lifecycle:

`PROPOSED → AUTHORISED → SENT → ACCEPTED → IN_PROGRESS → CONFIRMED`

It can also become `DECLINED`, `EXPIRED`, `FAILED` or `OUTCOME_UNKNOWN`.

Those states matter. A task can be authorised but not sent, sent but not accepted, and accepted but not completed. The interface and analytics must not collapse them into a green tick.

### Outcome model

Store what the intervention was expected to achieve, what actually happened, the observation window and how the result was verified. Distinguish:

- activity: a referral was sent;
- service delivery: a provider accepted and completed an agreed service;
- outcome: the person's situation or the asset's operating state improved;
- attribution: the extent to which the intervention caused that improvement.

Attribution is the hardest claim and needs an evaluation design. A dashboard improvement after deployment is not sufficient evidence of causation.

## 9. The multi-model intelligence system

Vigil should use several kinds of intelligence, with models chosen by measured performance on the relevant task. Multiple model vendors are useful for capability diversity and continuity, but the number of models is not the goal.

| Role              | Work                                                                  | Required output                                   |
| ----------------- | --------------------------------------------------------------------- | ------------------------------------------------- |
| Perception        | Speech, OCR, document structure, image/video observations             | Timestamped extraction tied to the original media |
| Retrieval         | Find relevant documents, operational records and prior decisions      | Authorised evidence with source references        |
| Entity resolution | Generate and compare possible matches                                 | Reversible links, reasons and review state        |
| Investigator      | Develop plausible explanations and identify evidence gaps             | Explicit hypotheses and evidence requirements     |
| Challenger        | Look for contradictions and alternative explanations                  | Specific objections linked to evidence            |
| Planner           | Convert objectives into tasks and dependencies                        | A structured, bounded proposed plan               |
| Optimiser         | Allocate vehicles, crews, appointments, stock or routes               | Feasible allocations with declared constraints    |
| Verifier          | Check citations, schemas, preconditions and action results            | Validation record, failures and unresolved issues |
| Communicator      | Explain status in the user's language and appropriate level of detail | A traceable, accessible explanation               |

Some roles use LLMs. Others should use rules, statistical models, retrieval algorithms or mathematical solvers. OR-Tools is an existing optimisation foundation; an LLM can help express a scheduling problem while a solver checks the constraints. [OR-Tools](https://developers.google.com/optimization/)

The routing contract should consider capability, measured local-language quality, data location, permissions, latency, availability, cost and task consequence. A model is eligible because it has passed the relevant evaluation, not because it is the largest or newest model.

Use one capable model for simple work. Add independent specialist passes when they are demonstrably valuable. Preserve disagreement rather than presenting a majority vote as truth. Models can share training biases and can all be persuaded by the same false source. Google Research's agent-scaling evaluation found benefits depended strongly on task structure; additional coordination could hurt sequential tasks. [Google Research, January 2026](https://research.google/blog/towards-a-science-of-scaling-agent-systems-when-and-why-agent-systems-work/)

Graph-based retrieval is useful for connecting material across a corpus, but extracted graph links remain claims requiring validation. It does not turn text into verified ground truth. [Microsoft GraphRAG](https://microsoft.github.io/graphrag/)

The runtime should persist short, structured findings, evidence references, decisions and action receipts. It does not require storing private model chain-of-thought to explain the work.

## 10. Autonomy that means something

Vigil can perform substantial work without continuous prompting: watch authorised feeds, prepare shift briefs, detect stale tasks, extract documents, assemble evidence, generate draft plans, request missing operational information through configured integrations, and monitor acknowledged actions.

Each mission should declare a delegation envelope: allowed tools, accessible records, maximum spend, time budget, retry limit, permitted side effects, escalation conditions and stop conditions. Delegation is an executable contract that the runtime enforces outside the model.

The system can automatically perform routine, reversible tasks within that envelope. It should present consequential decisions to the designated professional with the evidence and options needed to make a real decision. This prevents automation from silently converting an uncertain inference into an accusation or an unsafe contact.

Prompt instructions alone are insufficient isolation. Retrieved material is untrusted input; tool arguments are validated, permissions are rechecked, credentials are scoped, and third-party text cannot grant additional authority.

An AI outage should reduce convenience and throughput while preserving access to the mission, its evidence and the deterministic action interface. This is how an AI-powered operational system stands on its own.

## 11. Federation as part of the product

Make a **Vigil node** a deployable unit for a provider, enterprise, district, utility or other institution. It contains the local evidence and mission stores, necessary model capacity or eligible endpoints, user interfaces and integrations.

A shared protocol lets nodes exchange limited operational messages such as:

- a request to accept a referral;
- a capacity confirmation with a freshness timestamp;
- an evidence request tied to a particular mission;
- a narrowly scoped finding that can be inspected at its source;
- an assignment and signed acknowledgment;
- an aggregate outcome report.

The first implementation can use ordinary APIs, scoped access and explicit agreements. Advanced confidential computing, private set intersection and federated learning are later research options, not prerequisites or magic privacy guarantees.

Shared semantics do not require a central copy of all personal data. Protected care records and investigative records must retain different access boundaries even if they use some of the same software.

## 12. African operating conditions should create the technical advantage

### Language and communication

Evaluate code-switching, accents, local names, noisy voice messages, terminology and translation of intent. Preserve originals alongside translations. Include text, voice and assisted interaction, with signed-language access treated as its own accessibility problem rather than assumed to follow from speech support.

Build language capability with local researchers and paid language specialists. SADiLaR is an established local resource; WAXAL is another relevant research resource. Neither establishes that a model is fit for a protection or emergency workflow. Evaluate the particular languages, recording conditions and tasks Vigil will support. [SADiLaR](https://sadilar.org/en/), [Google Research WAXAL](https://research.google/blog/waxal-a-large-scale-open-resource-for-african-language-speech-technology/)

### Places and identity

Support landmarks, approximate locations, informal addresses, changing contact details and records with no reliable identifier. Represent location uncertainty explicitly. A shared surname, shared phone or similar spelling should generate a candidate match rather than silently collapse two people into one.

### Connectivity and field work

Cache the minimum useful mission packet locally: permitted evidence, maps, assigned tasks and communication options. Capture work offline and reconcile later with explicit conflict states. Expired authority does not become valid merely because a device is offline. A disconnected device cannot guarantee delivery to a remote responder.

### Resource constraints

Track whether a service, vehicle, crew or facility is actually available, when that was last confirmed, and which organisation owns the commitment. Insufficient capacity should become visible unmet demand rather than a fabricated successful referral.

### Environmental intelligence

Use satellite and environmental data for flood extent, water conditions, agriculture and infrastructure exposure. Digital Earth Africa already provides relevant continental datasets and derived products. Sensor timing, spatial resolution and validation still determine what can be inferred. [Digital Earth Africa products](https://docs.digitalearthafrica.org/en/latest/data_specs/)

## 13. The product experience

The signature interface should be a **mission workspace**. A user opens a problem and sees its evidence, current plan, available capacity and work still outstanding in one coherent place.

### Five connected views

1. **Situation:** what changed, where and when, with freshness and uncertainty.
2. **Evidence:** claims, originals, supporting and contradictory material, provenance and missing observations.
3. **Options:** feasible responses, resource requirements, assumptions and tradeoffs.
4. **Operations:** owners, acknowledgments, deadlines, dependencies and failed handoffs.
5. **Outcomes:** delivery confirmation, measured effects, unresolved needs and lessons.

The principal interaction is a mission canvas. Keep a timeline available throughout; selecting a claim reveals its source; selecting a plan shows the tasks that depend on it. A map appears when geography helps. A graph appears when relationships matter. An AI composer helps express intent, but the result becomes a visible object, comparison or action plan.

Distinctive interaction patterns:

- **Evidence lens:** inspect the source behind a sentence, relationship or recommendation without losing the mission context.
- **Uncertainty view:** see missing observations, stale capacity reports, disputed claims and reporting gaps.
- **Decision replay:** reconstruct the evidence and alternatives available when a decision was made.
- **Plan comparison:** compare feasible approaches against declared objectives and assumptions.
- **Handoff receipt:** show the accepting party, confirmation time and remaining conditions.
- **Revision notice:** explain which new evidence changed a plan and which tasks now need attention.

Suggested visual direction: precise cartography, strong typography, restrained mineral and earth tones, clear source annotations and readable operational density. Operator dark mode and field daylight mode should have equal design attention. Motion should explain actual state changes. Colours must also have text or shape equivalents.

A survivor-facing surface should have a different tone and information structure: clear language, minimal disclosure, accessible choices and control over contact. It should not expose an investigative command-centre interface.

AI may select approved visual components and draft an appropriate workspace configuration. Stable actions, navigation and accessibility remain deterministic. Generating arbitrary interface code during an urgent operation would undermine the consistency operators need.

## 14. Engineering architecture and language responsibilities

Keep the useful existing suite names while making the mission model their shared purpose:

| Layer             | Responsibility                                                                   | Proposed implementation direction                                                                |
| ----------------- | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| Cockpit and Field | Operator, analyst, practitioner and field experiences                            | TypeScript/React; Kotlin for a native Android client when offline/device requirements justify it |
| Core              | Missions, domain objects, claims, evidence references, action state and outcomes | Java with explicit transactional boundaries and SQL                                              |
| Mind              | Extraction, retrieval, reasoning, model routing and evaluation                   | Python workers and services                                                                      |
| Warden            | Identity context, data visibility and action authority                           | Audited policy semantics; bounded Rust core where portability and invariants justify it          |
| Forge             | Ingestion, validation, original-source preservation and event handling           | Java/Python adapters; Rust for demanding parsers and edge ingestion                              |
| Relay             | Installation, updates, health and rollback across deployment types               | Adopt existing delivery tools; Go when a custom controller or CLI is justified                   |
| Federation        | Scoped handoffs, evidence requests and node interoperability                     | Versioned contracts owned by Core/Relay; no new language required                                |

Use SQL as a first-class language for state, spatial operations and analytical queries. Consume specialised C/C++/GPU libraries through maintained bindings. Elixir remains a candidate only if measured communications requirements justify another runtime and its support burden.

Start with a modular transactional core, AI workers, an interface and one ingestion path. Keep original evidence in object storage and operational data in PostgreSQL with spatial/vector extensions where needed. Introduce a separate search engine, analytics store or event platform when the mission workload requires it. The mature deployment can use the broader architecture already researched in the repository.

Three implementation options were considered:

| Option                                                            | Advantage                                                                         | Main limitation                                                                           | Recommendation                                                    |
| ----------------------------------------------------------------- | --------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| Extend an existing case-management tool                           | Fastest route to a particular workflow                                            | Does not establish Vigil's general evidence and mission runtime                           | Use as a partner/integration option and a baseline for comparison |
| Build a compact Vigil core with reusable mission primitives       | Tests the distinctive proposition while keeping operational complexity manageable | Requires disciplined boundaries and deliberate later scaling                              | Preferred initial product architecture                            |
| Build the complete seven-product distributed platform immediately | Establishes a broad foundation                                                    | Delays learning, expands failure modes and consumes effort before workflows are validated | Reserve the full topology for demonstrated needs                  |

A second technical choice concerns evidence storage. Ordinary relational claim tables and links are a reasonable initial implementation. A property-graph engine and a distributed graph are alternatives to benchmark if traversal workloads become the constraint. The semantic model should not depend on a specific database vendor.

Versions, licences, model variants and hardware sizing require checks when selected for implementation. This concept does not ratify every version-specific claim in the founding dossier.

## 15. The deeper research programme

| Research direction                      | The ambition                                                                              | What must be proven                                                                                                   |
| --------------------------------------- | ----------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| **Mission compiler**                    | Translate an objective into a typed, executable plan assembled from declared capabilities | Correctness of task dependencies, permissions, resource constraints and failure handling on a bounded mission library |
| **Active investigation**                | Recommend the next observation with the greatest expected value for resolving uncertainty | Measured improvement over straightforward evidence checklists; realistic costs and source availability                |
| **Intervention laboratory**             | Compare repairs, staffing, referrals or response plans before committing scarce resources | Calibrated models, explicit causal assumptions, sensitivity analysis and prospective outcome evaluation               |
| **African operational models**          | Specialise speech, extraction and planning models for local workflows                     | Rights-cleared training data, local benchmarks and improvement over available general models                          |
| **Collaborative learning across nodes** | Reuse operational lessons while limiting raw-data movement                                | Resistance to leakage, poisoning, distribution shift and misleading pooled results                                    |
| **Cascading failure models**            | Anticipate how failures in energy, water, transport and care capacity interact            | Valid dependency data and forecasts that outperform simpler baselines                                                 |
| **Assisted physical response**          | Connect to inspection robots, authorised disaster drones and logistics systems            | Reliable sensing, safe operating envelopes and verified task completion                                               |
| **Verified software adaptation**        | Generate candidate mission packages and interface configurations from observed work       | Sandboxed tests, evaluation, review and reversible release before operational use                                     |

Near-term simulation should start with bounded queue, travel, stock and dependency models. A convincing three-dimensional visual does not establish predictive validity. Claims about predicting individual violence, automatically discovering guilt or perfectly forecasting national social behaviour are not credible engineering milestones.

The strongest long-term research question is: **Can a platform learn which intervention helps, for which situation, under which constraints, and transfer that knowledge responsibly to the next mission?**

## 16. What “stands on its own” should mean

- Core operations can run with local model endpoints and local data stores.
- No single model vendor is essential to the mission runtime.
- Model failure leaves usable evidence, forms, plans and recorded task state.
- A deployment can be installed, updated, backed up and restored without contacting a central Vigil service when its operating profile requires that independence.
- Field users can complete explicitly supported local work during disconnection.
- Data, domain definitions, evidence references and workflow histories can be exported in documented formats.
- Model behaviour can be evaluated and replaced independently of the operational record.
- Software, models and mission packages have identifiable versions and reproducible release artefacts.
- A local operator can understand health and failure states and carry out recovery with documented tools.

Using strong open-source components does not diminish independence. Ownership of the operating model, integration contracts, specialised intelligence and deployment capability is the more useful definition.

## 17. The ecosystem and its economics

The ecosystem has five participants: people requesting help; practitioners and responders; institutions operating nodes; builders supplying integrations and mission packages; and evaluators improving language and operational models.

Publish a useful SDK and versioned mission/evidence contracts. A package should declare its domain schema, capabilities, inputs, allowed actions, interface components, evaluation data and resource requirements. Test interoperability in a sandbox before allowing operational installation.

Potential commercial offers:

- an annual platform subscription for an operating organisation or node;
- mission packs for specific operational problems;
- in-country managed operations or appliances;
- integration and forward-deployed engineering;
- separately priced model/compute capacity;
- sponsored or institution-funded protection deployments.

Avoid charging affected people for urgent access to help. Avoid revenue incentives tied to allegations, arrests or volume of personal data collected. Procurement and infrastructure deployments can help fund core development, but care funding should be explicitly budgeted rather than assumed to materialise later.

The proposed durable advantages are local language evaluation; reusable evidence and mission semantics; dependable operation in constrained environments; integrations and practitioner knowledge; deployment independence; and credible evidence that the product improves work. An outcome library should contain evaluated methods and appropriately shareable findings, not a tradeable repository of sensitive case histories.

### Illustrative capital arithmetic

These are planning scenarios in rand, not salary surveys, supplier quotes, prices or a fundraising forecast. They exclude VAT, financing costs and a nationwide service network.

| Annual scenario                        | Explicit assumptions                                                                                                                                               | Total      |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- |
| Focused product team                   | 10 people × R100,000 fully loaded per month × 12 = R12m; compute/tools R1.5m; field/design/evaluation R1.5m; operational overhead R1m; 20% reserve on R16m = R3.2m | **R19.2m** |
| Larger platform and research programme | 25 people × R120,000 fully loaded per month × 12 = R36m; compute/infrastructure R8m; deployments/domain research R5m; overhead R3m; 20% reserve on R52m = R10.4m   | **R62.4m** |

These envelopes illustrate why a serious operational platform is a team and institution-building effort. A founder-led prototype can cost much less, but it does not establish readiness to operate urgent public-facing services.

## 18. Replace the phase ladder with complete mission increments

The existing P0–P43 inventory is useful as a backlog. It should not be the order in which users experience value.

**Planning assumption:** a dedicated multidisciplinary team of approximately 8–12 people, a bounded prototype scope and active domain partners. These are targets, not promises; real service deployment depends on verified operational capacity.

| Increment                             | Target window | Demonstration                                                                                             | Exit evidence                                                                                        |
| ------------------------------------- | ------------- | --------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| Mission definition and evaluation set | Weeks 1–3     | One Protect workflow and one infrastructure incident expressed as evidence, resources, tasks and outcomes | Practitioner-reviewed scenarios; source-access inventory; declared failure cases                     |
| Working mission thread                | Weeks 4–8     | Intake → evidence → AI-assisted plan → human decision → sandbox handoff → acknowledgment → outcome        | A replayable synthetic case with source inspection and durable task state                            |
| Shared-core proof                     | Weeks 9–12    | Both mission families use the same primitives with distinct permissions and views                         | No second copy of the core; two organisation contexts; model outage and disconnected-field exercises |
| Shadow use with a partner             | Months 4–6    | Staff compare outputs against normal work without relying on unproven automation                          | Measured task quality, false positives, workload, latency and feedback                               |
| Bounded operational deployment        | Months 6–12   | A defined service uses confirmed integrations and accountable response procedures                         | Observed handoffs and outcomes, recovery exercises and independent review                            |
| Repeated deployment and expansion     | Year 2 onward | Additional nodes and missions reuse evaluated components                                                  | Lower deployment effort, stable unit economics and repeatable outcome evidence                       |

The first compelling demo should show a source correction changing a hypothesis and plan; a receiving organisation declining a task; a fallback resource being selected; a model becoming unavailable; and a confirmed outcome. It should also show a successful ordinary path. That demonstrates a system capable of operating through reality, not only a polished happy path.

Field capture, communications, selected evaluation and recovery tests move into the first mission increments. Large-scale peering and sophisticated deployment machinery can deepen later.

## 19. How to evaluate the claim

Use explicit comparison groups and baselines. Compare AI-assisted workflows with a well-designed deterministic workflow and existing practice, not only with an intentionally weak chatbot.

| Dimension     | Measure                                                                                                   |
| ------------- | --------------------------------------------------------------------------------------------------------- |
| Evidence      | Unsupported-claim rate; accuracy of source links; contradiction handling; correction propagation          |
| Identity      | False merges and missed matches by relevant population/language/context; review burden                    |
| AI assistance | Task completion, valid tool use, abstention quality, time saved and cost per completed mission            |
| Language      | Meaning-preserving extraction and communication under real recording and code-switching conditions        |
| Operations    | Acknowledged handoffs, overdue work, confirmed completion and unresolved external outcomes                |
| Protection    | Safe-contact adherence, agreed service access, unwanted disclosures and survivor-defined outcomes         |
| Resilience    | Useful work under model outage, lost connectivity, duplicate events and recovery from backup              |
| Impact        | Prospective changes in the intended outcome, with uncertainty and plausible alternative causes considered |

Raw reporting counts need interpretation: increased reporting can mean improved access rather than increased underlying harm. Conviction rates, case closures and alerts generated are insufficient as the sole indicators of success. Measure what the mission was intended to improve.

## 20. Proposed decisions for the next revision

1. Adopt coordinated problem-solving and verified outcomes as the platform identity.
2. Make Protect a founding mission, with GBV practitioners and survivors shaping its workflow.
3. Keep an infrastructure mission as a commercial candidate and a test of shared-core reuse.
4. Add explicit claims, hypotheses, mission plans, resource commitments, acknowledgments and outcomes to the conceptual domain model.
5. Extend existing multi-model routing with disagreement, evidence-gap analysis, plan revision and evaluation against local tasks.
6. Bring two-organisation handoffs, field constraints and failure recovery into the first demonstrable increment.
7. Design the mission workspace before fixing its UI component inventory.
8. Retain pragmatic polyglot boundaries while delaying infrastructure that lacks a measured need.
9. Fund a bounded research programme for mission compilation, local language models and intervention evaluation.
10. Replace universal uniqueness claims with a testable competitive proposition.

The ambition is to build an African operational institution whose software helps people understand what is happening, coordinate a response and demonstrate that the response mattered. The founder can begin that now by establishing the first complete mission and the principles that let thousands of other missions follow.
