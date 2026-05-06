package com.adakadavra.dentis.patient.application.mapper;

import com.adakadavra.dentis.patient.application.dto.*;
import com.adakadavra.dentis.patient.domain.model.*;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PatientMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "active", constant = "true")
    Patient toDomain(CreatePatientRequest request);

    @Mapping(target = "fullName", expression = "java(patient.fullName())")
    @Mapping(target = "age", expression = "java(calculateAge(patient.getBirthDate()))")
    PatientResponse toResponse(Patient patient);

    ContactInfo toDomain(ContactInfoDto dto);
    ContactInfoDto toDto(ContactInfo contactInfo);

    Address toDomain(AddressDto dto);
    AddressDto toDto(Address address);

    Representative toDomain(RepresentativeDto dto);
    RepresentativeDto toDto(Representative representative);

    default int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
