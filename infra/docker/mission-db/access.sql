-- Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Scoped read and export receipts
-- Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
BEGIN;
CREATE TABLE IF NOT EXISTS mission_access (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id text NOT NULL,
    actor text NOT NULL,
    purpose text NOT NULL,
    operation text NOT NULL,
    resource text NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE mission_access ENABLE ROW LEVEL SECURITY;
ALTER TABLE mission_access FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_access ON mission_access;
CREATE POLICY tenant_access ON mission_access
USING (tenant_id=current_setting('vigil.tenant',true))
WITH CHECK (tenant_id=current_setting('vigil.tenant',true));
CREATE OR REPLACE TRIGGER access_append_only BEFORE UPDATE OR DELETE ON mission_access
FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
GRANT SELECT,INSERT ON mission_access TO vigil_app;
GRANT USAGE,SELECT ON SEQUENCE mission_access_id_seq TO vigil_app;
COMMIT;
