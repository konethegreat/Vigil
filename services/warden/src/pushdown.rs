// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Warden pushdown compilers for Postgres RLS/masking, OpenSearch DLS/FLS, and S3 scoped credentials (P3 task 3.6)
//! Compiles Warden security labels and [`PolicyContext`] into store-native enforcement
//! artifacts (DATA_MODEL sections 4.2, 4.5, 4.6, 9, and 10).

use std::collections::BTreeSet;

use crate::engine::evaluate;
use crate::model::{
    Classification, DenyReason, EvaluationInput, Operation, PolicyContext, ResourceDescriptor,
    expand_releasability, is_valid_marking_id,
};
use crate::sha256::{hex_encode, hmac_sha256};

/// Maximum lifetime of a Polaris/S3 scoped storage credential (15 minutes, DATA_MODEL section 9).
pub const MAX_S3_CREDENTIAL_TTL_S: u32 = 900;

/// Column descriptor used by [`compile_postgres_masking_view`].
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ColumnMaskSpec {
    /// Physical SQL column identifier (`^[a-z][a-z0-9_]{0,62}$`).
    pub column_name: String,
    /// Conjunctive column markings required to view the column in clear/masked form.
    pub column_markings: BTreeSet<String>,
    /// Minimum clearance required to view the column.
    pub column_classification: Classification,
}

/// Validates a PostgreSQL schema or table identifier (`^[a-z][a-z0-9_]{0,62}$`).
#[must_use]
pub fn is_valid_sql_identifier(ident: &str) -> bool {
    let bytes = ident.as_bytes();
    if bytes.is_empty() || bytes.len() > 63 {
        return false;
    }
    if !bytes[0].is_ascii_lowercase() {
        return false;
    }
    bytes[1..]
        .iter()
        .all(|&b| b.is_ascii_lowercase() || b.is_ascii_digit() || b == b'_')
}

/// Encodes `bytes` as URL-safe Base64 without `=` padding (RFC 4648 section 5).
#[must_use]
pub fn base64url_encode(bytes: &[u8]) -> String {
    const ALPHABET: &[u8; 64] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    let mut out = String::with_capacity((bytes.len() * 4).div_ceil(3));
    let mut chunks = bytes.chunks_exact(3);
    for chunk in &mut chunks {
        let n = (u32::from(chunk[0]) << 16) | (u32::from(chunk[1]) << 8) | u32::from(chunk[2]);
        out.push(ALPHABET[((n >> 18) & 0x3f) as usize] as char);
        out.push(ALPHABET[((n >> 12) & 0x3f) as usize] as char);
        out.push(ALPHABET[((n >> 6) & 0x3f) as usize] as char);
        out.push(ALPHABET[(n & 0x3f) as usize] as char);
    }
    let rem = chunks.remainder();
    if rem.len() == 1 {
        let n = u32::from(rem[0]) << 16;
        out.push(ALPHABET[((n >> 18) & 0x3f) as usize] as char);
        out.push(ALPHABET[((n >> 12) & 0x3f) as usize] as char);
    } else if rem.len() == 2 {
        let n = (u32::from(rem[0]) << 16) | (u32::from(rem[1]) << 8);
        out.push(ALPHABET[((n >> 18) & 0x3f) as usize] as char);
        out.push(ALPHABET[((n >> 12) & 0x3f) as usize] as char);
        out.push(ALPHABET[((n >> 6) & 0x3f) as usize] as char);
    }
    out
}

