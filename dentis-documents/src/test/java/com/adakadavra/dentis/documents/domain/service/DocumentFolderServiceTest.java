package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.DocumentVisibility;
import com.adakadavra.dentis.documents.domain.model.DocumentZone;
import com.adakadavra.dentis.documents.domain.repository.ClinicDocumentRepository;
import com.adakadavra.dentis.documents.domain.repository.DocumentFolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentFolderService")
class DocumentFolderServiceTest {

    @Mock DocumentFolderRepository folderRepo;
    @Mock ClinicDocumentRepository documentRepo;

    @InjectMocks DocumentFolderService service;

    private final UUID clinicId   = UUID.randomUUID();
    private final UUID userId     = UUID.randomUUID();
    private final UUID folderId   = UUID.randomUUID();

    private DocumentFolder kbRoot;
    private DocumentFolder generalFolder;

    @BeforeEach
    void setUp() {
        kbRoot = folder(folderId, clinicId, null, "Base de Conocimiento IA",
                "clinics/" + clinicId + "/knowledge-base/", DocumentZone.KNOWLEDGE_BASE, true);
        generalFolder = folder(UUID.randomUUID(), clinicId, null, "Facturas",
                "clinics/" + clinicId + "/general/", DocumentZone.GENERAL, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("ensureKnowledgeBaseRoot")
    class EnsureKnowledgeBaseRoot {

        @Test
        @DisplayName("should return existing root when already created")
        void returnsExistingRoot() {
            given(folderRepo.findByClinicIdAndZone(clinicId, DocumentZone.KNOWLEDGE_BASE))
                    .willReturn(List.of(kbRoot));

            DocumentFolder result = service.ensureKnowledgeBaseRoot(clinicId, userId);

            assertThat(result.getId()).isEqualTo(folderId);
            verify(folderRepo, never()).save(any());
        }

        @Test
        @DisplayName("should create root when none exists")
        void createsRootWhenNoneExists() {
            given(folderRepo.findByClinicIdAndZone(clinicId, DocumentZone.KNOWLEDGE_BASE))
                    .willReturn(List.of());
            given(folderRepo.save(any())).willReturn(kbRoot);

            service.ensureKnowledgeBaseRoot(clinicId, userId);

            ArgumentCaptor<DocumentFolder> captor = ArgumentCaptor.forClass(DocumentFolder.class);
            verify(folderRepo).save(captor.capture());
            DocumentFolder saved = captor.getValue();
            assertThat(saved.isSystem()).isTrue();
            assertThat(saved.getZone()).isEqualTo(DocumentZone.KNOWLEDGE_BASE);
            assertThat(saved.getParentId()).isNull();
            assertThat(saved.getS3Prefix()).contains("knowledge-base");
            assertThat(saved.getVisibility()).isEqualTo(DocumentVisibility.PUBLIC);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("createFolder")
    class CreateFolder {

        @Test
        @DisplayName("should be created at root when parentId is null with PUBLIC visibility")
        void createdAtRoot() {
            given(folderRepo.existsByClinicIdAndParentIdAndName(clinicId, null, "Contratos")).willReturn(false);
            given(folderRepo.save(any())).willReturn(generalFolder);

            service.createFolder(clinicId, null, "Contratos", DocumentZone.GENERAL, userId, DocumentVisibility.PUBLIC);

            ArgumentCaptor<DocumentFolder> captor = ArgumentCaptor.forClass(DocumentFolder.class);
            verify(folderRepo).save(captor.capture());
            assertThat(captor.getValue().getParentId()).isNull();
            assertThat(captor.getValue().getZone()).isEqualTo(DocumentZone.GENERAL);
            assertThat(captor.getValue().getVisibility()).isEqualTo(DocumentVisibility.PUBLIC);
        }

        @Test
        @DisplayName("should be created with PRIVATE visibility when specified")
        void createdWithPrivateVisibility() {
            given(folderRepo.existsByClinicIdAndParentIdAndName(clinicId, null, "MisNotas")).willReturn(false);
            given(folderRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            DocumentFolder result = service.createFolder(clinicId, null, "MisNotas",
                    DocumentZone.GENERAL, userId, DocumentVisibility.PRIVATE);

            assertThat(result.getVisibility()).isEqualTo(DocumentVisibility.PRIVATE);
        }

        @Test
        @DisplayName("should default to PUBLIC when visibility is null")
        void defaultsToPublicWhenNullVisibility() {
            given(folderRepo.existsByClinicIdAndParentIdAndName(clinicId, null, "Default")).willReturn(false);
            given(folderRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            DocumentFolder result = service.createFolder(clinicId, null, "Default",
                    DocumentZone.GENERAL, userId, null);

            assertThat(result.getVisibility()).isEqualTo(DocumentVisibility.PUBLIC);
        }

        @Test
        @DisplayName("should be rejected when name already exists in same parent")
        void rejectedWhenNameExists() {
            given(folderRepo.existsByClinicIdAndParentIdAndName(clinicId, null, "Facturas")).willReturn(true);

            assertThatThrownBy(() -> service.createFolder(clinicId, null, "Facturas",
                    DocumentZone.GENERAL, userId, DocumentVisibility.PUBLIC))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Facturas");

            verify(folderRepo, never()).save(any());
        }

        @Test
        @DisplayName("should inherit parent s3Prefix in child s3Prefix")
        void childInheritsParentPrefix() {
            UUID parentId = UUID.randomUUID();
            DocumentFolder parent = folder(parentId, clinicId, null, "Protocolos",
                    "clinics/" + clinicId + "/knowledge-base/protocolos/", DocumentZone.KNOWLEDGE_BASE, false);
            given(folderRepo.existsByClinicIdAndParentIdAndName(clinicId, parentId, "2024")).willReturn(false);
            given(folderRepo.findById(parentId)).willReturn(Optional.of(parent));
            given(folderRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            DocumentFolder child = service.createFolder(clinicId, parentId, "2024",
                    DocumentZone.KNOWLEDGE_BASE, userId, DocumentVisibility.PUBLIC);

            assertThat(child.getS3Prefix()).startsWith(parent.getS3Prefix());
            assertThat(child.getS3Prefix()).contains("2024");
        }

        @Test
        @DisplayName("should be rejected when parent folder not found")
        void rejectedWhenParentNotFound() {
            UUID unknownParent = UUID.randomUUID();
            given(folderRepo.existsByClinicIdAndParentIdAndName(clinicId, unknownParent, "Sub")).willReturn(false);
            given(folderRepo.findById(unknownParent)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.createFolder(clinicId, unknownParent, "Sub",
                    DocumentZone.GENERAL, userId, DocumentVisibility.PUBLIC))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("deleteFolder")
    class DeleteFolder {

        @Test
        @DisplayName("should be deleted when non-system folder belongs to clinic")
        void deletedSuccessfully() {
            given(folderRepo.findById(generalFolder.getId())).willReturn(Optional.of(generalFolder));

            service.deleteFolder(generalFolder.getId(), clinicId);

            verify(folderRepo).deleteById(generalFolder.getId());
        }

        @Test
        @DisplayName("should be rejected when folder is system-protected")
        void rejectedForSystemFolder() {
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbRoot));

            assertThatThrownBy(() -> service.deleteFolder(folderId, clinicId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot be deleted");

            verify(folderRepo, never()).deleteById(any());
        }

        @Test
        @DisplayName("should be rejected when folder belongs to another clinic")
        void rejectedWhenWrongClinic() {
            UUID otherClinic = UUID.randomUUID();
            given(folderRepo.findById(generalFolder.getId())).willReturn(Optional.of(generalFolder));

            assertThatThrownBy(() -> service.deleteFolder(generalFolder.getId(), otherClinic))
                    .isInstanceOf(IllegalStateException.class);

            verify(folderRepo, never()).deleteById(any());
        }

        @Test
        @DisplayName("should be rejected when folder not found")
        void rejectedWhenNotFound() {
            UUID unknown = UUID.randomUUID();
            given(folderRepo.findById(unknown)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteFolder(unknown, clinicId))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("listChildren")
    class ListChildren {

        @Test
        @DisplayName("SUPER_ADMIN should see all folders regardless of visibility")
        void superAdminSeesAll() {
            given(folderRepo.findByClinicIdAndParentId(clinicId, folderId))
                    .willReturn(List.of(generalFolder));

            List<DocumentFolder> result = service.listChildren(clinicId, folderId, userId, true);

            assertThat(result).hasSize(1);
            verify(folderRepo).findByClinicIdAndParentId(clinicId, folderId);
        }

        @Test
        @DisplayName("regular user should only see PUBLIC and own PRIVATE folders")
        void regularUserSeesVisible() {
            given(folderRepo.findVisibleByClinicIdAndParentId(clinicId, folderId, userId))
                    .willReturn(List.of(generalFolder));

            List<DocumentFolder> result = service.listChildren(clinicId, folderId, userId, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Facturas");
            verify(folderRepo).findVisibleByClinicIdAndParentId(clinicId, folderId, userId);
        }

        @Test
        @DisplayName("should return empty list when no visible subfolders exist")
        void returnsEmpty() {
            given(folderRepo.findVisibleByClinicIdAndParentId(clinicId, folderId, userId)).willReturn(List.of());

            assertThat(service.listChildren(clinicId, folderId, userId, false)).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("visibility on ensureKnowledgeBaseRoot")
    class KbRootVisibility {

        @Test
        @DisplayName("KB root is always created as PUBLIC")
        void kbRootAlwaysPublic() {
            given(folderRepo.findByClinicIdAndZone(clinicId, DocumentZone.KNOWLEDGE_BASE))
                    .willReturn(List.of());
            given(folderRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            DocumentFolder root = service.ensureKnowledgeBaseRoot(clinicId, userId);

            assertThat(root.getVisibility()).isEqualTo(DocumentVisibility.PUBLIC);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private DocumentFolder folder(UUID id, UUID clinicId, UUID parentId, String name,
                                   String prefix, DocumentZone zone, boolean system) {
        return DocumentFolder.builder()
                .id(id).clinicId(clinicId).parentId(parentId).name(name)
                .s3Prefix(prefix).zone(zone).system(system)
                .visibility(DocumentVisibility.PUBLIC)
                .createdBy(userId).createdAt(LocalDateTime.now())
                .build();
    }
}
