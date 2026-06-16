package com.adakadavra.dentis.documents.infrastructure.persistence.repository;

import com.adakadavra.dentis.documents.infrastructure.persistence.entity.DocumentFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentFileJpaRepository extends JpaRepository<DocumentFileEntity, UUID> {

    List<DocumentFileEntity> findByFolderId(UUID folderId);

    List<DocumentFileEntity> findByClinicId(UUID clinicId);

    List<DocumentFileEntity> findByFolderIdAndIndexedForIaTrue(UUID folderId);
}