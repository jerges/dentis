package com.dentis.application.service;

import com.dentis.application.dto.request.CreateAppointmentRequest;
import com.dentis.application.dto.request.UpdateAppointmentRequest;
import com.dentis.application.dto.response.AppointmentResponse;
import com.dentis.common.response.PageResponse;
import com.dentis.domain.enums.AppointmentStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {

    AppointmentResponse create(CreateAppointmentRequest request);

    AppointmentResponse update(String id, UpdateAppointmentRequest request);

    AppointmentResponse findById(String id);

    PageResponse<AppointmentResponse> findByPatient(String patientId, Pageable pageable);

    PageResponse<AppointmentResponse> findByDentist(String dentistId, Pageable pageable);

    List<AppointmentResponse> findDentistCalendar(String dentistId, LocalDateTime from, LocalDateTime to);

    AppointmentResponse updateStatus(String id, AppointmentStatus status);

    void cancel(String id);
}
