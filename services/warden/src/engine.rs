// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Warden W1-W12 policy evaluation and label propagation engine (P3 task 3.2)
//! Pure, deterministic implementation of rules W1 through W12 (VIGIL_DOMAIN_BLUEPRINT section 4.3).

use std::collections::BTreeSet;

use crate::model::{
    Checkpoint, Classification, Decision, DenyReason, EvaluationInput, JustificationGrant,
    MARKING_CHILD, MARKING_CRIMINAL_CONDUCT, MARKING_SECURITY_MEASURE, MARKING_SPECIAL_PI,
    Obligation, Operation, PolicyContext, PropagationResult, PropertyRedaction, RedactionReason,
    SecurityLabel, case_compartment_marking, expand_releasability, is_valid_marking_id,
};

/// Maximum W7 justification lifetime in seconds (8 hours, section 4.3 W7).
pub const MAX_JUSTIFICATION_TTL_S: i64 = 8 * 3600;
/// Minimum W7 justification character length.
pub const MIN_JUSTIFICATION_LEN: usize = 20;
/// Maximum W7 justification character length.
pub const MAX_JUSTIFICATION_LEN: usize = 2000;
/// Default minimum aggregate cell count for export suppression (section 4.7).
pub const DEFAULT_MIN_CELL_COUNT: u32 = 10;

/// Checks whether `justification` is valid for `(ctx, checkpoint, object_type, rid)`.
#[must_use]
pub fn is_valid_justification(
    j: &JustificationGrant,
    ctx: &PolicyContext,
    checkpoint: Checkpoint,
    object_type: &str,
    rid: &str,
) -> bool {
    if j.checkpoint != checkpoint {
        return false;
    }
    if j.principal != ctx.principal || j.purpose != ctx.declared_purpose {
        return false;
    }
    if j.case_id != ctx.case_id {
        return false;
    }
    if j.text_len < MIN_JUSTIFICATION_LEN || j.text_len > MAX_JUSTIFICATION_LEN {
        return false;
    }
    if j.expires_at_s < j.issued_at_s || (j.expires_at_s - j.issued_at_s) > MAX_JUSTIFICATION_TTL_S
    {
        return false;
    }
    if ctx.evaluation_time_s < j.issued_at_s || ctx.evaluation_time_s > j.expires_at_s {
        return false;
    }
    if !j.allowed_object_types.is_empty() && !j.allowed_object_types.contains(object_type) {
        return false;
    }
    if !j.allowed_rids.is_empty() && !j.allowed_rids.contains(rid) {
        return false;
    }
    true
}

fn check_label_against_subject(
    subject_markings: &BTreeSet<String>,
    subject_clearance: Classification,
    subject_rel_expanded: &BTreeSet<String>,
    declared_purpose: &str,
    subject_allowed_purposes: &BTreeSet<String>,
    label: &SecurityLabel,
    max_classification: Classification,
) -> Vec<DenyReason> {
    let mut reasons = Vec::new();

    for m in &label.markings {
        if !is_valid_marking_id(m) {
            reasons.push(DenyReason {
                rule: "W1",
                component: "MARKING",
                detail: format!("invalid marking syntax '{m}'"),
            });
        } else if !subject_markings.contains(m) {
            reasons.push(DenyReason {
                rule: "W1",
                component: "MARKING",
                detail: format!("missing required marking '{m}'"),
            });
        }
    }

    match label.classification {
        None => {
            reasons.push(DenyReason {
                rule: "W11",
                component: "CLASSIFICATION",
                detail: "resource classification is unspecified".to_owned(),
            });
        }
        Some(c) => {
            if c > max_classification {
                reasons.push(DenyReason {
                    rule: "W1",
                    component: "CLASSIFICATION",
                    detail: format!(
                        "classification {} exceeds deployment ceiling {}",
                        c.level(),
                        max_classification.level()
                    ),
                });
            }
            if subject_clearance < c {
                reasons.push(DenyReason {
                    rule: "W1",
                    component: "CLASSIFICATION",
                    detail: format!(
                        "clearance {} < classification {}",
                        subject_clearance.level(),
                        c.level()
                    ),
                });
            }
        }
    }

    if !label.releasability.is_empty() {
        let res_rel = expand_releasability(&label.releasability);
        if res_rel.is_disjoint(subject_rel_expanded) {
            reasons.push(DenyReason {
                rule: "W1",
                component: "RELEASABILITY",
                detail: "caller releasability does not intersect resource releasability".to_owned(),
            });
        }
    }

    if !subject_allowed_purposes.contains(declared_purpose) {
        reasons.push(DenyReason {
            rule: "W1",
            component: "PURPOSE",
            detail: format!("declared purpose '{declared_purpose}' is not held by caller"),
        });
    } else if !label.purpose_tags.is_empty() && !label.purpose_tags.contains(declared_purpose) {
        reasons.push(DenyReason {
            rule: "W1",
            component: "PURPOSE",
            detail: format!(
                "declared purpose '{declared_purpose}' is not permitted by resource purpose_tags"
            ),
        });
    }

    reasons
}

