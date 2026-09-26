-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Scoped investigation registry
BEGIN;
CREATE UNIQUE INDEX IF NOT EXISTS missions_tenant_id ON missions(tenant_id,id);
CREATE TABLE IF NOT EXISTS investigation_objects (
    tenant_id text NOT NULL,
    id text NOT NULL,
    object_type text NOT NULL CHECK(object_type IN ('PERSON','ORGANISATION','PLACE','ASSET','EVENT','DOCUMENT')),
    title text NOT NULL CHECK(length(title) BETWEEN 3 AND 160),
    summary text NOT NULL CHECK(length(summary) BETWEEN 5 AND 2000),
    occurred_at timestamptz,
    mission_id text NOT NULL,
    evidence_id text NOT NULL,
    evidence_version integer NOT NULL,
    original_sha256 text NOT NULL,
    created_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,id),
    FOREIGN KEY(tenant_id,mission_id) REFERENCES missions(tenant_id,id),
    CHECK(occurred_at IS NULL OR object_type='EVENT')
);
CREATE TABLE IF NOT EXISTS investigation_links (
    tenant_id text NOT NULL,
    id text NOT NULL,
    from_id text NOT NULL,
    to_id text NOT NULL,
    relation text NOT NULL CHECK(relation IN ('LOCATED_AT','INVOLVES','REPORTED_BY','SUPPLIED_BY','RELATED_TO','PRECEDES')),
    mission_id text NOT NULL,
    evidence_id text NOT NULL,
    evidence_version integer NOT NULL,
    original_sha256 text NOT NULL,
    assessment text NOT NULL CHECK(assessment IN ('REPORTED','CORROBORATED','DISPUTED')),
    reason text NOT NULL CHECK(length(reason) BETWEEN 5 AND 2000),
    revision integer NOT NULL DEFAULT 1,
    created_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,id),
    FOREIGN KEY(tenant_id,from_id) REFERENCES investigation_objects(tenant_id,id),
    FOREIGN KEY(tenant_id,to_id) REFERENCES investigation_objects(tenant_id,id),
    FOREIGN KEY(tenant_id,mission_id) REFERENCES missions(tenant_id,id),
    UNIQUE(tenant_id,mission_id,from_id,to_id,relation,evidence_id),
    CHECK(from_id<>to_id)
);
CREATE TABLE IF NOT EXISTS investigation_link_reviews (
    tenant_id text NOT NULL,
    link_id text NOT NULL,
    revision integer NOT NULL,
    assessment text NOT NULL,
    reason text NOT NULL,
    evidence_version integer NOT NULL,
    actor text NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,link_id,revision),
    FOREIGN KEY(tenant_id,link_id) REFERENCES investigation_links(tenant_id,id)
);
CREATE INDEX IF NOT EXISTS investigation_objects_mission ON investigation_objects(tenant_id,mission_id);
CREATE INDEX IF NOT EXISTS investigation_links_mission ON investigation_links(tenant_id,mission_id);
DO $$ DECLARE tab text; BEGIN
  FOREACH tab IN ARRAY ARRAY['investigation_objects','investigation_links','investigation_link_reviews'] LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',tab);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',tab);
    IF NOT EXISTS(SELECT 1 FROM pg_policies WHERE schemaname='public' AND tablename=tab AND policyname='tenant_scope') THEN
      EXECUTE format('CREATE POLICY tenant_scope ON %I USING (tenant_id=current_setting(''vigil.tenant'',true)) WITH CHECK (tenant_id=current_setting(''vigil.tenant'',true))',tab);
    END IF;
  END LOOP;
END $$;
CREATE OR REPLACE TRIGGER objects_immutable BEFORE UPDATE OR DELETE ON investigation_objects
FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
CREATE OR REPLACE TRIGGER reviews_append_only BEFORE UPDATE OR DELETE ON investigation_link_reviews
FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
GRANT SELECT,INSERT ON investigation_objects,investigation_link_reviews TO vigil_app;
GRANT SELECT,INSERT ON investigation_links TO vigil_app;
GRANT UPDATE(assessment,reason,revision,evidence_version) ON investigation_links TO vigil_app;
COMMIT;
