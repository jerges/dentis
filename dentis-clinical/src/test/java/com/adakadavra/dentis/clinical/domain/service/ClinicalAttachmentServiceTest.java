package com.adakadavra.dentis.clinical.domain.service;

import com.adakadavra.dentis.clinical.domain.model.ClinicalAttachment;
import com.adakadavra.dentis.clinical.infrastructure.persistence.entity.ClinicalAttachmentEntity;
import com.adakadavra.dentis.clinical.infrastructure.persistence.entity.ClinicalRecordEntity;
import com.adakadavra.dentis.clinical.infrastructure.persistence.repository.ClinicalAttachmentJpaRepository;
import com.adakadavra.dentis.clinical.infrastructure.persistence.repository.ClinicalRecordJpaRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClinicalAttachmentService")
class ClinicalAttachmentServiceTest {

    @Mock private ClinicalAttachmentJpaRepository attachmentRepo;
    @Mock private ClinicalRecordJpaRepository recordRepo;

    @InjectMocks
    private ClinicalAttachmentService clinicalAttachmentService;

    private UUID patientId;
    private UUID clinicId;
    private UUID recordId;
    private UUID attachmentId;
    private ClinicalRecordEntity record;
    private ClinicalAttachmentEntity attachmentEntity;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        clinicId = UUID.randomUUID();
        recordId = UUID.randomUUID();
        attachmentId = UUID.randomUUID();

        record = ClinicalRecordEntity.builder()
                .id(recordId)
                .patientId(patientId)
                .createdAt(LocalDateTime.now())
                .build();

        attachmentEntity = ClinicalAttachmentEntity.builder()
                .id(attachmentId)
                .clinicalRecord(record)
                .clinicId(clinicId)
                .fileName("rx_panoramica.jpg")
                .contentType("image/jpeg")
                .s3Key("clinics/" + clinicId + "/rx_panoramica.jpg")
                .fileSize(204800L)
                .description("Radiografía panorámica")
                .uploadedByDentistId(UUID.randomUUID())
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("listByRecord")
    class ListByRecord {

        @Test
        @DisplayName("should return all attachments for the patient's record")
        void shouldReturnAllAttachmentsForRecord() {
            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.of(record));
            when(attachmentRepo.findByClinicalRecordIdOrderByUploadedAtDesc(recordId))
                    .thenReturn(List.of(attachmentEntity));

            List<ClinicalAttachment> result = clinicalAttachmentService.listByRecord(patientId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFileName()).isEqualTo("rx_panoramica.jpg");
        }

        @Test
        @DisplayName("should return empty list when record has no attachments")
        void shouldReturnEmptyListWhenNoAttachments() {
            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.of(record));
            when(attachmentRepo.findByClinicalRecordIdOrderByUploadedAtDesc(recordId))
                    .thenReturn(List.of());

            List<ClinicalAttachment> result = clinicalAttachmentService.listByRecord(patientId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw when clinical record does not exist for patient")
        void shouldThrowWhenRecordDoesNotExist() {
            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicalAttachmentService.listByRecord(patientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Clinical record not found for patient");
        }
    }

    @Nested
    @DisplayName("listByTooth")
    class ListByTooth {

        @Test
        @DisplayName("should return attachments for the specific tooth")
        void shouldReturnAttachmentsForTooth() {
            ClinicalAttachmentEntity toothAttachment = ClinicalAttachmentEntity.builder()
                    .id(UUID.randomUUID())
                    .clinicalRecord(record)
                    .clinicId(clinicId)
                    .toothNumber(16)
                    .fileName("rx_16.jpg")
                    .contentType("image/jpeg")
                    .s3Key("clinics/" + clinicId + "/rx_16.jpg")
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.of(record));
            when(attachmentRepo.findByClinicalRecordIdAndToothNumberOrderByUploadedAtDesc(recordId, 16))
                    .thenReturn(List.of(toothAttachment));

            List<ClinicalAttachment> result = clinicalAttachmentService.listByTooth(patientId, 16);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getToothNumber()).isEqualTo(16);
        }

        @Test
        @DisplayName("should throw when clinical record does not exist for patient")
        void shouldThrowWhenRecordDoesNotExist() {
            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicalAttachmentService.listByTooth(patientId, 36))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("listByClinic")
    class ListByClinic {

