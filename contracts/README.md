<!-- Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mission contract ownership -->
<!-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification -->

# Mission contracts

The first increment uses bounded HTTP JSON commands. `mission.schema.json` describes public create and command payloads. Java validates allowed fields, lengths, enums, versions and state transitions; Mind additionally validates its request and model response with strict Pydantic schemas. Cockpit read models are in `apps/cockpit/src/lib/contracts.ts`.

These are hand-maintained contracts, not generated Protobuf clients. Change the schema, Java checks, TypeScript types, and relevant acceptance cases together. `verify-mission.py` exercises the deployed request path. The eventual common Protobuf and Warden contracts remain future work.

JSON files in this directory were authored for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25. JSON does not support attribution comments.
