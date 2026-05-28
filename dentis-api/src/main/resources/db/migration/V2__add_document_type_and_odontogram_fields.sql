-- ============================================================
-- V2: Document type, representative N/A flag,
--     dental arch, space closure status and per-surface
--     conditions for odontogram teeth.
-- ============================================================

-- ------------------------------------------------------------
-- Patients: document type + widen id_document + rep N/A flag
-- ------------------------------------------------------------
ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(20) NOT NULL DEFAULT 'NATIONAL_ID';

ALTER TABLE patients
    ALTER COLUMN id_document TYPE VARCHAR(30);

ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS representative_not_applicable BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE patients
    ALTER COLUMN representative_id_document TYPE VARCHAR(30);

-- ------------------------------------------------------------
-- Odontogram teeth: dental arch, surface conditions, space closure
-- ------------------------------------------------------------
ALTER TABLE odontogram_teeth
    ADD COLUMN IF NOT EXISTS dental_arch        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS surface_conditions TEXT,
    ADD COLUMN IF NOT EXISTS space_closure_status VARCHAR(20);
