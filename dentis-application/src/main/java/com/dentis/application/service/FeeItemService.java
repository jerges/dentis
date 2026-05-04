package com.dentis.application.service;

import com.dentis.application.dto.request.CreateFeeItemRequest;
import com.dentis.application.dto.response.FeeItemResponse;
import com.dentis.common.response.PageResponse;
import com.dentis.domain.enums.FeeCategory;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FeeItemService {

    FeeItemResponse create(CreateFeeItemRequest request);

    FeeItemResponse update(String id, CreateFeeItemRequest request);

    FeeItemResponse findById(String id);

    PageResponse<FeeItemResponse> findByCategory(FeeCategory category, Pageable pageable);

    PageResponse<FeeItemResponse> search(String query, Pageable pageable);

    List<FeeItemResponse> findAllActive();

    void deactivate(String id);
}
