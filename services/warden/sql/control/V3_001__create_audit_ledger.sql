-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Control-plane append-only hash-chained audit ledger (P3 task 3.7, DATA_MODEL section 3.5)
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.chain_head (
  tenant_id uuid PRIMARY KEY REFERENCES tenancy.tenant (tenant_id) ON DELETE RESTRICT,
  last_seq bigint NOT NULL DEFAULT 0,
  last_hash bytea NOT NULL CHECK (octet_length(last_hash) = 32)
);

CREATE TABLE audit.audit_inbox (
  inbox_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id uuid NOT NULL,
  event jsonb NOT NULL,
  received_at timestamptz NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE audit.audit_ledger (
  tenant_id uuid NOT NULL,
  seq bigint NOT NULL CHECK (seq >= 1),
  recorded_at timestamptz NOT NULL,
  event_id uuid NOT NULL,
  event_type text NOT NULL,
  occurred_at timestamptz NOT NULL,
  actor text NOT NULL CHECK (actor ~ '^(user|svc|agent):'),
  on_behalf_of text CHECK (on_behalf_of ~ '^user:'),
  purpose text,
  case_id text,
  resource_rid text,
  marking_ids text[] NOT NULL DEFAULT '{}',
  decision text NOT NULL CHECK (decision IN ('ALLOW','DENY','NOT_APPLICABLE')),
  policy_ctx_hash bytea,
  policy_version text,
  details jsonb NOT NULL DEFAULT '{}',
  prev_hash bytea NOT NULL CHECK (octet_length(prev_hash) = 32),
  hash bytea NOT NULL CHECK (octet_length(hash) = 32),
  signature bytea NOT NULL,
  signing_key_id text NOT NULL,
  PRIMARY KEY (tenant_id, seq, recorded_at),
  CHECK ((actor ~ '^agent:') = (on_behalf_of IS NOT NULL))
) PARTITION BY RANGE (recorded_at);

CREATE TABLE audit.audit_ledger_2026_11 PARTITION OF audit.audit_ledger
  FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

CREATE INDEX audit_ledger_resource_idx ON audit.audit_ledger (tenant_id, resource_rid, recorded_at);
CREATE INDEX audit_ledger_actor_idx ON audit.audit_ledger (tenant_id, actor, recorded_at);

CREATE FUNCTION audit.forbid_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'audit.audit_ledger is append-only (%)', TG_OP USING ERRCODE = 'insufficient_privilege';
END $$;

CREATE TRIGGER audit_ledger_append_only BEFORE UPDATE OR DELETE ON audit.audit_ledger
  FOR EACH ROW EXECUTE FUNCTION audit.forbid_mutation();

CREATE TRIGGER audit_ledger_no_truncate BEFORE TRUNCATE ON audit.audit_ledger
  FOR EACH STATEMENT EXECUTE FUNCTION audit.forbid_mutation();

REVOKE ALL ON audit.audit_ledger FROM PUBLIC;
GRANT INSERT, SELECT ON audit.audit_ledger TO vigil_audit_chainer;
GRANT SELECT ON audit.audit_ledger TO vigil_audit_reader;
