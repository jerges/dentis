package com.dentis.application.mapper;

import com.dentis.application.dto.request.CreateFeeItemRequest;
import com.dentis.application.dto.response.FeeItemResponse;
import com.dentis.domain.entity.FeeItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FeeItemMapper {

    FeeItemResponse toResponse(FeeItem feeItem);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    FeeItem toEntity(CreateFeeItemRequest request);
}
