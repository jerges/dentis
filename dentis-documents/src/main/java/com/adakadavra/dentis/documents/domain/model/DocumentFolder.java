package com.adakadavra.dentis.documents.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFolder {

    private UUID id;
    private UUID clinicId;
    private UUID parentId;        // null = raíz de la clínica
    private String name;
    private String path;          // ruta materializada, e.g. "/Facturas/2026"
    private FolderType type;
    private Visibility visibility;
    private UUID ownerUserId;
    private boolean system;       // true = no borrable/renombrable
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
