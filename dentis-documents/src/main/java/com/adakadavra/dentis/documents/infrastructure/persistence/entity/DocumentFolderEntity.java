package com.adakadavra.dentis.documents.infrastructure.persistence.entity;

import com.adakadavra.dentis.documents.domain.model.DocumentZone;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_folders",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_folder_clinic_parent_name",
                columnNames = {"clinic_id", "parent_id", "name"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFolderEntity implements Persistable<UUID> {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "s3_prefix", nullable = false, length = 500)
    private String s3Prefix;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false, length = 20)
    private DocumentZone zone;

    @Column(name = "system_folder", nullable = false)
    @Builder.Default
    private boolean systemFolder = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PostLoad
    void markNotNew() { this.isNew = false; }

    @Override
    public boolean isNew() { return isNew; }
}
