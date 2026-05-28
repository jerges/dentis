package com.adakadavra.dentis.clinic.infrastructure.persistence.mapper;

import com.adakadavra.dentis.clinic.domain.model.Clinic;
import com.adakadavra.dentis.clinic.infrastructure.persistence.entity.ClinicEntity;
import org.springframework.stereotype.Component;

@Component
public class ClinicEntityMapper {

    public Clinic toDomain(ClinicEntity entity) {
        if (entity == null) return null;
        return Clinic.builder()
                .id(entity.getId())
                .name(entity.getName())
                .nif(entity.getNif())
                .address(entity.getAddress())
                .city(entity.getCity())
                .province(entity.getProvince())
                .zipCode(entity.getZipCode())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ClinicEntity toEntity(Clinic domain) {
        if (domain == null) return null;
        return ClinicEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .nif(domain.getNif())
                .address(domain.getAddress())
                .city(domain.getCity())
                .province(domain.getProvince())
                .zipCode(domain.getZipCode())
                .phone(domain.getPhone())
                .email(domain.getEmail())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

