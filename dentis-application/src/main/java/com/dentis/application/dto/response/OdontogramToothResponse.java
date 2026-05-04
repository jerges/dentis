package com.dentis.application.dto.response;

import com.dentis.domain.enums.ToothStatus;
import com.dentis.domain.enums.ToothSurface;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class OdontogramToothResponse {
    private String id;
    private Integer toothNumber;
    private ToothStatus status;
    private Set<ToothSurface> affectedSurfaces;
    private String notes;
}
