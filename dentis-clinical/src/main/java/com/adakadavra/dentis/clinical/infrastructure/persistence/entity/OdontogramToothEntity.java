package com.adakadavra.dentis.clinical.infrastructure.persistence.entity;

import com.adakadavra.dentis.clinical.domain.model.ToothCondition;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "odontogram_teeth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdontogramToothEntity {

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

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}

