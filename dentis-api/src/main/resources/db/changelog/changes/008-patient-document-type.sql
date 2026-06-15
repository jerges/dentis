-- 008: Add document type to patients, representative N/A flag, dental arch to teeth
-- space_status and surface_conditions already added in 002 and 004

-- ----------------------------------------------------------
-- Patients: document type (Venezuela: NATIONAL_ID, FOREIGN_ID, PASSPORT, RIF)
-- ----------------------------------------------------------
ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(20) NOT NULL DEFAULT 'NATIONAL_ID';
--rollback ALTER TABLE patients DROP COLUMN IF EXISTS document_type;

ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS representative_not_applicable BOOLEAN NOT NULL DEFAULT FALSE;
--rollback ALTER TABLE patients DROP COLUMN IF EXISTS representative_not_applicable;

ALTER TABLE patients
    ALTER COLUMN id_document TYPE VARCHAR(30);
--rollback ALTER TABLE patients ALTER COLUMN id_document TYPE VARCHAR(20);

ALTER TABLE patients
    ALTER COLUMN representative_id_document TYPE VARCHAR(30);
--rollback ALTER TABLE patients ALTER COLUMN representative_id_document TYPE VARCHAR(20);

-- ----------------------------------------------------------
-- Odontogram teeth: dental arch (permanent / primary)
-- ----------------------------------------------------------
ALTER TABLE odontogram_teeth
    ADD COLUMN IF NOT EXISTS dental_arch VARCHAR(20);
--rollback ALTER TABLE odontogram_teeth DROP COLUMN IF EXISTS dental_arch;
