import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  DocumentFolder, DocumentFile, DocumentShare, ShareTarget,
  DocumentSearchResult, CreateFolderRequest, RegisterFileRequest,
  PresignedUploadResponse, Visibility, ResourceType
} from '../models/documents.model';
import { ApiResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly base = '/api/v1/documents';

  constructor(private readonly http: HttpClient) {}

  // ── Folders ──────────────────────────────────────────────────────────────

  listFolders(clinicId?: string, parentId?: string): Observable<DocumentFolder[]> {
    let url = `${this.base}/folders?`;
    if (clinicId) url += `clinicId=${clinicId}&`;
    if (parentId) url += `parentId=${parentId}`;
    return this.http.get<ApiResponse<DocumentFolder[]>>(url).pipe(map(r => r.data));
  }

  createFolder(req: CreateFolderRequest): Observable<DocumentFolder> {
    return this.http.post<ApiResponse<DocumentFolder>>(`${this.base}/folders`, req)
      .pipe(map(r => r.data));
  }

  updateFolder(id: string, req: { name?: string; visibility?: Visibility }): Observable<DocumentFolder> {
    return this.http.patch<ApiResponse<DocumentFolder>>(`${this.base}/folders/${id}`, req)
      .pipe(map(r => r.data));
  }

  deleteFolder(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/folders/${id}`)
      .pipe(map(() => void 0));
  }

  getKnowledgeBase(clinicId?: string): Observable<DocumentFolder> {
    const url = clinicId ? `${this.base}/kb?clinicId=${clinicId}` : `${this.base}/kb`;
    return this.http.get<ApiResponse<DocumentFolder>>(url).pipe(map(r => r.data));
  }

  // ── Files ────────────────────────────────────────────────────────────────

  listFiles(folderId: string): Observable<DocumentFile[]> {
    return this.http.get<ApiResponse<DocumentFile[]>>(`${this.base}/folders/${folderId}/files`)
      .pipe(map(r => r.data));
  }

  presignUpload(folderId: string, fileName: string, contentType: string, clinicId?: string): Observable<PresignedUploadResponse> {
    return this.http.post<ApiResponse<PresignedUploadResponse>>(`${this.base}/files/presign`,
      { folderId, fileName, contentType, clinicId })
      .pipe(map(r => r.data));
  }

  uploadToS3(uploadUrl: string, file: File): Observable<void> {
    return this.http.put<void>(uploadUrl, file, {
      headers: { 'Content-Type': file.type }
    });
  }

  registerFile(req: RegisterFileRequest): Observable<DocumentFile> {
    return this.http.post<ApiResponse<DocumentFile>>(`${this.base}/files`, req)
      .pipe(map(r => r.data));
  }

  downloadUrl(id: string): Observable<string> {
    return this.http.get<ApiResponse<string>>(`${this.base}/files/${id}/download`)
      .pipe(map(r => r.data));
  }

  updateFile(id: string, req: { visibility?: Visibility; folderId?: string }): Observable<DocumentFile> {
    return this.http.patch<ApiResponse<DocumentFile>>(`${this.base}/files/${id}`, req)
      .pipe(map(r => r.data));
  }

  deleteFile(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/files/${id}`)
      .pipe(map(() => void 0));
  }

  // ── Shares ────────────────────────────────────────────────────────────────

  share(resourceType: ResourceType, resourceId: string, sharedWithUserId: string): Observable<DocumentShare> {
    return this.http.post<ApiResponse<DocumentShare>>(`${this.base}/shares`,
      { resourceType, resourceId, sharedWithUserId })
      .pipe(map(r => r.data));
  }

  removeShare(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/shares/${id}`)
      .pipe(map(() => void 0));
  }

  shareTargets(): Observable<ShareTarget[]> {
    return this.http.get<ApiResponse<ShareTarget[]>>(`${this.base}/share-targets`)
      .pipe(map(r => r.data));
  }

  // ── Search ────────────────────────────────────────────────────────────────

  search(q: string): Observable<DocumentSearchResult[]> {
    return this.http.get<ApiResponse<DocumentSearchResult[]>>(`${this.base}/search?q=${encodeURIComponent(q)}`)
      .pipe(map(r => r.data));
  }
}