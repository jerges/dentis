package com.adakadavra.dentis.documents.infrastructure.persistence.mapper;

import com.adakadavra.dentis.documents.domain.model.ClinicDocument;
import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.DocumentVisibility;
import com.adakadavra.dentis.documents.domain.model.DocumentZone;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.ClinicDocumentEntity;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.DocumentFolderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DocumentMapper")
class DocumentMapperTest {

    private final DocumentMapper mapper = new DocumentMapper();

    private final UUID id       = UUID.randomUUID();
    private final UUID clinicId = UUID.randomUUID();
    private final UUID userId   = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("DocumentFolder")
    class FolderMapping {

        @Test
        @DisplayName("entity → domain preserves all fields")
        void entityToDomainPreservesAllFields() {
            DocumentFolderEntity entity = DocumentFolderEntity.builder()
                    .id(id).clinicId(clinicId).parentId(null)
                    .name("Base de Conocimiento IA")
                    .s3Prefix("clinics/" + clinicId + "/knowledge-base/")
                    .zone(DocumentZone.KNOWLEDGE_BASE).systemFolder(true)
                    .visibility(DocumentVisibility.PUBLIC)
                    .createdBy(userId).createdAt(now)
                    .build();

            DocumentFolder domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(id);
            assertThat(domain.getClinicId()).isEqualTo(clinicId);
            assertThat(domain.getParentId()).isNull();
            assertThat(domain.getName()).isEqualTo("Base de Conocimiento IA");
            assertThat(domain.getZone()).isEqualTo(DocumentZone.KNOWLEDGE_BASE);
            assertThat(domain.isSystem()).isTrue();
            assertThat(domain.getVisibility()).isEqualTo(DocumentVisibility.PUBLIC);
            assertThat(domain.getCreatedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("domain → entity preserves all fields")
        void domainToEntityPreservesAllFields() {
            DocumentFolder domain = DocumentFolder.builder()
                    .id(id).clinicId(clinicId).parentId(null)
                    .name("Facturas").s3Prefix("clinics/" + clinicId + "/general/")
                    .zone(DocumentZone.GENERAL).system(false)
                    .visibility(DocumentVisibility.PRIVATE)
                    .createdBy(userId).createdAt(now)
                    .build();

            DocumentFolderEntity entity = mapper.toEntity(domain);

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getName()).isEqualTo("Facturas");
            assertThat(entity.getZone()).isEqualTo(DocumentZone.GENERAL);
            assertThat(entity.isSystemFolder()).isFalse();
            assertThat(entity.getVisibility()).isEqualTo(DocumentVisibility.PRIVATE);
        }

        @Test
        @DisplayName("entity → domain → entity roundtrip is lossless")
        void roundtripIsLossless() {
            DocumentFolderEntity original = DocumentFolderEntity.builder()
                    .id(id).clinicId(clinicId).parentId(UUID.randomUUID())
                    .name("Protocolos").s3Prefix("clinics/x/kb/protocolos/")
                    .zone(DocumentZone.KNOWLEDGE_BASE).systemFolder(false)
                    .visibility(DocumentVisibility.PUBLIC)
                    .createdBy(userId).createdAt(now)
                    .build();

            DocumentFolderEntity roundtripped = mapper.toEntity(mapper.toDomain(original));

            assertThat(roundtripped.getId()).isEqualTo(original.getId());
            assertThat(roundtripped.getName()).isEqualTo(original.getName());
            assertThat(roundtripped.getZone()).isEqualTo(original.getZone());
            assertThat(roundtripped.getParentId()).isEqualTo(original.getParentId());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("ClinicDocument")
    class DocumentMapping {

        private final UUID folderId = UUID.randomUUID();

        @Test
        @DisplayName("entity → domain preserves all fields including indexedForIa")
        void entityToDomainPreservesAllFields() {
            ClinicDocumentEntity entity = ClinicDocumentEntity.builder()
                    .id(id).clinicId(clinicId).folderId(folderId)
                    .fileName("protocol.pdf").contentType("application/pdf")
                    .s3Key("clinics/x/kb/uuid-protocol.pdf")
                    .fileSize(204800L).description("Protocolo clínico")
                    .uploadedBy(userId).uploadedAt(now).indexedForIa(true)
                    .visibility(DocumentVisibility.PRIVATE)
                    .build();

            ClinicDocument domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(id);
            assertThat(domain.getFileName()).isEqualTo("protocol.pdf");
            assertThat(domain.getContentType()).isEqualTo("application/pdf");
            assertThat(domain.getFileSize()).isEqualTo(204800L);
            assertThat(domain.isIndexedForIa()).isTrue();
            assertThat(domain.getS3Key()).isEqualTo("clinics/x/kb/uuid-protocol.pdf");
            assertThat(domain.getVisibility()).isEqualTo(DocumentVisibility.PRIVATE);
        }

        @Test
        @DisplayName("domain → entity preserves all fields")
        void domainToEntityPreservesAllFields() {
            ClinicDocument domain = ClinicDocument.builder()
                    .id(id).clinicId(clinicId).folderId(folderId)
                    .fileName("factura.pdf").contentType("application/pdf")
                    .s3Key("clinics/x/docs/uuid-factura.pdf")
                    .fileSize(51200L).description(null)
                    .uploadedBy(userId).uploadedAt(now).indexedForIa(false)
                    .visibility(DocumentVisibility.PRIVATE)
                    .build();

            ClinicDocumentEntity entity = mapper.toEntity(domain);

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getFileName()).isEqualTo("factura.pdf");
            assertThat(entity.isIndexedForIa()).isFalse();
            assertThat(entity.getDescription()).isNull();
            assertThat(entity.getVisibility()).isEqualTo(DocumentVisibility.PRIVATE);
        }

        @Test
        @DisplayName("entity → domain → entity roundtrip is lossless")
        void roundtripIsLossless() {
            ClinicDocumentEntity original = ClinicDocumentEntity.builder()
                    .id(id).clinicId(clinicId).folderId(folderId)
                    .fileName("guia.pdf").contentType("application/pdf")
                    .s3Key("clinics/x/kb/uuid-guia.pdf")
                    .fileSize(1024L).description("Guía de tratamiento")
                    .uploadedBy(userId).uploadedAt(now).indexedForIa(true)
                    .visibility(DocumentVisibility.PRIVATE)
                    .build();

            ClinicDocumentEntity roundtripped = mapper.toEntity(mapper.toDomain(original));

            assertThat(roundtripped.getId()).isEqualTo(original.getId());
            assertThat(roundtripped.getFileName()).isEqualTo(original.getFileName());
            assertThat(roundtripped.getS3Key()).isEqualTo(original.getS3Key());
            assertThat(roundtripped.isIndexedForIa()).isEqualTo(original.isIndexedForIa());
            assertThat(roundtripped.getDescription()).isEqualTo(original.getDescription());
            assertThat(roundtripped.getVisibility()).isEqualTo(original.getVisibility());
        }
    }
}
