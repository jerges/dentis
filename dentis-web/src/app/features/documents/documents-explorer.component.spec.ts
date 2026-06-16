import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { DocumentsExplorerComponent } from './documents-explorer.component';
import { DocumentsService } from '../../core/services/documents.service';
import { AuthService } from '../../core/services/auth.service';
import { DocumentFolder, DocumentSearchResult } from '../../core/models/documents.model';

describe('DocumentsExplorerComponent', () => {
  let component: DocumentsExplorerComponent;
  let fixture: ComponentFixture<DocumentsExplorerComponent>;
  let docsSvcSpy: jasmine.SpyObj<DocumentsService>;
  let authSvcSpy: jasmine.SpyObj<AuthService>;

  const normalFolder: DocumentFolder = {
    id: 'folder-1', clinicId: 'clinic-1', parentId: null, name: 'Facturas', path: '/Facturas',
    type: 'NORMAL', visibility: 'PUBLIC', ownerUserId: 'user-1', system: false
  };

  const kbFolder: DocumentFolder = {
    id: 'kb-1', clinicId: 'clinic-1', parentId: null, name: 'Base de Conocimiento',
    path: '/Base de Conocimiento', type: 'KNOWLEDGE_BASE', visibility: 'PRIVATE',
    ownerUserId: null, system: true
  };

  beforeEach(async () => {
    docsSvcSpy = jasmine.createSpyObj<DocumentsService>('DocumentsService', [
      'listFolders', 'listFiles', 'createFolder', 'updateFolder', 'deleteFolder',
      'getKnowledgeBase', 'presignUpload', 'uploadToS3', 'registerFile', 'downloadUrl',
      'updateFile', 'deleteFile', 'share', 'removeShare', 'shareTargets', 'search'
    ]);
    authSvcSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getToken', 'currentUser']);

    docsSvcSpy.listFolders.and.returnValue(of([normalFolder]));
    docsSvcSpy.listFiles.and.returnValue(of([]));
    docsSvcSpy.getKnowledgeBase.and.returnValue(of(kbFolder));
    authSvcSpy.getToken.and.returnValue('mock-jwt');

    await TestBed.configureTestingModule({
      imports: [DocumentsExplorerComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: DocumentsService, useValue: docsSvcSpy },
        { provide: AuthService, useValue: authSvcSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentsExplorerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should call listFolders on init and populate folders signal', () => {
    expect(docsSvcSpy.listFolders).toHaveBeenCalled();
    expect(component.folders()).toEqual([normalFolder]);
  });

  it('should start with breadcrumb at root (Inicio)', () => {
    expect(component.breadcrumb().length).toBe(1);
    expect(component.breadcrumb()[0].id).toBeNull();
    expect(component.breadcrumb()[0].name).toBe('Inicio');
  });

  it('openFolder() appends to breadcrumb and calls loadContent with folder id', () => {
    component.openFolder(normalFolder);

    expect(component.breadcrumb().length).toBe(2);
    expect(component.breadcrumb()[1].id).toBe('folder-1');
    expect(docsSvcSpy.listFolders).toHaveBeenCalledTimes(2);
    expect(docsSvcSpy.listFiles).toHaveBeenCalledWith('folder-1');
  });

  it('onSearch() with query < 2 chars clears search results', () => {
    component.searchResults.set([{ id: 'r1', name: 'x', type: 'FOLDER', folderId: 'f1', folderPath: '/x' }] as DocumentSearchResult[]);
    component.searchQuery = 'a';
    component.onSearch();
    expect(component.searchResults()).toEqual([]);
  });

  it('onSearch() with query >= 2 chars calls search service', () => {
    const mockResult: DocumentSearchResult[] = [{ id: 'r1', name: 'Radiografias', type: 'FOLDER', folderId: 'folder-1', folderPath: '/Radiografias' }];
    docsSvcSpy.search.and.returnValue(of(mockResult));

    component.searchQuery = 'Radio';
    component.onSearch();

    expect(docsSvcSpy.search).toHaveBeenCalledWith('Radio');
    expect(component.searchResults()).toEqual(mockResult);
  });

  it('clearSearch() resets query and search results', () => {
    component.searchQuery = 'something';
    component.searchResults.set([{ id: 'r1', name: 'x', type: 'FOLDER', folderId: 'f1', folderPath: '/x' }] as DocumentSearchResult[]);

    component.clearSearch();

    expect(component.searchQuery).toBe('');
    expect(component.searchResults()).toEqual([]);
  });
});
