package com.adakadavra.dentis.documents.infrastructure.persistence.mapper;

import com.adakadavra.dentis.documents.domain.model.*;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DocumentEntityMapper")
class DocumentEntityMapperTest {

    private DocumentEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DocumentEntityMapper();
    }

    // ── DocumentFolder ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toEntity(DocumentFolder)")
    class FolderToEntity {

        @Test
        @DisplayName("should return null when folder is null")
        void shouldReturnNullWhenFolderIsNull() {
            assertThat(mapper.toEntity((DocumentFolder) null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from folder domain to entity")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();
            UUID clinicId = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now();

            DocumentFolder folder = DocumentFolder.builder()
                    .id(id)
                    .clinicId(clinicId)
                    .parentId(parentId)
                    .name("Facturas 2026")
                    .path("/Facturas/2026")
                    .type(FolderType.NORMAL)
                    .visibility(Visibility.PRIVATE)
                    .ownerUserId(ownerUserId)
                    .system(false)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();

            DocumentFolderEntity entity = mapper.toEntity(folder);

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getClinicId()).isEqualTo(clinicId);
            assertThat(entity.getParentId()).isEqualTo(parentId);
            assertThat(entity.getName()).isEqualTo("Facturas 2026");
            assertThat(entity.getPath()).isEqualTo("/Facturas/2026");
            assertThat(entity.getType()).isEqualTo(FolderType.NORMAL);
            assertThat(entity.getVisibility()).isEqualTo(Visibility.PRIVATE);
            assertThat(entity.getOwnerUserId()).isEqualTo(ownerUserId);
            assertThat(entity.isSystem()).isFalse();
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("should map system folder flag to entity")
        void shouldMapSystemFolderFlag() {
            DocumentFolder folder = DocumentFolder.builder()
                    .id(UUID.randomUUID())
                    .clinicId(UUID.randomUUID())
                    .name("Base de Conocimiento")
                    .path("/knowledge")
                    .type(FolderType.KNOWLEDGE_BASE)
                    .visibility(Visibility.PUBLIC)
                    .system(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            assertThat(mapper.toEntity(folder).isSystem()).isTrue();
            assertThat(mapper.toEntity(folder).getType()).isEqualTo(FolderType.KNOWLEDGE_BASE);
        }
    }

    @Nested
    @DisplayName("toDomain(DocumentFolderEntity)")
    class FolderToDomain {

        @Test
        @DisplayName("should return null when folder entity is null")
        void shouldReturnNullWhenEntityIsNull() {
            assertThat(mapper.toDomain((DocumentFolderEntity) null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from folder entity to domain")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();
            UUID clinicId = UUID.randomUUID();
            LocalDateTime createdAt = LocalDateTime.now().minusHours(2);
            LocalDateTime updatedAt = LocalDateTime.now();

            DocumentFolderEntity entity = DocumentFolderEntity.builder()
                    .id(id)
                    .clinicId(clinicId)
                    .parentId(null)
                    .name("Radiografías")
                    .path("/Radiografías")
                    .type(FolderType.NORMAL)
                    .visibility(Visibility.SHARED)
                    .ownerUserId(UUID.randomUUID())
                    .system(false)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();

            DocumentFolder domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(id);
            assertThat(domain.getClinicId()).isEqualTo(clinicId);
            assertThat(domain.getParentId()).isNull();
            assertThat(domain.getName()).isEqualTo("Radiografías");
            assertThat(domain.getPath()).isEqualTo("/Radiografías");
            assertThat(domain.getType()).isEqualTo(FolderType.NORMAL);
            assertThat(domain.getVisibility()).isEqualTo(Visibility.SHARED);
            assertThat(domain.isSystem()).isFalse();
            assertThat(domain.getCreatedAt()).isEqualTo(createdAt);
            assertThat(domain.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }

    // ── DocumentFile ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toEntity(DocumentFile)")
    class FileToEntity {

        @Test
        @DisplayName("should return null when file is null")
        void shouldReturnNullWhenFileIsNull() {
            assertThat(mapper.toEntity((DocumentFile) null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from file domain to entity")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();
            UUID clinicId = UUID.randomUUID();
            UUID folderId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
            LocalDateTime updatedAt = LocalDateTime.now();

            DocumentFile file = DocumentFile.builder()
                    .id(id)
                    .clinicId(clinicId)
                    .folderId(folderId)
                    .name("Radiografía panorámica")
                    .fileName("rx_panoramica.dcm")
                    .contentType("application/dicom")
                    .fileSize(2_048_000L)
                    .s3Key("clinics/abc/rx_panoramica.dcm")
                    .visibility(Visibility.PRIVATE)
                    .ownerUserId(ownerUserId)
                    .indexedForIa(true)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();

            DocumentFileEntity entity = mapper.toEntity(file);

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getClinicId()).isEqualTo(clinicId);
            assertThat(entity.getFolderId()).isEqualTo(folderId);
            assertThat(entity.getName()).isEqualTo("Radiografía panorámica");
            assertThat(entity.getFileName()).isEqualTo("rx_panoramica.dcm");
            assertThat(entity.getContentType()).isEqualTo("application/dicom");
            assertThat(entity.getFileSize()).isEqualTo(2_048_000L);
            assertThat(entity.getS3Key()).isEqualTo("clinics/abc/rx_panoramica.dcm");
            assertThat(entity.getVisibility()).isEqualTo(Visibility.PRIVATE);
            assertThat(entity.getOwnerUserId()).isEqualTo(ownerUserId);
            assertThat(entity.isIndexedForIa()).isTrue();
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }

    @Nested
    @DisplayName("toDomain(DocumentFileEntity)")
    class FileToDomain {

        @Test
        @DisplayName("should return null when file entity is null")
        void shouldReturnNullWhenEntityIsNull() {
            assertThat(mapper.toDomain((DocumentFileEntity) null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from file entity to domain")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();

            DocumentFileEntity entity = DocumentFileEntity.builder()
                    .id(id)
                    .clinicId(UUID.randomUUID())
                    .folderId(UUID.randomUUID())
                    .name("Consentimiento")
                    .fileName("consentimiento.pdf")
                    .contentType("application/pdf")
                    .fileSize(512_000L)
                    .s3Key("clinics/def/consentimiento.pdf")
                    .visibility(Visibility.PUBLIC)
                    .ownerUserId(UUID.randomUUID())
                    .indexedForIa(false)
                    .createdAt(LocalDateTime.now().minusMinutes(30))
                    .updatedAt(LocalDateTime.now())
                    .build();

            DocumentFile domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(id);
            assertThat(domain.getName()).isEqualTo("Consentimiento");
            assertThat(domain.getFileName()).isEqualTo("consentimiento.pdf");
            assertThat(domain.getContentType()).isEqualTo("application/pdf");
            assertThat(domain.getFileSize()).isEqualTo(512_000L);
            assertThat(domain.getS3Key()).isEqualTo("clinics/def/consentimiento.pdf");
            assertThat(domain.getVisibility()).isEqualTo(Visibility.PUBLIC);
            assertThat(domain.isIndexedForIa()).isFalse();
        }
    }

    // ── DocumentShare ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toEntity(DocumentShare)")
    class ShareToEntity {

        @Test
        @DisplayName("should return null when share is null")
        void shouldReturnNullWhenShareIsNull() {
            assertThat(mapper.toEntity((DocumentShare) null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from share domain to entity")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();
            UUID resourceId = UUID.randomUUID();
            UUID sharedWithUserId = UUID.randomUUID();
            LocalDateTime createdAt = LocalDateTime.now();

            DocumentShare share = DocumentShare.builder()
                    .id(id)
                    .resourceType(ResourceType.FILE)
                    .resourceId(resourceId)
                    .sharedWithUserId(sharedWithUserId)
                    .createdAt(createdAt)
                    .build();

            DocumentShareEntity entity = mapper.toEntity(share);

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getResourceType()).isEqualTo(ResourceType.FILE);
            assertThat(entity.getResourceId()).isEqualTo(resourceId);
            assertThat(entity.getSharedWithUserId()).isEqualTo(sharedWithUserId);
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("toDomain(DocumentShareEntity)")
    class ShareToDomain {

        @Test
        @DisplayName("should return null when share entity is null")
        void shouldReturnNullWhenEntityIsNull() {
            assertThat(mapper.toDomain((DocumentShareEntity) null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from share entity to domain")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();
            UUID resourceId = UUID.randomUUID();
            UUID sharedWithUserId = UUID.randomUUID();
            LocalDateTime createdAt = LocalDateTime.now();

            DocumentShareEntity entity = buildShareEntity(id, ResourceType.FOLDER, resourceId, sharedWithUserId, createdAt);

            DocumentShare domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(id);
            assertThat(domain.getResourceType()).isEqualTo(ResourceType.FOLDER);
            assertThat(domain.getResourceId()).isEqualTo(resourceId);
            assertThat(domain.getSharedWithUserId()).isEqualTo(sharedWithUserId);
            assertThat(domain.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    private DocumentShareEntity buildShareEntity(UUID id, ResourceType type, UUID resourceId,
                                                  UUID sharedWithUserId, LocalDateTime createdAt) {
        return DocumentShareEntity.builder()
                .id(id)
                .resourceType(type)
                .resourceId(resourceId)
                .sharedWithUserId(sharedWithUserId)
                .createdAt(createdAt)
                .build();
    }
}
