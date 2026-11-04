<!-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Warden Security Kernel documentation (P3 tasks 3.1, 3.2, 3.3, 3.5, 3.7) -->
# Warden Security Kernel (`services/warden`)

`services/warden` is the Rust security kernel for Vigil (P3). Every read, write,
export, declassification, and AI model context assembly passes through Warden.
No service reads or writes tenant data around it (ADR-0005).

## Scope in `feature/P3-warden-core`

- **Protobuf service & message contracts ([`proto/vigil/warden/v1/`](../../proto/vigil/warden/v1/)):**
  - [`policy.proto`](../../proto/vigil/warden/v1/policy.proto): `PolicyService`
    (`Evaluate`, `Propagate`, `CreateJustification`, `GetEffectiveContext`)
    matching `POL-01`, `POL-02`, and `POL-03` in [`docs/API_DESIGN.md`](../../docs/API_DESIGN.md)
    section B.6.
  - [`audit.proto`](../../proto/vigil/warden/v1/audit.proto): `AuditService`
    (`ListAuditEvents`, `AuditEvent`, `AuditChain`) matching `AUD-01` in
    [`docs/API_DESIGN.md`](../../docs/API_DESIGN.md) section B.7.
- **Four-part security label model ([`src/model.rs`](src/model.rs), task 3.1):**
  - $\lambda(R) = \langle M, C, REL, P \rangle$: conjunctive markings $M$
    (`^[A-Z][A-Z0-9-]{1,63}$`), hierarchical classification $C$ (`PUBLIC = 0` ..
    `TOP_SECRET = 4` with deployment ceiling `max_classification`), disjunctive
    releasability $REL$ (including `REL-SADC` expansion to the 16 SADC member
    states), and purpose tags $P$.
  - Reserved statutory markings: `PII`, `SPECIAL-PI`, `CRIMINAL-CONDUCT`,
    `CHILD`, `SECURITY-MEASURE`, and `CASE-<n>` compartments.
- **Pure policy evaluation and propagation engine ([`src/engine.rs`](src/engine.rs), tasks 3.2 & 3.5):**
  - `evaluate(input) -> Decision`: deterministic evaluation of rules **W1
    through W12**, returning `allow`, structured `DenyReason` items, property
    `redactions` (`MARKING`, `CLASSIFICATION`, `RELEASABILITY`, `PURPOSE`,
    `CASE`, `LINK_TARGET`) and `cipher_text` masking, mandatory `obligations`
    (`AUDIT_READ`, `NO_AI`, `NO_EXPORT`, `SUPPRESS_SMALL_CELLS(10)`,
    `ATTRIBUTE(terms_ref)`, `REQUIRE_JUSTIFICATION(checkpoint)`), and the
    external HTTP status (`404` on invisible resources under W11 so no existence
    oracle is exposed).
  - `propagate(inputs, transform_label, s15_compatible_purposes) -> PropagationResult`:
    deterministic W5 label derivation (union of markings, maximum of
    classification, intersection of expanded releasability and purpose tags with
    $\top$ neutral, and automatic blocking when $REL(D) = \emptyset$ or
    $P(D) = \emptyset$ unless reopened by an approved POPIA s15(3) compatibility
    record).
- **Tamper-evident hash-chained audit ledger ([`src/audit.rs`](src/audit.rs), task 3.7):**
  - `AuditLedger` and `verify_chain`: W12 per-tenant monotonic sequence,
    SHA-256 hash chaining (`prev_hash` -> `hash`), and HMAC-SHA-256 signature
    verification.
- **Cryptographic primitives ([`src/sha256.rs`](src/sha256.rs)):**
  - Pure-Rust, dependency-free FIPS 180-4 SHA-256, RFC 2104 HMAC-SHA-256, W4.6
    `psn_` pseudonym and `idf_` identifier key derivation, and `******4521`
    masked display.
- **Shared 551-case conformance & property suite ([`src/conformance.rs`](src/conformance.rs), task 3.3):**
  - 551 deterministic conformance vectors covering W1-W12, Worked Examples 1-3
    from [`docs/VIGIL_DOMAIN_BLUEPRINT.md`](../../docs/VIGIL_DOMAIN_BLUEPRINT.md)
    section 4.5, and adversarial edge cases, plus 500 randomized lattice
    property tests verifying commutativity, idempotence, and marking monotonicity.

## Running checks

```sh
cd services/warden
cargo fmt --check
cargo clippy --all-targets -- -D warnings
cargo test --locked
```
