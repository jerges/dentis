package com.adakadavra.dentis.scheduling.infrastructure.persistence.mapper;

import com.adakadavra.dentis.scheduling.domain.model.Appointment;
import com.adakadavra.dentis.scheduling.infrastructure.persistence.entity.AppointmentEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-06T12:05:10+0200",
    comments = "version: 1.6.2, compiler: javac, environment: Java 25.0.2 (JetBrains s.r.o.)"
)
@Component
public class AppointmentEntityMapperImpl implements AppointmentEntityMapper {

    @Override
    public AppointmentEntity toEntity(Appointment appointment) {
        if ( appointment == null ) {
            return null;
        }

        AppointmentEntity.AppointmentEntityBuilder appointmentEntity = AppointmentEntity.builder();

        appointmentEntity.id( appointment.getId() );
        appointmentEntity.patientId( appointment.getPatientId() );
        appointmentEntity.dentistId( appointment.getDentistId() );
        appointmentEntity.startDateTime( appointment.getStartDateTime() );
        appointmentEntity.endDateTime( appointment.getEndDateTime() );
        appointmentEntity.status( appointment.getStatus() );
        appointmentEntity.consultationReason( appointment.getConsultationReason() );
        appointmentEntity.notes( appointment.getNotes() );

        return appointmentEntity.build();
    }

    @Override
    public Appointment toDomain(AppointmentEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Appointment.AppointmentBuilder appointment = Appointment.builder();

        appointment.id( entity.getId() );
        appointment.patientId( entity.getPatientId() );
        appointment.dentistId( entity.getDentistId() );
        appointment.startDateTime( entity.getStartDateTime() );
        appointment.endDateTime( entity.getEndDateTime() );
        appointment.status( entity.getStatus() );
        appointment.consultationReason( entity.getConsultationReason() );
        appointment.notes( entity.getNotes() );

        return appointment.build();
    }
}
