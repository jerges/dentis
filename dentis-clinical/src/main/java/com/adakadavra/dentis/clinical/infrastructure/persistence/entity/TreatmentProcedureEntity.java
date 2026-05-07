package com.adakadavra.dentis.clinical.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "treatment_procedures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentProcedureEntity implements Persistable<UUID> {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_plan_id", nullable = false)
    private TreatmentPlanEntity treatmentPlan;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "tooth_number")
    private Integer toothNumber;

    @Column(name = "performed", nullable = false)
    private boolean performed;

    @Column(name = "performed_at")
    private LocalDateTime performedAt;

    @Column(name = "performed_by_dentist_id")
    private UUID performedByDentistId;

    @Column(name = "budget_item_id")
    private UUID budgetItemId;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