        @Test
        @DisplayName("should return all attachments for the clinic")
        void shouldReturnAllAttachmentsForClinic() {
            when(attachmentRepo.findByClinicIdOrderByUploadedAtDesc(clinicId))
                    .thenReturn(List.of(attachmentEntity));

            List<ClinicalAttachment> result = clinicalAttachmentService.listByClinic(clinicId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getClinicId()).isEqualTo(clinicId);
        }

        @Test
        @DisplayName("should return empty list when clinic has no attachments")
        void shouldReturnEmptyListWhenNone() {
            when(attachmentRepo.findByClinicIdOrderByUploadedAtDesc(clinicId))
                    .thenReturn(List.of());

            assertThat(clinicalAttachmentService.listByClinic(clinicId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should be saved with uploadedAt set automatically")
        void shouldBeSavedWithUploadedAt() {
            UUID dentistId = UUID.randomUUID();
            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.of(record));
            when(attachmentRepo.save(any())).thenReturn(attachmentEntity);

            ClinicalAttachment result = clinicalAttachmentService.register(
                    patientId, clinicId, "clinics/" + clinicId + "/rx.jpg",
                    "rx.jpg", "image/jpeg", 204800L, "Radiografía",
                    null, dentistId
            );

            assertThat(result.getFileName()).isEqualTo("rx_panoramica.jpg");

            ArgumentCaptor<ClinicalAttachmentEntity> captor =
                    ArgumentCaptor.forClass(ClinicalAttachmentEntity.class);
            verify(attachmentRepo).save(captor.capture());
            assertThat(captor.getValue().getUploadedAt()).isNotNull();
        }

        @Test
        @DisplayName("should be registered with tooth number when provided")
        void shouldBeRegisteredWithToothNumber() {
            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.of(record));
            when(attachmentRepo.save(any())).thenReturn(attachmentEntity);

            clinicalAttachmentService.register(
                    patientId, clinicId, "s3key", "file.jpg", "image/jpeg",
                    1024L, null, 16, UUID.randomUUID()
            );

            ArgumentCaptor<ClinicalAttachmentEntity> captor =
                    ArgumentCaptor.forClass(ClinicalAttachmentEntity.class);
            verify(attachmentRepo).save(captor.capture());
            assertThat(captor.getValue().getToothNumber()).isEqualTo(16);
        }

        @Test
        @DisplayName("should throw when clinical record does not exist for patient")
        void shouldThrowWhenRecordNotFound() {
            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicalAttachmentService.register(
                    patientId, clinicId, "key", "file.jpg", "image/jpeg",
                    1024L, null, null, UUID.randomUUID()
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Clinical record not found");
        }
    }

    @Nested
    @DisplayName("deleteAndGetKey")
    class DeleteAndGetKey {

        @Test
        @DisplayName("should delete and return s3 key when attachment belongs to patient")
        void shouldDeleteAndReturnS3Key() {
            when(attachmentRepo.findById(attachmentId)).thenReturn(Optional.of(attachmentEntity));
            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.of(record));

            String key = clinicalAttachmentService.deleteAndGetKey(attachmentId, patientId);

            assertThat(key).isEqualTo("clinics/" + clinicId + "/rx_panoramica.jpg");
            verify(attachmentRepo).delete(attachmentEntity);
        }

        @Test
        @DisplayName("should throw when attachment does not exist")
        void shouldThrowWhenAttachmentNotFound() {
            when(attachmentRepo.findById(attachmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicalAttachmentService.deleteAndGetKey(attachmentId, patientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Attachment not found");
        }

        @Test
        @DisplayName("should throw when attachment belongs to a different patient")
        void shouldThrowWhenAttachmentBelongsToDifferentPatient() {
            UUID anotherRecordId = UUID.randomUUID();
            ClinicalRecordEntity anotherRecord = ClinicalRecordEntity.builder()
                    .id(anotherRecordId)
                    .patientId(UUID.randomUUID())
                    .createdAt(LocalDateTime.now())
                    .build();

            when(attachmentRepo.findById(attachmentId)).thenReturn(Optional.of(attachmentEntity));
            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.of(anotherRecord));

            assertThatThrownBy(() -> clinicalAttachmentService.deleteAndGetKey(attachmentId, patientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to this patient");

            verify(attachmentRepo, never()).delete(any());
        }

        @Test
        @DisplayName("should throw when clinical record does not exist for patient")
        void shouldThrowWhenRecordNotFound() {
            when(attachmentRepo.findById(attachmentId)).thenReturn(Optional.of(attachmentEntity));
            when(recordRepo.findByPatientId(patientId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicalAttachmentService.deleteAndGetKey(attachmentId, patientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Clinical record not found");
        }
    }
}
