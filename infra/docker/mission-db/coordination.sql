-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Private observations and explicit two-party releases
CREATE TABLE IF NOT EXISTS observation_sources (
  id text PRIMARY KEY, tenant_id text NOT NULL, version integer NOT NULL, body jsonb NOT NULL
);
CREATE TABLE IF NOT EXISTS observations (
  id text PRIMARY KEY, tenant_id text NOT NULL, source_id text NOT NULL REFERENCES observation_sources(id),
  producer_event_id text NOT NULL, fingerprint text NOT NULL, occurred_at timestamptz NOT NULL,
  received_at timestamptz NOT NULL DEFAULT now(), body jsonb NOT NULL,
  UNIQUE(tenant_id,source_id,producer_event_id)
);
CREATE INDEX IF NOT EXISTS observations_window ON observations(tenant_id,occurred_at,id);
CREATE TABLE IF NOT EXISTS agency_cases (
  id text PRIMARY KEY, owner_tenant text NOT NULL, peer_tenant text NOT NULL,
  body jsonb NOT NULL, CHECK(owner_tenant <> peer_tenant)
);
CREATE TABLE IF NOT EXISTS agency_requests (
  id text PRIMARY KEY, case_id text NOT NULL REFERENCES agency_cases(id),
  sender_tenant text NOT NULL, recipient_tenant text NOT NULL,
  state text NOT NULL DEFAULT 'DRAFT', version integer NOT NULL DEFAULT 1, body jsonb NOT NULL,
  CHECK(sender_tenant <> recipient_tenant)
);
CREATE TABLE IF NOT EXISTS agency_packets (
  id text PRIMARY KEY, request_id text NOT NULL REFERENCES agency_requests(id), kind text NOT NULL,
  owner_tenant text NOT NULL, recipient_tenant text NOT NULL,
  expires_at timestamptz NOT NULL, revoked_at timestamptz, body jsonb NOT NULL, fingerprint text NOT NULL,
  UNIQUE(request_id,kind), CHECK(kind IN ('REQUEST','RESULT'))
);
CREATE TABLE IF NOT EXISTS agency_receipts (
  sequence bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  request_id text NOT NULL REFERENCES agency_requests(id), actor_tenant text NOT NULL, body jsonb NOT NULL
);
CREATE TABLE IF NOT EXISTS coordination_access (
  sequence bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, tenant_id text NOT NULL,
  actor text NOT NULL, purpose text NOT NULL, resource text NOT NULL, operation text NOT NULL,
  recorded_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE observation_sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE observation_sources FORCE ROW LEVEL SECURITY;
ALTER TABLE observations ENABLE ROW LEVEL SECURITY;
ALTER TABLE observations FORCE ROW LEVEL SECURITY;
ALTER TABLE agency_cases ENABLE ROW LEVEL SECURITY;
ALTER TABLE agency_cases FORCE ROW LEVEL SECURITY;
ALTER TABLE agency_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE agency_requests FORCE ROW LEVEL SECURITY;
ALTER TABLE agency_packets ENABLE ROW LEVEL SECURITY;
ALTER TABLE agency_packets FORCE ROW LEVEL SECURITY;
ALTER TABLE agency_receipts ENABLE ROW LEVEL SECURITY;
ALTER TABLE agency_receipts FORCE ROW LEVEL SECURITY;
ALTER TABLE coordination_access ENABLE ROW LEVEL SECURITY;
ALTER TABLE coordination_access FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS source_owner ON observation_sources;
CREATE POLICY source_owner ON observation_sources USING (tenant_id=current_setting('vigil.tenant',true)) WITH CHECK (tenant_id=current_setting('vigil.tenant',true));
DROP POLICY IF EXISTS observation_owner ON observations;
CREATE POLICY observation_owner ON observations USING (tenant_id=current_setting('vigil.tenant',true)) WITH CHECK (tenant_id=current_setting('vigil.tenant',true));
DROP POLICY IF EXISTS case_read ON agency_cases;
CREATE POLICY case_read ON agency_cases FOR SELECT USING (current_setting('vigil.tenant',true) IN (owner_tenant,peer_tenant));
DROP POLICY IF EXISTS case_insert ON agency_cases;
CREATE POLICY case_insert ON agency_cases FOR INSERT WITH CHECK (owner_tenant=current_setting('vigil.tenant',true));
DROP POLICY IF EXISTS request_read ON agency_requests;
CREATE POLICY request_read ON agency_requests FOR SELECT USING (sender_tenant=current_setting('vigil.tenant',true) OR (recipient_tenant=current_setting('vigil.tenant',true) AND state<>'DRAFT'));
DROP POLICY IF EXISTS request_insert ON agency_requests;
CREATE POLICY request_insert ON agency_requests FOR INSERT WITH CHECK (sender_tenant=current_setting('vigil.tenant',true) AND EXISTS (SELECT 1 FROM agency_cases c WHERE c.id=case_id AND recipient_tenant IN (c.owner_tenant,c.peer_tenant)));
DROP POLICY IF EXISTS request_update ON agency_requests;
CREATE POLICY request_update ON agency_requests FOR UPDATE USING (current_setting('vigil.tenant',true) IN (sender_tenant,recipient_tenant)) WITH CHECK (current_setting('vigil.tenant',true) IN (sender_tenant,recipient_tenant));
DROP POLICY IF EXISTS packet_read ON agency_packets;
CREATE POLICY packet_read ON agency_packets FOR SELECT USING (
  owner_tenant=current_setting('vigil.tenant',true) OR
  (recipient_tenant=current_setting('vigil.tenant',true) AND revoked_at IS NULL AND expires_at>now()
    AND EXISTS (SELECT 1 FROM agency_requests r WHERE r.id=request_id AND r.state<>'DRAFT')));
DROP POLICY IF EXISTS packet_insert ON agency_packets;
CREATE POLICY packet_insert ON agency_packets FOR INSERT WITH CHECK (
  owner_tenant=current_setting('vigil.tenant',true) AND EXISTS (SELECT 1 FROM agency_requests r WHERE r.id=request_id AND recipient_tenant IN (r.sender_tenant,r.recipient_tenant)));
DROP POLICY IF EXISTS packet_revoke ON agency_packets;
CREATE POLICY packet_revoke ON agency_packets FOR UPDATE USING (owner_tenant=current_setting('vigil.tenant',true)) WITH CHECK (owner_tenant=current_setting('vigil.tenant',true));
DROP POLICY IF EXISTS receipt_read ON agency_receipts;
CREATE POLICY receipt_read ON agency_receipts FOR SELECT USING (EXISTS (SELECT 1 FROM agency_requests r WHERE r.id=request_id));
DROP POLICY IF EXISTS receipt_insert ON agency_receipts;
CREATE POLICY receipt_insert ON agency_receipts FOR INSERT WITH CHECK (actor_tenant=current_setting('vigil.tenant',true) AND EXISTS (SELECT 1 FROM agency_requests r WHERE r.id=request_id));
DROP POLICY IF EXISTS access_owner ON coordination_access;
CREATE POLICY access_owner ON coordination_access USING (tenant_id=current_setting('vigil.tenant',true)) WITH CHECK (tenant_id=current_setting('vigil.tenant',true));

CREATE OR REPLACE FUNCTION prevent_packet_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP='DELETE' OR (to_jsonb(NEW)-'revoked_at') IS DISTINCT FROM (to_jsonb(OLD)-'revoked_at') OR OLD.revoked_at IS NOT NULL OR NEW.revoked_at IS NULL THEN
    RAISE EXCEPTION 'Only the first evidence release revocation is permitted';
  END IF;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS packet_immutable ON agency_packets;
CREATE TRIGGER packet_immutable BEFORE UPDATE OR DELETE ON agency_packets FOR EACH ROW EXECUTE FUNCTION prevent_packet_mutation();
DROP TRIGGER IF EXISTS observation_immutable ON observations;
CREATE TRIGGER observation_immutable BEFORE UPDATE OR DELETE ON observations FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
DROP TRIGGER IF EXISTS receipt_immutable ON agency_receipts;
CREATE TRIGGER receipt_immutable BEFORE UPDATE OR DELETE ON agency_receipts FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
DROP TRIGGER IF EXISTS coordination_access_immutable ON coordination_access;
CREATE TRIGGER coordination_access_immutable BEFORE UPDATE OR DELETE ON coordination_access FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
GRANT SELECT,INSERT ON observation_sources,agency_requests TO vigil_app;
GRANT UPDATE(version,body) ON observation_sources TO vigil_app;
GRANT UPDATE(state,version,body) ON agency_requests TO vigil_app;
GRANT SELECT,INSERT ON observations,agency_cases,agency_packets,agency_receipts,coordination_access TO vigil_app;
GRANT UPDATE(revoked_at) ON agency_packets TO vigil_app;
GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA public TO vigil_app;
