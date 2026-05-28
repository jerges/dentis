package com.adakadavra.dentis.billing.infrastructure.persistence.repository;

import com.adakadavra.dentis.billing.domain.model.TariffCategory;
import com.adakadavra.dentis.billing.infrastructure.persistence.entity.TariffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TariffJpaRepository extends JpaRepository<TariffEntity, UUID> {

    Optional<TariffEntity> findByCode(String code);

    List<TariffEntity> findByCategory(TariffCategory category);

    boolean existsByCode(String code);

    @Modifying
    @Query("update TariffEntity t set t.active = false where t.id = :id")
    void deactivateById(@Param("id") UUID id);
}

