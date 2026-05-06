package com.adakadavra.dentis.patient.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepresentativeDto {

    private String fullName;
    private String idDocument;
    private String relationship;
    private String phoneNumber;
}
