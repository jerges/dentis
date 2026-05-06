package com.adakadavra.dentis.billing.infrastructure.persistence.mapper;

import com.adakadavra.dentis.billing.domain.model.Tariff;
import com.adakadavra.dentis.billing.infrastructure.persistence.entity.TariffEntity;
import org.springframework.stereotype.Component;

@Component
public class TariffEntityMapper {

    public TariffEntity toEntity(Tariff tariff) {
        if (tariff == null) {
            return null;
        }
        return TariffEntity.builder()
                .id(tariff.getId())
                .code(tariff.getCode())
                .name(tariff.getName())
                .description(tariff.getDescription())
                .category(tariff.getCategory())
                .basePrice(tariff.getBasePrice())
                .discountAllowed(tariff.isDiscountAllowed())
                .active(tariff.isActive())
                .build();
    }

    public Tariff toDomain(TariffEntity entity) {
        if (entity == null) {
            return null;
        }
        return Tariff.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .basePrice(entity.getBasePrice())
                .discountAllowed(entity.isDiscountAllowed())
                .active(entity.isActive())
                .build();
    }
}