/// Builds and signs a PostgreSQL `vigil.policy_ctx` token (`<base64url(payload)>.<key_id>.<hex_hmac>`)
/// consumed by `SET LOCAL vigil.policy_ctx` and `vigil_sec.apply_ctx()` (DATA_MODEL section 4.2).
pub fn sign_postgres_policy_ctx(
    ctx: &PolicyContext,
    expires_at_rfc3339: &str,
    key_id: &str,
    secret: &[u8],
) -> Result<String, &'static str> {
    if secret.len() < 32 {
        return Err("policy_ctx signing secret must be at least 32 bytes");
    }
    let clearance = ctx
        .clearance
        .ok_or("policy_ctx requires a specified clearance")?;
    let markings_json: Vec<String> = ctx.markings.iter().map(|m| format!("\"{m}\"")).collect();
    let expanded_rel = expand_releasability(&ctx.releasability);
    let rel_json: Vec<String> = expanded_rel.iter().map(|r| format!("\"{r}\"")).collect();
    let case_str = ctx.case_id.as_deref().unwrap_or("");
    let payload_json = format!(
        "{{\"markings\":[{}],\"clearance\":{},\"releasability\":[{}],\"purpose\":\"{}\",\"case_id\":\"{}\",\"exp\":\"{}\"}}",
        markings_json.join(","),
        clearance.level(),
        rel_json.join(","),
        ctx.declared_purpose,
        case_str,
        expires_at_rfc3339
    );
    let body = base64url_encode(payload_json.as_bytes());
    let signing_input = format!("{body}.{key_id}");
    let sig = hex_encode(&hmac_sha256(secret, signing_input.as_bytes()));
    Ok(format!("{body}.{key_id}.{sig}"))
}

/// Compiles the canonical PostgreSQL Row-Level Security (RLS) DDL for an object or join table
/// (`DATA_MODEL.md` section 4.5).
pub fn compile_postgres_rls_policies(
    schema: &str,
    table: &str,
    traversal_columns: &[&str],
    include_actions_policies: bool,
) -> Result<String, &'static str> {
    if !is_valid_sql_identifier(schema) || !is_valid_sql_identifier(table) {
        return Err("invalid PostgreSQL schema or table identifier");
    }
    for &col in traversal_columns {
        if !is_valid_sql_identifier(col) {
            return Err("invalid traversal column identifier");
        }
    }
    let fq = format!("{schema}.{table}");
    let mut ddl = String::new();
    ddl.push_str(&format!("ALTER TABLE {fq} ENABLE ROW LEVEL SECURITY;\n"));
    ddl.push_str(&format!("ALTER TABLE {fq} FORCE ROW LEVEL SECURITY;\n"));
    ddl.push_str(&format!(
        "CREATE POLICY p_read ON {fq} FOR SELECT TO vigil_reader\n  \
         USING ((SELECT vigil_sec.ctx_ok()) AND deleted_at IS NULL\n     \
         AND markings <@ (SELECT vigil_sec.ctx_markings())\n     \
         AND classification <= (SELECT vigil_sec.ctx_clearance())\n     \
         AND (releasability = '{{}}' OR releasability && (SELECT vigil_sec.ctx_releasability()))\n     \
         AND (purpose_tags = '{{}}' OR (SELECT vigil_sec.ctx_purpose()) = ANY (purpose_tags)));\n"
    ));

    if include_actions_policies {
        ddl.push_str(&format!(
            "CREATE POLICY p_actions_read ON {fq} FOR SELECT TO vigil_actions\n  \
             USING ((SELECT vigil_sec.ctx_ok())\n     \
             AND markings <@ (SELECT vigil_sec.ctx_markings())\n     \
             AND classification <= (SELECT vigil_sec.ctx_clearance())\n     \
             AND (releasability = '{{}}' OR releasability && (SELECT vigil_sec.ctx_releasability()))\n     \
             AND (purpose_tags = '{{}}' OR (SELECT vigil_sec.ctx_purpose()) = ANY (purpose_tags)));\n"
        ));
        ddl.push_str(&format!(
            "CREATE POLICY p_actions_update ON {fq} FOR UPDATE TO vigil_actions\n  \
             USING ((SELECT vigil_sec.ctx_ok()) AND markings <@ (SELECT vigil_sec.ctx_markings())\n     \
             AND classification <= (SELECT vigil_sec.ctx_clearance()))\n  \
             WITH CHECK ((SELECT vigil_sec.ctx_ok()) AND markings <@ (SELECT vigil_sec.ctx_markings())\n     \
             AND classification <= (SELECT vigil_sec.ctx_clearance())\n     \
             AND (releasability = '{{}}' OR releasability && (SELECT vigil_sec.ctx_releasability())));\n"
        ));
        ddl.push_str(&format!(
            "CREATE POLICY p_actions_insert ON {fq} FOR INSERT TO vigil_actions\n  \
             WITH CHECK ((SELECT vigil_sec.ctx_ok()) AND markings <@ (SELECT vigil_sec.ctx_markings())\n     \
             AND classification <= (SELECT vigil_sec.ctx_clearance())\n     \
             AND (releasability = '{{}}' OR releasability && (SELECT vigil_sec.ctx_releasability())));\n"
        ));
    }

    ddl.push_str(&format!(
        "CREATE POLICY p_indexer ON {fq} FOR ALL TO vigil_indexer USING (true) WITH CHECK (true);\n"
    ));
    ddl.push_str(&format!(
        "CREATE POLICY p_traversal ON {fq} FOR SELECT TO vigil_traversal USING (true);\n"
    ));

    if include_actions_policies {
        ddl.push_str(&format!(
            "GRANT SELECT ON {fq} TO vigil_reader, vigil_actions, vigil_indexer;\n"
        ));
        ddl.push_str(&format!(
            "GRANT INSERT, UPDATE ON {fq} TO vigil_actions, vigil_indexer;\n"
        ));
    } else {
        ddl.push_str(&format!(
            "GRANT SELECT ON {fq} TO vigil_reader, vigil_indexer, vigil_traversal;\n"
        ));
        ddl.push_str(&format!("GRANT INSERT, UPDATE ON {fq} TO vigil_indexer;\n"));
    }

    if !traversal_columns.is_empty() {
        ddl.push_str(&format!(
            "GRANT SELECT ({}) ON {fq} TO vigil_traversal;\n",
            traversal_columns.join(", ")
        ));
    }
    Ok(ddl)
}

