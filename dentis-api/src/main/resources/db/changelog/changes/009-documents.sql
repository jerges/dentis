-- 009: Gestión documental — carpetas y documentos por clínica

CREATE TABLE IF NOT EXISTS document_folders (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clinic_id     UUID NOT NULL,
    parent_id     UUID REFERENCES document_folders(id) ON DELETE CASCADE,
    name          VARCHAR(150) NOT NULL,
    s3_prefix     VARCHAR(500) NOT NULL,
    zone          VARCHAR(20)  NOT NULL,
    system_folder BOOLEAN NOT NULL DEFAULT FALSE,
    created_by    UUID,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_folder_clinic_parent_name UNIQUE (clinic_id, parent_id, name)
);
--rollback DROP TABLE IF EXISTS document_folders;

CREATE INDEX IF NOT EXISTS idx_folder_clinic ON document_folders(clinic_id);
CREATE INDEX IF NOT EXISTS idx_folder_parent ON document_folders(parent_id);

CREATE TABLE IF NOT EXISTS clinic_documents (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clinic_id       UUID NOT NULL,
    folder_id       UUID NOT NULL REFERENCES document_folders(id) ON DELETE CASCADE,
    file_name       VARCHAR(300) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    s3_key          VARCHAR(500) NOT NULL,
    file_size       BIGINT,
    description     TEXT,
    uploaded_by     UUID,
    uploaded_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    indexed_for_ia  BOOLEAN NOT NULL DEFAULT FALSE,
    search_vector   TSVECTOR
);
--rollback DROP TABLE IF EXISTS clinic_documents;

CREATE INDEX IF NOT EXISTS idx_docs_folder  ON clinic_documents(folder_id);
CREATE INDEX IF NOT EXISTS idx_docs_clinic  ON clinic_documents(clinic_id);
CREATE INDEX IF NOT EXISTS idx_docs_fts     ON clinic_documents USING GIN(search_vector);

-- Trigger: auto-update search_vector on insert/update
CREATE OR REPLACE FUNCTION docs_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('spanish', coalesce(NEW.file_name, '')), 'A') ||
        setweight(to_tsvector('spanish', coalesce(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
--rollback DROP FUNCTION IF EXISTS docs_search_vector_update;

CREATE TRIGGER trg_docs_search_vector
    BEFORE INSERT OR UPDATE ON clinic_documents
    FOR EACH ROW EXECUTE FUNCTION docs_search_vector_update();
--rollback DROP TRIGGER IF EXISTS trg_docs_search_vector ON clinic_documents;
