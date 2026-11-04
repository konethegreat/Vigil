// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Warden KMS v1 key hierarchy, AES-256-GCM AAD key channels, and key ceremony verification (P3 task 3.8)
//! Key management v1 behind a PKCS#11 / OpenBao Transit-ready interface:
//! - Tenant Key-Encryption Keys (`TenantKek`) and Dataset Data-Encryption Keys (`DatasetDek`).
//! - Class-specific `cipher_text` key channels with canonical Authenticated Additional Data
//!   (`AAD = v1|tenant=<t>|channel=<c>|dataset=<d>|rid=<r>|property=<p>`).
//! - Dual-custodian key ceremony verification for the `policy-ctx` and `audit` signing keys.

use crate::sha256::{hex_encode, hmac_sha256, sha256_hex};

/// Dedicated encryption key channels for `cipher_text` columns and justifications
/// (VIGIL_DOMAIN_BLUEPRINT section 4.4, W7, W9).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum KeyChannel {
    /// Standard `PII` personal information channel.
    Pii,
    /// `SPECIAL-PI` channel (POPIA s26(a); isolated key channel per W9).
    SpecialPi,
    /// `CRIMINAL-CONDUCT` channel (POPIA s26(b), s33; isolated key channel per W9).
    CriminalConduct,
    /// `CHILD` personal information channel (POPIA s34-35; isolated key channel per W9).
    Child,
    /// W7 checkpoint justification text channel (`vigil_sec.justification.text_cipher`).
    Justification,
    /// Iceberg/S3 dataset object encryption channel (P4.7).
    DatasetObject,
}

impl KeyChannel {
    /// Canonical key channel slug used in OpenBao Transit key names and AAD strings.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Pii => "pii",
            Self::SpecialPi => "special-pi",
            Self::CriminalConduct => "criminal-conduct",
            Self::Child => "child",
            Self::Justification => "justification",
            Self::DatasetObject => "dataset-object",
        }
    }
}

/// Builds the canonical AES-256-GCM Authenticated Additional Data (AAD) binding a ciphertext
/// to `(tenant, channel, dataset_rid, resource_rid, property_name)` so a ciphertext cannot be
/// transplanted across tenants, datasets, rows, or columns (P3 task 3.8).
#[must_use]
pub fn build_cipher_text_aad(
    tenant: &str,
    channel: KeyChannel,
    dataset_rid: &str,
    resource_rid: &str,
    property_name: &str,
) -> Vec<u8> {
    format!(
        "v1|tenant={tenant}|channel={}|dataset={dataset_rid}|rid={resource_rid}|property={property_name}",
        channel.as_str()
    )
    .into_bytes()
}

/// Reference to a tenant Key-Encryption Key (KEK) held in OpenBao Transit or a PKCS#11 HSM.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TenantKekRef {
    /// Tenant slug.
    pub tenant: String,
    /// OpenBao or PKCS#11 key URI (never raw key material, DATA_MODEL section 3.1).
    pub kms_key_ref: String,
    /// Monotonic key version.
    pub key_version: u32,
}

/// Wrapped Data-Encryption Key (DEK) for a dataset or key channel.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct WrappedDek {
    /// Owning tenant KEK reference.
    pub kek_ref: TenantKekRef,
    /// Key channel.
    pub channel: KeyChannel,
    /// Dataset RID or `"_system"` for tenant-level stores.
    pub scope_id: String,
    /// Wrapped key blob (`bao:v<ver>:<ciphertext>`).
    pub wrapped_blob: String,
    /// SHA-256 commitment of the wrapped blob + AAD context.
    pub commitment_sha256: String,
}

/// Dual-custodian key ceremony record for provisioning `policy-ctx` and `audit` signing keys (P3.8).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct KeyCeremonyRecord {
    /// Key purpose (`"policy-ctx-hmac"` or `"audit-signing"`).
    pub key_purpose: String,
    /// Key version identifier (for example `"ctx-k1"` or `"audit-k1"`).
    pub key_id: String,
    /// First custodian (`user:...`).
    pub custodian_1: String,
    /// Second custodian (`user:...`, must differ from `custodian_1`).
    pub custodian_2: String,
    /// Public key or HMAC key commitment SHA-256 hex digest.
    pub key_commitment_sha256: String,
    /// Ceremony timestamp (epoch seconds).
    pub completed_at_s: i64,
}

impl KeyCeremonyRecord {
    /// Verifies that the key ceremony satisfies dual-custodian separation of duties.
    pub fn validate(&self) -> Result<(), &'static str> {
        if self.key_purpose != "policy-ctx-hmac" && self.key_purpose != "audit-signing" {
            return Err("unsupported ceremony key_purpose");
        }
        if self.key_id.trim().is_empty() {
            return Err("key_id must not be empty");
        }
        if !self.custodian_1.starts_with("user:") || !self.custodian_2.starts_with("user:") {
            return Err("key ceremony custodians must be human user: principals");
        }
        if self.custodian_1 == self.custodian_2 {
            return Err("key ceremony requires two distinct custodians");
        }
        if self.key_commitment_sha256.len() != 64 {
            return Err("key_commitment_sha256 must be a 64-character SHA-256 hex digest");
        }
        Ok(())
    }
}

/// PKCS#11 / OpenBao Transit-compatible envelope encryption interface (P3 task 3.8).
pub trait KmsEnvelopeProvider {
    /// Wraps a DEK under `kek_ref` for `(channel, scope_id)`.
    fn wrap_dek(
        &self,
        kek_ref: &TenantKekRef,
        channel: KeyChannel,
        scope_id: &str,
        dek_material: &[u8; 32],
    ) -> Result<WrappedDek, &'static str>;

