package com.adakadavra.dentis.clinical.domain.service;

import com.adakadavra.dentis.clinical.domain.model.ClinicalAttachment;
import com.adakadavra.dentis.clinical.infrastructure.persistence.entity.ClinicalAttachmentEntity;
import com.adakadavra.dentis.clinical.infrastructure.persistence.entity.ClinicalRecordEntity;
import com.adakadavra.dentis.clinical.infrastructure.persistence.repository.ClinicalAttachmentJpaRepository;
import com.adakadavra.dentis.clinical.infrastructure.persistence.repository.ClinicalRecordJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalAttachmentService {

    private final ClinicalAttachmentJpaRepository attachmentRepo;
    private final ClinicalRecordJpaRepository recordRepo;

    @Transactional(readOnly = true)
    public List<ClinicalAttachment> listByRecord(UUID patientId) {
        ClinicalRecordEntity record = recordRepo.findByPatientId(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Clinical record not found for patient: " + patientId));
        return attachmentRepo.findByClinicalRecordIdOrderByUploadedAtDesc(record.getId())
                .stream().map(this::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalAttachment> listByTooth(UUID patientId, int toothNumber) {
        ClinicalRecordEntity record = recordRepo.findByPatientId(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Clinical record not found for patient: " + patientId));
        return attachmentRepo.findByClinicalRecordIdAndToothNumberOrderByUploadedAtDesc(record.getId(), toothNumber)
                .stream().map(this::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalAttachment> listByClinic(UUID clinicId) {
        return attachmentRepo.findByClinicIdOrderByUploadedAtDesc(clinicId)
                .stream().map(this::toDomain).toList();
    }

    @Transactional
    public ClinicalAttachment register(UUID patientId, UUID clinicId, String s3Key, String fileName,
                                       String contentType, Long fileSize, String description,
                                       Integer toothNumber, UUID uploadedByDentistId) {
        ClinicalRecordEntity record = recordRepo.findByPatientId(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Clinical record not found for patient: " + patientId));

        ClinicalAttachmentEntity entity = ClinicalAttachmentEntity.builder()
                .clinicalRecord(record)
                .clinicId(clinicId)
                .toothNumber(toothNumber)
                .fileName(fileName)
                .contentType(contentType)
                .s3Key(s3Key)
                .fileSize(fileSize)
                .description(description)
                .uploadedByDentistId(uploadedByDentistId)
                .uploadedAt(LocalDateTime.now())
                .build();

        return toDomain(attachmentRepo.save(entity));
    }

    @Transactional
    public String deleteAndGetKey(UUID attachmentId, UUID patientId) {
        ClinicalAttachmentEntity entity = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
        ClinicalRecordEntity record = recordRepo.findByPatientId(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Clinical record not found for patient: " + patientId));
        if (!entity.getClinicalRecord().getId().equals(record.getId())) {
            throw new IllegalArgumentException("Attachment does not belong to this patient");
        }
        String key = entity.getS3Key();
        attachmentRepo.delete(entity);
        return key;
    }

    private ClinicalAttachment toDomain(ClinicalAttachmentEntity e) {
        return ClinicalAttachment.builder()
                .id(e.getId())
                .clinicalRecordId(e.getClinicalRecord().getId())
                .clinicId(e.getClinicId())
                .toothNumber(e.getToothNumber())
                .fileName(e.getFileName())
                .contentType(e.getContentType())
                .s3Key(e.getS3Key())
                .fileSize(e.getFileSize())
                .description(e.getDescription())
                .uploadedByDentistId(e.getUploadedByDentistId())
                .uploadedAt(e.getUploadedAt())
                .build();
    }
}
