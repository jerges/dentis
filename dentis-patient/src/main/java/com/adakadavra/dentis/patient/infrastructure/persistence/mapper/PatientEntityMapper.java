package com.adakadavra.dentis.patient.infrastructure.persistence.mapper;

import com.adakadavra.dentis.patient.domain.model.*;
import com.adakadavra.dentis.patient.infrastructure.persistence.entity.PatientEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PatientEntityMapper {

    @Mapping(target = "email", source = "contactInfo.email")
    @Mapping(target = "phoneNumber", source = "contactInfo.phoneNumber")
    @Mapping(target = "alternativePhone", source = "contactInfo.alternativePhone")
    @Mapping(target = "street", source = "address.street")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "state", source = "address.state")
    @Mapping(target = "zipCode", source = "address.zipCode")
    @Mapping(target = "representativeNotApplicable", source = "representative.notApplicable")
    @Mapping(target = "representativeFullName", source = "representative.fullName")
    @Mapping(target = "representativeIdDocument", source = "representative.idDocument")
    @Mapping(target = "representativeRelationship", source = "representative.relationship")
    @Mapping(target = "representativePhone", source = "representative.phoneNumber")
    PatientEntity toEntity(Patient patient);

    @Mapping(target = "contactInfo", expression = "java(buildContactInfo(entity))")
    @Mapping(target = "address", expression = "java(buildAddress(entity))")
    @Mapping(target = "representative", expression = "java(buildRepresentative(entity))")
    Patient toDomain(PatientEntity entity);

    default ContactInfo buildContactInfo(PatientEntity entity) {
        return ContactInfo.builder()
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .alternativePhone(entity.getAlternativePhone())
                .build();
    }

    default Address buildAddress(PatientEntity entity) {
        return Address.builder()
                .street(entity.getStreet())
                .city(entity.getCity())
                .state(entity.getState())
                .zipCode(entity.getZipCode())
                .build();
    }

    default Representative buildRepresentative(PatientEntity entity) {
        if (entity.isRepresentativeNotApplicable()) {
            return Representative.builder().notApplicable(true).build();
        }
        if (entity.getRepresentativeFullName() == null) return null;
        return Representative.builder()
                .notApplicable(false)
                .fullName(entity.getRepresentativeFullName())
                .idDocument(entity.getRepresentativeIdDocument())
                .relationship(entity.getRepresentativeRelationship())
                .phoneNumber(entity.getRepresentativePhone())
                .build();
    }
}
