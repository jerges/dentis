--liquibase formatted sql

--changeset dentis:002-clinical-enhancements
ALTER TABLE clinical_records
    ADD COLUMN IF NOT EXISTS dentition_type VARCHAR(20) DEFAULT 'PERMANENT';

ALTER TABLE odontogram_teeth
    ADD COLUMN IF NOT EXISTS space_status VARCHAR(30);

--rollback ALTER TABLE clinical_records DROP COLUMN IF EXISTS dentition_type;
--rollback ALTER TABLE odontogram_teeth DROP COLUMN IF EXISTS space_status;
