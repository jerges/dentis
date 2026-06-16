package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.ClinicDocument;
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
@DisplayName("ClinicDocumentService")
class ClinicDocumentServiceTest {

    @Mock ClinicDocumentRepository documentRepo;
    @Mock DocumentFolderRepository folderRepo;

    @InjectMocks ClinicDocumentService service;

    private final UUID clinicId  = UUID.randomUUID();
    private final UUID folderId  = UUID.randomUUID();
    private final UUID docId     = UUID.randomUUID();
    private final UUID userId    = UUID.randomUUID();

    private DocumentFolder kbFolder;
    private DocumentFolder generalFolder;
    private ClinicDocument sampleDoc;

    @BeforeEach
    void setUp() {
        kbFolder = buildFolder(folderId, clinicId, DocumentZone.KNOWLEDGE_BASE);
        generalFolder = buildFolder(UUID.randomUUID(), clinicId, DocumentZone.GENERAL);
        sampleDoc = buildDoc(docId, clinicId, folderId, "protocol.pdf", "application/pdf");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("buildPresignInfo")
    class BuildPresignInfo {

        @Test
        @DisplayName("should return s3Key with folder prefix and uuid")
        void returnsS3KeyWithFolderPrefix() {
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbFolder));

            ClinicDocumentService.PresignResult result = service.buildPresignInfo(
                    clinicId,
                    new ClinicDocumentService.PresignRequest(folderId, "informe.pdf", "application/pdf"));

            assertThat(result.s3Key()).startsWith(kbFolder.getS3Prefix());
            assertThat(result.s3Key()).endsWith("-informe.pdf");
        }

