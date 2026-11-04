// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - W12 tamper-evident hash-chained audit ledger (P3 task 3.7)
//! Append-only, hash-chained audit event builder and verifier implementing rule W12
//! (VIGIL_DOMAIN_BLUEPRINT section 4.3, API_DESIGN section B.7).

use crate::model::{Decision, EvaluationInput};
use crate::sha256::{hex_encode, hmac_sha256, sha256_hex};

/// Genesis `prev_hash` anchor for a tenant's audit chain (`seq == 1`).
pub const GENESIS_PREV_HASH: &str =
    "0000000000000000000000000000000000000000000000000000000000000000";

/// Cryptographic hash-chain link and signature for one audit record.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AuditChainLink {
    /// Hex digest of the preceding record in the tenant's audit chain.
    pub prev_hash: String,
    /// SHA-256 hex digest over `prev_hash` and the canonical event fields.
    pub hash: String,
    /// HMAC-SHA-256 signature over `hash` produced by the active audit signing key.
    pub signature: String,
    /// Key version identifier (for example `"audit-k1"`).
    pub signing_key_id: String,
}

/// Immutable W12 audit event emitted on every Warden decision (`ALLOW` and `DENY`).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AuditRecord {
    /// Monotonic per-tenant sequence number starting at 1.
    pub seq: u64,
    /// Tenant slug.
    pub tenant: String,
    /// Event identifier (UUIDv7).
    pub event_id: String,
    /// Event type (for example `"policy.evaluate"`, `"objects.query"`).
    pub event_type: String,
    /// Timestamp (epoch seconds) when the evaluated operation occurred.
    pub occurred_at_s: i64,
    /// Principal that initiated the operation.
    pub actor: String,
    /// Policy-context hash (`pch`) bound to the decision.
    pub context_hash: String,
    /// Declared purpose code.
    pub purpose: String,
    /// Bound case identifier, if any.
    pub case_id: Option<String>,
    /// Target resource RID.
    pub resource_rid: String,
    /// Sorted marking identifiers on the target resource.
    pub marking_ids: Vec<String>,
    /// Decision outcome (`"ALLOW"` or `"DENY"`).
    pub decision: String,
    /// Bound justification ID, if any (never the justification text, W7).
    pub justification_id: Option<String>,
    /// Bound justification text SHA-256, if any (W7).
    pub justification_sha256: Option<String>,
    /// Hash-chain envelope.
    pub chain: AuditChainLink,
}

impl AuditRecord {
    /// Returns the deterministic canonical byte string hashed into `chain.hash`.
    #[must_use]
    pub fn canonical_payload(&self, prev_hash: &str) -> String {
        let mut sorted_markings = self.marking_ids.clone();
        sorted_markings.sort();
        format!(
            "v1|prev={}|seq={}|tenant={}|event_id={}|type={}|at={}|actor={}|pch={}|purpose={}|case={}|rid={}|markings={}|decision={}|jid={}|jsha={}",
            prev_hash,
            self.seq,
            self.tenant,
            self.event_id,
            self.event_type,
            self.occurred_at_s,
            self.actor,
            self.context_hash,
            self.purpose,
            self.case_id.as_deref().unwrap_or(""),
            self.resource_rid,
            sorted_markings.join(","),
            self.decision,
            self.justification_id.as_deref().unwrap_or(""),
            self.justification_sha256.as_deref().unwrap_or(""),
        )
    }
}

/// Error returned when verifying a tenant's hash-chained audit sequence.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum AuditChainError {
    /// Sequence number is not strictly contiguous starting from the expected index.
    SequenceGap { expected: u64, actual: u64 },
    /// `prev_hash` does not match the preceding event's `hash`.
    BrokenPrevHash { seq: u64 },
    /// Recomputed `hash` does not match stored `chain.hash`.
    HashMismatch { seq: u64 },
    /// Recomputed signature does not match stored `chain.signature`.
    SignatureMismatch { seq: u64 },
}

/// Append-only, in-memory hash-chained audit ledger for a single tenant partition.
#[derive(Debug, Clone)]
pub struct AuditLedger {
    tenant: String,
    signing_key_id: String,
    signing_key: Vec<u8>,
    records: Vec<AuditRecord>,
}

