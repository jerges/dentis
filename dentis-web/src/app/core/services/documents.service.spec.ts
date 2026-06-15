import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DocumentsService } from './documents.service';
import { DocumentFolder, ClinicDocument } from '../models/documents.model';

const API = '/api/v1/documents';

const folder: DocumentFolder = {
  id: 'folder-1',
  parentId: null,
  name: 'Facturas',
  s3Prefix: 'clinics/c1/general/',
  zone: 'GENERAL',
  system: false,
  createdAt: '2026-01-01T10:00:00'
};

const kbFolder: DocumentFolder = {
  id: 'folder-kb',
  parentId: null,
  name: 'Base de Conocimiento IA',
  s3Prefix: 'clinics/c1/knowledge-base/',
  zone: 'KNOWLEDGE_BASE',
  system: true,
  createdAt: '2026-01-01T10:00:00'
};

const doc: ClinicDocument = {
  id: 'doc-1',
  folderId: 'folder-1',
  fileName: 'factura.pdf',
  contentType: 'application/pdf',
  fileSize: 51200,
  description: null,
  indexedForIa: false,
  uploadedAt: '2026-01-01T10:00:00'
};

describe('DocumentsService', () => {
  let service: DocumentsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DocumentsService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(DocumentsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('listFolders', () => {
    it('should GET /folders without params when no parentId given', () => {
      service.listFolders().subscribe();
      const req = httpMock.expectOne(`${API}/folders`);
      expect(req.request.method).toBe('GET');
      req.flush([folder]);
    });

    it('should include parentId param when provided', () => {
      service.listFolders('folder-1').subscribe();
      const req = httpMock.expectOne(r => r.url === `${API}/folders` && r.params.has('parentId'));
      expect(req.request.params.get('parentId')).toBe('folder-1');
      req.flush([folder]);
    });
  });

  describe('createFolder', () => {
    it('should POST to /folders with request body', () => {
      const payload = { parentId: null, name: 'Protocolos', zone: 'GENERAL' as const };
      let result: DocumentFolder | undefined;
      service.createFolder(payload).subscribe(r => (result = r));

      const req = httpMock.expectOne(`${API}/folders`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush(folder);
      expect(result).toEqual(folder);
    });
  });

  describe('deleteFolder', () => {
    it('should DELETE /folders/{id}', () => {
      service.deleteFolder('folder-1').subscribe();
      const req = httpMock.expectOne(`${API}/folders/folder-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('initKnowledgeBase', () => {
    it('should POST to /folders/kb/init', () => {
      let result: DocumentFolder | undefined;
      service.initKnowledgeBase().subscribe(r => (result = r));

      const req = httpMock.expectOne(`${API}/folders/kb/init`);
      expect(req.request.method).toBe('POST');
      req.flush(kbFolder);
      expect(result).toEqual(kbFolder);
    });
  });

  describe('presignUpload', () => {
    it('should POST to /presign and return uploadUrl and s3Key', () => {
      const payload = { folderId: 'folder-1', fileName: 'doc.pdf', contentType: 'application/pdf' };
      let result: { uploadUrl: string; s3Key: string } | undefined;
      service.presignUpload(payload).subscribe(r => (result = r));

      const req = httpMock.expectOne(`${API}/presign`);
      expect(req.request.method).toBe('POST');
      req.flush({ uploadUrl: 'https://s3.example.com/presign', s3Key: 'clinics/c1/general/uuid-doc.pdf' });
      expect(result?.uploadUrl).toBe('https://s3.example.com/presign');
      expect(result?.s3Key).toContain('uuid-doc.pdf');
    });
  });

  describe('uploadToS3', () => {
    it('should PUT to the presigned URL with Content-Type header', () => {
      const file = new File(['content'], 'doc.pdf', { type: 'application/pdf' });
      service.uploadToS3('https://s3.example.com/presign', file).subscribe();

      const req = httpMock.expectOne('https://s3.example.com/presign');
      expect(req.request.method).toBe('PUT');
      expect(req.request.headers.get('Content-Type')).toBe('application/pdf');
      req.flush(null);
    });
  });

  describe('registerDocument', () => {
    it('should POST to root endpoint and return saved document', () => {
      const payload = {
        folderId: 'folder-1', fileName: 'factura.pdf', contentType: 'application/pdf',
        s3Key: 'clinics/c1/general/uuid-factura.pdf', fileSize: 51200, description: null
      };
      let result: ClinicDocument | undefined;
      service.registerDocument(payload).subscribe(r => (result = r));

      const req = httpMock.expectOne(API);
      expect(req.request.method).toBe('POST');
      req.flush(doc);
      expect(result?.fileName).toBe('factura.pdf');
    });
  });

  describe('listDocuments', () => {
    it('should GET with folderId param', () => {
      let result: ClinicDocument[] | undefined;
      service.listDocuments('folder-1').subscribe(r => (result = r));

      const req = httpMock.expectOne(r => r.url === API && r.params.get('folderId') === 'folder-1');
      expect(req.request.method).toBe('GET');
      req.flush([doc]);
      expect(result).toEqual([doc]);
    });
  });

  describe('search', () => {
    it('should GET /search with q param', () => {
      let result: ClinicDocument[] | undefined;
      service.search('protocolo').subscribe(r => (result = r));

      const req = httpMock.expectOne(r => r.url === `${API}/search` && r.params.get('q') === 'protocolo');
      expect(req.request.method).toBe('GET');
      req.flush([doc]);
      expect(result).toEqual([doc]);
    });
  });

  describe('downloadUrl', () => {
    it('should GET /{id}/download and return downloadUrl', () => {
      let result: { downloadUrl: string } | undefined;
      service.downloadUrl('doc-1').subscribe(r => (result = r));

      const req = httpMock.expectOne(`${API}/doc-1/download`);
      expect(req.request.method).toBe('GET');
      req.flush({ downloadUrl: 'https://s3.example.com/download' });
      expect(result?.downloadUrl).toBe('https://s3.example.com/download');
    });
  });

  describe('deleteDocument', () => {
    it('should DELETE /{id}', () => {
      service.deleteDocument('doc-1').subscribe();
      const req = httpMock.expectOne(`${API}/doc-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
