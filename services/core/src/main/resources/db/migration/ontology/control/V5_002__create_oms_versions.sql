-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Control-plane Ontology Metadata Service (OMS) ontologies, schema branches, and immutable type versions (P5 tasks 5.1, 5.2; DATA_MODEL section 3.2)
-- db: vigil_control

CREATE SCHEMA IF NOT EXISTS oms;

CREATE TABLE oms.ontology (
  ontology_id uuid PRIMARY KEY DEFAULT uuidv7(),
  tenant_id uuid NOT NULL UNIQUE REFERENCES tenancy.tenant (tenant_id),
  rid text NOT NULL UNIQUE,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE oms.schema_branch (
  branch_id uuid PRIMARY KEY DEFAULT uuidv7(),
  ontology_id uuid NOT NULL REFERENCES oms.ontology (ontology_id),
  name text NOT NULL CHECK (name ~ '^(main|[a-z0-9][a-z0-9-]{1,40})$'),
  parent_branch_id uuid REFERENCES oms.schema_branch (branch_id),
  forked_at_seq bigint,
  dataset_branch text NOT NULL,
  status text NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','MERGED','ABANDONED')),
  created_by text NOT NULL CHECK (created_by ~ '^(user|svc):'),
  created_at timestamptz NOT NULL DEFAULT now(),
  merged_at timestamptz,
  UNIQUE (ontology_id, name),
  CHECK ((name = 'main') = (parent_branch_id IS NULL))
);

CREATE TABLE oms.type_version (
  type_version_id uuid PRIMARY KEY DEFAULT uuidv7(),
  ontology_id uuid NOT NULL REFERENCES oms.ontology (ontology_id),
  branch_id uuid NOT NULL REFERENCES oms.schema_branch (branch_id),
  type_rid text NOT NULL,
  type_kind text NOT NULL CHECK (type_kind IN ('OBJECT','LINK','INTERFACE','ACTION','FUNCTION','VALUE')),
  api_name text NOT NULL,
  version integer NOT NULL CHECK (version >= 1),
  publish_seq bigint GENERATED ALWAYS AS IDENTITY,
  compat text NOT NULL CHECK (compat IN ('INITIAL','COMPATIBLE','BREAKING')),
  definition jsonb NOT NULL,
  definition_sha256 bytea NOT NULL CHECK (octet_length(definition_sha256) = 32),
  migration_plan jsonb,
  pack_name text,
  pack_version text,
  published_by text NOT NULL CHECK (published_by ~ '^(user|svc):'),
  published_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (type_rid, branch_id, version),
  CHECK (compat <> 'BREAKING' OR migration_plan IS NOT NULL)
);

CREATE FUNCTION oms.forbid_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'oms.type_version is immutable (%)', TG_OP USING ERRCODE = 'insufficient_privilege';
END $$;

CREATE TRIGGER type_version_immutable BEFORE UPDATE OR DELETE ON oms.type_version
  FOR EACH ROW EXECUTE FUNCTION oms.forbid_mutation();
