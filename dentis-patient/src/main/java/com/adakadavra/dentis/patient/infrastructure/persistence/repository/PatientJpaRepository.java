package com.adakadavra.dentis.patient.infrastructure.persistence.repository;

import com.adakadavra.dentis.patient.infrastructure.persistence.entity.PatientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PatientJpaRepository extends JpaRepository<PatientEntity, UUID> {

    Optional<PatientEntity> findByIdDocument(String idDocument);

    Optional<PatientEntity> findByIdAndClinicId(UUID id, UUID clinicId);

    boolean existsByIdDocument(String idDocument);

    boolean existsByIdDocumentAndClinicId(String idDocument, UUID clinicId);

    Page<PatientEntity> findAllByClinicId(UUID clinicId, Pageable pageable);

    @Query("""
            SELECT p FROM PatientEntity p
            WHERE LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))
            AND p.active = true
            AND p.clinicId = :clinicId
            """)
    Page<PatientEntity> searchByNameAndClinicId(@Param("name") String name, @Param("clinicId") UUID clinicId, Pageable pageable);

    @Query("""
            SELECT p FROM PatientEntity p
            WHERE LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))
            AND p.active = true
            """)
    Page<PatientEntity> searchByName(@Param("name") String name, Pageable pageable);

    @Modifying
    @Query("UPDATE PatientEntity p SET p.active = false WHERE p.id = :id")
    void deactivateById(@Param("id") UUID id);
}
