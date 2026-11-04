// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Warden Security Kernel library root and property-based test suite (P3 tasks 3.1, 3.2, 3.3, 3.7)
//! Warden Security Kernel (`services/warden`).
//!
//! Pure, deterministic implementation of the Vigil security model (VIGIL_DOMAIN_BLUEPRINT
//! section 4, rules W1 through W12):
//! - [`model`]: Four-part security labels `lambda(R) = <M, C, REL, P>`, `PolicyContext`,
//!   `ResourceDescriptor`, `Decision`, and `PropagationResult`.
//! - [`engine`]: `evaluate(input) -> Decision` (rules W1-W11) and `propagate(inputs, tf) -> PropagationResult` (rule W5).
//! - [`audit`]: Append-only, SHA-256 hash-chained and HMAC-signed audit ledger (rule W12).
//! - [`sha256`]: Dependency-free FIPS 180-4 SHA-256, RFC 2104 HMAC-SHA-256, and W4.6 `psn_` / `idf_` derivation.
//! - [`conformance`]: 520+ case shared conformance suite covering W1-W12, worked examples, and adversarial edge cases.

pub mod audit;
pub mod conformance;
pub mod engine;
pub mod model;
pub mod sha256;

#[cfg(test)]
mod tests {
    use std::collections::BTreeSet;

    use crate::audit::{AuditChainError, AuditLedger, GENESIS_PREV_HASH};
    use crate::conformance::{build_conformance_suite, verify_conformance_suite};
    use crate::engine::{evaluate, propagate};
    use crate::model::{
        Classification, SecurityLabel, case_compartment_marking, expand_releasability,
        is_valid_marking_id,
    };
    use crate::sha256::{derive_identifier_key, derive_pseudonym, mask_display, sha256_hex};

    #[test]
    fn nist_sha256_and_pseudonyms_are_deterministic() {
        assert_eq!(
            sha256_hex(b""),
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );
        assert_eq!(
            sha256_hex(b"abc"),
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        );
        let key = b"synthetic-tenant-demo-psn-key-v1";
        let psn1 = derive_pseudonym(key, "SA_ID", "8001015009087");
        let psn2 = derive_pseudonym(key, "SA_ID", "8001015009087");
        assert_eq!(psn1, psn2);
        assert!(psn1.starts_with("psn_"));
        assert_eq!(psn1.len(), 30);

        let idf = derive_identifier_key(key, "CIPC_REG", "2020/123456/07");
        assert!(idf.starts_with("idf_"));
        assert_eq!(idf.len(), 30);

        assert_eq!(mask_display("8001015009087"), "******9087");
        assert_eq!(mask_display("1234"), "******");
    }

    #[test]
    fn marking_validation_and_sadc_expansion_work() {
        assert!(is_valid_marking_id("PII"));
        assert!(is_valid_marking_id("SPECIAL-PI"));
        assert!(is_valid_marking_id("CASE-2026-000042"));
        assert!(!is_valid_marking_id("p"));
        assert!(!is_valid_marking_id("lower-case"));
        assert!(!is_valid_marking_id("123-START"));
        assert_eq!(
            case_compartment_marking("case-2026-000042"),
            "CASE-2026-000042"
        );

        let expanded = expand_releasability(["REL-SADC"]);
        assert_eq!(expanded.len(), 16);
        assert!(expanded.contains("REL-ZA"));
        assert!(expanded.contains("REL-BW"));
        assert!(!expanded.contains("REL-KE"));
    }

    #[test]
    fn conformance_suite_passes_all_550_cases() {
        let count = verify_conformance_suite();
        assert!(count >= 500, "expected >= 500 cases, got {count}");
    }

