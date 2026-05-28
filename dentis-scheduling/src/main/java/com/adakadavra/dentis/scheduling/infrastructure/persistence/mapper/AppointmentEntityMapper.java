package com.adakadavra.dentis.scheduling.infrastructure.persistence.mapper;

import com.adakadavra.dentis.scheduling.domain.model.Appointment;
import com.adakadavra.dentis.scheduling.infrastructure.persistence.entity.AppointmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentEntityMapper {

    AppointmentEntity toEntity(Appointment appointment);

    Appointment toDomain(AppointmentEntity entity);
}
