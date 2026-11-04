-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Control-plane OMS type projections for object, property, link, interface, action, and function types (P5 task 5.1; DATA_MODEL section 3.3)
-- db: vigil_control

CREATE TABLE oms.object_type (
  object_type_id uuid PRIMARY KEY DEFAULT uuidv7(),
  ontology_id uuid NOT NULL REFERENCES oms.ontology (ontology_id),
  branch_id uuid NOT NULL REFERENCES oms.schema_branch (branch_id),
  rid text NOT NULL,
  api_name text NOT NULL CHECK (api_name ~ '^[A-Z][A-Za-z0-9]{1,62}$'),
  display_name text NOT NULL,
  status text NOT NULL CHECK (status IN ('EXPERIMENTAL','ACTIVE','DEPRECATED')),
  primary_key_property text NOT NULL,
  title_property text NOT NULL,
  existence_policy text NOT NULL DEFAULT 'SOURCE_WINS' CHECK (existence_policy IN ('SOURCE_WINS','EDIT_WINS')),
  physical_table text NOT NULL CHECK (physical_table ~ '^ot_[a-z][a-z0-9_]{1,59}$'),
  partition_modulus smallint NOT NULL DEFAULT 1 CHECK (partition_modulus IN (1,16,32,64)),
  default_markings text[] NOT NULL DEFAULT '{}',
  default_classification smallint NOT NULL DEFAULT 0 CHECK (default_classification BETWEEN 0 AND 4),
  current_version_id uuid NOT NULL REFERENCES oms.type_version (type_version_id),
  UNIQUE (ontology_id, branch_id, api_name),
  UNIQUE (ontology_id, branch_id, rid)
);

CREATE TABLE oms.property_type (
  property_type_id uuid PRIMARY KEY DEFAULT uuidv7(),
  object_type_id uuid NOT NULL REFERENCES oms.object_type (object_type_id) ON DELETE RESTRICT,
  api_name text NOT NULL CHECK (api_name ~ '^[a-z][a-z0-9_]{0,62}$'),
  base_type text NOT NULL CHECK (base_type IN ('boolean','integer','long','double','decimal','string','date','timestamp',
    'struct','geopoint','geoshape','time_series','geotemporal_series','vector','media_reference','cipher_text','marking')),
  is_array boolean NOT NULL DEFAULT false,
  value_type text,
  physical_column text NOT NULL CHECK (physical_column ~ '^[a-z][a-z0-9_]{0,62}$'),
  nullable boolean NOT NULL DEFAULT true,
  editable boolean NOT NULL DEFAULT false,
  conflict_policy text CHECK (conflict_policy IN ('SOURCE_WINS','EDIT_WINS','LATEST_TIMESTAMP')),
  column_markings text[] NOT NULL DEFAULT '{}',
  column_classification smallint NOT NULL DEFAULT 0 CHECK (column_classification BETWEEN 0 AND 4),
  searchable boolean NOT NULL DEFAULT false,
  vector_dimension integer CHECK (vector_dimension BETWEEN 1 AND 2000),
  embedding_model text,
  introduced_in integer NOT NULL,
  retired_in integer,
  UNIQUE (object_type_id, api_name),
  UNIQUE (object_type_id, physical_column),
  CHECK (editable = (conflict_policy IS NOT NULL)),
  CHECK (NOT (is_array AND base_type IN ('vector','time_series','geotemporal_series'))),
  CHECK ((base_type = 'vector') = (vector_dimension IS NOT NULL AND embedding_model IS NOT NULL)),
  CHECK (physical_column NOT IN ('pk','rid','markings','classification','releasability','purpose_tags','source_id',
    'source_record_id','connector_version','fetched_at','terms_ref','responsible_party','lawful_basis','data_origin',
    'dataset_rid','dataset_txn_id','content_sha256','edited_props','last_edit_seq','valid_from','updated_at','version',
    'deleted_at','redacted_properties'))
);

CREATE TABLE oms.link_type (
  link_type_id uuid PRIMARY KEY DEFAULT uuidv7(),
  ontology_id uuid NOT NULL REFERENCES oms.ontology (ontology_id),
  branch_id uuid NOT NULL REFERENCES oms.schema_branch (branch_id),
  rid text NOT NULL,
  api_name text NOT NULL CHECK (api_name ~ '^[a-z][a-z0-9_]{1,62}$'),
  inverse_api_name text NOT NULL CHECK (inverse_api_name ~ '^[a-z][a-z0-9_]{1,62}$'),
  cardinality text NOT NULL CHECK (cardinality IN ('ONE_TO_ONE','MANY_TO_ONE','MANY_TO_MANY','OBJECT_BACKED')),
  a_object_type_id uuid NOT NULL REFERENCES oms.object_type (object_type_id),
  b_object_type_id uuid NOT NULL REFERENCES oms.object_type (object_type_id),
  fk_property_id uuid REFERENCES oms.property_type (property_type_id),
  join_table text CHECK (join_table ~ '^lt_[a-z][a-z0-9_]{1,59}$'),
  backing_object_type_id uuid REFERENCES oms.object_type (object_type_id),
  datasource_rid text,
  link_markings text[] NOT NULL DEFAULT '{}',
  status text NOT NULL CHECK (status IN ('EXPERIMENTAL','ACTIVE','DEPRECATED')),
  current_version_id uuid NOT NULL REFERENCES oms.type_version (type_version_id),
  UNIQUE (ontology_id, branch_id, api_name),
  CHECK ((cardinality IN ('ONE_TO_ONE','MANY_TO_ONE') AND fk_property_id IS NOT NULL AND join_table IS NULL AND backing_object_type_id IS NULL)
      OR (cardinality = 'MANY_TO_MANY' AND join_table IS NOT NULL AND fk_property_id IS NULL AND backing_object_type_id IS NULL)
      OR (cardinality = 'OBJECT_BACKED' AND backing_object_type_id IS NOT NULL AND fk_property_id IS NULL AND join_table IS NULL))
);

