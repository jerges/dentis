package com.adakadavra.dentis.patient.infrastructure.persistence.entity;

import com.adakadavra.dentis.patient.domain.model.Gender;
import com.adakadavra.dentis.patient.domain.model.Sex;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_patient_id_document", columnList = "id_document"),
        @Index(name = "idx_patient_name", columnList = "first_name, last_name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientEntity implements Persistable<UUID> {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "id_document", nullable = false, unique = true, length = 20)
    private String idDocument;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 20)
    private Sex sex;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "social_name", length = 150)
    private String socialName;

    // ContactInfo embedded
    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "alternative_phone", length = 20)
    private String alternativePhone;

    // Address embedded
    @Column(name = "street", length = 200)
    private String street;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    // Representative embedded
    @Column(name = "representative_full_name", length = 200)
    private String representativeFullName;

    @Column(name = "representative_id_document", length = 20)
    private String representativeIdDocument;

    @Column(name = "representative_relationship", length = 50)
    private String representativeRelationship;

    @Column(name = "representative_phone", length = 20)
    private String representativePhone;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Clinic this patient belongs to. */
    @Column(name = "clinic_id")
    private UUID clinicId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    @Transient
    public boolean isNew() {
        return id == null;
    }
}
