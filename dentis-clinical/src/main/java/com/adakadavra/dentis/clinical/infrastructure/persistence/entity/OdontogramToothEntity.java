package com.adakadavra.dentis.clinical.infrastructure.persistence.entity;

import com.adakadavra.dentis.clinical.domain.model.SpaceStatus;
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
    @Column(name = "condition", nullable = false, length = 30)
    private ToothCondition condition;

    @Column(name = "affected_surfaces", length = 200)
    private String affectedSurfaces;

    @Column(name = "surface_conditions", length = 500)
    private String surfaceConditions;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_status", length = 30)
    private SpaceStatus spaceStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "root_findings", length = 200)
    private String rootFindings;

    @Column(name = "root_notes", columnDefinition = "TEXT")
    private String rootNotes;

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
