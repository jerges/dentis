--liquibase formatted sql

--changeset dentis:008-01 labels:documents
CREATE EXTENSION IF NOT EXISTS pg_trgm;
--rollback SELECT 1;

--changeset dentis:008-02 labels:documents
CREATE TABLE IF NOT EXISTS document_folders (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    clinic_id      UUID         NOT NULL,
    parent_id      UUID         REFERENCES document_folders(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    path           VARCHAR(1000) NOT NULL,
    type           VARCHAR(30)  NOT NULL DEFAULT 'NORMAL',
    visibility     VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',
    owner_user_id  UUID,
    system         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT now()
);
--rollback DROP TABLE IF EXISTS document_folders CASCADE;

--changeset dentis:008-03 labels:documents
CREATE INDEX IF NOT EXISTS idx_doc_folder_clinic  ON document_folders(clinic_id);
CREATE INDEX IF NOT EXISTS idx_doc_folder_parent  ON document_folders(parent_id);
CREATE INDEX IF NOT EXISTS idx_doc_folder_name_trgm ON document_folders USING GIN (name gin_trgm_ops);
-- Una sola carpeta de Base de Conocimiento por clínica
CREATE UNIQUE INDEX IF NOT EXISTS uidx_doc_folder_kb_clinic
    ON document_folders(clinic_id)
    WHERE type = 'KNOWLEDGE_BASE';
--rollback DROP INDEX IF EXISTS idx_doc_folder_clinic, idx_doc_folder_parent, idx_doc_folder_name_trgm, uidx_doc_folder_kb_clinic;

--changeset dentis:008-04 labels:documents
CREATE TABLE IF NOT EXISTS document_files (
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    clinic_id       UUID          NOT NULL,
    folder_id       UUID          NOT NULL REFERENCES document_folders(id) ON DELETE CASCADE,
    name            VARCHAR(255)  NOT NULL,
    file_name       VARCHAR(500)  NOT NULL,
    content_type    VARCHAR(100),
    file_size       BIGINT,
    s3_key          VARCHAR(1000) NOT NULL,
    visibility      VARCHAR(20)   NOT NULL DEFAULT 'PRIVATE',
    owner_user_id   UUID          NOT NULL,
    indexed_for_ia  BOOLEAN       NOT NULL DEFAULT FALSE,
    content_text    TEXT,
    tsv             TSVECTOR GENERATED ALWAYS AS (
                        to_tsvector('spanish',
                            coalesce(name, '') || ' ' || coalesce(content_text, ''))
                    ) STORED,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now()
);
--rollback DROP TABLE IF EXISTS document_files CASCADE;

--changeset dentis:008-05 labels:documents
CREATE INDEX IF NOT EXISTS idx_doc_file_clinic  ON document_files(clinic_id);
CREATE INDEX IF NOT EXISTS idx_doc_file_folder  ON document_files(folder_id);
CREATE INDEX IF NOT EXISTS idx_doc_file_owner   ON document_files(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_doc_file_tsv     ON document_files USING GIN (tsv);
CREATE INDEX IF NOT EXISTS idx_doc_file_name_trgm ON document_files USING GIN (name gin_trgm_ops);
--rollback DROP INDEX IF EXISTS idx_doc_file_clinic, idx_doc_file_folder, idx_doc_file_owner, idx_doc_file_tsv, idx_doc_file_name_trgm;

--changeset dentis:008-06 labels:documents
CREATE TABLE IF NOT EXISTS document_shares (
    id                  UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    resource_type       VARCHAR(10) NOT NULL,
    resource_id         UUID      NOT NULL,
    shared_with_user_id UUID      NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_doc_share UNIQUE (resource_type, resource_id, shared_with_user_id)
);
--rollback DROP TABLE IF EXISTS document_shares CASCADE;

--changeset dentis:008-07 labels:documents
CREATE INDEX IF NOT EXISTS idx_doc_share_resource ON document_shares(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_doc_share_user     ON document_shares(shared_with_user_id);
--rollback DROP INDEX IF EXISTS idx_doc_share_resource, idx_doc_share_user;

--changeset dentis:008-08 labels:documents
-- Crear carpeta "Base de Conocimiento" para cada clínica existente
INSERT INTO document_folders (id, clinic_id, parent_id, name, path, type, visibility, owner_user_id, system, created_at, updated_at)
SELECT
    gen_random_uuid(),
    id,
    NULL,
    'Base de Conocimiento',
    '/Base de Conocimiento',
    'KNOWLEDGE_BASE',
    'PRIVATE',
    NULL,
    TRUE,
    now(),
    now()
FROM clinics
ON CONFLICT DO NOTHING;
--rollback DELETE FROM document_folders WHERE type = 'KNOWLEDGE_BASE';