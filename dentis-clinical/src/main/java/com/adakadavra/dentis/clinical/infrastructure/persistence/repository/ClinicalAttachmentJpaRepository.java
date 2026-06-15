package com.adakadavra.dentis.clinical.infrastructure.persistence.repository;

import com.adakadavra.dentis.clinical.infrastructure.persistence.entity.ClinicalAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClinicalAttachmentJpaRepository extends JpaRepository<ClinicalAttachmentEntity, UUID> {

    List<ClinicalAttachmentEntity> findByClinicalRecordIdOrderByUploadedAtDesc(UUID clinicalRecordId);

    List<ClinicalAttachmentEntity> findByClinicalRecordIdAndToothNumberOrderByUploadedAtDesc(
            UUID clinicalRecordId, Integer toothNumber);

    List<ClinicalAttachmentEntity> findByClinicIdOrderByUploadedAtDesc(UUID clinicId);
}
