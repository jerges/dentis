package com.adakadavra.dentis.documents.infrastructure.persistence.entity;

import com.adakadavra.dentis.documents.domain.model.ResourceType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_shares", indexes = {
        @Index(name = "idx_doc_share_resource", columnList = "resource_type, resource_id"),
        @Index(name = "idx_doc_share_user", columnList = "shared_with_user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentShareEntity implements Persistable<UUID> {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 10)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "shared_with_user_id", nullable = false)
    private UUID sharedWithUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PostLoad
    void markNotNew() { this.isNew = false; }

    @Override
    public boolean isNew() { return isNew; }
}
