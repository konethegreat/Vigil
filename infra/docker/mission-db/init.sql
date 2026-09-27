-- Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Isolated synthetic mission store
-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
-- Local throwaway credentials. Production identity and migration roles are out of scope.
CREATE ROLE vigil_app LOGIN PASSWORD 'vigil_local_only' NOSUPERUSER NOBYPASSRLS;
CREATE TABLE missions (
    id text PRIMARY KEY,
    tenant_id text NOT NULL,
    version integer NOT NULL DEFAULT 1,
    body jsonb NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX missions_tenant_updated ON missions(tenant_id, updated_at DESC);
CREATE TABLE mission_audit (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id text NOT NULL,
    mission_id text NOT NULL REFERENCES missions(id),
    event_id text UNIQUE NOT NULL,
    recorded_at text NOT NULL,
    actor text NOT NULL,
    kind text NOT NULL,
    detail text NOT NULL,
    previous_hash text NOT NULL,
    event_hash text NOT NULL
);
CREATE INDEX audit_mission_id ON mission_audit(mission_id,id);
CREATE TABLE mission_commands (
    tenant_id text NOT NULL,
    actor text NOT NULL,
    scope text NOT NULL,
    command_key text NOT NULL,
    fingerprint text NOT NULL,
    response jsonb NOT NULL,
    PRIMARY KEY(tenant_id,actor,scope,command_key)
);
CREATE FUNCTION prevent_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Audit events are append-only'; END $$;
CREATE TRIGGER audit_append_only BEFORE UPDATE OR DELETE ON mission_audit
FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
ALTER TABLE missions ENABLE ROW LEVEL SECURITY;
ALTER TABLE missions FORCE ROW LEVEL SECURITY;
ALTER TABLE mission_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE mission_audit FORCE ROW LEVEL SECURITY;
ALTER TABLE mission_commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE mission_commands FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_missions ON missions USING (tenant_id=current_setting('vigil.tenant',true)) WITH CHECK (tenant_id=current_setting('vigil.tenant',true));
CREATE POLICY tenant_audit ON mission_audit USING (tenant_id=current_setting('vigil.tenant',true)) WITH CHECK (tenant_id=current_setting('vigil.tenant',true));
CREATE POLICY tenant_commands ON mission_commands USING (tenant_id=current_setting('vigil.tenant',true)) WITH CHECK (tenant_id=current_setting('vigil.tenant',true));
GRANT SELECT,INSERT,UPDATE ON missions TO vigil_app;
GRANT SELECT,INSERT ON mission_audit,mission_commands TO vigil_app;
GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA public TO vigil_app;
