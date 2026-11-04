-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Control-plane tenancy schema and tenant table (P3 task 3.9, DATA_MODEL section 3.1, MSG-029)
CREATE SCHEMA IF NOT EXISTS tenancy;

CREATE TABLE tenancy.tenant (
  tenant_id uuid PRIMARY KEY DEFAULT uuidv7(),
  slug text NOT NULL UNIQUE CHECK (slug ~ '^[a-z][a-z0-9]{2,29}$'),
  display_name text NOT NULL,
  country char(2) NOT NULL CHECK (country ~ '^[A-Z]{2}$'),
  residency_pod text NOT NULL CHECK (residency_pod ~ '^[a-z]{2}-[a-z]{2}-[0-9]+$'),
  deployment_tier text NOT NULL CHECK (deployment_tier IN ('SOVEREIGN','ON_PREM','AIR_GAPPED','EDGE','COMMERCIAL')),
  max_classification smallint NOT NULL DEFAULT 2 CHECK (max_classification BETWEEN 0 AND 4),
  db_name text GENERATED ALWAYS AS ('vigil_t_' || slug) STORED,
  kms_key_ref text NOT NULL,
  status text NOT NULL DEFAULT 'PROVISIONING' CHECK (status IN ('PROVISIONING','ACTIVE','SUSPENDED','OFFBOARDING','DELETED')),
  created_at timestamptz NOT NULL DEFAULT now()
);
