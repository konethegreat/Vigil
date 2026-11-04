-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - PostgreSQL 18 security and RLS verification suite for Warden P3 tables (MSG-023, MSG-030)
\set ON_ERROR_STOP on

-- 1. Create required roles used by DATA_MODEL.md sections 2, 3, and 4.
DO $$
DECLARE
    r text;
BEGIN
    FOREACH r IN ARRAY ARRAY[
        'vigil_audit_chainer',
        'vigil_audit_reader',
        'vigil_warden',
        'vigil_sec_owner',
        'vigil_reader',
        'vigil_actions',
        'vigil_indexer',
        'vigil_traversal',
        'vigil_erasure_owner',
        'vigil_erasure_saga'
    ] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
            EXECUTE format('CREATE ROLE %I NOLOGIN', r);
        END IF;
    END LOOP;
END
$$;

-- 2. Apply P3 control-plane and per-tenant migrations in order.
\i /work/services/warden/sql/control/V3_000__create_tenancy.sql
\i /work/services/warden/sql/control/V3_001__create_audit_ledger.sql
\i /work/services/warden/sql/control/V3_002__create_warden_registry.sql
\i /work/services/warden/sql/tenant/V3_001__create_vigil_sec.sql
\i /work/services/warden/sql/tenant/V3_002__create_justification.sql

-- 3. Verify tenancy.tenant constraints (slug, country, residency_pod, db_name generated column).
INSERT INTO tenancy.tenant (slug, display_name, country, residency_pod, deployment_tier, kms_key_ref, status)
VALUES ('demo', 'Demo Sovereign Pod', 'ZA', 'za-jb-1', 'SOVEREIGN', 'openbao://transit/keys/tenant-demo-kek', 'ACTIVE');

DO $$
DECLARE
    gen_db text;
BEGIN
    SELECT db_name INTO gen_db FROM tenancy.tenant WHERE slug = 'demo';
    IF gen_db <> 'vigil_t_demo' THEN
        RAISE EXCEPTION 'Expected generated db_name = vigil_t_demo, got %', gen_db;
    END IF;

    BEGIN
        INSERT INTO tenancy.tenant (slug, display_name, country, residency_pod, deployment_tier, kms_key_ref)
        VALUES ('BAD_SLUG', 'Bad Slug', 'ZA', 'za-jb-1', 'SOVEREIGN', 'openbao://transit/keys/bad');
        RAISE EXCEPTION 'Expected tenancy.tenant slug check constraint failure';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
END
$$;

-- 4. Verify audit.audit_ledger append-only hash-chain and mutation trigger (W12).
DO $$
DECLARE
    t_id uuid;
BEGIN
    SELECT tenant_id INTO t_id FROM tenancy.tenant WHERE slug = 'demo';

    INSERT INTO audit.chain_head (tenant_id, last_seq, last_hash)
    VALUES (t_id, 1, decode(repeat('a', 64), 'hex'));

    INSERT INTO audit.audit_ledger (
        tenant_id, seq, recorded_at, event_id, event_type, occurred_at,
        actor, purpose, resource_rid, marking_ids, decision,
        prev_hash, hash, signature, signing_key_id
    ) VALUES (
        t_id, 1, '2026-11-04T12:00:00Z'::timestamptz, uuidv7(), 'policy.evaluate', '2026-11-04T12:00:00Z'::timestamptz,
        'user:analyst-1', 'PROCUREMENT_INTEGRITY', 'ri.vigil.demo.object.supplier-1',
        ARRAY['PII', 'ORG-TREASURY', 'PRJ-DURBAN'], 'ALLOW',
        decode(repeat('0', 64), 'hex'),
        decode(repeat('a', 64), 'hex'),
        decode(repeat('b', 128), 'hex'),
        'audit-k1'
    );

    BEGIN
        UPDATE audit.audit_ledger SET decision = 'DENY' WHERE tenant_id = t_id AND seq = 1;
        RAISE EXCEPTION 'Expected audit.forbid_mutation() to block UPDATE on audit.audit_ledger';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    BEGIN
        DELETE FROM audit.audit_ledger WHERE tenant_id = t_id AND seq = 1;
        RAISE EXCEPTION 'Expected audit.forbid_mutation() to block DELETE on audit.audit_ledger';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END
$$;

-- 5. Verify warden.marking, warden.purpose, and warden.gate_authorisation constraints (three-party separation of duties).
DO $$
DECLARE
    t_id uuid;
