<!-- Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Agency exchange design direction -->

# ADR-0012: observations and agency exchange as connected operational capabilities

## Status

Proposed implementation direction under the founder's 27 September steering. Records intended next work; does not claim that live surveillance or agency federation is implemented or replace the frozen architecture.

## Context

The founder identified a cross-province violent-incident scenario where surveillance observations and institutional response must connect. The task extends beyond crime and any single application. Existing Forge/Core/Mind/Warden/Relay responsibilities remain appropriate.

## Options and decision direction

1. Centralise every agency's raw data and grant broad shared access. Reject: unclear custody, duplicate stores, avoidable exposure and a brittle trust model.
2. Add messaging between dashboards. Insufficient alone: messages cannot establish source provenance, shared definitions, task ownership or a verified result.
3. Build typed observations plus explicit evidence release into joint cases, with agency requests and durable receipts. Preferred: reuses the evidence/mission/action model while preserving source ownership and boundaries.

Specify observation time, receipt time, source capability/coverage, original artifact, uncertainty and idempotency before connectors. Add grant-aware projections rather than removing tenant isolation. Treat model associations as proposals until reviewed. Keep delivery, acceptance and result distinct. Design for existing institutional systems and a traceable manual bridge where no interface exists.

## Verification and consequences

[CONNECTED_OPERATIONS.md](../CONNECTED_OPERATIONS.md) contains the scenario walkthrough, platform responsibilities, contracts, pilot measurements and implementation sequence. The next executable slice should demonstrate synthetic observations and two-agency case exchange with isolation, stale/duplicate events, acknowledgments, expired grants and recovery tests. It must not imply access to SAPS, toll operators, camera networks or emergency dispatch without actual integrations.

The current source review increment remains a prerequisite. It does not itself implement the joint-case or observation contracts proposed here.
