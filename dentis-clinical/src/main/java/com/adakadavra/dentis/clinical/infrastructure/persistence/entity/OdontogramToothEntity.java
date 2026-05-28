package com.adakadavra.dentis.clinical.infrastructure.persistence.entity;

import com.adakadavra.dentis.clinical.domain.model.DentalArch;
import com.adakadavra.dentis.clinical.domain.model.SpaceClosureStatus;
import com.adakadavra.dentis.clinical.domain.model.ToothCondition;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "odontogram_teeth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdontogramToothEntity implements Persistable<UUID> {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_record_id", nullable = false)
    private ClinicalRecordEntity clinicalRecord;

    @Column(name = "tooth_number", nullable = false)
    private int toothNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "dental_arch", length = 20)
    private DentalArch dentalArch;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 30)
    private ToothCondition condition;

    @Column(name = "affected_surfaces", length = 200)
    private String affectedSurfaces;

    /** JSON-like serialisation: "MESIAL:CARIES,DISTAL:HEALTHY". */
    @Column(name = "surface_conditions", columnDefinition = "TEXT")
    private String surfaceConditions;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_closure_status", length = 20)
    private SpaceClosureStatus spaceClosureStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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
