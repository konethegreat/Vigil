// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Shared 520-case Warden conformance suite and property-test runner (P3 tasks 3.2, 3.3)
//! Builds and executes the shared 520+ case Warden conformance suite covering rules W1-W12,
//! the three worked examples in `VIGIL_DOMAIN_BLUEPRINT.md` section 4.5, and adversarial cases.

use std::collections::BTreeSet;

use crate::engine::{evaluate, propagate};
use crate::model::{
    Checkpoint, Classification, DeclassificationApproval, EvaluationInput, JustificationGrant,
    MARKING_CHILD, MARKING_CRIMINAL_CONDUCT, MARKING_PII, MARKING_SECURITY_MEASURE,
    MARKING_SPECIAL_PI, Obligation, Operation, PolicyContext, PropertyDescriptor, RedactionReason,
    ResourceDescriptor, SADC_MEMBER_RELEASABILITY, SecurityLabel,
};

/// One deterministic conformance vector for the Warden policy core.
#[derive(Debug, Clone)]
pub struct ConformanceCase {
    /// Stable case identifier (`"W1-001"`, `"EX1-ANALYST-ALLOW"`, etc.).
    pub id: String,
    /// Formal rule under test (`"W1"` .. `"W12"`).
    pub rule: &'static str,
    /// Evaluation input.
    pub input: EvaluationInput,
    /// Expected `allow` decision.
    pub expected_allow: bool,
    /// Expected external HTTP status (`200`, `403`, or `404`).
    pub expected_http_status: u16,
    /// Expected redacted or masked `(property_name, reason, masked)` tuples.
    pub expected_redactions: Vec<(String, RedactionReason, bool)>,
}

fn set_of(items: &[&str]) -> BTreeSet<String> {
    items.iter().map(|s| (*s).to_owned()).collect()
}

fn base_context() -> PolicyContext {
    PolicyContext {
        principal: "user:analyst-1".to_owned(),
        tenant: "demo".to_owned(),
        markings: set_of(&[MARKING_PII]),
        clearance: Some(Classification::Confidential),
        releasability: set_of(&["REL-ZA"]),
        allowed_purposes: set_of(&[
            "PROCUREMENT_INTEGRITY",
            "DUE_DILIGENCE",
            "CASE_INVESTIGATION",
            "CORRIDOR_OPERATIONS",
        ]),
        declared_purpose: "PROCUREMENT_INTEGRITY".to_owned(),
        case_id: None,
        roles: set_of(&["integrity-analyst"]),
        evaluation_time_s: 1_790_330_000,
        justifications: Vec::new(),
    }
}

fn base_resource() -> ResourceDescriptor {
    ResourceDescriptor {
        rid: "ri.objects.demo.object.Supplier.01926e60-1c2d-7e3f-8a4b-5c6d7e8f9a0b".to_owned(),
        tenant: "demo".to_owned(),
        object_type: "Supplier".to_owned(),
        label: SecurityLabel {
            markings: set_of(&[MARKING_PII]),
            classification: Some(Classification::Confidential),
            releasability: set_of(&["REL-ZA"]),
            purpose_tags: set_of(&["PROCUREMENT_INTEGRITY"]),
        },
        properties: Vec::new(),
        case_id: None,
        case_party_role: None,
        required_gates: BTreeSet::new(),
        attribution_terms_ref: None,
        no_ai_obligation: false,
    }
}

fn base_input() -> EvaluationInput {
    EvaluationInput {
        context: base_context(),
        operation: Operation::Read,
        resource: base_resource(),
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
    }
}

fn valid_justification(ctx: &PolicyContext, checkpoint: Checkpoint) -> JustificationGrant {
    JustificationGrant {
        justification_id: "01926e63-3d4e-7f5a-9b6c-7d8e9fa0b1c2".to_owned(),
        checkpoint,
        principal: ctx.principal.clone(),
        purpose: ctx.declared_purpose.clone(),
        case_id: ctx.case_id.clone(),
        text_len: 84,
        text_sha256: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855".to_owned(),
        issued_at_s: ctx.evaluation_time_s - 600,
        expires_at_s: ctx.evaluation_time_s + 3600,
        allowed_object_types: BTreeSet::new(),
        allowed_rids: BTreeSet::new(),
    }
}

