-- Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Immutable proposals and decisions
BEGIN;
CREATE TABLE IF NOT EXISTS extraction_runs (
    tenant_id text NOT NULL,
    id text NOT NULL,
    mission_id text NOT NULL,
    evidence_id text NOT NULL,
    evidence_version integer NOT NULL CHECK(evidence_version > 0),
    original_sha256 text NOT NULL,
    body jsonb NOT NULL,
    decision jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,id),
    FOREIGN KEY(tenant_id,mission_id) REFERENCES missions(tenant_id,id),
    UNIQUE(tenant_id,mission_id,evidence_id,evidence_version)
);
ALTER TABLE extraction_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE extraction_runs FORCE ROW LEVEL SECURITY;
DO $$ BEGIN
    IF NOT EXISTS(SELECT 1 FROM pg_policies WHERE schemaname='public' AND tablename='extraction_runs' AND policyname='tenant_scope') THEN
        CREATE POLICY tenant_scope ON extraction_runs
            USING(tenant_id=current_setting('vigil.tenant',true))
            WITH CHECK(tenant_id=current_setting('vigil.tenant',true));
    END IF;
END $$;
CREATE OR REPLACE FUNCTION protect_extraction() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Extraction receipts are immutable'; END IF;
    IF OLD.decision IS NOT NULL OR NEW.decision IS NULL
       OR (to_jsonb(OLD)-'decision') IS DISTINCT FROM (to_jsonb(NEW)-'decision') THEN
        RAISE EXCEPTION 'Only one final review decision may be recorded';
    END IF;
    RETURN NEW;
END $$;
CREATE OR REPLACE TRIGGER extraction_immutable BEFORE UPDATE OR DELETE ON extraction_runs
    FOR EACH ROW EXECUTE FUNCTION protect_extraction();
GRANT SELECT,INSERT ON extraction_runs TO vigil_app;
GRANT UPDATE(decision) ON extraction_runs TO vigil_app;
COMMIT;
