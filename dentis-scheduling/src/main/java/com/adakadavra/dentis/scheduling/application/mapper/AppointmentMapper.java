package com.adakadavra.dentis.scheduling.application.mapper;

import com.adakadavra.dentis.scheduling.application.dto.AppointmentResponse;
import com.adakadavra.dentis.scheduling.application.dto.CreateAppointmentRequest;
import com.adakadavra.dentis.scheduling.domain.model.Appointment;
import com.adakadavra.dentis.scheduling.domain.model.AppointmentStatus;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "status", constant = "SCHEDULED")
    Appointment toDomain(CreateAppointmentRequest request);

    AppointmentResponse toResponse(Appointment appointment);
}