        @Test
        @DisplayName("should be rejected when folder belongs to another clinic")
        void rejectedWhenWrongClinic() {
            UUID otherClinic = UUID.randomUUID();
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbFolder));

            assertThatThrownBy(() -> service.buildPresignInfo(otherClinic,
                    new ClinicDocumentService.PresignRequest(folderId, "file.pdf", "application/pdf")))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should be rejected when folder not found")
        void rejectedWhenFolderNotFound() {
            UUID unknown = UUID.randomUUID();
            given(folderRepo.findById(unknown)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.buildPresignInfo(clinicId,
                    new ClinicDocumentService.PresignRequest(unknown, "f.pdf", "application/pdf")))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should persist document with correct metadata and PUBLIC visibility")
        void persistsDocumentWithMetadata() {
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbFolder));
            given(documentRepo.save(any())).willReturn(sampleDoc);

            ClinicDocument result = service.register(clinicId, folderId, "protocol.pdf",
                    "application/pdf", "clinics/x/kb/uuid-protocol.pdf", 204800L, "Protocolo", userId,
                    DocumentVisibility.PUBLIC);

            assertThat(result.getFileName()).isEqualTo("protocol.pdf");
            assertThat(result.getClinicId()).isEqualTo(clinicId);

            ArgumentCaptor<ClinicDocument> captor = ArgumentCaptor.forClass(ClinicDocument.class);
            verify(documentRepo).save(captor.capture());
            assertThat(captor.getValue().isIndexedForIa()).isFalse();
            assertThat(captor.getValue().getVisibility()).isEqualTo(DocumentVisibility.PUBLIC);
        }

        @Test
        @DisplayName("should persist document with PRIVATE visibility when specified")
        void persistsWithPrivateVisibility() {
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbFolder));
            given(documentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ClinicDocument result = service.register(clinicId, folderId, "private.pdf",
                    "application/pdf", "clinics/x/kb/uuid-private.pdf", 1024L, null, userId,
                    DocumentVisibility.PRIVATE);

            assertThat(result.getVisibility()).isEqualTo(DocumentVisibility.PRIVATE);
        }

        @Test
        @DisplayName("should default to PUBLIC when visibility is null")
        void defaultsToPublic() {
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbFolder));
            given(documentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ClinicDocument result = service.register(clinicId, folderId, "default.pdf",
                    "application/pdf", "clinics/x/kb/uuid-default.pdf", 1024L, null, userId, null);

            assertThat(result.getVisibility()).isEqualTo(DocumentVisibility.PUBLIC);
        }

        @Test
        @DisplayName("should be rejected when folder belongs to another clinic")
        void rejectedWhenWrongClinic() {
            UUID otherClinic = UUID.randomUUID();
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbFolder));

            assertThatThrownBy(() -> service.register(otherClinic, folderId, "f.pdf",
                    "application/pdf", "key", 0L, null, userId, DocumentVisibility.PUBLIC))
                    .isInstanceOf(IllegalStateException.class);

            verify(documentRepo, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("listByFolder")
    class ListByFolder {

        @Test
        @DisplayName("SUPER_ADMIN should see all documents including PRIVATE")
        void superAdminSeesAll() {
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbFolder));
            given(documentRepo.findByFolderId(folderId)).willReturn(List.of(sampleDoc));

            List<ClinicDocument> result = service.listByFolder(folderId, clinicId, userId, true);

            assertThat(result).hasSize(1);
            verify(documentRepo).findByFolderId(folderId);
        }

        @Test
        @DisplayName("regular user should only see PUBLIC and own PRIVATE documents")
        void regularUserSeesVisible() {
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbFolder));
            given(documentRepo.findVisibleByFolderId(folderId, userId)).willReturn(List.of(sampleDoc));

            List<ClinicDocument> result = service.listByFolder(folderId, clinicId, userId, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFileName()).isEqualTo("protocol.pdf");
            verify(documentRepo).findVisibleByFolderId(folderId, userId);
        }

        @Test
        @DisplayName("should be rejected when folder belongs to another clinic")
        void rejectedWhenWrongClinic() {
            UUID otherClinic = UUID.randomUUID();
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbFolder));

            assertThatThrownBy(() -> service.listByFolder(folderId, otherClinic, userId, false))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("search")
    class Search {

        @Test
        @DisplayName("SUPER_ADMIN should search all documents")
        void superAdminSearchesAll() {
            given(documentRepo.search(clinicId, "protocolo")).willReturn(List.of(sampleDoc));

            List<ClinicDocument> result = service.search(clinicId, "protocolo", userId, true);

            assertThat(result).hasSize(1);
            verify(documentRepo).search(clinicId, "protocolo");
        }

        @Test
        @DisplayName("regular user should search only visible documents")
        void regularUserSearchesVisible() {
            given(documentRepo.searchVisible(clinicId, "protocolo", userId)).willReturn(List.of(sampleDoc));

            List<ClinicDocument> result = service.search(clinicId, "protocolo", userId, false);

            assertThat(result).hasSize(1);
            verify(documentRepo).searchVisible(clinicId, "protocolo", userId);
        }

        @Test
        @DisplayName("should return empty list when no results")
        void returnsEmpty() {
            given(documentRepo.searchVisible(clinicId, "xyz", userId)).willReturn(List.of());

            assertThat(service.search(clinicId, "xyz", userId, false)).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("deleteDocument")
    class DeleteDocument {

        @Test
        @DisplayName("should delete and return s3Key for caller to purge S3")
        void deletesAndReturnsKey() {
            given(documentRepo.findById(docId)).willReturn(Optional.of(sampleDoc));

            String key = service.deleteDocument(docId, clinicId);

            assertThat(key).isEqualTo(sampleDoc.getS3Key());
            verify(documentRepo).deleteById(docId);
        }

        @Test
        @DisplayName("should be rejected when document belongs to another clinic")
        void rejectedWhenWrongClinic() {
            UUID otherClinic = UUID.randomUUID();
            given(documentRepo.findById(docId)).willReturn(Optional.of(sampleDoc));

            assertThatThrownBy(() -> service.deleteDocument(docId, otherClinic))
                    .isInstanceOf(IllegalStateException.class);

            verify(documentRepo, never()).deleteById(any());
        }

        @Test
        @DisplayName("should be rejected when document not found")
        void rejectedWhenNotFound() {
            given(documentRepo.findById(docId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteDocument(docId, clinicId))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("isKnowledgeBaseFolder")
    class IsKnowledgeBaseFolder {

        @Test
        @DisplayName("should return true for PUBLIC KB zone folder")
        void trueForPublicKbFolder() {
            given(folderRepo.findById(folderId)).willReturn(Optional.of(kbFolder));

            assertThat(service.isKnowledgeBaseFolder(folderId)).isTrue();
        }

        @Test
        @DisplayName("should return false for PRIVATE KB folder (IA must not index it)")
        void falseForPrivateKbFolder() {
            DocumentFolder privateKb = buildFolder(folderId, clinicId, DocumentZone.KNOWLEDGE_BASE,
                    DocumentVisibility.PRIVATE);
            given(folderRepo.findById(folderId)).willReturn(Optional.of(privateKb));

            assertThat(service.isKnowledgeBaseFolder(folderId)).isFalse();
        }

        @Test
        @DisplayName("should return false for GENERAL zone folder")
        void falseForGeneralFolder() {
            UUID generalId = generalFolder.getId();
            given(folderRepo.findById(generalId)).willReturn(Optional.of(generalFolder));

            assertThat(service.isKnowledgeBaseFolder(generalId)).isFalse();
        }

        @Test
        @DisplayName("should return false when folder not found")
        void falseWhenNotFound() {
            UUID unknown = UUID.randomUUID();
            given(folderRepo.findById(unknown)).willReturn(Optional.empty());

            assertThat(service.isKnowledgeBaseFolder(unknown)).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("markIndexed")
    class MarkIndexed {

        @Test
        @DisplayName("should delegate to repository")
        void delegatesToRepository() {
            service.markIndexed(docId);

            verify(documentRepo).markIndexed(docId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private DocumentFolder buildFolder(UUID id, UUID clinicId, DocumentZone zone) {
        return buildFolder(id, clinicId, zone, DocumentVisibility.PUBLIC);
    }

    private DocumentFolder buildFolder(UUID id, UUID clinicId, DocumentZone zone,
                                        DocumentVisibility visibility) {
        String prefix = "clinics/" + clinicId + "/" + zone.name().toLowerCase() + "/";
        return DocumentFolder.builder()
                .id(id).clinicId(clinicId).parentId(null)
                .name(zone == DocumentZone.KNOWLEDGE_BASE ? "Base de Conocimiento IA" : "General")
                .s3Prefix(prefix).zone(zone)
                .system(zone == DocumentZone.KNOWLEDGE_BASE)
                .visibility(visibility)
                .createdBy(userId).createdAt(LocalDateTime.now())
                .build();
    }

    private ClinicDocument buildDoc(UUID id, UUID clinicId, UUID folderId,
                                     String fileName, String contentType) {
        return ClinicDocument.builder()
                .id(id).clinicId(clinicId).folderId(folderId)
                .fileName(fileName).contentType(contentType)
                .s3Key("clinics/" + clinicId + "/kb/uuid-" + fileName)
                .fileSize(102400L).description(null)
                .uploadedBy(userId).uploadedAt(LocalDateTime.now())
                .indexedForIa(false)
                .visibility(DocumentVisibility.PUBLIC)
                .build();
    }
}
