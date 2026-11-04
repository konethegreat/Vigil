-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Tenant encrypted and erasable W7 justification store (P3 tasks 3.5, 3.9, DATA_MODEL section 4.2a)
CREATE TABLE vigil_sec.justification (
  justification_id uuid PRIMARY KEY DEFAULT uuidv7(),
  principal text NOT NULL CHECK (principal ~ '^(user|svc):'),
  purpose text NOT NULL CHECK (purpose ~ '^[A-Z][A-Z0-9_]{2,63}$'),
  case_id text,
  checkpoint text NOT NULL CHECK (checkpoint IN ('SPECIAL_CATEGORY_ACCESS','PERSON_LINKAGE','EXPORT','DECLASSIFICATION','REVEAL')),
  scope jsonb NOT NULL DEFAULT '{}',
  text_cipher bytea,
  text_sha256 bytea NOT NULL CHECK (octet_length(text_sha256) = 32),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  expires_at timestamptz NOT NULL,
  erased_at timestamptz,
  CHECK (expires_at > created_at),
  CHECK ((erased_at IS NULL) = (text_cipher IS NOT NULL))
);

COMMENT ON COLUMN vigil_sec.justification.text_cipher IS 'vigil:markings=PII;classification=2';

ALTER TABLE vigil_sec.justification OWNER TO vigil_sec_owner;
GRANT INSERT, SELECT ON vigil_sec.justification TO vigil_reader, vigil_actions;
GRANT USAGE ON SCHEMA vigil_sec TO vigil_erasure_owner;
GRANT SELECT, UPDATE (text_cipher, erased_at) ON vigil_sec.justification TO vigil_erasure_owner;
