package com.adakadavra.dentis.scheduling.application.mapper;

import com.adakadavra.dentis.scheduling.application.dto.AppointmentResponse;
import com.adakadavra.dentis.scheduling.application.dto.CreateAppointmentRequest;
import com.adakadavra.dentis.scheduling.domain.model.Appointment;
import com.adakadavra.dentis.scheduling.domain.model.AppointmentStatus;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-06T12:05:10+0200",
    comments = "version: 1.6.2, compiler: javac, environment: Java 25.0.2 (JetBrains s.r.o.)"
)
@Component
public class AppointmentMapperImpl implements AppointmentMapper {

    @Override
    public Appointment toDomain(CreateAppointmentRequest request) {
        if ( request == null ) {
            return null;
        }

        Appointment.AppointmentBuilder appointment = Appointment.builder();

        appointment.patientId( request.getPatientId() );
        appointment.dentistId( request.getDentistId() );
        appointment.startDateTime( request.getStartDateTime() );
        appointment.endDateTime( request.getEndDateTime() );
        appointment.consultationReason( request.getConsultationReason() );
        appointment.notes( request.getNotes() );

        appointment.id( java.util.UUID.randomUUID() );
        appointment.status( AppointmentStatus.SCHEDULED );

        return appointment.build();
    }

    @Override
    public AppointmentResponse toResponse(Appointment appointment) {
        if ( appointment == null ) {
            return null;
        }

        AppointmentResponse.AppointmentResponseBuilder appointmentResponse = AppointmentResponse.builder();

        appointmentResponse.id( appointment.getId() );
        appointmentResponse.patientId( appointment.getPatientId() );
        appointmentResponse.dentistId( appointment.getDentistId() );
        appointmentResponse.startDateTime( appointment.getStartDateTime() );
        appointmentResponse.endDateTime( appointment.getEndDateTime() );
        appointmentResponse.status( appointment.getStatus() );
        appointmentResponse.consultationReason( appointment.getConsultationReason() );
        appointmentResponse.notes( appointment.getNotes() );

        return appointmentResponse.build();
    }
}
