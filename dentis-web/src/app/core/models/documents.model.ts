export type DocumentZone = 'KNOWLEDGE_BASE' | 'GENERAL';
export type DocumentVisibility = 'PUBLIC' | 'PRIVATE';

export interface DocumentFolder {
  id: string;
  parentId: string | null;
  name: string;
  s3Prefix: string;
  zone: DocumentZone;
  system: boolean;
  visibility: DocumentVisibility;
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
  visibility: DocumentVisibility;
  uploadedAt: string;
}

export interface CreateFolderRequest {
  parentId: string | null;
  name: string;
  zone: DocumentZone;
  visibility: DocumentVisibility;
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
  visibility: DocumentVisibility;
}
