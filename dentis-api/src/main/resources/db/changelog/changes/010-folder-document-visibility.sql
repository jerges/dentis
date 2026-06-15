-- Migration 010: Add visibility flag to document folders and clinic documents
-- PUBLIC  = visible to all clinic staff (default)
-- PRIVATE = visible only to the creator/uploader; excluded from IA indexing

ALTER TABLE document_folders
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC';

ALTER TABLE clinic_documents
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC';

CREATE INDEX IF NOT EXISTS idx_folders_visibility ON document_folders (clinic_id, visibility);
CREATE INDEX IF NOT EXISTS idx_docs_visibility    ON clinic_documents  (clinic_id, visibility);