/// Compiles a PostgreSQL `security_invoker` column-masking view (`objects_v.<type>`, DATA_MODEL section 4.6).
pub fn compile_postgres_masking_view(
    view_schema: &str,
    view_name: &str,
    source_schema: &str,
    source_table: &str,
    columns: &[ColumnMaskSpec],
) -> Result<String, &'static str> {
    if !is_valid_sql_identifier(view_schema)
        || !is_valid_sql_identifier(view_name)
        || !is_valid_sql_identifier(source_schema)
        || !is_valid_sql_identifier(source_table)
    {
        return Err("invalid schema or table/view identifier");
    }
    if columns.is_empty() {
        return Err("masking view requires at least one column");
    }

    let mut select_exprs = Vec::with_capacity(columns.len() + 1);
    let mut redacted_cases = Vec::new();

    for col in columns {
        if !is_valid_sql_identifier(&col.column_name) {
            return Err("invalid column name in ColumnMaskSpec");
        }
        for m in &col.column_markings {
            if !is_valid_marking_id(m) {
                return Err("invalid column marking identifier");
            }
        }
        if col.column_markings.is_empty() && col.column_classification == Classification::Public {
            select_exprs.push(format!("s.{}", col.column_name));
        } else {
            let mut conds = Vec::new();
            if !col.column_markings.is_empty() {
                let arr: Vec<&str> = col.column_markings.iter().map(String::as_str).collect();
                conds.push(format!(
                    "'{{{}}}'::text[] <@ (SELECT vigil_sec.ctx_markings())",
                    arr.join(",")
                ));
            }
            if col.column_classification > Classification::Public {
                conds.push(format!(
                    "(SELECT vigil_sec.ctx_clearance()) >= {}",
                    col.column_classification.level()
                ));
            }
            let pred = conds.join(" AND ");
            select_exprs.push(format!(
                "CASE WHEN {pred} THEN s.{} END AS {}",
                col.column_name, col.column_name
            ));
            redacted_cases.push(format!(
                "CASE WHEN NOT ({pred}) THEN '{}' END",
                col.column_name
            ));
        }
    }

    if redacted_cases.is_empty() {
        select_exprs.push("'{}'::text[] AS redacted_properties".to_owned());
    } else {
        select_exprs.push(format!(
            "array_remove(ARRAY[{}], NULL) AS redacted_properties",
            redacted_cases.join(", ")
        ));
    }

    Ok(format!(
        "CREATE VIEW {view_schema}.{view_name} WITH (security_barrier = true, security_invoker = true) AS\n\
         SELECT {}\n\
         FROM {source_schema}.{source_table} s;\n\
         GRANT SELECT ON {view_schema}.{view_name} TO vigil_reader;\n",
        select_exprs.join(",\n       ")
    ))
}