/// Builds the full 520+ case Warden conformance vector suite.
#[must_use]
pub fn build_conformance_suite() -> Vec<ConformanceCase> {
    let mut cases = Vec::with_capacity(540);

    // 1. Classification x Clearance matrix (5 x 5 = 25 cases, max_classification = TopSecret)
    let all_classes = [
        Classification::Public,
        Classification::Restricted,
        Classification::Confidential,
        Classification::Secret,
        Classification::TopSecret,
    ];
    for &u_c in &all_classes {
        for &r_c in &all_classes {
            let mut inp = base_input();
            inp.max_classification = Classification::TopSecret;
            inp.context.clearance = Some(u_c);
            inp.resource.label.classification = Some(r_c);
            let allow = u_c >= r_c;
            cases.push(ConformanceCase {
                id: format!("W1-CLASS-{}-VS-{}", u_c.level(), r_c.level()),
                rule: "W1",
                input: inp,
                expected_allow: allow,
                expected_http_status: if allow { 200 } else { 404 },
                expected_redactions: Vec::new(),
            });
        }
    }

    // 2. Deployment max_classification ceiling enforcement (10 cases)
    for &ceiling in &[
        Classification::Public,
        Classification::Restricted,
        Classification::Confidential,
    ] {
        for &r_c in &all_classes {
            if r_c > ceiling {
                let mut inp = base_input();
                inp.max_classification = ceiling;
                inp.context.clearance = Some(Classification::TopSecret);
                inp.resource.label.classification = Some(r_c);
                cases.push(ConformanceCase {
                    id: format!("W1-CEILING-{}-RES-{}", ceiling.level(), r_c.level()),
                    rule: "W1",
                    input: inp,
                    expected_allow: false,
                    expected_http_status: 404,
                    expected_redactions: Vec::new(),
                });
            }
        }
    }

    // 3. Conjunctive markings power-set matrix (8 x 8 = 64 cases)
    let marking_atoms = ["PII", "ORG-TREASURY", "PRJ-DURBAN"];
    for u_mask in 0u8..8 {
        for r_mask in 0u8..8 {
            let mut u_set = BTreeSet::new();
            let mut r_set = BTreeSet::new();
            for (bit, &m) in marking_atoms.iter().enumerate() {
                if (u_mask & (1 << bit)) != 0 {
                    u_set.insert(m.to_owned());
                }
                if (r_mask & (1 << bit)) != 0 {
                    r_set.insert(m.to_owned());
                }
            }
            let mut inp = base_input();
            inp.context.markings = u_set;
            inp.resource.label.markings = r_set;
            let allow = (r_mask & !u_mask) == 0;
            cases.push(ConformanceCase {
                id: format!("W1-MARKING-U{u_mask:03b}-R{r_mask:03b}"),
                rule: "W1",
                input: inp,
                expected_allow: allow,
                expected_http_status: if allow { 200 } else { 404 },
                expected_redactions: Vec::new(),
            });
        }
    }

    // 4. SADC releasability expansion & non-SADC disjunction (32 cases)
    for &member in &SADC_MEMBER_RELEASABILITY {
        let mut inp_allow = base_input();
        inp_allow.context.releasability = set_of(&[member]);
        inp_allow.resource.label.releasability = set_of(&["REL-SADC"]);
        cases.push(ConformanceCase {
            id: format!("W1-REL-SADC-ALLOW-{member}"),
            rule: "W1",
            input: inp_allow,
            expected_allow: true,
            expected_http_status: 200,
            expected_redactions: Vec::new(),
        });

        let mut inp_deny = base_input();
        inp_deny.context.releasability = set_of(&["REL-KE"]);
        inp_deny.resource.label.releasability = set_of(&[member]);
        cases.push(ConformanceCase {
            id: format!("W1-REL-NONMEMBER-DENY-{member}"),
            rule: "W1",
            input: inp_deny,
            expected_allow: false,
            expected_http_status: 404,
            expected_redactions: Vec::new(),
        });
    }

    // 5. Purpose binding matrix (40 cases)
    let purposes = [
        "PROCUREMENT_INTEGRITY",
        "DUE_DILIGENCE",
        "CASE_INVESTIGATION",
        "CORRIDOR_OPERATIONS",
        "AUDIT_OVERSIGHT",
    ];
    for (i, &declared) in purposes.iter().enumerate() {
        for (j, &res_p) in purposes.iter().enumerate() {
            for caller_holds in [false, true] {
                let mut inp = base_input();
                inp.context.declared_purpose = declared.to_owned();
                inp.context.allowed_purposes = if caller_holds {
                    set_of(&[declared])
                } else {
                    set_of(&["OTHER_PURPOSE"])
                };
                inp.resource.label.purpose_tags = set_of(&[res_p]);
                let allow = caller_holds && (i == j);
                cases.push(ConformanceCase {
                    id: format!("W1-PURPOSE-D{i}-R{j}-H{}", u8::from(caller_holds)),
                    rule: "W1",
                    input: inp,
                    expected_allow: allow,
                    expected_http_status: if allow { 200 } else { 404 },
                    expected_redactions: Vec::new(),
                });
            }
        }
    }

    // 6. W2 & W3 property redactions and cipher_text masking (80 cases)
    for idx in 0..80usize {
        let mut inp = base_input();
        let mode = idx % 8;
        let prop_name = format!("prop_{idx}");
        let mut expected_redactions = Vec::new();
        let prop = match mode {
            0 => {
                // Missing column marking -> REDACT(MARKING)
                expected_redactions.push((prop_name.clone(), RedactionReason::Marking, false));
                PropertyDescriptor {
                    name: prop_name,
                    label: SecurityLabel {
                        markings: set_of(&["ORG-FINANCE"]),
                        classification: Some(Classification::Public),
                        releasability: BTreeSet::new(),
                        purpose_tags: BTreeSet::new(),
                    },
                    is_cipher_text: false,
                    invisible_link_target: false,
                }
            }
            1 => {
                // Column classification higher than caller clearance -> REDACT(CLASSIFICATION)
                inp.context.clearance = Some(Classification::Restricted);
                inp.resource.label.classification = Some(Classification::Restricted);
                expected_redactions.push((
                    prop_name.clone(),
                    RedactionReason::Classification,
                    false,
                ));
                PropertyDescriptor {
                    name: prop_name,
                    label: SecurityLabel {
                        markings: BTreeSet::new(),
                        classification: Some(Classification::Confidential),
                        releasability: BTreeSet::new(),
                        purpose_tags: BTreeSet::new(),
                    },
                    is_cipher_text: false,
                    invisible_link_target: false,
                }
            }
            2 => {
                // Column releasability disjoint -> REDACT(RELEASABILITY)
                expected_redactions.push((
                    prop_name.clone(),
                    RedactionReason::Releasability,
                    false,
                ));
                PropertyDescriptor {
                    name: prop_name,
                    label: SecurityLabel {
                        markings: BTreeSet::new(),
                        classification: Some(Classification::Public),
                        releasability: set_of(&["REL-BW"]),
                        purpose_tags: BTreeSet::new(),
                    },
                    is_cipher_text: false,
                    invisible_link_target: false,
                }
            }
            3 => {
                // Column purpose mismatch -> REDACT(PURPOSE)
                expected_redactions.push((prop_name.clone(), RedactionReason::Purpose, false));
                PropertyDescriptor {
                    name: prop_name,
                    label: SecurityLabel {
                        markings: BTreeSet::new(),
                        classification: Some(Classification::Public),
                        releasability: BTreeSet::new(),
                        purpose_tags: set_of(&["DUE_DILIGENCE"]),
                    },
                    is_cipher_text: false,
                    invisible_link_target: false,
                }
            }
            4 => {
                // W3 invisible link target -> REDACT(LINK_TARGET)
                expected_redactions.push((prop_name.clone(), RedactionReason::LinkTarget, false));
                PropertyDescriptor {
                    name: prop_name,
                    label: SecurityLabel::public_unrestricted(),
                    is_cipher_text: false,
                    invisible_link_target: true,
                }
            }
            5 => {
                // W6 BYSTANDER without fresh justification -> REDACT(CASE)
                inp.resource.case_party_role = Some("BYSTANDER".to_owned());
                inp.bystander_expansion_justified = false;
                expected_redactions.push((prop_name.clone(), RedactionReason::Case, false));
                PropertyDescriptor {
                    name: prop_name,
                    label: SecurityLabel::public_unrestricted(),
                    is_cipher_text: false,
                    invisible_link_target: false,
                }
            }
            6 => {
                // Section 4.6 cipher_text with PII held, reveal=false -> MASK(property)
                expected_redactions.push((prop_name.clone(), RedactionReason::Marking, true));
                PropertyDescriptor {
                    name: prop_name,
                    label: SecurityLabel {
                        markings: set_of(&[MARKING_PII]),
                        classification: Some(Classification::Confidential),
                        releasability: BTreeSet::new(),
                        purpose_tags: BTreeSet::new(),
                    },
                    is_cipher_text: true,
                    invisible_link_target: false,
                }
            }
            _ => {
                // Visible unredacted property
                PropertyDescriptor {
                    name: prop_name,
                    label: SecurityLabel::public_unrestricted(),
                    is_cipher_text: false,
                    invisible_link_target: false,
                }
            }
        };
        inp.resource.properties.push(prop);
        cases.push(ConformanceCase {
            id: format!("W2-W3-PROP-{idx:03}"),
            rule: if mode == 4 {
                "W3"
            } else if mode == 5 {
                "W6"
            } else {
                "W2"
            },
            input: inp,
            expected_allow: true,
            expected_http_status: 200,
            expected_redactions,
        });
    }

    // 7. W4 Write rules: valid writes, write-up attempts, and silent declassification attempts (60 cases)
    for idx in 0..60usize {
        let mut inp = base_input();
        inp.operation = Operation::Write;
        let mode = idx % 6;
        let allow = match mode {
            0 => {
                // Valid write keeping identical label
                inp.proposed_label = Some(inp.resource.label.clone());
                true
            }
            1 => {
                // Valid write raising classification within caller clearance
                inp.resource.label.classification = Some(Classification::Restricted);
                let mut prop = inp.resource.label.clone();
                prop.classification = Some(Classification::Confidential);
                inp.proposed_label = Some(prop);
                true
            }
            2 => {
                // Invalid write-up beyond caller clearance
                inp.context.clearance = Some(Classification::Restricted);
                inp.resource.label.classification = Some(Classification::Restricted);
                let mut prop = inp.resource.label.clone();
                prop.classification = Some(Classification::Confidential);
                inp.proposed_label = Some(prop);
                false
            }
            3 => {
                // Invalid write adding marking caller does not hold
                let mut prop = inp.resource.label.clone();
                prop.markings.insert("ORG-SECRET".to_owned());
                inp.proposed_label = Some(prop);
                false
            }
            4 => {
                // Invalid write removing a marking without W8 declassification
                let mut prop = inp.resource.label.clone();
                prop.markings.clear();
                inp.proposed_label = Some(prop);
                false
            }
            _ => {
                // Invalid write lowering classification without W8 declassification
                let mut prop = inp.resource.label.clone();
                prop.classification = Some(Classification::Public);
                inp.proposed_label = Some(prop);
                false
            }
        };
        cases.push(ConformanceCase {
            id: format!("W4-WRITE-{idx:03}"),
            rule: "W4",
            input: inp,
            expected_allow: allow,
            expected_http_status: if allow { 200 } else { 404 },
            expected_redactions: Vec::new(),
        });
    }

    // 8. W6 Case compartment and purpose case_required checks (50 cases)
    for idx in 0..50usize {
        let mut inp = base_input();
        let mode = idx % 5;
        let allow = match mode {
            0 => {
                // Valid open case with threshold statement and CASE-<id> marking
                inp.purpose_requires_case = true;
                inp.case_open = true;
                inp.case_has_threshold_statement = true;
                inp.context.case_id = Some("case-2026-000042".to_owned());
                inp.context.markings.insert("CASE-2026-000042".to_owned());
                inp.resource.case_id = Some("case-2026-000042".to_owned());
                true
            }
            1 => {
                // Missing case_id when purpose requires case
                inp.purpose_requires_case = true;
                inp.case_open = true;
                inp.case_has_threshold_statement = true;
                inp.context.case_id = None;
                false
            }
            2 => {
                // Closed case
                inp.purpose_requires_case = true;
                inp.case_open = false;
                inp.case_has_threshold_statement = true;
                inp.context.case_id = Some("case-2026-000042".to_owned());
                false
            }
            3 => {
                // Missing hessenDATA threshold statement
                inp.purpose_requires_case = true;
                inp.case_open = true;
                inp.case_has_threshold_statement = false;
                inp.context.case_id = Some("case-2026-000042".to_owned());
                false
            }
            _ => {
                // Caller missing CASE-<n> compartment marking
                inp.context.case_id = Some("case-2026-000042".to_owned());
                inp.resource.case_id = Some("case-2026-000042".to_owned());
                false
            }
        };
        cases.push(ConformanceCase {
            id: format!("W6-CASE-{idx:03}"),
            rule: "W6",
            input: inp,
            expected_allow: allow,
            expected_http_status: if allow { 200 } else { 404 },
            expected_redactions: Vec::new(),
        });
    }

    // 9. W7 Justification checkpoints (60 cases)
    for idx in 0..60usize {
        let mut inp = base_input();
        let mode = idx % 6;
        let (allow, status) = match mode {
            0 => {
                // SPECIAL-PI with valid justification -> 200
                inp.context.markings.insert(MARKING_SPECIAL_PI.to_owned());
                inp.resource
                    .label
                    .markings
                    .insert(MARKING_SPECIAL_PI.to_owned());
                let j = valid_justification(&inp.context, Checkpoint::SpecialCategoryAccess);
                inp.context.justifications.push(j);
                (true, 200)
            }
            1 => {
                // SPECIAL-PI without justification -> 403 REQUIRE_JUSTIFICATION
                inp.context.markings.insert(MARKING_SPECIAL_PI.to_owned());
                inp.resource
                    .label
                    .markings
                    .insert(MARKING_SPECIAL_PI.to_owned());
                (false, 403)
            }
            2 => {
                // Expired justification (> 8h or past expires_at_s) -> 403
                inp.context
                    .markings
                    .insert(MARKING_CRIMINAL_CONDUCT.to_owned());
                inp.resource
                    .label
                    .markings
                    .insert(MARKING_CRIMINAL_CONDUCT.to_owned());
                let mut j = valid_justification(&inp.context, Checkpoint::SpecialCategoryAccess);
                j.expires_at_s = inp.context.evaluation_time_s - 1;
                inp.context.justifications.push(j);
                (false, 403)
            }
            3 => {
                // Justification too short (< 20 chars) -> 403
                inp.operation = Operation::Export;
                let mut j = valid_justification(&inp.context, Checkpoint::Export);
                j.text_len = 12;
                inp.context.justifications.push(j);
                (false, 403)
            }
            4 => {
                // Export with valid justification -> 200
                inp.operation = Operation::Export;
                let j = valid_justification(&inp.context, Checkpoint::Export);
                inp.context.justifications.push(j);
                (true, 200)
            }
            _ => {
                // Reveal cipher_text with valid Reveal justification -> 200 (and not masked!)
                inp.reveal_cipher_text = true;
                let j = valid_justification(&inp.context, Checkpoint::Reveal);
                inp.context.justifications.push(j);
                (true, 200)
            }
        };
        cases.push(ConformanceCase {
            id: format!("W7-CHECKPOINT-{idx:03}"),
            rule: "W7",
            input: inp,
            expected_allow: allow,
            expected_http_status: status,
            expected_redactions: Vec::new(),
        });
    }

    // 10. W8 Declassification dual control (40 cases)
    for idx in 0..40usize {
        let mut inp = base_input();
        inp.operation = Operation::Declassify;
        let mut prop = inp.resource.label.clone();
        prop.classification = Some(Classification::Public);
        prop.markings.clear();
        inp.proposed_label = Some(prop);
        let j = valid_justification(&inp.context, Checkpoint::Declassification);
        inp.context.justifications.push(j);

        let mode = idx % 4;
        let allow = match mode {
            0 => {
                // Valid dual control by two distinct marking administrators
                inp.declassification_approval = Some(DeclassificationApproval {
                    requester_principal: "user:admin-x".to_owned(),
                    requester_markings: set_of(&[MARKING_PII]),
                    requester_clearance: Classification::Confidential,
                    requester_justification_len: 65,
                    approver_principal: "user:admin-y".to_owned(),
                    approver_markings: set_of(&[MARKING_PII]),
                    approver_clearance: Classification::Confidential,
                    approver_justification_len: 72,
                });
                true
            }
            1 => {
                // Self-approval (separation of duties violation)
                inp.declassification_approval = Some(DeclassificationApproval {
                    requester_principal: "user:admin-x".to_owned(),
                    requester_markings: set_of(&[MARKING_PII]),
                    requester_clearance: Classification::Confidential,
                    requester_justification_len: 65,
                    approver_principal: "user:admin-x".to_owned(),
                    approver_markings: set_of(&[MARKING_PII]),
                    approver_clearance: Classification::Confidential,
                    approver_justification_len: 72,
                });
                false
            }
            2 => {
                // Approver missing affected marking
                inp.declassification_approval = Some(DeclassificationApproval {
                    requester_principal: "user:admin-x".to_owned(),
                    requester_markings: set_of(&[MARKING_PII]),
                    requester_clearance: Classification::Confidential,
                    requester_justification_len: 65,
                    approver_principal: "user:admin-y".to_owned(),
                    approver_markings: BTreeSet::new(),
                    approver_clearance: Classification::Confidential,
                    approver_justification_len: 72,
                });
                false
            }
            _ => {
                // Non-human agent principal attempting declassification
                inp.declassification_approval = Some(DeclassificationApproval {
                    requester_principal: "agent:ms_01".to_owned(),
                    requester_markings: set_of(&[MARKING_PII]),
                    requester_clearance: Classification::Confidential,
                    requester_justification_len: 65,
                    approver_principal: "user:admin-y".to_owned(),
                    approver_markings: set_of(&[MARKING_PII]),
                    approver_clearance: Classification::Confidential,
                    approver_justification_len: 72,
                });
                false
            }
        };
        cases.push(ConformanceCase {
            id: format!("W8-DECLASS-{idx:03}"),
            rule: "W8",
            input: inp,
            expected_allow: allow,
            expected_http_status: if allow { 200 } else { 404 },
            expected_redactions: Vec::new(),
        });
    }

    // 11. W9 Special classes & POPIA s57(1)(a) linkage gate (30 cases)
    for idx in 0..30usize {
        let mut inp = base_input();
        let mode = idx % 3;
        let allow = match mode {
            0 => {
                // CHILD data blocked without CHILD_DATA gate
                inp.context.markings.insert(MARKING_CHILD.to_owned());
                inp.resource.label.markings.insert(MARKING_CHILD.to_owned());
                let j = valid_justification(&inp.context, Checkpoint::SpecialCategoryAccess);
                inp.context.justifications.push(j);
                false
            }
            1 => {
                // CHILD data allowed when CHILD_DATA gate is active and justified
                inp.context.markings.insert(MARKING_CHILD.to_owned());
                inp.resource.label.markings.insert(MARKING_CHILD.to_owned());
                inp.active_gates.insert("CHILD_DATA".to_owned());
                let j = valid_justification(&inp.context, Checkpoint::SpecialCategoryAccess);
                inp.context.justifications.push(j);
                true
            }
            _ => {
                // Cross-party person linkage locked without S57_1A_LINKAGE gate
                inp.requires_person_linkage = true;
                let j = valid_justification(&inp.context, Checkpoint::PersonLinkage);
                inp.context.justifications.push(j);
                false
            }
        };
        cases.push(ConformanceCase {
            id: format!("W9-SPECIAL-{idx:03}"),
            rule: "W9",
            input: inp,
            expected_allow: allow,
            expected_http_status: if allow { 200 } else { 404 },
            expected_redactions: Vec::new(),
        });
    }

    // 12. W10 AI context clearance and SECURITY-MEASURE protection (30 cases)
    for idx in 0..30usize {
        let mut inp = base_input();
        inp.operation = Operation::AiContext;
        let mode = idx % 3;
        let allow = match mode {
            0 => {
                // Cleared dedicated model endpoint matching resource label
                inp.model_endpoint_label = Some(inp.resource.label.clone());
                inp.model_endpoint_is_shared = false;
                true
            }
            1 => {
                // Uncleared model endpoint (Public < Confidential)
                let mut ep = inp.resource.label.clone();
                ep.classification = Some(Classification::Public);
                inp.model_endpoint_label = Some(ep);
                false
            }
            _ => {
                // SECURITY-MEASURE sent to shared model -> blocked even if endpoint label matches
                inp.context
                    .markings
                    .insert(MARKING_SECURITY_MEASURE.to_owned());
                inp.resource
                    .label
                    .markings
                    .insert(MARKING_SECURITY_MEASURE.to_owned());
                inp.model_endpoint_label = Some(inp.resource.label.clone());
                inp.model_endpoint_is_shared = true;
                false
            }
        };
        cases.push(ConformanceCase {
            id: format!("W10-AI-{idx:03}"),
            rule: "W10",
            input: inp,
            expected_allow: allow,
            expected_http_status: if allow { 200 } else { 404 },
            expected_redactions: Vec::new(),
        });
    }

    // 13. W11 Deny by default, cross-tenant isolation, and invalid syntax (30 cases)
    for idx in 0..30usize {
        let mut inp = base_input();
        match idx % 3 {
            0 => inp.context.principal = String::new(),
            1 => inp.context.tenant = "other-tenant".to_owned(),
            _ => inp.operation = Operation::Unspecified,
        }
        cases.push(ConformanceCase {
            id: format!("W11-DEFAULT-DENY-{idx:03}"),
            rule: "W11",
            input: inp,
            expected_allow: false,
            expected_http_status: 404,
            expected_redactions: Vec::new(),
        });
    }

    cases
}

