# Vigil Forge Datasets Service (`services/forge-datasets/`)

Implements Phase 4 (`P4: Forge Datasets & Lakehouse`) of the Vigil architecture (`ARCHITECTURE.md` section 4, `API_DESIGN.md` section B.5, `DATA_MODEL.md` section 9, `VIGIL_DOMAIN_BLUEPRINT.md` sections 9-10).

## Capabilities

- **Task 4.1 - Dataset & Transaction API (`DatasetEngine`)**:
  - Manages Iceberg v3 tables (`format-version = 3`, `write.delete.mode = merge-on-read`) across `<tenant>.<pack>.<dataset>` namespaces (including reserved packs `_raw`, `_er`, `_edits`, `_quality`).
  - Supports `SNAPSHOT`, `APPEND`, `UPDATE`, and `DELETE` transactions with lifecycle states `OPEN -> COMMITTED | ABORTED`.
  - Enforces mandatory record provenance (`terms_ref`, `lawful_basis_ref`, `responsible_party`, `data_origin`), SHA-256 idempotency keys (`SHA-256(connector_id || cursor_from || cursor_to)`), at most one `OPEN` `SNAPSHOT` per branch, and 15-minute scoped S3 staging credentials (`<tenant>/_staging/<txn-uuid>/`).
- **Task 4.2 - Branch-Ready Datasets & Tags**:
  - Every dataset and transaction carries a branch key (`main`, `vb-<schema-branch>`, `vs-<scenario-id>`, `wap-<run-id>`).
  - Supports branch creation, fast-forward/snapshot merging with inherited markings, and immutable Iceberg snapshot tags (`release-<yyyymmdd>-<n>`, `er-run-<run-id>`, `erasure-<request-id>-post`).
- **Task 4.3 & 4.4 - Warden Marking Propagation & Lineage Events**:
  - Every commit and branch merge calls Warden W5 `propagate()` across input datasets and writes `vigil.markings`, `vigil.classification`, `vigil.releasability`, and `vigil.purpose_tags` to Iceberg table properties.
  - Emits OpenLineage run events (`COMPLETE` / `ABORT`) with security-label and data-quality facets, plus CloudEvents 1.0 `vigil.forge.dataset.transaction.committed.v1` envelopes.
- **Task 4.5 - Retention, Schema Evolution & POPIA Erasure**:
  - Configures 30-day / 10-snapshot retention (`14 days` for `_raw` AIS tables), validates safe column additions and field-ID renames, and executes two-person approved POPIA erasure across snapshots (v3 deletion vectors, explicit expiry of all pre-erasure snapshots overriding `min-snapshots-to-keep`, `erasure-<request-id>-post` tag, and a hash-chained `ErasureAuditProof` containing only counts and SHA-256 digests).
- **Task 4.7 - Dataset Encryption**:
  - Generates per-dataset 256-bit AES-256-GCM DEKs wrapped by the tenant's KEK (`P3.8`), encrypting S3/Iceberg objects with AAD bound to `<tenant>|<dataset_rid>|<branch>|<object_path>`.
