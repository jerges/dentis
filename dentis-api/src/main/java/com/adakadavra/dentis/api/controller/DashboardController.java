package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.api.security.entity.User;
import com.adakadavra.dentis.api.security.entity.UserRole;
import com.adakadavra.dentis.api.security.service.TenantSecurityService;
import com.adakadavra.dentis.billing.domain.repository.PaymentRepository;
import com.adakadavra.dentis.common.response.ApiResponse;
import com.adakadavra.dentis.ia.infrastructure.persistence.repository.ChatSessionJpaRepository;
import com.adakadavra.dentis.patient.domain.repository.PatientRepository;
import com.adakadavra.dentis.scheduling.domain.repository.AppointmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated operational statistics")
public class DashboardController {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final ChatSessionJpaRepository chatSessionJpaRepository;
    private final TenantSecurityService tenantSecurity;

    @GetMapping("/stats")
    @Operation(summary = "Get summary stats for the dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        long totalPatients = patientRepository.findAll(PageRequest.of(0, 1)).getTotalElements();

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atTime(LocalTime.MIN);
        LocalDateTime todayEnd   = today.atTime(LocalTime.MAX);
        long todayAppointments = appointmentRepository.findScheduledBetween(todayStart, todayEnd).size();

        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(LocalTime.MIN);
        LocalDateTime weekEnd   = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
        long weekAppointments = appointmentRepository.findScheduledBetween(weekStart, weekEnd).size();

        BigDecimal totalRevenue = paymentRepository.sumAllPayments();

        DashboardStats stats = new DashboardStats(totalPatients, todayAppointments, weekAppointments, totalRevenue);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    /**
     * IA token cost usage per user/clinic.
     * ADMIN → sees their own clinic's breakdown per user.
     * SUPER_ADMIN → sees all clinics' aggregated cost.
     */
    @GetMapping("/ia-usage")
    @Operation(summary = "IA token usage and cost by user (ADMIN: own clinic; SUPER_ADMIN: all clinics)")
    public ResponseEntity<ApiResponse<IaUsageResponse>> getIaUsage(@AuthenticationPrincipal User user) {
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            List<IaUsageRow> rows = chatSessionJpaRepository.getUsageAll().stream()
                    .map(r -> new IaUsageRow(
                            str(r[0]), str(r[1]),
                            toLong(r[2]), toLong(r[3]), toLong(r[4])))
                    .toList();
            return ResponseEntity.ok(ApiResponse.ok(new IaUsageResponse("ALL", rows)));
        }
        UUID clinicId = tenantSecurity.currentClinicId()
                .orElseThrow(() -> new IllegalStateException("No clinic context"));
        List<IaUsageRow> rows = chatSessionJpaRepository.getUsageByClinic(clinicId).stream()
                .map(r -> new IaUsageRow(
                        str(r[0]), null,
                        toLong(r[1]), toLong(r[2]), toLong(r[3])))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(new IaUsageResponse(clinicId.toString(), rows)));
    }

    private static String str(Object o) { return o != null ? o.toString() : null; }
    private static long toLong(Object o) { return o != null ? Long.parseLong(o.toString()) : 0L; }

    public record DashboardStats(
            long totalPatients,
            long todayAppointments,
            long weekAppointments,
            java.math.BigDecimal totalRevenue) {}

    public record IaUsageRow(
            String username,
            String clinicName,
            long messageCount,
            long totalInputTokens,
            long totalOutputTokens) {}

    public record IaUsageResponse(String scope, List<IaUsageRow> rows) {}
}