impl AuditLedger {
    /// Creates a new tenant audit ledger anchored at [`GENESIS_PREV_HASH`].
    #[must_use]
    pub fn new(
        tenant: impl Into<String>,
        signing_key_id: impl Into<String>,
        signing_key: &[u8],
    ) -> Self {
        Self {
            tenant: tenant.into(),
            signing_key_id: signing_key_id.into(),
            signing_key: signing_key.to_vec(),
            records: Vec::new(),
        }
    }

    /// Appends a W12 audit event for `(input, decision)` and returns a reference to the chained record.
    pub fn record_decision(
        &mut self,
        event_id: impl Into<String>,
        event_type: impl Into<String>,
        input: &EvaluationInput,
        decision: &Decision,
    ) -> &AuditRecord {
        let seq = (self.records.len() as u64) + 1;
        let prev_hash = self
            .records
            .last()
            .map_or_else(|| GENESIS_PREV_HASH.to_owned(), |r| r.chain.hash.clone());
        let first_just = input.context.justifications.first();
        let marking_ids: Vec<String> = input.resource.label.markings.iter().cloned().collect();

        let mut record = AuditRecord {
            seq,
            tenant: self.tenant.clone(),
            event_id: event_id.into(),
            event_type: event_type.into(),
            occurred_at_s: input.context.evaluation_time_s,
            actor: input.context.principal.clone(),
            context_hash: input.context.context_hash(),
            purpose: input.context.declared_purpose.clone(),
            case_id: input.context.case_id.clone(),
            resource_rid: input.resource.rid.clone(),
            marking_ids,
            decision: if decision.allow {
                "ALLOW".to_owned()
            } else {
                "DENY".to_owned()
            },
            justification_id: first_just.map(|j| j.justification_id.clone()),
            justification_sha256: first_just.map(|j| j.text_sha256.clone()),
            chain: AuditChainLink {
                prev_hash: prev_hash.clone(),
                hash: String::new(),
                signature: String::new(),
                signing_key_id: self.signing_key_id.clone(),
            },
        };
        let payload = record.canonical_payload(&prev_hash);
        let hash = sha256_hex(payload.as_bytes());
        let sig = hex_encode(&hmac_sha256(&self.signing_key, hash.as_bytes()));
        record.chain.hash = hash;
        record.chain.signature = sig;
        self.records.push(record);
        &self.records[self.records.len() - 1]
    }

    /// Returns all committed audit records in sequence order.
    #[must_use]
    pub fn records(&self) -> &[AuditRecord] {
        &self.records
    }

    /// Cryptographically verifies the entire hash chain and signatures.
    pub fn verify(&self) -> Result<(), AuditChainError> {
        verify_chain(&self.records, &self.signing_key)
    }
}

/// Verifies a slice of [`AuditRecord`] items against `signing_key`.
pub fn verify_chain(records: &[AuditRecord], signing_key: &[u8]) -> Result<(), AuditChainError> {
    let mut expected_prev = GENESIS_PREV_HASH;
    for (idx, rec) in records.iter().enumerate() {
        let expected_seq = (idx as u64) + 1;
        if rec.seq != expected_seq {
            return Err(AuditChainError::SequenceGap {
                expected: expected_seq,
                actual: rec.seq,
            });
        }
        if rec.chain.prev_hash != expected_prev {
            return Err(AuditChainError::BrokenPrevHash { seq: rec.seq });
        }
        let recomputed_hash = sha256_hex(rec.canonical_payload(expected_prev).as_bytes());
        if rec.chain.hash != recomputed_hash {
            return Err(AuditChainError::HashMismatch { seq: rec.seq });
        }
        let recomputed_sig = hex_encode(&hmac_sha256(signing_key, recomputed_hash.as_bytes()));
        if rec.chain.signature != recomputed_sig {
            return Err(AuditChainError::SignatureMismatch { seq: rec.seq });
        }
        expected_prev = &rec.chain.hash;
    }
    Ok(())
}
