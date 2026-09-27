<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — ADR-0004 sovereign-first deployment (P0) -->

# ADR-0004 — Sovereign-First Deployment

**Status:** Proposed (P0), to be accepted at founder sign-off (P0 task 0.7) · **Date:** 2026-09-25

## Context
The National Data and Cloud Policy (2024) ¶15.4.2 (a policy, not a statute; counsel to confirm its binding effect per client) requires government data relating to national security and sovereignty to be stored only in SA. The US CLOUD Act reaches data held by US providers wherever it sits: Microsoft France told the French Senate it "cannot guarantee" non-disclosure. Amazon Bedrock in Cape Town uses *global* cross-region inference for Claude 4.5. Europe's 2025–26 move away from Palantir was about jurisdiction. Research: [dossier §9–§10](../research/VIGIL_RESEARCH_DOSSIER.md#10-sovereign-infrastructure-and-compute), [appendix 04 §4](../research/appendix/04-legal-regulatory-ethics.md), [appendix 06 §2](../research/appendix/06-infra-data-sources.md).

## Options
1. **Hyperscaler-first, in SA regions.**
   - Against: CLOUD Act reach, thin in-region GPU capacity, and global inference routing (Bedrock).
2. **On-prem only.**
   - Against: slows commercial customers who are comfortable with a labelled cloud tier.
3. **Sovereign-first, with a separately labelled commercial cloud tier (chosen).**

## Decision
1. **Default topology:** SA carrier-neutral colocation (Teraco primary; Teraco CT2 or Equinix JN1 DR), customer on-prem, or air-gapped appliance.
2. **Hyperscaler regions only for a separate "hyperscaler-connected" commercial tier.** That tier is labelled as such and carries a CLOUD Act disclosure. **Never** sovereign or special personal information.
3. **No required egress.**
   - No component may need a network call outside the deployment boundary: models, maps, time, licence checks, telemetry.
   - The Mind gateway denies egress by default.
   - "No prompt leaves SA" is a tested guarantee (P14).
4. **Customer-held keys** (HSM/HYOK in P42), an SA operator entity, and a challenge-and-disclose policy for foreign legal demands, published in transparency reports.
5. **First compute:** one 4× RTX PRO 6000 Blackwell server plus a portable air-gap kit. No H100/B200 purchase until a contract justifies it. Burst only to in-country capacity under a DPA.

## Consequences
- **For:** a clear differentiator against Palantir and the hyperscalers.
- **Against:** a heavier operations burden, which Relay (P20–P21) and the appliance (P37) carry. Some SaaS conveniences (hosted observability, hosted LLM APIs) are excluded from the sovereign tier.
