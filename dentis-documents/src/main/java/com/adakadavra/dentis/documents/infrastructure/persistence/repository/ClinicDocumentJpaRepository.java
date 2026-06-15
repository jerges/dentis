package com.adakadavra.dentis.documents.infrastructure.persistence.repository;

import com.adakadavra.dentis.documents.infrastructure.persistence.entity.ClinicDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClinicDocumentJpaRepository extends JpaRepository<ClinicDocumentEntity, UUID> {

    List<ClinicDocumentEntity> findByFolderId(UUID folderId);

    @Query(value = """
            SELECT * FROM clinic_documents
            WHERE folder_id = :folderId
              AND (visibility = 'PUBLIC' OR uploaded_by = :userId)
            """, nativeQuery = true)
    List<ClinicDocumentEntity> findVisibleByFolderId(@Param("folderId") UUID folderId,
                                                      @Param("userId") UUID userId);

    List<ClinicDocumentEntity> findByClinicId(UUID clinicId);

    @Query(value = """
            SELECT * FROM clinic_documents
            WHERE clinic_id = :clinicId
              AND search_vector @@ plainto_tsquery('spanish', :query)
            ORDER BY ts_rank(search_vector, plainto_tsquery('spanish', :query)) DESC
            LIMIT 50
            """, nativeQuery = true)
    List<ClinicDocumentEntity> fullTextSearch(@Param("clinicId") UUID clinicId,
                                               @Param("query") String query);

    @Query(value = """
            SELECT * FROM clinic_documents
            WHERE clinic_id = :clinicId
              AND search_vector @@ plainto_tsquery('spanish', :query)
              AND (visibility = 'PUBLIC' OR uploaded_by = :userId)
            ORDER BY ts_rank(search_vector, plainto_tsquery('spanish', :query)) DESC
            LIMIT 50
            """, nativeQuery = true)
    List<ClinicDocumentEntity> fullTextSearchVisible(@Param("clinicId") UUID clinicId,
                                                      @Param("query") String query,
                                                      @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE ClinicDocumentEntity d SET d.indexedForIa = true WHERE d.id = :id")
    void markIndexed(@Param("id") UUID id);
}
