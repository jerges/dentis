package com.adakadavra.dentis.clinic.infrastructure.persistence.repository;

import com.adakadavra.dentis.clinic.domain.model.Clinic;
import com.adakadavra.dentis.clinic.domain.repository.ClinicRepository;
import com.adakadavra.dentis.clinic.infrastructure.persistence.mapper.ClinicEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ClinicRepositoryAdapter implements ClinicRepository {

    private final ClinicJpaRepository jpaRepository;
    private final ClinicEntityMapper mapper;

    @Override
    public Clinic save(Clinic clinic) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(clinic)));
    }

    @Override
    public Optional<Clinic> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Clinic> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public List<Clinic> findAllActive() {
        return jpaRepository.findAllByActiveTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByNif(String nif) {
        return jpaRepository.existsByNif(nif);
    }

    @Override
    public void deactivate(UUID id) {
        jpaRepository.deactivateById(id);
    }
}

