package com.adakadavra.dentis.documents.infrastructure.persistence.repository;

import com.adakadavra.dentis.documents.domain.model.DocumentZone;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.DocumentFolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentFolderJpaRepository extends JpaRepository<DocumentFolderEntity, UUID> {

    List<DocumentFolderEntity> findByClinicIdAndParentId(UUID clinicId, UUID parentId);

    @Query(value = """
            SELECT * FROM document_folders
            WHERE clinic_id = :clinicId
              AND (parent_id = :parentId OR (:parentId IS NULL AND parent_id IS NULL))
              AND (visibility = 'PUBLIC' OR created_by = :userId)
            """, nativeQuery = true)
    List<DocumentFolderEntity> findVisibleByClinicIdAndParentId(@Param("clinicId") UUID clinicId,
                                                                 @Param("parentId") UUID parentId,
                                                                 @Param("userId") UUID userId);

    List<DocumentFolderEntity> findByClinicIdAndZone(UUID clinicId, DocumentZone zone);

    boolean existsByClinicIdAndParentIdAndName(UUID clinicId, UUID parentId, String name);
}