CREATE UNIQUE INDEX link_type_one_datasource ON oms.link_type (branch_id, datasource_rid) WHERE datasource_rid IS NOT NULL;

CREATE TABLE oms.interface_type (
  interface_type_id uuid PRIMARY KEY DEFAULT uuidv7(),
  ontology_id uuid NOT NULL REFERENCES oms.ontology (ontology_id),
  branch_id uuid NOT NULL REFERENCES oms.schema_branch (branch_id),
  rid text NOT NULL,
  api_name text NOT NULL CHECK (api_name ~ '^[A-Z][A-Za-z0-9]{1,62}$'),
  shared_properties jsonb NOT NULL,
  current_version_id uuid NOT NULL REFERENCES oms.type_version (type_version_id),
  UNIQUE (ontology_id, branch_id, api_name)
);

CREATE TABLE oms.object_type_interface (
  object_type_id uuid NOT NULL REFERENCES oms.object_type (object_type_id),
  interface_type_id uuid NOT NULL REFERENCES oms.interface_type (interface_type_id),
  property_map jsonb NOT NULL,
  PRIMARY KEY (object_type_id, interface_type_id)
);

CREATE TABLE oms.action_type (
  action_type_id uuid PRIMARY KEY DEFAULT uuidv7(),
  ontology_id uuid NOT NULL REFERENCES oms.ontology (ontology_id),
  branch_id uuid NOT NULL REFERENCES oms.schema_branch (branch_id),
  rid text NOT NULL,
  api_name text NOT NULL CHECK (api_name ~ '^[a-z][a-z0-9-]{1,62}$'),
  display_name text NOT NULL,
  status text NOT NULL CHECK (status IN ('EXPERIMENTAL','ACTIVE','DEPRECATED')),
  outcome_class text NOT NULL CHECK (outcome_class IN ('INFORMATIONAL','OPERATIONAL','ADVERSE','LEGAL_EFFECT')),
  requires_human_approval boolean NOT NULL,
  agent_executable boolean NOT NULL DEFAULT false,
  justification_required boolean NOT NULL DEFAULT false,
  case_scoped boolean NOT NULL DEFAULT false,
  allowed_purposes text[] NOT NULL,
  has_writeback_webhook boolean NOT NULL DEFAULT false,
  webhook_timeout_ms integer CHECK (webhook_timeout_ms BETWEEN 100 AND 10000),
  max_objects integer NOT NULL DEFAULT 10000 CHECK (max_objects BETWEEN 1 AND 10000),
  max_object_types smallint NOT NULL DEFAULT 50 CHECK (max_object_types BETWEEN 1 AND 50),
  sync_budget_ms integer NOT NULL DEFAULT 60000 CHECK (sync_budget_ms BETWEEN 1000 AND 60000),
  current_version_id uuid NOT NULL REFERENCES oms.type_version (type_version_id),
  UNIQUE (ontology_id, branch_id, api_name),
  CHECK (outcome_class NOT IN ('ADVERSE','LEGAL_EFFECT') OR requires_human_approval),
  CHECK (has_writeback_webhook = (webhook_timeout_ms IS NOT NULL)),
  CHECK (cardinality(allowed_purposes) >= 1)
);

CREATE TABLE oms.function_type (
  function_type_id uuid PRIMARY KEY DEFAULT uuidv7(),
  ontology_id uuid NOT NULL REFERENCES oms.ontology (ontology_id),
  branch_id uuid NOT NULL REFERENCES oms.schema_branch (branch_id),
  rid text NOT NULL,
  api_name text NOT NULL CHECK (api_name ~ '^[a-z][a-z0-9-]{1,62}$'),
  language text NOT NULL CHECK (language IN ('TYPESCRIPT','PYTHON','WASM')),
  kind text NOT NULL CHECK (kind IN ('QUERY','EDITS')),
  artefact_sha256 bytea NOT NULL CHECK (octet_length(artefact_sha256) = 32),
  input_schema jsonb NOT NULL,
  output_schema jsonb NOT NULL,
  timeout_ms integer NOT NULL DEFAULT 60000 CHECK (timeout_ms BETWEEN 100 AND 60000),
  memory_mb integer NOT NULL DEFAULT 512 CHECK (memory_mb BETWEEN 16 AND 4096),
  current_version_id uuid NOT NULL REFERENCES oms.type_version (type_version_id),
  UNIQUE (ontology_id, branch_id, api_name)
);

CREATE INDEX property_type_object_idx ON oms.property_type (object_type_id);
CREATE INDEX link_type_a_idx ON oms.link_type (a_object_type_id);
CREATE INDEX link_type_b_idx ON oms.link_type (b_object_type_id);