BEGIN
    SELECT tenant_id INTO t_id FROM tenancy.tenant WHERE slug = 'demo';

    INSERT INTO warden.marking (tenant_id, marking_id, category, description, keycloak_group)
    VALUES
        (t_id, 'PII', 'PII', 'Personal Information', '/markings/PII'),
        (t_id, 'ORG-TREASURY', 'ORGANISATION', 'National Treasury', '/markings/ORG-TREASURY'),
        (t_id, 'PRJ-DURBAN', 'PROJECT', 'Durban Corridor', '/markings/PRJ-DURBAN');

    INSERT INTO warden.purpose (
        tenant_id, purpose_code, description, lawful_basis,
        case_required, permitted_methods, retention
    ) VALUES (
        t_id, 'PROCUREMENT_INTEGRITY',
        'Public procurement integrity verification under PFMA and PRECCA',
        'PUBLIC_LAW_DUTY', false, ARRAY['READ','SEARCH','TRAVERSE'], interval '5 years'
    );

    INSERT INTO warden.gate_authorisation (
        tenant_id, gate, kind, reference, issuing_authority,
        instrument_sha256, scope, valid_from, valid_to,
        requested_by, approved_by_1, approved_by_2
    ) VALUES (
        t_id, 'S57_1A_LINKAGE', 'REGULATOR_AUTHORISATION', 'IR-ZA-2026-0042', 'Information Regulator (SA)',
        decode(repeat('c', 64), 'hex'), '{"object_types":["Supplier"]}'::jsonb,
        now() - interval '1 day', now() + interval '30 days',
        'user:steward-1', 'user:dpo-1', 'user:sec-admin-1'
    );

    BEGIN
        INSERT INTO warden.gate_authorisation (
            tenant_id, gate, kind, reference, scope, valid_from, valid_to,
            requested_by, approved_by_1, approved_by_2
        ) VALUES (
            t_id, 'CHILD_DATA', 'REGULATOR_AUTHORISATION', 'IR-ZA-2026-0099', '{}'::jsonb,
            now() - interval '1 day', now() + interval '30 days',
            'user:steward-1', 'user:dpo-1', 'user:dpo-1'
        );
        RAISE EXCEPTION 'Expected gate_authorisation separation-of-duties constraint to reject duplicate approvers';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
END
$$;

-- 6. Verify vigil_sec.justification erasure invariant ((erased_at IS NULL) = (text_cipher IS NOT NULL)).
INSERT INTO vigil_sec.justification (
    principal, purpose, checkpoint, scope,
    text_cipher, text_sha256, expires_at
) VALUES (
    'user:analyst-1', 'PROCUREMENT_INTEGRITY', 'SPECIAL_CATEGORY_ACCESS',
    '{"rids":["ri.vigil.demo.object.supplier-1"]}'::jsonb,
    decode('01020304', 'hex'), decode(repeat('f', 64), 'hex'),
    clock_timestamp() + interval '4 hours'
);

DO $$
BEGIN
    BEGIN
        INSERT INTO vigil_sec.justification (
            principal, purpose, checkpoint, text_cipher, text_sha256, expires_at, erased_at
        ) VALUES (
            'user:analyst-1', 'PROCUREMENT_INTEGRITY', 'EXPORT',
            decode('0102', 'hex'), decode(repeat('f', 64), 'hex'),
            clock_timestamp() + interval '2 hours', clock_timestamp()
        );
        RAISE EXCEPTION 'Expected justification check constraint to forbid non-null text_cipher when erased_at is set';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;
END
$$;

-- 7. Create an RLS-enforced object table and masking view (DATA_MODEL sections 4.3, 4.5, 4.6)
--    and verify that a caller missing 1 of 3 markings is denied, while a caller holding all 3 is allowed.
CREATE SCHEMA IF NOT EXISTS objects;
CREATE SCHEMA IF NOT EXISTS objects_v;
GRANT USAGE ON SCHEMA objects, objects_v TO vigil_reader, vigil_actions, vigil_indexer, vigil_traversal;

CREATE TABLE objects.ot_supplier (
    id                  uuid        PRIMARY KEY DEFAULT uuidv7(),
    version             bigint      NOT NULL DEFAULT 1,
    markings            text[]      NOT NULL,
    classification      smallint    NOT NULL CHECK (classification BETWEEN 0 AND 4),
    releasability       text[]      NOT NULL DEFAULT '{}',
    purpose_tags        text[]      NOT NULL DEFAULT '{}',
    supplier_name       text        NOT NULL,
    bank_account_masked text,
    deleted_at          timestamptz
);

ALTER TABLE objects.ot_supplier ENABLE ROW LEVEL SECURITY;
ALTER TABLE objects.ot_supplier FORCE ROW LEVEL SECURITY;

CREATE POLICY p_read ON objects.ot_supplier FOR SELECT TO vigil_reader
  USING ((SELECT vigil_sec.ctx_ok()) AND deleted_at IS NULL
     AND markings <@ (SELECT vigil_sec.ctx_markings())
     AND classification <= (SELECT vigil_sec.ctx_clearance())
     AND (releasability = '{}' OR releasability && (SELECT vigil_sec.ctx_releasability()))
     AND (purpose_tags = '{}' OR (SELECT vigil_sec.ctx_purpose()) = ANY (purpose_tags)));

GRANT SELECT ON objects.ot_supplier TO vigil_reader;

CREATE VIEW objects_v.ot_supplier WITH (security_barrier = true, security_invoker = true) AS
SELECT s.id,
       s.supplier_name,
       CASE WHEN '{PII}'::text[] <@ (SELECT vigil_sec.ctx_markings()) AND (SELECT vigil_sec.ctx_clearance()) >= 2 THEN s.bank_account_masked END AS bank_account_masked,
       array_remove(ARRAY[CASE WHEN NOT ('{PII}'::text[] <@ (SELECT vigil_sec.ctx_markings()) AND (SELECT vigil_sec.ctx_clearance()) >= 2) THEN 'bank_account_masked' END], NULL) AS redacted_properties
