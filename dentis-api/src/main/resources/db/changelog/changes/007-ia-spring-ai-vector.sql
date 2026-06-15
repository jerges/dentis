--liquibase formatted sql

--changeset dentis:007-01 labels:ia
DROP TABLE IF EXISTS ia_document_chunks CASCADE;
--rollback SELECT 1;

--changeset dentis:007-02 labels:ia
CREATE TABLE IF NOT EXISTS ia_documents (
    id       UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content  TEXT,
    metadata JSONB,
    embedding vector(1024)
);
--rollback DROP TABLE IF EXISTS ia_documents;

--changeset dentis:007-03 labels:ia
CREATE INDEX IF NOT EXISTS idx_ia_documents_embedding
    ON ia_documents USING hnsw (embedding vector_cosine_ops);
--rollback DROP INDEX IF EXISTS idx_ia_documents_embedding;

--changeset dentis:007-04 labels:ia
CREATE INDEX IF NOT EXISTS idx_ia_documents_metadata
    ON ia_documents USING GIN (metadata jsonb_path_ops);
--rollback DROP INDEX IF EXISTS idx_ia_documents_metadata;
