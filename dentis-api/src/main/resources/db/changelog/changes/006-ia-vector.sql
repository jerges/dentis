--liquibase formatted sql

--changeset dentis:006-01 labels:ia
CREATE EXTENSION IF NOT EXISTS vector;
--rollback SELECT 1;

--changeset dentis:006-02 labels:ia
CREATE TABLE IF NOT EXISTS ia_document_chunks (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id               UUID NOT NULL,
    source_attachment_id    UUID REFERENCES clinical_attachments(id) ON DELETE CASCADE,
    source_s3_key           VARCHAR(500),
    chunk_index             INT NOT NULL DEFAULT 0,
    content                 TEXT NOT NULL,
    embedding               vector(1024),
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);
--rollback DROP TABLE IF EXISTS ia_document_chunks;

--changeset dentis:006-03 labels:ia
CREATE INDEX IF NOT EXISTS idx_chunks_clinic ON ia_document_chunks(clinic_id);
--rollback DROP INDEX IF EXISTS idx_chunks_clinic;

--changeset dentis:006-04 labels:ia
CREATE INDEX IF NOT EXISTS idx_chunks_embedding ON ia_document_chunks
    USING hnsw (embedding vector_cosine_ops);
--rollback DROP INDEX IF EXISTS idx_chunks_embedding;

--changeset dentis:006-05 labels:ia
CREATE TABLE IF NOT EXISTS ia_chat_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dentist_id  UUID NOT NULL,
    clinic_id   UUID NOT NULL,
    title       VARCHAR(200) NOT NULL DEFAULT 'Nueva consulta',
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_sessions_dentist ON ia_chat_sessions(dentist_id);
CREATE INDEX IF NOT EXISTS idx_sessions_clinic  ON ia_chat_sessions(clinic_id);
--rollback DROP TABLE IF EXISTS ia_chat_sessions;

--changeset dentis:006-06 labels:ia
CREATE TABLE IF NOT EXISTS ia_chat_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID NOT NULL REFERENCES ia_chat_sessions(id) ON DELETE CASCADE,
    role        VARCHAR(16) NOT NULL,
    content     TEXT NOT NULL,
    citations   TEXT,
    input_tokens  INT DEFAULT 0,
    output_tokens INT DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_messages_session ON ia_chat_messages(session_id);
--rollback DROP TABLE IF EXISTS ia_chat_messages;
