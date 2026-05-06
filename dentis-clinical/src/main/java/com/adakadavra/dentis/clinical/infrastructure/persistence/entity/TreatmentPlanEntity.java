package com.adakadavra.dentis.clinical.infrastructure.persistence.entity;

import com.adakadavra.dentis.clinical.domain.model.TreatmentPlanStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "treatment_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentPlanEntity {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_record_id", nullable = false)
    private ClinicalRecordEntity clinicalRecord;

    @Column(name = "dentist_id", nullable = false)
    private UUID dentistId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TreatmentPlanStatus status;

    @Column(name = "budget_id")
    private UUID budgetId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "estimated_end_date")
    private LocalDate estimatedEndDate;

    @OneToMany(mappedBy = "treatmentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TreatmentProcedureEntity> procedures = new ArrayList<>();
}

