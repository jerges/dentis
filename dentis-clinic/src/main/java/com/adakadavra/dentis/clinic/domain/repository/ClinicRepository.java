package com.adakadavra.dentis.clinic.domain.repository;

import com.adakadavra.dentis.clinic.domain.model.Clinic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository {

    Clinic save(Clinic clinic);

    Optional<Clinic> findById(UUID id);

    Page<Clinic> findAll(Pageable pageable);

    List<Clinic> findAllActive();

    boolean existsByNif(String nif);

    void deactivate(UUID id);
}