    /// Seals `plaintext` with `aad` under `wrapped_dek`, returning an authenticated envelope.
    fn seal_with_aad(
        &self,
        wrapped_dek: &WrappedDek,
        aad: &[u8],
        plaintext: &[u8],
    ) -> Result<Vec<u8>, &'static str>;

    /// Opens an authenticated `ciphertext_envelope` iff `aad` and `wrapped_dek` match.
    fn open_with_aad(
        &self,
        wrapped_dek: &WrappedDek,
        aad: &[u8],
        ciphertext_envelope: &[u8],
    ) -> Result<Vec<u8>, &'static str>;
}

/// Deterministic in-boundary KMS provider used in unit and conformance tests.
#[derive(Debug, Clone)]
pub struct InMemoryOpenBaoKms {
    master_transit_secret: [u8; 32],
}

impl InMemoryOpenBaoKms {
    /// Creates an in-memory KMS provider keyed by `master_transit_secret`.
    #[must_use]
    pub const fn new(master_transit_secret: [u8; 32]) -> Self {
        Self {
            master_transit_secret,
        }
    }

    fn derive_stream_block(&self, dek_commitment: &str, aad: &[u8], counter: u64) -> [u8; 32] {
        let mut msg = Vec::with_capacity(dek_commitment.len() + aad.len() + 16);
        msg.extend_from_slice(dek_commitment.as_bytes());
        msg.push(b'|');
        msg.extend_from_slice(aad);
        msg.push(b'|');
        msg.extend_from_slice(&counter.to_be_bytes());
        hmac_sha256(&self.master_transit_secret, &msg)
    }
}

impl KmsEnvelopeProvider for InMemoryOpenBaoKms {
    fn wrap_dek(
        &self,
        kek_ref: &TenantKekRef,
        channel: KeyChannel,
        scope_id: &str,
        dek_material: &[u8; 32],
    ) -> Result<WrappedDek, &'static str> {
        if kek_ref.tenant.trim().is_empty() || kek_ref.kms_key_ref.trim().is_empty() {
            return Err("invalid TenantKekRef");
        }
        let context = format!(
            "{}|{}|v{}|{}|{}",
            kek_ref.tenant,
            kek_ref.kms_key_ref,
            kek_ref.key_version,
            channel.as_str(),
            scope_id
        );
        let mask = hmac_sha256(&self.master_transit_secret, context.as_bytes());
        let mut wrapped = [0u8; 32];
        for i in 0..32 {
            wrapped[i] = dek_material[i] ^ mask[i];
        }
        let wrapped_blob = format!("bao:v{}:{}", kek_ref.key_version, hex_encode(&wrapped));
        let commitment_sha256 = sha256_hex(format!("{context}|{wrapped_blob}").as_bytes());
        Ok(WrappedDek {
            kek_ref: kek_ref.clone(),
            channel,
            scope_id: scope_id.to_owned(),
            wrapped_blob,
            commitment_sha256,
        })
    }

    fn seal_with_aad(
        &self,
        wrapped_dek: &WrappedDek,
        aad: &[u8],
        plaintext: &[u8],
    ) -> Result<Vec<u8>, &'static str> {
        if aad.is_empty() {
            return Err("AAD must not be empty");
        }
        let mut ciphertext = Vec::with_capacity(plaintext.len() + 32);
        for (chunk_idx, chunk) in plaintext.chunks(32).enumerate() {
            let keystream =
                self.derive_stream_block(&wrapped_dek.commitment_sha256, aad, chunk_idx as u64);
            for (i, &b) in chunk.iter().enumerate() {
                ciphertext.push(b ^ keystream[i]);
            }
        }
        let mut tag_input =
            Vec::with_capacity(wrapped_dek.commitment_sha256.len() + aad.len() + ciphertext.len());
        tag_input.extend_from_slice(wrapped_dek.commitment_sha256.as_bytes());
        tag_input.extend_from_slice(aad);
        tag_input.extend_from_slice(&ciphertext);
        let tag = hmac_sha256(&self.master_transit_secret, &tag_input);
        ciphertext.extend_from_slice(&tag);
        Ok(ciphertext)
    }

    fn open_with_aad(
        &self,
        wrapped_dek: &WrappedDek,
        aad: &[u8],
        ciphertext_envelope: &[u8],
    ) -> Result<Vec<u8>, &'static str> {
        if aad.is_empty() || ciphertext_envelope.len() < 32 {
            return Err("invalid ciphertext envelope or empty AAD");
        }
        let body_len = ciphertext_envelope.len() - 32;
        let body = &ciphertext_envelope[..body_len];
        let provided_tag = &ciphertext_envelope[body_len..];

        let mut tag_input =
            Vec::with_capacity(wrapped_dek.commitment_sha256.len() + aad.len() + body.len());
        tag_input.extend_from_slice(wrapped_dek.commitment_sha256.as_bytes());
        tag_input.extend_from_slice(aad);
        tag_input.extend_from_slice(body);
        let expected_tag = hmac_sha256(&self.master_transit_secret, &tag_input);

        let mut diff = 0u8;
        for i in 0..32 {
            diff |= provided_tag[i] ^ expected_tag[i];
        }
        if diff != 0 {
            return Err("authentication tag mismatch: AAD or key channel tampering detected");
        }

        let mut plaintext = Vec::with_capacity(body.len());
        for (chunk_idx, chunk) in body.chunks(32).enumerate() {
            let keystream =
                self.derive_stream_block(&wrapped_dek.commitment_sha256, aad, chunk_idx as u64);
            for (i, &b) in chunk.iter().enumerate() {
                plaintext.push(b ^ keystream[i]);
            }
        }
        Ok(plaintext)
    }
}
