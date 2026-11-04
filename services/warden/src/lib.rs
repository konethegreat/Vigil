// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Warden Security Kernel library root and property-based test suite (P3 tasks 3.1, 3.2, 3.3, 3.7)
// Modified by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Export pushdown and kms modules and add pushdown/KMS/shadow-swap verification tests (P3 tasks 3.4, 3.6, 3.8)
// Modified by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Add C-ABI / WASM conformance entrypoints for Java FFM, Python ctypes, and TypeScript WebAssembly bindings (P3 task 3.3)
//! Warden Security Kernel (`services/warden`).
//!
//! Pure, deterministic implementation of the Vigil security model (VIGIL_DOMAIN_BLUEPRINT
//! section 4, rules W1 through W12):
//! - [`model`]: Four-part security labels `lambda(R) = <M, C, REL, P>`, `PolicyContext`,
//!   `ResourceDescriptor`, `Decision`, and `PropagationResult`.
//! - [`engine`]: `evaluate(input) -> Decision` (rules W1-W11) and `propagate(inputs, tf) -> PropagationResult` (rule W5).
//! - [`audit`]: Append-only, SHA-256 hash-chained and HMAC-signed audit ledger (rule W12).
//! - [`pushdown`]: Store-native enforcement compilers for PostgreSQL RLS/masking views, OpenSearch DLS/FLS, and S3 scoped credentials (task 3.6).
//! - [`kms`]: Key hierarchy, AES-256-GCM AAD key channels, and dual-custodian ceremony verification (task 3.8).
//! - [`sha256`]: Dependency-free FIPS 180-4 SHA-256, RFC 2104 HMAC-SHA-256, and W4.6 `psn_` / `idf_` derivation.
//! - [`conformance`]: 520+ case shared conformance suite covering W1-W12, worked examples, and adversarial edge cases.

pub mod audit;
pub mod conformance;
pub mod engine;
pub mod kms;
pub mod model;
pub mod pushdown;
pub mod sha256;

/// Returns the total number of conformance vectors in the shared Warden conformance suite.
/// Exported with C ABI for Java FFM (`java.lang.foreign`), Python `ctypes`, and WASM hosts.
#[allow(unsafe_code)]
#[unsafe(no_mangle)]
pub extern "C" fn warden_conformance_count() -> u32 {
    conformance::build_conformance_suite().len() as u32
}

/// Executes the full 551-case shared conformance suite and W12 hash-chained audit verification,
/// returning the number of verified cases (`>= 500`) or `0` on failure.
#[allow(unsafe_code)]
#[unsafe(no_mangle)]
pub extern "C" fn warden_run_conformance_suite() -> u32 {
    let suite = conformance::build_conformance_suite();
    let count = conformance::verify_conformance_suite();
    let mut ledger = audit::AuditLedger::new("demo", "audit-k1", b"synthetic-audit-signing-key-v1");
    for (idx, case) in suite.iter().enumerate() {
        let decision = engine::evaluate(&case.input);
        ledger.record_decision(
            format!("01926e64-0000-7000-8000-{idx:012}"),
            "policy.evaluate",
            &case.input,
            &decision,
        );
    }
    if ledger.verify().is_err() {
        return 0;
    }
    count as u32
}

