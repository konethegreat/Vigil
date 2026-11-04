<!-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - In-boundary OpenBao KMS documentation (P3 task 3.8) -->
# In-Boundary KMS Configuration (`deploy/helm/openbao`)

Vigil uses **OpenBao** (MPL-2.0) as its in-boundary Key Management Service
behind a PKCS#11 HSM interface (P3 task 3.8, [`services/warden/src/kms.rs`](../../../services/warden/src/kms.rs)):

1. **Key Hierarchy (`TenantKek` -> `DatasetDek` / Channel DEK):**
   - Each tenant in `tenancy.tenant` holds a `kms_key_ref` URI
     (`openbao://transit/keys/tenant-<slug>-kek`) referencing a non-exportable
     Key-Encryption Key in OpenBao Transit or a PKCS#11 HSM. Raw key material is
     never stored in PostgreSQL or git.
   - Dedicated key channels isolate `PII`, `SPECIAL-PI` (POPIA s26(a)),
     `CRIMINAL-CONDUCT` (POPIA s26(b), s33), `CHILD` (POPIA s34-35),
     `JUSTIFICATION` (W7 checkpoint ciphertexts), and `DATASET-OBJECT` (P4.7
     Parquet/Iceberg encryption).

2. **Authenticated Additional Data (`AAD`) Binding:**
   - Every `cipher_text` value is sealed with canonical AAD:
     `v1|tenant=<t>|channel=<c>|dataset=<d>|rid=<r>|property=<p>`.
   - Moving a ciphertext across tenants, datasets, rows, or columns fails
     authentication immediately in [`KmsEnvelopeProvider::open_with_aad`](../../../services/warden/src/kms.rs).

3. **Dual-Custodian Key Ceremonies:**
   - The `policy-ctx-hmac` key (used by Warden to sign `vigil.policy_ctx`
     tokens verified by `vigil_sec.apply_ctx()` in PostgreSQL) and the
     `audit-signing` key (used for W12 `audit.audit_ledger` signatures) require
     two distinct human custodians (`user:...`) verified by
     [`KeyCeremonyRecord::validate`](../../../services/warden/src/kms.rs).
