package com.dentis.infrastructure.scheduler;

import com.dentis.application.service.NotificationService;
import com.dentis.common.constant.AppConstants;
import com.dentis.domain.entity.Appointment;
import com.dentis.domain.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendDailyReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(AppConstants.Appointment.REMINDER_HOURS_BEFORE - 2);
        LocalDateTime to = LocalDateTime.now().plusHours(AppConstants.Appointment.REMINDER_HOURS_BEFORE + 2);

        List<Appointment> appointments = appointmentRepository.findAppointmentsForReminder(from, to);
        log.info("Sending reminders for {} appointments", appointments.size());
        appointments.forEach(notificationService::sendAppointmentReminder);
    }
}