/// Compiled OpenSearch Document-Level Security (DLS) and Field-Level Security (FLS) payload (DATA_MODEL section 10).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct OpenSearchPushdown {
    /// JSON boolean filter clause enforcing W1 conjunctive markings, clearance, releasability, and purpose.
    pub dls_query_json: String,
    /// Fields excluded under W2 FLS because the caller is missing one or more column markings or clearance.
    pub excluded_fields: Vec<String>,
}

/// Compiles a [`PolicyContext`] and column marking specs into an [`OpenSearchPushdown`]
/// (DATA_MODEL section 10).
pub fn compile_opensearch_dls_fls(
    ctx: &PolicyContext,
    columns: &[ColumnMaskSpec],
) -> Result<OpenSearchPushdown, &'static str> {
    if ctx.principal.trim().is_empty()
        || ctx.tenant.trim().is_empty()
        || ctx.declared_purpose.trim().is_empty()
    {
        return Err("missing principal, tenant, or declared purpose");
    }
    let clearance = ctx.clearance.ok_or("caller clearance is unspecified")?;
    for m in &ctx.markings {
        if !is_valid_marking_id(m) {
            return Err("invalid caller marking syntax");
        }
    }
    let markings_terms: Vec<String> = ctx.markings.iter().map(|m| format!("\"{m}\"")).collect();
    let expanded_rel = expand_releasability(&ctx.releasability);
    let rel_terms: Vec<String> = expanded_rel.iter().map(|r| format!("\"{r}\"")).collect();

    let dls_query_json = format!(
        "{{\"bool\":{{\"filter\":[\
         {{\"bool\":{{\"should\":[{{\"term\":{{\"markings_count\":0}}}},{{\"terms_set\":{{\"markings\":{{\"terms\":[{}],\"minimum_should_match_field\":\"markings_count\"}}}}}}],\"minimum_should_match\":1}}}},\
         {{\"range\":{{\"classification\":{{\"lte\":{}}}}}}},\
         {{\"bool\":{{\"should\":[{{\"bool\":{{\"must_not\":{{\"exists\":{{\"field\":\"releasability\"}}}}}}}},{{\"terms\":{{\"releasability\":[{}]}}}}],\"minimum_should_match\":1}}}},\
         {{\"bool\":{{\"should\":[{{\"bool\":{{\"must_not\":{{\"exists\":{{\"field\":\"purpose_tags\"}}}}}}}},{{\"term\":{{\"purpose_tags\":\"{}\"}}}}],\"minimum_should_match\":1}}}}\
         ]}}}}",
        markings_terms.join(","),
        clearance.level(),
        rel_terms.join(","),
        ctx.declared_purpose
    );

    let mut excluded_fields = Vec::new();
    for col in columns {
        let missing_marking = !col.column_markings.is_subset(&ctx.markings);
        let insufficient_clearance = clearance < col.column_classification;
        if missing_marking || insufficient_clearance {
            excluded_fields.push(col.column_name.clone());
        }
    }

    Ok(OpenSearchPushdown {
        dls_query_json,
        excluded_fields,
    })
}

/// Access mode for a Warden-compiled S3/SeaweedFS scoped storage credential rule.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum S3AccessMode {
    /// Read-only access to committed dataset objects (`s3:GetObject`, `s3:ListBucket`).
    ReadOnly,
    /// Staging upload access for an OPEN transaction (`s3:PutObject`, `s3:GetObject`).
    StageWrite,
}

/// Scoped storage credential rule vended via Polaris/SeaweedFS after Warden evaluation (DATA_MODEL section 9).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ScopedStorageRule {
    /// Target S3 bucket (`vigil-<pod>-lake`).
    pub bucket: String,
    /// Scoped object key prefix (`<tenant>/_staging/<txn_id>/` or `<tenant>/<pack>/<dataset>/`).
    pub prefix: String,
    /// Allowed S3 actions.
    pub allowed_actions: Vec<String>,
    /// Credential TTL in seconds (at most 900 seconds = 15 minutes).
    pub ttl_seconds: u32,
    /// Bound policy-context hash (`pch`).
    pub context_hash: String,
}

