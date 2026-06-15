export type DocumentZone = 'KNOWLEDGE_BASE' | 'GENERAL';

export interface DocumentFolder {
  id: string;
  parentId: string | null;
  name: string;
  s3Prefix: string;
  zone: DocumentZone;
  system: boolean;
  createdAt: string;
}

export interface ClinicDocument {
  id: string;
  folderId: string;
  fileName: string;
  contentType: string;
  fileSize: number | null;
  description: string | null;
  indexedForIa: boolean;
  uploadedAt: string;
}

export interface CreateFolderRequest {
  parentId: string | null;
  name: string;
  zone: DocumentZone;
}

export interface PresignUploadRequest {
  folderId: string;
  fileName: string;
  contentType: string;
}

export interface RegisterDocumentRequest {
  folderId: string;
  fileName: string;
  contentType: string;
  s3Key: string;
  fileSize: number | null;
  description: string | null;
}
