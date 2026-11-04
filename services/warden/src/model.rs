// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Warden marking model, classification ladder, and W1-W12 policy types (P3 task 3.1)
//! Data structures for the Warden security kernel (VIGIL_DOMAIN_BLUEPRINT section 4).

use std::collections::BTreeSet;

use crate::sha256::sha256_hex;

/// Reserved marking for personal information of a natural person (POPIA s1, s11).
pub const MARKING_PII: &str = "PII";
/// Reserved marking for special personal information (POPIA s26(a)).
pub const MARKING_SPECIAL_PI: &str = "SPECIAL-PI";
/// Reserved marking for alleged offences and criminal proceedings (POPIA s26(b), s33).
pub const MARKING_CRIMINAL_CONDUCT: &str = "CRIMINAL-CONDUCT";
/// Reserved marking for personal information of children (POPIA s34-35).
pub const MARKING_CHILD: &str = "CHILD";
/// Reserved marking for critical infrastructure security arrangements (CIPA s26).
pub const MARKING_SECURITY_MEASURE: &str = "SECURITY-MEASURE";

/// SADC regional releasability group token.
pub const REL_SADC: &str = "REL-SADC";

/// The 16 SADC member state releasability tokens (VIGIL_DOMAIN_BLUEPRINT section 4.5 Example 2).
pub const SADC_MEMBER_RELEASABILITY: [&str; 16] = [
    "REL-AO", "REL-BW", "REL-CD", "REL-KM", "REL-LS", "REL-MG", "REL-MU", "REL-MW", "REL-MZ",
    "REL-NA", "REL-SC", "REL-SZ", "REL-TZ", "REL-ZA", "REL-ZM", "REL-ZW",
];

/// Canonical classification ladder (VIGIL_DOMAIN_BLUEPRINT section 4.2).
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
#[repr(u8)]
pub enum Classification {
    /// Level 0: public and open data.
    Public = 0,
    /// Level 1: internal operational data and commercial terminal parameters.
    Restricted = 1,
    /// Level 2: personal information, case files, and restricted-supplier person entries.
    Confidential = 2,
    /// Level 3: classified-enclave data only (P41).
    Secret = 3,
    /// Level 4: classified-enclave data only (P41).
    TopSecret = 4,
}

impl Classification {
    /// Converts a numeric classification level (0..=4) into a [`Classification`].
    /// Any out-of-range or negative value returns `None` (W11 deny-by-default).
    #[must_use]
    pub const fn from_level(level: i32) -> Option<Self> {
        match level {
            0 => Some(Self::Public),
            1 => Some(Self::Restricted),
            2 => Some(Self::Confidential),
            3 => Some(Self::Secret),
            4 => Some(Self::TopSecret),
            _ => None,
        }
    }

    /// Numeric classification level (0..=4).
    #[must_use]
    pub const fn level(self) -> u8 {
        self as u8
    }

    /// Canonical API code string.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Public => "PUBLIC",
            Self::Restricted => "RESTRICTED",
            Self::Confidential => "CONFIDENTIAL",
            Self::Secret => "SECRET",
            Self::TopSecret => "TOP_SECRET",
        }
    }
}

/// Validates a marking identifier against `^[A-Z][A-Z0-9-]{1,63}$` (section 4.1).
#[must_use]
pub fn is_valid_marking_id(id: &str) -> bool {
    let bytes = id.as_bytes();
    if bytes.len() < 2 || bytes.len() > 64 {
        return false;
    }
    if !bytes[0].is_ascii_uppercase() {
        return false;
    }
    bytes[1..]
        .iter()
        .all(|&b| b.is_ascii_uppercase() || b.is_ascii_digit() || b == b'-')
}

/// Returns the canonical case compartment marking for `case_id` (`CASE-<UPPER>`).
#[must_use]
pub fn case_compartment_marking(case_id: &str) -> String {
    let normalized: String = case_id
        .chars()
        .map(|c| {
            if c.is_ascii_alphanumeric() {
                c.to_ascii_uppercase()
            } else {
                '-'
            }
        })
        .collect();
    if normalized.starts_with("CASE-") {
        normalized
    } else {
        format!("CASE-{normalized}")
    }
}

