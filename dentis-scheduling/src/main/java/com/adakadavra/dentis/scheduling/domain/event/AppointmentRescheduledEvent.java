package com.adakadavra.dentis.scheduling.domain.event;

import com.adakadavra.dentis.scheduling.application.dto.AppointmentResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AppointmentRescheduledEvent extends ApplicationEvent {

    private final AppointmentResponse appointment;

    public AppointmentRescheduledEvent(Object source, AppointmentResponse appointment) {
        super(source);
        this.appointment = appointment;
    }
}
