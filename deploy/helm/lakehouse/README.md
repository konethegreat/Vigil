# Vigil Lakehouse Helm Configuration (P4)

This directory contains the pod-local lakehouse Helm configuration for **SeaweedFS** (`seaweedfs-values.yaml`) and **Apache Polaris** (`polaris-values.yaml`), both Apache-2.0 licensed under ADR-0003.

## Components

1. **SeaweedFS S3 Object Store (`seaweedfs-values.yaml`)**
   - Serves `s3://vigil-<pod>-lake/` inside each sovereign pod (`za-gp-1`, `za-wc-1`, `za-gp-2`).
   - Enforces TLS 1.3 and vends 15-minute (`900 s`) prefix-scoped STS upload credentials (`<tenant>/_staging/<transaction_uuid>/`) only after Warden allows the caller's policy context.
   - Encrypts Parquet data files and Iceberg manifests with per-dataset 256-bit AES-256-GCM DEKs wrapped by tenant KEKs from OpenBao (`P3.8`), binding the ciphertext AAD to `<tenant>|<dataset_rid>|<branch>|<object_path>`.

2. **Apache Polaris Iceberg v3 REST Catalog (`polaris-values.yaml`)**
   - Manages Iceberg format-version `3` tables with `write.delete.mode = merge-on-read` (v3 deletion vectors).
   - Enforces namespace conventions `<tenant>.<pack>.<dataset>` (including reserved packs `_raw`, `_er`, `_edits`, and `_quality`).
   - Only `forge-datasets` writes `vigil.*` security and provenance table properties after calling Warden `propagate()`; readers never trust client-supplied label properties.
   - Default snapshot retention: `history.expire.max-snapshot-age-ms = 2592000000` (30 days) and `history.expire.min-snapshots-to-keep = 10` (with 14-day retention on `_raw` AIS tables). POPIA erasure sagas explicitly expire all pre-erasure snapshots regardless of `min-snapshots-to-keep` and retain only `erasure-<request-id>-post`.