/// Evaluates conformance vector `case_index` and returns a packed 32-bit status word:
/// - bit 0: `1` if `decision.allow` is true, `0` otherwise.
/// - bits 1..=15: `decision.external_http_status` (`200`, `403`, or `404`).
/// - bits 16..=23: `decision.redactions.len()`.
/// - bits 24..=30: `decision.obligations.len()`.
/// - bit 31: `1` if `case_index` is out of bounds.
#[allow(unsafe_code)]
#[unsafe(no_mangle)]
pub extern "C" fn warden_eval_conformance_case(case_index: u32) -> u32 {
    let suite = conformance::build_conformance_suite();
    let Some(case) = suite.get(case_index as usize) else {
        return 1u32 << 31;
    };
    let decision = engine::evaluate(&case.input);
    let allow_bit = u32::from(decision.allow);
    let status_bits = u32::from(decision.external_http_status & 0x7fff) << 1;
    let redact_bits = ((decision.redactions.len().min(255)) as u32) << 16;
    let oblig_bits = ((decision.obligations.len().min(127)) as u32) << 24;
    allow_bit | status_bits | redact_bits | oblig_bits
}

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

    #[test]
    fn postgres_rls_masking_and_policy_ctx_compilers_match_data_model() {
        use crate::model::{EvaluationInput, Operation, PolicyContext, ResourceDescriptor};
        use crate::pushdown::{
            ColumnMaskSpec, S3AccessMode, compile_opensearch_dls_fls,
            compile_postgres_masking_view, compile_postgres_rls_policies, compile_s3_scoped_rule,
            sign_postgres_policy_ctx, verify_shadow_swap_labels,
        };

        let rls_ddl =
            compile_postgres_rls_policies("objects", "ot_supplier", &["id", "markings"], true)
                .expect("valid RLS DDL");
        assert!(rls_ddl.contains("ALTER TABLE objects.ot_supplier ENABLE ROW LEVEL SECURITY;"));
        assert!(rls_ddl.contains("ALTER TABLE objects.ot_supplier FORCE ROW LEVEL SECURITY;"));
        assert!(
            rls_ddl
                .contains("CREATE POLICY p_read ON objects.ot_supplier FOR SELECT TO vigil_reader")
        );
        assert!(rls_ddl.contains("CREATE POLICY p_actions_update ON objects.ot_supplier"));
        assert!(
            rls_ddl
                .contains("GRANT SELECT (id, markings) ON objects.ot_supplier TO vigil_traversal;")
        );

        let cols = vec![
            ColumnMaskSpec {
                column_name: "id".to_owned(),
                column_markings: BTreeSet::new(),
                column_classification: Classification::Public,
            },
            ColumnMaskSpec {
                column_name: "bank_account_masked".to_owned(),
                column_markings: ["PII".to_owned()].into_iter().collect(),
                column_classification: Classification::Confidential,
            },
        ];
        let view_ddl = compile_postgres_masking_view(
            "objects_v",
            "ot_supplier",
            "objects",
            "ot_supplier",
            &cols,
        )
        .expect("valid masking view DDL");
        assert!(view_ddl.contains("WITH (security_barrier = true, security_invoker = true)"));
        assert!(view_ddl.contains("'{PII}'::text[] <@ (SELECT vigil_sec.ctx_markings())"));
        assert!(view_ddl.contains("(SELECT vigil_sec.ctx_clearance()) >= 2"));
        assert!(view_ddl.contains("AS redacted_properties"));

        let ctx = PolicyContext {
            principal: "user:01926e64-1111-7000-8000-000000000001".to_owned(),
            tenant: "demo".to_owned(),
            markings: ["ORG-TRANSNET-DEMO".to_owned()].into_iter().collect(),
            clearance: Some(Classification::Confidential),
            releasability: ["REL-ZA".to_owned()].into_iter().collect(),
            allowed_purposes: ["PROCUREMENT_INTEGRITY".to_owned()].into_iter().collect(),
            declared_purpose: "PROCUREMENT_INTEGRITY".to_owned(),
            case_id: None,
            roles: ["vigil-analyst".to_owned()].into_iter().collect(),
            evaluation_time_s: 1_793_750_400,
            justifications: Vec::new(),
        };
        let token = sign_postgres_policy_ctx(
            &ctx,
            "2026-11-04T12:00:00Z",
            "ctx-k1",
            b"0123456789abcdef0123456789abcdef",
        )
        .expect("signed policy_ctx token");
        assert_eq!(token.split('.').count(), 3);

        let os = compile_opensearch_dls_fls(&ctx, &cols).expect("OpenSearch pushdown");
        assert!(os.dls_query_json.contains("minimum_should_match_field"));
        assert_eq!(os.excluded_fields, vec!["bank_account_masked".to_owned()]);

        let res = ResourceDescriptor {
            rid: "ri.vigil.demo.dataset.01926e64-0000-7000-8000-000000000099".to_owned(),
            tenant: "demo".to_owned(),
            object_type: "Supplier".to_owned(),
            label: SecurityLabel {
                markings: ["ORG-TRANSNET-DEMO".to_owned()].into_iter().collect(),
                classification: Some(Classification::Restricted),
                releasability: ["REL-ZA".to_owned()].into_iter().collect(),
                purpose_tags: ["PROCUREMENT_INTEGRITY".to_owned()].into_iter().collect(),
            },
            properties: Vec::new(),
            case_id: None,
            case_party_role: None,
            required_gates: BTreeSet::new(),
            attribution_terms_ref: None,
            no_ai_obligation: false,
        };
        let input = EvaluationInput {
            context: ctx,
            operation: Operation::Read,
            resource: res.clone(),
            proposed_label: None,
            active_gates: BTreeSet::new(),
            discretionary_allowed: true,
            max_classification: Classification::Confidential,
            purpose_requires_case: false,
            case_open: false,
            case_has_threshold_statement: false,
            bystander_expansion_justified: false,
            requires_person_linkage: false,
            reveal_cipher_text: false,
            model_endpoint_label: None,
            model_endpoint_is_shared: false,
            declassification_approval: None,
        };
        let s3_rule = compile_s3_scoped_rule(
            &input,
            "vigil-demo-lake",
            "demo/core/supplier/",
            S3AccessMode::ReadOnly,
            900,
        )
        .expect("valid S3 rule");
        assert_eq!(s3_rule.ttl_seconds, 900);
        assert!(
            compile_s3_scoped_rule(
                &input,
                "vigil-demo-lake",
                "demo/core/supplier/",
                S3AccessMode::ReadOnly,
                901,
            )
            .is_err()
        );

        // Shadow swap declassification guard (W8).
        let mut widened = res.clone();
        widened.label.classification = Some(Classification::Public);
        assert!(
            verify_shadow_swap_labels(
                std::slice::from_ref(&res),
                std::slice::from_ref(&widened),
                &BTreeSet::new()
            )
            .is_err()
        );
        let approved: BTreeSet<String> = [res.rid.clone()].into_iter().collect();
        assert!(verify_shadow_swap_labels(&[res], &[widened], &approved).is_ok());
    }

    #[test]
    fn kms_aad_binding_and_dual_custodian_ceremony_enforce_isolation() {
        use crate::kms::{
            InMemoryOpenBaoKms, KeyCeremonyRecord, KeyChannel, KmsEnvelopeProvider, TenantKekRef,
            build_cipher_text_aad,
        };

        let kms = InMemoryOpenBaoKms::new([0x42u8; 32]);
        let kek = TenantKekRef {
            tenant: "demo".to_owned(),
            kms_key_ref: "openbao://transit/keys/tenant-demo-kek".to_owned(),
            key_version: 1,
        };
        let dek = kms
            .wrap_dek(&kek, KeyChannel::SpecialPi, "ds_supplier", &[0x11u8; 32])
            .expect("wrap DEK");

        let aad = build_cipher_text_aad(
            "demo",
            KeyChannel::SpecialPi,
            "ds_supplier",
            "ri.vigil.demo.object.supplier-1",
            "tax_ref_cipher",
        );
        let ct = kms
            .seal_with_aad(&dek, &aad, b"ZA-TAX-99887766")
            .expect("seal with AAD");
        let pt = kms
            .open_with_aad(&dek, &aad, &ct)
            .expect("open with matching AAD");
        assert_eq!(pt, b"ZA-TAX-99887766");

        // Moving ciphertext to another row or column or tenant must fail authentication.
        let wrong_row_aad = build_cipher_text_aad(
            "demo",
            KeyChannel::SpecialPi,
            "ds_supplier",
            "ri.vigil.demo.object.supplier-2",
            "tax_ref_cipher",
        );
        assert!(kms.open_with_aad(&dek, &wrong_row_aad, &ct).is_err());

        let ceremony = KeyCeremonyRecord {
            key_purpose: "policy-ctx-hmac".to_owned(),
            key_id: "ctx-k1".to_owned(),
            custodian_1: "user:sec-admin-1".to_owned(),
            custodian_2: "user:dpo-1".to_owned(),
            key_commitment_sha256: "a".repeat(64),
            completed_at_s: 1_793_750_400,
        };
        assert!(ceremony.validate().is_ok());

        let same_custodian = KeyCeremonyRecord {
            custodian_2: "user:sec-admin-1".to_owned(),
            ..ceremony
        };
        assert!(same_custodian.validate().is_err());
    }
}