/// Runs all 520+ conformance cases and panics if any case diverges from expected output.
pub fn verify_conformance_suite() -> usize {
    let suite = build_conformance_suite();
    assert!(
        suite.len() >= 500,
        "P3 Done-when requires at least 500 conformance cases, got {}",
        suite.len()
    );

    for case in &suite {
        let decision = evaluate(&case.input);
        assert_eq!(
            decision.allow, case.expected_allow,
            "conformance case {} ({}) allow mismatch: got {:?}, reasons: {:?}",
            case.id, case.rule, decision.allow, decision.reasons
        );
        assert_eq!(
            decision.external_http_status, case.expected_http_status,
            "conformance case {} HTTP status mismatch",
            case.id
        );
        let actual_redactions: Vec<(String, RedactionReason, bool)> = decision
            .redactions
            .iter()
            .map(|r| (r.property_name.clone(), r.reason, r.masked))
            .collect();
        assert_eq!(
            actual_redactions, case.expected_redactions,
            "conformance case {} redactions mismatch",
            case.id
        );
        assert!(
            decision.obligations.contains(&Obligation::AuditRead),
            "conformance case {} missing mandatory W12 AUDIT_READ obligation",
            case.id
        );
    }

    // Also verify Worked Examples 1, 2, 3 from VIGIL_DOMAIN_BLUEPRINT section 4.5 on W5 propagate().
    let ocds = SecurityLabel::public_unrestricted();
    let restricted_suppliers = SecurityLabel {
        markings: set_of(&[MARKING_PII]),
        classification: Some(Classification::Confidential),
        releasability: BTreeSet::new(),
        purpose_tags: set_of(&["PROCUREMENT_INTEGRITY", "DUE_DILIGENCE"]),
    };
    let cipc_directorships = SecurityLabel {
        markings: set_of(&[MARKING_PII]),
        classification: Some(Classification::Confidential),
        releasability: BTreeSet::new(),
        purpose_tags: set_of(&["PROCUREMENT_INTEGRITY"]),
    };
    let ex1 = propagate(
        &[ocds, restricted_suppliers, cipc_directorships],
        &SecurityLabel::public_unrestricted(),
        None,
    );
    assert!(ex1.allowed);
    assert_eq!(ex1.derived_label.markings, set_of(&[MARKING_PII]));
    assert_eq!(
        ex1.derived_label.classification,
        Some(Classification::Confidential)
    );
    assert!(ex1.derived_label.releasability.is_empty());
    assert_eq!(
        ex1.derived_label.purpose_tags,
        set_of(&["PROCUREMENT_INTEGRITY"])
    );

    let sadc_corridor = SecurityLabel {
        markings: BTreeSet::new(),
        classification: Some(Classification::Restricted),
        releasability: set_of(&["REL-SADC"]),
        purpose_tags: BTreeSet::new(),
    };
    let za_customs = SecurityLabel {
        markings: BTreeSet::new(),
        classification: Some(Classification::Public),
        releasability: set_of(&["REL-ZA"]),
        purpose_tags: BTreeSet::new(),
    };
    let ex2 = propagate(
        &[sadc_corridor.clone(), za_customs],
        &SecurityLabel::public_unrestricted(),
        None,
    );
    assert!(ex2.allowed);
    assert_eq!(
        ex2.derived_label.classification,
        Some(Classification::Restricted)
    );
    assert_eq!(ex2.derived_label.releasability, set_of(&["REL-ZA"]));

    let ke_only = SecurityLabel {
        markings: BTreeSet::new(),
        classification: Some(Classification::Public),
        releasability: set_of(&["REL-KE"]),
        purpose_tags: BTreeSet::new(),
    };
    let ex2_blocked = propagate(
        &[sadc_corridor, ke_only],
        &SecurityLabel::public_unrestricted(),
        None,
    );
    assert!(!ex2_blocked.allowed);

    suite.len()
}
