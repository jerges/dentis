package com.adakadavra.dentis.documents.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PresignUploadRequest {
    @NotNull
    private UUID folderId;
    @NotBlank
    private String fileName;
    @NotBlank
    private String contentType;
}