/// Expands releasability groups (such as `REL-SADC`) into individual principal tokens.
#[must_use]
pub fn expand_releasability<I, S>(principals: I) -> BTreeSet<String>
where
    I: IntoIterator<Item = S>,
    S: AsRef<str>,
{
    let mut out = BTreeSet::new();
    for item in principals {
        let token = item.as_ref().trim();
        if token.is_empty() {
            continue;
        }
        if token == REL_SADC {
            for member in SADC_MEMBER_RELEASABILITY {
                out.insert((*member).to_owned());
            }
        } else {
            out.insert(token.to_owned());
        }
    }
    out
}

/// Four-part security label `lambda(R) = <M, C, REL, P>` (VIGIL_DOMAIN_BLUEPRINT section 4.1).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SecurityLabel {
    /// Conjunctive markings `M`.
    pub markings: BTreeSet<String>,
    /// Hierarchical classification `C` (`None` means unspecified and fails closed under W11).
    pub classification: Option<Classification>,
    /// Disjunctive releasability `REL` (empty means unrestricted `top`).
    pub releasability: BTreeSet<String>,
    /// Allowed purpose tags `P` (empty means unrestricted `top`).
    pub purpose_tags: BTreeSet<String>,
}

impl SecurityLabel {
    /// Creates an unrestricted `PUBLIC` security label.
    #[must_use]
    pub fn public_unrestricted() -> Self {
        Self {
            markings: BTreeSet::new(),
            classification: Some(Classification::Public),
            releasability: BTreeSet::new(),
            purpose_tags: BTreeSet::new(),
        }
    }
}

/// Operation evaluated by the Warden policy core.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Operation {
    /// Unspecified operation (fails closed under W11).
    Unspecified,
    /// W1/W2/W3 read access.
    Read,
    /// W4 create or update resource.
    Write,
    /// W7/W12 export or evidence bundle.
    Export,
    /// W10 placement in an AI model context.
    AiContext,
    /// W8 dual-control declassification.
    Declassify,
}

impl Operation {
    /// Canonical string representation.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Unspecified => "UNSPECIFIED",
            Self::Read => "READ",
            Self::Write => "WRITE",
            Self::Export => "EXPORT",
            Self::AiContext => "AI_CONTEXT",
            Self::Declassify => "DECLASSIFY",
        }
    }
}

/// W7 checkpoint classes requiring a bound justification (section 4.3).
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub enum Checkpoint {
    /// First access to `SPECIAL-PI`, `CRIMINAL-CONDUCT`, or `CHILD` data.
    SpecialCategoryAccess,
    /// Person-level cross-source linkage.
    PersonLinkage,
    /// Exporting data or evidence packs.
    Export,
    /// W8 declassification request or approval.
    Declassification,
    /// Revealing `cipher_text` values in clear.
    Reveal,
}

impl Checkpoint {
    /// Canonical API code string.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::SpecialCategoryAccess => "SPECIAL_CATEGORY_ACCESS",
            Self::PersonLinkage => "PERSON_LINKAGE",
            Self::Export => "EXPORT",
            Self::Declassification => "DECLASSIFICATION",
            Self::Reveal => "REVEAL",
        }
    }
}

/// Verified W7 justification bound to a request context.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct JustificationGrant {
    /// Justification identifier.
    pub justification_id: String,
    /// Checkpoint class.
    pub checkpoint: Checkpoint,
    /// Principal to whom the justification is bound.
    pub principal: String,
    /// Purpose code to which the justification is bound.
    pub purpose: String,
    /// Case identifier to which the justification is bound, if any.
    pub case_id: Option<String>,
    /// Character length of the justification text (must be 20..=2000).
    pub text_len: usize,
    /// SHA-256 hex digest of the justification text.
    pub text_sha256: String,
    /// Issuance timestamp (epoch seconds).
    pub issued_at_s: i64,
    /// Expiration timestamp (epoch seconds; at most `issued_at_s + 8 * 3600`).
    pub expires_at_s: i64,
    /// Allowed object types in scope (empty means any in the bound purpose/case).
    pub allowed_object_types: BTreeSet<String>,
    /// Allowed resource RIDs in scope (empty means any in the bound purpose/case).
    pub allowed_rids: BTreeSet<String>,
}

