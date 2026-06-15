--liquibase formatted sql

--changeset dentis:005-01 labels:attachments
CREATE TABLE IF NOT EXISTS clinical_attachments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinical_record_id      UUID NOT NULL REFERENCES clinical_records(id) ON DELETE CASCADE,
    clinic_id               UUID NOT NULL,
    tooth_number            INT,
    file_name               VARCHAR(255) NOT NULL,
    content_type            VARCHAR(100) NOT NULL,
    s3_key                  VARCHAR(500) NOT NULL,
    file_size               BIGINT,
    description             TEXT,
    uploaded_by_dentist_id  UUID,
    uploaded_at             TIMESTAMP NOT NULL DEFAULT now()
);
--rollback DROP TABLE IF EXISTS clinical_attachments;

--changeset dentis:005-02 labels:attachments
CREATE INDEX IF NOT EXISTS idx_attachments_record ON clinical_attachments(clinical_record_id);
--rollback DROP INDEX IF EXISTS idx_attachments_record;

--changeset dentis:005-03 labels:attachments
CREATE INDEX IF NOT EXISTS idx_attachments_clinic ON clinical_attachments(clinic_id);
--rollback DROP INDEX IF EXISTS idx_attachments_clinic;
