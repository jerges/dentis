package com.adakadavra.dentis.billing.infrastructure.persistence.entity;

import com.adakadavra.dentis.billing.domain.model.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_budget", columnList = "budget_id"),
        @Index(name = "idx_payment_patient", columnList = "patient_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "invoice_reference", length = 100)
    private String invoiceReference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;
}

