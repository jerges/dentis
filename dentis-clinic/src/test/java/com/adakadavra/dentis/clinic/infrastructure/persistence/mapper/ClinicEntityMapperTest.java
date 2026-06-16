package com.adakadavra.dentis.clinic.infrastructure.persistence.mapper;

import com.adakadavra.dentis.clinic.domain.model.Clinic;
import com.adakadavra.dentis.clinic.infrastructure.persistence.entity.ClinicEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClinicEntityMapper")
class ClinicEntityMapperTest {

    private ClinicEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ClinicEntityMapper();
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
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now();

            ClinicEntity entity = ClinicEntity.builder()
                    .id(id)
                    .name("Clínica Dental García")
                    .nif("B12345678")
                    .address("Calle Mayor 1")
                    .city("Madrid")
                    .province("Madrid")
                    .zipCode("28001")
                    .phone("910000001")
                    .email("clinica@garcia.com")
                    .active(true)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();

            Clinic domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(id);
            assertThat(domain.getName()).isEqualTo("Clínica Dental García");
            assertThat(domain.getNif()).isEqualTo("B12345678");
            assertThat(domain.getAddress()).isEqualTo("Calle Mayor 1");
            assertThat(domain.getCity()).isEqualTo("Madrid");
            assertThat(domain.getProvince()).isEqualTo("Madrid");
            assertThat(domain.getZipCode()).isEqualTo("28001");
            assertThat(domain.getPhone()).isEqualTo("910000001");
            assertThat(domain.getEmail()).isEqualTo("clinica@garcia.com");
            assertThat(domain.isActive()).isTrue();
            assertThat(domain.getCreatedAt()).isEqualTo(createdAt);
            assertThat(domain.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should return null when domain is null")
        void shouldReturnNullWhenDomainIsNull() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from domain to entity")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();
            LocalDateTime createdAt = LocalDateTime.now().minusDays(2);
            LocalDateTime updatedAt = LocalDateTime.now();

            Clinic domain = Clinic.builder()
                    .id(id)
                    .name("Clínica Norte")
                    .nif("A87654321")
                    .address("Av. de la Paz 10")
                    .city("Barcelona")
                    .province("Cataluña")
                    .zipCode("08001")
                    .phone("930000002")
                    .email("norte@clinica.es")
                    .active(false)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();

            ClinicEntity entity = mapper.toEntity(domain);

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getName()).isEqualTo("Clínica Norte");
            assertThat(entity.getNif()).isEqualTo("A87654321");
            assertThat(entity.getAddress()).isEqualTo("Av. de la Paz 10");
            assertThat(entity.getCity()).isEqualTo("Barcelona");
            assertThat(entity.getProvince()).isEqualTo("Cataluña");
            assertThat(entity.getZipCode()).isEqualTo("08001");
            assertThat(entity.getPhone()).isEqualTo("930000002");
            assertThat(entity.getEmail()).isEqualTo("norte@clinica.es");
            assertThat(entity.isActive()).isFalse();
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }
}