/// Verified caller security context `sigma = <M_u, c_u, R_u, p, k, J>`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PolicyContext {
    /// Authenticated principal (`user:<sub>`, `svc:<service>`, `agent:<session>`).
    pub principal: String,
    /// Tenant slug.
    pub tenant: String,
    /// Held markings `M_u`.
    pub markings: BTreeSet<String>,
    /// Maximum clearance `c_u`.
    pub clearance: Option<Classification>,
    /// Releasability principals `R_u`.
    pub releasability: BTreeSet<String>,
    /// Registered purposes the principal may declare.
    pub allowed_purposes: BTreeSet<String>,
    /// Declared request purpose `p`.
    pub declared_purpose: String,
    /// Bound case identifier `k`.
    pub case_id: Option<String>,
    /// Held roles.
    pub roles: BTreeSet<String>,
    /// Evaluation timestamp in epoch seconds.
    pub evaluation_time_s: i64,
    /// Bound W7 justifications.
    pub justifications: Vec<JustificationGrant>,
}

impl PolicyContext {
    /// Computes the deterministic policy-context hash (`pch`, DATA_MODEL section 12).
    #[must_use]
    pub fn context_hash(&self) -> String {
        let markings: Vec<&str> = self.markings.iter().map(String::as_str).collect();
        let rel: Vec<&str> = self.releasability.iter().map(String::as_str).collect();
        let roles: Vec<&str> = self.roles.iter().map(String::as_str).collect();
        let clearance_str = self.clearance.map_or("UNSPECIFIED", Classification::as_str);
        let case_str = self.case_id.as_deref().unwrap_or("");
        let canonical = format!(
            "v1|tenant={}|principal={}|clearance={}|markings={}|rel={}|purpose={}|case={}|roles={}",
            self.tenant,
            self.principal,
            clearance_str,
            markings.join(","),
            rel.join(","),
            self.declared_purpose,
            case_str,
            roles.join(",")
        );
        sha256_hex(canonical.as_bytes())
    }
}

/// Column or property security descriptor evaluated under W2 and W3.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PropertyDescriptor {
    /// Property API name.
    pub name: String,
    /// Column security label.
    pub label: SecurityLabel,
    /// True when stored as `cipher_text` (masked unless `REVEAL` is justified).
    pub is_cipher_text: bool,
    /// True when this property is a foreign key whose target failed W3 visibility.
    pub invisible_link_target: bool,
}

/// Target resource descriptor evaluated by Warden.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ResourceDescriptor {
    /// Resource RID.
    pub rid: String,
    /// Tenant owning the resource.
    pub tenant: String,
    /// Object type API name.
    pub object_type: String,
    /// Row or resource security label `lambda(R)`.
    pub label: SecurityLabel,
    /// Properties evaluated under W2 and W3.
    pub properties: Vec<PropertyDescriptor>,
    /// Case compartment ID when the resource is case-bound (W6).
    pub case_id: Option<String>,
    /// `CaseParty` role (for example `"BYSTANDER"`, `"SUBJECT"`).
    pub case_party_role: Option<String>,
    /// Statutory or operational gates required by the resource (for example `"S57_1A_LINKAGE"`).
    pub required_gates: BTreeSet<String>,
    /// Source attribution terms reference for exports (`ATTRIBUTE(terms_ref)`).
    pub attribution_terms_ref: Option<String>,
    /// Explicit `NO_AI` restriction on the resource.
    pub no_ai_obligation: bool,
}

/// Dual-control approval record for W8 declassification ("Expand Access").
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DeclassificationApproval {
    /// Requesting marking administrator (`user:...`).
    pub requester_principal: String,
    /// Markings held by the requester.
    pub requester_markings: BTreeSet<String>,
    /// Clearance of the requester.
    pub requester_clearance: Classification,
    /// Requester justification length (must be 20..=2000).
    pub requester_justification_len: usize,
    /// Approving marking administrator (`user:...`, must differ from requester).
    pub approver_principal: String,
    /// Markings held by the approver.
    pub approver_markings: BTreeSet<String>,
    /// Clearance of the approver.
    pub approver_clearance: Classification,
    /// Approver justification length (must be 20..=2000).
    pub approver_justification_len: usize,
}

