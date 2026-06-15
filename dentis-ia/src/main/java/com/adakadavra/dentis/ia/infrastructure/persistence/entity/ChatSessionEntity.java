package com.adakadavra.dentis.ia.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ia_chat_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "dentist_id", nullable = false)
    private UUID dentistId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew;

    @Override
    public boolean isNew() { return isNew; }
}