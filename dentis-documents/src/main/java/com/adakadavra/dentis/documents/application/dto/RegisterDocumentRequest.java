package com.adakadavra.dentis.documents.application.dto;

import com.adakadavra.dentis.documents.domain.model.DocumentVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RegisterDocumentRequest {
    @NotNull
    private UUID folderId;
    @NotBlank
    private String fileName;
    @NotBlank
    private String contentType;
    @NotBlank
    private String s3Key;
    private Long fileSize;
    private String description;
    /** Defaults to PUBLIC when not provided. */
    private DocumentVisibility visibility;
}
