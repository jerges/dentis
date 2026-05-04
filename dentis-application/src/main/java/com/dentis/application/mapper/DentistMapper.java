package com.dentis.application.mapper;

import com.dentis.application.dto.request.CreateDentistRequest;
import com.dentis.application.dto.response.DentistResponse;
import com.dentis.domain.entity.Dentist;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DentistMapper {

    @Mapping(target = "fullName", expression = "java(dentist.getFullName())")
    DentistResponse toResponse(Dentist dentist);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "appointments", ignore = true)
    Dentist toEntity(CreateDentistRequest request);
}
