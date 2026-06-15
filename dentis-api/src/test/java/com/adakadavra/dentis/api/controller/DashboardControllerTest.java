package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.billing.domain.repository.PaymentRepository;
import com.adakadavra.dentis.common.response.ApiResponse;
import com.adakadavra.dentis.patient.domain.repository.PatientRepository;
import com.adakadavra.dentis.scheduling.domain.model.Appointment;
import com.adakadavra.dentis.scheduling.domain.repository.AppointmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController")
class DashboardControllerTest {

    @Mock private PatientRepository patientRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    @DisplayName("should aggregate stats correctly")
    void shouldAggregateStatsCorrectly() {
        when(patientRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(1), 42L));
        when(appointmentRepository.findScheduledBetween(any(), any())).thenReturn(List.of());
        when(paymentRepository.sumAllPayments()).thenReturn(new BigDecimal("1500.00"));

        ResponseEntity<ApiResponse<DashboardController.DashboardStats>> response = dashboardController.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DashboardController.DashboardStats stats = response.getBody().getData();
        assertThat(stats.totalPatients()).isEqualTo(42L);
        assertThat(stats.totalRevenue()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("should return zero revenue when no payments exist")
    void shouldReturnZeroRevenueWhenNoPayments() {
        when(patientRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(1), 0L));
        when(appointmentRepository.findScheduledBetween(any(), any())).thenReturn(List.of());
        when(paymentRepository.sumAllPayments()).thenReturn(BigDecimal.ZERO);

        ResponseEntity<ApiResponse<DashboardController.DashboardStats>> response = dashboardController.getStats();

        assertThat(response.getBody().getData().totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should count week appointments separately from today appointments")
    void shouldCountWeekAppointmentsSeparately() {
        when(patientRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(1), 5L));
        List<Appointment> weekAppts = List.of(mock(), mock(), mock());
        when(appointmentRepository.findScheduledBetween(any(), any()))
                .thenReturn(List.of())  // today
                .thenReturn(weekAppts); // week
        when(paymentRepository.sumAllPayments()).thenReturn(BigDecimal.ZERO);

        ResponseEntity<ApiResponse<DashboardController.DashboardStats>> response = dashboardController.getStats();

        DashboardController.DashboardStats stats = response.getBody().getData();
        assertThat(stats.todayAppointments()).isEqualTo(0L);
        assertThat(stats.weekAppointments()).isEqualTo(3L);
    }

    private Appointment mock() {
        return org.mockito.Mockito.mock(Appointment.class);
    }
}
