export type Visibility = 'PUBLIC' | 'PRIVATE' | 'SHARED';
export type FolderType = 'NORMAL' | 'KNOWLEDGE_BASE';
export type ResourceType = 'FOLDER' | 'FILE';

export interface DocumentFolder {
  id: string;
  clinicId: string;
  parentId: string | null;
  name: string;
  path: string;
  type: FolderType;
  visibility: Visibility;
  ownerUserId: string | null;
  system: boolean;
}

export interface DocumentFile {
  id: string;
  clinicId: string;
  folderId: string;
  name: string;
  fileName: string;
  contentType: string;
  fileSize: number | null;
  visibility: Visibility;
  ownerUserId: string;
  indexedForIa: boolean;
  downloadUrl?: string;
}

export interface DocumentShare {
  id: string;
  resourceType: ResourceType;
  resourceId: string;
  sharedWithUserId: string;
}

export interface ShareTarget {
  id: string;
  fullName: string;
  username: string;
}

export interface DocumentSearchResult {
  id: string;
  name: string;
  type: string;
  folderId: string;
  folderPath: string;
}

export interface CreateFolderRequest {
  name: string;
  parentId?: string;
  clinicId?: string;
  visibility?: Visibility;
}

export interface RegisterFileRequest {
  folderId: string;
  clinicId?: string;
  name: string;
  fileName: string;
  contentType: string;
  fileSize?: number;
  s3Key: string;
  visibility?: Visibility;
}

export interface PresignedUploadResponse {
  s3Key: string;
  uploadUrl: string;
}