FROM objects.ot_supplier s;

GRANT SELECT ON objects_v.ot_supplier TO vigil_reader;

-- Insert a row requiring 3 conjunctive markings: PII, ORG-TREASURY, PRJ-DURBAN.
INSERT INTO objects.ot_supplier (
    markings, classification, releasability, purpose_tags, supplier_name, bank_account_masked
) VALUES (
    ARRAY['PII', 'ORG-TREASURY', 'PRJ-DURBAN'],
    2,
    ARRAY['REL-ZA'],
    ARRAY['PROCUREMENT_INTEGRITY'],
    'Umgeni Corridor Logistics (Pty) Ltd',
    '******4521'
);

-- Provision an active 32-byte HMAC key in vigil_sec.ctx_key.
INSERT INTO vigil_sec.ctx_key (key_id, secret, active)
VALUES ('ctx-k1', '0123456789abcdef0123456789abcdef'::bytea, true);

-- Helper function for test token construction using the exact HMAC verification rule in vigil_sec.verified_payload().
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

-- 7a. Fail-closed when vigil.policy_ctx is unset.
SET ROLE vigil_reader;
DO $$
DECLARE
    cnt integer;
BEGIN
    SELECT count(*) INTO cnt FROM objects.ot_supplier;
    IF cnt <> 0 THEN
        RAISE EXCEPTION 'Expected 0 rows without vigil.policy_ctx, got %', cnt;
    END IF;
END
$$;
RESET ROLE;

-- 7b. Deny when caller holds only 2 of the 3 required markings (missing PRJ-DURBAN).
DO $$
DECLARE
    tok text;
    cnt integer;
BEGIN
    tok := pg_temp.make_ctx_token(
        json_build_object(
            'markings', ARRAY['PII', 'ORG-TREASURY'],
            'clearance', 2,
            'releasability', ARRAY['REL-ZA'],
            'purpose', 'PROCUREMENT_INTEGRITY',
            'exp', (now() + interval '5 minutes')::text
        )::text,
        'ctx-k1',
        '0123456789abcdef0123456789abcdef'::bytea
    );
    PERFORM set_config('vigil.policy_ctx', tok, true);
    PERFORM vigil_sec.apply_ctx();
    SET LOCAL ROLE vigil_reader;
    SELECT count(*) INTO cnt FROM objects.ot_supplier;
    RESET ROLE;
    IF cnt <> 0 THEN
        RAISE EXCEPTION 'Expected 0 rows when missing 1 of 3 markings, got %', cnt;
    END IF;
END
$$;

-- 7c. Forged signature must fail closed in vigil_sec.apply_ctx().
DO $$
DECLARE
    bad_tok text;
BEGIN
    bad_tok := pg_temp.make_ctx_token(
        json_build_object(
            'markings', ARRAY['PII', 'ORG-TREASURY', 'PRJ-DURBAN'],
            'clearance', 2,
            'releasability', ARRAY['REL-ZA'],
            'purpose', 'PROCUREMENT_INTEGRITY',
            'exp', (now() + interval '5 minutes')::text
        )::text,
        'ctx-k1',
        'wrong-secret-key-000000000000000'::bytea
    );
    PERFORM set_config('vigil.policy_ctx', bad_tok, true);
    BEGIN
        PERFORM vigil_sec.apply_ctx();
        RAISE EXCEPTION 'Expected vigil_sec.apply_ctx() to reject forged signature';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END
$$;

-- 7d. Allow when caller holds all 3 markings, clearance >= 2, REL-ZA, and PROCUREMENT_INTEGRITY.
DO $$
DECLARE
    ok_tok text;
    cnt integer;
    masked_val text;
BEGIN
    ok_tok := pg_temp.make_ctx_token(
        json_build_object(
            'markings', ARRAY['PII', 'ORG-TREASURY', 'PRJ-DURBAN'],
            'clearance', 2,
            'releasability', ARRAY['REL-ZA'],
            'purpose', 'PROCUREMENT_INTEGRITY',
            'exp', (now() + interval '5 minutes')::text
        )::text,
        'ctx-k1',
        '0123456789abcdef0123456789abcdef'::bytea
    );
    PERFORM set_config('vigil.policy_ctx', ok_tok, true);
    PERFORM vigil_sec.apply_ctx();
    SET LOCAL ROLE vigil_reader;
    SELECT count(*), max(bank_account_masked) INTO cnt, masked_val FROM objects_v.ot_supplier;
    RESET ROLE;
    IF cnt <> 1 OR masked_val <> '******4521' THEN
        RAISE EXCEPTION 'Expected 1 visible row with masked bank account, got cnt=% val=%', cnt, masked_val;
    END IF;
END
$$;

\echo 'PASS [postgres-18-security-suite]: all P3 control-plane, tenant vigil_sec, RLS 3-marking conjunction, masking view, and W12 immutability assertions passed'
