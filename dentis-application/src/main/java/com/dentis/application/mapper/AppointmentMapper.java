package com.dentis.application.mapper;

import com.dentis.application.dto.response.AppointmentResponse;
import com.dentis.domain.entity.Appointment;
import org.mapstruct.*;

import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", expression = "java(appointment.getPatient().getFullName())")
    @Mapping(target = "dentistId", source = "dentist.id")
    @Mapping(target = "dentistName", expression = "java(appointment.getDentist().getFullName())")
    @Mapping(target = "dentistColorCode", source = "dentist.colorCode")
    @Mapping(target = "treatmentPlanId", source = "treatmentPlan.id")
    @Mapping(target = "durationMinutes", expression = "java(calculateDuration(appointment))")
    AppointmentResponse toResponse(Appointment appointment);

    default int calculateDuration(Appointment appointment) {
        if (appointment.getStartDateTime() == null || appointment.getEndDateTime() == null) return 0;
        return (int) ChronoUnit.MINUTES.between(appointment.getStartDateTime(), appointment.getEndDateTime());
    }
}
