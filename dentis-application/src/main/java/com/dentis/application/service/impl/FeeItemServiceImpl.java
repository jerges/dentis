package com.dentis.application.service.impl;

import com.dentis.application.dto.request.CreateFeeItemRequest;
import com.dentis.application.dto.response.FeeItemResponse;
import com.dentis.application.mapper.FeeItemMapper;
import com.dentis.application.service.FeeItemService;
import com.dentis.common.exception.BusinessException;
import com.dentis.common.exception.ResourceNotFoundException;
import com.dentis.common.response.PageResponse;
import com.dentis.domain.entity.FeeItem;
import com.dentis.domain.enums.FeeCategory;
import com.dentis.domain.repository.FeeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeItemServiceImpl implements FeeItemService {

    private final FeeItemRepository feeItemRepository;
    private final FeeItemMapper feeItemMapper;

    @Override
    @Transactional
    public FeeItemResponse create(CreateFeeItemRequest request) {
        if (request.getCode() != null && feeItemRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Fee item with code " + request.getCode() + " already exists",
                    "DUPLICATE_CODE");
        }
        FeeItem item = feeItemMapper.toEntity(request);
        return feeItemMapper.toResponse(feeItemRepository.save(item));
    }

    @Override
    @Transactional
    public FeeItemResponse update(String id, CreateFeeItemRequest request) {
        FeeItem item = findOrThrow(id);
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setBasePrice(request.getBasePrice());
        item.setMaxDiscountPercentage(request.getMaxDiscountPercentage() != null
                ? request.getMaxDiscountPercentage() : BigDecimal.ZERO);
        item.setDiscountAllowed(request.isDiscountAllowed());
        item.setCode(request.getCode());
        item.setCurrency(request.getCurrency());
        return feeItemMapper.toResponse(feeItemRepository.save(item));
    }

    @Override
    public FeeItemResponse findById(String id) {
        return feeItemMapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<FeeItemResponse> findByCategory(FeeCategory category, Pageable pageable) {
        Page<FeeItem> page = feeItemRepository.findByActiveTrueAndCategory(category, pageable);
        return PageResponse.of(
                page.getContent().stream().map(feeItemMapper::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements()
        );
    }

    @Override
    public PageResponse<FeeItemResponse> search(String query, Pageable pageable) {
        Page<FeeItem> page = feeItemRepository.searchFeeItems(query, pageable);
        return PageResponse.of(
                page.getContent().stream().map(feeItemMapper::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements()
        );
    }

    @Override
    public List<FeeItemResponse> findAllActive() {
        return feeItemRepository.findByActiveTrue().stream()
                .map(feeItemMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deactivate(String id) {
        FeeItem item = findOrThrow(id);
        item.setActive(false);
        feeItemRepository.save(item);
    }

    private FeeItem findOrThrow(String id) {
        return feeItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeeItem", id));
    }
}
