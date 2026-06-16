package com.adakadavra.dentis.billing.infrastructure.persistence.mapper;

import com.adakadavra.dentis.billing.domain.model.Tariff;
import com.adakadavra.dentis.billing.domain.model.TariffCategory;
import com.adakadavra.dentis.billing.infrastructure.persistence.entity.TariffEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TariffEntityMapper")
class TariffEntityMapperTest {

    private TariffEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TariffEntityMapper();
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should return null when tariff is null")
        void shouldReturnNullWhenTariffIsNull() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from domain to entity")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();

            Tariff tariff = Tariff.builder()
                    .id(id)
                    .code("T001")
                    .name("Limpieza dental")
                    .description("Limpieza completa")
                    .category(TariffCategory.GENERAL_DENTISTRY)
                    .basePrice(new BigDecimal("60.00"))
                    .discountAllowed(true)
                    .active(true)
                    .build();

            TariffEntity entity = mapper.toEntity(tariff);

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getCode()).isEqualTo("T001");
            assertThat(entity.getName()).isEqualTo("Limpieza dental");
            assertThat(entity.getDescription()).isEqualTo("Limpieza completa");
            assertThat(entity.getCategory()).isEqualTo(TariffCategory.GENERAL_DENTISTRY);
            assertThat(entity.getBasePrice()).isEqualByComparingTo("60.00");
            assertThat(entity.isDiscountAllowed()).isTrue();
            assertThat(entity.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("should return null when entity is null")
        void shouldReturnNullWhenEntityIsNull() {
            assertThat(mapper.toDomain(null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from entity to domain")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();

            TariffEntity entity = TariffEntity.builder()
                    .id(id)
                    .code("T002")
                    .name("Extracción")
                    .description("Extracción simple")
                    .category(TariffCategory.SURGERY)
                    .basePrice(new BigDecimal("80.00"))
                    .discountAllowed(false)
                    .active(false)
                    .build();

            Tariff domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(id);
            assertThat(domain.getCode()).isEqualTo("T002");
            assertThat(domain.getName()).isEqualTo("Extracción");
            assertThat(domain.getDescription()).isEqualTo("Extracción simple");
            assertThat(domain.getCategory()).isEqualTo(TariffCategory.SURGERY);
            assertThat(domain.getBasePrice()).isEqualByComparingTo("80.00");
            assertThat(domain.isDiscountAllowed()).isFalse();
            assertThat(domain.isActive()).isFalse();
        }
    }
}
