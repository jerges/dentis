package com.dentis.domain.repository;

import com.dentis.domain.entity.Appointment;
import com.dentis.domain.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    Page<Appointment> findByPatientId(String patientId, Pageable pageable);

    Page<Appointment> findByDentistId(String dentistId, Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.dentist.id = :dentistId " +
           "AND a.startDateTime >= :from AND a.startDateTime <= :to " +
           "AND a.status NOT IN (:excludedStatuses)")
    List<Appointment> findDentistAppointmentsBetween(
            @Param("dentistId") String dentistId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("excludedStatuses") List<AppointmentStatus> excludedStatuses);

    @Query("SELECT a FROM Appointment a WHERE a.dentist.id = :dentistId " +
           "AND a.startDateTime < :end AND a.endDateTime > :start " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW')")
    List<Appointment> findConflictingAppointments(
            @Param("dentistId") String dentistId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Appointment a WHERE a.reminderSent = false " +
           "AND a.status = 'SCHEDULED' " +
           "AND a.startDateTime BETWEEN :from AND :to")
    List<Appointment> findAppointmentsForReminder(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<Appointment> findByPatientIdOrderByStartDateTimeDesc(String patientId);
}
