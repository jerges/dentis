package com.dentis.domain.entity;

import com.dentis.domain.enums.ToothStatus;
import com.dentis.domain.enums.ToothSurface;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "odontogram_teeth",
        uniqueConstraints = @UniqueConstraint(columnNames = {"clinical_record_id", "tooth_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdontogramTooth extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinical_record_id", nullable = false)
    private ClinicalRecord clinicalRecord;

    @Column(nullable = false)
    private Integer toothNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ToothStatus status = ToothStatus.HEALTHY;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tooth_affected_surfaces",
            joinColumns = @JoinColumn(name = "tooth_id"))
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<ToothSurface> affectedSurfaces = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String notes;
}
