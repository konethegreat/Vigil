-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Tenant vigil_sec schema, HMAC policy-context verification, and W8 declassification guard (P3 tasks 3.6, 3.8, DATA_MODEL sections 4.1, 4.2, 4.9)
CREATE SCHEMA IF NOT EXISTS vigil_sec AUTHORIZATION vigil_sec_owner;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA vigil_sec;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA vigil_sec;

GRANT USAGE ON SCHEMA vigil_sec TO vigil_reader, vigil_actions, vigil_indexer, vigil_traversal;

CREATE TABLE vigil_sec.ctx_key (
  key_id text PRIMARY KEY,
  secret bytea NOT NULL CHECK (octet_length(secret) >= 32),
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE vigil_sec.ctx_key OWNER TO vigil_sec_owner;

CREATE FUNCTION vigil_sec.verified_payload() RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = pg_catalog, vigil_sec AS $$
DECLARE
  tok text := NULLIF(current_setting('vigil.policy_ctx', true), '');
  body text;
  kid text;
  sig text;
  k bytea;
  payload jsonb;
BEGIN
  IF tok IS NULL THEN RETURN NULL; END IF;
  body := split_part(tok, '.', 1);
  kid := split_part(tok, '.', 2);
  sig := split_part(tok, '.', 3);
  SELECT c.secret INTO k FROM vigil_sec.ctx_key c WHERE c.key_id = kid AND c.active;
  IF k IS NULL OR encode(vigil_sec.hmac(convert_to(body || '.' || kid, 'UTF8'), k, 'sha256'), 'hex') <> sig THEN
    RETURN NULL;
  END IF;
  payload := convert_from(decode(translate(body, '-_', '+/') || repeat('=', (4 - length(body) % 4) % 4), 'base64'), 'UTF8')::jsonb;
  IF (payload ->> 'exp')::timestamptz <= now() THEN RETURN NULL; END IF;
  RETURN payload;
END $$;
ALTER FUNCTION vigil_sec.verified_payload() OWNER TO vigil_sec_owner;

CREATE FUNCTION vigil_sec.apply_ctx() RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, vigil_sec AS $$
DECLARE p jsonb := vigil_sec.verified_payload();
BEGIN
  IF p IS NULL THEN
    RAISE EXCEPTION 'invalid or expired policy context' USING ERRCODE = 'insufficient_privilege';
  END IF;
  PERFORM set_config('vigil.user_markings', ARRAY(SELECT jsonb_array_elements_text(p -> 'markings'))::text, true);
  PERFORM set_config('vigil.clearance', p ->> 'clearance', true);
  PERFORM set_config('vigil.releasability', ARRAY(SELECT jsonb_array_elements_text(p -> 'releasability'))::text, true);
  PERFORM set_config('vigil.purpose', p ->> 'purpose', true);
  PERFORM set_config('vigil.case_id', COALESCE(p ->> 'case_id', ''), true);
END $$;
ALTER FUNCTION vigil_sec.apply_ctx() OWNER TO vigil_sec_owner;

CREATE FUNCTION vigil_sec.ctx_ok() RETURNS boolean
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = pg_catalog, vigil_sec AS $$
DECLARE p jsonb := vigil_sec.verified_payload();
BEGIN
  RETURN p IS NOT NULL
     AND current_setting('vigil.user_markings', true) = ARRAY(SELECT jsonb_array_elements_text(p -> 'markings'))::text
     AND current_setting('vigil.clearance', true) = p ->> 'clearance'
     AND current_setting('vigil.releasability', true) = ARRAY(SELECT jsonb_array_elements_text(p -> 'releasability'))::text
     AND current_setting('vigil.purpose', true) = p ->> 'purpose';
END $$;
ALTER FUNCTION vigil_sec.ctx_ok() OWNER TO vigil_sec_owner;

CREATE FUNCTION vigil_sec.ctx_markings() RETURNS text[] LANGUAGE sql STABLE PARALLEL SAFE
  AS $$ SELECT NULLIF(current_setting('vigil.user_markings', true), '')::text[] $$;

CREATE FUNCTION vigil_sec.ctx_clearance() RETURNS smallint LANGUAGE sql STABLE PARALLEL SAFE
  AS $$ SELECT NULLIF(current_setting('vigil.clearance', true), '')::smallint $$;

CREATE FUNCTION vigil_sec.ctx_releasability() RETURNS text[] LANGUAGE sql STABLE PARALLEL SAFE
  AS $$ SELECT NULLIF(current_setting('vigil.releasability', true), '')::text[] $$;

CREATE FUNCTION vigil_sec.ctx_purpose() RETURNS text LANGUAGE sql STABLE PARALLEL SAFE
  AS $$ SELECT NULLIF(current_setting('vigil.purpose', true), '') $$;

CREATE FUNCTION vigil_sec.object_rid(ns uuid, pk text) RETURNS uuid LANGUAGE sql IMMUTABLE PARALLEL SAFE
  AS $$ SELECT vigil_sec.uuid_generate_v5(ns, pk) $$;

REVOKE ALL ON FUNCTION vigil_sec.verified_payload() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION vigil_sec.apply_ctx(), vigil_sec.ctx_ok() TO vigil_reader, vigil_actions;
