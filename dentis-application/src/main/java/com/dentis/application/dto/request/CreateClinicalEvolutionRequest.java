package com.dentis.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CreateClinicalEvolutionRequest {

    @NotBlank
    private String clinicalRecordId;

    private String dentistId;

    @NotNull
    private LocalDate evolutionDate;

    @NotBlank
    private String description;

    private String procedure;

    private String observations;

    private String appointmentId;
}
