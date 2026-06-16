package com.adakadavra.dentis.documents.infrastructure.persistence.entity;

import com.adakadavra.dentis.documents.domain.model.Visibility;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_files", indexes = {
        @Index(name = "idx_doc_file_clinic", columnList = "clinic_id"),
        @Index(name = "idx_doc_file_folder", columnList = "folder_id"),
        @Index(name = "idx_doc_file_owner", columnList = "owner_user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFileEntity implements Persistable<UUID> {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "folder_id", nullable = false)
    private UUID folderId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "s3_key", nullable = false, length = 1000)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "indexed_for_ia", nullable = false)
    private boolean indexedForIa;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PostLoad
    void markNotNew() { this.isNew = false; }

    @Override
    public boolean isNew() { return isNew; }
}
