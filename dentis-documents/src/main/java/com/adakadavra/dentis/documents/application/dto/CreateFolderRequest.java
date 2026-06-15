package com.adakadavra.dentis.documents.application.dto;

import com.adakadavra.dentis.documents.domain.model.DocumentVisibility;
import com.adakadavra.dentis.documents.domain.model.DocumentZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateFolderRequest {
    /** Null = create at zone root. */
    private UUID parentId;
    @NotBlank
    private String name;
    @NotNull
    private DocumentZone zone;
    /** Defaults to PUBLIC when not provided. */
    private DocumentVisibility visibility;
}
