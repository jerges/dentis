package com.adakadavra.dentis.clinic.infrastructure.persistence.repository;

import com.adakadavra.dentis.clinic.infrastructure.persistence.entity.ClinicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ClinicJpaRepository extends JpaRepository<ClinicEntity, UUID> {

    boolean existsByNif(String nif);

    List<ClinicEntity> findAllByActiveTrue();

    @Modifying
    @Query("UPDATE ClinicEntity c SET c.active = false WHERE c.id = :id")
    void deactivateById(UUID id);
}

