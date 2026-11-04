<!-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Documentation for the common WIT component worlds (P2 task 2.4) -->
# Common WASM Component Worlds (`proto/wit/common`)

This directory holds the WebAssembly Component Model (WIT) interfaces owned by
P2 (task 2.4) and consumed by Wasmtime hosts in P3 (`services/warden`) and P9
(`services/functions-host`).

## Worlds

| File | Package | World | Consumers | Purpose |
|---|---|---|---|---|
| [`policy.wit`](policy.wit) | `vigil:policy@1.0.0` | `policy` | `services/warden`, `services/functions-host` | Pure, deterministic Warden policy evaluation (`evaluate`, `propagate`, `evaluate-encoded`) over security labels, markings, clearances, purposes, and redaction obligations |
| [`udf.wit`](udf.wit) | `vigil:udf@1.0.0` | `udf` | `services/functions-host` (P9), Ontology Functions | Sandboxed `QUERY` and `EDITS` Ontology Functions with caller-bound `host` object-set queries and proposed edit outputs |

## Sandbox and Security Invariants

1. **No ambient authority:** Neither `vigil:policy` nor `vigil:udf` imports any
   `wasi:filesystem`, `wasi:sockets`, `wasi:cli`, `wasi:clocks`, or `wasi:random`
   interfaces. Evaluation time and watermarks are supplied explicitly by the host
   in `policy-context` and `invocation-context`.
2. **Caller-bound data access:** `vigil:udf/host` callbacks execute under the
   calling principal's verified `PolicyContext` (W1-W12). Invisible objects
   never cross the host-guest boundary (W11).
3. **Platform limits enforced by the host:** Wasmtime hosts enforce fuel and
   epoch-interruption budgets (60 s synchronous budget), per-type memory limits,
   at most 100,000 objects loaded, and at most 3 `search-around` hops per
   invocation (`VIGIL_DOMAIN_BLUEPRINT.md` §1.8, §1.10).
4. **Fail-closed traps:** Any WASM trap or fuel exhaustion maps to `DENY` (in
   `vigil:policy`) or `DEADLINE_EXCEEDED` / `LIMIT_EXCEEDED` (in `vigil:udf`),
   never to a partial commit.

## Verification

`bash sdks/_codegen/verify.sh` validates the syntax, package headers, interface
exports/imports, and zero-WASI-import invariants of every `.wit` file in this
directory.
