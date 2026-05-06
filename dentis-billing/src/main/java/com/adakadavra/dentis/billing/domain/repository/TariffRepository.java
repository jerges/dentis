package com.adakadavra.dentis.billing.domain.repository;

import com.adakadavra.dentis.billing.domain.model.Tariff;
import com.adakadavra.dentis.billing.domain.model.TariffCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TariffRepository {

    Tariff save(Tariff tariff);

    Optional<Tariff> findById(UUID id);

    Optional<Tariff> findByCode(String code);

    Page<Tariff> findAll(Pageable pageable);

    List<Tariff> findByCategory(TariffCategory category);

    boolean existsByCode(String code);

    void deactivate(UUID id);

}
