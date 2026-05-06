package com.adakadavra.dentis.billing.infrastructure.persistence.entity;

import com.adakadavra.dentis.billing.domain.model.BudgetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "budgets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetEntity {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "treatment_plan_id")
    private UUID treatmentPlanId;

    @Column(name = "dentist_id", nullable = false)
    private UUID dentistId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BudgetStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BudgetItemEntity> items = new ArrayList<>();
}

