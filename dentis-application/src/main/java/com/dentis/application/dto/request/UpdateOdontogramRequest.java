package com.dentis.application.dto.request;

import com.dentis.domain.enums.ToothStatus;
import com.dentis.domain.enums.ToothSurface;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UpdateOdontogramRequest {

    @NotNull
    @Min(11)
    @Max(85)
    private Integer toothNumber;

    @NotNull
    private ToothStatus status;

    private Set<ToothSurface> affectedSurfaces;

    private String notes;
}
