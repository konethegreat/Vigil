-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Control-plane Warden registry for markings, purposes, and dual-control gate authorisations (P3 tasks 3.1, 3.5, DATA_MODEL section 3.6)
CREATE SCHEMA IF NOT EXISTS warden;

CREATE TABLE warden.marking (
  tenant_id uuid NOT NULL REFERENCES tenancy.tenant (tenant_id) ON DELETE RESTRICT,
  marking_id text NOT NULL CHECK (marking_id ~ '^[A-Z][A-Z0-9-]{1,63}$'),
  category text NOT NULL CHECK (category IN ('PII','SPECIAL_PI','CHILD','CRIMINAL_CONDUCT','SECURITY_MEASURE',
                                             'CASE','ORGANISATION','PROJECT','CUSTOMER')),
  description text NOT NULL,
  required_ground text,
  keycloak_group text NOT NULL,
  PRIMARY KEY (tenant_id, marking_id)
);

CREATE TABLE warden.purpose (
  tenant_id uuid NOT NULL REFERENCES tenancy.tenant (tenant_id) ON DELETE RESTRICT,
  purpose_code text NOT NULL CHECK (purpose_code ~ '^[A-Z][A-Z0-9_]{2,63}$'),
  description text NOT NULL,
  lawful_basis text NOT NULL,
  case_required boolean NOT NULL DEFAULT false,
  permitted_methods text[] NOT NULL,
  retention interval NOT NULL,
  publishable boolean NOT NULL DEFAULT true,
  PRIMARY KEY (tenant_id, purpose_code)
);

CREATE TABLE warden.gate_authorisation (
  authorisation_id uuid PRIMARY KEY DEFAULT uuidv7(),
  tenant_id uuid NOT NULL REFERENCES tenancy.tenant (tenant_id) ON DELETE RESTRICT,
  gate text NOT NULL CHECK (gate IN ('S57_1A_LINKAGE','S57_1B_WATCHLIST','CHILD_DATA','SPECIAL_PI_TRANSFER',
                                     'COMMS_METADATA','BIOMETRIC_RETROSPECTIVE','LPR_HOTLIST','S71_LEGAL_EFFECT')),
  kind text NOT NULL CHECK (kind IN ('REGULATOR_AUTHORISATION','SECTOR_CODE','COURT_DIRECTION',
                                     'COMPETENT_PERSON_CONSENT',
                                     'STATUTORY_AUTHORITY',
                                     'CONTRACT_WITH_SAFEGUARDS',
                                     'SYNTHETIC_DATA_ONLY')),
  reference text NOT NULL,
  issuing_authority text,
  instrument_sha256 bytea CHECK (instrument_sha256 IS NULL OR octet_length(instrument_sha256) = 32),
  scope jsonb NOT NULL,
  valid_from timestamptz NOT NULL,
  valid_to timestamptz NOT NULL,
  requested_by text NOT NULL CHECK (requested_by ~ '^user:'),
  approved_by_1 text NOT NULL CHECK (approved_by_1 ~ '^user:'),
  approved_by_2 text NOT NULL CHECK (approved_by_2 ~ '^user:'),
  counsel_ref text,
  status text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED','EXPIRED','REVOKED')),
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK (approved_by_1 <> approved_by_2
     AND requested_by NOT IN (approved_by_1, approved_by_2)),
  CHECK (valid_to > valid_from)
);

CREATE INDEX gate_authorisation_lookup ON warden.gate_authorisation (tenant_id, gate) WHERE status = 'ACTIVE';
