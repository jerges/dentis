package com.adakadavra.dentis.clinical.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "diagnoses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisEntity {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_record_id", nullable = false)
    private ClinicalRecordEntity clinicalRecord;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "diagnosed_at", nullable = false)
    private LocalDate diagnosedAt;

    @Column(name = "dentist_id", nullable = false)
    private UUID dentistId;
}