/// Evaluates Warden access and compiles a prefix-scoped S3 credential rule (TTL <= 15 minutes).
pub fn compile_s3_scoped_rule(
    input: &EvaluationInput,
    bucket: &str,
    prefix: &str,
    mode: S3AccessMode,
    ttl_seconds: u32,
) -> Result<ScopedStorageRule, DenyReason> {
    if ttl_seconds == 0 || ttl_seconds > MAX_S3_CREDENTIAL_TTL_S {
        return Err(DenyReason {
            rule: "W1",
            component: "STORAGE",
            detail: format!("S3 credential TTL {ttl_seconds}s exceeds 900s (15m) maximum"),
        });
    }
    if bucket.trim().is_empty() || !prefix.starts_with(&format!("{}/", input.context.tenant)) {
        return Err(DenyReason {
            rule: "W11",
            component: "STORAGE",
            detail: "S3 prefix must be scoped under the caller's tenant prefix".to_owned(),
        });
    }
    let expected_op = match mode {
        S3AccessMode::ReadOnly => Operation::Read,
        S3AccessMode::StageWrite => Operation::Write,
    };
    if input.operation != expected_op {
        return Err(DenyReason {
            rule: "W1",
            component: "STORAGE",
            detail: "EvaluationInput operation does not match S3AccessMode".to_owned(),
        });
    }
    let decision = evaluate(input);
    if !decision.allow {
        return Err(decision.reasons.into_iter().next().unwrap_or(DenyReason {
            rule: "W11",
            component: "STORAGE",
            detail: "access denied by Warden".to_owned(),
        }));
    }
    let allowed_actions = match mode {
        S3AccessMode::ReadOnly => vec!["s3:GetObject".to_owned(), "s3:ListBucket".to_owned()],
        S3AccessMode::StageWrite => vec!["s3:PutObject".to_owned(), "s3:GetObject".to_owned()],
    };
    Ok(ScopedStorageRule {
        bucket: bucket.to_owned(),
        prefix: prefix.to_owned(),
        allowed_actions,
        ttl_seconds,
        context_hash: input.context.context_hash(),
    })
}

/// Checks whether a shadow table swap (`ot_x__shadow` -> `ot_x`) would silently declassify
/// any existing row without an applied W8 `declassify-resource` action (DATA_MODEL section 4.4).
pub fn verify_shadow_swap_labels(
    live_rows: &[ResourceDescriptor],
    shadow_rows: &[ResourceDescriptor],
    declassified_rids: &BTreeSet<String>,
) -> Result<(), DenyReason> {
    for live in live_rows {
        if let Some(shadow) = shadow_rows.iter().find(|r| r.rid == live.rid) {
            let markings_widened = !live.label.markings.is_subset(&shadow.label.markings);
            let class_lowered = match (live.label.classification, shadow.label.classification) {
                (Some(l_c), Some(s_c)) => s_c < l_c,
                _ => true,
            };
            let rel_widened = !live.label.releasability.is_empty()
                && (shadow.label.releasability.is_empty()
                    || !expand_releasability(&shadow.label.releasability)
                        .is_subset(&expand_releasability(&live.label.releasability)));
            let purpose_widened = !live.label.purpose_tags.is_empty()
                && (shadow.label.purpose_tags.is_empty()
                    || !shadow
                        .label
                        .purpose_tags
                        .is_subset(&live.label.purpose_tags));

            if (markings_widened || class_lowered || rel_widened || purpose_widened)
                && !declassified_rids.contains(&live.rid)
            {
                return Err(DenyReason {
                    rule: "W8",
                    component: "SHADOW_SWAP",
                    detail: format!(
                        "shadow table swap widens access on row {} without an applied declassify-resource run",
                        live.rid
                    ),
                });
            }
        }
    }
    Ok(())
}
