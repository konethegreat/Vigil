-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Control-plane OMS physical DDL journal (P5 task 5.3; DATA_MODEL section 3.4)
-- db: vigil_control

CREATE TABLE oms.physical_ddl_log (
  ddl_id uuid PRIMARY KEY DEFAULT uuidv7(),
  tenant_id uuid NOT NULL REFERENCES tenancy.tenant (tenant_id),
  type_version_id uuid NOT NULL REFERENCES oms.type_version (type_version_id),
  statements text NOT NULL,
  statements_sha256 bytea NOT NULL CHECK (octet_length(statements_sha256) = 32),
  status text NOT NULL CHECK (status IN ('PENDING','APPLIED','FAILED')),
  applied_at timestamptz,
  UNIQUE (tenant_id, type_version_id)
);