/// Complete input to a pure Warden `evaluate()` call.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EvaluationInput {
    /// Caller policy context `sigma`.
    pub context: PolicyContext,
    /// Operation being attempted.
    pub operation: Operation,
    /// Target resource `R`.
    pub resource: ResourceDescriptor,
    /// Proposed new label `lambda(R')` for `Write` or `Declassify`.
    pub proposed_label: Option<SecurityLabel>,
    /// Active statutory/operational gates enabled for the tenant.
    pub active_gates: BTreeSet<String>,
    /// OpenFGA discretionary role check outcome.
    pub discretionary_allowed: bool,
    /// Deployment classification ceiling (`max_classification`, default `Confidential = 2`).
    pub max_classification: Classification,
    /// True when the declared purpose requires an open case with a threshold statement (W6).
    pub purpose_requires_case: bool,
    /// True when the bound case has status `OPEN`.
    pub case_open: bool,
    /// True when the bound case has a recorded hessenDATA threshold statement.
    pub case_has_threshold_statement: bool,
    /// True when a fresh justification authorizes expanding a `BYSTANDER` node.
    pub bystander_expansion_justified: bool,
    /// True when the operation performs cross-party person-level linkage (W7 + POPIA s57(1)(a)).
    pub requires_person_linkage: bool,
    /// True when the caller requests clear `cipher_text` values (`REVEAL` checkpoint).
    pub reveal_cipher_text: bool,
    /// Cleared label of the target AI model endpoint when `operation == Operation::AiContext` (W10).
    pub model_endpoint_label: Option<SecurityLabel>,
    /// True when the target AI model endpoint is a shared (non-dedicated) model.
    pub model_endpoint_is_shared: bool,
    /// W8 dual-control approval record when `operation == Operation::Declassify`.
    pub declassification_approval: Option<DeclassificationApproval>,
}

/// Reason why a property was redacted or masked (W2, W3, W6).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RedactionReason {
    /// Missing column marking.
    Marking,
    /// Insufficient clearance for column classification.
    Classification,
    /// Disjoint column releasability.
    Releasability,
    /// Declared purpose not in column purpose tags.
    Purpose,
    /// W6 bystander default redaction or case restriction.
    Case,
    /// W3 invisible foreign-key target.
    LinkTarget,
}

impl RedactionReason {
    /// Canonical API code string.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Marking => "MARKING",
            Self::Classification => "CLASSIFICATION",
            Self::Releasability => "RELEASABILITY",
            Self::Purpose => "PURPOSE",
            Self::Case => "CASE",
            Self::LinkTarget => "LINK_TARGET",
        }
    }
}

/// Property-level redaction or mask instruction.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PropertyRedaction {
    /// Property API name.
    pub property_name: String,
    /// Redaction reason.
    pub reason: RedactionReason,
    /// True when the enforcement point should return masked display (`******4521`) instead of null/redacted.
    pub masked: bool,
}

/// Mandatory enforcement obligation (VIGIL_DOMAIN_BLUEPRINT section 4.7).
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Obligation {
    /// Emit a W12 read audit event with row count and marking IDs.
    AuditRead,
    /// Never place the content in an AI model context (W10).
    NoAi,
    /// Exclude the content from exports and evidence packs.
    NoExport,
    /// Suppress aggregate cells below `k` in exports (default `k = 10`).
    SuppressSmallCells(u32),
    /// Carry the source attribution reference on exports.
    Attribute(String),
    /// Refuse until a valid justification for the checkpoint is bound (W7).
    RequireJustification(Checkpoint),
}

/// Structured denial or block reason citing the formal rule (`W1`..`W12`).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DenyReason {
    /// Formal rule code (`W1` through `W12`).
    pub rule: &'static str,
    /// Component that failed (`MARKING`, `CLASSIFICATION`, `RELEASABILITY`, `PURPOSE`, `CASE`, `GATE`, `JUSTIFICATION`, `DECLASSIFICATION`, `AI`, `CONTEXT`).
    pub component: &'static str,
    /// Diagnostic explanation.
    pub detail: String,
}

/// Output of `evaluate(sigma, R)` (P3 task 3.2).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Decision {
    /// True when access is allowed.
    pub allow: bool,
    /// Structured denial reasons (empty when `allow` is true).
    pub reasons: Vec<DenyReason>,
    /// Property redactions and masks to apply when `allow` is true.
    pub redactions: Vec<PropertyRedaction>,
    /// Mandatory obligations for the enforcement point.
    pub obligations: Vec<Obligation>,
    /// HTTP status code surfaced to non-admin callers (`404` on invisible resources per W11).
    pub external_http_status: u16,
}

/// Output of `propagate(inputs, transform_label)` under rule W5.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PropagationResult {
    /// True when the derivation is lawful; false when blocked by an empty `REL` or `P` intersection.
    pub allowed: bool,
    /// Derived security label `lambda(D)`.
    pub derived_label: SecurityLabel,
    /// Block reasons when `allowed` is false.
    pub reasons: Vec<DenyReason>,
}
