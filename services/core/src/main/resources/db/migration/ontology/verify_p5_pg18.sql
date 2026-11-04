-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - PostgreSQL 18 security and constraint verification suite for P5 OMS control-plane tables and generated packs/_core object/link tables (MSG-023, P5 Done-when)

-- 1. Bootstrap cluster roles and pgcrypto required by V3_001__create_vigil_sec.sql
DO $$
DECLARE
  r record;
BEGIN
  FOR r IN SELECT unnest(ARRAY[
    'vigil_ddl_admin', 'vigil_sec_owner', 'vigil_audit_chainer', 'vigil_audit_reader',
    'vigil_reader', 'vigil_actions', 'vigil_indexer', 'vigil_traversal',
    'vigil_erasure_owner', 'vigil_erasure_saga'
  ]) AS role_name LOOP
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r.role_name) THEN
      EXECUTE format('CREATE ROLE %I NOLOGIN', r.role_name);
    END IF;
  END LOOP;
END $$;

-- 2. Verify oms.type_version immutability and BREAKING migration_plan constraint
DO $$
DECLARE
  v_tid uuid;
  v_oid uuid;
  v_bid uuid;
  v_tvid uuid;
BEGIN
  INSERT INTO tenancy.tenant (slug, display_name, country, residency_pod, deployment_tier, kms_key_ref)
  VALUES ('demo', 'Demo Tenant', 'ZA', 'za-gp-1', 'SOVEREIGN', 'transit/keys/tenant-demo-kek')
  RETURNING tenant_id INTO v_tid;

  INSERT INTO oms.ontology (tenant_id, rid)
  VALUES (v_tid, 'ri.ontology.demo.ontology.main')
  RETURNING ontology_id INTO v_oid;

  INSERT INTO oms.schema_branch (ontology_id, name, dataset_branch, created_by)
  VALUES (v_oid, 'main', 'main', 'user:architect')
  RETURNING branch_id INTO v_bid;

  INSERT INTO oms.type_version (
    ontology_id, branch_id, type_rid, type_kind, api_name, version, compat,
    definition, definition_sha256, published_by
  ) VALUES (
    v_oid, v_bid, 'ri.ontology.demo.object-type.person', 'OBJECT', 'Person', 1, 'INITIAL',
    '{"api_name":"Person"}'::jsonb, decode(repeat('ab', 32), 'hex'), 'user:architect'
  ) RETURNING type_version_id INTO v_tvid;

  -- Verify oms.type_version immutability trigger forbids UPDATE
  BEGIN
    UPDATE oms.type_version SET version = 2 WHERE type_version_id = v_tvid;
    RAISE EXCEPTION 'Expected oms.type_version UPDATE to fail';
  EXCEPTION WHEN insufficient_privilege THEN
    NULL;
  END;

  -- Verify BREAKING without migration_plan fails check constraint
  BEGIN
    INSERT INTO oms.type_version (
      ontology_id, branch_id, type_rid, type_kind, api_name, version, compat,
      definition, definition_sha256, migration_plan, published_by
    ) VALUES (
      v_oid, v_bid, 'ri.ontology.demo.object-type.person', 'OBJECT', 'Person', 2, 'BREAKING',
      '{"api_name":"Person"}'::jsonb, decode(repeat('cd', 32), 'hex'), NULL, 'user:architect'
    );
    RAISE EXCEPTION 'Expected BREAKING without migration_plan to fail';
  EXCEPTION WHEN check_violation THEN
    NULL;
  END;

  -- Verify POPIA s71 check on oms.action_type: ADVERSE without requires_human_approval fails
  BEGIN
    INSERT INTO oms.action_type (
      ontology_id, branch_id, rid, api_name, display_name, status, outcome_class,
      requires_human_approval, allowed_purposes, current_version_id
    ) VALUES (
      v_oid, v_bid, 'ri.ontology.demo.action-type.refer', 'refer-supplier', 'Refer', 'ACTIVE',
      'ADVERSE', false, ARRAY['PROCUREMENT_INTEGRITY'], v_tvid
    );
    RAISE EXCEPTION 'Expected ADVERSE action without human approval to fail';
  EXCEPTION WHEN check_violation THEN
    NULL;
  END;
END $$;

-- 3. Verify generated packs/_core object tables, join tables, RLS policies, and column-masking views
CREATE SCHEMA IF NOT EXISTS objects;
CREATE SCHEMA IF NOT EXISTS objects_v;

CREATE TABLE IF NOT EXISTS objects.ot_person (
  pk text NOT NULL,
  rid uuid NOT NULL GENERATED ALWAYS AS (vigil_sec.object_rid('01926e5c-3a1b-7c4d-8e2f-9a0b1c2d3e4f'::uuid, pk)) STORED,
  person_id text NOT NULL,
  display_name text NOT NULL,
  name_variants text[],
  id_psn text,
  id_cipher text,
  member_count integer,
  markings text[] NOT NULL DEFAULT '{}',
  classification smallint NOT NULL DEFAULT 0 CHECK (classification BETWEEN 0 AND 4),
  releasability text[] NOT NULL DEFAULT '{}',
  purpose_tags text[] NOT NULL DEFAULT '{}',
  source_id text NOT NULL DEFAULT 'oms',
  source_record_id text,
  connector_version text,
  fetched_at timestamptz,
  terms_ref text,
  responsible_party text,
  lawful_basis text,
  data_origin text NOT NULL DEFAULT 'SYNTHETIC' CHECK (data_origin IN ('REAL','PSEUDONYMISED','SYNTHETIC')),
  dataset_rid text,
  dataset_txn_id text,
  content_sha256 bytea,
  edited_props jsonb NOT NULL DEFAULT '{}'::jsonb,
  last_edit_seq bigint,
  valid_from timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1,
  deleted_at timestamptz,
  PRIMARY KEY (pk)
);

