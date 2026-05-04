package com.dentis.application.mapper;

import com.dentis.application.dto.request.CreatePatientRequest;
import com.dentis.application.dto.response.PatientResponse;
import com.dentis.application.dto.response.PatientSummaryResponse;
import com.dentis.domain.entity.Patient;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.Period;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PatientMapper {

    @Mapping(target = "fullName", expression = "java(patient.getFullName())")
    @Mapping(target = "age", expression = "java(calculateAge(patient.getBirthDate()))")
    PatientResponse toResponse(Patient patient);

    @Mapping(target = "fullName", expression = "java(patient.getFullName())")
    @Mapping(target = "age", expression = "java(calculateAge(patient.getBirthDate()))")
    PatientSummaryResponse toSummaryResponse(Patient patient);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "clinicalRecord", ignore = true)
    @Mapping(target = "appointments", ignore = true)
    @Mapping(target = "budgets", ignore = true)
    Patient toEntity(CreatePatientRequest request);

    default int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
