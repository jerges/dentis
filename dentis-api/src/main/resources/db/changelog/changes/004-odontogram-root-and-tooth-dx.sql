--liquibase formatted sql

--changeset dentis:004-01 labels:root-findings
ALTER TABLE odontogram_teeth ADD COLUMN IF NOT EXISTS root_findings VARCHAR(200);
--rollback ALTER TABLE odontogram_teeth DROP COLUMN IF EXISTS root_findings;

--changeset dentis:004-02 labels:root-findings
ALTER TABLE odontogram_teeth ADD COLUMN IF NOT EXISTS root_notes TEXT;
--rollback ALTER TABLE odontogram_teeth DROP COLUMN IF EXISTS root_notes;

--changeset dentis:004-03 labels:surface-conditions
ALTER TABLE odontogram_teeth ADD COLUMN IF NOT EXISTS surface_conditions VARCHAR(500);
--rollback ALTER TABLE odontogram_teeth DROP COLUMN IF EXISTS surface_conditions;

--changeset dentis:004-04 labels:diagnosis-tooth
ALTER TABLE diagnoses ADD COLUMN IF NOT EXISTS tooth_number INT;
--rollback ALTER TABLE diagnoses DROP COLUMN IF EXISTS tooth_number;