    #[test]
    fn audit_ledger_chains_and_detects_tampering() {
        let suite = build_conformance_suite();
        let mut ledger = AuditLedger::new("demo", "audit-k1", b"synthetic-audit-signing-key");

        for (i, case) in suite.iter().take(25).enumerate() {
            let decision = evaluate(&case.input);
            let rec = ledger.record_decision(
                format!("01926e64-0000-7000-8000-{i:012}"),
                "policy.evaluate",
                &case.input,
                &decision,
            );
            assert_eq!(rec.seq, (i as u64) + 1);
            if i == 0 {
                assert_eq!(rec.chain.prev_hash, GENESIS_PREV_HASH);
            }
        }

        assert_eq!(ledger.verify(), Ok(()));

        // Tamper with a middle record's decision and verify detection.
        let mut tampered = ledger.records().to_vec();
        tampered[10].decision = "TAMPERED".to_owned();
        assert_eq!(
            crate::audit::verify_chain(&tampered, b"synthetic-audit-signing-key"),
            Err(AuditChainError::HashMismatch { seq: 11 })
        );

        // Tamper with a signature and verify detection.
        let mut sig_tampered = ledger.records().to_vec();
        sig_tampered[4].chain.signature = "00".repeat(32);
        assert_eq!(
            crate::audit::verify_chain(&sig_tampered, b"synthetic-audit-signing-key"),
            Err(AuditChainError::SignatureMismatch { seq: 5 })
        );
    }

    #[test]
    fn property_lattice_invariants_hold_across_randomized_inputs() {
        // Deterministic xorshift64 property test over 500 generated lattice pairs.
        let mut seed: u64 = 0x5649_4749_4c5f_5733;
        let mut next_u64 = || {
            seed ^= seed << 13;
            seed ^= seed >> 7;
            seed ^= seed << 17;
            seed
        };

        let markings_pool = ["PII", "ORG-A", "ORG-B", "PRJ-1", "PRJ-2"];
        let rel_pool = ["REL-ZA", "REL-BW", "REL-NA", "REL-MZ"];
        let purpose_pool = [
            "PROCUREMENT_INTEGRITY",
            "DUE_DILIGENCE",
            "CORRIDOR_OPERATIONS",
        ];

        for _ in 0..500 {
            let bits_a = next_u64();
            let bits_b = next_u64();

            let make_label = |bits: u64| -> SecurityLabel {
                let mut m = BTreeSet::new();
                for (i, &mk) in markings_pool.iter().enumerate() {
                    if (bits & (1 << i)) != 0 {
                        m.insert(mk.to_owned());
                    }
                }
                let mut r = BTreeSet::new();
                for (i, &rl) in rel_pool.iter().enumerate() {
                    if (bits & (1 << (i + 8))) != 0 {
                        r.insert(rl.to_owned());
                    }
                }
                let mut p = BTreeSet::new();
                for (i, &pp) in purpose_pool.iter().enumerate() {
                    if (bits & (1 << (i + 16))) != 0 {
                        p.insert(pp.to_owned());
                    }
                }
                let c = Classification::from_level(((bits >> 24) % 5) as i32);
                SecurityLabel {
                    markings: m,
                    classification: c,
                    releasability: r,
                    purpose_tags: p,
                }
            };

            let la = make_label(bits_a);
            let lb = make_label(bits_b);
            let tf = SecurityLabel::public_unrestricted();

            // Invariant 1: Commutativity of W5 propagate(A, B) == propagate(B, A).
            let ab = propagate(&[la.clone(), lb.clone()], &tf, None);
            let ba = propagate(&[lb.clone(), la.clone()], &tf, None);
            assert_eq!(ab.allowed, ba.allowed);
            assert_eq!(ab.derived_label, ba.derived_label);

            // Invariant 2: Idempotence of W5 propagate(A, A) == A.
            let aa = propagate(&[la.clone(), la.clone()], &tf, None);
            assert!(aa.allowed);
            assert_eq!(aa.derived_label, la);

            // Invariant 3: Derived markings are a superset of every input's markings.
            assert!(la.markings.is_subset(&ab.derived_label.markings));
            assert!(lb.markings.is_subset(&ab.derived_label.markings));
        }
    }
}
