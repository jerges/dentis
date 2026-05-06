package com.adakadavra.dentis.clinical.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clinical_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalRecordEntity {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false, unique = true)
    private UUID patientId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "clinicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OdontogramToothEntity> odontogram = new ArrayList<>();

    @OneToMany(mappedBy = "clinicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClinicalEvolutionEntity> evolutions = new ArrayList<>();

    @OneToMany(mappedBy = "clinicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiagnosisEntity> diagnoses = new ArrayList<>();

    @OneToMany(mappedBy = "clinicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TreatmentPlanEntity> treatmentPlans = new ArrayList<>();
}

