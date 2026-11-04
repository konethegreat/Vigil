-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Core lineage graph, W5 marking propagation, and decision-lineage tables (P4 task 4.6, VIGIL_DOMAIN_BLUEPRINT section 10)
-- db: vigil_t_<slug> (tenant lineage schema)

CREATE SCHEMA IF NOT EXISTS lineage;

CREATE TABLE IF NOT EXISTS lineage.lineage_node (
    node_rid text PRIMARY KEY,
    node_kind text NOT NULL CHECK (node_kind IN ('DATASET', 'TRANSFORM', 'OBJECT_TYPE', 'ACTION_RUN', 'DOCUMENT')),
    branch text NOT NULL DEFAULT 'main',
    version_or_snapshot text NOT NULL,
    markings text[] NOT NULL DEFAULT '{}',
    classification smallint NOT NULL DEFAULT 0 CHECK (classification BETWEEN 0 AND 4),
    releasability text[] NOT NULL DEFAULT '{}',
    purpose_tags text[] NOT NULL DEFAULT '{}',
    recorded_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS lineage.lineage_edge (
    from_rid text NOT NULL REFERENCES lineage.lineage_node(node_rid) ON DELETE RESTRICT,
    to_rid text NOT NULL REFERENCES lineage.lineage_node(node_rid) ON DELETE RESTRICT,
    transform_or_run_id text NOT NULL,
    propagated_markings text[] NOT NULL DEFAULT '{}',
    propagated_classification smallint NOT NULL DEFAULT 0 CHECK (propagated_classification BETWEEN 0 AND 4),
    recorded_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (from_rid, to_rid, transform_or_run_id)
);

CREATE INDEX IF NOT EXISTS lineage_edge_to_rid_idx ON lineage.lineage_edge (to_rid);

CREATE TABLE IF NOT EXISTS lineage.marking_propagation (
    propagation_id uuid PRIMARY KEY,
    target_rid text NOT NULL REFERENCES lineage.lineage_node(node_rid) ON DELETE RESTRICT,
    input_rids text[] NOT NULL,
    propagated_markings text[] NOT NULL DEFAULT '{}',
    propagated_classification smallint NOT NULL DEFAULT 0 CHECK (propagated_classification BETWEEN 0 AND 4),
    propagated_releasability text[] NOT NULL DEFAULT '{}',
    propagated_purpose_tags text[] NOT NULL DEFAULT '{}',
    policy_version text NOT NULL,
    evaluated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS marking_propagation_target_idx ON lineage.marking_propagation (target_rid);

CREATE TABLE IF NOT EXISTS lineage.decision_record (
    action_run_id uuid PRIMARY KEY,
    action_type text NOT NULL,
    actor_principal text NOT NULL,
    purpose text NOT NULL,
    case_id text,
    justification_id uuid,
    approval_request_id uuid,
    agent_session_id text,
    model_id text,
    parameter_diff_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    object_rids_read text[] NOT NULL DEFAULT '{}',
    dataset_transaction_rids text[] NOT NULL DEFAULT '{}',
    index_watermark text NOT NULL,
    source_document_sha256s text[] NOT NULL DEFAULT '{}',
    risk_explanation_ref text,
    warden_policy_version text NOT NULL,
    audit_event_hash bytea NOT NULL CHECK (octet_length(audit_event_hash) = 32),
    edit_seq_start bigint NOT NULL,
    edit_seq_end bigint NOT NULL CHECK (edit_seq_end >= edit_seq_start),
    recorded_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT decision_record_agent_model_pair_chk CHECK ((agent_session_id IS NULL) = (model_id IS NULL))
);

ALTER TABLE lineage.lineage_node ENABLE ROW LEVEL SECURITY;
ALTER TABLE lineage.lineage_node FORCE ROW LEVEL SECURITY;
ALTER TABLE lineage.lineage_edge ENABLE ROW LEVEL SECURITY;
ALTER TABLE lineage.lineage_edge FORCE ROW LEVEL SECURITY;
ALTER TABLE lineage.marking_propagation ENABLE ROW LEVEL SECURITY;
ALTER TABLE lineage.marking_propagation FORCE ROW LEVEL SECURITY;
ALTER TABLE lineage.decision_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE lineage.decision_record FORCE ROW LEVEL SECURITY;