fn is_as_restrictive_as(
    proposed: &SecurityLabel,
    current: &SecurityLabel,
) -> Result<(), DenyReason> {
    if !current.markings.is_subset(&proposed.markings) {
        return Err(DenyReason {
            rule: "W4",
            component: "DECLASSIFICATION",
            detail: "proposed write removes one or more markings without W8 declassification"
                .to_owned(),
        });
    }
    let (Some(prop_c), Some(curr_c)) = (proposed.classification, current.classification) else {
        return Err(DenyReason {
            rule: "W11",
            component: "CLASSIFICATION",
            detail: "unspecified classification on write".to_owned(),
        });
    };
    if prop_c < curr_c {
        return Err(DenyReason {
            rule: "W4",
            component: "DECLASSIFICATION",
            detail: "proposed write lowers classification without W8 declassification".to_owned(),
        });
    }
    if !current.releasability.is_empty() {
        if proposed.releasability.is_empty() {
            return Err(DenyReason {
                rule: "W4",
                component: "DECLASSIFICATION",
                detail: "proposed write widens releasability to unrestricted without W8".to_owned(),
            });
        }
        let curr_rel = expand_releasability(&current.releasability);
        let prop_rel = expand_releasability(&proposed.releasability);
        if !prop_rel.is_subset(&curr_rel) {
            return Err(DenyReason {
                rule: "W4",
                component: "DECLASSIFICATION",
                detail: "proposed write widens releasability principals without W8".to_owned(),
            });
        }
    }
    if !current.purpose_tags.is_empty()
        && (proposed.purpose_tags.is_empty()
            || !proposed.purpose_tags.is_subset(&current.purpose_tags))
    {
        return Err(DenyReason {
            rule: "W4",
            component: "DECLASSIFICATION",
            detail: "proposed write widens purpose tags without W8".to_owned(),
        });
    }
    Ok(())
}

