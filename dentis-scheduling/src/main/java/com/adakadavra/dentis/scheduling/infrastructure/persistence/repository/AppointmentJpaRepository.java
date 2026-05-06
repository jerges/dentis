package com.adakadavra.dentis.scheduling.infrastructure.persistence.repository;

import com.adakadavra.dentis.scheduling.infrastructure.persistence.entity.AppointmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, UUID> {

    @Query("""
            SELECT a FROM AppointmentEntity a
            WHERE a.dentistId = :dentistId
            AND a.startDateTime >= :from AND a.endDateTime <= :to
            AND a.status NOT IN ('CANCELLED')
            ORDER BY a.startDateTime ASC
            """)
    List<AppointmentEntity> findByDentistAndDateRange(
            @Param("dentistId") UUID dentistId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    List<AppointmentEntity> findByPatientId(UUID patientId);

    Page<AppointmentEntity> findByDentistId(UUID dentistId, Pageable pageable);

    @Query("""
            SELECT COUNT(a) > 0 FROM AppointmentEntity a
            WHERE a.dentistId = :dentistId
            AND a.status NOT IN ('CANCELLED')
            AND a.startDateTime < :end AND a.endDateTime > :start
            AND (:excludeId IS NULL OR a.id != :excludeId)
            """)
    boolean hasConflict(
            @Param("dentistId") UUID dentistId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") UUID excludeId
    );

    @Modifying
    @Query("UPDATE AppointmentEntity a SET a.status = 'CANCELLED' WHERE a.id = :id")
    void cancelById(@Param("id") UUID id);
}
