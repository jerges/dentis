package com.adakadavra.dentis.documents.infrastructure.persistence.repository;

import com.adakadavra.dentis.documents.domain.model.FolderType;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.DocumentFolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentFolderJpaRepository extends JpaRepository<DocumentFolderEntity, UUID> {

    List<DocumentFolderEntity> findByClinicIdAndParentId(UUID clinicId, UUID parentId);

    Optional<DocumentFolderEntity> findByClinicIdAndType(UUID clinicId, FolderType type);
}