/// Evaluates rules W1 through W12 on `input` and returns a deterministic [`Decision`].
#[must_use]
pub fn evaluate(input: &EvaluationInput) -> Decision {
    let ctx = &input.context;
    let res = &input.resource;
    let mut reasons = Vec::new();
    let mut obligations = vec![Obligation::AuditRead];

    if res.label.markings.contains(MARKING_SECURITY_MEASURE) || res.no_ai_obligation {
        obligations.push(Obligation::NoAi);
    }
    if res.label.markings.contains(MARKING_SECURITY_MEASURE) {
        obligations.push(Obligation::NoExport);
    }
    if input.operation == Operation::Export {
        obligations.push(Obligation::SuppressSmallCells(DEFAULT_MIN_CELL_COUNT));
        if let Some(terms) = &res.attribution_terms_ref
            && !terms.is_empty()
        {
            obligations.push(Obligation::Attribute(terms.clone()));
        }
    }

    // W11: Deny by default on missing or cross-tenant context.
    if ctx.principal.trim().is_empty()
        || ctx.tenant.trim().is_empty()
        || ctx.declared_purpose.trim().is_empty()
        || input.operation == Operation::Unspecified
    {
        reasons.push(DenyReason {
            rule: "W11",
            component: "CONTEXT",
            detail: "missing principal, tenant, declared purpose, or operation".to_owned(),
        });
        return Decision {
            allow: false,
            reasons,
            redactions: Vec::new(),
            obligations,
            external_http_status: 404,
        };
    }

    if ctx.tenant != res.tenant {
        reasons.push(DenyReason {
            rule: "W11",
            component: "CONTEXT",
            detail: "cross-tenant evaluation is forbidden".to_owned(),
        });
        return Decision {
            allow: false,
            reasons,
            redactions: Vec::new(),
            obligations,
            external_http_status: 404,
        };
    }

    let Some(caller_clearance) = ctx.clearance else {
        reasons.push(DenyReason {
            rule: "W11",
            component: "CLASSIFICATION",
            detail: "caller clearance is unspecified".to_owned(),
        });
        return Decision {
            allow: false,
            reasons,
            redactions: Vec::new(),
            obligations,
            external_http_status: 404,
        };
    };

    for m in &ctx.markings {
        if !is_valid_marking_id(m) {
            reasons.push(DenyReason {
                rule: "W11",
                component: "MARKING",
                detail: format!("invalid caller marking syntax '{m}'"),
            });
        }
    }

    let caller_rel = expand_releasability(&ctx.releasability);

    // W1: Base label evaluation (M, C, REL, P).
    reasons.extend(check_label_against_subject(
        &ctx.markings,
        caller_clearance,
        &caller_rel,
        &ctx.declared_purpose,
        &ctx.allowed_purposes,
        &res.label,
        input.max_classification,
    ));

    // W1: Discretionary check (OpenFGA).
    if !input.discretionary_allowed {
        reasons.push(DenyReason {
            rule: "W1",
            component: "DISCRETIONARY",
            detail: "OpenFGA discretionary check denied".to_owned(),
        });
    }

    // W1 & W9: Statutory and gated-module checks.
    for gate in &res.required_gates {
        if !input.active_gates.contains(gate) {
            reasons.push(DenyReason {
                rule: "W1",
                component: "GATE",
                detail: format!("required gate '{gate}' is not active"),
            });
        }
    }
    if res.label.markings.contains(MARKING_CHILD) && !input.active_gates.contains("CHILD_DATA") {
        reasons.push(DenyReason {
            rule: "W9",
            component: "GATE",
            detail: "CHILD data requires active CHILD_DATA gate".to_owned(),
        });
    }
    if input.requires_person_linkage && !input.active_gates.contains("S57_1A_LINKAGE") {
        reasons.push(DenyReason {
            rule: "W9",
            component: "GATE",
            detail: "cross-party person linkage is locked until POPIA s57(1)(a) gate S57_1A_LINKAGE is active".to_owned(),
        });
    }

    // W6: Purpose and case binding.
    if input.purpose_requires_case {
        match &ctx.case_id {
            None => reasons.push(DenyReason {
                rule: "W6",
                component: "CASE",
                detail: "declared purpose requires an active case_id".to_owned(),
            }),
            Some(cid) if cid.trim().is_empty() => reasons.push(DenyReason {
                rule: "W6",
                component: "CASE",
                detail: "declared purpose requires a non-empty case_id".to_owned(),
            }),
            Some(_) => {
                if !input.case_open {
                    reasons.push(DenyReason {
                        rule: "W6",
                        component: "CASE",
                        detail: "bound case must have status OPEN".to_owned(),
                    });
                }
                if !input.case_has_threshold_statement {
                    reasons.push(DenyReason {
                        rule: "W6",
                        component: "CASE",
                        detail: "bound case is missing a recorded threshold statement".to_owned(),
                    });
                }
            }
        }
    }

    if let Some(res_case_id) = &res.case_id
        && !res_case_id.trim().is_empty()
    {
        let expected_marking = case_compartment_marking(res_case_id);
        if !ctx.markings.contains(&expected_marking) {
            reasons.push(DenyReason {
                rule: "W6",
                component: "CASE",
                detail: format!("missing case compartment marking '{expected_marking}'"),
            });
        }
        if ctx.case_id.as_deref() != Some(res_case_id.as_str()) {
            reasons.push(DenyReason {
                rule: "W6",
                component: "CASE",
                detail: "request case_id does not match resource case compartment".to_owned(),
            });
        }
    }

    // W4: Write rules.
    if input.operation == Operation::Write {
        match &input.proposed_label {
            None => reasons.push(DenyReason {
                rule: "W4",
                component: "WRITE",
                detail: "WRITE operation requires proposed_label".to_owned(),
            }),
            Some(proposed) => {
                for m in &proposed.markings {
                    if !is_valid_marking_id(m) || !ctx.markings.contains(m) {
                        reasons.push(DenyReason {
                            rule: "W4",
                            component: "MARKING",
                            detail: format!("cannot write marking '{m}' not held by caller"),
                        });
                    }
                }
                if proposed.markings.contains(MARKING_CHILD)
                    && !input.active_gates.contains("CHILD_DATA")
                {
                    reasons.push(DenyReason {
                        rule: "W9",
                        component: "GATE",
                        detail: "writing CHILD marking requires active CHILD_DATA gate".to_owned(),
                    });
                }
                match proposed.classification {
                    None => reasons.push(DenyReason {
                        rule: "W4",
                        component: "CLASSIFICATION",
                        detail: "proposed classification is unspecified".to_owned(),
                    }),
                    Some(prop_c) => {
                        if prop_c > caller_clearance {
                            reasons.push(DenyReason {
                                rule: "W4",
                                component: "CLASSIFICATION",
                                detail: "write-up beyond caller clearance is forbidden".to_owned(),
                            });
                        }
                        if prop_c > input.max_classification {
                            reasons.push(DenyReason {
                                rule: "W4",
                                component: "CLASSIFICATION",
                                detail: "proposed classification exceeds deployment ceiling"
                                    .to_owned(),
                            });
                        }
                    }
                }
                if let Err(declass_err) = is_as_restrictive_as(proposed, &res.label) {
                    reasons.push(declass_err);
                }
            }
        }
    }

    // W8: Declassification (dual control).
    if input.operation == Operation::Declassify {
        match (&input.proposed_label, &input.declassification_approval) {
            (Some(proposed), Some(approval)) => {
                if !approval.requester_principal.starts_with("user:")
                    || !approval.approver_principal.starts_with("user:")
                {
                    reasons.push(DenyReason {
                        rule: "W8",
                        component: "DECLASSIFICATION",
                        detail: "declassification requires human user: principals".to_owned(),
                    });
                }
                if approval.requester_principal == approval.approver_principal {
                    reasons.push(DenyReason {
                        rule: "W8",
                        component: "DECLASSIFICATION",
                        detail:
                            "SEPARATION_OF_DUTIES: requester cannot approve own declassification"
                                .to_owned(),
                    });
                }
                let mut affected_markings = res.label.markings.clone();
                affected_markings.extend(proposed.markings.iter().cloned());
                if !affected_markings.is_subset(&approval.requester_markings)
                    || !affected_markings.is_subset(&approval.approver_markings)
                {
                    reasons.push(DenyReason {
                        rule: "W8",
                        component: "DECLASSIFICATION",
                        detail: "both marking administrators must hold all affected markings"
                            .to_owned(),
                    });
                }
                if let Some(curr_c) = res.label.classification
                    && (approval.requester_clearance < curr_c
                        || approval.approver_clearance < curr_c)
                {
                    reasons.push(DenyReason {
                        rule: "W8",
                        component: "DECLASSIFICATION",
                        detail: "both marking administrators must have clearance >= resource classification".to_owned(),
                    });
                }
                if !(MIN_JUSTIFICATION_LEN..=MAX_JUSTIFICATION_LEN)
                    .contains(&approval.requester_justification_len)
                    || !(MIN_JUSTIFICATION_LEN..=MAX_JUSTIFICATION_LEN)
                        .contains(&approval.approver_justification_len)
                {
                    reasons.push(DenyReason {
                        rule: "W8",
                        component: "JUSTIFICATION",
                        detail:
                            "both requester and approver must provide 20..=2000 char justifications"
                                .to_owned(),
                    });
                }
            }
            _ => reasons.push(DenyReason {
                rule: "W8",
                component: "DECLASSIFICATION",
                detail: "DECLASSIFY requires proposed_label and dual-control approval record"
                    .to_owned(),
            }),
        }
    }

    // W10: AI model context evaluation.
    if input.operation == Operation::AiContext {
        if res.no_ai_obligation {
            reasons.push(DenyReason {
                rule: "W10",
                component: "AI",
                detail: "NO_AI obligation blocks placement in a model context".to_owned(),
            });
        }
        if res.label.markings.contains(MARKING_SECURITY_MEASURE) && input.model_endpoint_is_shared {
            reasons.push(DenyReason {
                rule: "W10",
                component: "AI",
                detail: "SECURITY-MEASURE content is never sent to shared models".to_owned(),
            });
        }
        match &input.model_endpoint_label {
            None => reasons.push(DenyReason {
                rule: "W10",
                component: "AI",
                detail: "NO_CLEARED_MODEL: missing model_endpoint_label".to_owned(),
            }),
            Some(ep) => {
                let Some(ep_c) = ep.classification else {
                    reasons.push(DenyReason {
                        rule: "W10",
                        component: "AI",
                        detail: "NO_CLEARED_MODEL: model endpoint classification unspecified"
                            .to_owned(),
                    });
                    return Decision {
                        allow: false,
                        reasons,
                        redactions: Vec::new(),
                        obligations,
                        external_http_status: 403,
                    };
                };
                let ep_rel = expand_releasability(&ep.releasability);
                let ep_reasons = check_label_against_subject(
                    &ep.markings,
                    ep_c,
                    &ep_rel,
                    &ctx.declared_purpose,
                    &ep.purpose_tags,
                    &res.label,
                    input.max_classification,
                );
                if !ep_reasons.is_empty() {
                    reasons.push(DenyReason {
                        rule: "W10",
                        component: "AI",
                        detail:
                            "NO_CLEARED_MODEL: model endpoint is not cleared for resource label"
                                .to_owned(),
                    });
                }
            }
        }
    }

    if input.operation == Operation::Export && res.label.markings.contains(MARKING_SECURITY_MEASURE)
    {
        reasons.push(DenyReason {
            rule: "W1",
            component: "EXPORT",
            detail: "NO_EXPORT obligation blocks exporting SECURITY-MEASURE data".to_owned(),
        });
    }

    // W7: Checkpoints requiring bound justification.
    let mut required_checkpoints = BTreeSet::new();
    if res.label.markings.contains(MARKING_SPECIAL_PI)
        || res.label.markings.contains(MARKING_CRIMINAL_CONDUCT)
        || res.label.markings.contains(MARKING_CHILD)
    {
        required_checkpoints.insert(Checkpoint::SpecialCategoryAccess);
    }
    if input.requires_person_linkage {
        required_checkpoints.insert(Checkpoint::PersonLinkage);
    }
    if input.operation == Operation::Export {
        required_checkpoints.insert(Checkpoint::Export);
    }
    if input.operation == Operation::Declassify {
        required_checkpoints.insert(Checkpoint::Declassification);
    }
    if input.reveal_cipher_text {
        required_checkpoints.insert(Checkpoint::Reveal);
    }

    for cp in required_checkpoints {
        let satisfied = ctx
            .justifications
            .iter()
            .any(|j| is_valid_justification(j, ctx, cp, &res.object_type, &res.rid));
        if !satisfied {
            reasons.push(DenyReason {
                rule: "W7",
                component: "JUSTIFICATION",
                detail: format!(
                    "valid justification required for checkpoint {}",
                    cp.as_str()
                ),
            });
            obligations.push(Obligation::RequireJustification(cp));
        }
    }

    if !reasons.is_empty() {
        let only_checkpoint_missing = reasons.iter().all(|r| r.rule == "W7");
        return Decision {
            allow: false,
            reasons,
            redactions: Vec::new(),
            obligations,
            external_http_status: if only_checkpoint_missing { 403 } else { 404 },
        };
    }

    // W2, W3, W6: Property-level redaction and masking on allowed rows.
    let is_bystander = res.case_party_role.as_deref() == Some("BYSTANDER");
    let mut redactions = Vec::new();
    for prop in &res.properties {
        if prop.invisible_link_target {
            redactions.push(PropertyRedaction {
                property_name: prop.name.clone(),
                reason: RedactionReason::LinkTarget,
                masked: false,
            });
            continue;
        }
        if is_bystander && !input.bystander_expansion_justified {
            redactions.push(PropertyRedaction {
                property_name: prop.name.clone(),
                reason: RedactionReason::Case,
                masked: false,
            });
            continue;
        }
        if !prop.label.markings.is_subset(&ctx.markings)
            || prop.label.markings.iter().any(|m| !is_valid_marking_id(m))
        {
            redactions.push(PropertyRedaction {
                property_name: prop.name.clone(),
                reason: RedactionReason::Marking,
                masked: false,
            });
            continue;
        }
        let Some(prop_c) = prop.label.classification else {
            redactions.push(PropertyRedaction {
                property_name: prop.name.clone(),
                reason: RedactionReason::Classification,
                masked: false,
            });
            continue;
        };
        if caller_clearance < prop_c || prop_c > input.max_classification {
            redactions.push(PropertyRedaction {
                property_name: prop.name.clone(),
                reason: RedactionReason::Classification,
                masked: false,
            });
            continue;
        }
        if !prop.label.releasability.is_empty() {
            let prop_rel = expand_releasability(&prop.label.releasability);
            if prop_rel.is_disjoint(&caller_rel) {
                redactions.push(PropertyRedaction {
                    property_name: prop.name.clone(),
                    reason: RedactionReason::Releasability,
                    masked: false,
                });
                continue;
            }
        }
        if !prop.label.purpose_tags.is_empty()
            && !prop.label.purpose_tags.contains(&ctx.declared_purpose)
        {
            redactions.push(PropertyRedaction {
                property_name: prop.name.clone(),
                reason: RedactionReason::Purpose,
                masked: false,
            });
            continue;
        }
        if prop.is_cipher_text && !input.reveal_cipher_text {
            redactions.push(PropertyRedaction {
                property_name: prop.name.clone(),
                reason: RedactionReason::Marking,
                masked: true,
            });
        }
    }

    Decision {
        allow: true,
        reasons: Vec::new(),
        redactions,
        obligations,
        external_http_status: 200,
    }
}

