package com.adakadavra.dentis.clinical.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clinical_evolutions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalEvolutionEntity {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_record_id", nullable = false)
    private ClinicalRecordEntity clinicalRecord;

    @Column(name = "dentist_id", nullable = false)
    private UUID dentistId;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "findings", columnDefinition = "TEXT")
    private String findings;

    @Column(name = "treatment", columnDefinition = "TEXT")
    private String treatment;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}

