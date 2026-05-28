package com.adakadavra.dentis.billing.infrastructure.persistence.repository;

import com.adakadavra.dentis.billing.domain.model.Tariff;
import com.adakadavra.dentis.billing.domain.model.TariffCategory;
import com.adakadavra.dentis.billing.domain.repository.TariffRepository;
import com.adakadavra.dentis.billing.infrastructure.persistence.mapper.TariffEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TariffRepositoryAdapter implements TariffRepository {

    private final TariffJpaRepository jpaRepository;
    private final TariffEntityMapper entityMapper;

    @Override
    public Tariff save(Tariff tariff) {
        return entityMapper.toDomain(jpaRepository.save(entityMapper.toEntity(tariff)));
    }

    @Override
    public Optional<Tariff> findById(UUID id) {
        return jpaRepository.findById(id).map(entityMapper::toDomain);
    }

    @Override
    public Optional<Tariff> findByCode(String code) {
        return jpaRepository.findByCode(code).map(entityMapper::toDomain);
    }

    @Override
    public Page<Tariff> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(entityMapper::toDomain);
    }

    @Override
    public List<Tariff> findByCategory(TariffCategory category) {
        return jpaRepository.findByCategory(category)
                .stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        jpaRepository.deactivateById(id);
    }
}