/// Computes the derived security label `lambda(D)` for `D = f(I_1 ... I_n)` with transform
/// label `transform_label` under rule W5 (VIGIL_DOMAIN_BLUEPRINT section 4.3).
#[must_use]
pub fn propagate(
    inputs: &[SecurityLabel],
    transform_label: &SecurityLabel,
    s15_compatible_purposes: Option<&BTreeSet<String>>,
) -> PropagationResult {
    let mut reasons = Vec::new();
    let mut markings = transform_label.markings.clone();
    let mut max_class = transform_label.classification;

    let mut has_restricted_rel = !transform_label.releasability.is_empty();
    let mut rel_intersection = expand_releasability(&transform_label.releasability);

    let mut has_restricted_purpose = !transform_label.purpose_tags.is_empty();
    let mut purpose_intersection = transform_label.purpose_tags.clone();

    if transform_label.classification.is_none() {
        reasons.push(DenyReason {
            rule: "W5",
            component: "CLASSIFICATION",
            detail: "transform classification is unspecified".to_owned(),
        });
    }

    for (idx, item) in inputs.iter().enumerate() {
        for m in &item.markings {
            if !is_valid_marking_id(m) {
                reasons.push(DenyReason {
                    rule: "W5",
                    component: "MARKING",
                    detail: format!("input {idx} has invalid marking '{m}'"),
                });
            }
            markings.insert(m.clone());
        }
        match (max_class, item.classification) {
            (_, None) => reasons.push(DenyReason {
                rule: "W5",
                component: "CLASSIFICATION",
                detail: format!("input {idx} classification is unspecified"),
            }),
            (None, Some(c)) => max_class = Some(c),
            (Some(curr), Some(c)) if c > curr => max_class = Some(c),
            _ => {}
        }

        if !item.releasability.is_empty() {
            let expanded = expand_releasability(&item.releasability);
            if !has_restricted_rel {
                has_restricted_rel = true;
                rel_intersection = expanded;
            } else {
                rel_intersection = rel_intersection.intersection(&expanded).cloned().collect();
            }
        }

        if !item.purpose_tags.is_empty() {
            if !has_restricted_purpose {
                has_restricted_purpose = true;
                purpose_intersection = item.purpose_tags.clone();
            } else {
                purpose_intersection = purpose_intersection
                    .intersection(&item.purpose_tags)
                    .cloned()
                    .collect();
            }
        }
    }

    if has_restricted_rel && rel_intersection.is_empty() {
        reasons.push(DenyReason {
            rule: "W5",
            component: "RELEASABILITY",
            detail: "releasability intersection is empty; derivation blocked".to_owned(),
        });
    }

    if has_restricted_purpose && purpose_intersection.is_empty() {
        if let Some(compat) = s15_compatible_purposes
            && !compat.is_empty()
        {
            purpose_intersection = compat.clone();
        } else {
            reasons.push(DenyReason {
                rule: "W5",
                component: "PURPOSE",
                detail: "purpose intersection is empty; blocked under POPIA s15 compatibility gate"
                    .to_owned(),
            });
        }
    }

    PropagationResult {
        allowed: reasons.is_empty(),
        derived_label: SecurityLabel {
            markings,
            classification: max_class,
            releasability: if has_restricted_rel {
                rel_intersection
            } else {
                BTreeSet::new()
            },
            purpose_tags: if has_restricted_purpose {
                purpose_intersection
            } else {
                BTreeSet::new()
            },
        },
        reasons,
    }
}
