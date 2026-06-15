package com.adakadavra.dentis.scheduling.infrastructure.persistence.repository;

import com.adakadavra.dentis.scheduling.domain.model.Appointment;
import com.adakadavra.dentis.scheduling.domain.repository.AppointmentRepository;
import com.adakadavra.dentis.scheduling.infrastructure.persistence.mapper.AppointmentEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AppointmentRepositoryAdapter implements AppointmentRepository {

    private final AppointmentJpaRepository jpaRepository;
    private final AppointmentEntityMapper entityMapper;

    @Override
    public Appointment save(Appointment appointment) {
        return entityMapper.toDomain(jpaRepository.save(entityMapper.toEntity(appointment)));
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        return jpaRepository.findById(id).map(entityMapper::toDomain);
    }

    @Override
    public List<Appointment> findByDentistAndDateRange(UUID dentistId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByDentistAndDateRange(dentistId, from, to)
                .stream().map(entityMapper::toDomain).toList();
    }

    @Override
    public List<Appointment> findByPatientId(UUID patientId) {
        return jpaRepository.findByPatientId(patientId)
                .stream().map(entityMapper::toDomain).toList();
    }

    @Override
    public Page<Appointment> findByDentistId(UUID dentistId, Pageable pageable) {
        return jpaRepository.findByDentistId(dentistId, pageable).map(entityMapper::toDomain);
    }

    @Override
    public List<Appointment> findScheduledBetween(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findScheduledBetween(from, to)
                .stream().map(entityMapper::toDomain).toList();
    }

    @Override
    public boolean hasConflict(UUID dentistId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId) {
        return jpaRepository.hasConflict(dentistId, start, end, excludeAppointmentId);
    }

    @Override
    public void cancel(UUID id) {
        jpaRepository.cancelById(id);
    }
}