ALTER TABLE objects.ot_person ENABLE ROW LEVEL SECURITY;
ALTER TABLE objects.ot_person FORCE ROW LEVEL SECURITY;

CREATE POLICY p_read ON objects.ot_person FOR SELECT TO vigil_reader
  USING ((SELECT vigil_sec.ctx_ok()) AND deleted_at IS NULL
     AND markings <@ (SELECT vigil_sec.ctx_markings())
     AND classification <= (SELECT vigil_sec.ctx_clearance())
     AND (releasability = '{}' OR releasability && (SELECT vigil_sec.ctx_releasability()))
     AND (purpose_tags = '{}' OR (SELECT vigil_sec.ctx_purpose()) = ANY (purpose_tags)));

CREATE POLICY p_indexer ON objects.ot_person FOR ALL TO vigil_indexer USING (true) WITH CHECK (true);

GRANT USAGE ON SCHEMA objects, objects_v TO vigil_reader, vigil_indexer;
GRANT SELECT ON objects.ot_person TO vigil_reader, vigil_indexer;
GRANT INSERT, UPDATE ON objects.ot_person TO vigil_indexer;

CREATE OR REPLACE VIEW objects_v.person WITH (security_barrier = true, security_invoker = true) AS
SELECT t.pk, t.rid, t.person_id, t.display_name, t.name_variants, t.id_psn,
       CASE WHEN '{PII}'::text[] <@ (SELECT vigil_sec.ctx_markings()) THEN t.id_cipher END AS id_cipher,
       array_remove(ARRAY[CASE WHEN NOT ('{PII}'::text[] <@ (SELECT vigil_sec.ctx_markings())) THEN 'id_cipher' END], NULL) AS redacted_properties,
       t.markings, t.classification, t.releasability, t.purpose_tags, t.updated_at, t.version
FROM objects.ot_person t;

GRANT SELECT ON objects_v.person TO vigil_reader;

-- Seed a Person row via vigil_indexer and verify RLS + column masking via vigil_reader
SET ROLE vigil_indexer;
INSERT INTO objects.ot_person (pk, person_id, display_name, id_psn, id_cipher, markings, classification, purpose_tags)
VALUES ('per-001', 'per-001', 'Thabo Example', 'psn_123', 'cipher_xyz', ARRAY['FIN'], 1, ARRAY['PROCUREMENT_INTEGRITY']);
RESET ROLE;

INSERT INTO vigil_sec.ctx_key (key_id, secret, active)
VALUES ('ctx-k1', '0123456789abcdef0123456789abcdef'::bytea, true)
ON CONFLICT (key_id) DO NOTHING;

CREATE OR REPLACE FUNCTION pg_temp.make_ctx_token(payload_json text, k_id text, secret_bytes bytea)
RETURNS text
LANGUAGE plpgsql
AS $$
DECLARE
  b64 text;
  sig text;
BEGIN
  b64 := rtrim(translate(encode(convert_to(payload_json, 'UTF8'), 'base64'), E'+/\n', '-_'), '=');
  sig := encode(vigil_sec.hmac(convert_to(b64 || '.' || k_id, 'UTF8'), secret_bytes, 'sha256'), 'hex');
  RETURN b64 || '.' || k_id || '.' || sig;
END;
$$;

DO $$
DECLARE
  v_tok text;
  v_cipher text;
  v_redacted text[];
BEGIN
  -- Reader holding FIN (for row RLS) but NOT holding PII (for id_cipher column masking)
  v_tok := pg_temp.make_ctx_token(
    json_build_object(
      'markings', ARRAY['FIN'],
      'clearance', 2,
      'releasability', ARRAY['REL-ZA'],
      'purpose', 'PROCUREMENT_INTEGRITY',
      'exp', (now() + interval '5 minutes')::text
    )::text,
    'ctx-k1',
    '0123456789abcdef0123456789abcdef'::bytea
  );
  PERFORM set_config('vigil.policy_ctx', v_tok, true);
  PERFORM vigil_sec.apply_ctx();

  SET LOCAL ROLE vigil_reader;
  SELECT id_cipher, redacted_properties INTO v_cipher, v_redacted FROM objects_v.person WHERE pk = 'per-001';
  RESET ROLE;
  IF v_cipher IS NOT NULL OR v_redacted <> ARRAY['id_cipher'] THEN
    RAISE EXCEPTION 'Expected id_cipher to be masked and listed in redacted_properties';
  END IF;
END $$;

