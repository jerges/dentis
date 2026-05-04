package com.dentis.application.service;

import com.dentis.domain.entity.Appointment;

public interface NotificationService {

    void sendAppointmentConfirmation(Appointment appointment);

    void sendAppointmentReminder(Appointment appointment);

    void sendAppointmentCancellation(Appointment appointment);

    void processUnsentNotifications();
}
